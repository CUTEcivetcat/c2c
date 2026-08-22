package com.c2c.user.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.c2c.common.exception.BusinessException;
import com.c2c.common.utils.JwtUtils;
import com.c2c.common.utils.SensitiveWordUtils;
import com.c2c.review.entity.NicknameAudit;
import com.c2c.review.mapper.NicknameAuditMapper;
import com.c2c.user.dto.LoginVO;
import com.c2c.user.dto.RegisterDTO;
import com.c2c.user.dto.ResetPasswordDTO;
import com.c2c.user.dto.UserVO;
import com.c2c.user.dto.WechatLoginDTO;
import com.c2c.user.entity.User;
import com.c2c.user.mapper.UserMapper;
import com.c2c.user.service.EmailService;
import com.c2c.user.service.UserService;
import com.c2c.user.service.WechatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现类
 * <p>实现验证码与密码登录、注册、找回密码、手机号绑定、退出登录及个人信息管理等能力。
 * 验证码与 token 状态存储于 Redis，密码使用 BCrypt 加密。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final EmailService emailService;
    private final StringRedisTemplate redisTemplate;
    private final NicknameAuditMapper nicknameAuditMapper;
    private final WechatService wechatService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Override
    public void sendVerificationCode(String account) {
        sendVerificationCodeAndReturn(account);
    }

    @Override
    public String sendVerificationCodeAndReturn(String account) {
        String code = String.valueOf((int) ((Math.random() * 900000) + 100000));
        String codeKey = "sms:code:" + account;
        redisTemplate.opsForValue().set(codeKey, code, 5, TimeUnit.MINUTES);

        if (isEmail(account)) {
            emailService.sendVerificationCode(account, code);
        }

        log.info("verification code generated: account={}, code={}", account, code);
        return code;
    }

    @Override
    @Transactional
    public LoginVO loginByCode(String account, String smsCode) {
        String codeKey = "sms:code:" + account;
        String cachedCode = redisTemplate.opsForValue().get(codeKey);
        if (StrUtil.isBlank(cachedCode)) {
            throw new BusinessException(1001, "验证码已过期");
        }
        if (!cachedCode.equals(smsCode)) {
            throw new BusinessException(1002, "验证码错误");
        }

        User user = findUserByAccount(account);
        if (user == null) {
            user = autoRegister(account);
        }

        redisTemplate.delete(codeKey);
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);
        log.info("user login success (code): userId={}, account={}", user.getId(), account);
        return buildLoginVO(user);
    }

    @Override
    public LoginVO loginByPassword(String account, String password) {
        User user = findUserByAccount(account);
        if (user == null) {
            log.warn("login failed: account not found, account={}", account);
            throw new BusinessException(isEmail(account) ? "邮箱未注册" : "手机号未注册");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            log.warn("login failed: account disabled, userId={}", user.getId());
            throw new BusinessException("账号已被封禁");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("login failed: wrong password, userId={}", user.getId());
            throw new BusinessException("密码错误");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);
        log.info("user login success: userId={}, account={}", user.getId(), account);
        return buildLoginVO(user);
    }

    @Override
    @Transactional
    public void register(RegisterDTO dto) {
        String account = StrUtil.isNotBlank(dto.getPhone()) ? dto.getPhone() : dto.getEmail();
        String codeKey = "sms:code:" + account;
        String cachedCode = redisTemplate.opsForValue().get(codeKey);
        if (StrUtil.isBlank(cachedCode)) {
            throw new BusinessException(1001, "验证码已过期");
        }
        if (!cachedCode.equals(dto.getSmsCode())) {
            throw new BusinessException(1002, "验证码错误");
        }
        if (findUserByAccount(account) != null) {
            throw new BusinessException("该账号已注册");
        }

        User user = new User();
        user.setUsername(account);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        if (isEmail(account)) {
            user.setEmail(account);
            user.setEmailVerified(1);
        } else {
            user.setPhone(account);
            user.setEmailVerified(0);
        }

        String seq = String.format("%05d", (userMapper.selectCount(null) + 1) % 100000);
        user.setNickname(StrUtil.isNotBlank(dto.getNickname()) ? dto.getNickname() : "user_" + seq);
        user.setBio(StrUtil.isNotBlank(dto.getBio()) ? dto.getBio() : "这个用户还没有填写简介。");
        user.setReputationScore(new BigDecimal("5.0"));
        user.setStatus(1);
        userMapper.insert(user);
        redisTemplate.delete(codeKey);
        log.info("user registered: account={}, nickname={}", account, user.getNickname());
    }

    @Override
    @Transactional
    public LoginVO loginByWechat(WechatLoginDTO dto) {
        String openid = wechatService.code2Openid(dto.getCode());
        // 已存在 openid 的用户直接登录，否则自动注册
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getOpenid, openid));
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setUsername("wx_" + openid.hashCode());
            user.setPassword(passwordEncoder.encode(generateRandomPassword()));
            user.setLoginSource("wechat");
            user.setNickname(StrUtil.isNotBlank(dto.getNickname()) ? dto.getNickname() : "微信用户");
            if (StrUtil.isNotBlank(dto.getAvatarUrl())) user.setAvatarUrl(dto.getAvatarUrl());
            user.setBio("这个用户还没有填写简介。");
            user.setReputationScore(new BigDecimal("5.0"));
            user.setStatus(1);
            userMapper.insert(user);
            log.info("wechat user auto registered: userId={}", user.getId());
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已被封禁");
        }
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);
        log.info("wechat login success: userId={}", user.getId());
        return buildLoginVO(user);
    }

    @Override
    @Transactional
    public void bindEmail(Long userId, String email, String code) {
        String codeKey = "sms:code:" + email;
        String cachedCode = redisTemplate.opsForValue().get(codeKey);
        if (StrUtil.isBlank(cachedCode)) {
            throw new BusinessException(1001, "验证码已过期");
        }
        if (!cachedCode.equals(code)) {
            throw new BusinessException(1002, "验证码错误");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 邮箱已被他人占用
        User exists = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email).ne(User::getId, userId));
        if (exists != null) {
            throw new BusinessException("该邮箱已被其他账号绑定");
        }
        user.setEmail(email);
        user.setEmailVerified(1);
        userMapper.updateById(user);
        redisTemplate.delete(codeKey);
        log.info("email bound: userId={}, email={}", userId, email);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordDTO dto) {
        String codeKey = "sms:code:" + dto.getAccount();
        String cachedCode = redisTemplate.opsForValue().get(codeKey);
        if (StrUtil.isBlank(cachedCode)) {
            throw new BusinessException(1001, "验证码已过期");
        }
        if (!cachedCode.equals(dto.getSmsCode())) {
            throw new BusinessException(1002, "验证码错误");
        }

        User user = findUserByAccount(dto.getAccount());
        if (user == null) {
            throw new BusinessException("账号不存在");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
        redisTemplate.delete(codeKey);
        log.info("password reset: account={}", dto.getAccount());
    }

    @Override
    @Transactional
    public void bindPhone(Long userId, String phone, String smsCode) {
        String codeKey = "sms:code:" + phone;
        String cachedCode = redisTemplate.opsForValue().get(codeKey);
        if (StrUtil.isBlank(cachedCode)) {
            throw new BusinessException(1001, "验证码已过期");
        }
        if (!cachedCode.equals(smsCode)) {
            throw new BusinessException(1002, "验证码错误");
        }

        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (count > 0) {
            throw new BusinessException("该手机号已被绑定");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPhone(phone);
        userMapper.updateById(user);
        redisTemplate.delete(codeKey);
        log.info("phone bound: userId={}, phone={}", userId, phone);
    }

    @Override
    public void logout(String token) {
        long remainingTime = 0;
        try {
            remainingTime = JwtUtils.parseToken(token, jwtSecret).getExpiration().getTime() - System.currentTimeMillis();
        } catch (Exception ignored) {
        }
        if (remainingTime > 0) {
            redisTemplate.opsForValue().set("logout:token:" + token, "1", Duration.ofMillis(remainingTime));
        }
    }

    @Override
    public UserVO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toUserVO(user);
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, String nickname, String avatarUrl, Integer gender, String bio) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 昵称变更需审核：自动敏感词拦截 + 人工审核（审核通过后生效）
        if (StrUtil.isNotBlank(nickname)) {
            String newNick = nickname.trim();
            if (!newNick.equals(user.getNickname())) {
                if (newNick.length() > 20) {
                    throw new BusinessException("昵称最长 20 个字符");
                }
                String hit = SensitiveWordUtils.hit(newNick);
                if (hit != null) {
                    throw new BusinessException("昵称包含敏感词，请更换后再试");
                }
                if (user.getNicknameStatus() != null && user.getNicknameStatus() == 1) {
                    throw new BusinessException("已有昵称申请正在审核中，请耐心等待");
                }
                // 写入审核记录（状态 0 待审核）
                NicknameAudit audit = new NicknameAudit();
                audit.setUserId(userId);
                audit.setOldNickname(user.getNickname());
                audit.setNewNickname(newNick);
                audit.setStatus(0);
                nicknameAuditMapper.insert(audit);
                // 用户进入"昵称审核中"状态
                user.setNicknamePending(newNick);
                user.setNicknameStatus(1);
                log.info("nickname apply submitted: userId={}, new={}", userId, newNick);
            }
        }
        if (StrUtil.isNotBlank(avatarUrl)) {
            user.setAvatarUrl(avatarUrl);
        }
        if (StrUtil.isNotBlank(bio)) {
            user.setBio(bio);
        }
        if (gender != null) {
            user.setGender(gender);
        }
        userMapper.updateById(user);
    }

    @Override
    public UserVO getUserPublicInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return UserVO.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .reputationScore(user.getReputationScore())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private User findUserByAccount(String account) {
        if (isEmail(account)) {
            return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, account));
        }
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, account));
    }

    private User autoRegister(String account) {
        User user = new User();
        user.setUsername(account);
        user.setPassword(passwordEncoder.encode(generateRandomPassword()));

        if (isEmail(account)) {
            user.setEmail(account);
            user.setEmailVerified(1);
        } else {
            user.setPhone(account);
            user.setEmailVerified(0);
        }

        String seq = String.format("%05d", (userMapper.selectCount(null) + 1) % 100000);
        user.setNickname("user_" + seq);
        user.setBio("这个用户还没有填写简介。");
        user.setReputationScore(new BigDecimal("5.0"));
        user.setStatus(1);
        userMapper.insert(user);
        log.info("user auto registered: account={}, nickname={}", account, user.getNickname());
        return user;
    }

    private LoginVO buildLoginVO(User user) {
        // 普通登录也签发带角色标记的 token（role 0/1/2），
        // 供 /review/** 审核接口识别审核员（role=2）；旧 token 无该标记需重新登录。
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", String.valueOf(user.getRole() == null ? 0 : user.getRole()));
        String token = JwtUtils.createToken(user.getId(), claims, jwtSecret, jwtExpiration);
        String refreshToken = JwtUtils.createToken(user.getId(), jwtSecret, 2592000L);
        redisTemplate.opsForValue().set("refresh:token:" + user.getId(), refreshToken, Duration.ofDays(30));
        return LoginVO.builder()
                .token(token)
                .refreshToken(refreshToken)
                .expiresIn(jwtExpiration)
                .userInfo(toUserVO(user))
                .build();
    }

    private UserVO toUserVO(User user) {
        return UserVO.builder()
                .id(user.getId())
                .phone(desensitizePhone(user.getPhone()))
                .email(desensitizeEmail(user.getEmail()))
                .nickname(user.getNickname())
                .nicknamePending(user.getNicknamePending())
                .nicknameStatus(user.getNicknameStatus())
                .bio(user.getBio())
                .avatarUrl(user.getAvatarUrl())
                .gender(user.getGender())
                .role(user.getRole())
                .loginSource(user.getLoginSource())
                .reputationScore(user.getReputationScore())
                .balance(user.getBalance())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private boolean isEmail(String account) {
        return StrUtil.isNotBlank(account) && account.contains("@");
    }

    private String generateRandomPassword() {
        return "c2c_" + String.format("%08d", (int) (Math.random() * 100000000));
    }

    private String desensitizePhone(String phone) {
        if (StrUtil.isBlank(phone) || phone.length() < 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private String desensitizeEmail(String email) {
        if (StrUtil.isBlank(email) || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String name = parts[0];
        if (name.length() <= 2) {
            return "*@" + parts[1];
        }
        return name.charAt(0) + "***@" + parts[1];
    }
}
