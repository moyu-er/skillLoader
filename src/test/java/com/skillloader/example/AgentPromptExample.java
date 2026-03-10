package com.skillloader.example;

import com.skillloader.api.SkillLoader;
import com.skillloader.model.Skill;
import com.skillloader.model.SkillContent;

import java.util.List;

/**
 * SkillLoader 使用示例：如何获取 description 并构建系统提示词
 */
public class AgentPromptExample {
    
    public static void main(String[] args) {
        // 1. 创建 SkillLoader
        SkillLoader loader = SkillLoader.createDefault();
        
        // 2. 发现所有 skills
        List<Skill> skills = loader.discover();
        
        System.out.println("=== 发现的 Skills ===\\n");
        
        // 3. 遍历并打印每个 skill 的 description ✅
        for (Skill skill : skills) {
            System.out.println("Skill 名称: " + skill.name());
            System.out.println("描述: " + skill.description());  // ← 关键：获取 description
            System.out.println("来源: " + skill.source());
            System.out.println("优先级: " + skill.priority());
            System.out.println("---");
        }
        
        System.out.println("\\n=== 构建系统提示词 ===\\n");
        
        // 4. 构建系统提示词（包含所有 skills 的 description）
        String systemPrompt = buildSystemPrompt(skills);
        System.out.println(systemPrompt);
        
        // 5. 模拟用户请求，加载特定 skill
        System.out.println("\\n=== 用户请求：Git 工作流 ===\\n");
        
        // 根据用户输入找到相关 skill
        String userRequest = "请告诉我如何提交代码";
        String relevantSkill = findRelevantSkill(userRequest, skills);
        
        if (relevantSkill != null) {
            // 加载完整内容
            SkillContent content = loader.load(relevantSkill);
            
            System.out.println("找到相关 skill: " + content.metadata().name());
            System.out.println("描述: " + content.metadata().description());
            System.out.println("\\n完整内容预览:");
            System.out.println(content.markdownContent().substring(0, 
                Math.min(500, content.markdownContent().length())));
        }
    }
    
    /**
     * 构建系统提示词
     */
    private static String buildSystemPrompt(List<Skill> skills) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("你是一个智能助手，可以使用以下 skills 来帮助用户完成任务。\\n\\n");
        prompt.append("## 可用 Skills\\n\\n");
        
        for (Skill skill : skills) {
            // ✅ 将 description 填入系统提示词
            prompt.append("- **").append(skill.name()).append("**: ")
                  .append(skill.description()).append("\\n");
        }
        
        prompt.append("\\n## 使用说明\\n\\n");
        prompt.append("当用户提出需求时：\\n");
        prompt.append("1. 检查是否有匹配的 skill\\n");
        prompt.append("2. 如果有，按照 skill 中的规范执行\\n");
        prompt.append("3. 如果没有，使用通用知识回答\\n");
        
        return prompt.toString();
    }
    
    /**
     * 根据用户输入找到相关 skill（简单关键词匹配）
     */
    private static String findRelevantSkill(String userRequest, List<Skill> skills) {
        String lowerRequest = userRequest.toLowerCase();
        
        for (Skill skill : skills) {
            String name = skill.name().toLowerCase();
            String desc = skill.description().toLowerCase();
            
            // 简单匹配：检查关键词是否出现在名称或描述中
            if (lowerRequest.contains(name) || 
                name.contains(lowerRequest.replaceAll(".*\\s+", ""))) {
                return skill.name();
            }
            
            // 检查描述中的关键词
            String[] keywords = lowerRequest.split("\\s+");
            for (String keyword : keywords) {
                if (keyword.length() > 2 && desc.contains(keyword)) {
                    return skill.name();
                }
            }
        }
        
        return null;
    }
}
