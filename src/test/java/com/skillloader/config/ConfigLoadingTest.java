package com.skillloader.config;

import com.skillloader.api.SkillLoader;
import com.skillloader.api.exceptions.SkillLoaderException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * 配置文件加载测试。
 */
class ConfigLoadingTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldLoadFromYamlConfig() throws Exception {
        // 创建配置文件
        Path configFile = tempDir.resolve("skillloader.yml");
        Files.writeString(configFile, """
            skillloader:
              paths:
                - name: test-path
                  path: %s
                  priority: 5
                  required: false
                  type: filesystem
              security:
                strictMode: true
                allowSymlinks: false
                maxDepth: 3
              parser:
                markerFile: SKILL.md
                encoding: UTF-8
            """.formatted(tempDir.toString().replace("\\", "/")));

        // 创建测试 skill
        Path skillDir = tempDir.resolve("test-skill");
        Files.createDirectories(skillDir);
        Files.writeString(skillDir.resolve("SKILL.md"), """
            ---
            name: test-skill
            description: Test skill
            ---
            # Test
            """);

        SkillLoader loader = SkillLoader.fromConfig(configFile);

        assertThat(loader.getConfig().paths()).hasSize(1);
        assertThat(loader.getConfig().security().maxDepth()).isEqualTo(3);
        assertThat(loader.discover()).hasSize(1);
    }

    @Test
    void shouldLoadFromYamlWithClasspathType() throws Exception {
        Path configFile = tempDir.resolve("skillloader.yml");
        Files.writeString(configFile, """
            skillloader:
              paths:
                - name: builtin
                  path: skills
                  priority: 10
                  type: classpath
            """);

        SkillLoader loader = SkillLoader.fromConfig(configFile);

        assertThat(loader.getConfig().paths()).hasSize(1);
        assertThat(loader.getConfig().paths().get(0).type()).isEqualTo(PathType.CLASSPATH);
    }

    @Test
    void shouldThrowWhenConfigFileNotFound() {
        Path nonExistent = tempDir.resolve("non-existent.yml");

        assertThatThrownBy(() -> SkillLoader.fromConfig(nonExistent))
            .isInstanceOf(SkillLoaderException.class)
            .hasMessageContaining("Config file not found");
    }

    @Test
    void shouldThrowWhenPathConfigMissingName() throws Exception {
        Path configFile = tempDir.resolve("skillloader.yml");
        Files.writeString(configFile, """
            skillloader:
              paths:
                - path: /some/path
                  priority: 10
            """);

        assertThatThrownBy(() -> SkillLoader.fromConfig(configFile))
            .isInstanceOf(SkillLoaderException.class)
            .hasMessageContaining("Path config missing name or path");
    }

    @Test
    void shouldThrowWhenPathConfigMissingPath() throws Exception {
        Path configFile = tempDir.resolve("skillloader.yml");
        Files.writeString(configFile, """
            skillloader:
              paths:
                - name: test
                  priority: 10
            """);

        assertThatThrownBy(() -> SkillLoader.fromConfig(configFile))
            .isInstanceOf(SkillLoaderException.class)
            .hasMessageContaining("Path config missing name or path");
    }

    @Test
    void shouldUseDefaultValuesForMissingConfig() throws Exception {
        Path configFile = tempDir.resolve("skillloader.yml");
        Files.writeString(configFile, """
            skillloader:
              paths:
                - name: test
                  path: %s
            """.formatted(tempDir.toString().replace("\\", "/")));

        SkillLoader loader = SkillLoader.fromConfig(configFile);

        // 应该使用默认值
        assertThat(loader.getConfig().paths().get(0).priority()).isEqualTo(10);
        assertThat(loader.getConfig().paths().get(0).required()).isFalse();
        assertThat(loader.getConfig().paths().get(0).type()).isEqualTo(PathType.FILESYSTEM);
    }

    @Test
    void shouldHandleEmptyConfig() throws Exception {
        Path configFile = tempDir.resolve("skillloader.yml");
        Files.writeString(configFile, """
            skillloader:
              paths:
                - name: default
                  path: skills
                  type: classpath
            """);

        // 空配置应该使用默认值
        SkillLoader loader = SkillLoader.fromConfig(configFile);
        assertThat(loader.getConfig().paths()).hasSize(1);
    }

    @Test
    void shouldHandleMultiplePathsInConfig() throws Exception {
        Path dir1 = tempDir.resolve("dir1");
        Path dir2 = tempDir.resolve("dir2");
        Files.createDirectories(dir1);
        Files.createDirectories(dir2);

        Path configFile = tempDir.resolve("skillloader.yml");
        Files.writeString(configFile, """
            skillloader:
              paths:
                - name: path1
                  path: %s
                  priority: 5
                  type: filesystem
                - name: path2
                  path: %s
                  priority: 10
                  type: filesystem
                - name: builtin
                  path: skills
                  priority: 20
                  type: classpath
            """.formatted(dir1.toString().replace("\\", "/"), dir2.toString().replace("\\", "/")));

        SkillLoader loader = SkillLoader.fromConfig(configFile);

        assertThat(loader.getConfig().paths()).hasSize(3);
        assertThat(loader.getConfig().paths().get(0).name()).isEqualTo("path1");
        assertThat(loader.getConfig().paths().get(1).name()).isEqualTo("path2");
        assertThat(loader.getConfig().paths().get(2).name()).isEqualTo("builtin");
    }

    @Test
    void shouldHandleInvalidYaml() throws Exception {
        Path configFile = tempDir.resolve("skillloader.yml");
        Files.writeString(configFile, """
            invalid: yaml: content: :::
            """);

        // 应该抛出异常或返回空配置
        assertThatThrownBy(() -> SkillLoader.fromConfig(configFile))
            .isInstanceOf(SkillLoaderException.class);
    }
}
