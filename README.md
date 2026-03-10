# SkillLoader

Java 21 轻量级 Skill Loader SDK，用于从本地目录加载 AI Agent Skills。

**新仓库地址**: https://github.com/moyu-er/skillLoader

## 📚 文档

| 文档 | 说明 |
|------|------|
| [INTEGRATION.md](docs/INTEGRATION.md) | SDK 集成指南（Maven、Spring Boot） |
| [USAGE.md](docs/USAGE.md) | 使用指南（API、配置、最佳实践） |
| [CONFIGURATION.md](docs/CONFIGURATION.md) | 配置详解（路径、安全、解析器、生成器） |

## 核心特性

- 🔒 **只读设计** - 仅加载，不安装/不修改 skills
- 🛡️ **白名单机制** - 只能读取配置指定的目录
- 📂 **多路径优先级** - 同名 skill 高优先级覆盖低优先级
- ⚡ **零依赖** - 仅用 Java 21 标准库（SnakeYAML 可选）
- 🧪 **完整测试** - 100+ 单元测试，覆盖率 ≥80%
- 📝 **AGENTS.md 生成（可选）** - 默认关闭，需显式启用

## 快速开始

### Maven 依赖

```xml
<dependency>
    <groupId>com.skillloader</groupId>
    <artifactId>skill-loader-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 最简单用法

```java
// 创建 loader（默认读取 ./skills 目录）
SkillLoader loader = SkillLoader.createDefault();

// 发现所有 skills
List<Skill> skills = loader.discover();
System.out.println("Found " + skills.size() + " skills");

// 加载特定 skill
SkillContent content = loader.load("pdf");
System.out.println(content.metadata().name());
System.out.println(content.metadata().description());
System.out.println(content.markdownContent());
```

### 自定义配置（启用 AGENTS.md 生成）

```java
import com.skillloader.config.GeneratorConfig;

SkillLoader loader = SkillLoader.builder()
    .addFilesystemPath("project", "./my-skills", 10, false)
    .addClasspathPath("builtin", "/skills", 20, false)
    .enableGenerator()  // 启用 AGENTS.md 生成功能
    .build();
```

### 自定义配置（完整配置）

```java
SkillLoader loader = SkillLoader.builder()
    .addFilesystemPath("project", "./my-skills", 10, false)
    .addClasspathPath("builtin", "/skills", 20, false)
    .generator(new GeneratorConfig(
        "default",                              // template
        "<!-- SKILLS_TABLE_START -->",       // markerStart
        "<!-- SKILLS_TABLE_END -->",         // markerEnd
        true                                    // enabled
    ))
    .build();
```

### 配置文件 (skillloader.yml)

```yaml
skillloader:
  paths:
    - name: project-skills
      path: ./skills
      priority: 10
      required: false
      type: filesystem
    - name: global-skills
      path: ~/.skills
      priority: 20
      required: false
      type: filesystem
  
  security:
    strict-mode: true
    allow-symlinks: false
    max-depth: 3
  
  parser:
    marker-file: SKILL.md
    encoding: UTF-8
```

从配置文件加载：

```java
SkillLoader loader = SkillLoader.fromConfig(Path.of("skillloader.yml"));
```

## AGENTS.md 生成（可选功能）

**默认关闭**。如需启用，需在配置中设置 `generator.enabled: true`：

```yaml
skillloader:
  generator:
    enabled: true  # 默认 false，显式启用才允许写操作
    marker-start: "<!-- SKILLS_TABLE_START -->"
    marker-end: "<!-- SKILLS_TABLE_END -->"
```

启用后可以使用写操作：

```java
// 生成 AGENTS.md 内容（只读操作，随时可用）
String agentsMd = loader.generateAgentsMd();

// 写入文件（需要 generator.enabled = true）
loader.syncToFile(Path.of("AGENTS.md"));

// 更新现有文件（需要 generator.enabled = true）
loader.updateFile(Path.of("AGENTS.md"));
```

**安全提示**：`syncToFile` 和 `updateFile` 是唯一的写操作，默认禁用以防止意外文件修改。

## 项目结构

```
skill-loader-core/
├── api/              # 公共 API (SkillLoader, exceptions)
├── config/           # 配置系统 (SkillLoaderConfig, PathEntry, etc.)
├── model/            # 数据模型 (Skill, SkillContent, SkillMetadata, ResourceRef)
├── scanner/          # 目录扫描 (SkillScanner)
├── parser/           # YAML 解析 (SimpleYamlParser)
├── reader/           # 安全文件读取 (FileSystemReader, ClasspathReader)
├── registry/         # Skill 注册表 (DefaultSkillRegistry)
└── generator/        # AGENTS.md 生成 (可选功能)
```

## 开发进度

| Phase | 内容 | 状态 |
|-------|------|------|
| 1 | 项目骨架 + 异常体系 + 核心模型 | ✅ |
| 2 | 配置系统 | ✅ |
| 3 | 安全读取器 | ✅ |
| 4 | 目录扫描器 | ✅ |
| 5 | YAML 解析器 | ✅ |
| 6 | 注册表 | ✅ |
| 7 | AGENTS.md 生成器 | ✅ (默认关闭) |
| 8 | SkillLoader 门面 | ✅ |
| 9 | 配置文件加载 | ✅ |
| 10 | 集成测试 | ✅ |
| 11 | 文档完善 | 🔄 |

## CI/CD

支持 GitHub Actions：

```bash
# GitHub Actions (自动触发 PR/push)
.github/workflows/ci.yml
```

状态: [![CI](https://github.com/moyu-er/skillLoader/actions/workflows/ci.yml/badge.svg)](https://github.com/moyu-er/skillLoader/actions)

## 测试

```bash
# 运行所有测试
mvn test

# 生成覆盖率报告
mvn jacoco:report

# 代码风格检查
mvn checkstyle:check
```

## 示例 Skills

项目包含多个示例 skills（位于 `src/test/resources/skills/`）：

**简单示例**：
- `chart/` - 基础图表 skill
- `pdf/` - PDF 处理 skill
- `weather/` - 天气查询 skill

**复杂示例**（含 resources/）：
- `python-quality/` - Python 代码质量（含 scripts/, references/）
- `git-workflow/` - Git 工作流规范（含 scripts/, references/, assets/）
- `data-visualization/` - 数据可视化（含多个 resources/）

## Git 工作流

本项目使用 Git Flow 分支模型：

```
master (生产分支)
  ↑
develop (开发分支)
  ↑
feature/* (功能分支)
```

- **master**: 稳定版本，只能通过 PR 合并
- **develop**: 开发集成，功能完成后合并
- **feature/***: 新功能开发分支

详细规范见 `skills/skillloader-git-workflow/SKILL.md`

## 修复记录

| Issue | 问题 | 修复 |
|-------|------|------|
| #2 | `&amp;` HTML 实体转义错误 | 批量替换为 `&` |
| #3 | 检查异常导致编译失败 | 改为 RuntimeException |
| #5 | 正则表达式不支持多行 | 添加 `(?s)` DOTALL 标志 |
| #8 | 测试期望 HTML 转义 | 改为原始 XML 标签 |
| #10 | SkillScanner 未解析 description | 从 SKILL.md 解析 frontmatter |
| #15 | escapeXml 实现错误 | 修复 `&` → `&amp;` 转义顺序 |
| #16 | fromConfig 未实现 | 实现 YAML 配置文件加载 |

## 许可证

Apache License 2.0

---

**注意**: 原仓库 `xxsddm/skillLoader` 已迁移至 `moyu-er/skillLoader`。
