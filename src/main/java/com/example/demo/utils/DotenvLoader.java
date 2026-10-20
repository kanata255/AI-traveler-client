package com.example.demo.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 启动时加载项目根目录的 .env 文件到系统属性，供 Spring 占位符（如 ${DEEPSEEK_API_KEY}）解析。
 * Spring Boot 默认不读取 .env，这里在 SpringApplication.run 之前手动加载。
 */
public final class DotenvLoader {

    private static final Logger log = LoggerFactory.getLogger(DotenvLoader.class);

    private static final String DEFAULT_FILE = ".env";

    private DotenvLoader() {
    }

    public static void load() {
        Path path = Paths.get(System.getProperty("user.dir"), DEFAULT_FILE);
        if (!Files.isRegularFile(path)) {
            return;
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("读取 .env 失败: {}", path, e);
            return;
        }

        int loaded = 0;
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("export ")) {
                line = line.substring("export ".length()).trim();
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = line.substring(0, eq).trim();
            String value = stripQuotes(line.substring(eq + 1).trim());
            if (key.isEmpty()) {
                continue;
            }
            // 已存在的环境变量或系统属性优先，不覆盖
            if (System.getenv(key) != null || System.getProperty(key) != null) {
                log.warn(".env 中的 {} 已被环境变量/系统属性覆盖，跳过（如需改用 .env 请先移除该外部变量）", key);
                continue;
            }
            System.setProperty(key, value);
            loaded++;
        }
        log.info(".env 加载完成，共注入 {} 个变量", loaded);
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
