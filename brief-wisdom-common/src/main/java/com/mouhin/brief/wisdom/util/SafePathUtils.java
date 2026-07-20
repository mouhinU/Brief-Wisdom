package com.mouhin.brief.wisdom.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * 安全路径解析工具，防止路径穿越攻击
 *
 * @author Brief-Wisdom
 * @date 2026-07-08
 */
public final class SafePathUtils {

    private SafePathUtils() {
    }

    /**
     * 解析相对路径并校验其位于 baseRoot 之下，且处于允许的目录或文件白名单内
     *
     * @param baseRoot     基础根目录（绝对路径）
     * @param relativePath 相对路径
     * @param allowedDirs  允许访问的相对目录列表（如 docs）
     * @param allowedFiles 允许访问的根目录相对文件列表（如 AGENTS.md）
     * @return 规范化后的安全绝对路径
     */
    public static Path resolveAllowedPath(Path baseRoot, String relativePath,
                                          List<String> allowedDirs, List<String> allowedFiles) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("路径不能为空");
        }

        Path normalizedBase = baseRoot.toAbsolutePath().normalize();
        Path requested = normalizedBase.resolve(relativePath).normalize();

        if (!requested.startsWith(normalizedBase)) {
            throw new IllegalArgumentException("非法路径: " + relativePath);
        }

        String relative = normalizedBase.relativize(requested).toString().replace('\\', '/');
        if (allowedFiles.contains(relative)) {
            return requested;
        }

        for (String allowedDir : allowedDirs) {
            String normalizedDir = allowedDir.replace('\\', '/');
            if (relative.equals(normalizedDir) || relative.startsWith(normalizedDir + "/")) {
                return requested;
            }
        }

        throw new IllegalArgumentException("路径不在允许的导入范围内: " + relativePath);
    }

    /**
     * 解析基础根目录
     *
     * @param configuredBaseDir 配置的根目录，为空时使用 user.dir
     * @return 规范化后的绝对路径
     */
    public static Path resolveBaseRoot(String configuredBaseDir) {
        if (configuredBaseDir == null || configuredBaseDir.isBlank()) {
            return Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        }
        return Paths.get(configuredBaseDir).toAbsolutePath().normalize();
    }

    /**
     * 将逗号分隔的配置字符串解析为列表
     *
     * @param csv 逗号分隔字符串
     * @return 去空白后的条目列表
     */
    public static List<String> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
