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
            """.formatted(name, description, name, name);
        Files.writeString(skillDir.resolve("SKILL.md"), content);
    }
    
    @Test
    void shouldCompleteEndToEndWorkflow() throws Exception {
        // 1. 创建测试 skills
        createSkill(tempDir, "pdf", "PDF manipulation toolkit");
        createSkill(tempDir, "weather", "Weather forecast queries");
        
        // 2. 创建 loader（启用 generator 以支持写操作）
        SkillLoader loader = SkillLoader.builder()
            .addFilesystemPath("test", tempDir.toString())
            .enableGenerator()
            .build();
        
        // 3. 发现 skills
        List<Skill> skills = loader.discover();
        assertThat(skills).hasSize(2);
        
        // 4. 加载 skill
        SkillContent pdfContent = loader.load("pdf");
        assertThat(pdfContent.metadata().name()).isEqualTo("pdf");
        
        // 5. 生成 AGENTS.md
        String agentsMd = loader.generateAgentsMd();
        assertThat(agentsMd).contains("<skills_system");
        
        // 6. 同步到文件
        Path agentsFile = tempDir.resolve("AGENTS.md");
        loader.syncToFile(agentsFile);
        assertThat(agentsFile).exists();
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
        
        SkillLoader loader = SkillLoader.builder()
            .addPath(new com.skillloader.config.PathEntry("p1", dir1.toString(), 10, false, com.skillloader.config.PathType.FILESYSTEM))
            .addPath(new com.skillloader.config.PathEntry("p2", dir2.toString(), 20, false, com.skillloader.config.PathType.FILESYSTEM))
            .build();
        
        List<Skill> skills = loader.discover();
        
        // 应该只有 1 个 skill（去重）
        assertThat(skills).hasSize(1);
        
        // shared 应该来自 dir1（高优先级）
        SkillContent shared = loader.load("shared");
        assertThat(shared.metadata().description()).isEqualTo("From dir1");
    }
    
    @Test
    void shouldHandleComplexSkillWithResources() throws Exception {
        // 创建带资源的复杂 skill
        Path skillDir = tempDir.resolve("complex-skill");
        Files.createDirectories(skillDir);
        Files.createDirectories(skillDir.resolve("references"));
        Files.createDirectories(skillDir.resolve("scripts"));
        
        String content = """
            ---
            name: complex-skill
            description: A complex skill
            ---
            # Complex Skill
            """;
        Files.writeString(skillDir.resolve("SKILL.md"), content);
        Files.writeString(skillDir.resolve("references/doc.md"), "# Documentation");
        Files.writeString(skillDir.resolve("scripts/setup.sh"), "#!/bin/bash");
        
        SkillLoader loader = SkillLoader.builder()
            .addFilesystemPath("test", tempDir.toString())
            .build();
        
        List<Skill> skills = loader.discover();
        assertThat(skills).hasSize(1);
        
        SkillContent skillContent = loader.load("complex-skill");
        assertThat(skillContent.resources()).hasSize(2);
    }
}
