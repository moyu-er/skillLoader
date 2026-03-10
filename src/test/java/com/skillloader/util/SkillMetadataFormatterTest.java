package com.skillloader.util;

import com.skillloader.model.Skill;
import com.skillloader.model.SkillContent;
import com.skillloader.model.SkillMetadata;
import com.skillloader.model.SkillSource;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SkillMetadataFormatter 测试。
 */
class SkillMetadataFormatterTest {
    
    @Test
    void shouldFormatSkillWithDefaults() {
        Skill skill = new Skill(
            "git-workflow",
            "Git workflow guidelines",
            SkillSource.PROJECT,
            Paths.get("/skills/git-workflow"),
            10
        );
        
        String result = SkillMetadataFormatter.format(skill);
        
        assertThat(result).isEqualTo("- **git-workflow**: Git workflow guidelines");
    }
    
    @Test
    void shouldOmitEmptyDescription() {
        Skill skill = new Skill(
            "empty-desc",
            "",
            SkillSource.PROJECT,
            Paths.get("/skills/empty-desc"),
            10
        );
        
        String result = SkillMetadataFormatter.format(skill);
        
        assertThat(result).isEqualTo("- **empty-desc**");
    }
    
    @Test
    void shouldFormatToSystemPrompt() {
        List<Skill> skills = List.of(
            new Skill("git-workflow", "Git guidelines", SkillSource.PROJECT, Paths.get("/a"), 10),
            new Skill("python", "Python style", SkillSource.CLASSPATH, Paths.get("/b"), 20)
        );
        
        String result = SkillMetadataFormatter.toSystemPrompt(skills, "可用 Skills");
        
        assertThat(result).contains("## 可用 Skills");
        assertThat(result).contains("- **git-workflow**: Git guidelines");
        assertThat(result).contains("- **python**: Python style");
    }
    
    @Test
    void shouldFormatMetadataWithSmartOmission() {
        // tags 为空，应该自动省略
        SkillMetadata metadata = new SkillMetadata(
            "test-skill",
            "Test description",
            null,  // context 为空
            List.of(),  // tags 为空
            java.util.Map.of()
        );
        
        String result = SkillMetadataFormatter.format(metadata);
        
        assertThat(result).contains("- **test-skill**: Test description");
        assertThat(result).doesNotContain("tags");  // 空 tags 被省略
        assertThat(result).doesNotContain("context");  // 空 context 被省略
    }
    
    @Test
    void shouldFormatMetadataWithTags() {
        SkillMetadata metadata = new SkillMetadata(
            "test-skill",
            "Test description",
            "coding",
            List.of("java", "style"),
            java.util.Map.of("author", "team")
        );
        
        String result = SkillMetadataFormatter.format(metadata);
        
        assertThat(result).contains("- **test-skill**: Test description");
        assertThat(result).contains("tags: java, style");
    }
    
    @Test
    void shouldUseCustomFormatOptions() {
        SkillMetadata metadata = new SkillMetadata(
            "test",
            "desc",
            null,
            List.of("a", "b"),
            java.util.Map.of()
        );
        
        SkillMetadataFormatter.FormatOptions options = SkillMetadataFormatter.FormatOptions.builder()
            .namePrefix("[")
            .nameSuffix("]")
            .separator(" → ")
            .tagSeparator(" | ")
            .includeContext(true)  // 即使 includeContext=true，null 也不会显示
            .build();
        
        String result = SkillMetadataFormatter.format(metadata, options);
        
        assertThat(result).isEqualTo("[test] → desc | tags: a | b");
    }
    
    @Test
    void shouldUseCustomFormatter() {
        Skill skill = new Skill(
            "custom",
            "Custom format",
            SkillSource.PROJECT,
            Paths.get("/custom"),
            10
        );
        
        String result = SkillMetadataFormatter.format(skill, s -> 
            String.format("📦 %s: %s (%s)", s.name(), s.description(), s.source())
        );
        
        assertThat(result).isEqualTo("📦 custom: Custom format (PROJECT)");
    }
    
    @Test
    void shouldFormatDetailed() {
        Skill skill = new Skill(
            "git-workflow",
            "Git guidelines",
            SkillSource.PROJECT,
            Paths.get("/skills/git-workflow"),
            5
        );
        
        String result = SkillMetadataFormatter.formatDetailed(skill);
        
        assertThat(result).contains("git-workflow");
        assertThat(result).contains("Git guidelines");
        assertThat(result).contains("PROJECT");
        assertThat(result).contains("priority=5");
    }
}
