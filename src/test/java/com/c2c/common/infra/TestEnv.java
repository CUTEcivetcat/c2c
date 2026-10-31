package com.c2c.common.infra;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 测试环境参数读取：解析项目外部配置 config/application-pro.yml（当前激活 profile），
 * 提取 MySQL / Redis 连接参数（占位符 ${VAR:default} 优先取环境变量，其次默认值）。
 *
 * <p>常规测试类不走 Spring，连接信息从这里获取，保证"测的就是应用当前用的环境"。</p>
 */
class TestEnv {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([A-Z_]+)(?::([^}]*))?}");

    final String mysqlHost;
    final int mysqlPort;
    final String mysqlDb;
    final String mysqlUser;
    final String mysqlPassword;
    final String redisHost;
    final int redisPort;
    final String redisPassword;

    private TestEnv(String mysqlHost, int mysqlPort, String mysqlDb,
                    String mysqlUser, String mysqlPassword,
                    String redisHost, int redisPort, String redisPassword) {
        this.mysqlHost = mysqlHost;
        this.mysqlPort = mysqlPort;
        this.mysqlDb = mysqlDb;
        this.mysqlUser = mysqlUser;
        this.mysqlPassword = mysqlPassword;
        this.redisHost = redisHost;
        this.redisPort = redisPort;
        this.redisPassword = redisPassword;
    }

    String jdbcUrl() {
        return "jdbc:mysql://" + mysqlHost + ":" + mysqlPort + "/" + mysqlDb
                + "?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
    }

    static TestEnv load() {
        Path cfg = findConfig();
        Map<String, Map<String, String>> sections = parseYamlSections(cfg);
        Map<String, String> ds = sections.getOrDefault("datasource", new LinkedHashMap<>());
        Map<String, String> redis = sections.getOrDefault("redis", new LinkedHashMap<>());

        String url = resolve(ds.get("url"));
        Matcher m = Pattern.compile("jdbc:mysql://([^:/]+):(\\d+)/(\\w+)").matcher(url);
        if (!m.find()) {
            throw new IllegalStateException("无法从 datasource.url 解析连接参数: " + url);
        }
        return new TestEnv(
                m.group(1), Integer.parseInt(m.group(2)), m.group(3),
                resolve(ds.get("username")), resolve(ds.get("password")),
                resolve(redis.get("host")), Integer.parseInt(resolve(redis.get("port"))),
                resolve(redis.get("password")));
    }

    /** 从工作目录（项目根）或父目录定位外部配置 */
    private static Path findConfig() {
        String[] candidates = {
                "config/application-pro.yml",
                "../config/application-pro.yml"
        };
        for (String c : candidates) {
            Path p = Paths.get(c);
            if (Files.exists(p)) {
                return p;
            }
        }
        throw new IllegalStateException("未找到 config/application-pro.yml（请在项目根目录运行测试）");
    }

    /** 极简 YAML 节解析：只取 spring 下二级节（datasource/redis/mail）的 key: value */
    private static Map<String, Map<String, String>> parseYamlSections(Path cfg) {
        try {
            Map<String, Map<String, String>> sections = new LinkedHashMap<>();
            String current = null;
            for (String raw : Files.readAllLines(cfg, StandardCharsets.UTF_8)) {
                String line = raw.replace("\t", "    ");
                // 二级节：两个空格的 key:
                Matcher sec = Pattern.compile("^  (\\w+):\\s*$").matcher(line);
                if (sec.matches()) {
                    current = sec.group(1);
                    sections.putIfAbsent(current, new LinkedHashMap<>());
                    continue;
                }
                // 三级键：四个空格的 key: value
                Matcher kv = Pattern.compile("^    (\\w+):\\s*(.+)$").matcher(line);
                if (kv.matches() && current != null && !line.trim().startsWith("#")) {
                    sections.get(current).put(kv.group(1), kv.group(2).trim());
                }
            }
            return sections;
        } catch (IOException e) {
            throw new IllegalStateException("读取配置失败: " + cfg, e);
        }
    }

    /** 解析 ${VAR:default}：环境变量优先，其次默认值 */
    private static String resolve(String value) {
        if (value == null) {
            return "";
        }
        Matcher m = PLACEHOLDER.matcher(value);
        // 注意：Matcher.appendReplacement/Tail 的 StringBuilder 重载是 Java 9+ API，
        // 项目兼容 JDK8，此处必须用 StringBuffer
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String env = System.getenv(m.group(1));
            String def = m.group(2) == null ? "" : m.group(2);
            m.appendReplacement(sb, Matcher.quoteReplacement(env != null ? env : def));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * 测试输出：同时写到控制台与 target/connection-test-output.txt（UTF-8）。
     * 控制台编码受平台影响可能乱码，报告文件始终是 UTF-8。
     */
    static final class Out {
        private static final Path FILE = Paths.get("target", "connection-test-output.txt");
        private static boolean initialized = false;

        private Out() {
        }

        static synchronized void print(String line) {
            System.out.println(line);
            try {
                if (!initialized) {
                    Files.createDirectories(FILE.getParent());
                    Files.write(FILE, (line + "\n").getBytes(StandardCharsets.UTF_8));
                    initialized = true;
                } else {
                    Files.write(FILE, (line + "\n").getBytes(StandardCharsets.UTF_8),
                            StandardOpenOption.APPEND);
                }
            } catch (IOException ignored) {
                // 文件输出失败不影响测试
            }
        }
    }
}
