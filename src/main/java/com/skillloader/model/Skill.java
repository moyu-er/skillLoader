package com.skillloader.model;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Skill 定义。
 * 不可变对象，表示一个已发现的 skill。
 */
public final class Skill implements Comparable<Skill> {
    
    private final String name;
    private final String description;
    private final SkillSource source;
    private final Path location;
    private final int priority;
    
    public Skill(String name, String description, SkillSource source, Path location, int priority) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.description = description != null ? description : "";
        this.source = Objects.requireNonNull(source, "source cannot be null");
        this.location = Objects.requireNonNull(location, "location cannot be null");
        this.priority = priority;
    }
    
    public String name() {
        return name;
    }
    
    public String description() {
        return description;
    }
    
    public SkillSource source() {
        return source;
    }
    
    public Path location() {
        return location;
    }
    
    public int priority() {
        return priority;
    }
    
    @Override
    public int compareTo(Skill other) {
        // 优先级高的排前面
        int priorityCompare = Integer.compare(this.priority, other.priority);
        if (priorityCompare != 0) {
            return priorityCompare;
        }
        return this.name.compareTo(other.name);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Skill skill = (Skill) o;
        return priority == skill.priority &amp;&amp;
               Objects.equals(name, skill.name) &amp;&amp;
               Objects.equals(description, skill.description) &amp;&amp;
               source == skill.source &amp;&amp;
               Objects.equals(location, skill.location);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, description, source, location, priority);
    }
    
    @Override
    public String toString() {
        return "Skill{" +
               "name='" + name + '\'' +
               ", description='" + description + '\'' +
               ", source=" + source +
               ", location=" + location +
               ", priority=" + priority +
               '}';
    }
}
