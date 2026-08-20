package com.c2c.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品分类实体：支持多级树形分类，children 为子分类（非表字段）。
 */
@Data
@TableName("category")
public class Category {

    /** 分类 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类名称 */
    private String name;

    /** 父分类 ID，0 为顶级分类 */
    private Long parentId;

    /** 层级：1 一级 / 2 二级 / 3 三级 */
    private Integer level;

    /** 排序权重（越小越靠前） */
    private Integer sortOrder;

    /** 分类图标 URL */
    private String iconUrl;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 子分类列表（非数据库字段，树形查询时填充） */
    @TableField(exist = false)
    private List<Category> children;
}
