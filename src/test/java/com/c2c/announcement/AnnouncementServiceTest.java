package com.c2c.announcement;

import com.c2c.announcement.entity.Announcement;
import com.c2c.announcement.mapper.AnnouncementMapper;
import com.c2c.announcement.service.AnnouncementService;
import com.c2c.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 公告服务单元测试：发布校验、编辑保存扩展字段。
 */
@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock
    private AnnouncementMapper announcementMapper;
    @InjectMocks
    private AnnouncementService announcementService;

    @Test
    void create_emptyTitle_throws() {
        Announcement a = new Announcement();
        a.setTitle(" ");
        a.setContent("内容");
        assertThrows(BusinessException.class, () -> announcementService.create(a, 1L));
    }

    @Test
    void create_emptyContent_throws() {
        Announcement a = new Announcement();
        a.setTitle("标题");
        a.setContent("");
        assertThrows(BusinessException.class, () -> announcementService.create(a, 1L));
    }

    @Test
    void create_success_setsDefaults() {
        Announcement a = new Announcement();
        a.setTitle("公告");
        a.setContent("内容");
        when(announcementMapper.insert(any())).thenReturn(1);

        Announcement created = announcementService.create(a, 1L);

        assertEquals(1, created.getType());
        assertEquals(1, created.getStatus());
        assertEquals(0, created.getPinned());
        verify(announcementMapper).insert(a);
    }

    @Test
    void update_savesExtendedFields() {
        // 已有公告（模拟数据库中的记录）
        Announcement exist = new Announcement();
        exist.setId(1L);
        exist.setTitle("旧标题");
        when(announcementMapper.selectById(1L)).thenReturn(exist);

        // 编辑请求（管理端修改扩展字段）
        Announcement edit = new Announcement();
        edit.setIsForce(1);
        edit.setMinSeconds(5);
        edit.setScroll(0);
        edit.setShowOnPublish(1);
        edit.setPinned(1);

        announcementService.update(1L, edit);

        assertEquals(1, exist.getIsForce());
        assertEquals(5, exist.getMinSeconds());
        assertEquals(0, exist.getScroll());
        assertEquals(1, exist.getShowOnPublish());
        assertEquals(1, exist.getPinned());
        verify(announcementMapper).updateById(exist);
    }

    @Test
    void update_notFound_throws() {
        when(announcementMapper.selectById(99L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> announcementService.update(99L, new Announcement()));
    }
}
