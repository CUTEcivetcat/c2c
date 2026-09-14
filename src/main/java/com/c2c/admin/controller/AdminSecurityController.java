package com.c2c.admin.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.c2c.common.constant.AdminPerm;
import com.c2c.common.constant.ApiPath;
import com.c2c.common.exception.BusinessException;
import com.c2c.common.result.R;
import com.c2c.favorite.entity.Favorite;
import com.c2c.favorite.mapper.FavoriteMapper;
import com.c2c.im.entity.Conversation;
import com.c2c.im.entity.Message;
import com.c2c.im.mapper.ConversationMapper;
import com.c2c.im.mapper.MessageMapper;
import com.c2c.order.entity.Order;
import com.c2c.order.mapper.OrderMapper;
import com.c2c.product.entity.Product;
import com.c2c.product.entity.ProductComment;
import com.c2c.product.entity.ProductImage;
import com.c2c.product.entity.ProductIntent;
import com.c2c.product.mapper.ProductCommentMapper;
import com.c2c.product.mapper.ProductImageMapper;
import com.c2c.product.mapper.ProductIntentMapper;
import com.c2c.product.mapper.ProductMapper;
import com.c2c.rating.entity.Rating;
import com.c2c.rating.mapper.RatingMapper;
import com.c2c.review.entity.AdminLog;
import com.c2c.review.entity.NicknameAudit;
import com.c2c.review.entity.ProductAppeal;
import com.c2c.review.entity.Report;
import com.c2c.review.mapper.AdminLogMapper;
import com.c2c.review.mapper.NicknameAuditMapper;
import com.c2c.review.mapper.ProductAppealMapper;
import com.c2c.review.mapper.ReportMapper;
import com.c2c.user.entity.User;
import com.c2c.user.entity.UserAddress;
import com.c2c.user.mapper.UserAddressMapper;
import com.c2c.user.mapper.UserMapper;
import com.c2c.wallet.entity.WalletLog;
import com.c2c.wallet.mapper.WalletLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端安全能力：二级密码 + 次级管理员 + 删除账户。
 *
 * <p>权限模型：管理员账号在 user 表（role=1），并按 admin_level 分级：
 * <ul>
 *   <li>admin_level=2 总管理员（username=admin，母管理）：最高权限，可配置次级/子级管理员并赋权。</li>
 *   <li>admin_level=1 次级/子级管理员：拥有 admin_perms 中勾选的模块权限。</li>
 *   <li>审核员（role=2）：最低级别，仅可进入用户端审核工作台，不能登录管理端口。</li>
 *   <li>所有管理员均可设置/修改自己的二级密码（删除账户等高危操作必须输入）。</li>
 *   <li>仅总管理员可查看/添加/删除/赋权次级与子级管理员。</li>
 *   <li>删除用户账户必须输入当前管理员的二级密码，且目标不能是管理员/审核员，不能有进行中的订单。</li>
 * </ul>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "管理端安全", description = "二级密码设置 / 次级管理员管理 / 删除账户（高危操作需二级密码，/admin/** 需管理员 token）")
public class AdminSecurityController {

    private final UserMapper userMapper;
    private final UserAddressMapper userAddressMapper;
    private final FavoriteMapper favoriteMapper;
    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final ProductCommentMapper productCommentMapper;
    private final ProductIntentMapper productIntentMapper;
    private final ProductAppealMapper productAppealMapper;
    private final ReportMapper reportMapper;
    private final RatingMapper ratingMapper;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final NicknameAuditMapper nicknameAuditMapper;
    private final WalletLogMapper walletLogMapper;
    private final OrderMapper orderMapper;
    private final AdminLogMapper adminLogMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ==================== 二级密码 ====================

    /** 设置 / 修改当前管理员的二级密码（所有管理员可用）。 */
    @Operation(summary = "设置/修改我的二级密码", description = "body：password（≥6 位），BCrypt 加密存储")
    @PostMapping(ApiPath.ADMIN_AUTH_SECONDARY_PASSWORD)
    public R<Void> setSecondaryPassword(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long operatorId,
                                        @RequestBody Map<String, String> body) {
        String pwd = body.get("password");
        if (StrUtil.isBlank(pwd) || pwd.length() < 6) {
            throw new BusinessException("二级密码至少 6 位");
        }
        User me = userMapper.selectById(operatorId);
        if (me == null) {
            throw new BusinessException("管理员账号不存在");
        }
        me.setSecondaryPwd(passwordEncoder.encode(pwd));
        userMapper.updateById(me);
        log.info("管理员设置二级密码：operatorId={}", operatorId);
        return R.ok();
    }

    // ==================== 次级管理员（仅超级管理员 admin） ====================

    /** 次级/子级管理员列表。 */
    @Operation(summary = "管理员列表（次级/子级）", description = "仅总管理员（admin_level=2）可查看；隐藏密码字段，返回权限码")
    @GetMapping(ApiPath.ADMIN_AUTH_SECONDARY_ADMINS)
    public R<List<Map<String, Object>>> secondaryAdmins(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long operatorId) {
        User me = requireTopAdmin(operatorId);
        List<User> list = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRole, 1)
                .eq(User::getAdminLevel, 1)
                .orderByAsc(User::getId));
        return R.ok(list.stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("nickname", u.getNickname());
            m.put("phone", u.getPhone());
            m.put("email", u.getEmail());
            m.put("status", u.getStatus());
            m.put("adminLevel", u.getAdminLevel() == null ? 0 : u.getAdminLevel());
            m.put("adminPerms", u.getAdminPerms());
            m.put("hasSecondaryPwd", StrUtil.isNotBlank(u.getSecondaryPwd()));
            m.put("createdAt", u.getCreatedAt());
            return m;
        }).collect(Collectors.toList()));
    }

    /** 添加次级/子级管理员：用户名 + 登录密码 + 二级密码 + 权限码（仅总管理员）。 */
    @Operation(summary = "添加次级/子级管理员", description = "仅总管理员；body：username / password / secondaryPassword / perms(逗号分隔权限码)")
    @PostMapping(ApiPath.ADMIN_AUTH_SECONDARY_ADMINS)
    public R<Void> addSecondaryAdmin(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long operatorId,
                                     @RequestBody Map<String, Object> body) {
        User me = requireTopAdmin(operatorId);
        String username = StrUtil.trimToNull(StrUtil.toString(body.get("username")));
        String password = StrUtil.toString(body.get("password"));
        String secondaryPassword = StrUtil.toString(body.get("secondaryPassword"));
        String perms = normalizePerms(body.get("perms"));
        if (username == null || username.length() < 3 || username.length() > 30) {
            throw new BusinessException("用户名需 3~30 个字符");
        }
        if (!username.matches("[a-zA-Z0-9_]+")) {
            throw new BusinessException("用户名仅支持字母、数字、下划线");
        }
        if (StrUtil.isBlank(password) || password.length() < 6) {
            throw new BusinessException("登录密码至少 6 位");
        }
        if (StrUtil.isBlank(secondaryPassword) || secondaryPassword.length() < 6) {
            throw new BusinessException("二级密码至少 6 位");
        }
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (exists != null && exists > 0) {
            throw new BusinessException("该用户名已存在");
        }
        User admin = new User();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setNickname(username);
        admin.setRole(1);
        admin.setAdminLevel(1);
        admin.setAdminPerms(perms);
        admin.setStatus(1);
        admin.setSecondaryPwd(passwordEncoder.encode(secondaryPassword));
        admin.setLoginSource("admin");
        admin.setReputationScore(new java.math.BigDecimal("5.0"));
        userMapper.insert(admin);

        writeLog(operatorId, "add_secondary_admin", "user", admin.getId(),
                "新增管理员：" + username + "，权限：" + perms + "（操作人：" + me.getUsername() + "）");
        log.info("新增管理员：operatorId={}, username={}, perms={}", operatorId, username, perms);
        return R.ok();
    }

    /** 更新次级/子级管理员：调整权限码 / 重置登录密码 / 重置二级密码（仅总管理员）。 */
    @Operation(summary = "更新次级/子级管理员", description = "仅总管理员；body 可选：perms(权限码) / password(重置登录密码) / secondaryPassword(重置二级密码)")
    @PutMapping(ApiPath.ADMIN_AUTH_SECONDARY_ADMIN_ID)
    public R<Void> updateSecondaryAdmin(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long operatorId,
                                        @Parameter(description = "目标管理员用户 ID") @PathVariable Long id,
                                        @RequestBody Map<String, Object> body) {
        User me = requireTopAdmin(operatorId);
        if (id.equals(operatorId)) {
            throw new BusinessException("不能修改自己的权限");
        }
        User target = userMapper.selectById(id);
        if (target == null || target.getRole() == null || target.getRole() != 1
                || target.getAdminLevel() == null || target.getAdminLevel() != 1) {
            throw new BusinessException("目标不是次级/子级管理员");
        }
        StringBuilder detail = new StringBuilder("更新管理员：" + target.getUsername());
        if (body.containsKey("perms")) {
            String perms = normalizePerms(body.get("perms"));
            target.setAdminPerms(perms);
            detail.append("；权限→").append(perms);
        }
        String password = StrUtil.toString(body.get("password"));
        if (StrUtil.isNotBlank(password)) {
            if (password.length() < 6) {
                throw new BusinessException("登录密码至少 6 位");
            }
            target.setPassword(passwordEncoder.encode(password));
            detail.append("；重置登录密码");
        }
        String secondaryPassword = StrUtil.toString(body.get("secondaryPassword"));
        if (StrUtil.isNotBlank(secondaryPassword)) {
            if (secondaryPassword.length() < 6) {
                throw new BusinessException("二级密码至少 6 位");
            }
            target.setSecondaryPwd(passwordEncoder.encode(secondaryPassword));
            detail.append("；重置二级密码");
        }
        userMapper.updateById(target);
        writeLog(operatorId, "update_secondary_admin", "user", id, detail.toString() + "（操作人：" + me.getUsername() + "）");
        log.info("更新管理员：operatorId={}, targetId={}, detail={}", operatorId, id, detail);
        return R.ok();
    }

    /** 删除次级/子级管理员。 */
    @Operation(summary = "删除次级/子级管理员", description = "仅总管理员；禁止删除总管理员与自身")
    @DeleteMapping(ApiPath.ADMIN_AUTH_SECONDARY_ADMIN_ID)
    public R<Void> deleteSecondaryAdmin(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long operatorId,
                                        @Parameter(description = "目标管理员用户 ID") @PathVariable Long id) {
        User me = requireTopAdmin(operatorId);
        if (id.equals(operatorId)) {
            throw new BusinessException("不能删除自己");
        }
        User target = userMapper.selectById(id);
        if (target == null || target.getRole() == null || target.getRole() != 1
                || target.getAdminLevel() == null || target.getAdminLevel() != 1) {
            throw new BusinessException("目标不是次级/子级管理员");
        }
        userMapper.deleteById(id);
        writeLog(operatorId, "delete_secondary_admin", "user", id,
                "删除管理员：" + target.getUsername() + "（操作人：" + me.getUsername() + "）");
        log.info("删除管理员：operatorId={}, targetId={}", operatorId, id);
        return R.ok();
    }

    // ==================== 删除用户账户（需二级密码） ====================

    /** 删除用户账户：需输入当前管理员的二级密码；级联清理该用户的商品/收藏/地址/评价/意向/会话等关联数据。 */
    @Operation(summary = "删除用户账户", description = "需输入当前管理员的二级密码；目标不能是管理员/审核员，且不能有进行中的订单")
    @DeleteMapping(ApiPath.ADMIN_USER_DELETE)
    public R<Void> deleteUser(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long operatorId,
                              @Parameter(description = "目标用户 ID") @PathVariable Long userId,
                              @RequestBody Map<String, String> body) {
        User me = userMapper.selectById(operatorId);
        if (me == null) {
            throw new BusinessException("管理员账号不存在");
        }
        verifySecondaryPassword(me, body == null ? null : body.get("secondaryPassword"));

        User target = userMapper.selectById(userId);
        if (target == null) {
            throw new BusinessException("用户不存在");
        }
        if (target.getRole() != null && target.getRole() > 0) {
            throw new BusinessException("不能删除管理员/审核员账号");
        }
        if (userId.equals(operatorId)) {
            throw new BusinessException("不能删除当前登录的账户");
        }
        // 进行中的订单（待支付/待发货/待收货/已收货）不允许删除，防止交易数据悬空
        Long activeOrders = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .and(w -> w.eq(Order::getBuyerId, userId).or().eq(Order::getSellerId, userId))
                .in(Order::getStatus, 0, 1, 2, 3));
        if (activeOrders != null && activeOrders > 0) {
            throw new BusinessException("该用户存在进行中的订单，请先完成或取消相关订单后再删除");
        }

        // ---- 级联清理（表间无外键，手动删除） ----
        userAddressMapper.delete(new LambdaQueryWrapper<UserAddress>().eq(UserAddress::getUserId, userId));
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, userId));
        productCommentMapper.delete(new LambdaQueryWrapper<ProductComment>().eq(ProductComment::getUserId, userId));
        productIntentMapper.delete(new LambdaQueryWrapper<ProductIntent>()
                .and(w -> w.eq(ProductIntent::getBuyerId, userId)
                        .or().eq(ProductIntent::getSellerId, userId)));
        productAppealMapper.delete(new LambdaQueryWrapper<ProductAppeal>().eq(ProductAppeal::getSellerId, userId));
        reportMapper.delete(new LambdaQueryWrapper<Report>().eq(Report::getReporterId, userId));
        ratingMapper.delete(new LambdaQueryWrapper<Rating>()
                .and(w -> w.eq(Rating::getRaterId, userId).or().eq(Rating::getRatedUserId, userId)));
        nicknameAuditMapper.delete(new LambdaQueryWrapper<NicknameAudit>().eq(NicknameAudit::getUserId, userId));
        walletLogMapper.delete(new LambdaQueryWrapper<WalletLog>().eq(WalletLog::getUserId, userId));

        // 用户发布的商品 + 商品图片
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>().eq(Product::getSellerId, userId));
        for (Product p : products) {
            productImageMapper.delete(new LambdaQueryWrapper<ProductImage>().eq(ProductImage::getProductId, p.getId()));
            productMapper.deleteById(p.getId());
        }
        // 会话 + 消息
        List<Conversation> convs = conversationMapper.selectList(new LambdaQueryWrapper<Conversation>()
                .and(w -> w.eq(Conversation::getUser1Id, userId).or().eq(Conversation::getUser2Id, userId)));
        for (Conversation c : convs) {
            messageMapper.delete(new LambdaQueryWrapper<Message>().eq(Message::getConversationId, c.getId()));
            conversationMapper.deleteById(c.getId());
        }

        userMapper.deleteById(userId);
        writeLog(operatorId, "delete_user", "user", userId,
                "删除用户：" + (StrUtil.isNotBlank(target.getUsername()) ? target.getUsername() : target.getId()) + "（操作人：" + me.getUsername() + "）");
        log.info("删除用户账户：operatorId={}, targetId={}", operatorId, userId);
        return R.ok();
    }

    // ==================== 内部工具 ====================

    /** 校验当前管理员必须是总管理员（admin_level=2；username=admin 兜底，防止漏迁移时误拦）。 */
    private User requireTopAdmin(Long operatorId) {
        User me = userMapper.selectById(operatorId);
        boolean isTop = me != null && me.getRole() != null && me.getRole() == 1
                && (("admin".equals(me.getUsername()))
                    || (me.getAdminLevel() != null && me.getAdminLevel() == 2));
        if (!isTop) {
            throw new BusinessException(403, "仅总管理员（admin）可执行该操作");
        }
        return me;
    }

    /** 规范化权限码：List / 逗号串 → 合法权限码逗号串（过滤未知码，去重保序）。 */
    private String normalizePerms(Object permsObj) {
        java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>();
        if (permsObj instanceof List) {
            for (Object o : (List<?>) permsObj) {
                collectPerm(o, set);
            }
        } else {
            collectPerm(permsObj, set);
        }
        return String.join(",", set);
    }

    private void collectPerm(Object o, java.util.LinkedHashSet<String> set) {
        if (o == null) return;
        String raw = String.valueOf(o);
        if (raw.contains(",")) {
            for (String p : raw.split(",")) {
                addIfValid(p, set);
            }
        } else {
            addIfValid(raw, set);
        }
    }

    private void addIfValid(String p, java.util.LinkedHashSet<String> set) {
        String t = p.trim();
        if (t.isEmpty()) return;
        for (String valid : AdminPerm.ASSIGNABLE) {
            if (valid.equals(t)) {
                set.add(t);
                return;
            }
        }
    }

    /** 校验当前管理员的二级密码（未设置 / 错误均拒绝）。 */
    private void verifySecondaryPassword(User me, String input) {
        if (StrUtil.isBlank(me.getSecondaryPwd())) {
            throw new BusinessException("当前管理员尚未设置二级密码，请先到「安全设置」中设置后再执行该操作");
        }
        if (StrUtil.isBlank(input) || !passwordEncoder.matches(input, me.getSecondaryPwd())) {
            throw new BusinessException("二级密码错误");
        }
    }

    private void writeLog(Long operatorId, String action, String targetType, Long targetId, String detail) {
        AdminLog adminLog = new AdminLog();
        adminLog.setOperatorId(operatorId);
        adminLog.setOperatorRole(1);
        adminLog.setAction(action);
        adminLog.setTargetType(targetType);
        adminLog.setTargetId(targetId);
        adminLog.setDetail(detail);
        adminLogMapper.insert(adminLog);
    }
}
