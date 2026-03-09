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
     */
    public static SkillLoader fromConfig(Path configPath) throws SkillLoaderException {
        // TODO: 实现配置加载
        throw new UnsupportedOperationException("Config loading not implemented yet");
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
     * 
     * @param path AGENTS.md 文件路径
     */
    public void syncToFile(Path path) throws IOException {
        String content = generateAgentsMd();
        Files.writeString(path, content);
    }
    
    /**
     * 更新现有的 AGENTS.md 文件。
     * 
     * @param path AGENTS.md 文件路径
     */
    public void updateFile(Path path) throws IOException {
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
