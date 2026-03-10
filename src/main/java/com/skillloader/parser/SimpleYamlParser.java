package com.skillloader.parser;

import com.skillloader.api.exceptions.SkillParseException;
import com.skillloader.model.ResourceRef;
import com.skillloader.model.ResourceType;
import com.skillloader.model.SkillContent;
import com.skillloader.model.SkillMetadata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 轻量级 YAML Frontmatter 解析器。
 * 不依赖外部库，内置实现。
 */
public class SimpleYamlParser implements SkillParser {
    
    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile(
        "^---\\s*\\n(.*?)\\n---\\s*\\n(.*)$",
        Pattern.DOTALL
    );
    
    private final String markerFile;
    private final String encoding;
    
    public SimpleYamlParser(String markerFile, String encoding) {
        this.markerFile = markerFile;
        this.encoding = encoding;
    }
    
    @Override
    public SkillContent parse(Path skillDir) throws SkillParseException {
        Path skillFile = skillDir.resolve(markerFile);
        
        if (!Files.exists(skillFile)) {
            throw new SkillParseException(skillDir.toString(), "SKILL.md not found");
        }
        
        String content;
        try {
            content = Files.readString(skillFile, java.nio.charset.Charset.forName(encoding));
        } catch (IOException e) {
            throw new SkillParseException(skillDir.toString(), "Failed to read file: " + e.getMessage(), e);
        }
        
        SkillMetadata metadata = parseMetadata(content, skillDir);
        String markdown = extractMarkdown(content);
        List<ResourceRef> resources = discoverResources(skillDir);
        
        return new SkillContent(metadata, markdown, skillDir, resources);
    }
    
    @Override
    public SkillMetadata parseMetadata(Path skillDir) throws SkillParseException {
        Path skillFile = skillDir.resolve(markerFile);
        
        if (!Files.exists(skillFile)) {
            throw new SkillParseException(skillDir.toString(), "SKILL.md not found");
        }
        
        String content;
        try {
            content = Files.readString(skillFile, java.nio.charset.Charset.forName(encoding));
        } catch (IOException e) {
            throw new SkillParseException(skillDir.toString(), "Failed to read file: " + e.getMessage(), e);
        }
        
        return parseMetadata(content, skillDir);
    }
    
    /**
     * 从内容解析元数据。
     */
    private SkillMetadata parseMetadata(String content, Path skillDir) throws SkillParseException {
        Matcher matcher = FRONTMATTER_PATTERN.matcher(content);
        
        if (!matcher.find()) {
            throw new SkillParseException(skillDir.toString(), "Invalid SKILL.md format: missing YAML frontmatter");
        }
        
        String yamlContent = matcher.group(1).trim();
        Map<String, Object> data = parseYaml(yamlContent);
        
        String name = getString(data, "name");
        if (name == null || name.isBlank()) {
            throw new SkillParseException(skillDir.toString(), "Missing required field: name");
        }
        
        String description = getString(data, "description");
        String context = getString(data, "context");
        List<String> tags = getStringList(data, "tags");
        
        // 移除已处理的字段，剩下的放入 extra
        Map<String, Object> extra = new HashMap<>(data);
        extra.remove("name");
        extra.remove("description");
        extra.remove("context");
        extra.remove("tags");
        
        return new SkillMetadata(name, description, context, tags, extra);
    }
    
    /**
     * 解析 YAML 内容（简化版）。
     */
    private Map<String, Object> parseYaml(String yaml) {
        Map<String, Object> result = new HashMap<>();
        
        for (String line : yaml.split("\\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                
                // 移除引号
                if ((value.startsWith("\"") && value.endsWith("\"")) ||
                    (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                
                // 尝试解析为列表
                if (value.startsWith("[") && value.endsWith("]")) {
                    result.put(key, parseList(value));
                } else {
                    result.put(key, value);
                }
            }
        }
        
        return result;
    }
    
    /**
     * 解析列表字符串。
     */
    private List<String> parseList(String value) {
        List<String> result = new ArrayList<>();
        String content = value.substring(1, value.length() - 1);
        
        for (String item : content.split(",")) {
            item = item.trim();
            // 移除引号
            if ((item.startsWith("\"") && item.endsWith("\"")) ||
                (item.startsWith("'") && item.endsWith("'"))) {
                item = item.substring(1, item.length() - 1);
            }
            if (!item.isEmpty()) {
                result.add(item);
            }
        }
        
        return result;
    }
    
    /**
     * 提取 Markdown 内容。
     */
    private String extractMarkdown(String content) {
        Matcher matcher = FRONTMATTER_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(2).trim();
        }
        return content.trim();
    }
    
    /**
     * 发现资源文件。
     */
    private List<ResourceRef> discoverResources(Path skillDir) {
        List<ResourceRef> resources = new ArrayList<>();
        
        // 检查常见资源目录
        String[] resourceDirs = {"scripts", "references", "assets"};
        
        for (String dirName : resourceDirs) {
            Path dir = skillDir.resolve(dirName);
            if (Files.exists(dir) && Files.isDirectory(dir)) {
                try (var stream = Files.list(dir)) {
                    stream.forEach(file -> {
                        ResourceType type = determineResourceType(dirName);
                        resources.add(new ResourceRef(
                            file.getFileName().toString(),
                            file.toUri(),
                            type
                        ));
                    });
                } catch (IOException e) {
                    // 忽略读取失败的资源
                }
            }
        }
        
        return resources;
    }
    
    private ResourceType determineResourceType(String dirName) {
        return switch (dirName) {
            case "scripts" -> ResourceType.SCRIPT;
            case "references" -> ResourceType.REFERENCE;
            case "assets" -> ResourceType.ASSET;
            default -> ResourceType.OTHER;
        };
    }
    
    private String getString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value instanceof String ? (String) value : null;
    }
    
    @SuppressWarnings("unchecked")
    private List<String> getStringList(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof List) {
            return (List<String>) value;
        }
        return null;
    }
    
    /**
     * 静态方法：解析 YAML frontmatter。
     * 从字符串内容中提取 frontmatter 数据。
     *
     * @param content SKILL.md 内容
     * @return frontmatter 键值对
     */
    public static Map<String, Object> parseFrontmatter(String content) {
        Map<String, Object> result = new HashMap<>();
        
        if (content == null || content.isBlank()) {
            return result;
        }
        
        Matcher matcher = FRONTMATTER_PATTERN.matcher(content);
        if (!matcher.find()) {
            return result; // 没有找到 frontmatter，返回空 map
        }
        
        String yamlContent = matcher.group(1).trim();
        return parseYamlStatic(yamlContent);
    }
    
    /**
     * 静态解析 YAML 内容（简化版）。
     */
    private static Map<String, Object> parseYamlStatic(String yaml) {
        Map<String, Object> result = new HashMap<>();
        
        for (String line : yaml.split("\\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                
                // 移除引号
                if ((value.startsWith("\"") && value.endsWith("\"")) ||
                    (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                
                // 尝试解析为列表
                if (value.startsWith("[") && value.endsWith("]")) {
                    result.put(key, parseListStatic(value));
                } else {
                    result.put(key, value);
                }
            }
        }
        
        return result;
    }
    
    /**
     * 静态解析列表字符串。
     */
    private static List<String> parseListStatic(String value) {
        List<String> result = new ArrayList<>();
        String content = value.substring(1, value.length() - 1);
        
        for (String item : content.split(",")) {
            item = item.trim();
            // 移除引号
            if ((item.startsWith("\"") && item.endsWith("\"")) ||
                (item.startsWith("'") && item.endsWith("'"))) {
                item = item.substring(1, item.length() - 1);
            }
            if (!item.isEmpty()) {
                result.add(item);
            }
        }
        
        return result;
    }
}
