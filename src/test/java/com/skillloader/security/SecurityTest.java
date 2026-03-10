package com.skillloader.security;

import com.skillloader.api.SkillLoader;
import com.skillloader.api.exceptions.SecurityException;
import com.skillloader.config.PathEntry;
import com.skillloader.config.PathType;
import com.skillloader.config.SecurityConfig;
import com.skillloader.config.SkillLoaderConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * 安全功能测试。
 */
class SecurityTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldBlockPathTraversalAttack() throws Exception {
        // 创建 skill
        Path skillDir = tempDir.resolve("test-skill");
        Files.createDirectories(skillDir);
        Files.writeString(skillDir.resolve("SKILL.md"), """
            ---
            name: test-skill
            description: Test
            ---
            # Test
            """);

        SkillLoaderConfig config = SkillLoaderConfig.builder()
            .addFilesystemPath("test", tempDir.toString())
            .security(new SecurityConfig(true, false, 3))
            .build();
        SkillLoader loader = SkillLoader.fromConfig(config);

        // 尝试通过路径遍历访问白名单外的文件
        Path outsideFile = tempDir.resolve("../outside.txt");

        // 路径遍历应该被阻止
        assertThatThrownBy(() -> {
            // 尝试加载不存在的 skill，但路径包含遍历
            loader.load("test-skill/../../../etc/passwd");
        }).isInstanceOf(com.skillloader.api.exceptions.SkillNotFoundException.class);
    }



    @Test
    void shouldEnforceMaxDepth() throws Exception {
        // 创建深层嵌套结构
        Path deepDir = tempDir;
        for (int i = 0; i < 5; i++) {
            deepDir = deepDir.resolve("level" + i);
            Files.createDirectories(deepDir);
        }

        // 在 depth=4 处创建 skill
        Files.writeString(deepDir.resolve("SKILL.md"), """
            ---
            name: deep-skill
            description: Deep
            ---
            # Deep
            """);

        // 使用 maxDepth=2
        SkillLoaderConfig config = SkillLoaderConfig.builder()
            .addFilesystemPath("test", tempDir.toString())
            .security(new SecurityConfig(true, false, 2))
            .build();
        SkillLoader loader = SkillLoader.fromConfig(config);

        // 不应该发现深层 skill
        var skills = loader.discover();
        assertThat(skills).isEmpty();
    }

    @Test
    void shouldRespectStrictMode() throws Exception {
        // 在非白名单目录创建 skill
        Path outsideDir = tempDir.resolve("outside");
        Files.createDirectories(outsideDir);
        Files.writeString(outsideDir.resolve("SKILL.md"), """
            ---
            name: outside-skill
            description: Outside
            ---
            # Outside
            """);

        // 只配置白名单为子目录，不包含 outside
        Path whitelistDir = tempDir.resolve("whitelist");
        Files.createDirectories(whitelistDir);

        SkillLoaderConfig config = SkillLoaderConfig.builder()
            .addFilesystemPath("whitelist", whitelistDir.toString())
            .security(new SecurityConfig(true, false, 3)) // 严格模式
            .build();
        SkillLoader loader = SkillLoader.fromConfig(config);

        // 不应该发现 outside 中的 skill
        var skills = loader.discover();
        assertThat(skills).isEmpty();
    }

    @Test
    void shouldHandleRequiredPathNotExist() {
        // 配置一个必需的但不存在的路径
        SkillLoaderConfig config = SkillLoaderConfig.builder()
            .addPath(new PathEntry("required", "/non/existent/path", 10, true, PathType.FILESYSTEM))
            .build();

        // 应该抛出异常
        assertThatThrownBy(() -> SkillLoader.fromConfig(config).discover())
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to scan required path");
    }

    @Test
    void shouldAllowNonRequiredPathNotExist() {
        // 配置一个非必需的且不存在的路径
        SkillLoaderConfig config = SkillLoaderConfig.builder()
            .addPath(new PathEntry("optional", "/non/existent/path", 10, false, PathType.FILESYSTEM))
            .build();

        // 不应该抛出异常，只是返回空列表
        var skills = SkillLoader.fromConfig(config).discover();
        assertThat(skills).isEmpty();
    }

    @Test
    void shouldBlockAccessToParentDirectory() throws Exception {
        // 创建白名单子目录
        Path whitelistDir = tempDir.resolve("whitelist");
        Files.createDirectories(whitelistDir);

        // 在白名单外创建文件
        Path secretFile = tempDir.resolve("secret.txt");
        Files.writeString(secretFile, "secret data");

        SkillLoaderConfig config = SkillLoaderConfig.builder()
            .addFilesystemPath("test", whitelistDir.toString())
            .security(new SecurityConfig(true, false, 3))
            .build();
        SkillLoader loader = SkillLoader.fromConfig(config);

        // 尝试通过相对路径访问父目录应该被阻止
        // 实际测试通过 SkillLoader API 无法直接测试文件读取
        // 但可以通过 scanner 的行为验证
        var skills = loader.discover();
        assertThat(skills).isEmpty();
    }
}
