package com.skillloader.api;

import com.skillloader.api.exceptions.SkillLoaderException;
import com.skillloader.api.exceptions.SkillNotFoundException;
import com.skillloader.config.*;
import com.skillloader.generator.AgentsMdGenerator;
import com.skillloader.generator.DefaultAgentsMdGenerator;
import com.skillloader.model.Skill;
import com.skillloader.model.SkillContent;
import com.skillloader.model.SkillMetadata;
import com.skillloader.parser.SimpleYamlParser;
import com.skillloader.parser.SkillParser;
import com.skillloader.registry.DefaultSkillRegistry;
import com.skillloader.registry.SkillRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * SkillLoader 门面类。
 * 提供统一的 API 用于发现、加载 skills 和生成 AGENTS.md。
 */
public final class SkillLoader {

    private final SkillLoaderConfig config;
    private final SkillRegistry registry;
    private final SkillParser parser;
    private final AgentsMdGenerator generator;

    private SkillLoader(SkillLoaderConfig config) {
        this.config = Objects.requireNonNull(config);
        this.registry = new DefaultSkillRegistry(config);
        this.parser = new SimpleYamlParser(config.parser().markerFile(), config.parser().encoding());
        this.generator = new DefaultAgentsMdGenerator();
    }

    /**
     * 创建默认配置的 SkillLoader。
     */
    public static SkillLoader createDefault() {
        return new SkillLoader(SkillLoaderConfig.defaults());
    }

    /**
     * 从配置文件创建。
     * 支持 YAML/JSON 格式的配置文件。
     */
    public static SkillLoader fromConfig(Path configPath) throws SkillLoaderException {
        Objects.requireNonNull(configPath, "configPath cannot be null");

        if (!Files.exists(configPath)) {
            throw new SkillLoaderException("Config file not found: " + configPath);
        }

        try {
            String content = Files.readString(configPath);
            SkillLoaderConfig config = parseConfig(content);
            return new SkillLoader(config);
        } catch (IOException e) {
            throw new SkillLoaderException("Failed to read config file: " + e.getMessage(), e);
        }
    }

    /**
     * 解析配置文件内容。
     */
    private static SkillLoaderConfig parseConfig(String content) throws SkillLoaderException {
        // 简单的 YAML 配置解析
        Map<String, Object> config = SimpleYamlParser.parseFrontmatter(content);

        if (config.isEmpty()) {
            throw new SkillLoaderException("Invalid config file: empty or invalid format");
        }

        SkillLoaderConfig.Builder builder = SkillLoaderConfig.builder();

        // 解析 paths
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> paths = (List<Map<String, Object>>) config.get("paths");
        if (paths != null) {
            for (Map<String, Object> pathConfig : paths) {
                String name = (String) pathConfig.get("name");
                String path = (String) pathConfig.get("path");
                Integer priority = (Integer) pathConfig.getOrDefault("priority", 10);
                Boolean required = (Boolean) pathConfig.getOrDefault("required", false);
                String type = (String) pathConfig.getOrDefault("type", "filesystem");

                if (name == null || path == null) {
                    throw new SkillLoaderException("Path config missing name or path");
                }

                PathType pathType = "classpath".equalsIgnoreCase(type) ? PathType.CLASSPATH : PathType.FILESYSTEM;
                builder.addPath(new PathEntry(name, path, priority, required, pathType));
            }
        }

        // 解析 security 配置
        @SuppressWarnings("unchecked")
        Map<String, Object> security = (Map<String, Object>) config.get("security");
        if (security != null) {
            Boolean strictMode = (Boolean) security.getOrDefault("strictMode", true);
            Boolean allowSymlinks = (Boolean) security.getOrDefault("allowSymlinks", false);
            Integer maxDepth = (Integer) security.getOrDefault("maxDepth", 10);
            builder.security(new SecurityConfig(strictMode, allowSymlinks, maxDepth));
        }

        // 解析 parser 配置
        @SuppressWarnings("unchecked")
        Map<String, Object> parser = (Map<String, Object>) config.get("parser");
        if (parser != null) {
            String markerFile = (String) parser.get("markerFile");
            String encoding = (String) parser.get("encoding");
            ParserConfig parserConfig = new ParserConfig(
                markerFile != null ? markerFile : "SKILL.md",
                encoding != null ? encoding : "UTF-8"
            );
            builder.parser(parserConfig);
        }

        return builder.build();
    }

    /**
     * 从配置对象创建。
     */
    public static SkillLoader fromConfig(SkillLoaderConfig config) {
        return new SkillLoader(config);
    }

    /**
     * 发现所有可用的 skills。
     */
    public List<Skill> discover() {
        return registry.discover();
    }

    /**
     * 加载指定 skill 的完整内容。
     *
     * @throws SkillNotFoundException skill 不存在
     */
    public SkillContent load(String skillName) throws SkillNotFoundException {
        Skill skill = registry.find(skillName)
            .orElseThrow(() -> new SkillNotFoundException(skillName));

        return parser.parse(skill.location());
    }

    /**
     * 获取 skill 元数据（不加载完整内容）。
     */
    public Optional<SkillMetadata> getMetadata(String skillName) {
        return registry.find(skillName).flatMap(skill -> {
            try {
                return Optional.of(parser.parseMetadata(skill.location()));
            } catch (Exception e) {
                return Optional.empty();
            }
        });
    }

    /**
     * 生成 AGENTS.md 内容。
     */
    public String generateAgentsMd() {
        return generator.generate(registry.discover());
    }

    /**
     * 同步到 AGENTS.md 文件。
     * 仅在生成器启用时执行写操作。
     *
     * @param path AGENTS.md 文件路径
     * @throws IllegalStateException 如果生成器未启用
     */
    public void syncToFile(Path path) throws IOException {
        if (!config.generator().enabled()) {
            throw new IllegalStateException(
                "AGENTS.md generation is disabled. " +
                "Enable it by setting generator.enabled=true in configuration."
            );
        }
        String content = generateAgentsMd();
        Files.writeString(path, content);
    }

    /**
     * 更新现有的 AGENTS.md 文件。
     * 仅在生成器启用时执行写操作。
     *
     * @param path AGENTS.md 文件路径
     * @throws IllegalStateException 如果生成器未启用
     */
    public void updateFile(Path path) throws IOException {
        if (!config.generator().enabled()) {
            throw new IllegalStateException(
                "AGENTS.md generation is disabled. " +
                "Enable it by setting generator.enabled=true in configuration."
            );
        }
        String existing = Files.exists(path) ? Files.readString(path) : "";
        String updated = generator.updateExisting(existing, registry.discover());
        Files.writeString(path, updated);
    }

    /**
     * 获取白名单路径列表（调试用）。
     */
    public List<PathEntry> getAllowedPaths() {
        return config.paths();
    }

    /**
     * 获取配置。
     */
    public SkillLoaderConfig getConfig() {
        return config;
    }

    /**
     * 获取注册表。
     */
    public SkillRegistry registry() {
        return registry;
    }

    /**
     * 创建构建器。
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * SkillLoader 构建器。
     */
    public static class Builder {
        private final SkillLoaderConfig.Builder configBuilder = SkillLoaderConfig.builder();

        public Builder addPath(PathEntry path) {
            configBuilder.addPath(path);
            return this;
        }

        public Builder addFilesystemPath(String name, String path) {
            configBuilder.addFilesystemPath(name, path);
            return this;
        }

        public Builder addClasspathPath(String name, String path) {
            configBuilder.addClasspathPath(name, path);
            return this;
        }

        public SkillLoader build() {
            return new SkillLoader(configBuilder.build());
        }
    }
}
