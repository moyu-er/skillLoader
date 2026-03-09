package com.skillloader.registry;

import com.skillloader.model.Skill;

import java.util.List;
import java.util.Optional;

/**
 * Skill 注册表接口。
 */
public interface SkillRegistry {
    
    /**
     * 发现所有可用的 skills（按优先级去重）。
     */
    List<Skill> discover();
    
    /**
     * 按名称查找 skill。
     */
    Optional<Skill> find(String name);
    
    /**
     * 按 source 查找 skills。
     */
    List<Skill> findBySource(com.skillloader.model.SkillSource source);
    
    /**
     * 获取所有 skill 名称。
     */
    List<String> getSkillNames();
    
    /**
     * 检查是否存在指定 skill。
     */
    boolean hasSkill(String name);
}
