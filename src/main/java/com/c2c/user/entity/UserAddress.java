package com.c2c.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收货地址实体：用户下单时使用的收货信息，可在下单时保存快照。
 */
@Data
@TableName("user_address")
public class UserAddress {

    /** 地址 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 ID */
    private Long userId;

    /** 收货人姓名 */
    private String receiverName;

    /** 收货人手机号 */
    private String phone;

    /** 省份 */
    private String province;

    /** 城市 */
    private String city;

    /** 区/县 */
    private String district;

    /** 详细地址 */
    private String detail;

    /** 邮政编码 */
    private String postalCode;

    /** 是否默认地址：1 默认 / 0 非默认 */
    private Integer isDefault;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
