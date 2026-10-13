package com.c2c.ai.client;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.c2c.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 多模态大模型 HTTP 调用封装（OpenAI 兼容协议 {@code POST {baseUrl}/chat/completions}）。
 *
 * <p>豆包 / 通义 / DeepSeek 等兼容 OpenAI 协议且支持 vision 的模型均可接入，
 * 通过 {@code app.ai.base-url / api-key / model} 配置切换，代码无需改动。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiOpenAiClient {

    /** 系统提示词：强制只输出一个 JSON 对象，禁止编造不确定信息 */
    private static final String SYSTEM_PROMPT =
            "你是二手交易平台的智能发布助手。\n"
            + "根据用户提供的商品图片，识别物品类别、成色、品牌型号（能识别才写），"
            + "并给出适合发布的中文标题、描述、平台分类名与二手建议售价区间。\n"
            + "要求：\n"
            + "1. 只输出一个 JSON 对象，不要输出任何其他文字、解释或 Markdown。\n"
            + "2. JSON 字段：title（≤30字）、description（≤100字）、categoryName（匹配平台两级分类名，如\"教材教辅\"、\"数码电器\"下的子类）、"
            + "condition（1全新/2几乎全新/3轻微使用/4明显使用）、priceMin、priceMax（按二手行情给建议区间，数字）。\n"
            + "3. 不确定的信息用空字符串或 null，禁止编造。";

    private final AiProperties properties;

    /**
     * 调用多模态大模型，返回模型给出的原始 JSON 文本。
     *
     * @return 模型返回的 content（JSON 字符串）；调用失败 / 超时返回 null（由上层降级规则引擎）
     */
    public String chatWithImage(String imageUrl) {
        if (StrUtil.isBlank(properties.getApiKey()) || StrUtil.isBlank(properties.getBaseUrl())) {
            return null;
        }
        JSONObject body = new JSONObject();
        body.set("model", properties.getModel());
        body.set("max_tokens", 300);
        body.set("temperature", 0.3);

        JSONArray messages = new JSONArray();
        messages.add(new JSONObject().set("role", "system").set("content", SYSTEM_PROMPT));

        JSONObject userMsg = new JSONObject();
        userMsg.set("role", "user");
        JSONArray content = new JSONArray();
        content.add(new JSONObject().set("type", "text").set("text", "请识别这张商品图片，按规则输出 JSON。"));
        content.add(new JSONObject().set("type", "image_url").set("image_url",
                new JSONObject().set("url", imageUrl)));
        userMsg.set("content", content);
        messages.add(userMsg);
        body.set("messages", messages);

        try {
            String url = StrUtil.removeSuffix(properties.getBaseUrl(), "/") + "/chat/completions";
            HttpResponse resp = HttpRequest.post(url)
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout((int) properties.getTimeout())
                    .execute();
            String respBody = safeBody(resp);
            if (!resp.isOk() || StrUtil.isBlank(respBody)) {
                log.warn("AI 大模型调用失败: http={}, body={}", resp.getStatus(), StrUtil.maxLength(respBody, 200));
                return null;
            }
            JSONObject json = JSONUtil.parseObj(respBody);
            JSONArray choices = json.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                log.warn("AI 大模型响应缺少 choices: {}", StrUtil.maxLength(respBody, 200));
                return null;
            }
            String contentText = choices.getJSONObject(0).getJSONObject("message").getStr("content");
            if (StrUtil.isBlank(contentText)) {
                return null;
            }
            // 兼容部分模型输出带 ```json 包裹
            return contentText.replaceAll("(?s)^```(?:json)?\\s*|\\s*```$", "").trim();
        } catch (Exception e) {
            log.warn("AI 大模型调用异常: {}", e.getMessage());
            return null;
        }
    }

    /** 兜底：即使未配置 UTF-8，也保证字符串按 UTF-8 读取（防止中文乱码） */
    private String safeBody(HttpResponse resp) {
        return new String(resp.bodyBytes(), StandardCharsets.UTF_8);
    }
}
