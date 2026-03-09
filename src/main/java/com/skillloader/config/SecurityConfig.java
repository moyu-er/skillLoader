package com.skillloader.config;

import java.util.Objects;

/**
 * 安全配置。
 */
public final class SecurityConfig {
    
    public static final boolean DEFAULT_STRICT_MODE = true;
    public static final boolean DEFAULT_ALLOW_SYMLINKS = false;
    public static final int DEFAULT_MAX_DEPTH = 3;
    
    private final boolean strictMode;
    private final boolean allowSymlinks;
    private final int maxDepth;
    
    public SecurityConfig(boolean strictMode, boolean allowSymlinks, int maxDepth) {
        this.strictMode = strictMode;
        this.allowSymlinks = allowSymlinks;
        this.maxDepth = maxDepth > 0 ? maxDepth : DEFAULT_MAX_DEPTH;
    }
    
    public static SecurityConfig defaults() {
        return new SecurityConfig(DEFAULT_STRICT_MODE, DEFAULT_ALLOW_SYMLINKS, DEFAULT_MAX_DEPTH);
    }
    
    public boolean strictMode() {
        return strictMode;
    }
    
    public boolean allowSymlinks() {
        return allowSymlinks;
    }
    
    public int maxDepth() {
        return maxDepth;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SecurityConfig that = (SecurityConfig) o;
        return strictMode == that.strictMode &amp;&amp;
               allowSymlinks == that.allowSymlinks &amp;&amp;
               maxDepth == that.maxDepth;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(strictMode, allowSymlinks, maxDepth);
    }
    
    @Override
    public String toString() {
        return "SecurityConfig{" +
               "strictMode=" + strictMode +
               ", allowSymlinks=" + allowSymlinks +
               ", maxDepth=" + maxDepth +
               '}';
    }
}
