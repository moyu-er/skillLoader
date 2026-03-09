# SkillLoader Java SDK - 详细设计方案

## 设计原则

1. **最小依赖**: 仅用 Java 21 标准库 + 可选 SnakeYAML
2. **高度抽象**: 核心接口可扩展，不绑定具体实现
3. **配置优先**: 所有路径、行为都可配置
4. **默认合理**: resources/skills 作为默认路径

## Maven 依赖策略

```xml
<dependencies>
    <!-- 核心模块: 零依赖 -->
    <!-- 仅用 Java 21 标准库 -->
    
    <!-- 可选: 如果用户需要更好的 YAML 支持 -->
    <dependency>
        <groupId>org.yaml</groupId>
        <artifactId>snakeyaml</artifactId>
        <version>2.2</version>
        <optional>true</optional>
    </dependency>
</dependencies>
```

**YAML 解析策略**:
- 首选: 内置轻量级 YAML 解析器（只解析简单 key-value）
- 备选: 如果 classpath 有 SnakeYAML，自动使用

## 核心架构

```
┌─────────────────────────────────────────────────────────────────┐
│                      SkillLoader (Facade)                        │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐ │
│  │   Config    │  │  Registry   │  │      Generator          │ │
│  │  (配置中心)  │  │  (Skill索引) │  │   (AGENTS.md生成)       │ │
│  └──────┬──────┘  └──────┬──────┘  └───────────┬─────────────┘ │
│         │                │                     │               │
│         └────────────────┴─────────────────────┘               │
│                          │                                     │
│  ┌───────────────────────▼──────────────────────────────────┐  │
│  │              Strategy / Provider 层 (抽象)                │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │  │
│  │  │PathProvider  │  │SkillParser   │  │ ContentLoader│   │  │
│  │  │(路径提供)    │  │ (解析策略)   │  │ (内容加载)   │   │  │
│  │  └──────────────┘  └──────────────┘  └──────────────┘   │  │
│  └──────────────────────────────────────────────────────────┘  │
│                          │                                     │
│  ┌───────────────────────▼──────────────────────────────────┐  │
│  │              Storage 层 (具体实现)                        │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │  │
│  │  │FileSystem    │  │Resource      │  │ Custom...    │   │  │
│  │  │Storage       │  │Storage       │  │              │   │  │
│  │  │(文件系统)    │  │(Classpath)   │  │              │   │  │
│  │  └──────────────┘  └──────────────┘  └──────────────┘   │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## 模块结构

```
skill-loader-core/
├── src/main/java/com/skillloader/
│   ├── api/                          # 公共 API (用户直接使用)
│   │   ├── SkillLoader.java          # Facade
│   │   ├── SkillLoaderBuilder.java   # 建造者
│   │   └── SkillLoadException.java   # 异常
│   │
│   ├── model/                        # 数据模型 (不变对象)
│   │   ├── Skill.java                # Skill 定义
│   │   ├── SkillMetadata.java        # YAML Frontmatter
│   │   ├── SkillContent.java         # 完整内容
│   │   ├── SkillSource.java          # 枚举 PROJECT/GLOBAL/CLASSPATH
│   │   └── ResourceRef.java          # 资源引用 (script, ref等)
│   │
│   ├── config/                       # 配置
│   │   ├── SkillLoaderConfig.java    # 配置对象
│   │   └── PathConfiguration.java    # 路径配置
│   │
│   ├── spi/                          # 服务提供者接口 (扩展点)
│   │   ├── SkillPathProvider.java    # 路径发现策略
│   │   ├── SkillParser.java          # 解析策略
│   │   ├── SkillContentLoader.java   # 内容加载策略
│   │   └── SkillStorage.java         # 存储抽象
│   │
│   ├── internal/                     # 内部实现 (用户不直接调用)
│   │   ├── registry/
│   │   │   └── DefaultSkillRegistry.java
│   │   ├── scanner/
│   │   │   └── SkillScanner.java
│   │   ├── parser/
│   │   │   ├── SimpleYamlParser.java     # 内置轻量YAML
│   │   │   └── SnakeYamlAdapter.java     # 适配器
│   │   ├── storage/
│   │   │   ├── FileSystemStorage.java
│   │   │   └── ClasspathStorage.java
│   │   └── generator/
│   │       └── DefaultAgentsMdGenerator.java
│   │
│   └── util/                         # 工具类
│       └── PathUtils.java
│
└── src/main/resources/
    └── com/skillloader/
        └── default-config.properties   # 默认配置
```

## SPI (扩展接口)

### 1. SkillPathProvider - 路径发现策略

```java
/**
 * 定义从哪里寻找 skills
 * 用户可实现自定义策略，如从数据库、远程API等获取路径
 */
public interface SkillPathProvider {
    /**
     * 返回搜索路径列表，按优先级排序
     */
    List<PathCandidate> providePaths();
    
    /**
     * 是否支持指定路径
     */
    boolean supports(URI uri);
}

/**
 * 路径候选
 */
public record PathCandidate(
    URI uri,
    SkillSource source,
    int priority           // 优先级，数字越小越优先
) {}
```

**默认实现**:
- `DefaultPathProvider`: 文件系统 + classpath
- `ClasspathPathProvider`: 仅 classpath
- `CompositePathProvider`: 组合多个 provider

### 2. SkillParser - 解析策略

```java
/**
 * SKILL.md 解析器
 * 可自定义解析逻辑，支持不同格式
 */
public interface SkillParser {
    /**
     * 是否支持解析该路径
     */
    boolean canParse(Path skillPath);
    
    /**
     * 解析 skill
     */
    SkillContent parse(Path skillPath) throws SkillParseException;
    
    /**
     * 仅解析 metadata (快速检查)
     */
    SkillMetadata parseMetadata(Path skillPath);
}
```

**默认实现**:
- `StandardSkillParser`: 标准 SKILL.md 格式
- `FrontmatterParser`: 仅解析 YAML frontmatter

### 3. SkillStorage - 存储抽象

```java
/**
 * Skill 存储层
 * 抽象文件系统、classpath、网络等差异
 */
public interface SkillStorage {
    /**
     * 是否存在
     */
    boolean exists(URI uri);
    
    /**
     * 列出目录内容
     */
    List<StorageEntry> list(URI uri);
    
    /**
     * 读取文件内容
     */
    String read(URI uri);
    
    /**
     * 读取为流
     */
    InputStream openStream(URI uri);
    
    /**
     * 获取基础路径（用于解析相对路径）
     */
    Path getBasePath(URI uri);
}

public record StorageEntry(
    String name,
    URI uri,
    boolean isDirectory
) {}
```

**默认实现**:
- `FileSystemStorage`: 本地文件系统
- `ClasspathStorage`: classpath 资源

## 配置系统

### SkillLoaderConfig

```java
public final class SkillLoaderConfig {
    
    // ========== 路径配置 ==========
    
    /**
     * Skill 根目录列表
     * 默认: [resources/skills]
     */
    private final List<PathConfig> skillRoots;
    
    /**
     * 是否扫描子目录
     * 默认: true
     */
    private final boolean scanSubdirectories;
    
    /**
     * Skill 标识文件
     * 默认: "SKILL.md"
     */
    private final String skillMarkerFile;
    
    // ========== 存储配置 ==========
    
    /**
     * 自定义 StorageProvider
     */
    private final List<SkillStorage> customStorages;
    
    /**
     * 是否启用 classpath 扫描
     * 默认: true
     */
    private final boolean enableClasspathScan;
    
    /**
     * Classpath 根路径
     * 默认: "skills"
     */
    private final String classpathRoot;
    
    // ========== 解析配置 ==========
    
    /**
     * 自定义 Parser
     */
    private final SkillParser customParser;
    
    /**
     * 是否使用 SnakeYAML（如果可用）
     * 默认: true
     */
    private final boolean preferSnakeYaml;
    
    // ========== 生成配置 ==========
    
    /**
     * AGENTS.md 模板
     */
    private final String agentsMdTemplate;
    
    /**
     * 是否包含 skill 内容（而不仅是列表）
     * 默认: false
     */
    private final boolean inlineSkillContent;
    
    // Builder 省略...
}

/**
 * 路径配置
 */
public record PathConfig(
    String name,                    // 标识名
    URI uri,                        // 路径 URI
    SkillSource source,             // 来源类型
    int priority,                   // 优先级
    boolean required                // 是否必须存在
) {}
```

### 使用示例

```java
// 方式1: 默认配置 (resources/skills)
SkillLoader loader = SkillLoader.createDefault();

// 方式2: Builder 配置
SkillLoader loader = SkillLoader.builder()
    // 添加文件系统路径
    .addSkillRoot(Path.of("/custom/skills"), SkillSource.PROJECT)
    
    // 添加 classpath 路径
    .addClasspathRoot("my-skills")
    
    // 完全自定义路径
    .addPath(PathConfig.builder()
        .name("db-skills")
        .uri(URI.create("jdbc:mysql://..."))
        .source(SkillSource.CUSTOM)
        .priority(1)
        .build())
    
    // 自定义 parser
    .parser(new MyCustomParser())
    
    .build();

// 方式3: 从配置文件加载
SkillLoader loader = SkillLoader.fromConfig(
    Path.of("skill-loader.properties")
);
```

## 默认配置

### skill-loader-defaults.properties

```properties
# 默认路径 (优先级从高到低)
skillloader.paths[0].name=project-classpath
skillloader.paths[0].uri=classpath:/skills
skillloader.paths[0].source=CLASSPATH
skillloader.paths[0].priority=10

skillloader.paths[1].name=user-config
skillloader.paths[1].uri=${user.home}/.skillloader/skills
skillloader.paths[1].source=GLOBAL
skillloader.paths[1].priority=20

# 解析配置
skillloader.parser.marker-file=SKILL.md
skillloader.parser.prefer-snakeyaml=true

# 扫描配置
skillloader.scan.subdirectories=true
skillloader.scan.follow-symlinks=false

# 生成配置
skillloader.generator.template=default
skillloader.generator.inline-content=false
```

## 在项目 A 中使用

### 场景1: 最简单用法

```java
// 项目 A 的 resources/skills/ 下有 skills
// 自动发现并加载

SkillLoader loader = SkillLoader.createDefault();
List<Skill> skills = loader.discover();

// 生成 AGENTS.md
String agentsMd = loader.generateAgentsMd();
Files.writeString(Path.of("AGENTS.md"), agentsMd);
```

### 场景2: 自定义路径

```java
// 项目 A 的 skills 在 resources/my-skills/

SkillLoader loader = SkillLoader.builder()
    .addClasspathRoot("my-skills")
    .build();
```

### 场景3: 多来源混合

```java
// 从 classpath + 文件系统 + 用户目录加载

SkillLoader loader = SkillLoader.builder()
    // 项目内置 skills (优先级最高)
    .addClasspathRoot("skills", 1)
    
    // 项目本地自定义 skills
    .addSkillRoot(Path.of("custom-skills"), SkillSource.PROJECT, 2)
    
    // 用户全局 skills
    .addSkillRoot(
        Path.of(System.getProperty("user.home"), ".myapp", "skills"),
        SkillSource.GLOBAL,
        3
    )
    .build();
```

### 场景4: Spring Boot 集成

```java
@Configuration
public class SkillLoaderConfig {
    
    @Bean
    public SkillLoader skillLoader(
            @Value("${skillloader.root:classpath:/skills}") String root) {
        return SkillLoader.builder()
            .addClasspathRoot(root.replace("classpath:/", ""))
            .build();
    }
    
    @Bean
    public SkillRegistry skillRegistry(SkillLoader loader) {
        return loader.registry();
    }
}
```

## 核心流程

### 1. Skill Discovery 流程

```
用户调用 discover()
    ↓
PathProvider 提供路径列表
    ↓
按 priority 排序
    ↓
对每个路径:
    Storage.list() 列出目录
    检查 SKILL.md 存在
    Parser.parseMetadata() 快速解析
    ↓
Registry 去重 (同名取高优先级)
    ↓
返回 List<Skill>
```

### 2. Skill Load 流程

```
用户调用 load("pdf")
    ↓
Registry.find("pdf") 定位 Skill
    ↓
Parser.parse(skillPath) 完整解析
    ↓
加载 resources/ (scripts/, references/等)
    ↓
返回 SkillContent
```

### 3. AGENTS.md 生成流程

```
用户调用 generateAgentsMd()
    ↓
Registry.discover() 获取所有 skill
    ↓
Generator 渲染模板
    ↓
输出 XML 格式
```

## 扩展示例

### 自定义 PathProvider (从数据库加载)

```java
public class DatabasePathProvider implements SkillPathProvider {
    
    private final DataSource dataSource;
    
    @Override
    public List<PathCandidate> providePaths() {
        // 从数据库查询 skill 路径
        List<SkillRecord> records = jdbcTemplate.query(
            "SELECT path, priority FROM skills WHERE enabled = true",
            ...
        );
        
        return records.stream()
            .map(r -> new PathCandidate(
                URI.create("db://" + r.path()),
                SkillSource.CUSTOM,
                r.priority()
            ))
            .toList();
    }
}
```

### 自定义 Storage (从 S3 加载)

```java
public class S3SkillStorage implements SkillStorage {
    
    private final S3Client s3Client;
    
    @Override
    public String read(URI uri) {
        String bucket = uri.getHost();
        String key = uri.getPath().substring(1);
        
        GetObjectRequest request = GetObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build();
            
        return s3Client.getObjectAsString(request);
    }
    
    // ... 其他方法
}
```

## 测试策略

```java
@Test
void shouldLoadSkillsFromClasspath() {
    // 使用内存文件系统或测试资源
    SkillLoader loader = SkillLoader.builder()
        .addClasspathRoot("test-skills")
        .build();
    
    List<Skill> skills = loader.discover();
    
    assertThat(skills).hasSize(2);
    assertThat(skills.get(0).name()).isEqualTo("pdf");
}

@Test
void shouldGenerateAgentsMd() {
    SkillLoader loader = SkillLoader.createDefault();
    
    String agentsMd = loader.generateAgentsMd();
    
    assertThat(agentsMd).contains("<skills_system>");
    assertThat(agentsMd).contains("</skills_system>");
}
```

## 实现优先级 (修订版)

| Phase | 内容 | 时间 | 说明 |
|-------|------|------|------|
| 1 | 项目骨架 + 模型定义 | 30min | Skill, SkillMetadata, SkillContent |
| 2 | Storage SPI + 实现 | 40min | FileSystemStorage, ClasspathStorage |
| 3 | Parser SPI + 实现 | 40min | SimpleYamlParser, SnakeYamlAdapter |
| 4 | PathProvider SPI | 30min | DefaultPathProvider |
| 5 | Registry | 30min | DefaultSkillRegistry |
| 6 | Generator | 30min | DefaultAgentsMdGenerator |
| 7 | Facade + Config | 30min | SkillLoader, Builder |
| 8 | 配置加载 | 20min | properties 支持 |
| 9 | 测试 | 40min | 单元测试 + 集成测试 |
| 10 | 示例 + 文档 | 20min | README, 示例项目 |

**总计: ~5.5 小时**
