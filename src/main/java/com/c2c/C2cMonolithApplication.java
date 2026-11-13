package com.c2c;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用启动类：聚合 admin / order / product / rating 等模块，作为单体应用统一启动入口。
 *
 * <p>JWT 密钥启动校验由 {@code JwtSecretCheckEnvironmentPostProcessor} 负责
 * （见 spring.factories 注册），会在 Spring 环境加载完成后校验，避免弱密钥上线。</p>
 *
 * <p>Spring AI 的 OpenAI/ChatClient 自动装配在此统一排除：模型实例由
 * {@code SpringAiModelClient} 按 sys_config 运行时配置手动构建，未配置 Key 时不强制建 bean，
 * 保留现有「留空走本地规则引擎」的降级设计。</p>
 *
 * @author wzh
 */
@MapperScan("com.c2c.**.mapper")
@SpringBootApplication(exclude = {
        org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration.class,
        org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration.class,
        org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration.class,
        org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration.class,
        org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration.class,
        org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration.class,
        org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration.class,
        org.springframework.ai.model.chat.memory.autoconfigure.ChatMemoryAutoConfiguration.class
})
@org.springframework.scheduling.annotation.EnableScheduling
public class C2cMonolithApplication {
    public static void main(String[] args) {
        SpringApplication.run(C2cMonolithApplication.class, args);
    }
}
