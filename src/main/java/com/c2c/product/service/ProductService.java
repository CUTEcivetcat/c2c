package com.c2c.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.product.dto.ProductCreateDTO;
import com.c2c.product.dto.ProductVO;

/**
 * 商品服务接口
 * <p>覆盖商品发布、编辑、上下架、违规管理、详情查询、搜索及卖家商品列表等能力，
 * 操作均带卖家归属校验，管理端支持违规下架与恢复上架。</p>
 */
public interface ProductService {

    /** 发布商品（保存商品基本信息与图片，初始状态为在售） */
    Long publish(ProductCreateDTO dto, Long sellerId);

    /** 编辑商品（校验卖家归属，图片采用先删后增方式更新） */
    void update(Long productId, ProductCreateDTO dto, Long sellerId);

    /** 卖家自行下架商品 */
    void offShelf(Long productId, Long sellerId);

    /** 卖家自行修改商品状态（带归属校验，禁止直接设置为违规下架） */
    void updateStatus(Long productId, Long sellerId, Integer status);

    /** 管理员违规下架 */
    void ban(Long productId, String reason);

    /** 管理员恢复上架 */
    void restore(Long productId);

    /** 查询商品详情（浏览量+1，带 Redis 缓存，附带卖家与收藏信息） */
    ProductVO getDetail(Long productId, Long currentUserId);

    /** 搜索在售商品（支持关键词、分类、成色、价格区间与排序，分页） */
    Page<ProductVO> search(String keyword, Long categoryId, Integer condition,
                           Double minPrice, Double maxPrice, String sort, int page, int size);

    /** 查询卖家自己发布的商品（分页） */
    Page<ProductVO> getMyPublished(Long sellerId, int page, int size);

    /** 查看某个用户发布的在售商品（公开，用于用户主页） */
    Page<ProductVO> getUserOnSaleList(Long userId, int page, int size);
}



