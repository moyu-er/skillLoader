# SkillLoader

Java 21 轻量级 Skill Loader SDK，用于从本地目录加载 AI Agent Skills。

## 核心特性

- 🔒 **只读设计** - 仅加载，不安装/不修改
- 🛡️ **白名单机制** - 只能读取配置指定的目录
- 📂 **多路径优先级** - 同名 skill 高优先级覆盖低优先级
- ⚡ **零依赖** - 仅用 Java 21 标准库
- 🧪 **完整测试** - 100+ 单元测试，覆盖率 ≥80%

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
System.out.println(content.markdownContent());

// 生成 AGENTS.md
loader.syncToFile(Path.of("AGENTS.md"));
```

### 自定义配置

```java
SkillLoader loader = SkillLoader.builder()
    .addFilesystemPath("project", "./my-skills")
    .addClasspathPath("builtin", "/skills")
    .build();
```

### 配置文件 (skillloader.yml)

```yaml
skillloader:
  paths:
    - name: project-skills
      path: ./skills
      priority: 10
    - name: resources-skills
      path: classpath:/skills
      priority: 20
  
  security:
    strict-mode: true
    allow-symlinks: false
    max-depth: 3
```

## 项目结构

```
skill-loader-core/
├── api/              # 公共 API
├── config/           # 配置系统
├── model/            # 数据模型
├── scanner/          # 目录扫描
├── parser/           # YAML 解析
├── reader/           # 安全文件读取
├── registry/         # Skill 注册表
└── generator/        # AGENTS.md 生成
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
| 7 | AGENTS.md 生成器 | ✅ |
| 8 | SkillLoader 门面 | ✅ |
| 9 | 集成测试 | 🔄 |
| 10 | 文档完善 | 🔄 |

## CI/CD

支持 GitHub Actions 和 Jenkins：

```bash
# GitHub Actions (自动触发)
.github/workflows/ci.yml

# Jenkins
Jenkinsfile
```

## 测试

```bash
# 运行所有测试
mvn test

# 生成覆盖率报告
mvn jacoco:report
```

## 示例 Skills

项目包含 11 个示例 skills：

- `python-quality` - Python 代码质量检查
- `eslint-config-generator` - ESLint 配置生成
- `typescript-config-generator` - TypeScript 配置生成
- `git-workflow-enhanced` - Git 工作流规范
- `project-constraints` - 项目约束规范
- `handover-doc-guide` - 交接文档指南
- `playwright-install` - Playwright 安装指南
- `screenshot-compress` - 截图压缩工具
- `skill-loader-usage` - SDK 使用指南
- `skillloader-project-guide` - 项目开发规范
- `jenkins-setup-guide` - Jenkins CI/CD 配置

## 许可证

Apache License 2.0
