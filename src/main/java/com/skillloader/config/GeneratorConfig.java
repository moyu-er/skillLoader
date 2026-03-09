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
    
    public GeneratorConfig(String template, String markerStart, String markerEnd) {
        this.template = template != null ? template : DEFAULT_TEMPLATE;
        this.markerStart = markerStart != null ? markerStart : DEFAULT_MARKER_START;
        this.markerEnd = markerEnd != null ? markerEnd : DEFAULT_MARKER_END;
    }
    
    public static GeneratorConfig defaults() {
        return new GeneratorConfig(DEFAULT_TEMPLATE, DEFAULT_MARKER_START, DEFAULT_MARKER_END);
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
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GeneratorConfig that = (GeneratorConfig) o;
        return Objects.equals(template, that.template) &amp;&amp;
               Objects.equals(markerStart, that.markerStart) &amp;&amp;
               Objects.equals(markerEnd, that.markerEnd);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(template, markerStart, markerEnd);
    }
    
    @Override
    public String toString() {
        return "GeneratorConfig{" +
               "template='" + template + '\'' +
               ", markerStart='" + markerStart + '\'' +
               ", markerEnd='" + markerEnd + '\'' +
               '}';
    }
}
