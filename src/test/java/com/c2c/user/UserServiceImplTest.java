package com.c2c.user;

import com.c2c.common.exception.BusinessException;
import com.c2c.review.mapper.NicknameAuditMapper;
import com.c2c.user.entity.User;
import com.c2c.user.mapper.UserMapper;
import com.c2c.user.service.EmailService;
import com.c2c.user.service.WechatService;
import com.c2c.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 绑定手机号服务层校验回归测试。
 *
 * @author wzh
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private EmailService emailService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private NicknameAuditMapper nicknameAuditMapper;
    @Mock
    private WechatService wechatService;

    @InjectMocks
    private UserServiceImpl userService;

    private static final Long USER_ID = 1L;
    private static final String PHONE = "13800000001";
    private static final String CODE = "123456";

    @BeforeEach
    void mockRedis() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void bindPhone_illegalFormat_rejected() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> userService.bindPhone(USER_ID, "12345", CODE));
        assertEquals("手机号格式不正确", e.getMessage());
        verify(userMapper, never()).selectCount(any());
    }

    @Test
    void bindPhone_codeExpired_rejected() {
        when(valueOps.get(anyString())).thenReturn(null);
        BusinessException e = assertThrows(BusinessException.class,
                () -> userService.bindPhone(USER_ID, PHONE, CODE));
        assertEquals("验证码已过期", e.getMessage());
    }

    @Test
    void bindPhone_occupiedByOther_rejected() {
        when(valueOps.get(anyString())).thenReturn(CODE);
        when(userMapper.selectCount(any())).thenReturn(1L);
        BusinessException e = assertThrows(BusinessException.class,
                () -> userService.bindPhone(USER_ID, PHONE, CODE));
        assertEquals("该手机号已被绑定", e.getMessage());
        verify(userMapper, never()).updateById(any());
    }

    @Test
    void bindPhone_selfNumber_successBound() {
        User user = new User();
        user.setId(USER_ID);
        when(valueOps.get(anyString())).thenReturn(CODE);
        // 排除自身后占用计数为 0：换绑成当前手机号不误报
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.selectById(USER_ID)).thenReturn(user);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        assertDoesNotThrow(() -> userService.bindPhone(USER_ID, PHONE, CODE));
        assertEquals(PHONE, user.getPhone());
        verify(redisTemplate).delete(anyString());
    }
}
