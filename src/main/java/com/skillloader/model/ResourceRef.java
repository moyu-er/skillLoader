package com.skillloader.model;

import java.net.URI;
import java.util.Objects;

/**
 * Skill 资源引用。
 * 指向 skill 目录下的脚本、参考文档等资源。
 */
public final class ResourceRef {
    
    private final String name;
    private final URI uri;
    private final ResourceType type;
    
    public ResourceRef(String name, URI uri, ResourceType type) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.uri = Objects.requireNonNull(uri, "uri cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
    }
    
    public String name() {
        return name;
    }
    
    public URI uri() {
        return uri;
    }
    
    public ResourceType type() {
        return type;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceRef that = (ResourceRef) o;
        return Objects.equals(name, that.name) &amp;&amp;
               Objects.equals(uri, that.uri) &amp;&amp;
               type == that.type;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, uri, type);
    }
    
    @Override
    public String toString() {
        return "ResourceRef{" +
               "name='" + name + '\'' +
               ", uri=" + uri +
               ", type=" + type +
               '}';
    }
}
