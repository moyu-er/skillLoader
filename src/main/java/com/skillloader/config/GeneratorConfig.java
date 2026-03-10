package com.skillloader.config;

import java.util.Objects;

/**
 * 生成器配置。
 */
public final class GeneratorConfig {
    
    public static final String DEFAULT_TEMPLATE = "default";
    public static final String DEFAULT_MARKER_START = "<!-- SKILLS_TABLE_START -->";
    public static final String DEFAULT_MARKER_END = "<!-- SKILLS_TABLE_END -->";
    
    private final String template;
    private final String markerStart;
    private final String markerEnd;
    private final boolean enabled;  // AGENTS.md 生成功能开关，默认关闭
    
    public GeneratorConfig(String template, String markerStart, String markerEnd, boolean enabled) {
        this.template = template != null ? template : DEFAULT_TEMPLATE;
        this.markerStart = markerStart != null ? markerStart : DEFAULT_MARKER_START;
        this.markerEnd = markerEnd != null ? markerEnd : DEFAULT_MARKER_END;
        this.enabled = enabled;
    }
    
    public static GeneratorConfig defaults() {
        // 默认禁用 AGENTS.md 生成功能
        return new GeneratorConfig(DEFAULT_TEMPLATE, DEFAULT_MARKER_START, DEFAULT_MARKER_END, false);
    }
    
    public String template() {
        return template;
    }
    
    public String markerStart() {
        return markerStart;
    }
    
    public String markerEnd() {
        return markerEnd;
    }
    
    /**
     * AGENTS.md 生成功能是否启用。
     * 默认关闭（false）。
     */
    public boolean enabled() {
        return enabled;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GeneratorConfig that = (GeneratorConfig) o;
        return enabled == that.enabled &&
               Objects.equals(template, that.template) &&
               Objects.equals(markerStart, that.markerStart) &&
               Objects.equals(markerEnd, that.markerEnd);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(template, markerStart, markerEnd, enabled);
    }
    
    @Override
    public String toString() {
        return "GeneratorConfig{" +
               "template='" + template + '\'' +
               ", markerStart='" + markerStart + '\'' +
               ", markerEnd='" + markerEnd + '\'' +
               ", enabled=" + enabled +
               '}';
    }
}
