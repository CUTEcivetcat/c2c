package com.c2c.favorite.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 商品收藏实体：用户与商品的收藏关系。
 */
@Data
@TableName("favorite")
public class Favorite {
    /** 收藏记录 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 收藏用户 ID */
    private Long userId;

    /** 被收藏的商品 ID */
    private Long productId;

    /** 收藏时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
