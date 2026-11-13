package com.c2c.popup.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动弹窗实体：管理员配置的活动提示，可绑定优惠券等活动；绑定券时有效期跟随券。
 */
@Data
@TableName("activity_popup")
public class ActivityPopup {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 弹窗标题 */
    private String title;

    /** 弹窗内容 */
    private String content;

    /** 弹窗配图（可空） */
    private String imageUrl;

    /** 关联券模板 ID，0 表示无关联活动 */
    private Long couponTemplateId;

    /** 跳转类型 coupon/product/announcement/url/none */
    private String jumpType;

    /** 跳转目标（券/商品/公告 ID 或链接） */
    private String jumpTarget;

    /** 生效开始 */
    private LocalDateTime validStart;

    /** 有效期截止；绑定券时跟随券有效期 */
    private LocalDateTime validEnd;

    /** 1 展示 0 下架 */
    private Integer status;

    /** 创建人 ID */
    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
