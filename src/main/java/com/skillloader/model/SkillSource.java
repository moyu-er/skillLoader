package com.skillloader.model;

import java.nio.file.Path;

/**
 * Skill 来源类型。
 */
public enum SkillSource {
    /**
     * 项目本地 skills。
     */
    PROJECT,
    
    /**
     * 用户全局 skills。
     */
    GLOBAL,
    
    /**
     * Classpath 内的 skills。
     */
    CLASSPATH,
    
    /**
     * 自定义来源。
     */
    CUSTOM
}
