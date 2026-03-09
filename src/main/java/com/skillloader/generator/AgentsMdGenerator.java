package com.skillloader.generator;

import com.skillloader.model.Skill;

import java.util.List;

/**
 * AGENTS.md 生成器接口。
 */
public interface AgentsMdGenerator {
    
    /**
     * 生成完整的 AGENTS.md 内容。
     */
    String generate(List<Skill> skills);
    
    /**
     * 更新现有的 AGENTS.md 内容。
     */
    String updateExisting(String existingContent, List<Skill> skills);
}
