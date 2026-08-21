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

        walletService.recharge(1L, new BigDecimal("100"));

        assertEquals(new BigDecimal("100"), user.getBalance());
        verify(walletLogMapper).insert(any());
    }

    @Test
    void recharge_negativeAmount_throws() {
        assertThrows(BusinessException.class, () -> walletService.recharge(1L, new BigDecimal("-5")));
        assertThrows(BusinessException.class, () -> walletService.recharge(1L, BigDecimal.ZERO));
    }

    @Test
    void deductBalance_insufficient_throws() {
        User user = userWithBalance("10");
        when(userMapper.selectById(1L)).thenReturn(user);

        assertThrows(BusinessException.class,
                () -> walletService.deductBalance(1L, new BigDecimal("20"), 1L, "支付"));
        // 余额未被修改
        assertEquals(new BigDecimal("10"), user.getBalance());
    }

    @Test
    void deductBalance_sufficient_deducts() {
        User user = userWithBalance("100");
        when(userMapper.selectById(1L)).thenReturn(user);

        walletService.deductBalance(1L, new BigDecimal("30"), 1L, "支付");

        assertEquals(new BigDecimal("70"), user.getBalance());
        verify(walletLogMapper).insert(any());
    }

    @Test
    void receive_addsToSellerBalance() {
        User seller = userWithBalance("50");
        when(userMapper.selectById(1L)).thenReturn(seller);

        walletService.receive(1L, new BigDecimal("30"), 1L, "订单收款");

        assertEquals(new BigDecimal("80"), seller.getBalance());
        verify(walletLogMapper).insert(any());
    }
}
