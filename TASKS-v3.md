# SkillLoader 开发任务清单 - 只读版

## 技术选型

- **语言**: Java 21 (LTS)
- **构建**: Maven 3.9+
- **依赖**: 零依赖（仅用 JDK）
- **可选**: SnakeYAML 检测（反射加载，不强制依赖）

## 核心原则

1. **只读**: 不提供任何写入/安装接口
2. **白名单**: 只能读取配置中指定的目录
3. **多路径**: 支持多个路径，优先级从高到低
4. **安全**: 路径遍历防护、符号链接控制

---

## Phase 1: 项目骨架 + 异常体系 (30min)

### 1.1 Maven 项目
- [ ] 创建 pom.xml (Java 21, 零依赖)
- [ ] 目录结构
- [ ] 包结构 (com.skillloader.api/model/config/scanner/parser/reader/registry/generator)

### 1.2 异常体系
```
SkillLoaderException (checked)
├── ConfigException
├── SecurityException
├── SkillNotFoundException
└── SkillParseException
```
- [ ] SkillLoaderException.java
- [ ] ConfigException.java
- [ ] SecurityException.java
- [ ] SkillNotFoundException.java
- [ ] SkillParseException.java

---

## Phase 2: 配置系统 (40min)

### 2.1 配置模型
- [ ] PathEntry.java (name, path, priority, required, type)
- [ ] ParserConfig.java (markerFile, encoding, maxFileSize)
- [ ] SecurityConfig.java (strictMode, allowSymlinks, maxDepth)
- [ ] GeneratorConfig.java (template, markerStart, markerEnd)
- [ ] SkillLoaderConfig.java (整合所有配置，带验证)

### 2.2 配置加载器
- [ ] ConfigLoader.java (接口)
- [ ] YamlConfigLoader.java (YAML 格式)
- [ ] PropertiesConfigLoader.java (Properties 格式)
- [ ] CompositeConfigLoader.java (多来源合并)

### 2.3 变量替换
- [ ] 支持 ${user.home}, ${user.dir} 等系统变量
- [ ] 支持环境变量

### 2.4 验证逻辑
- [ ] 至少配置一个路径
- [ ] 路径按优先级排序
- [ ] 路径格式校验

---

## Phase 3: 安全读取器 (40min)

### 3.1 核心接口
- [ ] SecureFileReader.java (接口)
  - isAllowed(Path): boolean
  - read(Path): String
  - listDirectory(Path): List<Path>
  - exists(Path): boolean

### 3.2 白名单实现
- [ ] PathWhitelist.java (管理允许的路径)
- [ ] 路径规范化 (normalize)
- [ ] 路径包含检查 (startsWith)
- [ ] 路径遍历防护 (../ 检测)

### 3.3 具体实现
- [ ] FileSystemReader.java (文件系统)
- [ ] ClasspathReader.java (classpath 资源)

### 3.4 安全特性
- [ ] 符号链接检测 (当 allowSymlinks=false)
- [ ] 文件大小限制检查
- [ ] 编码验证

---

## Phase 4: 目录扫描器 (30min)

### 4.1 扫描器
- [ ] SkillScanner.java
  - scan(PathEntry): List<Skill>
  - 递归扫描 (限制深度)
  - SKILL.md 存在检测

### 4.2 扫描策略
- [ ] 深度限制 (maxDepth)
- [ ] 符号链接处理
- [ ] 错误处理 (单个目录失败不中断)

### 4.3 扫描结果
- [ ] ScanResult.java
- [ ] 收集路径、来源、优先级

---

## Phase 5: YAML 解析器 (30min)

### 5.1 解析器接口
- [ ] SkillParser.java
  - parse(Path): SkillContent
  - parseMetadata(Path): SkillMetadata

### 5.2 内置轻量 YAML 解析
- [ ] YamlFrontmatterParser.java
  - 正则提取 --- ... ---
  - 简单 key: value 解析
  - 数组支持 (key: [a, b, c])

### 5.3 SnakeYAML 适配（可选）
- [ ] SnakeYamlDetector.java (检测是否存在)
- [ ] SnakeYamlAdapter.java (使用反射适配)
- [ ] 优先使用 SnakeYAML（如果存在）

### 5.4 Markdown 提取
- [ ] 提取 frontmatter 后的内容
- [ ] 保留原始格式

---

## Phase 6: 注册表 (30min)

### 6.1 注册表接口
- [ ] SkillRegistry.java
  - discover(): List<Skill>
  - find(String): Optional<Skill>
  - findBySource(SkillSource): List<Skill>

### 6.2 实现
- [ ] DefaultSkillRegistry.java
  - Map<String, Skill> 索引
  - 优先级去重（同名取高优先级）
  - 线程安全 (ConcurrentHashMap 或不可变集合)

### 6.3 数据模型
- [ ] Skill.java (name, description, source, location, priority)
- [ ] SkillMetadata.java (name, description, context, tags, extra)
- [ ] SkillContent.java (metadata, markdown, baseUri, resources)
- [ ] ResourceRef.java (name, uri, type)
- [ ] PathType.java (FILESYSTEM, CLASSPATH)

---

## Phase 7: AGENTS.md 生成器 (30min)

### 7.1 生成器接口
- [ ] AgentsMdGenerator.java
  - generate(List<Skill>): String
  - updateExisting(String, List<Skill>): String

### 7.2 默认实现
- [ ] DefaultAgentsMdGenerator.java
  - XML 格式生成
  - 查找标记 (markerStart/end)
  - 替换或插入

### 7.3 模板
```xml
<skills_system priority="1">
## Available Skills
<!-- SKILLS_TABLE_START -->
<usage>...</usage>
<available_skills>
  ${skills}
</available_skills>
<!-- SKILLS_TABLE_END -->
</skills_system>
```

---

## Phase 8: SkillLoader 门面 (20min)

### 8.1 建造者
- [ ] SkillLoaderBuilder.java
  - addPath(PathEntry)
  - fromConfig(Path)
  - build(): SkillLoader

### 8.2 门面类
- [ ] SkillLoader.java (final)
  - createDefault(): SkillLoader
  - fromConfig(Path): SkillLoader
  - fromConfig(SkillLoaderConfig): SkillLoader
  - discover(): List<Skill>
  - load(String): SkillContent
  - getMetadata(String): Optional<SkillMetadata>
  - generateAgentsMd(): String
  - getAllowedPaths(): List<PathEntry>
  - getConfig(): SkillLoaderConfig

---

## Phase 9: 安全测试 (40min)

### 9.1 白名单测试
- [ ] 允许白名单内路径
- [ ] 拒绝白名单外路径
- [ ] 路径遍历攻击防护 (../)
- [ ] 符号链接检测

### 9.2 配置测试
- [ ] 空路径配置报错
- [ ] 变量替换测试
- [ ] 优先级排序测试

### 9.3 扫描测试
- [ ] 深度限制测试
- [ ] 优先级覆盖测试

---

## Phase 10: 集成测试 + 示例 (30min)

### 10.1 测试资源
```
src/test/resources/
├── skillloader.yml                    # 测试配置
└── skills/
    ├── pdf/
    │   ├── SKILL.md
    │   └── scripts/
    │       └── extract.py
    └── weather/
        └── SKILL.md
```

### 10.2 示例 SKILL.md
```markdown
---
name: pdf
description: PDF manipulation toolkit
context: document
tags: [pdf, extraction]
---

# PDF Skill

When user asks about PDFs...
```

### 10.3 集成测试
- [ ] 完整流程测试
- [ ] 多路径优先级测试
- [ ] AGENTS.md 生成测试

### 10.4 示例代码
- [ ] BasicExample.java
- [ ] SpringBootExample.java

### 10.5 README.md
- [ ] 快速开始
- [ ] 配置说明
- [ ] 安全说明

---

## 时间预估

| Phase | 内容 | 时间 |
|-------|------|------|
| 1 | 项目骨架 + 异常 | 30min |
| 2 | 配置系统 | 40min |
| 3 | 安全读取器 | 40min |
| 4 | 目录扫描器 | 30min |
| 5 | YAML 解析器 | 30min |
| 6 | 注册表 | 30min |
| 7 | 生成器 | 30min |
| 8 | 门面 | 20min |
| 9 | 安全测试 | 40min |
| 10 | 集成 + 示例 | 30min |
| **总计** | | **~5h** |

---

## 检查清单

### 安全
- [ ] 白名单机制
- [ ] 路径遍历防护
- [ ] 符号链接控制
- [ ] 文件大小限制

### 功能
- [ ] 多路径配置
- [ ] 优先级去重
- [ ] YAML 解析
- [ ] AGENTS.md 生成

### 质量
- [ ] 零依赖
- [ ] Java 21 特性
- [ ] 完整 Javadoc
- [ ] 测试覆盖 80%+

### 交付
- [ ] 可编译项目
- [ ] 完整测试
- [ ] 使用示例
- [ ] 设计文档
