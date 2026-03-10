package com.skillloader.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.skillloader.config.CacheConfig;
import com.skillloader.model.SkillContent;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Skill 内容缓存管理器。
 * 使用 Caffeine 实现 LRU 缓存，避免重复读取文件。
 */
public class SkillContentCache {
    
    private final CacheConfig config;
    private final Cache<String, SkillContent> cache;
    
    /**
     * 创建缓存管理器。
     */
    public SkillContentCache(CacheConfig config) {
        this.config = Objects.requireNonNull(config);
        this.cache = createCache(config);
    }
    
    /**
     * 创建默认缓存管理器。
     */
    public static SkillContentCache createDefault() {
        return new SkillContentCache(CacheConfig.defaults());
    }
    
    /**
     * 创建禁用缓存的管理器。
     */
    public static SkillContentCache disabled() {
        return new SkillContentCache(CacheConfig.disabled());
    }
    
    /**
     * 根据配置创建 Caffeine 缓存。
     */
    private Cache<String, SkillContent> createCache(CacheConfig config) {
        if (!config.enabled()) {
            return null;
        }
        
        return Caffeine.newBuilder()
            .maximumSize(config.maxSize())
            .expireAfterAccess(Duration.ofMinutes(config.expireAfterAccessMinutes()))
            .recordStats()
            .build();
    }
    
    /**
     * 获取缓存的 Skill 内容。
     * 如果缓存未命中，使用 loader 加载并缓存。
     *
     * @param skillPath Skill 目录路径
     * @param loader 加载函数
     * @return Skill 内容
     */
    public SkillContent get(Path skillPath, Function<Path, SkillContent> loader) {
        if (!config.enabled() || cache == null) {
            return loader.apply(skillPath);
        }
        
        String key = normalizeKey(skillPath);
        return cache.get(key, k -> loader.apply(skillPath));
    }
    
    /**
     * 从缓存获取（如果存在）。
     */
    public Optional<SkillContent> getIfPresent(Path skillPath) {
        if (!config.enabled() || cache == null) {
            return Optional.empty();
        }
        
        String key = normalizeKey(skillPath);
        return Optional.ofNullable(cache.getIfPresent(key));
    }
    
    /**
     * 手动放入缓存。
     */
    public void put(Path skillPath, SkillContent content) {
        if (!config.enabled() || cache == null) {
            return;
        }
        
        String key = normalizeKey(skillPath);
        cache.put(key, content);
    }
    
    /**
     * 使指定 Skill 的缓存失效。
     */
    public void invalidate(Path skillPath) {
        if (!config.enabled() || cache == null) {
            return;
        }
        
        String key = normalizeKey(skillPath);
        cache.invalidate(key);
    }
    
    /**
     * 清空所有缓存。
     */
    public void invalidateAll() {
        if (!config.enabled() || cache == null) {
            return;
        }
        
        cache.invalidateAll();
    }
    
    /**
     * 获取缓存统计信息。
     */
    public CacheStats getStats() {
        if (!config.enabled() || cache == null) {
            return CacheStats.empty();
        }
        
        com.github.benmanes.caffeine.cache.stats.CacheStats stats = cache.stats();
        return new CacheStats(
            stats.hitCount(),
            stats.missCount(),
            stats.hitRate(),
            cache.estimatedSize()
        );
    }
    
    /**
     * 获取当前缓存大小。
     */
    public long size() {
        if (!config.enabled() || cache == null) {
            return 0;
        }
        
        return cache.estimatedSize();
    }
    
    /**
     * 检查缓存是否启用。
     */
    public boolean isEnabled() {
        return config.enabled();
    }
    
    /**
     * 规范化缓存键。
     */
    private String normalizeKey(Path path) {
        return path.toAbsolutePath().normalize().toString();
    }
    
    /**
     * 缓存统计信息。
     */
    public record CacheStats(
        long hitCount,
        long missCount,
        double hitRate,
        long size
    ) {
        public static CacheStats empty() {
            return new CacheStats(0, 0, 0.0, 0);
        }
        
        @Override
        public String toString() {
            return String.format(
                "CacheStats{hits=%d, misses=%d, hitRate=%.2f%%, size=%d}",
                hitCount, missCount, hitRate * 100, size
            );
        }
    }
}
