# SkillLoader SDK 集成指南

本文档详细介绍如何在项目中集成和使用 SkillLoader SDK。

## 目录

- [Maven 集成](#maven-集成)
- [基本使用](#基本使用)
- [配置方式](#配置方式)
- [路径类型说明](#路径类型说明)
- [Spring Boot 集成](#spring-boot-集成)

## Maven 集成

### 1. 添加依赖

在项目的 `pom.xml` 中添加 SkillLoader 依赖：

```xml
<dependency>
    <groupId>com.skillloader</groupId>
    <artifactId>skill-loader-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 安装到本地仓库（开发时）

如果使用的是本地开发的版本，需要先安装到本地 Maven 仓库：

```bash
cd skillLoader
mvn clean install
```

## 基本使用

### 方式一：默认配置（推荐）

默认从 classpath 下的 `skills` 目录扫描：

```java
import com.skillloader.api.SkillLoader;
import com.skillloader.model.Skill;
import com.skillloader.model.SkillContent;

public class SkillDemo {
    public static void main(String[] args) {
        // 创建 loader（默认从 classpath:skills 扫描）
        SkillLoader loader = SkillLoader.createDefault();
        
        // 发现所有 skills
        List<Skill> skills = loader.discover();
        System.out.println("发现 " + skills.size() + " 个 skills");
        
        // 加载特定 skill
        SkillContent content = loader.load("pdf");
        System.out.println("名称: " + content.metadata().name());
        System.out.println("描述: " + content.metadata().description());
    }
}
```

### 方式二：自定义配置

```java
SkillLoader loader = SkillLoader.builder()
    .addFilesystemPath("local", "./my-skills", 10, false)
    .addClasspathPath("builtin", "skills", 20, false)
    .build();
```

### 方式三：配置文件（推荐）

在**你的项目**（使用 SDK 的主程序）的 `src/main/resources` 目录下创建 `skillloader.yml`：

```yaml
skillloader:
  paths:
    - name: project-skills
      path: ./skills
      priority: 10
      required: false
      type: filesystem
    - name: builtin-skills
      path: skills
      priority: 20
      required: false
      type: classpath
```

**项目结构：**
```
你的项目/
├── src/
│   └── main/
│       ├── java/
│       └── resources/
│           ├── skillloader.yml    # 配置文件放在这里
│           └── skills/            # 内置 skills 目录
│               ├── pdf/
│               │   └── SKILL.md
│               └── git-workflow/
│                   └── SKILL.md
└── pom.xml
```

加载配置：

```java
// 从 classpath 加载配置文件
SkillLoader loader = SkillLoader.fromConfig(
    Path.of(SkillLoaderConfig.class.getClassLoader()
        .getResource("skillloader.yml").toURI())
);

// 或者从文件系统加载
SkillLoader loader = SkillLoader.fromConfig(Path.of("config/skillloader.yml"));
```

## 配置方式

### 路径配置参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `name` | 路径标识名称 | 必填 |
| `path` | 路径值 | 必填 |
| `priority` | 优先级（数字越小优先级越高） | 10 |
| `required` | 是否必需（不存在时报错） | false |
| `type` | 路径类型：filesystem 或 classpath | filesystem |

### 多路径优先级

当多个路径包含同名 skill 时，**优先级高的覆盖优先级低的**：

```java
SkillLoader loader = SkillLoader.builder()
    .addPath(new PathEntry("high", "./skills-v2", 5, false, PathType.FILESYSTEM))
    .addPath(new PathEntry("low", "./skills-v1", 10, false, PathType.FILESYSTEM))
    .build();

// 同名 skill 会优先从 ./skills-v2 加载
```

## 路径类型说明

### Filesystem 路径

从文件系统目录加载 skills：

```java
.addFilesystemPath("local", "./skills")
```

适用场景：
- 开发时加载本地 skills
- 用户自定义 skills
- 动态更新的 skills

### Classpath 路径

从 classpath 加载 skills（可以打包在 JAR 中）：

```java
.addClasspathPath("builtin", "skills")
```

适用场景：
- 内置 skills 打包在 SDK 中
- 发布时携带默认 skills
- 只读 skills

**项目结构示例：**

```
你的项目/
├── src/
│   └── main/
│       ├── java/
│       └── resources/
│           └── skills/           # 内置 skills 目录
│               ├── pdf/
│               │   └── SKILL.md
│               └── git-workflow/
│                   └── SKILL.md
└── pom.xml
```

## Spring Boot 集成

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.skillloader</groupId>
    <artifactId>skill-loader-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 创建配置类

```java
import com.skillloader.api.SkillLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SkillLoaderConfig {
    
    @Bean
    public SkillLoader skillLoader() {
        // 方式一：默认配置（从 classpath:skills 扫描）
        return SkillLoader.createDefault();
        
        // 方式二：自定义配置
        // return SkillLoader.builder()
        //     .addClasspathPath("builtin", "skills")
        //     .addFilesystemPath("local", "./skills")
        //     .build();
    }
}
```

### 3. 在 Controller 中使用

```java
import com.skillloader.api.SkillLoader;
import com.skillloader.model.Skill;
import com.skillloader.model.SkillContent;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
public class SkillController {
    
    private final SkillLoader skillLoader;
    
    public SkillController(SkillLoader skillLoader) {
        this.skillLoader = skillLoader;
    }
    
    @GetMapping
    public List<Skill> listSkills() {
        return skillLoader.discover();
    }
    
    @GetMapping("/{name}")
    public SkillContent getSkill(@PathVariable String name) {
        return skillLoader.load(name);
    }
}
```

### 4. 配置文件方式（推荐）

在你的 Spring Boot 项目的 `src/main/resources` 目录下创建 `skillloader.yml`：

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
```

**Spring Boot 项目结构：**
```
你的 Spring Boot 项目/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/example/
│       │       └── config/
│       │           └── SkillLoaderConfig.java
│       └── resources/
│           ├── application.yml
│           ├── skillloader.yml          # SkillLoader 配置文件
│           └── skills/                  # 内置 skills
│               ├── pdf/
│               │   └── SKILL.md
│               └── git-workflow/
│                   └── SKILL.md
└── pom.xml
```

配置类：

```java
import com.skillloader.api.SkillLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class SkillLoaderConfig {
    
    @Value("classpath:skillloader.yml")
    private Resource configResource;
    
    @Bean
    public SkillLoader skillLoader() throws Exception {
        return SkillLoader.fromConfig(configResource.getFile().toPath());
    }
}
```

## 常见问题

### Q: 启动时报错 "SKILL.md not found"

**原因**：扫描到了 skill 目录，但无法读取其中的 SKILL.md 文件。

**解决**：
1. 确保 skill 目录中包含 `SKILL.md` 文件
2. 检查路径配置是否正确
3. 对于 classpath 路径，确保 resources 目录已正确打包

### Q: 如何调试扫描不到 skills 的问题？

```java
SkillLoader loader = SkillLoader.createDefault();

// 查看配置的路径
System.out.println("配置的路径: " + loader.getAllowedPaths());

// 查看发现的 skills
List<Skill> skills = loader.discover();
System.out.println("发现的 skills: " + skills);
```

### Q: 同名 skill 如何确定优先级？

优先级数字**越小越优先**。例如：
- priority=5 的 skill 会覆盖 priority=10 的同名 skill

### Q: 支持哪些文件编码？

默认使用 UTF-8 编码。可以在配置中修改：

```yaml
parser:
  encoding: UTF-8
```
