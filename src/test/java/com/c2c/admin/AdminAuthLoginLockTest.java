package com.c2c.admin;

import com.c2c.admin.controller.AdminAuthController;
import com.c2c.common.exception.BusinessException;
import com.c2c.common.infra.TestLog;
import com.c2c.system.service.SysConfigService;
import com.c2c.user.entity.User;
import com.c2c.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 管理员登录失败锁定单元测试（S3/M1-1.2）。
 *
 * <p>规则：连续失败达到上限（默认5次）锁定一段时间（默认15分钟）；
 * 锁定期间即使密码正确也拒绝；开关关闭时不做任何限制。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings("unchecked")
class AdminAuthLoginLockTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private SysConfigService sysConfigService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AdminAuthController controller;

    private User admin;

    @org.junit.jupiter.api.BeforeAll
    static void header() {
        TestLog.log("[测试类] 管理员登录失败锁定：5次失败锁定15分钟 / 锁定期间拒绝 / 开关关闭不限制 / 成功清计数");
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "jwtSecret",
                "test-secret-test-secret-test-secret-123456!");
        ReflectionTestUtils.setField(controller, "jwtExpiration", 604800L);

        admin = new User();
        admin.setId(4L);
        admin.setUsername("admin");
        admin.setRole(1);
        admin.setStatus(1);
        admin.setAdminLevel(2);
        admin.setAdminPerms("*");
        admin.setPassword(new BCryptPasswordEncoder().encode("right-pass"));

        when(userMapper.selectOne(any())).thenReturn(admin);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // 开关开启，上限5次，锁15分钟
        when(sysConfigService.getBool(eq("security.admin-login-lock.enabled"), eq(true))).thenReturn(true);
        when(sysConfigService.getInt(eq("security.admin-login-lock.max-attempts"), eq(5))).thenReturn(5);
        when(sysConfigService.getInt(eq("security.admin-login-lock.lock-minutes"), eq(15))).thenReturn(15);
    }

    private java.util.Map<String, String> wrongLogin() {
        java.util.Map<String, String> body = new HashMap<>();
        body.put("account", "admin");
        body.put("password", "wrong-pass");
        return body;
    }

    @Test
    void fiveFailures_lockAccount() {
        TestLog.log("[测试] 连续失败5次 → 第5次提示锁定15分钟，写锁定键并清除失败计数");
        when(redisTemplate.hasKey("adminlogin:lock:admin")).thenReturn(false);
        when(valueOperations.increment("adminlogin:fail:admin")).thenReturn(1L, 2L, 3L, 4L, 5L);

        // 前 4 次：普通的"用户名或密码错误"
        for (int i = 0; i < 4; i++) {
            BusinessException e = assertThrows(BusinessException.class, () -> controller.login(wrongLogin()));
            assertEquals("用户名或密码错误", e.getMessage());
        }
        // 第 5 次：触发锁定
        BusinessException fifth = assertThrows(BusinessException.class, () -> controller.login(wrongLogin()));
        assertTrue(fifth.getMessage().contains("已锁定"), "第5次失败应提示锁定: " + fifth.getMessage());
        verify(valueOperations).set(eq("adminlogin:lock:admin"), eq("1"), eq(15L), eq(java.util.concurrent.TimeUnit.MINUTES));
        verify(redisTemplate).delete("adminlogin:fail:admin");
    }

    @Test
    void lockedAccount_rejectedEvenWithRightPassword() {
        TestLog.log("[测试] 锁定期间：即使密码正确也直接拒绝（在查库之前拦截）");
        when(redisTemplate.hasKey("adminlogin:lock:admin")).thenReturn(true);

        java.util.Map<String, String> body = new HashMap<>();
        body.put("account", "admin");
        body.put("password", "right-pass");
        BusinessException e = assertThrows(BusinessException.class, () -> controller.login(body));
        assertTrue(e.getMessage().contains("锁定"));
        verify(userMapper, never()).selectOne(any()); // 锁定检查在查库之前，直接拒绝
    }

    @Test
    void lockDisabled_noRestriction() {
        TestLog.log("[测试] 锁定开关关闭：失败不计数不锁定，连错6次也只是普通密码错误");
        when(sysConfigService.getBool(eq("security.admin-login-lock.enabled"), eq(true))).thenReturn(false);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        // 开关关闭：失败不计数不锁定（第5次也只是普通密码错误）
        when(valueOperations.increment(anyString())).thenReturn(9L);
        for (int i = 0; i < 6; i++) {
            BusinessException e = assertThrows(BusinessException.class, () -> controller.login(wrongLogin()));
            assertEquals("用户名或密码错误", e.getMessage());
        }
        verify(valueOperations, never()).set(eq("adminlogin:lock:admin"), anyString(), anyLong(), any());
    }

    @Test
    void correctPassword_clearsFailCount() {
        TestLog.log("[测试] 密码正确：登录成功并清除历史失败计数");
        when(redisTemplate.hasKey("adminlogin:lock:admin")).thenReturn(false);

        java.util.Map<String, String> body = new HashMap<>();
        body.put("account", "admin");
        body.put("password", "right-pass");
        com.c2c.common.result.R<java.util.Map<String, Object>> res = controller.login(body);
        assertEquals(200, res.getCode());
        verify(redisTemplate).delete("adminlogin:fail:admin");
    }
}
