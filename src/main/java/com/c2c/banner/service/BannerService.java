package com.c2c.banner.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.c2c.banner.entity.Banner;
import com.c2c.banner.mapper.BannerMapper;
import com.c2c.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 轮播图服务：公开列表 + 管理端 CRUD。
 */
@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerMapper bannerMapper;

    /** 用户端公开列表（仅启用，按排序） */
    public List<Banner> listEnabled() {
        return bannerMapper.selectList(new LambdaQueryWrapper<Banner>()
                .eq(Banner::getStatus, 1)
                .orderByAsc(Banner::getSortOrder)
                .orderByDesc(Banner::getId));
    }

    /** 管理端全部列表 */
    public List<Banner> listAll() {
        return bannerMapper.selectList(new LambdaQueryWrapper<Banner>()
                .orderByAsc(Banner::getSortOrder)
                .orderByDesc(Banner::getId));
    }

    public Banner create(Banner banner) {
        if (banner.getImageUrl() == null || banner.getImageUrl().trim().isEmpty()) {
            throw new BusinessException("请上传图片");
        }
        banner.setId(null);
        banner.setStatus(banner.getStatus() == null ? 1 : banner.getStatus());
        banner.setSortOrder(banner.getSortOrder() == null ? 0 : banner.getSortOrder());
        bannerMapper.insert(banner);
        return banner;
    }

    public Banner update(Long id, Banner banner) {
        Banner exist = bannerMapper.selectById(id);
        if (exist == null) throw new BusinessException("轮播图不存在");
        if (banner.getTitle() != null) exist.setTitle(banner.getTitle());
        if (banner.getImageUrl() != null) exist.setImageUrl(banner.getImageUrl());
        if (banner.getLinkUrl() != null) exist.setLinkUrl(banner.getLinkUrl());
        if (banner.getSortOrder() != null) exist.setSortOrder(banner.getSortOrder());
        if (banner.getStatus() != null) exist.setStatus(banner.getStatus());
        bannerMapper.updateById(exist);
        return exist;
    }

    public void delete(Long id) {
        bannerMapper.deleteById(id);
    }
}