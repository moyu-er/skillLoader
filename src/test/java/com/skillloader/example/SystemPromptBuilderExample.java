package com.skillloader.example;

import com.skillloader.api.SkillLoader;
import com.skillloader.model.Skill;
import com.skillloader.util.SkillMetadataFormatter;

import java.util.List;

/**
 * 使用 SkillMetadataFormatter 构建系统提示词示例。
 */
public class SystemPromptBuilderExample {
    
    public static void main(String[] args) {
        // 创建 loader
        SkillLoader loader = SkillLoader.createDefault();
        
        // 示例 1：简单用法 - 生成系统提示词
        System.out.println("=== 示例 1：简单系统提示词 ===\n");
        String simplePrompt = buildSimpleSystemPrompt(loader);
        System.out.println(simplePrompt);
        
        // 示例 2：自定义格式
        System.out.println("\n=== 示例 2：自定义格式 ===\n");
        String customPrompt = buildCustomSystemPrompt(loader);
        System.out.println(customPrompt);
        
        // 示例 3：完全自定义
        System.out.println("\n=== 示例 3：完全自定义格式 ===\n");
        String fullCustomPrompt = buildFullyCustomPrompt(loader);
        System.out.println(fullCustomPrompt);
    }
    
    /**
     * 简单用法：使用默认格式。
     */
    private static String buildSimpleSystemPrompt(SkillLoader loader) {
        List<Skill> skills = loader.discover();
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个智能编程助手。\n\n");
        
        // 使用工具类生成 skills 列表
        prompt.append(SkillMetadataFormatter.toSystemPrompt(skills, "可用 Skills"));
        
        prompt.append("\n当用户提出需求时，如果有匹配的 skill，请使用 load_skill 工具加载详细内容。\n");
        
        return prompt.toString();
    }
    
    /**
     * 自定义格式：显示更多字段。
     */
    private static String buildCustomSystemPrompt(SkillLoader loader) {
        List<Skill> skills = loader.discover();
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个智能助手。\n\n");
        prompt.append("## 可用 Skills\n\n");
        
        // 为每个 skill 使用详细格式
        for (Skill skill : skills) {
            prompt.append(SkillMetadataFormatter.formatDetailed(skill)).append("\n");
        }
        
        return prompt.toString();
    }
    
    /**
     * 完全自定义：使用函数式接口。
     */
    private static String buildFullyCustomPrompt(SkillLoader loader) {
        List<Skill> skills = loader.discover();
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("🤖 智能助手模式\n\n");
        prompt.append("📚 技能库：\n\n");
        
        // 完全自定义每个 skill 的格式
        for (Skill skill : skills) {
            String formatted = SkillMetadataFormatter.format(skill, s -> {
                // 自定义格式：emoji + 名称 + 换行描述
                return String.format(
                    "• %s\n  └─ %s\n",
                    s.name(),
                    s.description().isEmpty() ? "暂无描述" : s.description()
                );
            });
            prompt.append(formatted);
        }
        
        prompt.append("\n💡 提示：提及任何技能名称，我会加载详细内容帮助你。\n");
        
        return prompt.toString();
    }
    
    /**
     * 使用 FormatOptions 精细控制。
     */
    private static String buildWithOptions(SkillLoader loader) {
        // 假设从 content 获取 metadata
        // SkillContent content = loader.load("git-workflow");
        
        // 自定义选项：包含 context，省略 tags
        SkillMetadataFormatter.FormatOptions options = 
            SkillMetadataFormatter.FormatOptions.builder()
                .namePrefix("### ")           // 用 ### 作为名称前缀
                .nameSuffix("")               // 不加后缀
                .separator("\n")              // 名称和描述换行
                .fieldSeparator("\n- ")       // 字段用列表项
                .includeDescription(true)
                .includeContext(true)          // 显示 context
                .includeTags(false)            // 不显示 tags
                .includeExtra(true)            // 显示额外字段
                .build();
        
        // return SkillMetadataFormatter.format(content.metadata(), options);
        return "示例代码";
    }
}
