package com.skillloader.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Skill 元数据。
 * 从 SKILL.md 的 YAML frontmatter 解析得到。
 */
public final class SkillMetadata {
    
    private final String name;
    private final String description;
    private final String context;
    private final List<String> tags;
    private final Map<String, Object> extra;
    
    public SkillMetadata(String name, String description, String context, 
                        List<String> tags, Map<String, Object> extra) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.description = description != null ? description : "";
        this.context = context;
        this.tags = tags != null ? List.copyOf(tags) : List.of();
        this.extra = extra != null ? Map.copyOf(extra) : Map.of();
    }
    
    public String name() {
        return name;
    }
    
    public String description() {
        return description;
    }
    
    public Optional<String> context() {
        return Optional.ofNullable(context);
    }
    
    public List<String> tags() {
        return tags;
    }
    
    public Map<String, Object> extra() {
        return extra;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SkillMetadata that = (SkillMetadata) o;
        return Objects.equals(name, that.name) &amp;&amp;
               Objects.equals(description, that.description) &amp;&amp;
               Objects.equals(context, that.context) &amp;&amp;
               Objects.equals(tags, that.tags) &amp;&amp;
               Objects.equals(extra, that.extra);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, description, context, tags, extra);
    }
    
    @Override
    public String toString() {
        return "SkillMetadata{" +
               "name='" + name + '\'' +
               ", description='" + description + '\'' +
               ", context='" + context + '\'' +
               ", tags=" + tags +
               ", extra=" + extra +
               '}';
    }
}
