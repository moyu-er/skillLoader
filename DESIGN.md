# SkillLoader Java SDK - 精简设计方案

## 核心目标
- **纯本地 skill 加载器** - 不涉及 Git 下载/远程安装
- **轻量级** - 最小依赖，Java 21 标准库为主
- **服务大模型** - 核心是将本地 skills 整理成 AGENTS.md 格式供 LLM 读取

## 功能范围（精简后）

### ✅ 包含功能
1. **Skill 发现** - 扫描指定目录下的所有 skill
2. **Skill 读取** - 解析 SKILL.md 的 YAML frontmatter + markdown
3. **AGENTS.md 生成** - 生成 `<skills_system>` XML 块
4. **Skill 查询** - 按名称、标签等查找 skill

### ❌ 不包含功能
- Git 克隆/远程安装
- Skill 版本管理
- Skill 更新检查
- 复杂的存储抽象

## 架构设计

```
┌─────────────────────────────────────────────┐
│           SkillLoader (Facade)              │
├─────────────────────────────────────────────┤
│  ┌──────────────┐      ┌──────────────┐    │
│  │ SkillScanner │      │ SkillParser  │    │
│  │   (发现)     │ ────▶│   (解析)     │    │
│  └──────────────┘      └──────────────┘    │
│         │                    │             │
│         ▼                    ▼             │
│  ┌──────────────────────────────────────┐  │
│  │         SkillRegistry                │  │
│  │   (内存中的 skill 索引)              │  │
│  └──────────────────────────────────────┘  │
│                    │                       │
│                    ▼                       │
│  ┌──────────────────────────────────────┐  │
│  │      AgentsMdGenerator               │  │
│  │   (生成 AGENTS.md XML)               │  │
│  └──────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
```

## 模块结构

```
skill-loader/
├── src/main/java/com/skillloader/
│   ├── SkillLoader.java              # Facade 入口
│   ├── config/
│   │   └── SkillLoaderConfig.java    # 配置类
│   ├── model/
│   │   ├── Skill.java                # Skill 领域对象
│   │   ├── SkillMetadata.java        # YAML frontmatter
│   │   └── SkillContent.java         # 完整内容
│   ├── scanner/
│   │   └── SkillScanner.java         # 目录扫描器
│   ├── parser/
│   │   └── SkillParser.java          # SKILL.md 解析器
│   ├── registry/
│   │   └── SkillRegistry.java        # Skill 注册表
│   └── generator/
│       └── AgentsMdGenerator.java    # AGENTS.md 生成器
└── pom.xml
```

## 核心接口

### 1. SkillLoader (门面)
```java
public class SkillLoader {
    // 快速开始
    public static SkillLoader createDefault(Path projectDir);
    
    // 扫描 skills
    public List<Skill> scan();
    
    // 加载 skill 内容
    public SkillContent load(String skillName);
    
    // 生成 AGENTS.md 内容
    public String generateAgentsMd();
    
    // 同步到文件
    public void syncToFile(Path agentsMdPath);
}
```

### 2. 配置
```java
public record SkillLoaderConfig(
    Path projectDir,                    // 项目目录
    Path userHome,                      // 用户主目录
    List<Path> customSearchPaths,       // 自定义搜索路径
    String skillsFolderName,            // 默认: ".agent/skills"
    boolean includeGlobalSkills         // 是否包含 ~/.agent/skills
) {
    // Builder 模式
    public static Builder builder() { }
}
```

### 3. 数据模型
```java
// Skill 定义
public record Skill(
    String name,
    String description,
    Path baseDir,
    SkillSource source  // PROJECT or GLOBAL
) {}

// YAML Frontmatter
public record SkillMetadata(
    String name,
    String description,
    Optional<String> context,
    Optional<List<String>> tags,
    Map<String, Object> extra
) {}

// 完整内容（给 LLM 使用）
public record SkillContent(
    SkillMetadata metadata,
    String markdownContent,
    Path baseDir,
    List<Path> resources  // references/, scripts/ 等
) {}
```

### 4. SkillScanner
```java
public class SkillScanner {
    // 扫描所有路径，按优先级去重
    public List<Skill> scan();
    
    // 搜索优先级：
    // 1. ${projectDir}/.agent/skills/
    // 2. ${userHome}/.agent/skills/ (如果 includeGlobalSkills=true)
    
    // 检查路径是否是有效的 skill
    private boolean isValidSkill(Path skillDir);
}
```

### 5. SkillParser
```java
public class SkillParser {
    // 解析 SKILL.md 文件
    public SkillContent parse(Path skillDir);
    
    // 解析 YAML frontmatter
    public SkillMetadata parseMetadata(String content);
    
    // 提取 markdown 正文
    public String parseMarkdown(String content);
}
```

### 6. AgentsMdGenerator
```java
public class AgentsMdGenerator {
    // 生成完整的 AGENTS.md 内容
    public String generate(List<Skill> skills);
    
    // 生成 skills_system XML 块
    public String generateSkillsSystem(List<Skill> skills);
    
    // 更新现有 AGENTS.md
    public String updateExisting(String existingContent, List<Skill> skills);
}
```

## SKILL.md 格式

```markdown
---
name: pdf
description: PDF manipulation toolkit
context: document-processing
tags: [document, pdf, extraction]
---

# PDF Skill

When user asks about PDFs, follow these steps:
...
```

## AGENTS.md 输出格式

```xml
<skills_system priority="1">

## Available Skills

<!-- SKILLS_TABLE_START -->
<usage>
When users ask you to perform tasks, check if any of the available skills 
below can help complete the task more effectively.

How to use skills:
- Invoke: Load skill content from the XML below
- Base directory provided for resolving bundled resources

Usage notes:
- Only use skills listed in <available_skills> below
- Do not invoke a skill that is already loaded in your context
</usage>

<available_skills>

<skill>
<name>pdf</name>
<description>PDF manipulation toolkit</description>
<location>project</location>
</skill>

<skill>
<name>weather</name>
<description>Weather forecast queries</description>
<location>global</location>
</skill>

</available_skills>
<!-- SKILLS_TABLE_END -->

</skills_system>
```

## 使用示例

### 基础用法
```java
// 创建 loader
SkillLoader loader = SkillLoader.createDefault(Path.of("."));

// 扫描所有 skills
List<Skill> skills = loader.scan();
System.out.println("Found " + skills.size() + " skills");

// 加载特定 skill（给 LLM 用）
SkillContent pdf = loader.load("pdf");
systemPrompt += "\n\n" + pdf.markdownContent();

// 生成 AGENTS.md
loader.syncToFile(Path.of("AGENTS.md"));
```

### 高级配置
```java
SkillLoaderConfig config = SkillLoaderConfig.builder()
    .projectDir(Path.of("/my/project"))
    .includeGlobalSkills(true)
    .customSearchPaths(List.of(
        Path.of("/custom/skills")
    ))
    .build();

SkillLoader loader = new SkillLoader(config);
```

### 在 AI Agent 中使用
```java
public class MyAgent {
    private final SkillLoader skillLoader;
    
    public MyAgent() {
        this.skillLoader = SkillLoader.createDefault(Path.of("."));
    }
    
    public String buildSystemPrompt() {
        // 生成包含 skills 的 system prompt
        return skillLoader.generateAgentsMd();
    }
    
    public String loadSkillForContext(String skillName) {
        SkillContent content = skillLoader.load(skillName);
        return content.markdownContent();
    }
}
```

## 依赖规划

```xml
<dependencies>
    <!-- 仅用 Java 21 标准库 -->
    <!-- 可能需要轻量级 YAML 解析 -->
    <dependency>
        <groupId>org.yaml</groupId>
        <artifactId>snakeyaml</artifactId>
        <version>2.2</version>
    </dependency>
</dependencies>
```

## 实现优先级

1. **P0** - Core: SkillScanner, SkillParser, SkillLoader
2. **P1** - Registry: SkillRegistry (内存索引)
3. **P2** - Generator: AgentsMdGenerator
4. **P3** - Utils: 配置 Builder, 工具类

## 预估代码量

- 总代码行数: ~800-1000 行
- 核心类: 6-8 个
- 零复杂依赖
