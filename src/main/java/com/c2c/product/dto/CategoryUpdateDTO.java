package com.c2c.product.dto;

import lombok.Data;

/**
 * 商品分类更新请求参数对象
 */
@Data
public class CategoryUpdateDTO {

    /** 传空表示不修改名称 */
    private String name;

    /** 传空表示不修改父分类 */
    private Long parentId;

    /** 排序值（数字越小越靠前） */
    private Integer sortOrder;

    /** 分类图标URL */
    private String iconUrl;
}
