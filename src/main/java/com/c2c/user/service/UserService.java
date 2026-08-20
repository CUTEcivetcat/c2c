package com.c2c.user.service;

import com.c2c.user.dto.*;

/**
 * 用户服务接口
 * <p>覆盖用户账号全生命周期业务：验证码发送与登录、密码登录、注册、找回密码、
 * 手机号绑定、退出登录，以及个人信息的查询与修改等能力。</p>
 */
public interface UserService {

    /** 发送验证码（不返回）*/
    void sendVerificationCode(String account);

    /** 发送验证码并返回（开发模式） */
    String sendVerificationCodeAndReturn(String account);

    /** 验证码登录（不存在则自动注册）*/
    LoginVO loginByCode(String account, String smsCode);

    /** 密码登录（智能识别手机号/邮箱）*/
    LoginVO loginByPassword(String account, String password);

    /** 注册 */
    void register(RegisterDTO dto);

    /** 找回密码 */
    void resetPassword(ResetPasswordDTO dto);

    /** 绑定手机号 */
    void bindPhone(Long userId, String phone, String smsCode);

    /** 退出登录 */
    void logout(String token);

    /** 个人信息 */
    UserVO getProfile(Long userId);

    /** 修改个人信息（昵称、头像、性别、简介等，非空字段才会更新） */
    void updateProfile(Long userId, String nickname, String avatarUrl, Integer gender, String bio);

    /** 获取用户公开信息（供其他用户查看） */
    UserVO getUserPublicInfo(Long userId);
}



