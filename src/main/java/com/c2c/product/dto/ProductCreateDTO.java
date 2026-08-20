package com.c2c.product.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
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
    private String title;

    /** 商品描述 */
    private String description;

    /** 售价 */
    @NotNull(message = "价格不能为空")
    private BigDecimal price;

    /** 原价/划线价 */
    private BigDecimal originalPrice;

    /** 商品分类ID */
    @NotNull(message = "分类不能为空")
    private Long categoryId;

    /** 成色（如 0=全新，1=几乎全新，2=轻微使用痕迹，3=明显使用痕迹） */
    @NotNull(message = "成色不能为空")
    private Integer condition;

    /** 运费类型（如 0=包邮，1=买家承担运费，2=自定义运费） */
    private Integer freightType;

    /** 运费金额 */
    private BigDecimal freightAmount;

    /** 商品所在地 */
    private String location;

    /** 上传后的图片URL列表，第一张为封面 */
    private List<String> images;
}
