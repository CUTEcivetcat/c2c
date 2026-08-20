package com.c2c.product.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 商品分类创建请求参数对象
 */
@Data
public class CategoryCreateDTO {

    /** 分类名称 */
    @NotBlank(message = "分类名称不能为空")
    private String name;

    /** 父分类ID，0 或空表示顶级分类 */
    private Long parentId;

    /** 排序值（数字越小越靠前） */
    private Integer sortOrder;

    /** 分类图标URL */
    private String iconUrl;
}
