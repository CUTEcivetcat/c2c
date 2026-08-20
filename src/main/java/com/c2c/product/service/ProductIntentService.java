package com.c2c.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.product.dto.IntentCreateDTO;
import com.c2c.product.dto.IntentVO;

/**
 * 购买意向服务接口
 * <p>买家可就商品向卖家表达购买意向（询价/留言），卖家可回复、标记成交或关闭，
 * 支持买卖双方分别查看意向列表。</p>
 */
public interface ProductIntentService {

    /** 买家对商品表达购买意向 */
    Long create(Long productId, IntentCreateDTO dto, Long buyerId);

    /** 买家：我发出的意向 */
    Page<IntentVO> myList(Long buyerId, int page, int size);

    /** 卖家：收到的意向 */
    Page<IntentVO> sellerList(Long sellerId, int page, int size);

    /** 卖家回复 */
    void reply(Long intentId, String reply, Long sellerId);

    /** 买家或卖家关闭意向 */
    void close(Long intentId, Long userId);

    /** 卖家标记成交 */
    void deal(Long intentId, Long sellerId);
}
