package com.skillloader.config;

import com.skillloader.api.exceptions.ConfigException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * SkillLoader 配置。
 */
public final class SkillLoaderConfig {
    
    private final List<PathEntry> paths;
    private final ParserConfig parser;
    private final SecurityConfig security;
    private final GeneratorConfig generator;
    private final CacheConfig cache;
    
    public SkillLoaderConfig(List<PathEntry> paths, 
                            ParserConfig parser, 
                            SecurityConfig security, 
                            GeneratorConfig generator,
                            CacheConfig cache) {
        // 验证：至少有一个路径
        if (paths == null || paths.isEmpty()) {
            throw new ConfigException("At least one path must be configured");
        }
        
        // 按优先级排序（数字小的在前）
        List<PathEntry> sortedPaths = new ArrayList<>(paths);
        sortedPaths.sort(Comparator.comparingInt(PathEntry::priority));
        this.paths = Collections.unmodifiableList(sortedPaths);
        
        this.parser = parser != null ? parser : ParserConfig.defaults();
        this.security = security != null ? security : SecurityConfig.defaults();
        this.generator = generator != null ? generator : GeneratorConfig.defaults();
        this.cache = cache != null ? cache : CacheConfig.defaults();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static SkillLoaderConfig defaults() {
        return new SkillLoaderConfig(
            List.of(new PathEntry("default", "skills", 10, false, PathType.CLASSPATH)),
            ParserConfig.defaults(),
            SecurityConfig.defaults(),
            GeneratorConfig.defaults(),
            CacheConfig.defaults()
        );
    }
    
    public List<PathEntry> paths() {
        return paths;
    }
    
    public ParserConfig parser() {
        return parser;
    }
    
    public SecurityConfig security() {
        return security;
    }
    
    public GeneratorConfig generator() {
        return generator;
    }

    public CacheConfig cache() {
        return cache;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SkillLoaderConfig that = (SkillLoaderConfig) o;
        return Objects.equals(paths, that.paths) &&
               Objects.equals(parser, that.parser) &&
               Objects.equals(security, that.security) &&
               Objects.equals(generator, that.generator) &&
               Objects.equals(cache, that.cache);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paths, parser, security, generator, cache);
    }

    @Override
    public String toString() {
        return "SkillLoaderConfig{" +
               "paths=" + paths +
               ", parser=" + parser +
               ", security=" + security +
               ", generator=" + generator +
               ", cache=" + cache +
               '}';
    }
    
    /**
     * 配置构建器。
     */
    public static class Builder {
        private final List<PathEntry> paths = new ArrayList<>();
        private ParserConfig parser;
        private SecurityConfig security;
        private GeneratorConfig generator;
        private CacheConfig cache;

        public Builder addPath(PathEntry path) {
            this.paths.add(Objects.requireNonNull(path, "path cannot be null"));
            return this;
        }

        public Builder addPath(String name, String path, int priority, boolean required, PathType type) {
            this.paths.add(new PathEntry(name, path, priority, required, type));
            return this;
        }

        public Builder addFilesystemPath(String name, String path) {
            return addPath(name, path, 10, false, PathType.FILESYSTEM);
        }

        public Builder addClasspathPath(String name, String path) {
            return addPath(name, path, 20, false, PathType.CLASSPATH);
        }

        public Builder parser(ParserConfig parser) {
            this.parser = parser;
            return this;
        }

        public Builder security(SecurityConfig security) {
            this.security = security;
            return this;
        }

        public Builder generator(GeneratorConfig generator) {
            this.generator = generator;
            return this;
        }

        public Builder cache(CacheConfig cache) {
            this.cache = cache;
            return this;
        }

        public SkillLoaderConfig build() {
            return new SkillLoaderConfig(paths, parser, security, generator, cache);
        }
    }
}
