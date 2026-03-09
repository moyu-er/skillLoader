package com.skillloader.parser;

import com.skillloader.api.exceptions.SkillParseException;
import com.skillloader.model.SkillContent;
import com.skillloader.model.SkillMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class SimpleYamlParserTest {
    
    @TempDir
    Path tempDir;
    
    private SimpleYamlParser createParser() {
        return new SimpleYamlParser("SKILL.md", "UTF-8");
    }
    
    private void createSkillFile(Path skillDir, String content) throws Exception {
        Files.createDirectories(skillDir);
        Files.writeString(skillDir.resolve("SKILL.md"), content);
    }
    
    @Test
    void shouldParseValidSkill() throws Exception {
        Path skillDir = tempDir.resolve("test-skill");
        createSkillFile(skillDir, """
            ---
            name: test-skill
            description: A test skill
            context: testing
            tags: [java, test]
            ---
            # Test Skill
            This is the content.
            """);
        
        SimpleYamlParser parser = createParser();
        SkillContent content = parser.parse(skillDir);
        
        assertThat(content.metadata().name()).isEqualTo("test-skill");
        assertThat(content.metadata().description()).isEqualTo("A test skill");
        assertThat(content.metadata().context()).hasValue("testing");
        assertThat(content.metadata().tags()).containsExactly("java", "test");
        assertThat(content.markdownContent()).contains("# Test Skill");
    }
    
    @Test
    void shouldThrowWhenSkillFileNotFound() {
        Path skillDir = tempDir.resolve("non-existent");
        SimpleYamlParser parser = createParser();
        
        assertThatThrownBy(() -> parser.parse(skillDir))
            .isInstanceOf(SkillParseException.class)
            .hasMessageContaining("SKILL.md not found");
    }
    
    @Test
    void shouldThrowWhenMissingFrontmatter() throws Exception {
        Path skillDir = tempDir.resolve("bad-skill");
        createSkillFile(skillDir, "# No frontmatter\nJust content");
        
        SimpleYamlParser parser = createParser();
        
        assertThatThrownBy(() -> parser.parse(skillDir))
            .isInstanceOf(SkillParseException.class)
            .hasMessageContaining("missing YAML frontmatter");
    }
    
    @Test
    void shouldThrowWhenMissingName() throws Exception {
        Path skillDir = tempDir.resolve("no-name");
        createSkillFile(skillDir, """
            ---
            description: Missing name
            ---
            # Content
            """);
        
        SimpleYamlParser parser = createParser();
        
        assertThatThrownBy(() -> parser.parse(skillDir))
            .isInstanceOf(SkillParseException.class)
            .hasMessageContaining("Missing required field: name");
    }
    
    @Test
    void shouldParseMinimalSkill() throws Exception {
        Path skillDir = tempDir.resolve("minimal");
        createSkillFile(skillDir, """
            ---
            name: minimal
            ---
            # Minimal
            """);
        
        SimpleYamlParser parser = createParser();
        SkillContent content = parser.parse(skillDir);
        
        assertThat(content.metadata().name()).isEqualTo("minimal");
        assertThat(content.metadata().description()).isEmpty();
        assertThat(content.metadata().context()).isEmpty();
    }
    
    @Test
    void shouldParseQuotedValues() throws Exception {
        Path skillDir = tempDir.resolve("quoted");
        createSkillFile(skillDir, """
            ---
            name: "quoted-skill"
            description: 'A quoted description'
            ---
            # Content
            """);
        
        SimpleYamlParser parser = createParser();
        SkillContent content = parser.parse(skillDir);
        
        assertThat(content.metadata().name()).isEqualTo("quoted-skill");
        assertThat(content.metadata().description()).isEqualTo("A quoted description");
    }
    
    @Test
    void shouldExtractMarkdownContent() throws Exception {
        Path skillDir = tempDir.resolve("content-test");
        createSkillFile(skillDir, """
            ---
            name: content-test
            ---
            # Heading
            
            Paragraph with **bold** text.
            
            - List item 1
            - List item 2
            
            ```java
            code block
            ```
            """);
        
        SimpleYamlParser parser = createParser();
        SkillContent content = parser.parse(skillDir);
        
        assertThat(content.markdownContent()).contains("# Heading");
        assertThat(content.markdownContent()).contains("**bold**");
        assertThat(content.markdownContent()).contains("```java");
    }
    
    @Test
    void shouldParseMetadataOnly() throws Exception {
        Path skillDir = tempDir.resolve("metadata-only");
        createSkillFile(skillDir, """
            ---
            name: meta-test
            description: Just metadata
            ---
            # Content
            """);
        
        SimpleYamlParser parser = createParser();
        SkillMetadata metadata = parser.parseMetadata(skillDir);
        
        assertThat(metadata.name()).isEqualTo("meta-test");
        assertThat(metadata.description()).isEqualTo("Just metadata");
    }
    
    @Test
    void shouldIncludeExtraFields() throws Exception {
        Path skillDir = tempDir.resolve("extra");
        createSkillFile(skillDir, """
            ---
            name: extra-test
            custom-field: custom-value
            another: 123
            ---
            # Content
            """);
        
        SimpleYamlParser parser = createParser();
        SkillContent content = parser.parse(skillDir);
        
        assertThat(content.metadata().extra())
            .containsEntry("custom-field", "custom-value")
            .containsEntry("another", "123");
    }
    
    @Test
    void shouldDiscoverResources() throws Exception {
        Path skillDir = tempDir.resolve("resources");
        Files.createDirectories(skillDir.resolve("scripts"));
        Files.createFile(skillDir.resolve("scripts/test.py"));
        Files.createDirectories(skillDir.resolve("references"));
        Files.createFile(skillDir.resolve("references/doc.md"));
        
        createSkillFile(skillDir, """
            ---
            name: resource-test
            ---
            # Content
            """);
        
        SimpleYamlParser parser = createParser();
        SkillContent content = parser.parse(skillDir);
        
        assertThat(content.resources()).hasSize(2);
        assertThat(content.resources())
            .extracting(r -> r.name())
            .contains("test.py", "doc.md");
    }
    
    @Test
    void shouldHandleEmptyTags() throws Exception {
        Path skillDir = tempDir.resolve("empty-tags");
        createSkillFile(skillDir, """
            ---
            name: empty-tags
            tags: []
            ---
            # Content
            """);
        
        SimpleYamlParser parser = createParser();
        SkillContent content = parser.parse(skillDir);
        
        assertThat(content.metadata().tags()).isEmpty();
    }
}
