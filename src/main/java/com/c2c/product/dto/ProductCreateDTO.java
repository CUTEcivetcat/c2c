package com.c2c.product.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品发布（创建）请求参数对象
 */
@Data
public class ProductCreateDTO {

    /** 商品标题 */
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题不能超过200字")
    private String title;

    /** 商品描述 */
    @Size(max = 5000, message = "描述不能超过5000字")
    private String description;

    /** 售价 */
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    private BigDecimal price;

    /** 原价/划线价 */
    @DecimalMin(value = "0.01", message = "原价必须大于0")
    private BigDecimal originalPrice;

    /** 商品分类ID */
    @NotNull(message = "分类不能为空")
    private Long categoryId;

    /** 成色（1全新/2几乎全新/3轻微使用/4明显使用） */
    @NotNull(message = "成色不能为空")
    private Integer condition;

    /** 运费类型（1包邮/2买家承担） */
    private Integer freightType;

    /** 运费金额（0 表示包邮） */
    @DecimalMin(value = "0.00", message = "运费不能为负数")
    private BigDecimal freightAmount;

    /** 商品所在地 */
    @Size(max = 100, message = "所在地不能超过100字")
    private String location;

    /** 上传后的图片URL列表，第一张为封面（最多9张） */
    @Size(max = 9, message = "图片最多9张")
    private List<String> images;
}
