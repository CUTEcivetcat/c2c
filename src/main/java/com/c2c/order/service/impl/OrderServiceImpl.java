package com.c2c.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.enums.ProductStatus;
import com.c2c.common.exception.BusinessException;
import com.c2c.order.entity.Order;
import com.c2c.order.feign.ProductFeignClient;
import com.c2c.order.feign.UserFeignClient;
import com.c2c.order.mapper.OrderMapper;
import com.c2c.order.mq.OrderEventPublisher;
import com.c2c.order.service.OrderService;
import com.c2c.product.dto.ProductVO;
import com.c2c.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订单服务实现类
 * <p>实现订单全流程的状态流转：通过 Feign 调用商品、用户服务校验数据，
 * 通过 MQ 发布订单事件，并对同一商品/订单加锁防止并发重复操作。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final Map<String, Object> LOCKS = new ConcurrentHashMap<>();

    private final OrderMapper orderMapper;
    private final ProductFeignClient productFeignClient;
    private final UserFeignClient userFeignClient;
    private final OrderEventPublisher eventPublisher;
    private final WalletService walletService;

    @Override
    @Transactional
    public Map<String, Object> create(Long productId, Long addressId, Long buyerId) {
        synchronized (lockFor("product:" + productId)) {
            Map<String, Object> productResp = productFeignClient.getProduct(productId, buyerId);
            @SuppressWarnings("unchecked")
            Map<String, Object> product = (Map<String, Object>) productResp.get("data");
            if (product == null) {
                throw new BusinessException("商品不存在");
            }

            Long sellerId = Long.valueOf(product.get("sellerId").toString());
            if (sellerId.equals(buyerId)) {
                throw new BusinessException("不能购买自己的商品");
            }

            Integer productStatus = Integer.valueOf(product.get("status").toString());
            if (productStatus != ProductStatus.ON_SALE.getCode()) {
                throw new BusinessException("商品当前不可购买");
            }

            Map<String, Object> addrResp = userFeignClient.getAddress(addressId, buyerId);
            @SuppressWarnings("unchecked")
            Map<String, Object> address = (Map<String, Object>) addrResp.get("data");
            if (address == null) {
                throw new BusinessException("收货地址不存在");
            }

            String orderNo = generateOrderNo();
            BigDecimal price = new BigDecimal(product.get("price").toString());
            BigDecimal freight = new BigDecimal(product.getOrDefault("freightAmount", "0").toString());

            Order order = new Order();
            order.setOrderNo(orderNo);
            order.setBuyerId(buyerId);
            order.setSellerId(sellerId);
            order.setProductId(productId);
            order.setProductTitle(String.valueOf(product.get("title")));
            order.setProductImage(firstImageUrl(product.get("images")));
            order.setPrice(price);
            order.setFreightAmount(freight);
            order.setTotalAmount(price.add(freight));
            order.setAddressId(addressId);
            order.setAddressSnapshot(buildAddressSnapshot(address));
            order.setStatus(0);
            orderMapper.insert(order);

            productFeignClient.updateStatus(productId, ProductStatus.RESERVED.getCode());
            eventPublisher.publishOrderCreated(order.getId(), orderNo);
            eventPublisher.publishOrderTimeout(order.getId());

            log.info("order created: orderId={}, orderNo={}, buyerId={}, sellerId={}",
                    order.getId(), orderNo, buyerId, sellerId);

            Map<String, Object> result = new HashMap<>();
            result.put("orderId", order.getId());
            result.put("orderNo", orderNo);
            result.put("totalAmount", order.getTotalAmount());
            result.put("status", 0);
            result.put("expireTime", LocalDateTime.now().plusMinutes(30).toString());
            return result;
        }
    }

    @Override
    public Order getDetail(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new BusinessException("无权查看该订单");
        }
        return order;
    }

    @Override
    public Page<Order> getBuyerList(Long buyerId, Integer status, int page, int size) {
        LambdaQueryWrapper<Order> w = new LambdaQueryWrapper<Order>()
                .eq(Order::getBuyerId, buyerId)
                .orderByDesc(Order::getCreatedAt);
        if (status != null) {
            w.eq(Order::getStatus, status);
        }
        return orderMapper.selectPage(new Page<>(page, size), w);
    }

    @Override
    public Page<Order> getSellerList(Long sellerId, Integer status, int page, int size) {
        LambdaQueryWrapper<Order> w = new LambdaQueryWrapper<Order>()
                .eq(Order::getSellerId, sellerId)
                .orderByDesc(Order::getCreatedAt);
        if (status != null) {
            w.eq(Order::getStatus, status);
        }
        return orderMapper.selectPage(new Page<>(page, size), w);
    }

    @Override
    @Transactional
    public void pay(Long orderId, Long buyerId) {
        synchronized (lockFor("order:" + orderId)) {
            Order order = orderMapper.selectById(orderId);
            if (order == null || !order.getBuyerId().equals(buyerId)) {
                throw new BusinessException("订单不存在");
            }
            if (order.getStatus() != 0) {
                throw new BusinessException("当前订单状态不允许支付");
            }

            // 余额扣款 + 平台托管
            BigDecimal amount = order.getTotalAmount();
            walletService.deductBalance(buyerId, amount, orderId, "订单支付 #" + order.getOrderNo());

            // 自动发货 + 自动确认收货
            walletService.receive(order.getSellerId(), amount, orderId,
                    "订单收款 #" + order.getOrderNo());

            order.setStatus(4);
            order.setPaymentMethod("balance");
            order.setPaymentTime(LocalDateTime.now());
            order.setShipTime(LocalDateTime.now());
            order.setShipCompany("自动发货");
            order.setReceiveTime(LocalDateTime.now());
            order.setCompleteTime(LocalDateTime.now());
            order.setEscrow(null);
            orderMapper.updateById(order);

            productFeignClient.updateStatus(order.getProductId(), ProductStatus.SOLD.getCode());
            eventPublisher.publishOrderPaid(orderId);
            log.info("order paid & auto-completed: orderId={}, amount={}", orderId, amount);
        }
    }

    @Override
    public void ship(Long orderId, Long sellerId, String shipCompany, String shipNo) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getSellerId().equals(sellerId)) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 1) {
            throw new BusinessException("当前订单状态不允许发货");
        }

        order.setStatus(2);
        order.setShipCompany(shipCompany);
        order.setShipNo(shipNo);
        order.setShipTime(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("order shipped: orderId={}", orderId);
    }

    @Override
    @Transactional
    public void receive(Long orderId, Long buyerId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getBuyerId().equals(buyerId)) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 2) {
            throw new BusinessException("当前订单状态不允许收货");
        }

        // 平台托管金额打给卖家
        if (order.getEscrow() != null && order.getEscrow().compareTo(BigDecimal.ZERO) > 0) {
            walletService.receive(order.getSellerId(), order.getEscrow(), orderId,
                    "订单收款 #" + order.getOrderNo());
        }

        order.setStatus(4);
        order.setReceiveTime(LocalDateTime.now());
        order.setCompleteTime(LocalDateTime.now());
        order.setEscrow(null);
        orderMapper.updateById(order);
        log.info("order received and completed: orderId={}", orderId);
    }

    @Override
    @Transactional
    public void cancel(Long orderId, Long userId, String reason) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }
        if (order.getStatus() != 0 && order.getStatus() != 1) {
            throw new BusinessException("当前订单状态不允许取消");
        }

        // 已支付（有托管金）则退款
        if (order.getEscrow() != null && order.getEscrow().compareTo(BigDecimal.ZERO) > 0) {
            walletService.refund(order.getBuyerId(), order.getEscrow(), orderId,
                    "订单退款 #" + order.getOrderNo());
        }

        order.setStatus(5);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(reason);
        order.setEscrow(null);
        orderMapper.updateById(order);

        if (order.getStatus() == 5 || order.getStatus() == 1) {
            productFeignClient.updateStatus(order.getProductId(), ProductStatus.ON_SALE.getCode());
        }
        log.info("order cancelled: orderId={}", orderId);
    }

    private Object lockFor(String key) {
        return LOCKS.computeIfAbsent(key, k -> new Object());
    }

    private String generateOrderNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = String.format("%06d", (int) (Math.random() * 1000000));
        return date + suffix;
    }

    private String firstImageUrl(Object images) {
        if (!(images instanceof List) || ((List<?>) images).isEmpty()) {
            return "";
        }
        Object first = ((List<?>) images).get(0);
        if (first instanceof Map) {
            Object url = ((Map<?, ?>) first).get("url");
            return url == null ? "" : url.toString();
        }
        if (first instanceof ProductVO.ImageVO) {
            return ((ProductVO.ImageVO) first).getUrl();
        }
        return "";
    }

    private String buildAddressSnapshot(Map<String, Object> address) {
        return "{"
                + "\"receiverName\":\"" + address.getOrDefault("receiverName", "") + "\","
                + "\"phone\":\"" + address.getOrDefault("phone", "") + "\","
                + "\"province\":\"" + address.getOrDefault("province", "") + "\","
                + "\"city\":\"" + address.getOrDefault("city", "") + "\","
                + "\"district\":\"" + address.getOrDefault("district", "") + "\","
                + "\"detail\":\"" + address.getOrDefault("detail", "") + "\""
                + "}";
    }
}
