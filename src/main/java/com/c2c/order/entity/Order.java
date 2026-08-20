package com.c2c.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体：买家下单后由卖家履约（发货/收货/完成/取消）。
 */
@Data
@TableName("`order`")
public class Order {

    /** 订单 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 买家用户 ID */
    private Long buyerId;

    /** 卖家用户 ID */
    private Long sellerId;

    /** 商品 ID */
    private Long productId;

    /** 商品标题（下单时快照） */
    private String productTitle;

    /** 商品封面图（下单时快照） */
    private String productImage;

    /** 商品价格 */
    private BigDecimal price;

    /** 运费 */
    private BigDecimal freightAmount;

    /** 订单总金额（价格 + 运费） */
    private BigDecimal totalAmount;

    /** 收货地址 ID（下单时选中） */
    private Long addressId;

    /** 收货地址快照（JSON 字符串，防止地址被修改影响订单） */
    private String addressSnapshot;

    /** 订单状态：0 待支付 / 1 已支付 / 2 已发货 / 3 已收货 / 4 已完成 / 5 已取消 */
    @TableField("`status`")
    private Integer status;

    /** 支付方式 */
    private String paymentMethod;

    /** 支付时间 */
    private LocalDateTime paymentTime;

    /** 快递公司 */
    private String shipCompany;

    /** 快递单号 */
    private String shipNo;

    /** 发货时间 */
    private LocalDateTime shipTime;

    /** 收货时间 */
    private LocalDateTime receiveTime;

    /** 完成时间 */
    private LocalDateTime completeTime;

    /** 取消时间 */
    private LocalDateTime cancelTime;

    /** 取消原因 */
    private String cancelReason;

    /** 买家是否已评价：1 是 / 0 否 */
    private Integer buyerRated;

    /** 卖家是否已评价：1 是 / 0 否 */
    private Integer sellerRated;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
