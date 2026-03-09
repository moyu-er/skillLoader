package com.skillloader.api.exceptions;

/**
 * Skill 未找到异常。
 * 当尝试加载不存在的 skill 时抛出。
 */
public class SkillNotFoundException extends SkillLoaderException {
    
    private final String skillName;
    
    public SkillNotFoundException(String skillName) {
        super("Skill not found: " + skillName);
        this.skillName = skillName;
    }
    
    public SkillNotFoundException(String skillName, String message) {
        super(message);
        this.skillName = skillName;
    }
    
    public String getSkillName() {
        return skillName;
    }
}
