package com.c2c.suggestion.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.exception.BusinessException;
import com.c2c.suggestion.dto.SuggestionCreateDTO;
import com.c2c.suggestion.dto.SuggestionReplyDTO;
import com.c2c.suggestion.entity.Suggestion;
import com.c2c.suggestion.enums.SuggestionStatus;
import com.c2c.suggestion.mapper.SuggestionMapper;
import com.c2c.suggestion.vo.SuggestionVO;
import com.c2c.user.entity.User;
import com.c2c.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 建议反馈服务：用户提交建议、查看我的建议；管理端分页查看并回复是否采纳。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SuggestionService {

    private final SuggestionMapper suggestionMapper;
    private final UserMapper userMapper;

    /** 用户提交建议 */
    @Transactional
    public Long create(Long userId, SuggestionCreateDTO dto) {
        String content = StrUtil.trimToEmpty(dto.getContent());
        if (content.isEmpty()) {
            throw new BusinessException("请填写建议内容");
        }
        Suggestion suggestion = new Suggestion();
        suggestion.setUserId(userId);
        suggestion.setContent(content);
        suggestion.setContact(StrUtil.blankToDefault(dto.getContact(), null));
        suggestion.setStatus(SuggestionStatus.PENDING.getCode());
        suggestionMapper.insert(suggestion);
        log.info("建议反馈提交：suggestionId={}, userId={}", suggestion.getId(), userId);
        return suggestion.getId();
    }

    /** 用户端：我的建议，按时间倒序分页 */
    public Page<SuggestionVO> mySuggestions(Long userId, int page, int size) {
        LambdaQueryWrapper<Suggestion> wrapper = new LambdaQueryWrapper<Suggestion>()
                .eq(Suggestion::getUserId, userId)
                .orderByDesc(Suggestion::getCreatedAt);
        return toVoPage(suggestionMapper.selectPage(new Page<>(page, size), wrapper));
    }

    /** 管理端：建议列表，可按状态/关键字筛选；待处理排前 */
    public Page<SuggestionVO> adminList(Integer status, int page, int size, String keyword) {
        LambdaQueryWrapper<Suggestion> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Suggestion::getStatus, status);
        }
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(Suggestion::getContent, keyword.trim());
        }
        wrapper.orderByAsc(Suggestion::getStatus).orderByDesc(Suggestion::getCreatedAt);
        return toVoPage(suggestionMapper.selectPage(new Page<>(page, size), wrapper));
    }

    /** 管理端：回复建议并标记是否采纳；仅待处理可回复 */
    @Transactional
    public void reply(Long id, Long adminId, SuggestionReplyDTO dto) {
        Suggestion suggestion = suggestionMapper.selectById(id);
        if (suggestion == null) {
            throw new BusinessException("建议不存在");
        }
        if (suggestion.getStatus() != null && suggestion.getStatus() != SuggestionStatus.PENDING.getCode()) {
            throw new BusinessException("该建议已处理");
        }
        if (dto.getAdopt() == null) {
            throw new BusinessException("请选择是否采纳");
        }
        suggestion.setStatus(Boolean.TRUE.equals(dto.getAdopt())
                ? SuggestionStatus.ADOPTED.getCode() : SuggestionStatus.REJECTED.getCode());
        suggestion.setReply(StrUtil.blankToDefault(dto.getReply(), ""));
        suggestion.setRepliedBy(adminId);
        suggestion.setRepliedAt(LocalDateTime.now());
        suggestionMapper.updateById(suggestion);
        log.info("建议反馈回复：suggestionId={}, adopt={}, handlerId={}", id, dto.getAdopt(), adminId);
    }

    // ==================== 私有装配 ====================

    private Page<SuggestionVO> toVoPage(Page<Suggestion> page) {
        Page<SuggestionVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(buildVOs(page.getRecords()));
        return result;
    }

    private List<SuggestionVO> buildVOs(List<Suggestion> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> userIds = list.stream().map(Suggestion::getUserId).collect(Collectors.toSet());
        Map<Long, String> nickMap = selectNicknames(userIds);
        return list.stream().map(s -> {
            SuggestionVO vo = new SuggestionVO();
            vo.setId(s.getId());
            vo.setUserId(s.getUserId());
            vo.setNickname(nickMap.getOrDefault(s.getUserId(), "用户" + s.getUserId()));
            vo.setContent(s.getContent());
            vo.setContact(s.getContact());
            vo.setStatus(s.getStatus());
            vo.setStatusText(SuggestionStatus.getTextByCode(s.getStatus()));
            vo.setReply(s.getReply());
            vo.setRepliedBy(s.getRepliedBy());
            vo.setRepliedAt(s.getRepliedAt());
            vo.setCreatedAt(s.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
    }

    private Map<Long, String> selectNicknames(Set<Long> userIds) {
        Map<Long, String> map = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return map;
        }
        userMapper.selectBatchIds(userIds).forEach(u -> map.put(u.getId(), u.getNickname()));
        return map;
    }
}
