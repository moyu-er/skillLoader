package com.skillloader.parser;

import com.skillloader.api.exceptions.SkillParseException;
import com.skillloader.model.SkillContent;
import com.skillloader.model.SkillMetadata;

import java.nio.file.Path;

/**
 * Skill 解析器接口。
 */
public interface SkillParser {
    
    /**
     * 解析 skill 目录，返回完整内容。
     */
    SkillContent parse(Path skillDir) throws SkillParseException;
    
    /**
     * 仅解析元数据（快速检查）。
     */
    SkillMetadata parseMetadata(Path skillDir) throws SkillParseException;
}
