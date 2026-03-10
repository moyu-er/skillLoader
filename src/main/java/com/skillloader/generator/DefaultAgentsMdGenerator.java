package com.skillloader.generator;

import com.skillloader.model.Skill;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 默认 AGENTS.md 生成器实现。
 */
public class DefaultAgentsMdGenerator implements AgentsMdGenerator {
    
    private static final String MARKER_START = "<!-- SKILLS_TABLE_START -->";
    private static final String MARKER_END = "<!-- SKILLS_TABLE_END -->";
    
    @Override
    public String generate(List<Skill> skills) {
        return generateSkillsSystem(skills);
    }
    
    @Override
    public String updateExisting(String existingContent, List<Skill> skills) {
        String newSection = generateSkillsSection(skills);
        
        if (existingContent.contains(MARKER_START) && existingContent.contains(MARKER_END)) {
            // 替换现有部分
            return existingContent.replaceAll(
                MARKER_START + ".*?" + MARKER_END,
                newSection
            );
        }
        
        // 没有找到标记，追加到末尾
        return existingContent + "\n\n" + generateSkillsSystem(skills);
    }
    
    /**
     * 生成完整的 skills_system 块。
     */
    private String generateSkillsSystem(List<Skill> skills) {
        StringBuilder sb = new StringBuilder();
        sb.append("<skills_system priority=\"1\"\u003e\n\n");
        sb.append("## Available Skills\n\n");
        sb.append(generateSkillsSection(skills));
        sb.append("\n</skills_system\u003e\n");
        return sb.toString();
    }
    
    /**
     * 生成 skills 部分（带标记）。
     */
    private String generateSkillsSection(List<Skill> skills) {
        StringBuilder sb = new StringBuilder();
        sb.append(MARKER_START).append("\n");
        sb.append(generateUsageBlock());
        sb.append(generateSkillsList(skills));
        sb.append(MARKER_END);
        return sb.toString();
    }
    
    /**
     * 生成使用说明块。
     */
    private String generateUsageBlock() {
        return """
            <usage>
            When users ask you to perform tasks, check if any of the available skills 
            below can help complete the task more effectively.

            How to use skills:
            - Load skill content from the XML below
            - Base directory provided for resolving bundled resources

            Usage notes:
            - Only use skills listed in <available_skills> below
            - Do not invoke a skill that is already loaded in your context
            </usage>

            """;
    }
    
    /**
     * 生成 skill 列表。
     */
    private String generateSkillsList(List<Skill> skills) {
        if (skills.isEmpty()) {
            return "<available_skills>\n\n</available_skills>\n";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("<available_skills>\n\n");
        
        for (Skill skill : skills) {
            sb.append(generateSkillEntry(skill));
            sb.append("\n");
        }
        
        sb.append("</available_skills>\n");
        return sb.toString();
    }
    
    /**
     * 生成单个 skill 条目。
     */
    private String generateSkillEntry(Skill skill) {
        String description = skill.description() != null ? skill.description() : "";
        // XML 转义
        description = escapeXml(description);
        
        return """
            <skill>
            <name>%s</name>
            <description>%s</description>
            <location>%s</location>
            </skill>
            """.formatted(
                skill.name(),
                description,
                skill.source().toString().toLowerCase()
            );
    }
    
    /**
     * XML 转义。
     */
    private String escapeXml(String text) {
        if (text == null) return "";
        return text
            .replace("&", "&")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}
