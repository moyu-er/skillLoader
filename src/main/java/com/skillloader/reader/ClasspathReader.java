package com.skillloader.reader;

import com.skillloader.api.exceptions.SecurityException;
import com.skillloader.config.PathEntry;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
        URL url = classLoader.getResource(resourcePath);
        
        if (url == null) {
            return List.of();
        }
        
        String protocol = url.getProtocol();
        
        if ("file".equals(protocol)) {
            // 文件系统目录（开发模式）
            return listFileSystemDirectory(dir, url);
        } else if ("jar".equals(protocol)) {
            // JAR 文件中的目录（生产模式）
            return listJarDirectory(dir, resourcePath, url);
        }
        
        return List.of();
    }
    
    private List<Path> listFileSystemDirectory(Path dir, URL url) throws IOException {
        try {
            Path path = Paths.get(url.toURI());
            if (!Files.isDirectory(path)) {
                return List.of();
            }
            
            List<Path> result = new ArrayList<>();
            try (var stream = Files.list(path)) {
                stream.forEach(child -> {
                    result.add(dir.resolve(child.getFileName().toString()));
                });
            }
            return result;
        } catch (URISyntaxException e) {
            throw new IOException("Invalid URL: " + url, e);
        }
    }
    
    private List<Path> listJarDirectory(Path dir, String resourcePath, URL url) throws IOException {
        // 确保路径以 / 结尾，用于匹配目录下的条目
        String dirPath = resourcePath.endsWith("/") ? resourcePath : resourcePath + "/";
        
        JarURLConnection jarConn = (JarURLConnection) url.openConnection();
        String jarEntryName = jarConn.getEntryName(); // 可能是 null 如果是根目录
        
        // 获取 JAR 文件 URL
        URL jarFileUrl = jarConn.getJarFileURL();
        
        Set<String> entries = new HashSet<>();
        
        try (JarFile jarFile = new JarFile(new File(jarFileUrl.toURI()))) {
            Enumeration<JarEntry> jarEntries = jarFile.entries();
            
            while (jarEntries.hasMoreElements()) {
                JarEntry entry = jarEntries.nextElement();
                String entryName = entry.getName();
                
                // 检查是否是目标目录下的直接子条目
                if (entryName.startsWith(dirPath) && !entryName.equals(dirPath)) {
                    String relativePath = entryName.substring(dirPath.length());
                    
                    // 只取直接子目录或文件（不包含嵌套）
                    int slashIndex = relativePath.indexOf('/');
                    if (slashIndex == -1 || slashIndex == relativePath.length() - 1) {
                        // 去掉末尾的 /
                        if (relativePath.endsWith("/")) {
                            relativePath = relativePath.substring(0, relativePath.length() - 1);
                        }
                        if (!relativePath.isEmpty()) {
                            entries.add(relativePath);
                        }
                    }
                }
            }
        } catch (URISyntaxException e) {
            throw new IOException("Invalid JAR URL: " + jarFileUrl, e);
        }
        
        // 转换为 Path 列表
        List<Path> result = new ArrayList<>();
        for (String entry : entries) {
            result.add(dir.resolve(entry));
        }
        return result;
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
