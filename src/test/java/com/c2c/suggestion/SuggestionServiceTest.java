package com.c2c.suggestion;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.exception.BusinessException;
import com.c2c.suggestion.dto.SuggestionCreateDTO;
import com.c2c.suggestion.dto.SuggestionReplyDTO;
import com.c2c.suggestion.entity.Suggestion;
import com.c2c.suggestion.enums.SuggestionStatus;
import com.c2c.suggestion.mapper.SuggestionMapper;
import com.c2c.suggestion.service.SuggestionService;
import com.c2c.suggestion.vo.SuggestionVO;
import com.c2c.user.entity.User;
import com.c2c.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 建议反馈服务单元测试：提交校验、回复采纳/未采纳、重复处理拦截、我的建议装配。
 */
@ExtendWith(MockitoExtension.class)
class SuggestionServiceTest {

    @Mock
    private SuggestionMapper suggestionMapper;
    @Mock
    private UserMapper userMapper;
    @InjectMocks
    private SuggestionService suggestionService;

    private Suggestion pending(Long id, Long userId) {
        Suggestion s = new Suggestion();
        s.setId(id);
        s.setUserId(userId);
        s.setContent("建议优化分类筛选");
        s.setStatus(SuggestionStatus.PENDING.getCode());
        return s;
    }

    @Test
    void create_blankContent_reject() {
        SuggestionCreateDTO dto = new SuggestionCreateDTO();
        dto.setContent("   ");
        assertThrows(BusinessException.class, () -> suggestionService.create(1L, dto));
        verify(suggestionMapper, never()).insert(any());
    }

    @Test
    void create_normal_insertPending() {
        SuggestionCreateDTO dto = new SuggestionCreateDTO();
        dto.setContent("希望增加收藏分类");
        dto.setContact("13800000001");
        suggestionService.create(2L, dto);
        verify(suggestionMapper, times(1)).insert(argThat(s ->
                s.getUserId().equals(2L) && s.getStatus() == SuggestionStatus.PENDING.getCode()
                        && "13800000001".equals(s.getContact())));
    }

    @Test
    void reply_notFound_reject() {
        when(suggestionMapper.selectById(99L)).thenReturn(null);
        SuggestionReplyDTO dto = new SuggestionReplyDTO();
        dto.setAdopt(true);
        assertThrows(BusinessException.class, () -> suggestionService.reply(99L, 4L, dto));
    }

    @Test
    void reply_alreadyHandled_reject() {
        Suggestion s = pending(1L, 2L);
        s.setStatus(SuggestionStatus.ADOPTED.getCode());
        when(suggestionMapper.selectById(1L)).thenReturn(s);
        SuggestionReplyDTO dto = new SuggestionReplyDTO();
        dto.setAdopt(false);
        assertThrows(BusinessException.class, () -> suggestionService.reply(1L, 4L, dto));
        verify(suggestionMapper, never()).updateById(any());
    }

    @Test
    void reply_adopt_markAdoptedAndRecordReply() {
        Suggestion s = pending(1L, 2L);
        when(suggestionMapper.selectById(1L)).thenReturn(s);
        SuggestionReplyDTO dto = new SuggestionReplyDTO();
        dto.setAdopt(true);
        dto.setReply("已纳入排期");
        suggestionService.reply(1L, 4L, dto);
        verify(suggestionMapper, times(1)).updateById(argThat(x ->
                x.getStatus() == SuggestionStatus.ADOPTED.getCode()
                        && "已纳入排期".equals(x.getReply())
                        && x.getRepliedBy().equals(4L) && x.getRepliedAt() != null));
    }

    @Test
    void reply_reject_markRejected() {
        Suggestion s = pending(2L, 3L);
        when(suggestionMapper.selectById(2L)).thenReturn(s);
        SuggestionReplyDTO dto = new SuggestionReplyDTO();
        dto.setAdopt(false);
        suggestionService.reply(2L, 4L, dto);
        verify(suggestionMapper, times(1)).updateById(argThat(x ->
                x.getStatus() == SuggestionStatus.REJECTED.getCode()));
    }

    @Test
    void mySuggestions_assembleNickname() {
        Suggestion s = pending(5L, 2L);
        Page<Suggestion> page = new Page<>(1, 10);
        page.setRecords(Collections.singletonList(s));
        when(suggestionMapper.selectPage(any(), any())).thenReturn(page);
        User u = new User();
        u.setId(2L);
        u.setNickname("小王");
        when(userMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(u));

        Page<SuggestionVO> res = suggestionService.mySuggestions(2L, 1, 10);
        List<SuggestionVO> records = res.getRecords();
        assertEquals(1, records.size());
        assertEquals("小王", records.get(0).getNickname());
        assertEquals(SuggestionStatus.PENDING.getTextByCode(1), records.get(0).getStatusText());
    }
}
