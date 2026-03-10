package com.skillloader.reader;

import com.skillloader.config.PathEntry;
import com.skillloader.config.PathType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ClasspathReader 针对 Maven resources 目录的测试。
 */
class ClasspathReaderMavenTest {
    
    @TempDir
    Path tempDir;
    
    /**
     * 测试场景：模拟 Maven 项目结构
     * src/main/resources/skills/my-skill/SKILL.md
     */
    @Test
    void shouldDiscoverSkillsInMavenResourcesDirectory() throws Exception {
        // 创建 Maven 项目结构
        Path resourcesDir = tempDir.resolve("src/main/resources/skills");
        Path skillDir = resourcesDir.resolve("my-skill");
        Files.createDirectories(skillDir);
        Files.writeString(skillDir.resolve("SKILL.md"), 
            "---\nname: my-skill\ndescription: Test skill\n---\n# Content");
        
        // 创建 ClasspathReader 指向 resources/skills
        PathEntry entry = new PathEntry("resources", "skills", 10, false, PathType.CLASSPATH);
        ClasspathReader reader = new ClasspathReader(
            List.of(entry),
            1024 * 1024,
            new java.io.File(resourcesDir.getParent().getParent().toString()).toURI().toURL()
        );
        
        // 列出 skills 目录
        Path skillsPath = Path.of("skills");
        List<Path> children = reader.listDirectory(skillsPath);
        
        // 验证发现 skills
        assertThat(children).isNotEmpty();
        assertThat(children.stream().map(Path::toString))
            .contains("skills/my-skill");
    }
    
    /**
     * 测试场景：验证 isDirectory 在文件系统模式下正常工作
     */
    @Test
    void shouldCorrectlyIdentifyDirectoryInFileSystemMode() throws Exception {
        // 创建目录结构
        Path skillsDir = tempDir.resolve("skills");
        Path skillDir = skillsDir.resolve("git-workflow");
        Files.createDirectories(skillDir);
        Files.writeString(skillDir.resolve("SKILL.md"), 
            "---\nname: git-workflow\ndescription: Git workflow\n---\n# Content");
        
        PathEntry entry = new PathEntry("test", "skills", 10, false, PathType.CLASSPATH);
        
        // 使用 file:// URL 模拟文件系统模式
        ClasspathReader reader = new ClasspathReader(
            List.of(entry),
            1024 * 1024,
            new java.io.File(tempDir.toString()).toURI().toURL()
        );
        
        // 测试 isDirectory
        Path skillsPath = Path.of("skills");
        assertThat(reader.isDirectory(skillsPath)).isTrue();
        
        Path skillPath = Path.of("skills/git-workflow");
        assertThat(reader.isDirectory(skillPath)).isTrue();
        
        Path filePath = Path.of("skills/git-workflow/SKILL.md");
        assertThat(reader.isDirectory(filePath)).isFalse();
    }
    
    /**
     * 测试场景：验证 exists 方法在文件系统模式下正常工作
     */
    @Test
    void shouldCorrectlyCheckFileExistence() throws Exception {
        // 创建文件
        Path skillsDir = tempDir.resolve("skills");
        Path skillDir = skillsDir.resolve("test-skill");
        Files.createDirectories(skillDir);
        Files.writeString(skillDir.resolve("SKILL.md"), "content");
        
        PathEntry entry = new PathEntry("test", "skills", 10, false, PathType.CLASSPATH);
        ClasspathReader reader = new ClasspathReader(
            List.of(entry),
            1024 * 1024,
            new java.io.File(tempDir.toString()).toURI().toURL()
        );
        
        // 测试 exists
        assertThat(reader.exists(Path.of("skills/test-skill/SKILL.md"))).isTrue();
        assertThat(reader.exists(Path.of("skills/non-existent"))).isFalse();
    }
    
    /**
     * 测试场景：验证读取文件内容
     */
    @Test
    void shouldReadFileContent() throws Exception {
        // 创建文件
        Path skillsDir = tempDir.resolve("skills");
        Path skillDir = skillsDir.resolve("test");
        Files.createDirectories(skillDir);
        String content = "---\nname: test\ndescription: Test\n---\n# Hello World";
        Files.writeString(skillDir.resolve("SKILL.md"), content);
        
        PathEntry entry = new PathEntry("test", "skills", 10, false, PathType.CLASSPATH);
        ClasspathReader reader = new ClasspathReader(
            List.of(entry),
            1024 * 1024,
            new java.io.File(tempDir.toString()).toURI().toURL()
        );
        
        // 读取内容
        String readContent = reader.read(Path.of("skills/test/SKILL.md"));
        assertThat(readContent).isEqualTo(content);
    }
    
    /**
     * 测试场景：验证路径安全检查
     */
    @Test
    void shouldValidatePathSecurity() throws Exception {
        PathEntry entry = new PathEntry("test", "skills", 10, false, PathType.CLASSPATH);
        ClasspathReader reader = new ClasspathReader(
            List.of(entry),
            1024 * 1024,
            new java.io.File(tempDir.toString()).toURI().toURL()
        );
        
        // 允许的路径
        assertThat(reader.isAllowed(Path.of("skills/test"))).isTrue();
        
        // 不允许的路径（路径遍历攻击）
        assertThat(reader.isAllowed(Path.of("skills/../../../etc/passwd"))).isFalse();
        assertThat(reader.isAllowed(Path.of("/etc/passwd"))).isFalse();
    }
}
