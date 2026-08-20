package com.c2c.product.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品信息视图对象（对外返回的商品详情/列表信息）
 */
@Data
@Builder
public class ProductVO {

    /** 商品ID */
    private Long id;

    /** 卖家用户ID */
    private Long sellerId;

    /** 卖家昵称 */
    private String sellerName;

    /** 卖家头像 */
    private String sellerAvatar;

    /** 卖家信誉评分 */
    private BigDecimal sellerReputation;

    /** 商品分类ID */
    private Long categoryId;

    /** 分类名称 */
    private String categoryName;

    /** 商品标题 */
    private String title;

    /** 商品描述 */
    private String description;

    /** 售价 */
    private BigDecimal price;

    /** 原价/划线价 */
    private BigDecimal originalPrice;

    /** 成色 */
    private Integer condition;

    /** 成色文本描述 */
    private String conditionText;

    /** 商品状态（如 0=待审核，1=在售，2=已下架，3=审核驳回，4=已售出） */
    private Integer status;

    /** 状态文本描述 */
    private String statusText;

    /** 审核驳回原因 */
    private String reviewReason;

    /** 运费类型 */
    private Integer freightType;

    /** 运费类型文本描述 */
    private String freightText;

    /** 运费金额 */
    private BigDecimal freightAmount;

    /** 浏览量 */
    private Integer viewCount;

    /** 收藏量 */
    private Integer favoriteCount;

    /** 商品所在地 */
    private String location;

    /** 当前登录用户是否已收藏该商品 */
    private Boolean isFavorited;

    /** 商品图片列表 */
    private List<ImageVO> images;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /**
     * 商品图片信息
     */
    @Data
    @Builder
    public static class ImageVO {

        /** 图片ID */
        private Long id;

        /** 图片URL */
        private String url;

        /** 是否为封面图 */
        private Boolean isCover;
    }
}
