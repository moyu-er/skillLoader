package com.skillloader.api;

import com.skillloader.api.exceptions.SkillNotFoundException;
import com.skillloader.config.PathEntry;
import com.skillloader.config.PathType;
import com.skillloader.config.SkillLoaderConfig;
import com.skillloader.model.Skill;
import com.skillloader.model.SkillContent;
import com.skillloader.model.SkillMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class SkillLoaderTest {
    
    @TempDir
    Path tempDir;
    
    private void createSkill(Path dir, String name) throws Exception {
        Path skillDir = dir.resolve(name);
        Files.createDirectories(skillDir);
        String content = """
            ---
            name: %s
            description: Test %s skill
            ---
            # %s Skill
            Test content.
            """.formatted(name, name, name);
        Files.writeString(skillDir.resolve("SKILL.md"), content);
    }
    
    private SkillLoader createLoader() throws Exception {
        createSkill(tempDir, "pdf");
        createSkill(tempDir, "weather");
        
        return SkillLoader.builder()
            .addFilesystemPath("test", tempDir.toString())
            .build();
    }
    
    @Test
    void shouldDiscoverSkills() throws Exception {
        SkillLoader loader = createLoader();
        
        List<Skill> skills = loader.discover();
        
        assertThat(skills).hasSize(2);
    }
    
    @Test
    void shouldLoadSkill() throws Exception {
        SkillLoader loader = createLoader();
        
        SkillContent content = loader.load("pdf");
        
        assertThat(content.metadata().name()).isEqualTo("pdf");
        assertThat(content.markdownContent()).contains("# pdf Skill");
    }
    
    @Test
    void shouldThrowWhenSkillNotFound() throws Exception {
        SkillLoader loader = createLoader();
        
        assertThatThrownBy(() -> loader.load("non-existent"))
            .isInstanceOf(SkillNotFoundException.class)
            .hasMessageContaining("non-existent");
    }
    
    @Test
    void shouldGetMetadata() throws Exception {
        SkillLoader loader = createLoader();
        
        Optional<SkillMetadata> metadata = loader.getMetadata("pdf");
        
        assertThat(metadata).isPresent();
        assertThat(metadata.get().name()).isEqualTo("pdf");
    }
    
    @Test
    void shouldReturnEmptyMetadataForNonExistent() throws Exception {
        SkillLoader loader = createLoader();
        
        Optional<SkillMetadata> metadata = loader.getMetadata("non-existent");
        
        assertThat(metadata).isEmpty();
    }
    
    @Test
    void shouldGenerateAgentsMd() throws Exception {
        SkillLoader loader = createLoader();
        
        String agentsMd = loader.generateAgentsMd();
        
        assertThat(agentsMd).contains("<skills_system");
        assertThat(agentsMd).contains("<name>pdf</name>");
        assertThat(agentsMd).contains("<name>weather</name>");
    }
    
    @Test
    void shouldSyncToFile() throws Exception {
        SkillLoader loader = SkillLoader.builder()
            .addFilesystemPath("test", tempDir.toString())
            .enableGenerator()
            .build();
        createSkill(tempDir, "pdf");
        Path agentsFile = tempDir.resolve("AGENTS.md");
        
        loader.syncToFile(agentsFile);
        
        assertThat(agentsFile).exists();
        String content = Files.readString(agentsFile);
        assertThat(content).contains("<skills_system");
    }
    
    @Test
    void shouldUpdateFile() throws Exception {
        SkillLoader loader = SkillLoader.builder()
            .addFilesystemPath("test", tempDir.toString())
            .enableGenerator()
            .build();
        createSkill(tempDir, "pdf");
        Path agentsFile = tempDir.resolve("AGENTS.md");
        Files.writeString(agentsFile, "# Existing\n\nOld content");
        
        loader.updateFile(agentsFile);
        
        String content = Files.readString(agentsFile);
        assertThat(content).contains("# Existing");
        assertThat(content).contains("<skills_system");
    }
    
    @Test
    void shouldThrowWhenSyncToFileWithoutEnablingGenerator() throws Exception {
        SkillLoader loader = createLoader();
        Path agentsFile = tempDir.resolve("AGENTS.md");
        
        assertThatThrownBy(() -> loader.syncToFile(agentsFile))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("AGENTS.md generation is disabled");
    }
    
    @Test
    void shouldGetAllowedPaths() throws Exception {
        SkillLoader loader = createLoader();
        
        List<PathEntry> paths = loader.getAllowedPaths();
        
        assertThat(paths).hasSize(1);
        assertThat(paths.get(0).path()).isEqualTo(tempDir.toString());
    }
    
    @Test
    void shouldGetConfig() throws Exception {
        SkillLoader loader = createLoader();
        
        SkillLoaderConfig config = loader.getConfig();
        
        assertThat(config).isNotNull();
        assertThat(config.paths()).hasSize(1);
    }
    
    @Test
    void shouldCreateWithDefaultConfig() {
        SkillLoader loader = SkillLoader.createDefault();
        
        assertThat(loader).isNotNull();
        assertThat(loader.getConfig()).isNotNull();
    }
    
    @Test
    void shouldCreateWithBuilder() throws Exception {
        createSkill(tempDir, "builder-test");
        
        SkillLoader loader = SkillLoader.builder()
            .addFilesystemPath("fs", tempDir.toString())
            .build();
        
        assertThat(loader.discover()).hasSize(1);
        assertThat(loader.getAllowedPaths()).hasSize(1);
    }
    
    @Test
    void shouldCreateWithClasspathPath() throws Exception {
        SkillLoader loader = SkillLoader.builder()
            .addClasspathPath("cp", "skills")
            .build();
        
        // 从 classpath 扫描 skills（测试资源目录中有 10 个 skills）
        assertThat(loader.discover()).isNotEmpty();
        assertThat(loader.getAllowedPaths()).hasSize(1);
    }
    
    @Test
    void shouldReturnRegistry() throws Exception {
        SkillLoader loader = createLoader();
        
        assertThat(loader.registry()).isNotNull();
        assertThat(loader.registry().discover()).hasSize(2);
    }
    
    // ==================== 多路径配置测试 ====================
    
    @Test
    void shouldSupportMultipleFilesystemPaths() throws Exception {
        // 创建两个不同的目录
        Path dir1 = tempDir.resolve("skills1");
        Path dir2 = tempDir.resolve("skills2");
        Files.createDirectories(dir1);
        Files.createDirectories(dir2);
        
        createSkill(dir1, "skill-a");
        createSkill(dir2, "skill-b");
        
        SkillLoader loader = SkillLoader.builder()
            .addPath(new PathEntry("path1", dir1.toString(), 10, false, PathType.FILESYSTEM))
            .addPath(new PathEntry("path2", dir2.toString(), 20, false, PathType.FILESYSTEM))
            .build();
        
        List<Skill> skills = loader.discover();
        
        assertThat(skills).hasSize(2);
        assertThat(skills.stream().map(Skill::name)).containsExactlyInAnyOrder("skill-a", "skill-b");
    }
    
    @Test
    void shouldSupportMixedClasspathAndFilesystemPaths() throws Exception {
        // 文件系统路径
        createSkill(tempDir, "filesystem-skill");
        
        SkillLoader loader = SkillLoader.builder()
            .addPath(new PathEntry("fs", tempDir.toString(), 10, false, PathType.FILESYSTEM))
            .addPath(new PathEntry("cp", "skills", 20, false, PathType.CLASSPATH))
            .build();
        
        List<Skill> skills = loader.discover();
        
        // 应该包含文件系统路径的 skill 和 classpath 路径的 skills
        assertThat(skills).isNotEmpty();
        assertThat(skills.stream().map(Skill::name)).contains("filesystem-skill");
        // 也应该包含 classpath 中的一些 skills（如 pdf, weather 等）
        assertThat(skills.stream().map(Skill::name)).containsAnyOf("pdf", "weather", "chart");
    }
    
    @Test
    void shouldRespectPriorityWithMultiplePaths() throws Exception {
        // 创建两个路径，包含同名 skill
        Path dir1 = tempDir.resolve("high-priority");
        Path dir2 = tempDir.resolve("low-priority");
        Files.createDirectories(dir1);
        Files.createDirectories(dir2);
        
        // 同名 skill，不同描述
        createSkillWithDescription(dir1, "shared-skill", "From high priority");
        createSkillWithDescription(dir2, "shared-skill", "From low priority");
        
        // dir1 优先级高（数字小）
        SkillLoader loader = SkillLoader.builder()
            .addPath(new PathEntry("high", dir1.toString(), 5, false, PathType.FILESYSTEM))
            .addPath(new PathEntry("low", dir2.toString(), 10, false, PathType.FILESYSTEM))
            .build();
        
        List<Skill> skills = loader.discover();
        
        // 应该只有一个 skill（去重）
        assertThat(skills).hasSize(1);
        
        // 应该来自高优先级路径
        SkillContent content = loader.load("shared-skill");
        assertThat(content.metadata().description()).isEqualTo("From high priority");
    }
    
    @Test
    void shouldHandleMultipleClasspathPaths() throws Exception {
        // 配置多个 classpath 路径（虽然测试资源中只有 skills 目录有内容）
        SkillLoader loader = SkillLoader.builder()
            .addPath(new PathEntry("cp1", "skills", 10, false, PathType.CLASSPATH))
            .addPath(new PathEntry("cp2", "config", 20, false, PathType.CLASSPATH)) // 可能不存在
            .build();
        
        List<Skill> skills = loader.discover();
        
        // 至少应该发现 skills 目录中的内容
        assertThat(skills).isNotEmpty();
    }
    
    @Test
    void shouldGetAllowedPathsReturnAllConfiguredPaths() throws Exception {
        SkillLoader loader = SkillLoader.builder()
            .addPath(new PathEntry("fs1", tempDir.toString(), 10, false, PathType.FILESYSTEM))
            .addPath(new PathEntry("fs2", tempDir.toString(), 20, false, PathType.FILESYSTEM))
            .addPath(new PathEntry("cp", "skills", 30, false, PathType.CLASSPATH))
            .build();
        
        List<PathEntry> paths = loader.getAllowedPaths();
        
        assertThat(paths).hasSize(3);
        // 验证路径按优先级排序（数字小的在前）
        assertThat(paths.get(0).priority()).isEqualTo(10);
        assertThat(paths.get(1).priority()).isEqualTo(20);
        assertThat(paths.get(2).priority()).isEqualTo(30);
    }
    
    @Test
    void shouldHandleEmptyPathGracefully() throws Exception {
        // 一个路径有内容，一个路径为空
        Path emptyDir = tempDir.resolve("empty");
        Files.createDirectories(emptyDir);
        createSkill(tempDir, "only-skill");
        
        SkillLoader loader = SkillLoader.builder()
            .addPath(new PathEntry("with-content", tempDir.toString(), 10, false, PathType.FILESYSTEM))
            .addPath(new PathEntry("empty", emptyDir.toString(), 20, false, PathType.FILESYSTEM))
            .build();
        
        List<Skill> skills = loader.discover();
        
        assertThat(skills).hasSize(1);
        assertThat(skills.get(0).name()).isEqualTo("only-skill");
    }
    
    @Test
    void shouldDistinguishPathSources() throws Exception {
        createSkill(tempDir, "local-skill");
        
        SkillLoader loader = SkillLoader.builder()
            .addPath(new PathEntry("local", tempDir.toString(), 10, false, PathType.FILESYSTEM))
            .addPath(new PathEntry("builtin", "skills", 20, false, PathType.CLASSPATH))
            .build();
        
        List<Skill> skills = loader.discover();
        
        // 验证不同来源的 skill 有正确的 source 标记
        Skill localSkill = skills.stream()
            .filter(s -> s.name().equals("local-skill"))
            .findFirst()
            .orElseThrow();
        assertThat(localSkill.source()).isEqualTo(com.skillloader.model.SkillSource.PROJECT);
        
        Skill classpathSkill = skills.stream()
            .filter(s -> s.name().equals("pdf"))
            .findFirst()
            .orElseThrow();
        assertThat(classpathSkill.source()).isEqualTo(com.skillloader.model.SkillSource.CLASSPATH);
    }
    
    // 辅助方法
    private void createSkillWithDescription(Path dir, String name, String description) throws Exception {
        Path skillDir = dir.resolve(name);
        Files.createDirectories(skillDir);
        String content = """
            ---
            name: %s
            description: %s
            ---
            # %s Skill
            Test content.
            """.formatted(name, description, name);
        Files.writeString(skillDir.resolve("SKILL.md"), content);
    }
}
