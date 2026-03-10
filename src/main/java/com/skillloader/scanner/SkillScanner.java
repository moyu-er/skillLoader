package com.skillloader.scanner;

import com.skillloader.api.exceptions.SecurityException;
import com.skillloader.config.PathEntry;
import com.skillloader.config.PathType;
import com.skillloader.config.SecurityConfig;
import com.skillloader.model.Skill;
import com.skillloader.model.SkillSource;
import com.skillloader.parser.SimpleYamlParser;
import com.skillloader.reader.ClasspathReader;
import com.skillloader.reader.FileSystemReader;
import com.skillloader.reader.SecureFileReader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Skill 目录扫描器。
 * 扫描配置的目录，发现所有 skill。
 */
public class SkillScanner {
    
    private final List<PathEntry> pathEntries;
    private final SecurityConfig securityConfig;
    private final String markerFile;
    
    public SkillScanner(List<PathEntry> pathEntries, SecurityConfig securityConfig, String markerFile) {
        this.pathEntries = List.copyOf(Objects.requireNonNull(pathEntries));
        this.securityConfig = Objects.requireNonNull(securityConfig);
        this.markerFile = Objects.requireNonNull(markerFile);
    }
    
    /**
     * 扫描所有配置的目录，返回发现的 skills。
     */
    public List<Skill> scanAll() {
        List<Skill> allSkills = new ArrayList<>();
        
        for (PathEntry entry : pathEntries) {
            try {
                List<Skill> skills = scanEntry(entry);
                allSkills.addAll(skills);
            } catch (Exception e) {
                // 单个路径失败不中断，记录或继续
                if (entry.required()) {
                    throw new RuntimeException("Failed to scan required path: " + entry.path(), e);
                }
                // 非必需路径失败，跳过
            }
        }
        
        return allSkills;
    }
    
    /**
     * 扫描单个路径条目。
     */
    private List<Skill> scanEntry(PathEntry entry) throws IOException {
        SecureFileReader reader = createReader(entry);
        
        // 检查路径是否存在
        if (!reader.exists(Path.of(entry.path()))) {
            if (entry.required()) {
                throw new IOException("Required path does not exist: " + entry.path());
            }
            return List.of();
        }
        
        // 根据类型扫描
        if (entry.type() == PathType.FILESYSTEM) {
            return scanFileSystemEntry(entry, reader);
        } else {
            return scanClasspathEntry(entry, reader);
        }
    }
    
    /**
     * 扫描文件系统目录。
     */
    private List<Skill> scanFileSystemEntry(PathEntry entry, SecureFileReader reader) throws IOException {
        Path basePath = Path.of(entry.path()).toAbsolutePath().normalize();
        return scanDirectory(basePath, entry, reader, 0);
    }
    
    /**
     * 递归扫描目录。
     */
    private List<Skill> scanDirectory(Path dir, PathEntry entry, SecureFileReader reader, int depth) 
            throws IOException {
        // 深度检查
        if (depth > securityConfig.maxDepth()) {
            return List.of();
        }
        
        List<Skill> skills = new ArrayList<>();
        
        List<Path> contents;
        try {
            contents = reader.listDirectory(dir);
        } catch (SecurityException e) {
            // 权限问题，跳过
            return skills;
        }
        
        for (Path item : contents) {
            if (reader.isDirectory(item)) {
                // 检查是否是 skill 目录
                if (isSkillDirectory(item, reader)) {
                    Skill skill = createSkill(item, entry, reader);
                    skills.add(skill);
                } else {
                    // 递归扫描子目录
                    skills.addAll(scanDirectory(item, entry, reader, depth + 1));
                }
            }
        }
        
        return skills;
    }
    
    /**
     * 扫描 Classpath 条目。
     * 扫描 classpath 下的指定目录，发现所有 skill。
     */
    private List<Skill> scanClasspathEntry(PathEntry entry, SecureFileReader reader) throws IOException {
        Path basePath = Path.of(entry.path());
        return scanClasspathDirectory(basePath, entry, reader, 0);
    }
    
    /**
     * 递归扫描 Classpath 目录。
     */
    private List<Skill> scanClasspathDirectory(Path dir, PathEntry entry, SecureFileReader reader, int depth) 
            throws IOException {
        // 深度检查
        if (depth > securityConfig.maxDepth()) {
            return List.of();
        }
        
        List<Skill> skills = new ArrayList<>();
        
        List<Path> contents;
        try {
            contents = reader.listDirectory(dir);
        } catch (SecurityException e) {
            // 权限问题，跳过
            return skills;
        }
        
        for (Path item : contents) {
            // 检查是否是 skill 目录（包含 marker file）
            if (isSkillDirectory(item, reader)) {
                Skill skill = createSkill(item, entry, reader);
                skills.add(skill);
            } else {
                // 递归扫描子目录
                skills.addAll(scanClasspathDirectory(item, entry, reader, depth + 1));
            }
        }
        
        return skills;
    }
    
    /**
     * 检查是否是 skill 目录（包含 marker file）。
     */
    private boolean isSkillDirectory(Path dir, SecureFileReader reader) {
        Path marker = dir.resolve(markerFile);
        return reader.exists(marker);
    }
    
    /**
     * 创建 Skill 对象，从 SKILL.md 解析 name 和 description。
     */
    private Skill createSkill(Path skillDir, PathEntry entry, SecureFileReader reader) {
        String name = skillDir.getFileName().toString();
        String description = "";
        
        // 读取 SKILL.md 解析 frontmatter
        try {
            Path skillMdPath = skillDir.resolve(markerFile);
            String content = reader.read(skillMdPath);
            
            // 解析 YAML frontmatter
            Map<String, Object> frontmatter = SimpleYamlParser.parseFrontmatter(content);
            
            // 获取 name（优先使用 frontmatter 中的，否则用目录名）
            if (frontmatter.containsKey("name")) {
                name = String.valueOf(frontmatter.get("name"));
            }
            
            // 获取 description
            if (frontmatter.containsKey("description")) {
                description = String.valueOf(frontmatter.get("description"));
            }
        } catch (Exception e) {
            // 解析失败时使用默认值（目录名作为 name，空字符串作为 description）
        }
        
        SkillSource source = entry.type() == PathType.CLASSPATH 
            ? SkillSource.CLASSPATH 
            : (isGlobalPath(entry) ? SkillSource.GLOBAL : SkillSource.PROJECT);
        
        return new Skill(name, description, source, skillDir, entry.priority());
    }
    
    /**
     * 判断是否是全局路径。
     */
    private boolean isGlobalPath(PathEntry entry) {
        String path = entry.path();
        return path.contains("user.home") || path.startsWith("/usr/") || path.startsWith("~");
    }
    
    /**
     * 创建对应的 FileReader。
     */
    private SecureFileReader createReader(PathEntry entry) {
        List<PathEntry> singlePath = List.of(entry);
        
        if (entry.type() == PathType.CLASSPATH) {
            return new ClasspathReader(singlePath, Long.MAX_VALUE);
        } else {
            return new FileSystemReader(singlePath, securityConfig, Long.MAX_VALUE, "UTF-8");
        }
    }
}
