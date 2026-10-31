package com.c2c.common.infra;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis 连接状态测试（常规测试，不启动 Spring 容器；用原生 RESP 协议直连，不依赖客户端库）。
 *
 * <p>输出连接与命令的响应时间；Redis 不可达时测试失败，即"环境状态报告"。</p>
 *
 * <p>运行：mvn test -Dtest=RedisConnectionTest（需在项目根目录，读取 config/application-pro.yml）。
 * 输出同时写入 target/connection-test-output.txt（UTF-8）。</p>
 */
class RedisConnectionTest {

    private static TestEnv env;
    private static final int TIMEOUT_MS = 5000;

    @BeforeAll
    static void init() {
        env = TestEnv.load();
        TestEnv.Out.print("[env] config=" + env.configFile
                + " redis=" + env.redisHost + ":" + env.redisPort);
    }

    /** 建立 socket 连接（含 AUTH 鉴权），输出连接耗时 */
    private RedisSession connectWithTiming() throws IOException {
        long t0 = System.nanoTime();
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(env.redisHost, env.redisPort), TIMEOUT_MS);
        socket.setSoTimeout(TIMEOUT_MS);
        RedisSession session = new RedisSession(socket);
        if (!env.redisPassword.isEmpty()) {
            String auth = session.command("AUTH", env.redisPassword);
            assertTrue(auth.startsWith("+OK"), "AUTH 应返回 OK，实际: " + auth);
        }
        double ms = (System.nanoTime() - t0) / 1_000_000.0;
        TestEnv.Out.print(String.format("[Redis] 连接建立耗时: %.2f ms", ms));
        return session;
    }

    @Test
    void testPing() throws Exception {
        try (RedisSession session = connectWithTiming()) {
            long t0 = System.nanoTime();
            String reply = session.command("PING");
            double ms = (System.nanoTime() - t0) / 1_000_000.0;
            TestEnv.Out.print(String.format("[Redis] PING -> %s, 耗时: %.2f ms", reply, ms));
            assertEquals("+PONG", reply);
        }
    }

    @Test
    void testSetGetDel() throws Exception {
        String key = "test:conn:java";
        try (RedisSession session = connectWithTiming()) {
            long t0 = System.nanoTime();
            String setReply = session.command("SET", key, "hello-c2c");
            double setMs = (System.nanoTime() - t0) / 1_000_000.0;
            assertEquals("+OK", setReply);
            TestEnv.Out.print(String.format("[Redis] SET 耗时: %.2f ms", setMs));

            t0 = System.nanoTime();
            String value = session.command("GET", key);
            double getMs = (System.nanoTime() - t0) / 1_000_000.0;
            assertEquals("hello-c2c", value);
            TestEnv.Out.print(String.format("[Redis] GET 耗时: %.2f ms, value=%s", getMs, value));

            t0 = System.nanoTime();
            long del = session.commandInt("DEL", key);
            double delMs = (System.nanoTime() - t0) / 1_000_000.0;
            assertEquals(1, del);
            TestEnv.Out.print(String.format("[Redis] DEL 耗时: %.2f ms, deleted=%d", delMs, del));
        }
    }

    /** 最小 RESP 客户端：command 执行并返回结果（字符串回复含前缀如 +OK/-ERR，bulk 返回内容） */
    private static class RedisSession implements Closeable {
        private final Socket socket;
        private final BufferedWriter out;
        private final BufferedReader in;

        RedisSession(Socket socket) throws IOException {
            this.socket = socket;
            this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        }

        String command(String... args) throws IOException {
            StringBuilder sb = new StringBuilder();
            sb.append('*').append(args.length).append("\r\n");
            for (String arg : args) {
                sb.append('$').append(arg.getBytes(StandardCharsets.UTF_8).length).append("\r\n")
                        .append(arg).append("\r\n");
            }
            out.write(sb.toString());
            out.flush();
            return readReply();
        }

        long commandInt(String... args) throws IOException {
            return Long.parseLong(command(args).substring(1));
        }

        /** 读取一条 RESP 回复：简单/错误/整数直接读行；bulk 读取长度+内容 */
        private String readReply() throws IOException {
            String line = in.readLine();
            if (line == null) {
                throw new EOFException("Redis 连接已关闭");
            }
            if (line.startsWith("$")) {
                int len = Integer.parseInt(line.substring(1));
                if (len < 0) {
                    return null;
                }
                char[] buf = new char[len];
                int read = 0;
                while (read < len) {
                    int n = in.read(buf, read, len - read);
                    if (n < 0) {
                        throw new EOFException("Redis bulk 内容不完整");
                    }
                    read += n;
                }
                in.readLine(); // 丢弃末尾 CRLF
                return new String(buf);
            }
            return line;
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }
}
