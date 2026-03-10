# SkillLoader 使用指南

## 1. 快速开始

### 1.1 添加依赖

```xml
<dependency>
    <groupId>com.skillloader</groupId>
    <artifactId>skill-loader-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 1.2 准备 Skill 目录

```
你的项目/
├── skills/                    # Skill 目录
│   ├── git-workflow/         # 每个 skill 一个子目录
│   │   └── SKILL.md          # 必须包含 SKILL.md
│   └── python-quality/
│       ├── SKILL.md
│       ├── references/
│       │   └── pyproject-snippet.toml
│       └── scripts/
│           └── check.py
└── src/...
```

### 1.3 SKILL.md 格式

```markdown
---
name: git-workflow
description: Git 工作流规范，包含分支管理、PR 流程等
tags: [git, workflow, pr]
author: Your Name
version: 1.0.0
---

# Git 工作流规范

详细内容...
```

---

## 2. 基础使用（只读模式）

### 2.1 发现所有 Skills

```java
import com.skillloader.api.SkillLoader;
import com.skillloader.model.Skill;
import java.util.List;

public class SkillDemo {
    public static void main(String[] args) {
        // 创建 loader（默认读取 ./skills 目录）
        SkillLoader loader = SkillLoader.createDefault();
        
        // 发现所有 skills
        List<Skill> skills = loader.discover();
        
        // 打印基本信息
        for (Skill skill : skills) {
            System.out.println("名称: " + skill.name());
            System.out.println("描述: " + skill.description());  // ✅ 这里获取 description
            System.out.println("路径: " + skill.location());
            System.out.println("来源: " + skill.source());
            System.out.println("---");
        }
    }
}
```

### 2.2 获取完整内容

```java
import com.skillloader.model.SkillContent;
import com.skillloader.model.SkillMetadata;

// 加载特定 skill
SkillContent content = loader.load("git-workflow");

// 获取元数据
SkillMetadata metadata = content.metadata();
String name = metadata.name();              // 名称
String description = metadata.description(); // 描述 ✅
String context = metadata.context();         // 上下文
List<String> tags = metadata.tags();         // 标签
Map<String, Object> extra = metadata.extra(); // 额外字段

// 获取 Markdown 内容
String markdown = content.markdownContent(); // 完整内容

// 获取资源文件
List<ResourceRef> resources = content.resources();
for (ResourceRef ref : resources) {
    System.out.println("资源: " + ref.name());
    System.out.println("类型: " + ref.type());  // SCRIPT, REFERENCE, ASSET
    System.out.println("URI: " + ref.uri());
}
```

---

## 3. 集成到 AI Agent（系统提示词）

### 3.1 构建系统提示词

```java
public class AgentPromptBuilder {
    
    private final SkillLoader loader;
    
    public AgentPromptBuilder() {
        this.loader = SkillLoader.createDefault();
    }
    
    /**
     * 构建系统提示词，包含所有可用 skills
     */
    public String buildSystemPrompt() {
        StringBuilder prompt = new StringBuilder();
        
        // 1. 基础角色定义
        prompt.append("你是一个智能助手，可以使用以下 skills 来帮助用户完成任务。\\n\\n");
        
        // 2. 获取所有 skills
        List<Skill> skills = loader.discover();
        
        // 3. 添加 skill 列表（名称 + 描述）✅
        prompt.append("## 可用 Skills\\n\\n");
        for (Skill skill : skills) {
            prompt.append("- **").append(skill.name()).append("**: ")
                  .append(skill.description()).append("\\n");
        }
        
        // 4. 添加使用说明
        prompt.append("\\n## 如何使用\\n\\n");
        prompt.append("当用户提出需求时，检查是否有合适的 skill 可以应用。\\n");
        prompt.append("如果有，请加载 skill 内容并按照其中的规范执行。\\n");
        
        return prompt.toString();
    }
    
    /**
     * 加载特定 skill 的完整内容到提示词
     */
    public String loadSkillContent(String skillName) {
        try {
            SkillContent content = loader.load(skillName);
            SkillMetadata metadata = content.metadata();
            
            StringBuilder sb = new StringBuilder();
            sb.append("# Skill: ").append(metadata.name()).append("\\n\\n");
            sb.append("描述: ").append(metadata.description()).append("\\n");
            sb.append("标签: ").append(String.join(", ", metadata.tags())).append("\\n\\n");
            sb.append("---\\n\\n");
            sb.append(content.markdownContent());
            
            return sb.toString();
        } catch (Exception e) {
            return "Skill not found: " + skillName;
        }
    }
}
```

### 3.2 使用示例

```java
public class AIAgent {
    public static void main(String[] args) {
        AgentPromptBuilder builder = new AgentPromptBuilder();
        
        // 构建系统提示词
        String systemPrompt = builder.buildSystemPrompt();
        System.out.println("=== 系统提示词 ===");
        System.out.println(systemPrompt);
        
        // 用户请求时加载特定 skill
        String userRequest = "帮我检查代码风格";
        
        // 判断应该使用哪个 skill
        if (userRequest.contains("代码风格")) {
            String pythonSkill = builder.loadSkillContent("python-quality");
            System.out.println("\\n=== Python 代码质量规范 ===");
            System.out.println(pythonSkill);
        }
    }
}
```

---

## 4. 高级用法

### 4.1 自定义配置

```java
SkillLoader loader = SkillLoader.builder()
    // 添加多个路径
    .addFilesystemPath("project", "./my-skills", 10, false)
    .addClasspathPath("builtin", "/skills", 20, false)
    
    // 自定义安全配置
    .security(new SecurityConfig(true, false, 5))
    
    // 自定义解析器
    .parser(new ParserConfig("SKILL.md", "UTF-8", 1024 * 1024))
    
    .build();
```

### 4.2 从配置文件加载

创建 `skillloader.yml`：

```yaml
skillloader:
  paths:
    - name: project-skills
      path: ./skills
      priority: 10
      type: filesystem
    
    - name: global-skills
      path: ~/.skills
      priority: 20
      type: filesystem
```

代码中使用：

```java
SkillLoader loader = SkillLoader.fromConfig(Path.of("skillloader.yml"));
```

### 4.3 启用 AGENTS.md 生成（可选）

```java
SkillLoader loader = SkillLoader.builder()
    .addFilesystemPath("project", "./skills")
    .enableGenerator()  // 启用写操作
    .build();

// 生成 AGENTS.md 内容（字符串）
String agentsMd = loader.generateAgentsMd();

// 写入文件（需要 enableGenerator）
loader.syncToFile(Path.of("AGENTS.md"));
```

---

## 5. 完整集成示例（OpenAI API）

```java
import com.skillloader.api.SkillLoader;
import com.skillloader.model.Skill;

public class OpenAIIntegration {
    
    private final SkillLoader loader;
    
    public OpenAIIntegration() {
        this.loader = SkillLoader.createDefault();
    }
    
    public String chat(String userMessage) {
        // 1. 构建系统提示词
        String systemPrompt = buildSystemPromptWithSkills();
        
        // 2. 判断是否需要加载特定 skill
        String skillContent = findRelevantSkill(userMessage);
        if (skillContent != null) {
            systemPrompt += "\\n\\n## 当前任务相关规范\\n\\n" + skillContent;
        }
        
        // 3. 调用 OpenAI API
        // ...
        
        return response;
    }
    
    private String buildSystemPromptWithSkills() {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个智能编程助手。\\n\\n");
        sb.append("可用技能列表：\\n");
        
        for (Skill skill : loader.discover()) {
            sb.append("- ").append(skill.name())
              .append(": ").append(skill.description())
              .append("\\n");
        }
        
        sb.append("\\n当用户提出需求时，如果有匹配的 skill，请按 skill 中的规范执行。");
        
        return sb.toString();
    }
    
    private String findRelevantSkill(String message) {
        // 简单关键词匹配
        if (message.contains("git") || message.contains("分支") || message.contains("提交")) {
            return loader.load("git-workflow").markdownContent();
        }
        if (message.contains("python") || message.contains("代码质量")) {
            return loader.load("python-quality").markdownContent();
        }
        return null;
    }
}
```

---

## 6. 最佳实践

### 6.1 Skill 设计原则

1. **描述要清晰**：description 是 AI 选择 skill 的依据
2. **标签要准确**：便于分类和检索
3. **内容要完整**：包含使用说明、示例、注意事项

### 6.2 性能优化

```java
// 缓存 loader 实例（线程安全）
private static final SkillLoader LOADER = SkillLoader.createDefault();

// 缓存 skill 列表
private List<Skill> cachedSkills;

public List<Skill> getSkills() {
    if (cachedSkills == null) {
        cachedSkills = LOADER.discover();
    }
    return cachedSkills;
}
```

### 6.3 错误处理

```java
try {
    SkillContent content = loader.load("some-skill");
} catch (SkillNotFoundException e) {
    // Skill 不存在
    System.out.println("Skill not found: " + e.getMessage());
} catch (SkillParseException e) {
    // 解析失败（YAML 格式错误等）
    System.out.println("Failed to parse: " + e.getMessage());
}
```

---

## 7. 调试技巧

```java
// 查看所有可用路径
List<PathEntry> paths = loader.getAllowedPaths();

// 查看配置
SkillLoaderConfig config = loader.getConfig();
System.out.println("Max depth: " + config.security().maxDepth());
System.out.println("Marker file: " + config.parser().markerFile());

// 重新扫描
loader.registry().refresh();
List<Skill> updatedSkills = loader.discover();
```

---

## 总结

**核心流程**：
1. 创建 `SkillLoader` 实例
2. 调用 `discover()` 获取所有 skills（含 description）✅
3. 根据用户请求选择合适的 skill
4. 调用 `load(skillName)` 获取完整内容
5. 将 description 和内容填入系统提示词

**关键 API**：
- `skill.description()` - 获取描述（用于选择 skill）
- `content.markdownContent()` - 获取完整内容（用于提示词）
- `content.resources()` - 获取关联资源
