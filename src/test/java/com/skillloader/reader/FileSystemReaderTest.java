package com.skillloader.reader;

import com.skillloader.api.exceptions.SecurityException;
import com.skillloader.config.PathEntry;
import com.skillloader.config.PathType;
import com.skillloader.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class FileSystemReaderTest {
    
    @TempDir
    Path tempDir;
    
    private FileSystemReader createReader(Path allowedPath) {
        List<PathEntry> paths = List.of(
            new PathEntry("test", allowedPath.toString(), 10, false, PathType.FILESYSTEM)
        );
        SecurityConfig security = new SecurityConfig(true, false, 3);
        return new FileSystemReader(paths, security, 1024 * 1024, "UTF-8");
    }
    
    @Test
    void shouldAllowPathInWhitelist() {
        FileSystemReader reader = createReader(tempDir);
        assertThat(reader.isAllowed(tempDir.resolve("test.txt"))).isTrue();
    }
    
    @Test
    void shouldRejectPathOutsideWhitelist() {
        FileSystemReader reader = createReader(tempDir);
        assertThat(reader.isAllowed(Path.of("/etc/passwd"))).isFalse();
    }
    
    @Test
    void shouldRejectNullPath() {
        FileSystemReader reader = createReader(tempDir);
        assertThat(reader.isAllowed(null)).isFalse();
    }
    
    @Test
    void shouldReadFileSuccessfully() throws Exception {
        FileSystemReader reader = createReader(tempDir);
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Hello World");
        
        String content = reader.read(testFile);
        
        assertThat(content).isEqualTo("Hello World");
    }
    
    @Test
    void shouldThrowSecurityExceptionWhenReadingOutsideWhitelist() {
        FileSystemReader reader = createReader(tempDir);
        
        assertThatThrownBy(() -> reader.read(Path.of("/etc/passwd")))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("not in whitelist");
    }
    
    @Test
    void shouldRejectPathTraversalAttack() {
        FileSystemReader reader = createReader(tempDir);
        Path traversalPath = tempDir.resolve("../secret.txt");
        
        assertThat(reader.isAllowed(traversalPath)).isFalse();
    }
    
    @Test
    void shouldListDirectoryContents() throws Exception {
        FileSystemReader reader = createReader(tempDir);
        Files.createFile(tempDir.resolve("file1.txt"));
        Files.createFile(tempDir.resolve("file2.txt"));
        
        List<Path> contents = reader.listDirectory(tempDir);
        
        assertThat(contents).hasSize(2);
    }
    
    @Test
    void shouldThrowWhenListingOutsideWhitelist() {
        FileSystemReader reader = createReader(tempDir);
        
        assertThatThrownBy(() -> reader.listDirectory(Path.of("/etc")))
            .isInstanceOf(SecurityException.class);
    }
    
    @Test
    void shouldCheckFileExists() throws Exception {
        FileSystemReader reader = createReader(tempDir);
        Path testFile = tempDir.resolve("exists.txt");
        Files.writeString(testFile, "content");
        
        assertThat(reader.exists(testFile)).isTrue();
        assertThat(reader.exists(tempDir.resolve("notexists.txt"))).isFalse();
    }
    
    @Test
    void shouldReturnFalseForExistsOutsideWhitelist() {
        FileSystemReader reader = createReader(tempDir);
        assertThat(reader.exists(Path.of("/etc/passwd"))).isFalse();
    }
    
    @Test
    void shouldRejectSymlinksWhenNotAllowed() throws Exception {
        FileSystemReader reader = createReader(tempDir);
        Path realFile = tempDir.resolve("real.txt");
        Path symlink = tempDir.resolve("link.txt");
        Files.writeString(realFile, "content");
        Files.createSymbolicLink(symlink, realFile);
        
        assertThatThrownBy(() -> reader.read(symlink))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("Symbolic links are not allowed");
    }
    
    @Test
    void shouldAllowSymlinksWhenAllowed() throws Exception {
        List<PathEntry> paths = List.of(
            new PathEntry("test", tempDir.toString(), 10, false, PathType.FILESYSTEM)
        );
        SecurityConfig security = new SecurityConfig(true, true, 3); // allow symlinks
        FileSystemReader reader = new FileSystemReader(paths, security, 1024 * 1024, "UTF-8");
        
        Path realFile = tempDir.resolve("real.txt");
        Path symlink = tempDir.resolve("link.txt");
        Files.writeString(realFile, "content");
        Files.createSymbolicLink(symlink, realFile);
        
        String content = reader.read(symlink);
        assertThat(content).isEqualTo("content");
    }
    
    @Test
    void shouldRejectFileTooLarge() throws Exception {
        FileSystemReader reader = new FileSystemReader(
            List.of(new PathEntry("test", tempDir.toString(), 10, false, PathType.FILESYSTEM)),
            new SecurityConfig(true, false, 3),
            10, // 10 bytes max
            "UTF-8"
        );
        
        Path testFile = tempDir.resolve("large.txt");
        Files.writeString(testFile, "This is more than 10 bytes");
        
        assertThatThrownBy(() -> reader.read(testFile))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("File too large");
    }
    
    @Test
    void shouldOpenStreamSuccessfully() throws Exception {
        FileSystemReader reader = createReader(tempDir);
        Path testFile = tempDir.resolve("stream.txt");
        Files.writeString(testFile, "Stream content");
        
        try (var stream = reader.openStream(testFile)) {
            String content = new String(stream.readAllBytes());
            assertThat(content).isEqualTo("Stream content");
        }
    }
    
    @Test
    void shouldCheckIsDirectory() throws Exception {
        FileSystemReader reader = createReader(tempDir);
        Path subDir = tempDir.resolve("subdir");
        Files.createDirectory(subDir);
        
        assertThat(reader.isDirectory(subDir)).isTrue();
        assertThat(reader.isDirectory(tempDir.resolve("notadir.txt"))).isFalse();
    }
    
    @Test
    void shouldResolveVariables() throws Exception {
        List<PathEntry> paths = List.of(
            new PathEntry("home", "${user.home}/test-skills", 10, false, PathType.FILESYSTEM)
        );
        FileSystemReader reader = new FileSystemReader(paths, new SecurityConfig(true, false, 3), 1024, "UTF-8");
        
        Path homePath = Path.of(System.getProperty("user.home"), "test-skills", "file.txt");
        // 至少验证不抛异常
        assertThat(reader.isAllowed(homePath)).isTrue();
    }
    
    @Test
    void shouldHandleMultipleAllowedPaths() {
        List<PathEntry> paths = List.of(
            new PathEntry("path1", tempDir.resolve("dir1").toString(), 10, false, PathType.FILESYSTEM),
            new PathEntry("path2", tempDir.resolve("dir2").toString(), 20, false, PathType.FILESYSTEM)
        );
        FileSystemReader reader = new FileSystemReader(paths, new SecurityConfig(true, false, 3), 1024, "UTF-8");
        
        assertThat(reader.isAllowed(tempDir.resolve("dir1/file.txt"))).isTrue();
        assertThat(reader.isAllowed(tempDir.resolve("dir2/file.txt"))).isTrue();
        assertThat(reader.isAllowed(tempDir.resolve("dir3/file.txt"))).isFalse();
    }
}
