package com.c2c.coupon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.exception.BusinessException;
import com.c2c.coupon.entity.CouponTemplate;
import com.c2c.coupon.entity.UserCoupon;
import com.c2c.coupon.mapper.CouponTemplateMapper;
import com.c2c.coupon.mapper.UserCouponMapper;
import com.c2c.product.entity.Category;
import com.c2c.product.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 优惠券用户端服务：可领列表、并发领券、我的券、抵扣试算与支付锁定。
 *
 * <p>并发口径：领券库存走模板表原子条件更新（claimed_count &lt; total_count）防超发；
 * 同一用户重复领取靠 user_coupon 唯一索引 (coupon_id, user_id) 兜底。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    /** 状态：未使用 */
    public static final int STATUS_UNUSED = 0;
    /** 状态：已使用 */
    public static final int STATUS_USED = 1;
    /** 状态：已过期 */
    public static final int STATUS_EXPIRED = 2;
    /** 状态：已作废 */
    public static final int STATUS_VOID = 3;
    /** 状态：已锁定（待支付占用，支付成功转已使用，取消/超时退回未使用） */
    public static final int STATUS_RESERVED = 4;

    /** 领券分布式锁前缀（按券模板 ID 加锁），多实例部署下避免同券并发超卖；DB 原子更新仍为最终兜底 */
    private static final String CLAIM_LOCK_PREFIX = "coupon:lock:claim:";
    /** 锁自动过期时间，防持锁进程崩溃死锁 */
    private static final Duration CLAIM_LOCK_TTL = Duration.ofSeconds(10);
    /** 抢锁自旋次数与间隔 */
    private static final int CLAIM_LOCK_RETRIES = 5;
    private static final long CLAIM_LOCK_RETRY_MS = 80;
    /** 释放锁 Lua 脚本：仅当锁 value 匹配才删除，避免误删他人锁 */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end");
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    private final CouponTemplateMapper templateMapper;
    private final UserCouponMapper userCouponMapper;
    private final CategoryMapper categoryMapper;
    private final StringRedisTemplate redisTemplate;

    /** 可领取券列表：生效中、在有效期内、尚有库存；标注当前用户是否已领。
     *  categoryId 非空时仅返回通用券或绑定该分类的券。 */
    public Page<Map<String, Object>> available(Long userId, int page, int size, Long categoryId) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<CouponTemplate> w = new LambdaQueryWrapper<CouponTemplate>()
                .eq(CouponTemplate::getStatus, 1)
                .and(q -> q.isNull(CouponTemplate::getValidStart).or().le(CouponTemplate::getValidStart, now))
                .and(q -> q.isNull(CouponTemplate::getValidEnd).or().ge(CouponTemplate::getValidEnd, now));
        if (categoryId != null) {
            // 递归匹配：通用券，或券绑分类落在商品分类自身/祖先链上
            Set<Long> catChain = ancestorChain(categoryId);
            w.and(q -> q.eq(CouponTemplate::getCategoryId, 0)
                    .or().in(CouponTemplate::getCategoryId, catChain));
        }
        w.orderByDesc(CouponTemplate::getId);

        Page<CouponTemplate> tplPage = templateMapper.selectPage(new Page<>(page, size), w);
        // 游客（userId 为空）不标记已领；登录用户再查本人已领券
        Set<Long> claimed = new HashSet<>();
        if (userId != null) {
            claimed = userCouponMapper.selectList(
                            new LambdaQueryWrapper<UserCoupon>().eq(UserCoupon::getUserId, userId))
                    .stream().map(UserCoupon::getCouponId).collect(Collectors.toSet());
        }

        Map<Long, String> catNames = categoryNameMap(tplPage.getRecords().stream()
                .map(CouponTemplate::getCategoryId).filter(c -> c != null && c > 0).collect(Collectors.toSet()));

        Page<Map<String, Object>> result = new Page<>(tplPage.getCurrent(), tplPage.getSize(), tplPage.getTotal());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (CouponTemplate t : tplPage.getRecords()) {
            rows.add(toAvailableRow(t, claimed.contains(t.getId()), catNames));
        }
        result.setRecords(rows);
        return result;
    }

    /** 领取券：分布式锁内原子扣库存 + 写用户券，重复领取/抢光均拒绝 */
    @Transactional
    public UserCoupon claim(Long couponId, Long userId) {
        String lockKey = CLAIM_LOCK_PREFIX + couponId;
        String lockValue = UUID.randomUUID().toString();
        if (!tryClaimLock(lockKey, lockValue)) {
            throw new BusinessException("手慢了，请稍后再试");
        }
        try {
            CouponTemplate tpl = templateMapper.selectById(couponId);
            if (tpl == null) {
                throw new BusinessException("优惠券不存在");
            }
            LocalDateTime now = LocalDateTime.now();
            if (tpl.getStatus() == null || tpl.getStatus() != 1) {
                throw new BusinessException("优惠券已停用");
            }
            if (tpl.getValidStart() != null && tpl.getValidStart().isAfter(now)) {
                throw new BusinessException("优惠券尚未开始");
            }
            if (tpl.getValidEnd() != null && tpl.getValidEnd().isBefore(now)) {
                throw new BusinessException("优惠券已过期");
            }

            Long exists = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                    .eq(UserCoupon::getCouponId, couponId)
                    .eq(UserCoupon::getUserId, userId));
            if (exists != null && exists > 0) {
                throw new BusinessException("您已领取过该券");
            }

            int rows = templateMapper.claimOne(couponId);
            if (rows == 0) {
                throw new BusinessException("优惠券已被抢光");
            }

            UserCoupon coupon = new UserCoupon();
            coupon.setCouponId(couponId);
            coupon.setUserId(userId);
            coupon.setStatus(STATUS_UNUSED);
            coupon.setDiscountAmount(tpl.getDiscountAmount());
            coupon.setExpireTime(tpl.getValidEnd());
            try {
                userCouponMapper.insert(coupon);
            } catch (Exception e) {
                // 唯一索引兜底：并发重复领取，回滚库存计数后提示
                log.warn("领券唯一索引冲突: couponId={}, userId={}", couponId, userId);
                throw new BusinessException("您已领取过该券");
            }
            log.info("coupon claimed: userCouponId={}, couponId={}, userId={}", coupon.getId(), couponId, userId);
            return coupon;
        } finally {
            releaseClaimLock(lockKey, lockValue);
        }
    }

    /** 自旋抢锁：SET NX 带过期时间，成功返回 true；超时未拿到返回 false */
    private boolean tryClaimLock(String key, String value) {
        for (int i = 0; i < CLAIM_LOCK_RETRIES; i++) {
            Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, value, CLAIM_LOCK_TTL);
            if (Boolean.TRUE.equals(ok)) {
                return true;
            }
            try {
                Thread.sleep(CLAIM_LOCK_RETRY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /** 仅当锁 value 匹配才删除，避免误删其他线程/实例持有的锁 */
    private void releaseClaimLock(String key, String value) {
        try {
            redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), value);
        } catch (Exception e) {
            log.warn("释放领券锁失败: key={}, err={}", key, e.getMessage());
        }
    }

    /** 我的券列表（可按状态筛选），顺带把已到期未用券置为过期 */
    public List<Map<String, Object>> myCoupons(Long userId, Integer status) {
        expireDueCoupons(userId);
        LambdaQueryWrapper<UserCoupon> w = new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .orderByDesc(UserCoupon::getId);
        if (status != null) {
            w.eq(UserCoupon::getStatus, status);
        }
        List<UserCoupon> coupons = userCouponMapper.selectList(w);
        Set<Long> couponIds = coupons.stream().map(UserCoupon::getCouponId).collect(Collectors.toSet());
        Map<Long, CouponTemplate> tplMap = couponIds.isEmpty() ? new HashMap<>()
                : templateMapper.selectBatchIds(couponIds).stream()
                .collect(Collectors.toMap(CouponTemplate::getId, t -> t, (a, b) -> a));
        Map<Long, String> catNames = categoryNameMap(tplMap.values().stream()
                .map(CouponTemplate::getCategoryId).filter(c -> c != null && c > 0).collect(Collectors.toSet()));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (UserCoupon uc : coupons) {
            CouponTemplate tpl = tplMap.get(uc.getCouponId());
            Map<String, Object> row = new HashMap<>();
            row.put("userCouponId", uc.getId());
            row.put("couponId", uc.getCouponId());
            row.put("status", uc.getStatus());
            row.put("discountAmount", uc.getDiscountAmount());
            row.put("orderId", uc.getOrderId());
            row.put("receiveTime", uc.getReceiveTime());
            row.put("useTime", uc.getUseTime());
            row.put("expireTime", uc.getExpireTime());
            row.put("name", tpl != null ? tpl.getName() : null);
            row.put("type", tpl != null ? tpl.getType() : null);
            row.put("minAmount", tpl != null ? tpl.getMinAmount() : null);
            row.put("categoryId", tpl != null ? tpl.getCategoryId() : null);
            row.put("categoryName", tpl != null ? catNames.get(tpl.getCategoryId()) : null);
            rows.add(row);
        }
        return rows;
    }

    /**
     * 下单前抵扣试算：校验券归属、状态、有效期与门槛，返回抵扣额与应付金额。
     *
     * @param userCouponId 用户券 ID
     * @param userId       买家
     * @param orderAmount  订单金额（商品价 + 运费，抵扣前）
     * @param categoryId   商品分类 ID（可空；非空时校验券是否适用该分类）
     */
    public Map<String, Object> calculate(Long userCouponId, Long userId, BigDecimal orderAmount, Long categoryId) {
        UserCoupon uc = requireUsable(userCouponId, userId);
        CouponTemplate tpl = templateMapper.selectById(uc.getCouponId());
        if (tpl == null || tpl.getStatus() == null || tpl.getStatus() != 1) {
            throw new BusinessException("优惠券已停用");
        }
        BigDecimal minAmount = tpl.getMinAmount() != null ? tpl.getMinAmount() : BigDecimal.ZERO;
        if (orderAmount.compareTo(minAmount) < 0) {
            throw new BusinessException("未满足使用门槛：满 ¥" + minAmount + " 可用");
        }
        Long tplCat = tpl.getCategoryId();
        if (categoryId != null && tplCat != null && tplCat != 0
                && !ancestorChain(categoryId).contains(tplCat)) {
            throw new BusinessException("该券不适用于此分类商品");
        }

        BigDecimal discount;
        if (Integer.valueOf(CouponTemplate.TYPE_DISCOUNT).equals(tpl.getType())) {
            // 折扣券：按订单金额与折扣率算减免额，保留两位；再按模板封顶金额截断（0 不封顶）
            BigDecimal rate = tpl.getDiscountAmount();
            discount = orderAmount.multiply(BigDecimal.ONE.subtract(rate))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal cap = tpl.getMaxDiscountAmount();
            if (cap != null && cap.compareTo(BigDecimal.ZERO) > 0 && discount.compareTo(cap) > 0) {
                discount = cap;
            }
        } else {
            // 满减券：减免额为领取时快照
            discount = uc.getDiscountAmount();
        }
        if (discount.compareTo(orderAmount) > 0) {
            discount = orderAmount;
        }
        BigDecimal payable = orderAmount.subtract(discount);

        Map<String, Object> result = new HashMap<>();
        result.put("userCouponId", uc.getId());
        result.put("couponId", uc.getCouponId());
        result.put("orderAmount", orderAmount);
        result.put("discountAmount", discount);
        result.put("payableAmount", payable);
        return result;
    }

    /**
     * 下单选券后锁定券（待支付占用）：仅未使用券可被占用，原子条件更新防同一券被多单复用。
     */
    @Transactional
    public void reserve(Long userCouponId, Long orderId, Long userId) {
        UserCoupon uc = userCouponMapper.selectById(userCouponId);
        if (uc == null || !uc.getUserId().equals(userId)) {
            throw new BusinessException("优惠券不存在");
        }
        int rows = userCouponMapper.update(null, new UpdateWrapper<UserCoupon>()
                .eq("id", userCouponId)
                .eq("status", STATUS_UNUSED)
                .set("status", STATUS_RESERVED)
                .set("order_id", orderId));
        if (rows == 0) {
            throw new BusinessException("优惠券已被使用或占用，请重新选择");
        }
        log.info("coupon reserved: userCouponId={}, orderId={}", userCouponId, orderId);
    }

    /**
     * 支付成功时锁定券：兼容未使用/已锁定两种前置状态，原子条件更新防同一券被多单使用。
     */
    @Transactional
    public void markUsed(Long userCouponId, Long orderId, Long userId) {
        UserCoupon uc = userCouponMapper.selectById(userCouponId);
        if (uc == null || !uc.getUserId().equals(userId)) {
            throw new BusinessException("优惠券不存在");
        }
        if (uc.getStatus() != STATUS_UNUSED && uc.getStatus() != STATUS_RESERVED) {
            throw new BusinessException("优惠券不可用");
        }
        int rows = userCouponMapper.update(null, new UpdateWrapper<UserCoupon>()
                .eq("id", uc.getId())
                .in("status", STATUS_UNUSED, STATUS_RESERVED)
                .set("status", STATUS_USED)
                .set("order_id", orderId)
                .set("use_time", LocalDateTime.now()));
        if (rows == 0) {
            throw new BusinessException("优惠券已被使用，请重新下单");
        }
        log.info("coupon locked: userCouponId={}, orderId={}", userCouponId, orderId);
    }

    /**
     * 订单取消/退款后释放已使用券：未过期则退回未使用并解绑订单；已过期则置过期。
     * 仅处理绑定本订单、状态为已使用的券，避免误退他单券。
     */
    @Transactional
    public void releaseAfterCancel(Long userCouponId, Long orderId) {
        if (userCouponId == null) {
            return;
        }
        UserCoupon uc = userCouponMapper.selectById(userCouponId);
        if (uc == null || uc.getStatus() == null) {
            return;
        }
        if (orderId != null && !orderId.equals(uc.getOrderId())) {
            return;
        }
        // 待支付被占用的券：取消/超时后退回未使用并解绑订单
        if (uc.getStatus() == STATUS_RESERVED) {
            int reserved = userCouponMapper.update(null, new UpdateWrapper<UserCoupon>()
                    .eq("id", uc.getId())
                    .eq("status", STATUS_RESERVED)
                    .set("status", STATUS_UNUSED)
                    .set("order_id", null));
            if (reserved > 0) {
                log.info("reserved coupon released after cancel: userCouponId={}, orderId={}", userCouponId, orderId);
            }
            return;
        }
        if (uc.getStatus() != STATUS_USED) {
            return;
        }
        boolean expired = uc.getExpireTime() != null && uc.getExpireTime().isBefore(LocalDateTime.now());
        int target = expired ? STATUS_EXPIRED : STATUS_UNUSED;
        int rows = userCouponMapper.update(null, new UpdateWrapper<UserCoupon>()
                .eq("id", uc.getId())
                .eq("status", STATUS_USED)
                .set("status", target)
                .set("order_id", null)
                .set("use_time", null));
        if (rows > 0) {
            log.info("coupon released after cancel: userCouponId={}, orderId={}, expired={}", userCouponId, orderId, expired);
        }
    }

    /** 校验券可用于本次支付/下单，返回券记录；不满足则抛业务异常 */
    private UserCoupon requireUsable(Long userCouponId, Long userId) {        if (userCouponId == null) {
            throw new BusinessException("未选择优惠券");
        }
        UserCoupon uc = userCouponMapper.selectById(userCouponId);
        if (uc == null || !uc.getUserId().equals(userId)) {
            throw new BusinessException("优惠券不存在");
        }
        if (uc.getStatus() == null || uc.getStatus() != STATUS_UNUSED) {
            throw new BusinessException("优惠券当前不可用");
        }
        if (uc.getExpireTime() != null && uc.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("优惠券已过期");
        }
        return uc;
    }

    /** 把当前用户已到期仍未使用的券批量置为过期 */
    private void expireDueCoupons(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        userCouponMapper.update(null, new UpdateWrapper<UserCoupon>()
                .eq("user_id", userId)
                .eq("status", STATUS_UNUSED)
                .isNotNull("expire_time")
                .lt("expire_time", now)
                .set("status", STATUS_EXPIRED));
    }

    private Map<String, Object> toAvailableRow(CouponTemplate t, boolean claimed, Map<Long, String> catNames) {
        Map<String, Object> row = new HashMap<>();
        row.put("couponId", t.getId());
        row.put("name", t.getName());
        row.put("type", t.getType());
        row.put("discountAmount", t.getDiscountAmount());
        row.put("maxDiscountAmount", t.getMaxDiscountAmount());
        row.put("minAmount", t.getMinAmount());
        row.put("categoryId", t.getCategoryId());
        row.put("categoryName", catNames.get(t.getCategoryId()));
        row.put("totalCount", t.getTotalCount());
        row.put("claimedCount", t.getClaimedCount());
        row.put("remaining", Math.max(t.getTotalCount() - t.getClaimedCount(), 0));
        row.put("validStart", t.getValidStart());
        row.put("validEnd", t.getValidEnd());
        row.put("claimed", claimed);
        return row;
    }

    /** 收集某分类自身及全部祖先 ID（沿 parentId 上溯到顶级；防环、防深链兜底） */
    private Set<Long> ancestorChain(Long categoryId) {
        Set<Long> chain = new HashSet<>();
        Long cur = categoryId;
        int guard = 0;
        while (cur != null && cur > 0 && !chain.contains(cur) && guard++ < 10) {
            chain.add(cur);
            Category c = categoryMapper.selectById(cur);
            if (c == null || c.getParentId() == null || c.getParentId() == 0) {
                break;
            }
            cur = c.getParentId();
        }
        return chain;
    }

    /** 按分类 ID 集合批量取分类名，返回 id → name */
    private Map<Long, String> categoryNameMap(Set<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return new HashMap<>();
        }
        return categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
    }
}
