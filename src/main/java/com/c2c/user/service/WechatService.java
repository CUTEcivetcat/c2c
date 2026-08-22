package com.c2c.user.service;

import com.c2c.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 微信小程序服务：code2session 换取 openid。
 * <p>mock-mode=true 时（本地开发无真实 AppSecret），根据 code 派生固定 openid，
 * 便于在微信开发者工具（测试号/体验号）中本地联调登录流程。</p>
 */
@Slf4j
@Service
public class WechatService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${wechat.appid:}")
    private String appid;

    @Value("${wechat.secret:}")
    private String secret;

    /** 开发模式开关：true 时派生 mock openid，便于本地测试 */
    @Value("${wechat.mock:false}")
    private boolean mock;

    /** 根据前端传来的 wx.login 的 code 换取 openid（+sessionKey） */
    public String code2Openid(String code) {
        if (mock) {
            // 本地测试：用 code 派生一个稳定 openid，避免依赖真实微信接口
            String openid = "mock_" + Integer.toHexString((code == null ? "dev" : code).hashCode());
            log.info("[mock wechat] code={} -> openid={}", code, openid);
            return openid;
        }
        if (appid == null || appid.trim().isEmpty() || secret == null || secret.trim().isEmpty()) {
            throw new BusinessException("微信登录未配置（缺少 appid/secret），且未开启 mock 模式");
        }
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + appid
                + "&secret=" + secret + "&js_code=" + code + "&grant_type=authorization_code";
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp == null) {
                throw new BusinessException("微信登录失败：无响应");
            }
            Object errcode = resp.get("errcode");
            if (errcode != null && !"0".equals(String.valueOf(errcode))) {
                throw new BusinessException("微信登录失败：" + resp.get("errmsg"));
            }
            String openid = (String) resp.get("openid");
            if (openid == null || openid.isEmpty()) {
                throw new BusinessException("微信登录失败：未获取到 openid");
            }
            return openid;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("wechat code2session failed", e);
            throw new BusinessException("微信登录失败，请稍后重试");
        }
    }
}