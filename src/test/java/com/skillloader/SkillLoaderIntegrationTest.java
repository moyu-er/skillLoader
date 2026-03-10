package com.skillloader;

import com.skillloader.api.SkillLoader;
import com.skillloader.model.Skill;
import com.skillloader.model.SkillContent;
import com.skillloader.model.SkillMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * 集成测试 - 完整端到端流程。
 */
class SkillLoaderIntegrationTest {
    
    @TempDir
    Path tempDir;
    
    private void createSkill(Path dir, String name, String description) throws Exception {
        Path skillDir = dir.resolve(name);
        Files.createDirectories(skillDir);
        String content = """
            ---
            name: %s
            description: %s
            context: testing
            tags: [test, integration]
            ---
            # %s
            
            This is the content of %s skill.
            
            ## Usage
            
            ```java
            SkillLoader loader = SkillLoader.createDefault();
            ```
            """.formatted(name, description, name, name);
        Files.writeString(skillDir.resolve("SKILL.md"), content);
    }
    
    @Test
    void shouldCompleteEndToEndWorkflow() throws Exception {
        // 1. 创建测试 skills
        createSkill(tempDir, "pdf", "PDF manipulation toolkit");
        createSkill(tempDir, "weather", "Weather forecast queries");
        createSkill(tempDir, "chart", "Chart generation tools");
        
        // 2. 创建 loader
        SkillLoader loader = SkillLoader.builder()
            .addFilesystemPath("test", tempDir.toString())
            .build();
        
        // 3. 发现 skills
        List<Skill> skills = loader.discover();
        assertThat(skills).hasSize(3);
        
        // 4. 加载 skill
        SkillContent pdfContent = loader.load("pdf");
        assertThat(pdfContent.metadata().name()).isEqualTo("pdf");
        assertThat(pdfContent.metadata().description()).isEqualTo("PDF manipulation toolkit");
        assertThat(pdfContent.markdownContent()).contains("# pdf");
        
        // 5. 生成 AGENTS.md
        String agentsMd = loader.generateAgentsMd();
        assertThat(agentsMd).contains("<skills_system");
        assertThat(agentsMd).contains("<name>pdf</name>");
        assertThat(agentsMd).contains("<name>weather</name>");
        assertThat(agentsMd).contains("<name>chart</name>");
        
        // 6. 同步到文件
        Path agentsFile = tempDir.resolve("AGENTS.md");
        loader.syncToFile(agentsFile);
        assertThat(agentsFile).exists();
        String fileContent = Files.readString(agentsFile);
        assertThat(fileContent).contains("<skills_system");
        
        // 7. 验证元数据
        assertThat(loader.getMetadata("pdf")).isPresent();
        assertThat(loader.getMetadata("pdf").get().context()).hasValue("testing");
    }
    
    @Test
    void shouldWorkWithRealSkillsFromResources() throws Exception {
        // 使用项目内置的测试 resources 目录（文件系统路径）
        Path testResources = Paths.get("src/test/resources/skills");
        
        // 如果目录存在（在 IDE 或本地运行时）
        if (Files.exists(testResources)) {
            SkillLoader loader = SkillLoader.builder()
                .addFilesystemPath("resources", testResources.toString())
                .build();
            
            List<Skill> skills = loader.discover();
            
            // 应该能发现 resources 中的 skills
            assertThat(skills).isNotEmpty();
            
            // 尝试加载一个
            if (!skills.isEmpty()) {
                String firstSkillName = skills.get(0).name();
                SkillContent content = loader.load(firstSkillName);
                assertThat(content).isNotNull();
                assertThat(content.metadata()).isNotNull();
            }
        }
    }
    
    @Test
    void shouldHandleMultiplePathsWithPriority() throws Exception {
        // 创建两个路径，同名 skill
        Path dir1 = tempDir.resolve("dir1");
        Path dir2 = tempDir.resolve("dir2");
        Files.createDirectories(dir1);
        Files.createDirectories(dir2);
        
        createSkill(dir1, "shared", "From dir1");
        createSkill(dir2, "shared", "From dir2");
        createSkill(dir2, "unique", "Only in dir2");
        
        // dir1 优先级更高 (10 < 20)
        SkillLoader loader = SkillLoader.builder()
            .addPath(new com.skillloader.config.PathEntry("p1", dir1.toString(), 10, false, com.skillloader.config.PathType.FILESYSTEM))
            .addPath(new com.skillloader.config.PathEntry("p2", dir2.toString(), 20, false, com.skillloader.config.PathType.FILESYSTEM))
            .build();
        
        List<Skill> skills = loader.discover();
        
        // 应该有 2 个 skills（shared 去重 + unique）
        assertThat(skills).hasSize(2);
        
        // shared 应该来自 dir1（高优先级）
        SkillContent shared = loader.load("shared");
        assertThat(shared.metadata().description()).isEqualTo("From dir1");
    }
    
    @Test
    void shouldUpdateExistingAgentsMd() throws Exception {
        createSkill(tempDir, "skill1", "First skill");
        
        SkillLoader loader = SkillLoader.builder()
            .addFilesystemPath("test", tempDir.toString())
            .build();
        
        Path agentsFile = tempDir.resolve("AGENTS.md");
        
        // 第一次创建
        loader.syncToFile(agentsFile);
        String firstContent = Files.readString(agentsFile);
        assertThat(firstContent).contains("<name>skill1</name>");
        
        // 添加新 skill
        createSkill(tempDir, "skill2", "Second skill");
        
        // 刷新注册表
        loader.registry().refresh();
        
        // 更新文件
        loader.updateFile(agentsFile);
        String updatedContent = Files.readString(agentsFile);
        
        assertThat(updatedContent).contains("<name>skill1</name>");
        assertThat(updatedContent).contains("<name>skill2</name>");
    }
    
    @Test
    void shouldHandleComplexSkillWithResources() throws Exception {
        // 创建带资源的复杂 skill
        Path skillDir = tempDir.resolve("complex-skill");
        Files.createDirectories(skillDir);
        Files.createDirectories(skillDir.resolve("references"));
        Files.createDirectories(skillDir.resolve("scripts"));
        Files.createDirectories(skillDir.resolve("assets"));
        
        String content = """
            ---
            name: complex-skill
            description: A complex skill with resources
            tags: [complex, test]
            ---
            # Complex Skill
            
            This skill has resources.
            """;
        Files.writeString(skillDir.resolve("SKILL.md"), content);
        Files.writeString(skillDir.resolve("references/doc.md"), "# Documentation");
        Files.writeString(skillDir.resolve("scripts/setup.sh"), "#!/bin/bash");
        Files.writeString(skillDir.resolve("assets/icon.png"), "PNG data");
        
        SkillLoader loader = SkillLoader.builder()
            .addFilesystemPath("test", tempDir.toString())
            .build();
        
        List<Skill> skills = loader.discover();
        assertThat(skills).hasSize(1);
        
        SkillContent skillContent = loader.load("complex-skill");
        assertThat(skillContent.resources()).hasSize(3);
    }
}
