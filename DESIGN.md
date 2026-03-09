# SkillLoader Java SDK - 设计方案

## 核心定位

**只做 Loader，不做 Installer**

```
读取配置 → 扫描目录 → 解析 SKILL.md → 生成 AGENTS.md
    ↑_________↓
     只能读白名单目录
```

## 安全原则

1. **白名单机制**: 只能读取配置中明确指定的目录
2. **路径校验**: 所有读取操作前验证路径在白名单内
3. **禁止遍历**: 不允许 `../` 等路径遍历
4. **只读**: 不提供任何写入/修改/安装接口

## 技术选型

- **语言**: Java 21 (LTS)
- **构建**: Maven 3.9+
- **依赖**: 零依赖（仅用 JDK），SnakeYAML 可选（provided）
- **测试**: JUnit 5 + AssertJ

## 多路径配置

### 优先级队列（从高到低）

```yaml
skillloader:
  paths:
    # 优先级 1: 项目本地 skills（最高）
    - name: project-local
      path: ./skills
      priority: 10
      
    # 优先级 2: 项目 resources
    - name: project-resources
      path: classpath:/skills
      priority: 20
      
    # 优先级 3: 用户全局 skills
    - name: user-global
      path: ${user.home}/.skillloader/skills
      priority: 30
```

### 同名处理

同名 skill，优先级高的覆盖低的（项目本地覆盖全局）。

## 配置文件

### 支持位置（按优先级）

1. `skillloader.yml` - 项目根目录
2. `skillloader.properties` - 项目根目录
3. `application.yml` - 项目根目录（Spring Boot 风格）
4. `META-INF/skillloader.yml` - classpath 内
5. 系统属性: `-Dskillloader.config=/path/to/config.yml`

### 完整配置示例

```yaml
skillloader:
  paths:
    - name: project-skills
      path: ./skills
      priority: 10
      required: false
      
    - name: resources-skills
      path: classpath:/skills
      priority: 20
      required: false
  
  parser:
    marker-file: SKILL.md
    encoding: UTF-8
    max-file-size: 1MB
    
  security:
    strict-mode: true
    allow-symlinks: false
    max-depth: 3
    
  generator:
    template: default
    marker-start: "<!-- SKILLS_TABLE_START -->"
    marker-end: "<!-- SKILLS_TABLE_END -->"
```

## 架构设计

```
┌──────────────────────────────────────────────────────────────┐
│                      SkillLoader (Facade)                     │
├──────────────────────────────────────────────────────────────┤
│  Config → Scanner → SecureReader → Parser → Registry → Gen   │
└──────────────────────────────────────────────────────────────┘
```

## 核心 API

### SkillLoader

```java
public final class SkillLoader {
    // 工厂方法
    public static SkillLoader createDefault();
    public static SkillLoader fromConfig(Path configPath);
    
    // 核心方法
    public List<Skill> discover();
    public SkillContent load(String skillName);
    public String generateAgentsMd();
    public List<PathEntry> getAllowedPaths();
}
```

### 配置类

```java
public record SkillLoaderConfig(
    List<PathEntry> paths,      // 白名单路径（必须）
    ParserConfig parser,
    SecurityConfig security,
    GeneratorConfig generator
) {}

public record PathEntry(
    String name,
    String path,           // 支持 ${user.home} 变量
    int priority,          // 数字越小越优先
    boolean required,
    PathType type          // FILESYSTEM 或 CLASSPATH
) {}
```

## 使用示例

### 最简单用法

```java
SkillLoader loader = SkillLoader.createDefault();
List<Skill> skills = loader.discover();
String agentsMd = loader.generateAgentsMd();
Files.writeString(Path.of("AGENTS.md"), agentsMd);
```

### Spring Boot 集成

```java
@Configuration
public class SkillLoaderConfiguration {
    
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

## 模块结构

```
skill-loader-core/
├── api/              # 公共 API + 异常
├── config/           # 配置系统
├── model/            # 数据模型
├── scanner/          # 目录扫描
├── parser/           # SKILL.md 解析
├── reader/           # 安全文件读取
├── registry/         # Skill 注册表
└── generator/        # AGENTS.md 生成
```

## 异常体系

```
SkillLoaderException (checked)
├── ConfigException
├── SecurityException
├── SkillNotFoundException
└── SkillParseException
```

## 开发计划

| Phase | 内容 | 时间 |
|-------|------|------|
| 1 | 项目骨架 + 异常体系 + 核心模型 | ✅ 完成 |
| 2 | 配置系统 | 40min |
| 3 | 安全读取器 | 40min |
| 4 | 目录扫描器 | 30min |
| 5 | YAML 解析器 | 30min |
| 6 | 注册表 | 30min |
| 7 | AGENTS.md 生成器 | 30min |
| 8 | SkillLoader 门面 | 20min |
| 9 | 安全测试 | 40min |
| 10 | 集成测试 + 示例 | 30min |

**总计: ~5小时**

## 相关文档

- [GitHub 仓库](https://github.com/xxsddm/skillLoader)
- [开发进度](PROGRESS.md)
