package com.skillloader.model;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Skill 完整内容。
 * 包含 metadata、markdown 正文和资源引用。
 */
public final class SkillContent {
    
    private final SkillMetadata metadata;
    private final String markdownContent;
    private final Path baseDir;
    private final List<ResourceRef> resources;
    
    public SkillContent(SkillMetadata metadata, String markdownContent, 
                       Path baseDir, List<ResourceRef> resources) {
        this.metadata = Objects.requireNonNull(metadata, "metadata cannot be null");
        this.markdownContent = markdownContent != null ? markdownContent : "";
        this.baseDir = Objects.requireNonNull(baseDir, "baseDir cannot be null");
        this.resources = resources != null ? List.copyOf(resources) : List.of();
    }
    
    public SkillMetadata metadata() {
        return metadata;
    }
    
    public String markdownContent() {
        return markdownContent;
    }
    
    public Path baseDir() {
        return baseDir;
    }
    
    public List<ResourceRef> resources() {
        return resources;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SkillContent that = (SkillContent) o;
        return Objects.equals(metadata, that.metadata) &&
               Objects.equals(markdownContent, that.markdownContent) &&
               Objects.equals(baseDir, that.baseDir) &&
               Objects.equals(resources, that.resources);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(metadata, markdownContent, baseDir, resources);
    }
    
    @Override
    public String toString() {
        return "SkillContent{" +
               "metadata=" + metadata +
               ", markdownContentLength=" + markdownContent.length() +
               ", baseDir=" + baseDir +
               ", resources=" + resources +
               '}';
    }
}
