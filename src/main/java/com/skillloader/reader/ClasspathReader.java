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
        
        String resourcePath = toResourcePath(dir);
        List<Path> results = new ArrayList<>();
        
        try {
            // 尝试从文件系统读取（开发模式）
            URL url = classLoader.getResource(resourcePath);
            if (url != null) {
                if (url.getProtocol().equals("file")) {
                    // 开发模式：资源在文件系统中
                    java.io.File file = new java.io.File(url.toURI());
                    if (file.isDirectory()) {
                        java.io.File[] children = file.listFiles();
                        if (children != null) {
                            for (java.io.File child : children) {
                                results.add(dir.resolve(child.getName()));
                            }
                        }
                    }
                } else if (url.getProtocol().equals("jar")) {
                    // 生产模式：资源在 JAR 文件中
                    results.addAll(listJarDirectory(resourcePath));
                }
            }
        } catch (Exception e) {
            // 扫描失败时返回空列表
        }
        
        return results;
    }
    
    /**
     * 列出 JAR 文件中的目录内容。
     */
    private List<Path> listJarDirectory(String resourcePath) throws IOException {
        List<Path> results = new ArrayList<>();
        
        try {
            java.util.Enumeration<URL> urls = classLoader.getResources(resourcePath);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                String urlStr = url.toString();
                
                if (urlStr.startsWith("jar:")) {
                    int bangIndex = urlStr.indexOf("!");
                    if (bangIndex > 0) {
                        String jarPath = urlStr.substring(4, bangIndex);
                        String entryPath = urlStr.substring(bangIndex + 2);
                        
                        try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(
                                new java.io.File(new java.net.URI(jarPath)))) {
                            java.util.Enumeration<java.util.jar.JarEntry> entries = jarFile.entries();
                            while (entries.hasMoreElements()) {
                                java.util.jar.JarEntry entry = entries.nextElement();
                                String name = entry.getName();
                                if (name.startsWith(entryPath) && !name.equals(entryPath)) {
                                    String relativeName = name.substring(entryPath.length());
                                    if (!relativeName.contains("/")) {
                                        results.add(Path.of(resourcePath, relativeName));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 忽略 JAR 扫描错误
        }
        
        return results;
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
        
        String resourcePath = toResourcePath(path);
        URL url = classLoader.getResource(resourcePath);
        
        if (url == null) {
            return false;
        }
        
        // 开发模式：检查文件系统
        if (url.getProtocol().equals("file")) {
            try {
                return new java.io.File(url.toURI()).isDirectory();
            } catch (Exception e) {
                return false;
            }
        }
        
        // JAR 模式：尝试列出子资源来判断是否是目录
        try {
            java.util.Enumeration<URL> urls = classLoader.getResources(resourcePath);
            if (urls.hasMoreElements()) {
                // 如果能找到子资源，说明是目录
                return hasChildResources(resourcePath);
            }
        } catch (IOException e) {
            return false;
        }
        
        return false;
    }
    
    /**
     * 检查是否有子资源（用于判断是否是目录）。
     */
    private boolean hasChildResources(String resourcePath) {
        try {
            java.util.Enumeration<URL> urls = classLoader.getResources(resourcePath);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                String urlStr = url.toString();
                
                if (urlStr.startsWith("jar:")) {
                    int bangIndex = urlStr.indexOf("!");
                    if (bangIndex > 0) {
                        String jarPath = urlStr.substring(4, bangIndex);
                        String entryPath = urlStr.substring(bangIndex + 2);
                        
                        try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(
                                new java.io.File(new java.net.URI(jarPath)))) {
                            java.util.Enumeration<java.util.jar.JarEntry> entries = jarFile.entries();
                            while (entries.hasMoreElements()) {
                                java.util.jar.JarEntry entry = entries.nextElement();
                                String name = entry.getName();
                                if (name.startsWith(entryPath + "/") && !name.equals(entryPath + "/")) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 忽略错误
        }
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
