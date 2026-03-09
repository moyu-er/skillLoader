package com.skillloader.api.exceptions;

/**
 * Skill 解析异常。
 * 当 SKILL.md 格式错误、YAML frontmatter 解析失败时抛出。
 */
public class SkillParseException extends SkillLoaderException {
    
    private final String skillPath;
    
    public SkillParseException(String skillPath, String message) {
        super("Failed to parse skill at " + skillPath + ": " + message);
        this.skillPath = skillPath;
    }
    
    public SkillParseException(String skillPath, String message, Throwable cause) {
        super("Failed to parse skill at " + skillPath + ": " + message, cause);
        this.skillPath = skillPath;
    }
    
    public String getSkillPath() {
        return skillPath;
    }
}
