# SkillLoader SDK 配置指南

本文档详细介绍 SkillLoader SDK 的各种配置选项。

## 目录

- [配置方式](#配置方式)
- [路径配置](#路径配置)
- [安全配置](#安全配置)
- [解析器配置](#解析器配置)
- [生成器配置](#生成器配置)
- [配置示例](#配置示例)

## 配置方式

SkillLoader 支持三种配置方式：

### 1. 代码配置（推荐简单场景）

```java
SkillLoader loader = SkillLoader.builder()
    .addFilesystemPath("local", "./skills")
    .addClasspathPath("builtin", "skills")
    .enableGenerator()
    .build();
```

### 2. 默认配置

```java
// 默认从 classpath:skills 扫描
SkillLoader loader = SkillLoader.createDefault();
```

默认配置等价于：

```java
SkillLoader.builder()
    .addPath(new PathEntry("default", "skills", 10, false, PathType.CLASSPATH))
    .build();
```

### 3. 配置文件（推荐复杂场景）

在**你的项目**（使用 SDK 的主程序）的 `src/main/resources` 目录下创建 `skillloader.yml`：

```yaml
skillloader:
  paths:
    - name: project
      path: ./skills
      priority: 10
      required: false
      type: filesystem
```

**项目结构：**
```
你的项目/
├── src/
│   └── main/
│       ├── java/
│       └── resources/
│           ├── skillloader.yml    # <-- 配置文件放在这里
│           └── skills/            # 内置 skills 目录
│               └── ...
└── pom.xml
```

加载配置：

```java
// 从 classpath 加载
URL configUrl = getClass().getClassLoader().getResource("skillloader.yml");
SkillLoader loader = SkillLoader.fromConfig(Path.of(configUrl.toURI()));

// 或者从文件系统加载
SkillLoader loader = SkillLoader.fromConfig(Path.of("config/skillloader.yml"));
```

## 路径配置

### PathEntry 参数

| 参数 | 类型 | 必需 | 默认值 | 说明 |
|------|------|------|--------|------|
| `name` | String | 是 | - | 路径标识名称 |
| `path` | String | 是 | - | 路径值 |
| `priority` | int | 否 | 10 | 优先级（数字越小优先级越高） |
| `required` | boolean | 否 | false | 是否必需（不存在时报错） |
| `type` | PathType | 否 | FILESYSTEM | 路径类型：FILESYSTEM 或 CLASSPATH |

### 路径类型

#### FILESYSTEM（文件系统）

从文件系统目录加载 skills：

```java
.addFilesystemPath("local", "./skills")
// 等价于
.addPath(new PathEntry("local", "./skills", 10, false, PathType.FILESYSTEM))
```

特点：
- 可以动态修改 skills
- 适合用户自定义 skills
- 支持符号链接（可配置）

#### CLASSPATH（类路径）

从 classpath 加载 skills（可打包在 JAR 中）：

```java
.addClasspathPath("builtin", "skills")
// 等价于
.addPath(new PathEntry("builtin", "skills", 20, false, PathType.CLASSPATH))
```

特点：
- 只读，运行时不能修改
- 适合内置默认 skills
- 打包在 JAR 中分发

### 多路径配置

```java
SkillLoader loader = SkillLoader.builder()
    // 高优先级：用户自定义 skills
    .addPath(new PathEntry("user", "~/.skills", 5, false, PathType.FILESYSTEM))
    // 中优先级：项目 skills
    .addPath(new PathEntry("project", "./skills", 10, false, PathType.FILESYSTEM))
    // 低优先级：内置 skills
    .addPath(new PathEntry("builtin", "skills", 20, false, PathType.CLASSPATH))
    .build();
```

**优先级规则**：
- 数字越小优先级越高
- 同名 skill 高优先级覆盖低优先级
- 路径按优先级排序存储

## 安全配置

### SecurityConfig 参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `strictMode` | boolean | true | 严格模式（路径必须在白名单内） |
| `allowSymlinks` | boolean | false | 是否允许符号链接 |
| `maxDepth` | int | 3 | 最大扫描深度 |

### 配置示例

```java
SecurityConfig security = new SecurityConfig(
    true,   // strictMode：严格检查路径
    false,  // allowSymlinks：不允许符号链接
    3       // maxDepth：最大扫描深度 3 层
);

SkillLoader loader = SkillLoader.builder()
    .security(security)
    .build();
```

### YAML 配置

```yaml
skillloader:
  security:
    strict-mode: true
    allow-symlinks: false
    max-depth: 3
```

## 解析器配置

### ParserConfig 参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `markerFile` | String | "SKILL.md" | Skill 标识文件名 |
| `encoding` | String | "UTF-8" | 文件编码 |

### 配置示例

```java
ParserConfig parser = new ParserConfig(
    "SKILL.md",  // markerFile
    "UTF-8"      // encoding
);

SkillLoader loader = SkillLoader.builder()
    .parser(parser)
    .build();
```

### YAML 配置

```yaml
skillloader:
  parser:
    marker-file: SKILL.md
    encoding: UTF-8
```

## 生成器配置

### GeneratorConfig 参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `template` | String | "default" | 模板名称 |
| `markerStart` | String | "<!-- SKILLS_TABLE_START -->" | 表格开始标记 |
| `markerEnd` | String | "<!-- SKILLS_TABLE_END -->" | 表格结束标记 |
| `enabled` | boolean | false | 是否启用生成器 |

### 配置示例

```java
GeneratorConfig generator = new GeneratorConfig(
    "default",                          // template
    "<!-- SKILLS_TABLE_START -->",   // markerStart
    "<!-- SKILLS_TABLE_END -->",     // markerEnd
    true                                // enabled
);

SkillLoader loader = SkillLoader.builder()
    .generator(generator)
    .build();
```

或使用快捷方式：

```java
SkillLoader loader = SkillLoader.builder()
    .enableGenerator()  // 使用默认配置启用
    .build();
```

### YAML 配置

```yaml
skillloader:
  generator:
    enabled: true
    template: default
    marker-start: "<!-- SKILLS_TABLE_START -->"
    marker-end: "<!-- SKILLS_TABLE_END -->"
```

## 配置示例

### 完整 YAML 配置

```yaml
skillloader:
  # 路径配置
  paths:
    # 用户自定义 skills（高优先级）
    - name: user
      path: ~/.skills
      priority: 5
      required: false
      type: filesystem
    
    # 项目 skills（中优先级）
    - name: project
      path: ./skills
      priority: 10
      required: false
      type: filesystem
    
    # 内置 skills（低优先级）
    - name: builtin
      path: skills
      priority: 20
      required: false
      type: classpath
  
  # 安全配置
  security:
    strict-mode: true
    allow-symlinks: false
    max-depth: 3
  
  # 解析器配置
  parser:
    marker-file: SKILL.md
    encoding: UTF-8
  
  # 生成器配置
  generator:
    enabled: true
    template: default
    marker-start: "<!-- SKILLS_TABLE_START -->"
    marker-end: "<!-- SKILLS_TABLE_END -->"
```

### 完整代码配置

```java
import com.skillloader.api.SkillLoader;
import com.skillloader.config.*;

public class SkillLoaderConfigDemo {
    public static void main(String[] args) {
        SkillLoader loader = SkillLoader.builder()
            // 路径配置
            .addPath(new PathEntry("user", "~/.skills", 5, false, PathType.FILESYSTEM))
            .addPath(new PathEntry("project", "./skills", 10, false, PathType.FILESYSTEM))
            .addPath(new PathEntry("builtin", "skills", 20, false, PathType.CLASSPATH))
            
            // 安全配置
            .security(new SecurityConfig(true, false, 3))
            
            // 解析器配置
            .parser(new ParserConfig("SKILL.md", "UTF-8"))
            
            // 生成器配置
            .generator(new GeneratorConfig(
                "default",
                "<!-- SKILLS_TABLE_START -->",
                "<!-- SKILLS_TABLE_END -->",
                true
            ))
            
            .build();
    }
}
```

### Spring Boot 配置

`application.yml`：

```yaml
skillloader:
  paths:
    - name: builtin
      path: skills
      priority: 20
      type: classpath
    - name: local
      path: ${user.home}/.skills
      priority: 10
      type: filesystem
  generator:
    enabled: true
```

配置类：

```java
@Configuration
public class SkillLoaderConfig {
    
    @Bean
    public SkillLoader skillLoader() throws Exception {
        // 从 classpath 加载配置
        return SkillLoader.fromConfig(
            new ClassPathResource("skillloader.yml").getFile().toPath()
        );
    }
}
```

## 环境变量支持

配置文件支持使用环境变量：

```yaml
skillloader:
  paths:
    - name: user
      path: ${SKILLS_PATH:~/.skills}  # 使用环境变量，默认为 ~/.skills
      priority: 10
      type: filesystem
```

在代码中使用：

```java
// 先替换环境变量
String configContent = Files.readString(Path.of("skillloader.yml"));
configContent = configContent.replace("${SKILLS_PATH}", 
    System.getenv().getOrDefault("SKILLS_PATH", "~/.skills"));

// 然后加载配置
SkillLoaderConfig config = parseConfig(configContent);
SkillLoader loader = new SkillLoader(config);
```
