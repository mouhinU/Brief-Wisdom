package com.mouhin.brief.wisdom.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * SafePathUtils 单元测试
 *
 * @author Brief-Wisdom
 * @date 2026-07-08
 */
@DisplayName("SafePathUtils 路径安全测试")
class SafePathUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("允许访问白名单目录下的文件")
    void resolveAllowedPathWithinAllowedDir() {
        Path docsDir = tempDir.resolve("docs");
        assertDoesNotThrow(() -> java.nio.file.Files.createDirectories(docsDir));

        Path resolved = SafePathUtils.resolveAllowedPath(
                tempDir,
                "docs/readme.md",
                List.of("docs"),
                List.of()
        );

        assertEquals(docsDir.resolve("readme.md").normalize(), resolved.normalize());
    }

    @Test
    @DisplayName("拒绝路径穿越攻击")
    void rejectPathTraversal() {
        assertThrows(IllegalArgumentException.class, () ->
                SafePathUtils.resolveAllowedPath(
                        tempDir,
                        "../../etc/passwd",
                        List.of("docs"),
                        List.of()
                ));
    }

    @Test
    @DisplayName("允许访问白名单根目录文件")
    void resolveAllowedRootFile() {
        Path resolved = SafePathUtils.resolveAllowedPath(
                tempDir,
                "AGENTS.md",
                List.of("docs"),
                List.of("AGENTS.md")
        );

        assertEquals(tempDir.resolve("AGENTS.md").normalize(), resolved.normalize());
    }

    @Test
    @DisplayName("拒绝不在白名单内的路径")
    void rejectPathOutsideWhitelist() {
        assertThrows(IllegalArgumentException.class, () ->
                SafePathUtils.resolveAllowedPath(
                        tempDir,
                        "scripts/run.sh",
                        List.of("docs"),
                        List.of("AGENTS.md")
                ));
    }
}
