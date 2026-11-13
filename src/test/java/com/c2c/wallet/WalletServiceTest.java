package com.c2c.wallet;

import com.c2c.common.exception.BusinessException;
import com.c2c.user.entity.User;
import com.c2c.user.mapper.UserMapper;
import com.c2c.wallet.entity.BankCard;
import com.c2c.wallet.mapper.WalletLogMapper;
import com.c2c.wallet.service.BankCardService;
import com.c2c.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private BankCardService bankCardService;
    @InjectMocks
    private WalletService walletService;

    @org.junit.jupiter.api.BeforeAll
    static void header() {
        com.c2c.common.infra.TestLog.log("[测试类] 钱包服务：充值加余额写流水 / 负数拒绝 / 余额不足不扣款不写流水 / 收款入账");
    }

    private User userWithBalance(String balance) {
        User user = new User();
        user.setId(1L);
        user.setBalance(new BigDecimal(balance));
        return user;
    }

    @Test
    void recharge_increasesBalance_andWritesLog() {
        com.c2c.common.infra.TestLog.log("[测试] 充值100：调用原子加余额 + 写入流水(before=0, after=100)");
        User user = userWithBalance("0");
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.addBalance(1L, new BigDecimal("100"))).thenReturn(1);

        walletService.recharge(1L, new BigDecimal("100"));

        verify(userMapper).addBalance(1L, new BigDecimal("100"));
        verify(walletLogMapper).insert(any());
    }

    @Test
    void recharge_negativeAmount_throws() {
        com.c2c.common.infra.TestLog.log("[测试] 充值校验：-5元 和 0元 都应被拒绝，且不触碰余额");
        assertThrows(BusinessException.class, () -> walletService.recharge(1L, new BigDecimal("-5")));
        assertThrows(BusinessException.class, () -> walletService.recharge(1L, BigDecimal.ZERO));
        verify(userMapper, never()).addBalance(any(), any());
    }

    @Test
    void deductBalance_insufficient_throws() {
        com.c2c.common.infra.TestLog.log("[测试] 余额不足：余额10扣20，原子扣减返回0行 → 抛异常且不写流水");
        User user = userWithBalance("10");
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.deductBalance(1L, new BigDecimal("20"))).thenReturn(0);

        assertThrows(BusinessException.class,
                () -> walletService.deductBalance(1L, new BigDecimal("20"), 1L, "支付"));
        verify(walletLogMapper, never()).insert(any());
    }

    @Test
    void deductBalance_sufficient_deducts() {
        com.c2c.common.infra.TestLog.log("[测试] 正常扣款：余额100扣30，原子扣减成功 + 写流水");
        User user = userWithBalance("100");
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.deductBalance(1L, new BigDecimal("30"))).thenReturn(1);

        walletService.deductBalance(1L, new BigDecimal("30"), 1L, "支付");

        verify(userMapper).deductBalance(1L, new BigDecimal("30"));
        verify(walletLogMapper).insert(any());
    }

    @Test
    void receive_addsToSellerBalance() {
        com.c2c.common.infra.TestLog.log("[测试] 收款：卖家余额50收30 → 原子加余额 + 写流水");
        User seller = userWithBalance("50");
        when(userMapper.selectById(1L)).thenReturn(seller);
        when(userMapper.addBalance(1L, new BigDecimal("30"))).thenReturn(1);

        walletService.receive(1L, new BigDecimal("30"), 1L, "订单收款");

        verify(userMapper).addBalance(1L, new BigDecimal("30"));
        verify(walletLogMapper).insert(any());
    }

    private BankCard defaultCard() {
        BankCard card = new BankCard();
        card.setId(1L);
        card.setCardHolder("小王");
        card.setCardNo("6222000000001234");
        return card;
    }

    @Test
    void withdraw_noBoundCard_throws() {
        com.c2c.common.infra.TestLog.log("[测试] 提现未绑卡：拦截提示先绑卡，不扣余额不写流水");
        when(bankCardService.requireBoundCard(1L)).thenThrow(new BusinessException("请先绑定演示银行卡"));
        assertThrows(BusinessException.class, () -> walletService.withdraw(1L, new BigDecimal("10"), null));
        verify(userMapper, never()).deductBalance(any(), any());
    }

    @Test
    void withdraw_insufficient_throws() {
        com.c2c.common.infra.TestLog.log("[测试] 提现余额不足：原子扣减返回0行 → 抛异常不写流水");
        User user = userWithBalance("10");
        when(bankCardService.requireBoundCard(1L)).thenReturn(defaultCard());
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.deductBalance(1L, new BigDecimal("20"))).thenReturn(0);
        assertThrows(BusinessException.class, () -> walletService.withdraw(1L, new BigDecimal("20"), null));
        verify(walletLogMapper, never()).insert(any());
    }

    @Test
    void grantBonus_addsBalance_andWritesLog() {
        com.c2c.common.infra.TestLog.log("[测试] 余额券补贴：加余额 + 写 bonus 流水");
        User user = userWithBalance("0");
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.addBalance(1L, new BigDecimal("20"))).thenReturn(1);
        walletService.grantBonus(1L, new BigDecimal("20"), null, "余额券到账");
        verify(userMapper).addBalance(1L, new BigDecimal("20"));
        verify(walletLogMapper).insert(any());
    }

    @Test
    void rechargeByTicket_invalidTicket_throws() {
        com.c2c.common.infra.TestLog.log("[测试] 票据失效：Redis 无票据 → 拒绝充值，不加余额");
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenReturn(null);
        assertThrows(BusinessException.class, () -> walletService.rechargeByTicket("bad-ticket", new BigDecimal("10")));
        verify(userMapper, never()).addBalance(any(), any());
    }
}
