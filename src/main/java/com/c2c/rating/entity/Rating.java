package com.c2c.rating.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户互评实体：交易完成后买家对卖家（或反之）的评价与打分。
 */
@Data
@TableName("rating")
public class Rating {
    /** 评价 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联订单 ID */
    private Long orderId;

    /** 评价人用户 ID */
    private Long raterId;

    /** 被评价用户 ID */
    private Long ratedUserId;

    /** 评价角色：1 买家评卖家 / 2 卖家评买家 */
    private Integer role;

    /** 评分（1~5） */
    private Integer score;

    /** 评价文字内容 */
    private String comment;

    /** 评价标签（JSON 数组字符串） */
    private String tags;

    /** 评价时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
