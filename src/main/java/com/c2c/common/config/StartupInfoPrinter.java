package com.c2c.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

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
            List<String> lanIps = detectLanIps();

            log.info("");
            log.info("================================================================");
            log.info("  闲小鱼 C2C 二手交易平台 启动完成");
            log.info("----------------------------------------------------------------");
            log.info("  环境 Profile : {}", profile);
            log.info("  本机访问     : {}/", localBase);
            if (lanIps.isEmpty()) {
                log.info("  局域网访问   : （未检测到网卡 IP）");
            } else {
                for (String ip : lanIps) {
                    log.info("  局域网访问   : http://{}:{}{}/", ip, port, ctx);
                }
            }
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

    /**
     * 自动识别所有可用网卡的 IPv4 局域网地址（跳过回环/虚拟/未启用网卡，
     * 过滤 169.254.x.x 链路本地地址），多网卡场景下列出全部可用 IP，
     * 并按「常见局域网网段优先」排序（192.168 > 10. > 172.16-31. > 其他）。
     */
    private List<String> detectLanIps() {
        List<String> ips = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while (nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }
                // 过滤虚拟网卡（VMware / VirtualBox / WSL / Hyper-V / 容器网桥等）
                String ifName = ni.getName().toLowerCase();
                String display = ni.getDisplayName() == null ? "" : ni.getDisplayName().toLowerCase();
                if (ifName.contains("vmnet") || ifName.contains("veth") || ifName.contains("wsl")
                        || display.contains("vmware") || display.contains("virtualbox") || display.contains("vbox")
                        || display.contains("hyper-v") || display.contains("vpn")) {
                    continue;
                }
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()
                            && !addr.getHostAddress().startsWith("169.254.")) {
                        ips.add(addr.getHostAddress());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("识别局域网 IP 失败: {}", e.getMessage());
        }
        // 按网段优先级排序：192.168 > 10. > 172.16~31 > 其他
        ips.sort((a, b) -> rank(a) - rank(b));
        return ips;
    }

    /** 局域网网段优先级（越小越靠前） */
    private int rank(String ip) {
        if (ip.startsWith("192.168.")) return 0;
        if (ip.startsWith("10.")) return 1;
        if (ip.matches("^172\\.(1[6-9]|2[0-9]|3[01])\\..*")) return 2;
        return 3;
    }
}
