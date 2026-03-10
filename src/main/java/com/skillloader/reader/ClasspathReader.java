package com.skillloader.reader;

import com.skillloader.api.exceptions.SecurityException;
import com.skillloader.config.PathEntry;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Classpath 安全读取器实现。
 */
public class ClasspathReader implements SecureFileReader {
    
    private final List<PathEntry> allowedPaths;
    private final long maxFileSize;
    private final ClassLoader classLoader;
    
    public ClasspathReader(List<PathEntry> allowedPaths, long maxFileSize) {
        this(allowedPaths, maxFileSize, ClasspathReader.class.getClassLoader());
    }
    
    public ClasspathReader(List<PathEntry> allowedPaths, long maxFileSize, ClassLoader classLoader) {
        this.allowedPaths = List.copyOf(Objects.requireNonNull(allowedPaths));
        this.maxFileSize = maxFileSize;
        this.classLoader = Objects.requireNonNull(classLoader);
    }
    
    @Override
    public boolean isAllowed(Path path) {
        if (path == null) {
            return false;
        }
        
        String pathStr = path.toString();
        
        for (PathEntry entry : allowedPaths) {
            if (entry.type() != com.skillloader.config.PathType.CLASSPATH) {
                continue;
            }
            String allowedPath = stripClasspathPrefix(entry.path());
            if (pathStr.startsWith(allowedPath) || pathStr.startsWith("/" + allowedPath)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public String read(Path path) throws SecurityException, IOException {
        validatePath(path);
        
        String resourcePath = toResourcePath(path);
        URL url = classLoader.getResource(resourcePath);
        
        if (url == null) {
            throw new IOException("Resource not found: " + path);
        }
        
        // 检查大小
        try (InputStream is = url.openStream()) {
            byte[] content = is.readAllBytes();
            if (content.length > maxFileSize) {
                throw new IOException("Resource too large: " + content.length + " bytes");
            }
            return new String(content, StandardCharsets.UTF_8);
        }
    }
    
    @Override
    public List<Path> listDirectory(Path dir) throws SecurityException, IOException {
        validatePath(dir);
        
        // Classpath 目录列表实现较复杂，通常返回空列表
        // 实际应用中可以通过扫描 jar 文件实现
        return List.of();
    }
    
    @Override
    public boolean exists(Path path) {
        if (!isAllowed(path)) {
            return false;
        }
        String resourcePath = toResourcePath(path);
        return classLoader.getResource(resourcePath) != null;
    }
    
    @Override
    public boolean isDirectory(Path path) throws SecurityException {
        validatePath(path);
        // Classpath 无法准确判断是否是目录
        return false;
    }
    
    @Override
    public InputStream openStream(Path path) throws SecurityException, IOException {
        validatePath(path);
        
        String resourcePath = toResourcePath(path);
        InputStream is = classLoader.getResourceAsStream(resourcePath);
        
        if (is == null) {
            throw new IOException("Resource not found: " + path);
        }
        
        return is;
    }
    
    private void validatePath(Path path) throws SecurityException {
        if (!isAllowed(path)) {
            throw new SecurityException("Path not in whitelist: " + path);
        }
    }
    
    private String toResourcePath(Path path) {
        String pathStr = path.toString();
        // 移除前导斜杠
        if (pathStr.startsWith("/")) {
            pathStr = pathStr.substring(1);
        }
        return pathStr;
    }
    
    private String stripClasspathPrefix(String path) {
        if (path.startsWith("classpath:/")) {
            return path.substring("classpath:/".length());
        }
        if (path.startsWith("classpath:")) {
            return path.substring("classpath:".length());
        }
        return path;
    }
}
