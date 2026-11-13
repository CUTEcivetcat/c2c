package com.c2c.system.controller;

import com.c2c.common.exception.BusinessException;
import com.c2c.review.mapper.AdminLogMapper;
import com.c2c.system.service.SysConfigService;
import com.c2c.user.entity.User;
import com.c2c.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 系统配置控制器单测：总管理员校验、普通键拒空值、AI 接入键允许清空（停用回退规则引擎）。
 *
 * @author wzh
 */
@ExtendWith(MockitoExtension.class)
class AdminConfigControllerTest {

    @Mock
    private SysConfigService sysConfigService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private AdminLogMapper adminLogMapper;

    @InjectMocks
    private AdminConfigController controller;

    private User topAdmin() {
        User u = new User();
        u.setId(4L);
        u.setAdminLevel(2);
        return u;
    }

    @Test
    void update_nonTopAdmin_reject403() {
        User u = new User();
        u.setId(2L);
        u.setAdminLevel(0);
        when(userMapper.selectById(2L)).thenReturn(u);

        BusinessException e = assertThrows(BusinessException.class,
                () -> controller.update(2L, Map.of("key", "sys.x", "value", "1")));
        assertEquals(403, e.getCode());
        verify(sysConfigService, never()).update(any(), any(), any());
    }

    @Test
    void update_blankValue_nonAiKey_reject() {
        when(userMapper.selectById(4L)).thenReturn(topAdmin());

        assertThrows(BusinessException.class,
                () -> controller.update(4L, Map.of("key", "sys.sms-echo-code", "value", "  ")));
        verify(sysConfigService, never()).update(any(), any(), any());
    }

    @Test
    void update_aiModel_blankValue_reject() {
        when(userMapper.selectById(4L)).thenReturn(topAdmin());

        // 模型名不属于可清空的接入键，空值仍拒绝
        assertThrows(BusinessException.class,
                () -> controller.update(4L, Map.of("key", "ai.model", "value", "")));
        verify(sysConfigService, never()).update(any(), any(), any());
    }

    @Test
    void update_aiBaseUrl_blank_allowed() {
        when(userMapper.selectById(4L)).thenReturn(topAdmin());

        controller.update(4L, Map.of("key", "ai.base-url", "value", ""));
        verify(sysConfigService, times(1)).update(eq("ai.base-url"), eq(""), eq(4L));
    }

    @Test
    void update_aiApiKey_blank_allowed() {
        when(userMapper.selectById(4L)).thenReturn(topAdmin());

        controller.update(4L, Map.of("key", "ai.api-key", "value", ""));
        verify(sysConfigService, times(1)).update(eq("ai.api-key"), eq(""), eq(4L));
    }

    @Test
    void update_normalValue_ok() {
        when(userMapper.selectById(4L)).thenReturn(topAdmin());

        controller.update(4L, Map.of("key", "ai.model", "value", "deepseek-flash"));
        verify(sysConfigService, times(1)).update(eq("ai.model"), eq("deepseek-flash"), eq(4L));
    }
}
