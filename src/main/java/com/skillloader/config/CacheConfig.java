package com.skillloader.config;

import java.util.Objects;

/**
 * 缓存配置。
 * 控制 Skill 内容的缓存行为。
 *
 * @param enabled 是否启用缓存，默认 true
 * @param maxSize 缓存最大条目数，默认 10
 * @param expireAfterAccessMinutes 访问后过期时间（分钟），默认 30
 */
public record CacheConfig(
    boolean enabled,
    int maxSize,
    long expireAfterAccessMinutes
) {
    
    /** 默认最大缓存大小 */
    public static final int DEFAULT_MAX_SIZE = 10;
    
    /** 默认过期时间（分钟） */
    public static final long DEFAULT_EXPIRE_MINUTES = 30;
    
    /**
     * 创建默认配置。
     */
    public CacheConfig {
        if (maxSize <= 0) {
            maxSize = DEFAULT_MAX_SIZE;
        }
        if (expireAfterAccessMinutes <= 0) {
            expireAfterAccessMinutes = DEFAULT_EXPIRE_MINUTES;
        }
    }
    
    /**
     * 获取默认配置。
     */
    public static CacheConfig defaults() {
        return new CacheConfig(true, DEFAULT_MAX_SIZE, DEFAULT_EXPIRE_MINUTES);
    }
    
    /**
     * 禁用缓存。
     */
    public static CacheConfig disabled() {
        return new CacheConfig(false, DEFAULT_MAX_SIZE, DEFAULT_EXPIRE_MINUTES);
    }
    
    /**
     * 创建自定义大小的配置。
     */
    public static CacheConfig withSize(int maxSize) {
        return new CacheConfig(true, maxSize, DEFAULT_EXPIRE_MINUTES);
    }
}
