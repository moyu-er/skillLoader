package com.skillloader.reader;

import com.skillloader.api.exceptions.SecurityException;
import com.skillloader.config.PathEntry;
import com.skillloader.config.PathType;
import com.skillloader.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * 文件大小限制测试。
 */
class FileSizeLimitTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldBlockFileExceedingMaxSize() throws Exception {
        // 创建大文件 (1MB + 1 byte)
        long maxSize = 1024 * 1024; // 1MB
        byte[] largeContent = new byte[(int) (maxSize + 1)];

        Path skillDir = tempDir.resolve("large-skill");
        Files.createDirectories(skillDir);
        Files.write(skillDir.resolve("SKILL.md"), largeContent);

        PathEntry entry = new PathEntry("test", tempDir.toString(), 10, false, PathType.FILESYSTEM);
        FileSystemReader reader = new FileSystemReader(
            List.of(entry),
            new SecurityConfig(true, false, 3),
            maxSize,
            "UTF-8"
        );

        assertThatThrownBy(() -> reader.read(skillDir.resolve("SKILL.md")))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("File too large");
    }

    @Test
    void shouldAllowFileWithinMaxSize() throws Exception {
        // 创建小文件 (1KB)
        long maxSize = 1024 * 1024; // 1MB
        String smallContent = "Small content";

        Path skillDir = tempDir.resolve("small-skill");
        Files.createDirectories(skillDir);
        Files.writeString(skillDir.resolve("SKILL.md"), smallContent);

        PathEntry entry = new PathEntry("test", tempDir.toString(), 10, false, PathType.FILESYSTEM);
        FileSystemReader reader = new FileSystemReader(
            List.of(entry),
            new SecurityConfig(true, false, 3),
            maxSize,
            "UTF-8"
        );

        String content = reader.read(skillDir.resolve("SKILL.md"));
        assertThat(content).isEqualTo(smallContent);
    }

    @Test
    void shouldRespectCustomMaxSize() throws Exception {
        // 创建 10KB 文件
        long customMaxSize = 5 * 1024; // 5KB
        byte[] content = new byte[10 * 1024]; // 10KB

        Path skillDir = tempDir.resolve("custom-skill");
        Files.createDirectories(skillDir);
        Files.write(skillDir.resolve("SKILL.md"), content);

        PathEntry entry = new PathEntry("test", tempDir.toString(), 10, false, PathType.FILESYSTEM);
        FileSystemReader reader = new FileSystemReader(
            List.of(entry),
            new SecurityConfig(true, false, 3),
            customMaxSize,
            "UTF-8"
        );

        assertThatThrownBy(() -> reader.read(skillDir.resolve("SKILL.md")))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("File too large");
    }

    @Test
    void shouldHandleEmptyFile() throws Exception {
        Path skillDir = tempDir.resolve("empty-skill");
        Files.createDirectories(skillDir);
        Files.writeString(skillDir.resolve("SKILL.md"), ""); // 空文件

        PathEntry entry = new PathEntry("test", tempDir.toString(), 10, false, PathType.FILESYSTEM);
        FileSystemReader reader = new FileSystemReader(
            List.of(entry),
            new SecurityConfig(true, false, 3),
            1024 * 1024,
            "UTF-8"
        );

        String content = reader.read(skillDir.resolve("SKILL.md"));
        assertThat(content).isEmpty();
    }

    @Test
    void shouldHandleExactlyMaxSize() throws Exception {
        // 创建恰好等于 maxSize 的文件
        long maxSize = 1024; // 1KB
        byte[] content = new byte[(int) maxSize];

        Path skillDir = tempDir.resolve("exact-skill");
        Files.createDirectories(skillDir);
        Files.write(skillDir.resolve("SKILL.md"), content);

        PathEntry entry = new PathEntry("test", tempDir.toString(), 10, false, PathType.FILESYSTEM);
        FileSystemReader reader = new FileSystemReader(
            List.of(entry),
            new SecurityConfig(true, false, 3),
            maxSize,
            "UTF-8"
        );

        // 恰好等于 maxSize 应该被允许
        String result = reader.read(skillDir.resolve("SKILL.md"));
        assertThat(result.getBytes()).hasSize((int) maxSize);
    }
}
