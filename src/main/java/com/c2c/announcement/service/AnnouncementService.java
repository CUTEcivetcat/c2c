package com.c2c.announcement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.announcement.entity.Announcement;
import com.c2c.announcement.mapper.AnnouncementMapper;
import com.c2c.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 公告服务：提供公开列表/最新公告，以及管理端的发布、编辑、下架、删除能力。
 */
@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementMapper announcementMapper;

    /** 用户端公开列表（仅已发布，置顶优先，按时间倒序） */
    public Page<Announcement> listPublished(int page, int size, Integer type) {
        LambdaQueryWrapper<Announcement> w = new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getStatus, 1)
                .orderByDesc(Announcement::getPinned)
                .orderByDesc(Announcement::getId);
        if (type != null) {
            w.eq(Announcement::getType, type);
        }
        return announcementMapper.selectPage(new Page<>(page, size), w);
    }

    /** 首页横幅：最新若干条已发布公告 */
    public List<Announcement> listLatest(int limit) {
        return announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getStatus, 1)
                .orderByDesc(Announcement::getPinned)
                .orderByDesc(Announcement::getId)
                .last("LIMIT " + Math.min(Math.max(limit, 1), 20)));
    }

    /** 强制弹窗公告列表（用户登录时展示，is_force=1 且已发布，置顶优先、时间倒序） */
    public List<Announcement> listForce() {
        return announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getStatus, 1)
                .eq(Announcement::getIsForce, 1)
                .orderByDesc(Announcement::getPinned)
                .orderByDesc(Announcement::getId));
    }

    /** 发布商品页右侧展示公告（show_on_publish=1 且已发布） */
    public List<Announcement> listPublish() {
        return announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getStatus, 1)
                .eq(Announcement::getShowOnPublish, 1)
                .orderByDesc(Announcement::getPinned)
                .orderByDesc(Announcement::getId));
    }

    /** 管理端全部公告（含已下架） */
    public Page<Announcement> listAll(int page, int size, String keyword) {
        LambdaQueryWrapper<Announcement> w = new LambdaQueryWrapper<Announcement>()
                .orderByDesc(Announcement::getId);
        if (keyword != null && !keyword.trim().isEmpty()) {
            w.like(Announcement::getTitle, keyword.trim());
        }
        return announcementMapper.selectPage(new Page<>(page, size), w);
    }

    /** 发布公告 */
    public Announcement create(Announcement a, Long adminId) {
        if (a.getTitle() == null || a.getTitle().trim().isEmpty()) {
            throw new BusinessException("公告标题不能为空");
        }
        if (a.getContent() == null || a.getContent().trim().isEmpty()) {
            throw new BusinessException("公告内容不能为空");
        }
        a.setId(null);
        a.setTitle(a.getTitle().trim());
        a.setType(a.getType() == null ? 1 : a.getType());
        a.setStatus(a.getStatus() == null ? 1 : a.getStatus());
        a.setPinned(a.getPinned() == null ? 0 : a.getPinned());
        a.setCreatedBy(adminId);
        announcementMapper.insert(a);
        return a;
    }

    /** 编辑公告 */
    public Announcement update(Long id, Announcement a) {
        Announcement exist = announcementMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("公告不存在");
        }
        if (a.getTitle() != null) exist.setTitle(a.getTitle().trim());
        if (a.getContent() != null) exist.setContent(a.getContent());
        if (a.getType() != null) exist.setType(a.getType());
        if (a.getPinned() != null) exist.setPinned(a.getPinned());
        announcementMapper.updateById(exist);
        return exist;
    }

    /** 下架/发布（status 1 发布 0 下架） */
    public void changeStatus(Long id, Integer status) {
        Announcement exist = announcementMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("公告不存在");
        }
        exist.setStatus(status == null || status != 1 ? 0 : 1);
        announcementMapper.updateById(exist);
    }

    /** 删除公告 */
    public void delete(Long id) {
        announcementMapper.deleteById(id);
    }
}
