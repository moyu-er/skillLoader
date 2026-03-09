---
name: skill-loader-usage
description: SkillLoader Java SDK 使用指南。包含配置方法、API 使用、安全规范等完整说明。
tags: [java, sdk, skill-loader, guide]
---

# SkillLoader 使用指南

Java 21 轻量级 Skill Loader SDK，用于从本地目录加载 AI Agent Skills。

## 核心特性

- 🔒 **只读设计** - 仅加载，不安装/不修改
- 🛡️ **白名单机制** - 只能读取配置中指定的目录
- 📂 **多路径支持** - 支持多个路径，按优先级去重
- ⚡ **零依赖** - 仅用 Java 21 标准库

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.skillloader</groupId>
    <artifactId>skill-loader-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 配置文件

创建 `skillloader.yml`：

```yaml
skillloader:
  paths:
    - name: project-skills
      path: ./skills
      priority: 10
      
    - name: resources-skills
      path: classpath:/skills
      priority: 20
```

### 3. 使用代码

```java
// 创建 loader
SkillLoader loader = SkillLoader.createDefault();

// 发现所有 skills
List<Skill> skills = loader.discover();
System.out.println("Found " + skills.size() + " skills");

// 加载特定 skill
SkillContent content = loader.load("python-quality");
System.out.println(content.markdownContent());

// 生成 AGENTS.md
String agentsMd = loader.generateAgentsMd();
Files.writeString(Path.of("AGENTS.md"), agentsMd);
```

## 配置详解

### 路径配置

```yaml
skillloader:
  paths:
    # 项目本地 skills（最高优先级）
    - name: project-local
      path: ./skills
      priority: 10
      required: false
      
    # Classpath 内的 skills
    - name: classpath-skills
      path: classpath:/skills
      priority: 20
      required: false
      
    # 用户全局 skills
    - name: user-global
      path: ${user.home}/.skillloader/skills
      priority: 30
      required: false
```

### 安全配置

```yaml
skillloader:
  security:
    strict-mode: true      # 严格模式：只能读白名单路径
    allow-symlinks: false  # 禁止符号链接
    max-depth: 3           # 目录扫描最大深度
```

### 生成配置

```yaml
skillloader:
  generator:
    template: default
    marker-start: "<!-- SKILLS_TABLE_START -->"
    marker-end: "<!-- SKILLS_TABLE_END -->"
```

## API 参考

### SkillLoader

```java
// 工厂方法
static SkillLoader createDefault()
static SkillLoader fromConfig(Path configPath)
static SkillLoader fromConfig(SkillLoaderConfig config)

// 核心方法
List<Skill> discover()
SkillContent load(String skillName)
Optional<SkillMetadata> getMetadata(String skillName)
String generateAgentsMd()

// 调试
List<PathEntry> getAllowedPaths()
SkillLoaderConfig getConfig()
```

### Skill

```java
public record Skill(
    String name,
    String description,
    SkillSource source,
    Path location,
    int priority
)
```

### SkillContent

```java
public record SkillContent(
    SkillMetadata metadata,
    String markdownContent,
    Path baseDir,
    List<ResourceRef> resources
)
```

## Spring Boot 集成

```java
@Configuration
public class SkillLoaderConfig {
    
    @Bean
    public SkillLoader skillLoader() {
        return SkillLoader.createDefault();
    }
    
    @Bean
    public String agentsMdContent(SkillLoader loader) {
        return loader.generateAgentsMd();
    }
}
```

## AGENTS.md 输出格式

```xml
<skills_system priority="1">

## Available Skills

<!-- SKILLS_TABLE_START -->
<usage>
When users ask you to perform tasks, check if any of the available skills 
below can help complete the task more effectively.
</usage>

<available_skills>

<skill>
<name>python-quality</name>
<description>Python 代码质量工具集合</description>
<location>project</location>
</skill>

<skill>
<name>eslint-config-generator</name>
<description>ESLint 配置生成器</description>
<location>project</location>
</skill>

</available_skills>
<!-- SKILLS_TABLE_END -->

</skills_system>
```

## 安全规范

### 白名单机制

- 只能读取配置中 `paths` 指定的目录
- 所有读取操作前会验证路径在白名单内
- 尝试读取白名单外路径会抛出 `SecurityException`

### 路径遍历防护

```java
// 以下路径会被拒绝：
/etc/passwd                    # 不在白名单
./skills/../../../etc/passwd   # 路径遍历
/path/to/symlink               # 符号链接（当 allowSymlinks=false）
```

### 查看白名单

```java
SkillLoader loader = SkillLoader.createDefault();
List<PathEntry> allowed = loader.getAllowedPaths();
allowed.forEach(e -> System.out.println(e.name() + ": " + e.path()));
```

## 示例项目结构

```
my-project/
├── skillloader.yml          # 配置文件
├── AGENTS.md                # 生成的 agents 文件
├── skills/                  # 项目本地 skills
│   ├── my-custom-skill/
│   │   └── SKILL.md
│   └── another-skill/
│       └── SKILL.md
└── src/
    └── main/
        └── resources/
            └── skills/      # Classpath skills
                └── builtin-skill/
                    └── SKILL.md
```

## 故障排查

### Skill 未发现

```java
// 检查白名单路径
loader.getAllowedPaths().forEach(System.out::println);

// 检查目录是否存在
loader.discover().forEach(s -> System.out.println(s.name()));
```

### 安全异常

```java
try {
    loader.load("some-skill");
} catch (SecurityException e) {
    // 路径不在白名单中
    System.out.println("Allowed paths: " + loader.getAllowedPaths());
}
```

### 配置错误

```java
try {
    SkillLoader loader = SkillLoader.fromConfig(Path.of("skillloader.yml"));
} catch (ConfigException e) {
    // 检查配置文件格式
    System.out.println(e.getMessage());
}
```

## 相关链接

- [GitHub 仓库](https://github.com/xxsddm/skillLoader)
- [设计文档](DESIGN-v3.md)
