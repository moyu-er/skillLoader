package com.skillloader;

import com.skillloader.api.SkillLoader;
import com.skillloader.config.*;
import com.skillloader.model.Skill;
import com.skillloader.model.SkillContent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * 复杂场景端到端测试。
 */
class SkillLoaderEndToEndTest {
    
    @TempDir
    Path tempDir;
    
    private void createComplexSkill(Path dir, String name, String description, 
                                     List<String> tags, String extraField) throws Exception {
        Path skillDir = dir.resolve(name);
        Files.createDirectories(skillDir);
        
        String tagsStr = tags.isEmpty() ? "[]" : "[" + String.join(", ", tags) + "]";
        
        String content = """
            ---
            name: %s
            description: %s
            context: production
            tags: %s
            extra-field: %s
            author: test-author
            version: 1.0.0
            ---
            # %s Skill
            
            %s
            
            ## Installation
            
            ```bash
            mvn install
            ```
            
            ## Usage
            
            ```java
            SkillLoader loader = SkillLoader.createDefault();
            loader.load("%s");
            ```
            
            ## Features
            
            - Feature 1
            - Feature 2
            - Feature 3
            
            ## API Reference
            
            See [references/api.md](references/api.md)
            """.formatted(name, description, tagsStr, extraField, name, description, name);
        
        Files.writeString(skillDir.resolve("SKILL.md"), content);
        
        // 添加资源文件
        Path refDir = skillDir.resolve("references");
        Files.createDirectories(refDir);
        Files.writeString(refDir.resolve("api.md"), "# API Documentation\n\nAPI details here.");
        
        Path scriptDir = skillDir.resolve("scripts");
        Files.createDirectories(scriptDir);
        Files.writeString(scriptDir.resolve("setup.sh"), "#!/bin/bash\necho 'Setup complete'");
    }
    
    @Test
    void shouldHandleComplexSkillWithResources() throws Exception {
        // 创建复杂 skill
        createComplexSkill(
            tempDir, 
            "complex-skill", 
            "A complex skill with many features",
            List.of("java", "complex", "test"),
            "extra-value"
        );
        
        SkillLoader loader = SkillLoader.builder()
            .addFilesystemPath("test", tempDir.toString())
            .build();
        
        // 发现
        List<Skill> skills = loader.discover();
        assertThat(skills).hasSize(1);
        
        // 加载
        SkillContent content = loader.load("complex-skill");
        
        // 验证元数据
        assertThat(content.metadata().name()).isEqualTo("complex-skill");
        assertThat(content.metadata().description()).isEqualTo("A complex skill with many features");
        assertThat(content.metadata().context()).hasValue("production");
        assertThat(content.metadata().tags()).containsExactly("java", "complex", "test");
        
        // 验证额外字段
        assertThat(content.metadata().extra())
            .containsEntry("extra-field", "extra-value")
            .containsEntry("author", "test-author")
            .containsEntry("version", "1.0.0");
        
        // 验证 Markdown 内容
        assertThat(content.markdownContent())
            .contains("# complex-skill Skill")
            .contains("## Installation")
            .contains("## Usage")
            .contains("## Features")
            .contains("## API Reference");
        
        // 验证资源
        assertThat(content.resources()).hasSize(2);
        assertThat(content.resources())
            .extracting(r -> r.name())
            .contains("api.md", "setup.sh");
    }
    
    @Test
    void shouldHandleMultipleComplexSkills() throws Exception {
        // 创建多个复杂 skills
        createComplexSkill(tempDir, "pdf-tool", "PDF processing", List.of("pdf", "document"), "pdf-extra");
        createComplexSkill(tempDir, "chart-gen", "Chart generation", List.of("chart", "viz"), "chart-extra");
        createComplexSkill(tempDir, "api-client", "API client", List.of("api", "http"), "api-extra");
        
        SkillLoader loader = SkillLoader.builder()
            .addFilesystemPath("test", tempDir.toString())
            .build();
        
        List<Skill> skills = loader.discover();
        assertThat(skills).hasSize(3);
        
        // 验证每个 skill 都能正确加载
        for (Skill skill : skills) {
            SkillContent content = loader.load(skill.name());
            assertThat(content.metadata()).isNotNull();
            assertThat(content.markdownContent()).isNotEmpty();
            assertThat(content.resources()).hasSize(2);
        }
    }
    
    @Test
    void shouldGenerateValidAgentsMd() throws Exception {
        createComplexSkill(tempDir, "skill-a", "Description A", List.of("tag-a"), "extra-a");
        createComplexSkill(tempDir, "skill-b", "Description B", List.of("tag-b"), "extra-b");
        
        SkillLoader loader = SkillLoader.builder()
            .addFilesystemPath("test", tempDir.toString())
            .build();
        
        String agentsMd = loader.generateAgentsMd();
        
        // 验证 XML 格式正确
        assertThat(agentsMd).contains("&lt;skills_system");
        assertThat(agentsMd).contains("&lt;/skills_system&gt;");
        assertThat(agentsMd).contains("&lt;available_skills&gt;");
        assertThat(agentsMd).contains("&lt;/available_skills&gt;");
        assertThat(agentsMd).contains("&lt;!-- SKILLS_TABLE_START --&gt;");
        assertThat(agentsMd).contains("&lt;!-- SKILLS_TABLE_END --&gt;");
        
        // 验证包含所有 skills
        assertThat(agentsMd).contains("&lt;name&gt;skill-a&lt;/name&gt;");
        assertThat(agentsMd).contains("&lt;name&gt;skill-b&lt;/name&gt;");
        assertThat(agentsMd).contains("&lt;description&gt;Description A&lt;/description&gt;");
        assertThat(agentsMd).contains("&lt;description&gt;Description B&lt;/description&gt;");
        
        // 验证包含使用说明
        assertThat(agentsMd).contains("&lt;usage&gt;");
        assertThat(agentsMd).contains("How to use skills");
    }
    
    @Test
    void shouldHandleNestedDirectoriesWithMaxDepth() throws Exception {
        // 创建嵌套目录结构
        Path level1 = tempDir.resolve("category1");
        Path level2 = level1.resolve("subcategory");
        Path level3 = level2.resolve("deep");
        Files.createDirectories(level3);
        
        createComplexSkill(level1, "level1-skill", "Level 1", List.of("l1"), "extra");
        createComplexSkill(level2, "level2-skill", "Level 2", List.of("l2"), "extra");
        createComplexSkill(level3, "level3-skill", "Level 3", List.of("l3"), "extra");
        
        // 使用 maxDepth = 2
        SkillLoaderConfig config = SkillLoaderConfig.builder()
            .addPath(new PathEntry("test", tempDir.toString(), 10, false, PathType.FILESYSTEM))
            .security(new SecurityConfig(true, false, 2))
            .build();
        
        SkillLoader loader = SkillLoader.fromConfig(config);
        
        List<Skill> skills = loader.discover();
        
        // 应该只能发现 level1 和 level2 (depth <= 2)
        assertThat(skills).hasSize(2);
        assertThat(skills).extracting(s -> s.name()).contains("level1-skill", "level2-skill");
    }
    
    @Test
    void shouldHandleEmptyAndMissingSkills() throws Exception {
        // 空目录
        SkillLoader loader = SkillLoader.builder()
            .addFilesystemPath("test", tempDir.toString())
            .build();
        
        List<Skill> skills = loader.discover();
        assertThat(skills).isEmpty();
        
        // 尝试加载不存在的 skill
        assertThatThrownBy(() -> loader.load("non-existent"))
            .isInstanceOf(com.skillloader.api.exceptions.SkillNotFoundException.class);
        
        // 生成空的 AGENTS.md
        String agentsMd = loader.generateAgentsMd();
        assertThat(agentsMd).contains("&lt;available_skills&gt;");
        assertThat(agentsMd).contains("&lt;/available_skills&gt;");
    }
    
    @Test
    void shouldMaintainPriorityOrder() throws Exception {
        Path highPriorityDir = tempDir.resolve("high");
        Path lowPriorityDir = tempDir.resolve("low");
        Files.createDirectories(highPriorityDir);
        Files.createDirectories(lowPriorityDir);
        
        // 同名 skill，不同描述
        createComplexSkill(highPriorityDir, "shared", "High Priority", List.of("high"), "high-extra");
        createComplexSkill(lowPriorityDir, "shared", "Low Priority", List.of("low"), "low-extra");
        createComplexSkill(lowPriorityDir, "unique", "Unique to low", List.of("unique"), "unique-extra");
        
        SkillLoaderConfig config = SkillLoaderConfig.builder()
            .addPath(new PathEntry("high", highPriorityDir.toString(), 5, false, PathType.FILESYSTEM))
            .addPath(new PathEntry("low", lowPriorityDir.toString(), 10, false, PathType.FILESYSTEM))
            .build();
        
        SkillLoader loader = SkillLoader.fromConfig(config);
        
        // 应该只有 2 个 skills（shared 去重 + unique）
        List<Skill> skills = loader.discover();
        assertThat(skills).hasSize(2);
        
        // shared 应该来自高优先级
        SkillContent shared = loader.load("shared");
        assertThat(shared.metadata().description()).isEqualTo("High Priority");
        assertThat(shared.metadata().tags()).contains("high");
    }
}
