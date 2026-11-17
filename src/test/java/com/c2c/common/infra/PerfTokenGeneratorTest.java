package com.c2c.common.infra;

import com.c2c.common.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 压测账号 / Token 生成器（M4-2 基线配套）。
 *
 * <p>用途：压测不再依赖单账号实时登录。脚本直接从库中读取可用普通买家，
 * 用当前 {@code jwt.secret} 复刻登录签发口径（claims: role / tv）签出 access token，
 * 导出成 JMeter CSV（{@code userId,token,account}），供 CSV Data Set 喂给鉴权头。</p>
 *
 * <p>口径与 {@code AuthTokenFilter} 校验保持一致：普通用户 token 不带 {@code type=refresh}，
 * 携带与库内一致的 {@code tv}（令牌版本），避免被版本比对判失效。</p>
 *
 * <p>运行（项目根目录，需可读 config/application-pro.yml 与 MySQL）：</p>
 * <pre>
 *   mvn test -Dtest=PerfTokenGeneratorTest
 * </pre>
 * 或直接运行本类 {@code main} 方法。产物：{@code JMeter/result/perf-tokens.csv}。
 *
 * @author wzh
 */
class PerfTokenGeneratorTest {

    /** 导出给 JMeter 的 CSV（相对项目根） */
    private static final Path CSV_PATH = Paths.get("JMeter", "result", "perf-tokens.csv");

    /** access token 有效期：7 天，压测 5 分钟内不会过期 */
    private static final long TTL_SECONDS = 604800L;

    /** AI 助手账号，不作为压测买家 */
    private static final long ASSISTANT_ID = 90000001L;

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([A-Z_]+)(?::([^}]*))?}");

    /** 一条导出记录 */
    private record Row(long userId, String account, String token) {
    }

    @Test
    void generatePerfTokens() throws Exception {
        List<Row> rows = generate();
        assertFalse(rows.isEmpty(), "应至少读到一个可用普通买家");
        TestLog.log("[perf] 共生成 " + rows.size() + " 个压测 token -> " + CSV_PATH);
        for (Row r : rows) {
            TestLog.log(String.format("[perf] userId=%d account=%s token=%s...",
                    r.userId(), r.account(), r.token().substring(0, Math.min(20, r.token().length()))));
        }
    }

    /** 主入口：IDE 里直接运行即可生成 CSV */
    public static void main(String[] args) throws Exception {
        new PerfTokenGeneratorTest().generate();
    }

    /** 读库签 token 并写 CSV，返回生成记录 */
    private List<Row> generate() throws Exception {
        TestEnv env = TestEnv.load();
        String secret = readJwtSecret();

        List<Row> rows = new ArrayList<>();
        try (Connection c = DriverManager.getConnection(env.jdbcUrl(), env.mysqlUser, env.mysqlPassword);
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT id, phone, email, role, token_version FROM user " +
                             "WHERE role = 0 AND status = 1 AND id <> " + ASSISTANT_ID +
                             " ORDER BY id")) {
            while (rs.next()) {
                long id = rs.getLong("id");
                String phone = rs.getString("phone");
                String email = rs.getString("email");
                int role = rs.getInt("role");
                int tv = rs.getInt("token_version");
                String account = phone != null && !phone.isBlank() ? phone : email;

                Map<String, Object> claims = new HashMap<>();
                claims.put("role", String.valueOf(role));
                claims.put("tv", tv);
                String token = JwtUtils.createToken(id, claims, secret, TTL_SECONDS);

                // 自检：签出的 token 可解析、subject 与 tv 一致
                Claims parsed = JwtUtils.parseToken(token, secret);
                assertTrue(id == Long.parseLong(parsed.getSubject()), "token subject 应等于用户 ID");
                assertEqualsTv(tv, parsed);
                assertNotNull(token);

                rows.add(new Row(id, account == null ? "" : account, token));
            }
        }

        Files.createDirectories(CSV_PATH.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(CSV_PATH, StandardCharsets.UTF_8)) {
            w.write("userId,token,account");
            w.newLine();
            for (Row r : rows) {
                w.write(r.userId() + "," + r.token() + "," + r.account());
                w.newLine();
            }
        }
        return rows;
    }

    /** 校验 token 内 tv 声明与库内令牌版本一致 */
    private void assertEqualsTv(int expectedTv, Claims claims) {
        Integer tokenTv = claims.get("tv", Integer.class);
        int actual = tokenTv == null ? 0 : tokenTv;
        assertTrue(expectedTv == actual, "token tv 应与库内 token_version 一致");
    }

    /** 从 config/application-pro.yml 读取 jwt.secret（解析 ${ENV:default}，环境变量优先） */
    private String readJwtSecret() throws Exception {
        Path cfg = Paths.get("config", "application-pro.yml");
        if (!Files.exists(cfg)) {
            throw new IllegalStateException("未找到 config/application-pro.yml");
        }
        boolean inJwt = false;
        for (String raw : Files.readAllLines(cfg, StandardCharsets.UTF_8)) {
            String line = raw.replace("\t", "    ");
            String trimmed = line.trim();
            if (trimmed.startsWith("#") || trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.equals("jwt:")) {
                inJwt = true;
                continue;
            }
            // 遇到顶格的新节，退出 jwt 节
            if (inJwt && line.matches("^[A-Za-z].*:")) {
                inJwt = false;
                continue;
            }
            if (inJwt && trimmed.startsWith("secret:")) {
                String value = trimmed.substring(trimmed.indexOf(':') + 1).trim();
                return resolvePlaceholder(value);
            }
        }
        throw new IllegalStateException("配置中未找到 jwt.secret");
    }

    /** 解析 ${VAR:default}：环境变量优先，其次默认值 */
    private String resolvePlaceholder(String value) {
        Matcher m = PLACEHOLDER.matcher(value);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String env = System.getenv(m.group(1));
            String def = m.group(2) == null ? "" : m.group(2);
            m.appendReplacement(sb, Matcher.quoteReplacement(env != null ? env : def));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
