package com.c2c.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.InetAddress;

/**
 * 启动完成信息打印器：应用就绪后，在控制台输出访问地址、Swagger 文档、
 * 健康检查、上传/日志目录与环境提示，方便开发与部署后快速核对环境。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupInfoPrinter implements ApplicationRunner {

    private final Environment env;

    @Override
    public void run(ApplicationArguments args) {
        try {
            // Spring Boot 2.4+：spring.profiles.default 指定的 profile 属于"默认 profile"，
            // 不会出现在 getActiveProfiles() 里，需两者都取
            String[] active = env.getActiveProfiles();
            String[] defaults = env.getDefaultProfiles();
            String profile = active.length > 0 ? String.join(",", active)
                    : (defaults.length > 0 ? String.join(",", defaults) : "default");
            String port = env.getProperty("server.port", "8080");
            String ctx = env.getProperty("server.servlet.context-path", "");
            String uploadRoot = env.getProperty("app.upload-root", "./uploads");
            String logFile = env.getProperty("logging.file.name", "./logs/app.log");

            String localBase = "http://localhost:" + port + ctx;
            String lanIp = "localhost";
            try {
                lanIp = InetAddress.getLocalHost().getHostAddress();
            } catch (Exception ignored) {
                // 取不到局域网地址时仅展示 localhost
            }
            String lanBase = "http://" + lanIp + ":" + port + ctx;

            log.info("");
            log.info("================================================================");
            log.info("  闲小鱼 C2C 二手交易平台 启动完成");
            log.info("----------------------------------------------------------------");
            log.info("  环境 Profile : {}", profile);
            log.info("  本机访问     : {}/", localBase);
            log.info("  局域网访问   : {}/", lanBase);
            log.info("  Swagger 文档 : {}/swagger-ui/index.html", localBase);
            log.info("  API 文档 JSON: {}/v3/api-docs", localBase);
            log.info("  健康检查     : {}/actuator/health", localBase);
            log.info("  上传目录     : {}", uploadRoot);
            log.info("  日志文件     : {}", logFile);
            log.info("----------------------------------------------------------------");
            if (profile.contains("prod")) {
                log.info("  提示：当前为生产环境，请确认 config/ 外部配置已就位；");
                log.info("        公网仅开放 80/443/221，数据库/Redis/后端监听本机。");
            } else {
                log.info("  提示：本地开发模式，外部敏感配置来自 config/application-pro.yml");
                log.info("        （连接服务器数据库/缓存）；上传与日志走相对路径 ./uploads、./logs。");
            }
            log.info("================================================================");
            log.info("");
        } catch (Exception e) {
            log.warn("打印启动信息失败: {}", e.getMessage());
        }
    }
}
