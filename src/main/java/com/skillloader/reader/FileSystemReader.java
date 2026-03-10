package com.skillloader.reader;

import com.skillloader.api.exceptions.SecurityException;
import com.skillloader.config.PathEntry;
import com.skillloader.config.SecurityConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.List;
import java.util.Objects;

/**
 * 文件系统安全读取器实现。
 */
public class FileSystemReader implements SecureFileReader {
    
    private final List<PathEntry> allowedPaths;
    private final SecurityConfig securityConfig;
    private final long maxFileSize;
    private final Charset charset;
    
    public FileSystemReader(List<PathEntry> allowedPaths, SecurityConfig securityConfig, 
                           long maxFileSize, String encoding) {
        this.allowedPaths = List.copyOf(Objects.requireNonNull(allowedPaths));
        this.securityConfig = Objects.requireNonNull(securityConfig);
        this.maxFileSize = maxFileSize;
        this.charset = Charset.forName(encoding);
    }
    
    @Override
    public boolean isAllowed(Path path) {
        if (path == null) {
            return false;
        }
        
        Path normalized = normalize(path);
        
        for (PathEntry entry : allowedPaths) {
            if (entry.type() != com.skillloader.config.PathType.FILESYSTEM) {
                continue;
            }
            Path allowedBase = resolvePath(entry.path());
            if (normalized.startsWith(allowedBase)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public String read(Path path) throws SecurityException, IOException {
        validatePath(path);
        
        Path normalized = normalize(path);
        
        // 检查文件大小
        long size = Files.size(normalized);
        if (size > maxFileSize) {
            throw new IOException("File too large: " + size + " bytes (max: " + maxFileSize + ")");
        }
        
        return Files.readString(normalized, charset);
    }
    
    @Override
    public List<Path> listDirectory(Path dir) throws SecurityException, IOException {
        validatePath(dir);
        
        Path normalized = normalize(dir);
        
        if (!Files.isDirectory(normalized)) {
            throw new IOException("Not a directory: " + dir);
        }
        
        try (var stream = Files.list(normalized)) {
            return stream.toList();
        }
    }
    
    @Override
    public boolean exists(Path path) {
        if (!isAllowed(path)) {
            return false;
        }
        return Files.exists(normalize(path));
    }
    
    @Override
    public boolean isDirectory(Path path) throws SecurityException {
        validatePath(path);
        return Files.isDirectory(normalize(path));
    }
    
    @Override
    public InputStream openStream(Path path) throws SecurityException, IOException {
        validatePath(path);
        return Files.newInputStream(normalize(path));
    }
    
    /**
     * 验证路径在白名单内。
     */
    private void validatePath(Path path) throws SecurityException {
        if (!isAllowed(path)) {
            throw new SecurityException("Path not in whitelist: " + path);
        }
        
        Path normalized = normalize(path);
        
        // 检查符号链接
        if (!securityConfig.allowSymlinks() && Files.isSymbolicLink(normalized)) {
            throw new SecurityException("Symbolic links are not allowed: " + path);
        }
    }
    
    /**
     * 规范化路径（解析 . 和 ..）。
     */
    private Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }
    
    /**
     * 解析路径字符串为 Path。
     */
    private Path resolvePath(String pathStr) {
        // 处理系统变量如 ${user.home}
        String resolved = resolveVariables(pathStr);
        return Path.of(resolved).toAbsolutePath().normalize();
    }
    
    /**
     * 解析 ${...} 变量。
     */
    private String resolveVariables(String path) {
        String result = path;
        
        // ${user.home}
        if (result.contains("${user.home}")) {
            result = result.replace("${user.home}", System.getProperty("user.home"));
        }
        
        // ${user.dir}
        if (result.contains("${user.dir}")) {
            result = result.replace("${user.dir}", System.getProperty("user.dir"));
        }
        
        return result;
    }
}
