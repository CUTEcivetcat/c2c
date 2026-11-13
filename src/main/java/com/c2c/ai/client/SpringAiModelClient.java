package com.c2c.ai.client;

import cn.hutool.core.util.StrUtil;
import com.c2c.ai.config.AiRuntimeConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Spring AI 模型客户端封装。
 *
 * <p>按 {@link AiRuntimeConfig} 运行时配置（sys_config 热更新）按需构建并缓存
 * {@link OpenAiChatModel}；配置指纹变化时重建。未配置 Base URL / Key 时不构建模型，
 * 上层据此降级本地规则引擎。</p>
 *
 * <p>支持纯文本对话与多模态图片识别；客服工具回调由调用方经 {@link #chatOptions} 注入。</p>
 *
 * @author wzh
 */
@Slf4j
@Component
public class SpringAiModelClient {

    private final AiRuntimeConfig config;

    /** 缓存的模型实例与其配置指纹 */
    private volatile OpenAiChatModel cachedModel;
    private volatile String cachedFingerprint;

    public SpringAiModelClient(AiRuntimeConfig config) {
        this.config = config;
    }

    /** 是否已配置可发起真实调用的模型 */
    public boolean configured() {
        return config.configured();
    }

    /**
     * 获取（按需构建/重建）聊天模型；未配置返回 null。
     */
    public OpenAiChatModel model() {
        if (!config.configured()) {
            return null;
        }
        String fingerprint = config.fingerprint();
        OpenAiChatModel local = cachedModel;
        if (local != null && fingerprint.equals(cachedFingerprint)) {
            return local;
        }
        synchronized (this) {
            if (cachedModel != null && fingerprint.equals(cachedFingerprint)) {
                return cachedModel;
            }
            OpenAiApi api = OpenAiApi.builder()
                    .baseUrl(config.baseUrl())
                    .apiKey(config.apiKey())
                    .restClientBuilder(restClientBuilder())
                    .build();
            OpenAiChatOptions defaults = OpenAiChatOptions.builder()
                    .model(config.model())
                    .build();
            cachedModel = OpenAiChatModel.builder()
                    .openAiApi(api)
                    .defaultOptions(defaults)
                    .build();
            cachedFingerprint = fingerprint;
            log.info("Spring AI 模型客户端已构建：model={}, baseUrl={}", config.model(), config.baseUrl());
            return cachedModel;
        }
    }

    /** 按配置超时构造 RestClient.Builder */
    private RestClient.Builder restClientBuilder() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeout = (int) Math.max(config.timeout(), 1000L);
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return RestClient.builder().requestFactory(factory);
    }

    /** 构造对话选项（温度/最大 token/工具回调/工具上下文） */
    public OpenAiChatOptions chatOptions(double temperature, int maxTokens,
                                         List<ToolCallback> callbacks, Map<String, Object> toolContext) {
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder()
                .model(config.model())
                .temperature(temperature)
                .maxTokens(maxTokens);
        if (callbacks != null && !callbacks.isEmpty()) {
            builder.toolCallbacks(callbacks);
        }
        if (toolContext != null && !toolContext.isEmpty()) {
            builder.toolContext(toolContext);
        }
        return builder.build();
    }

    /**
     * 纯文本对话：传入消息列表与选项，返回模型文本；未配置/异常返回 null。
     */
    public String chatText(List<Message> messages, OpenAiChatOptions options) {
        OpenAiChatModel model = model();
        if (model == null) {
            return null;
        }
        try {
            ChatResponse response = model.call(new Prompt(messages, options));
            if (response == null || response.getResult() == null) {
                return null;
            }
            String text = response.getResult().getOutput().getText();
            return StrUtil.isBlank(text) ? null : text.trim();
        } catch (Exception e) {
            log.warn("Spring AI 文本对话失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 多模态图片识别：系统提示词 + 商品图片，返回模型原始文本（JSON 字符串）；未配置/异常返回 null。
     */
    public String chatWithImage(String systemPrompt, String imageUrl) {
        OpenAiChatModel model = model();
        if (model == null || StrUtil.isBlank(imageUrl)) {
            return null;
        }
        try {
            Media media = Media.builder()
                    .mimeType(MimeTypeUtils.IMAGE_JPEG)
                    .data(URI.create(imageUrl))
                    .build();
            UserMessage userMessage = UserMessage.builder()
                    .text("请识别这张商品图片，按规则输出 JSON。")
                    .media(media)
                    .build();
            Prompt prompt = new Prompt(
                    List.of(new SystemMessage(systemPrompt), userMessage),
                    OpenAiChatOptions.builder()
                            .model(config.model())
                            .temperature(0.3)
                            .maxTokens(300)
                            .build());
            ChatResponse response = model.call(prompt);
            if (response == null || response.getResult() == null) {
                return null;
            }
            String content = response.getResult().getOutput().getText();
            if (StrUtil.isBlank(content)) {
                return null;
            }
            // 兼容部分模型输出带 ```json 包裹
            return content.replaceAll("(?s)^```(?:json)?\\s*|\\s*```$", "").trim();
        } catch (Exception e) {
            log.warn("Spring AI 多模态识别失败: {}", e.getMessage());
            return null;
        }
    }
}
