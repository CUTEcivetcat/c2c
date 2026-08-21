package com.c2c.wallet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.exception.BusinessException;
import com.c2c.common.utils.MapUtils;
import com.c2c.user.entity.User;
import com.c2c.user.mapper.UserMapper;
import com.c2c.wallet.entity.WalletLog;
import com.c2c.wallet.mapper.WalletLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 钱包服务：余额充值、查询概况、资金流水记录。
 */
@Service
@RequiredArgsConstructor
public class WalletService {

    private final UserMapper userMapper;
    private final WalletLogMapper walletLogMapper;

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
        user.setBalance(after);
        userMapper.updateById(user);

        WalletLog log = new WalletLog();
        log.setUserId(userId);
        log.setType("recharge");
        log.setAmount(amount);
        log.setBalanceBefore(before);
        log.setBalanceAfter(after);
        log.setRemark("模拟充值 ¥" + amount);
        walletLogMapper.insert(log);
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

    /** 扣余额（供 OrderService 调用） */
    @Transactional
    public void deductBalance(Long userId, BigDecimal amount, Long orderId, String remark) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        BigDecimal before = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        if (before.compareTo(amount) < 0) throw new BusinessException("余额不足");
        BigDecimal after = before.subtract(amount);
        user.setBalance(after);
        userMapper.updateById(user);

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
        user.setBalance(after);
        userMapper.updateById(user);

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
        user.setBalance(after);
        userMapper.updateById(user);

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