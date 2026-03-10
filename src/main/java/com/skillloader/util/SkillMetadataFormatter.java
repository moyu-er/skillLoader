package com.skillloader.util;

import com.skillloader.model.Skill;
import com.skillloader.model.SkillContent;
import com.skillloader.model.SkillMetadata;

import java.nio.file.Path;
import java.util.function.Function;

/**
 * Skill 元信息格式化工具。
 * 提供灵活的方式生成 skill 描述字符串。
 */
public final class SkillMetadataFormatter {
    
    private SkillMetadataFormatter() {
        // 工具类，禁止实例化
    }
    
    /**
     * 默认格式化 skill 为简洁字符串（智能省略空字段）。
     * 格式：name: description [tags]
     *
     * @param skill skill 对象
     * @return 格式化字符串
     */
    public static String format(Skill skill) {
        StringBuilder sb = new StringBuilder();
        sb.append("- **").append(skill.name()).append("**");
        
        if (!skill.description().isBlank()) {
            sb.append(": ").append(skill.description());
        }
        
        return sb.toString();
    }
    
    /**
     * 格式化 skill 为详细字符串（包含 source 和 priority）。
     *
     * @param skill skill 对象
     * @return 格式化字符串
     */
    public static String formatDetailed(Skill skill) {
        StringBuilder sb = new StringBuilder();
        sb.append("- **").append(skill.name()).append("**");
        
        if (!skill.description().isBlank()) {
            sb.append(": ").append(skill.description());
        }
        
        sb.append(" [").append(skill.source()).append(", priority=").append(skill.priority()).append("]");
        
        return sb.toString();
    }
    
    /**
     * 从 SkillContent 格式化（包含完整元数据）。
     * 智能省略空字段（tags 为空时不显示，context 为空时不显示）。
     *
     * @param content skill 内容
     * @return 格式化字符串
     */
    public static String format(SkillContent content) {
        return format(content.metadata(), FormatOptions.defaults());
    }
    
    /**
     * 从 SkillMetadata 格式化（智能省略空字段）。
     *
     * @param metadata 元数据
     * @return 格式化字符串
     */
    public static String format(SkillMetadata metadata) {
        return format(metadata, FormatOptions.defaults());
    }
    
    /**
     * 使用自定义选项格式化元数据。
     *
     * @param metadata 元数据
     * @param options 格式化选项
     * @return 格式化字符串
     */
    public static String format(SkillMetadata metadata, FormatOptions options) {
        StringBuilder sb = new StringBuilder();
        
        // 名称（始终显示）
        sb.append(options.namePrefix())
          .append(metadata.name())
          .append(options.nameSuffix());
        
        // 描述
        if (options.includeDescription() && metadata.description() != null && !metadata.description().isBlank()) {
            sb.append(options.separator()).append(metadata.description());
        }
        
        // 上下文
        if (options.includeContext() && metadata.context().isPresent()) {
            sb.append(options.fieldSeparator())
              .append("context: ")
              .append(metadata.context().get());
        }
        
        // 标签（为空时自动省略）
        if (options.includeTags() && !metadata.tags().isEmpty()) {
            sb.append(options.fieldSeparator())
              .append("tags: ")
              .append(String.join(options.tagSeparator(), metadata.tags()));
        }
        
        // 额外字段
        if (options.includeExtra() && !metadata.extra().isEmpty()) {
            metadata.extra().forEach((key, value) -> {
                if (value != null) {
                    sb.append(options.fieldSeparator())
                      .append(key).append(": ").append(value);
                }
            });
        }
        
        return sb.toString();
    }
    
    /**
     * 自定义格式化（使用函数式接口）。
     *
     * @param skill skill 对象
     * @param formatter 自定义格式化函数
     * @return 格式化字符串
     */
    public static String format(Skill skill, Function<Skill, String> formatter) {
        return formatter.apply(skill);
    }
    
    /**
     * 生成系统提示词格式的 skills 列表。
     *
     * @param skills skill 列表
     * @param title 标题（如 "可用 Skills"）
     * @return 系统提示词片段
     */
    public static String toSystemPrompt(Iterable<Skill> skills, String title) {
        StringBuilder sb = new StringBuilder();
        sb.append("## ").append(title).append("\n\n");
        
        for (Skill skill : skills) {
            sb.append(format(skill)).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 生成系统提示词格式的 skills 列表（使用自定义格式化器）。
     *
     * @param skills skill 列表
     * @param title 标题
     * @param formatter 自定义格式化函数
     * @return 系统提示词片段
     */
    public static String toSystemPrompt(Iterable<Skill> skills, String title, Function<Skill, String> formatter) {
        StringBuilder sb = new StringBuilder();
        sb.append("## ").append(title).append("\n\n");
        
        for (Skill skill : skills) {
            sb.append(formatter.apply(skill)).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 格式化选项。
     */
    public static class FormatOptions {
        private final String namePrefix;
        private final String nameSuffix;
        private final String separator;
        private final String fieldSeparator;
        private final String tagSeparator;
        private final boolean includeDescription;
        private final boolean includeContext;
        private final boolean includeTags;
        private final boolean includeExtra;
        
        private FormatOptions(Builder builder) {
            this.namePrefix = builder.namePrefix;
            this.nameSuffix = builder.nameSuffix;
            this.separator = builder.separator;
            this.fieldSeparator = builder.fieldSeparator;
            this.tagSeparator = builder.tagSeparator;
            this.includeDescription = builder.includeDescription;
            this.includeContext = builder.includeContext;
            this.includeTags = builder.includeTags;
            this.includeExtra = builder.includeExtra;
        }
        
        public static FormatOptions defaults() {
            return new Builder().build();
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        // Getters
        public String namePrefix() { return namePrefix; }
        public String nameSuffix() { return nameSuffix; }
        public String separator() { return separator; }
        public String fieldSeparator() { return fieldSeparator; }
        public String tagSeparator() { return tagSeparator; }
        public boolean includeDescription() { return includeDescription; }
        public boolean includeContext() { return includeContext; }
        public boolean includeTags() { return includeTags; }
        public boolean includeExtra() { return includeExtra; }
        
        /**
         * 格式化选项构建器。
         */
        public static class Builder {
            private String namePrefix = "- **";
            private String nameSuffix = "**";
            private String separator = ": ";
            private String fieldSeparator = " | ";
            private String tagSeparator = ", ";
            private boolean includeDescription = true;
            private boolean includeContext = false;
            private boolean includeTags = true;
            private boolean includeExtra = false;
            
            public Builder namePrefix(String prefix) {
                this.namePrefix = prefix;
                return this;
            }
            
            public Builder nameSuffix(String suffix) {
                this.nameSuffix = suffix;
                return this;
            }
            
            public Builder separator(String separator) {
                this.separator = separator;
                return this;
            }
            
            public Builder fieldSeparator(String separator) {
                this.fieldSeparator = separator;
                return this;
            }
            
            public Builder tagSeparator(String separator) {
                this.tagSeparator = separator;
                return this;
            }
            
            public Builder includeDescription(boolean include) {
                this.includeDescription = include;
                return this;
            }
            
            public Builder includeContext(boolean include) {
                this.includeContext = include;
                return this;
            }
            
            public Builder includeTags(boolean include) {
                this.includeTags = include;
                return this;
            }
            
            public Builder includeExtra(boolean include) {
                this.includeExtra = include;
                return this;
            }
            
            public FormatOptions build() {
                return new FormatOptions(this);
            }
        }
    }
}
