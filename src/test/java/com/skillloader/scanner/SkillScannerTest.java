package com.skillloader.scanner;

import com.skillloader.api.exceptions.SecurityException;
import com.skillloader.config.PathEntry;
import com.skillloader.config.PathType;
import com.skillloader.config.SecurityConfig;
import com.skillloader.model.Skill;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class SkillScannerTest {
    
    @TempDir
    Path tempDir;
    
    private void createSkill(Path dir, String name) throws IOException {
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
    
    @Test
    void shouldScanSingleSkill() throws Exception {
        createSkill(tempDir, "pdf");
        
        List<PathEntry> paths = List.of(
            new PathEntry("test", tempDir.toString(), 10, false, PathType.FILESYSTEM)
        );
        SkillScanner scanner = new SkillScanner(paths, SecurityConfig.defaults(), "SKILL.md");
        
        List<Skill> skills = scanner.scanAll();
        
        assertThat(skills).hasSize(1);
        assertThat(skills.get(0).name()).isEqualTo("pdf");
    }
    
    @Test
    void shouldScanMultipleSkills() throws Exception {
        createSkill(tempDir, "pdf");
        createSkill(tempDir, "weather");
        createSkill(tempDir, "chart");
        
        List<PathEntry> paths = List.of(
            new PathEntry("test", tempDir.toString(), 10, false, PathType.FILESYSTEM)
        );
        SkillScanner scanner = new SkillScanner(paths, SecurityConfig.defaults(), "SKILL.md");
        
        List<Skill> skills = scanner.scanAll();
        
        assertThat(skills).hasSize(3);
        assertThat(skills).extracting(Skill::name)
            .containsExactlyInAnyOrder("pdf", "weather", "chart");
    }
    
    @Test
    void shouldScanNestedDirectories() throws Exception {
        // 创建嵌套目录结构
        Path nested = tempDir.resolve("category");
        Files.createDirectories(nested);
        createSkill(nested, "nested-skill");
        
        List<PathEntry> paths = List.of(
            new PathEntry("test", tempDir.toString(), 10, false, PathType.FILESYSTEM)
        );
        SkillScanner scanner = new SkillScanner(paths, SecurityConfig.defaults(), "SKILL.md");
        
        List<Skill> skills = scanner.scanAll();
        
        assertThat(skills).hasSize(1);
        assertThat(skills.get(0).name()).isEqualTo("nested-skill");
    }
    
    @Test
    void shouldRespectMaxDepth() throws Exception {
        // 创建超过 maxDepth 的嵌套结构
        Path level1 = tempDir.resolve("level1");
        Path level2 = level1.resolve("level2");
        Path level3 = level2.resolve("level3");
        Path level4 = level3.resolve("level4");
        Files.createDirectories(level4);
        createSkill(level4, "deep-skill");
        
        SecurityConfig security = new SecurityConfig(true, false, 2); // maxDepth = 2
        List<PathEntry> paths = List.of(
            new PathEntry("test", tempDir.toString(), 10, false, PathType.FILESYSTEM)
        );
        SkillScanner scanner = new SkillScanner(paths, security, "SKILL.md");
        
        List<Skill> skills = scanner.scanAll();
        
        // depth > 2 的 skill 不应该被发现
        assertThat(skills).isEmpty();
    }
    
    @Test
    void shouldReturnEmptyListForNonExistentPath() {
        List<PathEntry> paths = List.of(
            new PathEntry("test", "/non/existent/path", 10, false, PathType.FILESYSTEM)
        );
        SkillScanner scanner = new SkillScanner(paths, SecurityConfig.defaults(), "SKILL.md");
        
        List<Skill> skills = scanner.scanAll();
        
        assertThat(skills).isEmpty();
    }
    
    @Test
    void shouldThrowExceptionForRequiredNonExistentPath() {
        List<PathEntry> paths = List.of(
            new PathEntry("test", "/non/existent/path", 10, true, PathType.FILESYSTEM)
        );
        SkillScanner scanner = new SkillScanner(paths, SecurityConfig.defaults(), "SKILL.md");
        
        assertThatThrownBy(scanner::scanAll)
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to scan required path");
    }
    
    @Test
    void shouldScanMultiplePaths() throws Exception {
        Path dir1 = tempDir.resolve("dir1");
        Path dir2 = tempDir.resolve("dir2");
        Files.createDirectories(dir1);
        Files.createDirectories(dir2);
        createSkill(dir1, "skill1");
        createSkill(dir2, "skill2");
        
        List<PathEntry> paths = List.of(
            new PathEntry("path1", dir1.toString(), 10, false, PathType.FILESYSTEM),
            new PathEntry("path2", dir2.toString(), 20, false, PathType.FILESYSTEM)
        );
        SkillScanner scanner = new SkillScanner(paths, SecurityConfig.defaults(), "SKILL.md");
        
        List<Skill> skills = scanner.scanAll();
        
        assertThat(skills).hasSize(2);
    }
    
    @Test
    void shouldSkipNonSkillDirectories() throws Exception {
        // 创建普通目录（不含 SKILL.md）
        Path regularDir = tempDir.resolve("regular-dir");
        Files.createDirectories(regularDir);
        Files.writeString(regularDir.resolve("some-file.txt"), "content");
        
        // 创建 skill 目录
        createSkill(tempDir, "valid-skill");
        
        List<PathEntry> paths = List.of(
            new PathEntry("test", tempDir.toString(), 10, false, PathType.FILESYSTEM)
        );
        SkillScanner scanner = new SkillScanner(paths, SecurityConfig.defaults(), "SKILL.md");
        
        List<Skill> skills = scanner.scanAll();
        
        assertThat(skills).hasSize(1);
        assertThat(skills.get(0).name()).isEqualTo("valid-skill");
    }
    
    @Test
    void shouldAssignCorrectSource() throws Exception {
        createSkill(tempDir, "test-skill");
        
        List<PathEntry> paths = List.of(
            new PathEntry("test", tempDir.toString(), 10, false, PathType.FILESYSTEM)
        );
        SkillScanner scanner = new SkillScanner(paths, SecurityConfig.defaults(), "SKILL.md");
        
        List<Skill> skills = scanner.scanAll();
        
        assertThat(skills.get(0).source()).isEqualTo(com.skillloader.model.SkillSource.PROJECT);
    }
    
    @Test
    void shouldPreservePriority() throws Exception {
        createSkill(tempDir, "test-skill");
        
        List<PathEntry> paths = List.of(
            new PathEntry("test", tempDir.toString(), 42, false, PathType.FILESYSTEM)
        );
        SkillScanner scanner = new SkillScanner(paths, SecurityConfig.defaults(), "SKILL.md");
        
        List<Skill> skills = scanner.scanAll();
        
        assertThat(skills.get(0).priority()).isEqualTo(42);
    }
    
    @Test
    void shouldParseNameAndDescriptionFromSkillMd() throws Exception {
        Path skillDir = tempDir.resolve("directory-name");
        Files.createDirectories(skillDir);
        String content = """
            ---
            name: custom-skill-name
            description: This is the custom description
            ---
            # Skill Content
            Test content.
            """;
        Files.writeString(skillDir.resolve("SKILL.md"), content);
        
        List<PathEntry> paths = List.of(
            new PathEntry("test", tempDir.toString(), 10, false, PathType.FILESYSTEM)
        );
        SkillScanner scanner = new SkillScanner(paths, SecurityConfig.defaults(), "SKILL.md");
        
        List<Skill> skills = scanner.scanAll();
        
        assertThat(skills).hasSize(1);
        assertThat(skills.get(0).name()).isEqualTo("custom-skill-name");
        assertThat(skills.get(0).description()).isEqualTo("This is the custom description");
    }
    
    @Test
    void shouldUseDirectoryNameWhenSkillMdHasNoName() throws Exception {
        Path skillDir = tempDir.resolve("fallback-name");
        Files.createDirectories(skillDir);
        String content = """
            ---
            description: Only description, no name
            ---
            # Skill Content
            """;
        Files.writeString(skillDir.resolve("SKILL.md"), content);
        
        List<PathEntry> paths = List.of(
            new PathEntry("test", tempDir.toString(), 10, false, PathType.FILESYSTEM)
        );
        SkillScanner scanner = new SkillScanner(paths, SecurityConfig.defaults(), "SKILL.md");
        
        List<Skill> skills = scanner.scanAll();
        
        assertThat(skills).hasSize(1);
        assertThat(skills.get(0).name()).isEqualTo("fallback-name");
        assertThat(skills.get(0).description()).isEqualTo("Only description, no name");
    }
