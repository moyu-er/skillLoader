# SkillLoader 开发任务清单

## 技术选型确认

- **语言**: Java 21 (LTS)
- **构建**: Maven 3.9+
- **依赖**: 
  - 核心: 零依赖 (仅 JDK)
  - 可选: SnakeYAML 2.2 (provided scope)
- **测试**: JUnit 5 + AssertJ

## Phase 1: 项目骨架 (30分钟)

### 1.1 创建 Maven 项目
```bash
# 目录结构
skill-loader/
├── pom.xml
├── src/
│   ├── main/java/com/skillloader/
│   │   ├── api/
│   │   ├── model/
│   │   ├── config/
│   │   ├── spi/
│   │   ├── internal/
│   │   └── util/
│   ├── main/resources/
│   └── test/java/com/skillloader/
└── README.md
```

### 1.2 pom.xml 配置
- [ ] Java 21 编译目标
- [ ] SnakeYAML optional 依赖
- [ ] JUnit 5 测试依赖
- [ ] Maven 编译器插件
- [ ] 源码编码 UTF-8

### 1.3 package-info.java
- [ ] com.skillloader.api - 公共 API
- [ ] com.skillloader.model - 数据模型
- [ ] com.skillloader.spi - 扩展接口
- [ ] com.skillloader.internal - 内部实现 (不导出)

---

## Phase 2: 核心模型 (30分钟)

### 2.1 Skill.java
```java
public record Skill(
    String name,
    String description,
    SkillSource source,
    URI location,
    int priority
) implements Comparable<Skill> {}
```

### 2.2 SkillMetadata.java
```java
public record SkillMetadata(
    String name,
    String description,
    Optional<String> context,
    Map<String, List<String>> tags,
    Map<String, Object> extra
) {}
```

### 2.3 SkillContent.java
```java
public record SkillContent(
    SkillMetadata metadata,
    String markdownContent,
    URI baseUri,
    List<ResourceRef> resources
) {}
```

### 2.4 ResourceRef.java
```java
public record ResourceRef(
    String name,
    URI uri,
    ResourceType type  // SCRIPT, REFERENCE, ASSET
) {}
```

### 2.5 SkillSource.java (Enum)
- PROJECT
- GLOBAL
- CLASSPATH
- CUSTOM

### 2.6 异常类
- [ ] SkillLoaderException (checked)
- [ ] SkillNotFoundException
- [ ] SkillParseException

---

## Phase 3: Storage SPI (40分钟)

### 3.1 SkillStorage.java (接口)
```java
public interface SkillStorage {
    boolean exists(URI uri);
    List<StorageEntry> list(URI uri) throws IOException;
    String read(URI uri) throws IOException;
    InputStream openStream(URI uri) throws IOException;
    URI resolve(URI base, String relative);
}
```

### 3.2 StorageEntry.java (record)

### 3.3 FileSystemStorage.java (实现)
- [ ] 基于 java.nio.file
- [ ] 支持符号链接选项

### 3.4 ClasspathStorage.java (实现)
- [ ] 基于 ClassLoader#getResource
- [ ] 支持 jar 内资源

### 3.5 CompositeStorage.java (组合多个 storage)

---

## Phase 4: Parser SPI (40分钟)

### 4.1 SkillParser.java (接口)
```java
public interface SkillParser {
    boolean canParse(URI uri);
    SkillContent parse(URI uri, SkillStorage storage) throws SkillParseException;
    SkillMetadata parseMetadata(URI uri, SkillStorage storage);
}
```

### 4.2 SimpleYamlParser.java (内置实现)
- [ ] 轻量级 YAML 解析 (只支持简单 key: value)
- [ ] 不依赖外部库
- [ ] 正则提取 frontmatter

### 4.3 SnakeYamlAdapter.java (适配器)
- [ ] 检测 SnakeYAML 是否存在
- [ ] 使用反射或 ServiceLoader 加载
- [ ] fallback 到 SimpleYamlParser

### 4.4 ParserChain.java (责任链模式)
- 尝试多个 parser，返回第一个成功的

---

## Phase 5: PathProvider SPI (30分钟)

### 5.1 SkillPathProvider.java (接口)
```java
public interface SkillPathProvider {
    List<PathCandidate> providePaths();
}
```

### 5.2 PathCandidate.java (record)
```java
public record PathCandidate(
    URI uri,
    SkillSource source,
    int priority,
    String name
) {}
```

### 5.3 DefaultPathProvider.java (实现)
- [ ] 从配置读取路径
- [ ] 支持 file:// 和 classpath://

### 5.4 CompositePathProvider.java (组合)

---

## Phase 6: Registry (30分钟)

### 6.1 SkillRegistry.java (接口)
```java
public interface SkillRegistry {
    List<Skill> discover();
    Optional<Skill> find(String name);
    List<Skill> findBySource(SkillSource source);
    List<Skill> findByTag(String tag);
}
```

### 6.2 DefaultSkillRegistry.java (实现)
- [ ] 扫描所有路径
- [ ] 按优先级去重
- [ ] 内存索引 (Map<name, Skill>)
- [ ] 线程安全 (ConcurrentHashMap)

---

## Phase 7: Generator (30分钟)

### 7.1 AgentsMdGenerator.java (接口)
```java
public interface AgentsMdGenerator {
    String generate(List<Skill> skills);
    String generateSkillsSystem(List<Skill> skills);
    String updateExisting(String existingContent, List<Skill> skills);
}
```

### 7.2 DefaultAgentsMdGenerator.java (实现)
- [ ] XML 格式生成
- [ ] 支持更新现有文件
- [ ] 查找 <!-- SKILLS_TABLE_START --> 标记

### 7.3 内置模板
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

## Phase 8: Config + Facade (30分钟)

### 8.1 SkillLoaderConfig.java
```java
public final class SkillLoaderConfig {
    private final List<PathConfig> skillRoots;
    private final String skillMarkerFile;
    private final boolean scanSubdirectories;
    private final List<SkillStorage> customStorages;
    private final boolean enableClasspathScan;
    private final String classpathRoot;
    private final SkillParser customParser;
    private final boolean preferSnakeYaml;
    private final String agentsMdTemplate;
    
    // Builder 模式
    public static Builder builder() { ... }
}
```

### 8.2 PathConfig.java (record)
```java
public record PathConfig(
    String name,
    URI uri,
    SkillSource source,
    int priority,
    boolean required
) {}
```

### 8.3 SkillLoaderBuilder.java
- [ ] addSkillRoot(Path, SkillSource)
- [ ] addClasspathRoot(String)
- [ ] addPath(PathConfig)
- [ ] parser(SkillParser)
- [ ] build()

### 8.4 SkillLoader.java (Facade)
```java
public final class SkillLoader {
    // 工厂方法
    public static SkillLoader createDefault();
    public static SkillLoaderBuilder builder();
    public static SkillLoader fromConfig(Path configFile);
    
    // 核心方法
    public List<Skill> discover();
    public SkillContent load(String name);
    public String generateAgentsMd();
    public void syncToFile(Path agentsMdPath);
    
    // 访问内部组件
    public SkillRegistry registry();
    public SkillLoaderConfig config();
}
```

---

## Phase 9: 配置文件支持 (20分钟)

### 9.1 默认配置加载
- [ ] 从 classpath 加载 skill-loader.properties
- [ ] 支持变量替换 (${user.home})

### 9.2 配置格式
```properties
skillloader.paths[0].name=default
skillloader.paths[0].uri=classpath:/skills
skillloader.paths[0].source=CLASSPATH
skillloader.paths[0].priority=10

skillloader.marker-file=SKILL.md
skillloader.scan-subdirectories=true
skillloader.prefer-snakeyaml=true
```

### 9.3 ConfigLoader.java (内部)
- [ ] 解析 properties
- [ ] 构建 SkillLoaderConfig

---

## Phase 10: 测试 (40分钟)

### 10.1 单元测试
- [ ] SkillParserTest - 测试 YAML 解析
- [ ] StorageTest - 测试 FileSystem/Classpath 存储
- [ ] RegistryTest - 测试发现与去重
- [ ] GeneratorTest - 测试 AGENTS.md 生成

### 10.2 集成测试
- [ ] SkillLoaderIntegrationTest - 完整流程
- [ ] 使用 test/resources/skills/ 下的示例 skill

### 10.3 测试资源
```
src/test/resources/
├── skills/
│   ├── pdf/
│   │   ├── SKILL.md
│   │   └── scripts/
│   │       └── extract.py
│   └── weather/
│       └── SKILL.md
```

### 10.4 示例 SKILL.md
```markdown
---
name: pdf
description: PDF manipulation toolkit
context: document-processing
tags: [document, pdf]
---

# PDF Skill

When user asks about PDFs...
```

---

## Phase 11: 文档 (20分钟)

### 11.1 README.md
- [ ] 项目简介
- [ ] 快速开始
- [ ] 配置说明
- [ ] 扩展指南

### 11.2 CHANGELOG.md (初始)

### 11.3 LICENSE (Apache 2.0)

---

## 检查清单

### 代码质量
- [ ] 所有公共 API 有 Javadoc
- [ ] 内部类标记 @Internal
- [ ] 使用 Java 21 新特性 (record, switch expr, text blocks)
- [ ] 无警告编译

### 测试覆盖
- [ ] 核心类 80%+ 覆盖
- [ ] 边界情况测试
- [ ] 异常路径测试

### 交付物
- [ ] 可编译的 Maven 项目
- [ ] 完整的测试套件
- [ ] 使用示例
- [ ] 设计文档

---

## 时间预估

| Phase | 任务 | 时间 |
|-------|------|------|
| 1 | 项目骨架 | 30min |
| 2 | 核心模型 | 30min |
| 3 | Storage SPI | 40min |
| 4 | Parser SPI | 40min |
| 5 | PathProvider | 30min |
| 6 | Registry | 30min |
| 7 | Generator | 30min |
| 8 | Facade + Config | 30min |
| 9 | 配置加载 | 20min |
| 10 | 测试 | 40min |
| 11 | 文档 | 20min |
| **总计** | | **~5.5h** |

---

## 下一步

确认后，开始 **Phase 1: 项目骨架**
