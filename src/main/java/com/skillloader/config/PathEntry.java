package com.skillloader.config;

import java.util.Objects;

/**
 * 路径条目配置。
 */
public final class PathEntry {
    
    private final String name;
    private final String path;
    private final int priority;
    private final boolean required;
    private final PathType type;
    
    public PathEntry(String name, String path, int priority, boolean required, PathType type) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.path = Objects.requireNonNull(path, "path cannot be null");
        this.priority = priority;
        this.required = required;
        this.type = Objects.requireNonNull(type, "type cannot be null");
    }
    
    public String name() {
        return name;
    }
    
    public String path() {
        return path;
    }
    
    public int priority() {
        return priority;
    }
    
    public boolean required() {
        return required;
    }
    
    public PathType type() {
        return type;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PathEntry pathEntry = (PathEntry) o;
        return priority == pathEntry.priority &&
               required == pathEntry.required &&
               Objects.equals(name, pathEntry.name) &&
               Objects.equals(path, pathEntry.path) &&
               type == pathEntry.type;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, path, priority, required, type);
    }
    
    @Override
    public String toString() {
        return "PathEntry{" +
               "name='" + name + '\'' +
               ", path='" + path + '\'' +
               ", priority=" + priority +
               ", required=" + required +
               ", type=" + type +
               '}';
    }
}
