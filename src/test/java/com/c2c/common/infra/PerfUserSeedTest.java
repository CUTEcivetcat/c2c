package com.c2c.common.infra;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * 压测账号批量造号器（不启动 Spring 容器，纯 JDBC，不依赖 Spring Test 装配）。
 *
 * <p>通过 {@link TestEnv} 读取 {@code config/application-pro.yml} 的 MySQL 连接配置直连数据库，
 * 按给定的昵称前缀与数量批量写入普通买家（{@code role=0, status=1}，即 token 生成器筛选口径），
 * 密码明文统一 {@code ww123456}，BCrypt 现场计算哈希。写完再跑 {@link PerfTokenGeneratorTest}
 * 即可把新号纳入压测 token CSV。</p>
 *
 * <p>两种入口：</p>
 * <ul>
 *   <li>直接运行本类 {@code main}：控制台交互填写昵称前缀、数量（空回车走默认值）；</li>
 *   <li>{@code mvn -o test -Dtest=PerfUserSeedTest}：非交互，读环境变量
 *       {@code PERF_SEED_PREFIX} / {@code PERF_SEED_COUNT}，缺省用默认值。</li>
 * </ul>
 *
 * <p>口径：手机号固定号段 {@code 1500000} + 4 位序号（15000000001 起），username 同手机号；
 * 幂等：每次先清理同号段旧压测号再插入，不触碰管理员/审核员/AI 助手账号。</p>
 *
 * @author wzh
 */
class PerfUserSeedTest {

    /** 默认昵称前缀 */
    private static final String DEFAULT_PREFIX = "压测用户";
    /** 默认数量 */
    private static final int DEFAULT_COUNT = 50;
    /** 手机号固定号段（7 位），拼 4 位序号得 11 位手机号 */
    private static final String PHONE_PREFIX = "1500000";
    /** 压测号明文密码（与演示账号一致） */
    private static final String PLAIN_PASSWORD = "ww123456";
    /** 数量上限（序号 4 位） */
    private static final int MAX_COUNT = 9999;
    /** nickname 列 VARCHAR(50)，预留序号最长 4 位 */
    private static final int MAX_PREFIX_LEN = 46;

    /** 非交互入口：环境变量覆盖默认值，便于 mvn test 批量执行 */
    @Test
    void seedPerfUsers() throws Exception {
        String prefix = System.getenv().getOrDefault("PERF_SEED_PREFIX", DEFAULT_PREFIX).trim();
        int count = parseCount(System.getenv().getOrDefault("PERF_SEED_COUNT", String.valueOf(DEFAULT_COUNT)));
        int inserted = seed(prefix, count);
        TestLog.log("[perf-seed] 完成，写入 " + inserted + " 个压测账号，前缀=" + prefix);
    }

    /** 交互入口：启动前提示填写昵称前缀与数量 */
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);

        System.out.print("请输入昵称前缀（回车默认 " + DEFAULT_PREFIX + "）：");
        String prefix = scanner.nextLine().trim();
        if (prefix.isEmpty()) {
            prefix = DEFAULT_PREFIX;
        }

        System.out.print("请输入数量（回车默认 " + DEFAULT_COUNT + "，上限 " + MAX_COUNT + "）：");
        String line = scanner.nextLine().trim();
        int count = line.isEmpty() ? DEFAULT_COUNT : parseCount(line);

        int inserted = new PerfUserSeedTest().seed(prefix, count);
        TestLog.log("[perf-seed] 完成，写入 " + inserted + " 个压测账号，前缀=" + prefix);
    }

    /** 校验参数并批量造号，返回实际插入行数 */
    private int seed(String prefix, int count) throws Exception {
        if (count < 1 || count > MAX_COUNT) {
            throw new IllegalArgumentException("数量需在 1~" + MAX_COUNT + " 之间，当前: " + count);
        }
        if (prefix.isEmpty()) {
            prefix = DEFAULT_PREFIX;
        }
        if (prefix.length() > MAX_PREFIX_LEN) {
            throw new IllegalArgumentException("昵称前缀过长（≤" + MAX_PREFIX_LEN + " 字符），当前 " + prefix.length() + " 字符");
        }

        String bcrypt = BCrypt.hashpw(PLAIN_PASSWORD, BCrypt.gensalt(10));
        TestEnv env = TestEnv.load();
        TestLog.log("[perf-seed] config=" + env.configFile
                + " mysql=" + env.mysqlHost + ":" + env.mysqlPort + "/" + env.mysqlDb
                + " count=" + count + " prefix=" + prefix);

        String deleteSql = "DELETE FROM `user` WHERE `phone` LIKE ? AND `role` = 0";
        String insertSql = "INSERT INTO `user` " +
                "(`username`,`password`,`phone`,`email`,`email_verified`,`login_source`," +
                "`avatar_url`,`nickname`,`bio`,`gender`,`status`,`role`," +
                "`admin_level`,`admin_perms`,`token_version`,`reputation_score`,`balance`) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (Connection c = DriverManager.getConnection(env.jdbcUrl(), env.mysqlUser, env.mysqlPassword)) {
            c.setAutoCommit(false);
            try (Statement ignore = c.createStatement()) {
                ignore.execute("SET NAMES utf8mb4");
            }
            try {
                // 清理上一批同号段压测号（仅普通用户）
                try (PreparedStatement del = c.prepareStatement(deleteSql)) {
                    del.setString(1, PHONE_PREFIX + "%");
                    int deleted = del.executeUpdate();
                    TestLog.log("[perf-seed] 清理旧压测号 " + deleted + " 个");
                }

                try (PreparedStatement ins = c.prepareStatement(insertSql)) {
                    for (int n = 1; n <= count; n++) {
                        String seq = String.format("%04d", n);
                        String phone = PHONE_PREFIX + seq;
                        String nickname = prefix + String.format("%03d", n);
                        ins.setString(1, phone);
                        ins.setString(2, bcrypt);
                        ins.setString(3, phone);
                        ins.setNull(4, Types.VARCHAR);
                        ins.setInt(5, 0);
                        ins.setString(6, "phone");
                        ins.setNull(7, Types.VARCHAR);
                        ins.setString(8, nickname);
                        ins.setString(9, "压测专用账号，可随时清理");
                        ins.setInt(10, 0);
                        ins.setInt(11, 1);
                        ins.setInt(12, 0);
                        ins.setInt(13, 0);
                        ins.setNull(14, Types.VARCHAR);
                        ins.setInt(15, 0);
                        ins.setBigDecimal(16, new BigDecimal("5.0"));
                        ins.setBigDecimal(17, new BigDecimal("0.00"));
                        ins.addBatch();
                    }
                    int[] results = ins.executeBatch();
                    c.commit();
                    TestLog.log(String.format("[perf-seed] 首号 %s（%s），末号 %s（%s）",
                            PHONE_PREFIX + "0001", prefix + "001",
                            PHONE_PREFIX + String.format("%04d", count),
                            prefix + String.format("%03d", count)));
                    return results.length;
                }
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** 解析数量，非法输入给出明确报错 */
    private static int parseCount(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("数量需为整数: " + raw);
        }
    }
}
