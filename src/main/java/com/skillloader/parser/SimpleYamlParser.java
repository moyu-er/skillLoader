package com.skillloader.parser;

import com.skillloader.api.exceptions.SkillParseException;
import com.skillloader.model.ResourceRef;
import com.skillloader.model.ResourceType;
import com.skillloader.model.SkillContent;
import com.skillloader.model.SkillMetadata;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于 SnakeYAML 的 YAML Frontmatter 解析器。
 * 使用成熟的 YAML 框架，避免手动造轮子。
 */
public class SimpleYamlParser implements SkillParser {

    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile(
        "^---\\s*\\n(.*?)\\n---\\s*\\n(.*)$",
        Pattern.DOTALL
    );

    private final String markerFile;
    private final String encoding;
    // SnakeYAML 的 Yaml 对象不是线程安全的，使用 ThreadLocal
    private final ThreadLocal<Yaml> yamlHolder;

    public SimpleYamlParser(String markerFile, String encoding) {
        this.markerFile = markerFile;
        this.encoding = encoding;
        this.yamlHolder = ThreadLocal.withInitial(Yaml::new);
    }

    private Yaml getYaml() {
        return yamlHolder.get();
    }

    @Override
    public SkillContent parse(Path skillDir) throws SkillParseException {
        Path skillFile = skillDir.resolve(markerFile);

        String content = readSkillContent(skillDir, skillFile);
        if (content == null) {
            throw new SkillParseException(skillDir.toString(), "SKILL.md not found");
        }

        SkillMetadata metadata = parseMetadata(content, skillDir);
        String markdown = extractMarkdown(content);
        List<ResourceRef> resources = discoverResources(skillDir);

        return new SkillContent(metadata, markdown, skillDir, resources);
    }

    @Override
    public SkillMetadata parseMetadata(Path skillDir) throws SkillParseException {
        Path skillFile = skillDir.resolve(markerFile);

        String content = readSkillContent(skillDir, skillFile);
        if (content == null) {
            throw new SkillParseException(skillDir.toString(), "SKILL.md not found");
        }

        return parseMetadata(content, skillDir);
    }

    /**
     * 读取 skill 内容，支持文件系统和 classpath。
     * @return 文件内容，如果找不到则返回 null
     */
    private String readSkillContent(Path skillDir, Path skillFile) throws SkillParseException {
        // 首先尝试文件系统读取
        if (Files.exists(skillFile)) {
            try {
                return Files.readString(skillFile, java.nio.charset.Charset.forName(encoding));
            } catch (IOException e) {
                // 文件系统读取失败，尝试 classpath
            }
        }

        // 尝试从 classpath 读取
        String resourcePath = toResourcePath(skillFile);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = SimpleYamlParser.class.getClassLoader();
        }

        try (InputStream is = classLoader.getResourceAsStream(resourcePath)) {
            if (is != null) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new SkillParseException(skillDir.toString(), "Failed to read from classpath: " + e.getMessage(), e);
        }

        return null;
    }

    /**
     * 将 Path 转换为 classpath 资源路径。
     */
    private String toResourcePath(Path path) {
        String pathStr = path.toString();
        // 移除前导斜杠和反斜杠
        if (pathStr.startsWith("/") || pathStr.startsWith("\\")) {
            pathStr = pathStr.substring(1);
        }
        // 将反斜杠转换为正斜杠
        return pathStr.replace("\\", "/");
    }

    /**
     * 从内容解析元数据。
     */
    @SuppressWarnings("unchecked")
    private SkillMetadata parseMetadata(String content, Path skillDir) throws SkillParseException {
        Matcher matcher = FRONTMATTER_PATTERN.matcher(content);

        if (!matcher.find()) {
            throw new SkillParseException(skillDir.toString(), "Invalid SKILL.md format: missing YAML frontmatter");
        }

        String yamlContent = matcher.group(1).trim();

        // 使用 SnakeYAML 解析
        Map<String, Object> data;
        try {
            data = getYaml().load(yamlContent);
            if (data == null) {
                data = new HashMap<>();
            }
        } catch (Exception e) {
            throw new SkillParseException(skillDir.toString(), "Failed to parse YAML frontmatter: " + e.getMessage(), e);
        }

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
    @SuppressWarnings("unchecked")
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

        // 使用 SnakeYAML 解析
        Yaml yamlInstance = new Yaml();
        try {
            Map<String, Object> data = yamlInstance.load(yamlContent);
            if (data != null) {
                return data;
            }
        } catch (Exception e) {
            // 解析失败，返回空 map
        }

        return result;
    }
}
