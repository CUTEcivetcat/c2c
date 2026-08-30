package com.c2c.user.controller;

import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import com.c2c.user.dto.*;
import com.c2c.user.entity.User;
import com.c2c.user.mapper.UserMapper;
import com.c2c.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 用户账号：验证码发送 / 登录 / 注册 / 找回密码 / 绑定手机 / 退出 / 个人信息 / 他人公开信息 /
 * 信誉分更新，以及管理端的用户列表、启禁用、总数统计。
 * 除公开接口（验证码/登录/注册/找回密码）外，均需登录；admin 开头接口需管理员 token。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "用户账号", description = "验证码 / 登录 / 注册 / 找回密码 / 绑定手机 / 个人信息 / 管理端用户管理")
public class UserController {

    private final UserService userService;
    private final StringRedisTemplate redisTemplate;
    private final UserMapper userMapper;

    /** 是否在响应中回显验证码（仅本地开发置 true，生产必须 false） */
    @org.springframework.beans.factory.annotation.Value("${app.sms-echo-code:false}")
    private boolean smsEchoCode;

    // ==================== 验证码 ====================

    /**
     * 发送验证码（智能识别手机号/邮箱）
     * 手机号 → 开发模式弹窗显示
     * 邮箱 → QQ邮箱SMTP发送
     * <p>安全说明：默认不回显验证码，仅当 app.sms-echo-code=true（本地开发）时返回 code 字段，生产必须保持 false。</p>
     */
    @Operation(summary = "发送验证码", description = "account 自动识别手机号或邮箱。验证码仅通过短信/邮件下发，生产环境不回显")
    @PostMapping(ApiPath.USER_SMS_SEND)
    public R<Map<String, String>> sendSms(@Parameter(description = "手机号或邮箱") @RequestParam("account") String account) {
        String code = userService.sendVerificationCodeAndReturn(account);
        Map<String, String> result = com.c2c.common.utils.MapUtils.of("type", account.contains("@") ? "email" : "sms");
        if (smsEchoCode) {
            result.put("code", code);
        }
        return R.ok(result);
    }

    // ==================== 登录 ====================

    /**
     * 统一登录入口
     * loginType=1 密码登录 / loginType=2 验证码登录
     * account 自动识别手机号或邮箱
     */
    @Operation(summary = "登录", description = "loginType=1 密码登录 / loginType=2 验证码登录；account 自动识别手机号或邮箱")
    @PostMapping(ApiPath.USER_LOGIN)
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        LoginVO vo;
        if (dto.getLoginType() != null && dto.getLoginType() == 2) {
            vo = userService.loginByCode(dto.getAccount(), dto.getSmsCode());
        } else {
            vo = userService.loginByPassword(dto.getAccount(), dto.getPassword());
        }
        return R.ok(vo);
    }

    // ==================== 注册 ====================

    @Operation(summary = "注册")
    @PostMapping(ApiPath.USER_REGISTER)
    public R<Void> register(@Valid @RequestBody RegisterDTO dto) {
        userService.register(dto);
        return R.ok();
    }

    // ==================== 微信登录 ====================

    @Operation(summary = "微信小程序登录", description = "前端 wx.login 的 code 登录，未注册则自动注册")
    @PostMapping(ApiPath.USER_WECHAT_LOGIN)
    public R<LoginVO> wechatLogin(@Valid @RequestBody WechatLoginDTO dto) {
        return R.ok(userService.loginByWechat(dto));
    }

    // ==================== 找回密码 ====================

    @Operation(summary = "找回密码", description = "通过验证码重置密码")
    @PostMapping(ApiPath.USER_RESET_PASSWORD)
    public R<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        userService.resetPassword(dto);
        return R.ok();
    }

    // ==================== 绑定手机号 ====================

    @Operation(summary = "绑定手机号")
    @PostMapping(ApiPath.USER_BIND_PHONE)
    public R<Void> bindPhone(@Valid @RequestBody BindPhoneDTO dto,
                             @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        userService.bindPhone(userId, dto.getPhone(), dto.getSmsCode());
        return R.ok();
    }

    // ==================== 绑定邮箱（微信登录后） ====================

    @Operation(summary = "绑定邮箱", description = "微信登录用户绑定邮箱，需先发送邮箱验证码")
    @PostMapping(ApiPath.USER_BIND_EMAIL)
    public R<Void> bindEmail(@Valid @RequestBody BindEmailDTO dto,
                             @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        userService.bindEmail(userId, dto.getEmail(), dto.getCode());
        return R.ok();
    }

    // ==================== 退出 ====================

    @Operation(summary = "退出登录", description = "将当前 token 加入注销黑名单")
    @PostMapping(ApiPath.USER_LOGOUT)
    public R<Void> logout(@RequestHeader("Authorization") String authHeader) {
        userService.logout(authHeader.substring(7));
        return R.ok();
    }

    // ==================== 个人信息 ====================

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping(ApiPath.USER_PROFILE)
    public R<UserVO> getProfile(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        return R.ok(userService.getProfile(userId));
    }

    @Operation(summary = "修改个人信息", description = "昵称 / 头像 / 性别 / 简介，均选填，只更新传入字段")
    @PutMapping(ApiPath.USER_PROFILE)
    public R<Void> updateProfile(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                 @Parameter(description = "昵称") @RequestParam(required = false) String nickname,
                                 @Parameter(description = "头像 URL") @RequestParam(required = false) String avatarUrl,
                                 @Parameter(description = "性别 0/1/2") @RequestParam(required = false) Integer gender,
                                 @Parameter(description = "个人简介") @RequestParam(required = false) String bio) {
        userService.updateProfile(userId, nickname, avatarUrl, gender, bio);
        return R.ok();
    }

    @Operation(summary = "查看他人公开信息", description = "用户主页：他人公开资料，无需登录")
    @GetMapping(ApiPath.USER_PROFILE_ID)
    public R<UserVO> getUserPublicInfo(@Parameter(description = "目标用户 ID") @PathVariable Long userId) {
        return R.ok(userService.getUserPublicInfo(userId));
    }

    // ==================== Admin 接口 ====================

    @Operation(summary = "用户分页列表（管理端）", description = "按手机号 / 邮箱 / 昵称模糊搜索")
    @GetMapping(ApiPath.USER_ADMIN_LIST)
    public R<Map<String, Object>> adminList(@Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword,
                                            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User> w =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (cn.hutool.core.util.StrUtil.isNotBlank(keyword)) {
            w.and(wr -> wr.like(User::getPhone, keyword).or().like(User::getEmail, keyword).or().like(User::getNickname, keyword));
        }
        w.orderByDesc(User::getCreatedAt);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> result =
                userMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size), w);
        return R.ok(com.c2c.common.utils.MapUtils.of("records", result.getRecords(), "total", result.getTotal(), "page", page, "size", size));
    }

    @Operation(summary = "用户总数统计（管理端）")
    @GetMapping(ApiPath.USER_ADMIN_COUNT)
    public R<Map<String, Long>> countUsers() {
        return R.ok(com.c2c.common.utils.MapUtils.of("total", userMapper.selectCount(null)));
    }
}
