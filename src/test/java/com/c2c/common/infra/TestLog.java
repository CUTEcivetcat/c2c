package com.c2c.common.infra;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 测试提示输出：同时写控制台与 target/connection-test-output.txt（UTF-8）。
 *
 * <p>控制台编码受平台影响可能乱码，报告文件始终是 UTF-8；
 * 单元测试默认静默断言，用本工具打印"正在验证什么"，运行时可读、报告可留档。</p>
 */
public final class TestLog {

    private static final Path FILE = Paths.get("target", "connection-test-output.txt");
    private static boolean initialized = false;

    private TestLog() {
    }

    public static synchronized void log(String line) {
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
