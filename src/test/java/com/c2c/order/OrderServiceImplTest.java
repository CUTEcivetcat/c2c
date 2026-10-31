package com.c2c.order;

import com.c2c.common.exception.BusinessException;
import com.c2c.order.entity.Order;
import com.c2c.order.feign.ProductFeignClient;
import com.c2c.order.feign.UserFeignClient;
import com.c2c.order.mapper.OrderMapper;
import com.c2c.order.mq.OrderEventPublisher;
import com.c2c.order.service.impl.OrderServiceImpl;
import com.c2c.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 订单状态机单元测试（M2：支付改托管后的真实流转）。
 *
 * <p>状态机：0 待支付 --pay--> 1 已支付(escrow) --ship(卖家)--> 2 已发货 --receive(买家)--> 4 已完成(打款卖家)
 * 取消：0 直接取消 / 1 退款取消，均恢复商品在售。</p>
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private ProductFeignClient productFeignClient;
    @Mock
    private UserFeignClient userFeignClient;
    @Mock
    private OrderEventPublisher eventPublisher;
    @Mock
    private WalletService walletService;
    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order orderWithStatus(int status) {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("202610310000000001");
        order.setBuyerId(2L);
        order.setSellerId(1L);
        order.setProductId(9L);
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setStatus(status);
        return order;
    }

    @Test
    void pay_status0_deductsIntoEscrow() {
        Order order = orderWithStatus(0);
        when(orderMapper.selectById(1L)).thenReturn(order);

        orderService.pay(1L, 2L);

        verify(walletService).deductBalance(eq(2L), eq(new BigDecimal("100.00")), eq(1L), contains("订单支付"));
        assertEquals(1, order.getStatus(), "支付后应为 1 已支付(托管)");
        assertEquals(new BigDecimal("100.00"), order.getEscrow(), "托管金应等于订单总额");
        verify(orderMapper).updateById(order);
        // 托管阶段不打款卖家、不标记已售
        verify(walletService, never()).receive(any(), any(), any(), any());
        verify(productFeignClient, never()).updateStatus(any(), any());
    }

    @Test
    void pay_wrongStatus_throws() {
        Order order = orderWithStatus(1);
        when(orderMapper.selectById(1L)).thenReturn(order);

        assertThrows(BusinessException.class, () -> orderService.pay(1L, 2L));
        verify(walletService, never()).deductBalance(any(), any(), any(), any());
    }

    @Test
    void ship_status1_transitionsTo2() {
        Order order = orderWithStatus(1);
        when(orderMapper.selectById(1L)).thenReturn(order);

        orderService.ship(1L, 1L, "顺丰", "SF123");

        assertEquals(2, order.getStatus());
        assertEquals("SF123", order.getShipNo());
        verify(orderMapper).updateById(order);
    }

    @Test
    void ship_wrongStatus_throws() {
        Order order = orderWithStatus(0);
        when(orderMapper.selectById(1L)).thenReturn(order);

        assertThrows(BusinessException.class, () -> orderService.ship(1L, 1L, "顺丰", "SF123"));
    }

    @Test
    void receive_status2_releasesEscrowToSeller() {
        Order order = orderWithStatus(2);
        order.setEscrow(new BigDecimal("100.00"));
        when(orderMapper.selectById(1L)).thenReturn(order);

        orderService.receive(1L, 2L);

        verify(walletService).receive(eq(1L), eq(new BigDecimal("100.00")), eq(1L), contains("订单收款"));
        assertEquals(4, order.getStatus(), "收货后应为 4 已完成");
        assertNull(order.getEscrow());
        verify(productFeignClient).updateStatus(9L, 3); // 商品标记已售（ProductStatus.SOLD=3）
    }

    @Test
    void cancel_unpaid_restoresProductWithoutRefund() {
        Order order = orderWithStatus(0);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(orderMapper.update(isNull(), any())).thenReturn(1);

        orderService.cancel(1L, 2L, "不想买了");

        // 未支付取消：不退款、写取消状态（条件更新落库）、商品恢复在售
        verify(walletService, never()).refund(any(), any(), any(), any());
        verify(orderMapper).update(isNull(), any());
        verify(productFeignClient).updateStatus(9L, 1); // 恢复在售
    }

    @Test
    void cancel_paid_refundsBuyer() {
        Order order = orderWithStatus(1);
        order.setEscrow(new BigDecimal("100.00"));
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(orderMapper.update(isNull(), any())).thenReturn(1);

        orderService.cancel(1L, 2L, "协商取消");

        // 已支付取消：先抢占总状态改为已取消，再全额退款给买家，商品恢复在售
        verify(orderMapper).update(isNull(), any());
        verify(walletService).refund(eq(2L), eq(new BigDecimal("100.00")), eq(1L), contains("订单退款"));
        verify(productFeignClient).updateStatus(9L, 1);
    }

    @Test
    void cancel_lostRace_throwsAndDoesNothing() {
        Order order = orderWithStatus(0);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(orderMapper.update(isNull(), any())).thenReturn(0); // 并发支付抢占了状态

        assertThrows(BusinessException.class, () -> orderService.cancel(1L, 2L, "取消"));

        verify(walletService, never()).refund(any(), any(), any(), any());
        verify(productFeignClient, never()).updateStatus(any(), any());
    }

    @Test
    void cancel_shippedOrder_throws() {
        Order order = orderWithStatus(2);
        when(orderMapper.selectById(1L)).thenReturn(order);

        assertThrows(BusinessException.class, () -> orderService.cancel(1L, 2L, "取消"));
        verify(walletService, never()).refund(any(), any(), any(), any());
    }

    @Test
    void cancel_notParticipant_throws() {
        Order order = orderWithStatus(0);
        when(orderMapper.selectById(1L)).thenReturn(order);

        assertThrows(BusinessException.class, () -> orderService.cancel(1L, 999L, "取消"));
    }
}
