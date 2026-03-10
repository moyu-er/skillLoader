# SkillLoader SDK 使用指南

本文档详细介绍 SkillLoader SDK 的各种使用场景和 API。

## 目录

- [Skill 目录结构](#skill-目录结构)
- [SKILL.md 格式](#skillmd-格式)
- [API 使用详解](#api-使用详解)
- [AGENTS.md 生成](#agentsmd-生成)
- [高级用法](#高级用法)

## Skill 目录结构

每个 skill 是一个独立的目录，包含 `SKILL.md` 文件：

```
skills/
├── pdf/                      # Skill 目录名
│   ├── SKILL.md             # 必需：Skill 定义文件
│   ├── references/          # 可选：参考文档
│   │   └── api-reference.md
│   ├── scripts/             # 可选：脚本文件
│   │   └── extract-text.py
│   └── assets/              # 可选：图片等资源
│       └── icon.png
├── git-workflow/
│   ├── SKILL.md
│   └── references/
│       └── pr-template.md
└── ...
```

## SKILL.md 格式

SKILL.md 使用 YAML Frontmatter + Markdown 格式：

```markdown
---
name: pdf
description: PDF manipulation toolkit for extracting text and creating documents
context: document-processing
tags: [pdf, document, extraction]
author: Your Name
version: 1.0.0
---

# PDF 处理工具

## 功能概述

本 skill 提供 PDF 文档处理功能...

## 使用示例

```java
// 示例代码
```

## 注意事项

1. 注意点一
2. 注意点二
```

### Frontmatter 字段说明

| 字段 | 必需 | 说明 |
|------|------|------|
| `name` | 是 | Skill 唯一标识 |
| `description` | 是 | Skill 描述 |
| `context` | 否 | 使用场景/上下文 |
| `tags` | 否 | 标签列表 |
| `author` | 否 | 作者 |
| `version` | 否 | 版本号 |

## API 使用详解

### 1. 发现 Skills

```java
SkillLoader loader = SkillLoader.createDefault();

// 发现所有 skills
List<Skill> skills = loader.discover();

for (Skill skill : skills) {
    System.out.println("名称: " + skill.name());
    System.out.println("描述: " + skill.description());
    System.out.println("来源: " + skill.source());  // CLASSPATH / PROJECT / GLOBAL
    System.out.println("路径: " + skill.location());
    System.out.println("优先级: " + skill.priority());
}
```

### 2. 加载 Skill 内容

```java
// 加载完整内容
SkillContent content = loader.load("pdf");

// 获取元数据
SkillMetadata metadata = content.metadata();
System.out.println("名称: " + metadata.name());
System.out.println("描述: " + metadata.description());
System.out.println("上下文: " + metadata.context());
System.out.println("标签: " + metadata.tags());

// 获取 Markdown 内容
String markdown = content.markdownContent();

// 获取资源文件列表
List<ResourceRef> resources = content.resources();
for (ResourceRef ref : resources) {
    System.out.println("资源: " + ref.name() + " (" + ref.type() + ")");
}
```

### 3. 获取元数据（不加载完整内容）

```java
Optional<SkillMetadata> metadata = loader.getMetadata("pdf");

metadata.ifPresent(m -> {
    System.out.println("名称: " + m.name());
    System.out.println("描述: " + m.description());
});
```

### 4. 检查 Skill 是否存在

```java
boolean exists = loader.registry().find("pdf").isPresent();
```

## AGENTS.md 生成

SkillLoader 可以自动生成 AGENTS.md 文件，用于 AI Agent 的上下文管理。

### 启用生成功能

```java
SkillLoader loader = SkillLoader.builder()
    .addFilesystemPath("skills", "./skills")
    .enableGenerator()  // 启用 AGENTS.md 生成
    .build();
```

### 生成内容

```java
// 生成 AGENTS.md 内容字符串
String agentsMd = loader.generateAgentsMd();
System.out.println(agentsMd);
```

### 同步到文件

```java
// 完整写入文件
loader.syncToFile(Path.of("AGENTS.md"));

// 更新现有文件（保留其他内容）
loader.updateFile(Path.of("AGENTS.md"));
```

### AGENTS.md 格式示例

```markdown
# AI Agent Skills

<!-- SKILLS_TABLE_START -->
| Skill | Description | Tags |
|-------|-------------|------|
| pdf | PDF manipulation toolkit | pdf, document |
| git-workflow | Git workflow guide | git, workflow |
<!-- SKILLS_TABLE_END -->

<skills_system>
<skill name="pdf">
<description>PDF manipulation toolkit...</description>
<context>document-processing</context>
<tags>pdf, document, extraction</tags>
</skill>
...
</skills_system>
```

## 高级用法

### 1. 自定义配置

```java
SkillLoader loader = SkillLoader.builder()
    // 添加文件系统路径
    .addFilesystemPath("project", "./skills", 10, false)
    
    // 添加 classpath 路径
    .addClasspathPath("builtin", "skills", 20, false)
    
    // 使用 PathEntry 完整配置
    .addPath(new PathEntry(
        "global",           // 名称
        "~/.skills",        // 路径
        30,                 // 优先级
        false,              // 是否必需
        PathType.FILESYSTEM // 类型
    ))
    
    // 配置解析器
    .parser(new ParserConfig("SKILL.md", "UTF-8"))
    
    // 配置安全选项
    .security(new SecurityConfig(true, false, 3))
    
    // 启用 AGENTS.md 生成
    .enableGenerator()
    
    .build();
```

### 2. 从配置文件加载

YAML 配置 (`skillloader.yml`)：

```yaml
skillloader:
  paths:
    - name: project
      path: ./skills
      priority: 10
      required: false
      type: filesystem
    - name: builtin
      path: skills
      priority: 20
      required: false
      type: classpath
  
  security:
    strict-mode: true
    allow-symlinks: false
    max-depth: 3
  
  parser:
    marker-file: SKILL.md
    encoding: UTF-8
  
  generator:
    enabled: true
    template: default
    marker-start: "<!-- SKILLS_TABLE_START -->"
    marker-end: "<!-- SKILLS_TABLE_END -->"
```

加载：

```java
SkillLoader loader = SkillLoader.fromConfig(Path.of("skillloader.yml"));
```

### 3. 多路径优先级控制

```java
// 高优先级路径（数字小）
Path highPriority = Path.of("./skills-v2");
// 低优先级路径（数字大）
Path lowPriority = Path.of("./skills-v1");

// 两个路径都有 "pdf" skill
SkillLoader loader = SkillLoader.builder()
    .addPath(new PathEntry("v2", highPriority.toString(), 5, false, PathType.FILESYSTEM))
    .addPath(new PathEntry("v1", lowPriority.toString(), 10, false, PathType.FILESYSTEM))
    .build();

// 加载时会优先使用 v2 版本的 pdf skill
SkillContent content = loader.load("pdf");
```

### 4. 错误处理

```java
try {
    SkillContent content = loader.load("non-existent");
} catch (SkillNotFoundException e) {
    System.out.println("Skill 不存在: " + e.getMessage());
} catch (SkillParseException e) {
    System.out.println("解析失败: " + e.getMessage());
} catch (SecurityException e) {
    System.out.println("安全错误: " + e.getMessage());
}
```

### 5. 获取配置信息

```java
// 获取所有配置的路径
List<PathEntry> paths = loader.getAllowedPaths();

// 获取完整配置
SkillLoaderConfig config = loader.getConfig();
System.out.println("解析器配置: " + config.parser());
System.out.println("安全配置: " + config.security());
```

### 6. 使用 Registry 直接操作

```java
SkillRegistry registry = loader.registry();

// 查找特定 skill
Optional<Skill> skill = registry.find("pdf");

// 发现所有 skills
List<Skill> allSkills = registry.discover();

// 按名称过滤
List<Skill> filtered = registry.discover(skill -> 
    skill.name().startsWith("git")
);
```

## 最佳实践

1. **路径优先级**：将用户自定义 skills 设置为高优先级，内置 skills 设置为低优先级
2. **错误处理**：始终处理 `SkillNotFoundException` 异常
3. **配置管理**：使用配置文件管理复杂配置，代码中只使用简单配置
4. **资源管理**：将大文件放在 `assets` 目录，脚本放在 `scripts` 目录
5. **版本控制**：在 SKILL.md 的 frontmatter 中标注版本号
