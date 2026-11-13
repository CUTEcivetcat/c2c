package com.c2c.wallet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.exception.BusinessException;
import com.c2c.user.entity.User;
import com.c2c.user.mapper.UserMapper;
import com.c2c.wallet.entity.BankCard;
import com.c2c.wallet.entity.WalletLog;
import com.c2c.wallet.mapper.BankCardMapper;
import com.c2c.wallet.mapper.WalletLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 钱包服务：余额充值、查询概况、资金流水记录。
 * <p>余额增减全部走 UserMapper 的原子 SQL（条件更新），避免并发超扣/竞态。</p>
 */
@Service
@RequiredArgsConstructor
public class WalletService {

    private final UserMapper userMapper;
    private final WalletLogMapper walletLogMapper;
    private final StringRedisTemplate redisTemplate;
    private final BankCardService bankCardService;

    /** 充值二维码票据前缀；票据短期有效、一次性，绑定用户 ID */
    private static final String TICKET_PREFIX = "wallet:recharge:ticket:";
    /** 票据有效期（分钟） */
    private static final Duration TICKET_TTL = Duration.ofMinutes(5);

    /** 模拟充值（直接加余额） */
    @Transactional
    public void recharge(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("充值金额必须大于 0");
        }
        if (amount.compareTo(new BigDecimal("99999")) > 0) {
            throw new BusinessException("单次充值不能超过 99999 元");
        }
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");

        BigDecimal before = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        BigDecimal after = before.add(amount);
        userMapper.addBalance(userId, amount);

        WalletLog log = new WalletLog();
        log.setUserId(userId);
        log.setType("recharge");
        log.setAmount(amount);
        log.setBalanceBefore(before);
        log.setBalanceAfter(after);
        log.setRemark("模拟充值 ¥" + amount);
        walletLogMapper.insert(log);
    }

    /** 银行卡方式模拟充值：选默认/指定演示卡，加余额并记流水 */
    @Transactional
    public void rechargeByCard(Long userId, BigDecimal amount, Long cardId) {
        checkAmount(amount);
        BankCard card = cardId == null ? bankCardService.requireBoundCard(userId) : requireOwnedCard(userId, cardId);
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        BigDecimal before = nz(user.getBalance());
        BigDecimal after = before.add(amount);
        userMapper.addBalance(userId, amount);
        walletLogMapper.insert(baseLog(userId, "recharge", amount, before, after, null,
                "银行卡模拟充值 ¥" + amount + "（尾号 " + last4(card.getCardNo()) + "）"));
    }

    /** 生成充值二维码票据：返回一次性短期 token，H5 凭 token 充值 */
    public String createRechargeTicket(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(TICKET_PREFIX + token, String.valueOf(userId), TICKET_TTL);
        return token;
    }

    /** H5 凭票据充值：校验并一次性消费票据，加余额记扫码充值流水 */
    @Transactional
    public void rechargeByTicket(String ticket, BigDecimal amount) {
        checkAmount(amount);
        if (ticket == null || ticket.trim().isEmpty()) {
            throw new BusinessException("充值票据无效");
        }
        String key = TICKET_PREFIX + ticket.trim();
        String uidStr = redisTemplate.opsForValue().get(key);
        if (uidStr == null) {
            throw new BusinessException("充值票据已失效，请重新扫码");
        }
        redisTemplate.delete(key);
        Long userId = Long.valueOf(uidStr);
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        BigDecimal before = nz(user.getBalance());
        BigDecimal after = before.add(amount);
        userMapper.addBalance(userId, amount);
        walletLogMapper.insert(baseLog(userId, "recharge", amount, before, after, null,
                "扫码充值 ¥" + amount));
    }

    /** 模拟提现：扣余额到默认演示卡，未绑卡/余额不足拦截，记 withdraw 流水 */
    @Transactional
    public void withdraw(Long userId, BigDecimal amount, Long cardId) {
        checkAmount(amount);
        BankCard card = cardId == null ? bankCardService.requireBoundCard(userId) : requireOwnedCard(userId, cardId);
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        BigDecimal before = nz(user.getBalance());
        int rows = userMapper.deductBalance(userId, amount);
        if (rows == 0) {
            throw new BusinessException("余额不足");
        }
        BigDecimal after = before.subtract(amount);
        walletLogMapper.insert(baseLog(userId, "withdraw", amount.negate(), before, after, null,
                "提现到演示卡（尾号 " + last4(card.getCardNo()) + "）¥" + amount));
    }

    /** 余额券/活动补贴：直接加余额，记 bonus 流水（供券服务调用） */
    @Transactional
    public void grantBonus(Long userId, BigDecimal amount, Long orderId, String remark) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("补贴金额必须大于 0");
        }
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        BigDecimal before = nz(user.getBalance());
        BigDecimal after = before.add(amount);
        userMapper.addBalance(userId, amount);
        walletLogMapper.insert(baseLog(userId, "bonus", amount, before, after, orderId, remark));
    }

    private BankCard requireOwnedCard(Long userId, Long cardId) {
        return bankCardService.list(userId).stream()
                .filter(c -> c.getId().equals(cardId)).findFirst()
                .orElseThrow(() -> new BusinessException("银行卡不存在"));
    }

    private void checkAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("金额必须大于 0");
        }
        if (amount.compareTo(new BigDecimal("99999")) > 0) {
            throw new BusinessException("单笔金额不能超过 99999 元");
        }
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private String last4(String cardNo) {
        return cardNo != null && cardNo.length() >= 4 ? cardNo.substring(cardNo.length() - 4) : cardNo;
    }

    private WalletLog baseLog(Long userId, String type, BigDecimal amount, BigDecimal before,
                              BigDecimal after, Long orderId, String remark) {
        WalletLog log = new WalletLog();
        log.setUserId(userId);
        log.setType(type);
        log.setAmount(amount);
        log.setBalanceBefore(before);
        log.setBalanceAfter(after);
        log.setOrderId(orderId);
        log.setRemark(remark);
        return log;
    }

    /** 钱包概况（余额 + 最近流水） */
    public Map<String, Object> getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        BigDecimal balance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        Page<WalletLog> page = walletLogMapper.selectPage(new Page<>(1, 20),
                new LambdaQueryWrapper<WalletLog>()
                        .eq(WalletLog::getUserId, userId)
                        .orderByDesc(WalletLog::getId));
        Map<String, Object> result = new HashMap<>();
        result.put("balance", balance);
        result.put("logs", page.getRecords());
        return result;
    }

    /** 扣余额（供 OrderService 调用）。原子条件扣减，余额不足抛异常。 */
    @Transactional
    public void deductBalance(Long userId, BigDecimal amount, Long orderId, String remark) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        BigDecimal before = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;

        int rows = userMapper.deductBalance(userId, amount);
        if (rows == 0) {
            throw new BusinessException("余额不足");
        }
        BigDecimal after = before.subtract(amount);

        WalletLog log = new WalletLog();
        log.setUserId(userId);
        log.setType("pay");
        log.setAmount(amount.negate());
        log.setBalanceBefore(before);
        log.setBalanceAfter(after);
        log.setOrderId(orderId);
        log.setRemark(remark);
        walletLogMapper.insert(log);
    }

    /** 退款（取消订单时退回买家） */
    @Transactional
    public void refund(Long userId, BigDecimal amount, Long orderId, String remark) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        BigDecimal before = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        BigDecimal after = before.add(amount);
        userMapper.addBalance(userId, amount);

        WalletLog log = new WalletLog();
        log.setUserId(userId);
        log.setType("refund");
        log.setAmount(amount);
        log.setBalanceBefore(before);
        log.setBalanceAfter(after);
        log.setOrderId(orderId);
        log.setRemark(remark);
        walletLogMapper.insert(log);
    }

    /** 收款（确认收货时打款给卖家） */
    @Transactional
    public void receive(Long userId, BigDecimal amount, Long orderId, String remark) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        BigDecimal before = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        BigDecimal after = before.add(amount);
        userMapper.addBalance(userId, amount);

        WalletLog log = new WalletLog();
        log.setUserId(userId);
        log.setType("receive");
        log.setAmount(amount);
        log.setBalanceBefore(before);
        log.setBalanceAfter(after);
        log.setOrderId(orderId);
        log.setRemark(remark);
        walletLogMapper.insert(log);
    }
}
