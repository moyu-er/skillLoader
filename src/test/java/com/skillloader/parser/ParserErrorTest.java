package com.skillloader.parser;

import com.skillloader.api.exceptions.SkillParseException;
import com.skillloader.model.SkillMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * Parser 错误处理测试。
 */
class ParserErrorTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldThrowWhenSkillMdNotFound() throws Exception {
        Path skillDir = tempDir.resolve("empty-skill");
        Files.createDirectories(skillDir);
        // 不创建 SKILL.md

        SimpleYamlParser parser = new SimpleYamlParser("SKILL.md", "UTF-8");

        assertThatThrownBy(() -> parser.parse(skillDir))
            .isInstanceOf(SkillParseException.class)
            .hasMessageContaining("SKILL.md not found");
    }

    @Test
    void shouldThrowWhenMissingFrontmatter() throws Exception {
        Path skillDir = tempDir.resolve("no-frontmatter");
        Files.createDirectories(skillDir);
        Files.writeString(skillDir.resolve("SKILL.md"), """
            # No Frontmatter

            This skill has no YAML frontmatter.
            """);

        SimpleYamlParser parser = new SimpleYamlParser("SKILL.md", "UTF-8");

        assertThatThrownBy(() -> parser.parse(skillDir))
            .isInstanceOf(SkillParseException.class)
            .hasMessageContaining("missing YAML frontmatter");
    }

    @Test
    void shouldThrowWhenMissingName() throws Exception {
        Path skillDir = tempDir.resolve("no-name");
        Files.createDirectories(skillDir);
        Files.writeString(skillDir.resolve("SKILL.md"), """
            ---
            description: Missing name field
            ---
            # No Name
            """);

        SimpleYamlParser parser = new SimpleYamlParser("SKILL.md", "UTF-8");

        assertThatThrownBy(() -> parser.parse(skillDir))
            .isInstanceOf(SkillParseException.class)
            .hasMessageContaining("Missing required field: name");
    }

    @Test
    void shouldThrowWhenNameIsEmpty() throws Exception {
        Path skillDir = tempDir.resolve("empty-name");
        Files.createDirectories(skillDir);
        Files.writeString(skillDir.resolve("SKILL.md"), """
            ---
            name: ""
            description: Empty name
            ---
            # Empty Name
            """);

        SimpleYamlParser parser = new SimpleYamlParser("SKILL.md", "UTF-8");

        assertThatThrownBy(() -> parser.parse(skillDir))
            .isInstanceOf(SkillParseException.class)
            .hasMessageContaining("Missing required field: name");
    }

    @Test
    void shouldThrowWhenNameIsBlank() throws Exception {
        Path skillDir = tempDir.resolve("blank-name");
        Files.createDirectories(skillDir);
        Files.writeString(skillDir.resolve("SKILL.md"), """
            ---
            name: "   "
            description: Blank name
            ---
            # Blank Name
            """);

        SimpleYamlParser parser = new SimpleYamlParser("SKILL.md", "UTF-8");

        assertThatThrownBy(() -> parser.parse(skillDir))
            .isInstanceOf(SkillParseException.class)
            .hasMessageContaining("Missing required field: name");
    }

    @Test
    void shouldHandleInvalidYamlInFrontmatter() throws Exception {
        Path skillDir = tempDir.resolve("invalid-yaml");
        Files.createDirectories(skillDir);
        // 使用正确的 YAML 格式，但包含未闭合的列表
        Files.writeString(skillDir.resolve("SKILL.md"), """
            ---
            name: test
            description: invalid yaml
            tags: [unclosed
            ---
            # Invalid YAML
            """);

        SimpleYamlParser parser = new SimpleYamlParser("SKILL.md", "UTF-8");

        // 无效的 YAML 应该抛出异常
        assertThatThrownBy(() -> parser.parse(skillDir))
            .isInstanceOf(SkillParseException.class)
            .hasMessageContaining("Failed to parse YAML");
    }

    @Test
    void shouldHandleEmptyFrontmatter() throws Exception {
        Path skillDir = tempDir.resolve("empty-frontmatter");
        Files.createDirectories(skillDir);
        Files.writeString(skillDir.resolve("SKILL.md"), """
            ---
            ---
            # Empty Frontmatter
            """);

        SimpleYamlParser parser = new SimpleYamlParser("SKILL.md", "UTF-8");

        assertThatThrownBy(() -> parser.parse(skillDir))
            .isInstanceOf(SkillParseException.class)
            .hasMessageContaining("missing YAML frontmatter");
    }

    @Test
    void shouldParseMetadataOnly() throws Exception {
        Path skillDir = tempDir.resolve("metadata-only");
        Files.createDirectories(skillDir);
        Files.writeString(skillDir.resolve("SKILL.md"), """
            ---
            name: test-skill
            description: Test description
            context: testing
            tags: [tag1, tag2, tag3]
            ---
            # Content
            """);

        SimpleYamlParser parser = new SimpleYamlParser("SKILL.md", "UTF-8");

        SkillMetadata metadata = parser.parseMetadata(skillDir);

        assertThat(metadata.name()).isEqualTo("test-skill");
        assertThat(metadata.description()).isEqualTo("Test description");
        assertThat(metadata.context()).hasValue("testing");
        assertThat(metadata.tags()).containsExactly("tag1", "tag2", "tag3");
    }

    @Test
    void shouldHandleSpecialCharactersInContent() throws Exception {
        Path skillDir = tempDir.resolve("special-chars");
        Files.createDirectories(skillDir);
        // 使用单引号包裹包含特殊字符的值
        Files.writeString(skillDir.resolve("SKILL.md"), """
            ---
            name: special
            description: 'Special chars: <>&"'''
            ---
            # Special Characters

            Code with special chars:
            ```java
            String s = "<script>alert('xss')</script>";
            ```

            HTML entities: &amp; &lt; &gt;
            """);

        SimpleYamlParser parser = new SimpleYamlParser("SKILL.md", "UTF-8");

        var content = parser.parse(skillDir);

        assertThat(content.metadata().name()).isEqualTo("special");
        assertThat(content.metadata().description()).contains("<>", "&", "\"", "'");
        assertThat(content.markdownContent()).contains("<script>");
        assertThat(content.markdownContent()).contains("&amp;");
    }

    @Test
    void shouldHandleUnicodeContent() throws Exception {
        Path skillDir = tempDir.resolve("unicode");
        Files.createDirectories(skillDir);
        // 使用引号包裹包含冒号的值
        Files.writeString(skillDir.resolve("SKILL.md"), """
            ---
            name: unicode-skill
            description: "Unicode: 中文测试 🎉 émojis"
            ---
            # Unicode Content

            中文内容
            🎉 Emojis
            Café résumé
            """);

        SimpleYamlParser parser = new SimpleYamlParser("SKILL.md", "UTF-8");

        var content = parser.parse(skillDir);

        assertThat(content.metadata().name()).isEqualTo("unicode-skill");
        assertThat(content.metadata().description()).contains("中文测试");
        assertThat(content.markdownContent()).contains("中文内容");
        assertThat(content.markdownContent()).contains("🎉");
    }

    @Test
    void shouldHandleVeryLongDescription() throws Exception {
        Path skillDir = tempDir.resolve("long-desc");
        Files.createDirectories(skillDir);

        String longDescription = "A".repeat(10000);

        Files.writeString(skillDir.resolve("SKILL.md"), """
            ---
            name: long-desc
            description: %s
            ---
            # Long Description
            """.formatted(longDescription));

        SimpleYamlParser parser = new SimpleYamlParser("SKILL.md", "UTF-8");

        var content = parser.parse(skillDir);

        assertThat(content.metadata().description()).hasSize(10000);
    }

    @Test
    void shouldHandleExtraFields() throws Exception {
        Path skillDir = tempDir.resolve("extra-fields");
        Files.createDirectories(skillDir);
        Files.writeString(skillDir.resolve("SKILL.md"), """
            ---
            name: extra
            description: With extra fields
            custom-field-1: value1
            custom-field-2: 123
            author: Test Author
            version: 2.0.0
            license: MIT
            ---
            # Extra Fields
            """);

        SimpleYamlParser parser = new SimpleYamlParser("SKILL.md", "UTF-8");

        var content = parser.parse(skillDir);

        assertThat(content.metadata().extra())
            .containsEntry("custom-field-1", "value1")
            .containsEntry("custom-field-2", 123)
            .containsEntry("author", "Test Author")
            .containsEntry("version", "2.0.0")
            .containsEntry("license", "MIT");
    }
}
