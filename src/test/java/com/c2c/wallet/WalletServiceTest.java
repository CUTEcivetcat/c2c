package com.c2c.wallet;

import com.c2c.common.exception.BusinessException;
import com.c2c.user.entity.User;
import com.c2c.user.mapper.UserMapper;
import com.c2c.wallet.mapper.WalletLogMapper;
import com.c2c.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 钱包服务单元测试：充值、扣款（余额不足）、收款。
 *
 * <p>余额增减走 UserMapper 的原子 SQL（addBalance / deductBalance），
 * 单元测试验证原子方法被调用、返回值被正确处理、流水被写入；余额数值的最终落库由 SQL 保证。</p>
 */
@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private WalletLogMapper walletLogMapper;
    @InjectMocks
    private WalletService walletService;

    private User userWithBalance(String balance) {
        User user = new User();
        user.setId(1L);
        user.setBalance(new BigDecimal(balance));
        return user;
    }

    @Test
    void recharge_increasesBalance_andWritesLog() {
        User user = userWithBalance("0");
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.addBalance(1L, new BigDecimal("100"))).thenReturn(1);

        walletService.recharge(1L, new BigDecimal("100"));

        // 原子加余额方法被调用
        verify(userMapper).addBalance(1L, new BigDecimal("100"));
        // 流水写入（before=0, after=100）
        verify(walletLogMapper).insert(any());
    }

    @Test
    void recharge_negativeAmount_throws() {
        assertThrows(BusinessException.class, () -> walletService.recharge(1L, new BigDecimal("-5")));
        assertThrows(BusinessException.class, () -> walletService.recharge(1L, BigDecimal.ZERO));
        verify(userMapper, never()).addBalance(any(), any());
    }

    @Test
    void deductBalance_insufficient_throws() {
        User user = userWithBalance("10");
        when(userMapper.selectById(1L)).thenReturn(user);
        // 原子扣减返回 0 行 = 余额不足（并发安全由 SQL 条件保证）
        when(userMapper.deductBalance(1L, new BigDecimal("20"))).thenReturn(0);

        assertThrows(BusinessException.class,
                () -> walletService.deductBalance(1L, new BigDecimal("20"), 1L, "支付"));
        // 不写流水
        verify(walletLogMapper, never()).insert(any());
    }

    @Test
    void deductBalance_sufficient_deducts() {
        User user = userWithBalance("100");
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.deductBalance(1L, new BigDecimal("30"))).thenReturn(1);

        walletService.deductBalance(1L, new BigDecimal("30"), 1L, "支付");

        verify(userMapper).deductBalance(1L, new BigDecimal("30"));
        verify(walletLogMapper).insert(any());
    }

    @Test
    void receive_addsToSellerBalance() {
        User seller = userWithBalance("50");
        when(userMapper.selectById(1L)).thenReturn(seller);
        when(userMapper.addBalance(1L, new BigDecimal("30"))).thenReturn(1);

        walletService.receive(1L, new BigDecimal("30"), 1L, "订单收款");

        verify(userMapper).addBalance(1L, new BigDecimal("30"));
        verify(walletLogMapper).insert(any());
    }
}
