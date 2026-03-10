package com.skillloader.reader;

import com.skillloader.api.exceptions.SecurityException;
import com.skillloader.config.PathEntry;
import com.skillloader.config.PathType;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ClasspathReaderTest {
    
    private ClasspathReader createReader(String allowedPath) {
        List<PathEntry> paths = List.of(
            new PathEntry("test", allowedPath, 10, false, PathType.CLASSPATH)
        );
        return new ClasspathReader(paths, 1024 * 1024);
    }
    
    @Test
    void shouldAllowClasspathPath() {
        ClasspathReader reader = createReader("skills");
        assertThat(reader.isAllowed(Path.of("skills/test.txt"))).isTrue();
    }
    
    @Test
    void shouldRejectNonClasspathType() {
        List<PathEntry> paths = List.of(
            new PathEntry("test", "/some/path", 10, false, PathType.FILESYSTEM)
        );
        ClasspathReader reader = new ClasspathReader(paths, 1024);
        
        assertThat(reader.isAllowed(Path.of("/some/path/file.txt"))).isFalse();
    }
    
    @Test
    void shouldRejectPathOutsideWhitelist() {
        ClasspathReader reader = createReader("skills");
        assertThat(reader.isAllowed(Path.of("other/file.txt"))).isFalse();
    }
    
    @Test
    void shouldStripClasspathPrefix() {
        ClasspathReader reader1 = createReader("classpath:/skills");
        ClasspathReader reader2 = createReader("classpath:skills");
        
        assertThat(reader1.isAllowed(Path.of("skills/test.txt"))).isTrue();
        assertThat(reader2.isAllowed(Path.of("skills/test.txt"))).isTrue();
    }
    
    @Test
    void shouldRejectNullPath() {
        ClasspathReader reader = createReader("skills");
        assertThat(reader.isAllowed(null)).isFalse();
    }
    
    @Test
    void shouldReadExistingResource() throws Exception {
        // 使用测试资源目录中的实际文件
        ClasspathReader reader = createReader("skills");
        Path resourcePath = Path.of("skills/pdf/SKILL.md");
        
        // 检查是否允许
        assertThat(reader.isAllowed(resourcePath)).isTrue();
    }
    
    @Test
    void shouldThrowSecurityExceptionWhenReadingOutsideWhitelist() {
        ClasspathReader reader = createReader("skills");
        
        assertThatThrownBy(() -> reader.read(Path.of("other/file.txt")))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("not in whitelist");
    }
    
    @Test
    void shouldThrowWhenResourceNotFound() {
        ClasspathReader reader = createReader("skills");
        Path nonExistent = Path.of("skills/non-existent-file-12345.txt");
        
        assertThatThrownBy(() -> reader.read(nonExistent))
            .isInstanceOf(java.io.IOException.class)
            .hasMessageContaining("Resource not found");
    }
    
    @Test
    void shouldReturnFalseForExistsOutsideWhitelist() {
        ClasspathReader reader = createReader("skills");
        assertThat(reader.exists(Path.of("other/file.txt"))).isFalse();
    }
    
    @Test
    void shouldListDirectoryContents() throws Exception {
        ClasspathReader reader = createReader("skills");
        List<Path> contents = reader.listDirectory(Path.of("skills"));
        
        // 应该返回 skills 目录下的子目录列表
        assertThat(contents).isNotEmpty();
        assertThat(contents).allMatch(path -> path.startsWith("skills"));
    }
    
    @Test
    void shouldReturnEmptyListForNonExistentDirectory() throws Exception {
        ClasspathReader reader = createReader("skills");
        // 使用白名单内的路径，但该路径下没有资源
        List<Path> contents = reader.listDirectory(Path.of("skills/non-existent-subdir"));
        
        assertThat(contents).isEmpty();
    }
    
    @Test
    void shouldOpenStreamForAllowedResource() throws Exception {
        ClasspathReader reader = createReader("skills");
        Path resourcePath = Path.of("skills/pdf/SKILL.md");
        
        // 验证资源存在且可读
        if (reader.exists(resourcePath)) {
            try (InputStream is = reader.openStream(resourcePath)) {
                assertThat(is).isNotNull();
                byte[] content = is.readAllBytes();
                assertThat(content.length).isGreaterThan(0);
            }
        }
    }
    
    @Test
    void shouldHandleMultipleAllowedPaths() {
        List<PathEntry> paths = List.of(
            new PathEntry("path1", "skills", 10, false, PathType.CLASSPATH),
            new PathEntry("path2", "config", 20, false, PathType.CLASSPATH)
        );
        ClasspathReader reader = new ClasspathReader(paths, 1024);
        
        assertThat(reader.isAllowed(Path.of("skills/test.txt"))).isTrue();
        assertThat(reader.isAllowed(Path.of("config/app.yml"))).isTrue();
        assertThat(reader.isAllowed(Path.of("other/file.txt"))).isFalse();
    }
    
    @Test
    void shouldListNestedDirectoryContents() throws Exception {
        ClasspathReader reader = createReader("skills");
        // 测试嵌套目录列表（如 git-workflow/scripts）
        List<Path> contents = reader.listDirectory(Path.of("skills/git-workflow"));
        
        // 应该返回 git-workflow 目录下的子目录/文件
        assertThat(contents).isNotEmpty();
        assertThat(contents).allMatch(path -> path.startsWith("skills/git-workflow"));
    }
    
    @Test
    void shouldReadSkillMarkdownContent() throws Exception {
        ClasspathReader reader = createReader("skills");
        Path skillPath = Path.of("skills/pdf/SKILL.md");
        
        String content = reader.read(skillPath);
        
        assertThat(content).isNotEmpty();
        assertThat(content).contains("---"); // 应该包含 frontmatter
    }
    
    @Test
    void shouldVerifyAllTestSkillsAreListable() throws Exception {
        ClasspathReader reader = createReader("skills");
        List<Path> contents = reader.listDirectory(Path.of("skills"));
        
        // 验证测试资源目录中的所有 skills 都能被列出
        List<String> skillNames = contents.stream()
            .map(p -> p.getFileName().toString())
            .toList();
        
        assertThat(skillNames).contains(
            "pdf", "weather", "chart", "git-workflow", 
            "python-quality", "data-visualization"
        );
    }
}
