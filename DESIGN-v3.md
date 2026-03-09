# SkillLoader Java SDK - 只读版设计方案

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
      
    # 优先级 4: 系统 skills
    - name: system
      path: /usr/share/skillloader/skills
      priority: 40
```

### 同名处理

```java
// 同名 skill，优先级高的覆盖低的
// 例: 项目本地 pdf skill 会覆盖全局 pdf skill
```

## 配置文件

### 支持位置（按优先级）

1. `skillloader.yml` - 项目根目录（最高优先级）
2. `skillloader.properties` - 项目根目录
3. `application.yml` - 项目根目录（Spring Boot 风格）
4. `META-INF/skillloader.yml` - classpath 内
5. 系统属性: `-Dskillloader.config=/path/to/config.yml`

### 完整配置示例

```yaml
skillloader:
  # 白名单路径列表（必须配置，否则报错）
  paths:
    - name: project-skills
      path: ./skills                    # 相对路径（相对于工作目录）
      priority: 10
      required: false                   # 不存在不报错
      
    - name: resources-skills
      path: classpath:/skills           # classpath 路径
      priority: 20
      required: false
      
    - name: user-skills
      path: ${user.home}/.skillloader/skills  # 系统变量
      priority: 30
      required: false
  
  # 解析配置
  parser:
    marker-file: SKILL.md               # skill 标识文件
    encoding: UTF-8
    max-file-size: 1MB                  # 单文件大小限制
    
  # 安全限制
  security:
    strict-mode: true                   # 严格模式：只能读白名单路径
    allow-symlinks: false               # 禁止符号链接
    max-depth: 3                        # 目录扫描最大深度
    
  # 生成配置
  generator:
    template: default                   # AGENTS.md 模板
    marker-start: "<!-- SKILLS_TABLE_START -->"
    marker-end: "<!-- SKILLS_TABLE_END -->"
```

### Properties 格式

```properties
# 路径配置
skillloader.paths[0].name=project-skills
skillloader.paths[0].path=./skills
skillloader.paths[0].priority=10
skillloader.paths[0].required=false

skillloader.paths[1].name=resources-skills
skillloader.paths[1].path=classpath:/skills
skillloader.paths[1].priority=20

# 解析配置
skillloader.parser.marker-file=SKILL.md
skillloader.parser.encoding=UTF-8

# 安全
skillloader.security.strict-mode=true
skillloader.security.allow-symlinks=false
```

## 架构设计

```
┌──────────────────────────────────────────────────────────────┐
│                      SkillLoader (Read-Only)                  │
├──────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌───────────────┐  │
│  │  ConfigLoader   │  │  PathValidator  │  │  PathScanner  │  │
│  │  (配置加载)      │  │  (路径白名单校验)│  │  (目录扫描)   │  │
│  └────────┬────────┘  └────────┬────────┘  └───────┬───────┘  │
│           │                    │                   │          │
│           └────────────────────┴───────────────────┘          │
│                              │                                │
│  ┌───────────────────────────▼────────────────────────────┐  │
│  │                 SecureFileReader                      │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │  │
│  │  │  路径检查     │→ │  读取内容    │→ │  返回结果    │  │  │
│  │  │  (白名单?)   │  │  (限制大小)  │  │             │  │  │
│  │  └──────────────┘  └──────────────┘  └──────────────┘  │  │
│  └─────────────────────────────────────────────────────────┘  │
│                              │                                │
│  ┌───────────────────────────▼────────────────────────────┐  │
│  │                 SkillParser                           │  │
│  │  YAML Frontmatter 解析 + Markdown 提取                │  │
│  └─────────────────────────────────────────────────────────┘  │
│                              │                                │
│  ┌───────────────────────────▼────────────────────────────┐  │
│  │                 SkillRegistry                         │  │
│  │  内存索引 (name → Skill)                               │  │
│  │  优先级去重                                            │  │
│  └─────────────────────────────────────────────────────────┘  │
│                              │                                │
│  ┌───────────────────────────▼────────────────────────────┐  │
│  │              AgentsMdGenerator                        │  │
│  │  生成 XML skills_system 块                             │  │
│  └─────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

## 核心 API

### SkillLoader (门面)

```java
public final class SkillLoader {
    
    /**
     * 从默认配置文件创建
     */
    public static SkillLoader createDefault();
    
    /**
     * 从指定配置文件创建
     */
    public static SkillLoader fromConfig(Path configPath);
    
    /**
     * 从配置对象创建
     */
    public static SkillLoader fromConfig(SkillLoaderConfig config);
    
    /**
     * 发现所有可用的 skills（按优先级去重）
     */
    public List<Skill> discover();
    
    /**
     * 加载指定 skill 的完整内容
     */
    public SkillContent load(String skillName) throws SkillNotFoundException;
    
    /**
     * 获取 skill 元数据（不加载完整内容）
     */
    public Optional<SkillMetadata> getMetadata(String skillName);
    
    /**
     * 生成 AGENTS.md 内容
     */
    public String generateAgentsMd();
    
    /**
     * 获取白名单路径列表（调试用）
     */
    public List<PathEntry> getAllowedPaths();
    
    /**
     * 获取配置
     */
    public SkillLoaderConfig getConfig();
}
```

### 配置类

```java
public record SkillLoaderConfig(
    List<PathEntry> paths,           // 白名单路径（必须）
    ParserConfig parser,
    SecurityConfig security,
    GeneratorConfig generator
) {
    public SkillLoaderConfig {
        // 验证：至少有一个路径
        if (paths == null || paths.isEmpty()) {
            throw new SkillLoaderException("At least one path must be configured");
        }
        // 验证：路径按优先级排序
        paths = paths.stream()
            .sorted(Comparator.comparingInt(PathEntry::priority))
            .toList();
    }
}

public record PathEntry(
    String name,           // 标识名
    String path,           // 路径字符串（支持 ${var} 变量）
    int priority,          // 优先级（数字越小越优先）
    boolean required,      // 是否必须存在
    PathType type          // FILESYSTEM 或 CLASSPATH
) {}

public record ParserConfig(
    String markerFile,     // 默认: "SKILL.md"
    String encoding,       // 默认: "UTF-8"
    long maxFileSize       // 默认: 1MB
) {}

public record SecurityConfig(
    boolean strictMode,    // 默认: true
    boolean allowSymlinks, // 默认: false
    int maxDepth           // 默认: 3
) {}

public record GeneratorConfig(
    String template,
    String markerStart,
    String markerEnd
) {}
```

### 安全读取器

```java
/**
 * 带白名单校验的文件读取器
 */
public interface SecureFileReader {
    /**
     * 检查路径是否在白名单内
     */
    boolean isAllowed(Path path);
    
    /**
     * 安全读取文件内容
     * @throws SecurityException 如果路径不在白名单
     * @throws IOException 读取失败
     */
    String read(Path path) throws SecurityException, IOException;
    
    /**
     * 列出目录内容（仅一层）
     */
    List<Path> listDirectory(Path dir) throws SecurityException, IOException;
    
    /**
     * 检查文件是否存在且在白名单
     */
    boolean exists(Path path);
}
```

## 目录扫描

### 扫描策略

```java
public class SkillScanner {
    
    public List<Skill> scan(PathEntry entry) {
        // 1. 验证路径在白名单
        if (!reader.isAllowed(path)) {
            throw new SecurityException("Path not in whitelist: " + path);
        }
        
        // 2. 检查目录存在
        if (!reader.exists(path)) {
            if (entry.required()) {
                throw new SkillLoaderException("Required path not found: " + path);
            }
            return List.of();
        }
        
        // 3. 扫描子目录（限制深度）
        return scanDirectory(path, 0, entry.priority());
    }
    
    private List<Skill> scanDirectory(Path dir, int depth, int priority) {
        // 检查深度限制
        if (depth > config.security().maxDepth()) {
            return List.of();
        }
        
        List<Skill> skills = new ArrayList<>();
        
        for (Path subdir : reader.listDirectory(dir)) {
            // 检查是否是 skill 目录（包含 SKILL.md）
            if (isSkillDirectory(subdir)) {
                skills.add(parseSkill(subdir, priority));
            } else if (isDirectory(subdir)) {
                // 递归扫描（增加深度）
                skills.addAll(scanDirectory(subdir, depth + 1, priority));
            }
        }
        
        return skills;
    }
    
    private boolean isSkillDirectory(Path dir) {
        Path marker = dir.resolve(config.parser().markerFile());
        return reader.exists(marker);
    }
}
```

## 使用示例

### 场景 1: 最简单用法

```java
// 在项目根目录创建 skillloader.yml
// 然后直接创建 Loader

SkillLoader loader = SkillLoader.createDefault();

// 发现所有 skills
List<Skill> skills = loader.discover();
System.out.println("Found " + skills.size() + " skills");

// 加载特定 skill
SkillContent pdf = loader.load("pdf");

// 生成 AGENTS.md
String agentsMd = loader.generateAgentsMd();
Files.writeString(Path.of("AGENTS.md"), agentsMd);
```

### 场景 2: 显式配置

```java
// 代码中显式指定配置
SkillLoaderConfig config = new SkillLoaderConfig(
    List.of(
        new PathEntry("project", "./skills", 10, false, PathType.FILESYSTEM),
        new PathEntry("resources", "classpath:/skills", 20, false, PathType.CLASSPATH),
        new PathEntry("user", "${user.home}/.myapp/skills", 30, false, PathType.FILESYSTEM)
    ),
    new ParserConfig("SKILL.md", "UTF-8", 1024 * 1024),
    new SecurityConfig(true, false, 3),
    new GeneratorConfig("default", "<!-- START -->", "<!-- END -->")
);

SkillLoader loader = SkillLoader.fromConfig(config);
```

### 场景 3: 检查白名单

```java
SkillLoader loader = SkillLoader.createDefault();

// 查看允许读取的路径
List<PathEntry> allowed = loader.getAllowedPaths();
allowed.forEach(e -> System.out.println(e.name() + ": " + e.path()));
```

### 场景 4: Spring Boot 集成

```java
@Configuration
public class SkillLoaderConfiguration {
    
    @Bean
    public SkillLoader skillLoader(
            @Value("${skillloader.config:skillloader.yml}") String configPath) {
        return SkillLoader.fromConfig(Path.of(configPath));
    }
    
    @Bean
    public String agentsMdContent(SkillLoader loader) {
        return loader.generateAgentsMd();
    }
}
```

## 错误处理

### 异常体系

```
SkillLoaderException (checked)
├── ConfigException           # 配置错误
├── SecurityException         # 安全违规（路径不在白名单）
├── SkillNotFoundException    # Skill 不存在
└── SkillParseException       # 解析失败
```

### 常见错误

```java
// 1. 未配置路径
try {
    SkillLoader loader = SkillLoader.fromConfig(Path.of("empty.yml"));
} catch (ConfigException e) {
    // "At least one path must be configured"
}

// 2. 路径不在白名单
try {
    // loader 内部会自动校验
    // 用户无法直接访问 SecureFileReader
} catch (SecurityException e) {
    // "Path not in whitelist: /etc/passwd"
}

// 3. Skill 不存在
try {
    loader.load("nonexistent");
} catch (SkillNotFoundException e) {
    // "Skill not found: nonexistent"
}
```

## 测试策略

### 安全测试

```java
@Test
void shouldRejectPathOutsideWhitelist() {
    // 配置只允许 /allowed/path
    SkillLoaderConfig config = new SkillLoaderConfig(
        List.of(new PathEntry("allowed", "/allowed/path", 10, true, PathType.FILESYSTEM)),
        ...
    );
    
    SecureFileReader reader = new SecureFileReader(config.paths());
    
    // 尝试读取白名单外路径
    assertThrows(SecurityException.class, () -> {
        reader.read(Path.of("/etc/passwd"));
    });
    
    // 尝试路径遍历
    assertThrows(SecurityException.class, () -> {
        reader.read(Path.of("/allowed/path/../../../etc/passwd"));
    });
}

@Test
void shouldRejectSymlinksWhenDisabled() {
    // 创建符号链接
    Path symlink = Files.createSymbolicLink(
        Path.of("/allowed/path/link"),
        Path.of("/secret/file")
    );
    
    SecureFileReader reader = new SecureFileReader(config.paths(), false);
    
    assertThrows(SecurityException.class, () -> {
        reader.read(symlink);
    });
}
```

## 模块结构

```
skill-loader-core/
├── src/main/java/com/skillloader/
│   ├── api/
│   │   ├── SkillLoader.java              # 门面
│   │   ├── SkillLoaderBuilder.java       # 建造者
│   │   └── exceptions/
│   │       ├── SkillLoaderException.java
│   │       ├── ConfigException.java
│   │       ├── SecurityException.java
│   │       ├── SkillNotFoundException.java
│   │       └── SkillParseException.java
│   │
│   ├── config/
│   │   ├── SkillLoaderConfig.java        # 配置对象
│   │   ├── PathEntry.java
│   │   ├── ParserConfig.java
│   │   ├── SecurityConfig.java
│   │   ├── GeneratorConfig.java
│   │   └── ConfigLoader.java             # 配置加载器
│   │
│   ├── model/
│   │   ├── Skill.java
│   │   ├── SkillMetadata.java
│   │   ├── SkillContent.java
│   │   ├── ResourceRef.java
│   │   └── PathType.java                 # FILESYSTEM/CLASSPATH
│   │
│   ├── scanner/
│   │   ├── SkillScanner.java             # 目录扫描
│   │   └── ScanResult.java
│   │
│   ├── parser/
│   │   ├── SkillParser.java              # SKILL.md 解析
│   │   ├── YamlFrontmatterParser.java    # 轻量级 YAML
│   │   └── SnakeYamlDetector.java        # 检测 SnakeYAML
│   │
│   ├── reader/
│   │   ├── SecureFileReader.java         # 安全读取器（核心）
│   │   ├── FileSystemReader.java         # 文件系统实现
│   │   └── ClasspathReader.java          # Classpath 实现
│   │
│   ├── registry/
│   │   ├── SkillRegistry.java            # 注册表接口
│   │   └── DefaultSkillRegistry.java     # 实现
│   │
│   └── generator/
│       ├── AgentsMdGenerator.java        # 生成器接口
│       └── DefaultAgentsMdGenerator.java # 实现
│
└── src/main/resources/
    └── default-skillloader.yml           # 默认配置
```

## 实现优先级

| Phase | 内容 | 时间 |
|-------|------|------|
| 1 | 项目骨架 + 异常体系 | 30min |
| 2 | 配置系统（加载+验证） | 40min |
| 3 | 安全读取器（白名单） | 40min |
| 4 | 目录扫描器 | 30min |
| 5 | YAML 解析器 | 30min |
| 6 | 注册表（优先级去重） | 30min |
| 7 | AGENTS.md 生成器 | 30min |
| 8 | SkillLoader 门面 | 20min |
| 9 | 安全测试 | 40min |
| 10 | 集成测试 + 示例 | 30min |
| **总计** | | **~5h** |
