package com.c2c.common.infra;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MySQL 数据库连接状态测试（常规 JDBC 测试，不启动 Spring 容器）。
 *
 * <p>展示部分业务数据（用户/商品/订单/系统配置各取前几行）并输出响应时间；
 * 数据库不可达时测试失败，即"环境状态报告"。</p>
 *
 * <p>运行：mvn test -Dtest=MySqlConnectionTest（需在项目根目录，读取 config/application-pro.yml）。
 * 输出同时写入 target/connection-test-output.txt（UTF-8），控制台乱码时以报告文件为准。</p>
 */
class MySqlConnectionTest {

    private static TestEnv env;

    @BeforeAll
    static void init() {
        env = TestEnv.load();
        TestEnv.Out.print("[env] mysql=" + env.mysqlHost + ":" + env.mysqlPort + "/" + env.mysqlDb
                + " redis=" + env.redisHost + ":" + env.redisPort);
    }

    /** 建立连接并输出连接耗时 */
    private Connection connectWithTiming() throws SQLException {
        long t0 = System.nanoTime();
        Connection c = DriverManager.getConnection(env.jdbcUrl(), env.mysqlUser, env.mysqlPassword);
        double ms = (System.nanoTime() - t0) / 1_000_000.0;
        TestEnv.Out.print(String.format("[MySQL] 连接建立耗时: %.2f ms", ms));
        return c;
    }

    @Test
    void testConnection() throws Exception {
        try (Connection c = connectWithTiming()) {
            assertTrue(c.isValid(5), "连接应在 5 秒内有效");
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT VERSION()")) {
                rs.next();
                TestEnv.Out.print("[MySQL] 版本: " + rs.getString(1));
            }
        }
    }

    @Test
    void testShowUsers() throws Exception {
        String sql = "SELECT id, username, nickname, role, admin_level, balance FROM user ORDER BY id LIMIT 5";
        try (Connection c = connectWithTiming();
             Statement st = c.createStatement()) {
            long t0 = System.nanoTime();
            try (ResultSet rs = st.executeQuery(sql)) {
                double ms = (System.nanoTime() - t0) / 1_000_000.0;
                TestEnv.Out.print("[user] id | username | nickname | role | admin_level | balance");
                int n = 0;
                while (rs.next()) {
                    n++;
                    TestEnv.Out.print(String.format("[user] %d | %s | %s | %d | %d | %s",
                            rs.getLong(1), rs.getString(2), rs.getString(3),
                            rs.getInt(4), rs.getInt(5), rs.getBigDecimal(6)));
                }
                TestEnv.Out.print(String.format("[MySQL] user 表查询 %d 行, 耗时: %.2f ms", n, ms));
                assertTrue(n > 0, "user 表应有数据");
            }
        }
    }

    @Test
    void testShowProducts() throws Exception {
        String sql = "SELECT id, title, price, status, view_count FROM product ORDER BY id LIMIT 5";
        try (Connection c = connectWithTiming();
             Statement st = c.createStatement()) {
            long t0 = System.nanoTime();
            try (ResultSet rs = st.executeQuery(sql)) {
                double ms = (System.nanoTime() - t0) / 1_000_000.0;
                TestEnv.Out.print("[product] id | title | price | status | view_count");
                int n = 0;
                while (rs.next()) {
                    n++;
                    TestEnv.Out.print(String.format("[product] %d | %s | %s | %d | %d",
                            rs.getLong(1), rs.getString(2), rs.getBigDecimal(3),
                            rs.getInt(4), rs.getInt(5)));
                }
                TestEnv.Out.print(String.format("[MySQL] product 表查询 %d 行, 耗时: %.2f ms", n, ms));
                assertTrue(n > 0, "product 表应有数据");
            }
        }
    }

    @Test
    void testShowOrders() throws Exception {
        String sql = "SELECT id, order_no, buyer_id, seller_id, total_amount, status FROM `order` ORDER BY id LIMIT 3";
        try (Connection c = connectWithTiming();
             Statement st = c.createStatement()) {
            long t0 = System.nanoTime();
            try (ResultSet rs = st.executeQuery(sql)) {
                double ms = (System.nanoTime() - t0) / 1_000_000.0;
                TestEnv.Out.print("[order] id | order_no | buyer | seller | total | status");
                int n = 0;
                while (rs.next()) {
                    n++;
                    TestEnv.Out.print(String.format("[order] %d | %s | %d | %d | %s | %d",
                            rs.getLong(1), rs.getString(2), rs.getLong(3),
                            rs.getLong(4), rs.getBigDecimal(5), rs.getInt(6)));
                }
                TestEnv.Out.print(String.format("[MySQL] order 表查询 %d 行, 耗时: %.2f ms", n, ms));
            }
        }
    }

    @Test
    void testShowSysConfig() throws Exception {
        String sql = "SELECT config_key, config_value FROM sys_config ORDER BY id";
        try (Connection c = connectWithTiming();
             Statement st = c.createStatement()) {
            long t0 = System.nanoTime();
            try (ResultSet rs = st.executeQuery(sql)) {
                double ms = (System.nanoTime() - t0) / 1_000_000.0;
                TestEnv.Out.print("[sys_config] key = value");
                int n = 0;
                while (rs.next()) {
                    n++;
                    TestEnv.Out.print(String.format("[sys_config] %s = %s", rs.getString(1), rs.getString(2)));
                }
                TestEnv.Out.print(String.format("[MySQL] sys_config 表查询 %d 行, 耗时: %.2f ms", n, ms));
                assertEquals(4, n, "sys_config 应为 4 行种子配置");
            }
        }
    }
}
