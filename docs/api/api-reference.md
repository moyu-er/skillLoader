# API 参考

## SkillLoader 核心 API

### SkillLoader 类

#### 创建实例

```java
// 使用默认配置（扫描 classpath:resources/skills/）
SkillLoader loader = SkillLoader.createDefault();

// 使用自定义配置
SkillLoader loader = SkillLoader.builder()
    .addFilesystemPath("local", "./my-skills")
    .addClasspathPath("builtin", "skills")
    .build();

// 从配置文件加载
SkillLoader loader = SkillLoader.fromConfig(Path.of("skillloader.yml"));
```

#### 主要方法

| 方法 | 返回类型 | 说明 |
|------|----------|------|
| `discover()` | `List<Skill>` | 发现所有可用的 skills |
| `load(String skillName)` | `SkillContent` | 加载指定 skill 的完整内容（含 metadata + markdown + resources）|
| `getMetadata(String skillName)` | `Optional<SkillMetadata>` | 仅获取 skill 元数据（轻量级，不解析完整内容）|
| `generateAgentsMd()` | `String` | 生成 AGENTS.md 内容（字符串）|
| `syncToFile(Path)` | `void` | 将 AGENTS.md 写入文件（需启用 generator）|
| `updateFile(Path)` | `void` | 更新现有 AGENTS.md 文件（需启用 generator）|
| `getAllowedPaths()` | `List<PathEntry>` | 获取允许访问的路径列表（调试用）|
| `getConfig()` | `SkillLoaderConfig` | 获取当前配置 |
| `registry()` | `SkillRegistry` | 获取注册表（高级用法）|

---

## Skill 类

代表一个发现的 skill。

### 属性

| 属性 | 类型 | 说明 |
|------|------|------|
| `name` | `String` | skill 名称 |
| `description` | `String` | skill 描述 |
| `source` | `SkillSource` | 来源（PROJECT/GLOBAL/CLASSPATH）|
| `location` | `Path` | 文件系统路径 |
| `priority` | `int` | 优先级（数字越小优先级越高）|

### 示例

```java
for (Skill skill : loader.discover()) {
    System.out.println(skill.name());
    System.out.println(skill.description());
}
```

---

## SkillContent 类

代表 skill 的完整内容。

### 属性

| 属性 | 类型 | 说明 |
|------|------|------|
| `metadata` | `SkillMetadata` | 元数据 |
| `markdownContent` | `String` | Markdown 文档内容 |
| `baseDir` | `Path` | skill 目录的根路径 |
| `resources` | `List<ResourceRef>` | 资源文件列表 |

### 示例

```java
SkillContent content = loader.load("git-workflow");

// 获取元数据
SkillMetadata meta = content.metadata();
System.out.println(meta.name());
System.out.println(meta.description());

// 获取文档
String doc = content.markdownContent();

// 获取 skill 目录路径
Path skillDir = content.baseDir();

// 获取资源文件列表
for (ResourceRef ref : content.resources()) {
    System.out.println(ref.name() + " (" + ref.type() + ")");
}
```

### 读取资源文件内容

SkillLoader 返回资源引用，**不包含文件内容读取**。外部服务需要自己读取：

```java
SkillContent content = loader.load("git-workflow");

// 找到特定资源
ResourceRef resource = content.resources().stream()
    .filter(r -> r.name().equals("script.sh"))
    .findFirst()
    .orElseThrow();

// 读取内容（外部服务实现）
Path resourcePath = Paths.get(resource.uri());
String content = Files.readString(resourcePath);
```

---

## SkillMetadata 类

代表 skill 的元数据。

### 属性

| 属性 | 类型 | 说明 |
|------|------|------|
| `name` | `String` | 名称 |
| `description` | `String` | 描述 |
| `context` | `Optional<String>` | 上下文 |
| `tags` | `List<String>` | 标签列表 |
| `extra` | `Map<String, Object>` | 额外字段 |

### 示例

```java
SkillMetadata meta = content.metadata();

System.out.println("Name: " + meta.name());
System.out.println("Description: " + meta.description());
System.out.println("Context: " + meta.context().orElse("N/A"));
System.out.println("Tags: " + String.join(", ", meta.tags()));

// 访问额外字段
Object author = meta.extra().get("author");
Object version = meta.extra().get("version");
```

---

## ResourceRef 类

代表资源文件引用。

### 属性

| 属性 | 类型 | 说明 |
|------|------|------|
| `name` | `String` | 文件名 |
| `uri` | `URI` | 文件 URI |
| `type` | `ResourceType` | 类型（SCRIPT/REFERENCE/ASSET/OTHER）|

### ResourceType 枚举

| 类型 | 说明 | 典型用途 |
|------|------|----------|
| `SCRIPT` | 可执行脚本 | .sh, .py, .js 等 |
| `REFERENCE` | 参考文档 | .md, .txt, .toml 等 |
| `ASSET` | 静态资源 | .png, .jpg, .svg 等 |
| `OTHER` | 其他 | 未知类型 |

### 示例

```java
for (ResourceRef ref : content.resources()) {
    System.out.println("Name: " + ref.name());
    System.out.println("Type: " + ref.type());
    System.out.println("URI: " + ref.uri());
    
    // 读取内容
    Path path = Paths.get(ref.uri());
    String content = Files.readString(path);
}
```

---

## 配置类

### SkillLoaderConfig

```java
// 默认配置
SkillLoaderConfig defaults = SkillLoaderConfig.defaults();

// 自定义配置
SkillLoaderConfig config = SkillLoaderConfig.builder()
    .addFilesystemPath("project", "./skills", 10, false)
    .addClasspathPath("builtin", "/skills", 20, false)
    .security(new SecurityConfig(true, false, 5))
    .parser(new ParserConfig("SKILL.md", "UTF-8", 1024 * 1024))
    .generator(new GeneratorConfig("default", "<!-- START -->", "<!-- END -->", false))
    .build();
```

### SecurityConfig

```java
SecurityConfig security = new SecurityConfig(
    true,    // strictMode: 严格模式
    false,   // allowSymlinks: 允许符号链接
    5        // maxDepth: 最大扫描深度
);
```

### ParserConfig

```java
ParserConfig parser = new ParserConfig(
    "SKILL.md",      // markerFile: 标记文件名
    "UTF-8",         // encoding: 文件编码
    1024 * 1024      // maxFileSize: 最大文件大小（字节）
);
```

### GeneratorConfig

```java
GeneratorConfig generator = new GeneratorConfig(
    "default",                          // template: 模板名称
    "<!-- SKILLS_TABLE_START -->",    // markerStart: 开始标记
    "<!-- SKILLS_TABLE_END -->",      // markerEnd: 结束标记
    false                               // enabled: 是否启用
);
```

---

## 异常类

### SkillNotFoundException

当 skill 不存在时抛出。

```java
try {
    loader.load("non-existent");
} catch (SkillNotFoundException e) {
    System.out.println("Skill not found: " + e.getMessage());
}
```

### SkillParseException

当解析 SKILL.md 失败时抛出。

```java
try {
    loader.load("invalid-skill");
} catch (SkillParseException e) {
    System.out.println("Parse failed: " + e.getMessage());
}
```

### SecurityException

当访问超出白名单范围时抛出。

```java
try {
    loader.load("outside-whitelist");
} catch (SecurityException e) {
    System.out.println("Access denied: " + e.getMessage());
}
```

---

## 完整示例

```java
import com.skillloader.api.SkillLoader;
import com.skillloader.model.*;

public class SkillDemo {
    public static void main(String[] args) {
        // 创建 loader
        SkillLoader loader = SkillLoader.createDefault();
        
        // 发现所有 skills
        System.out.println("=== Available Skills ===");
        for (Skill skill : loader.discover()) {
            System.out.println(skill.name() + ": " + skill.description());
        }
        
        // 加载特定 skill
        System.out.println("\\n=== Git Workflow Skill ===");
        SkillContent content = loader.load("git-workflow");
        
        // 打印元数据
        SkillMetadata meta = content.metadata();
        System.out.println("Name: " + meta.name());
        System.out.println("Description: " + meta.description());
        System.out.println("Tags: " + meta.tags());
        
        // 打印文档
        System.out.println("\\nContent:");
        System.out.println(content.markdownContent().substring(0, 500));
        
        // 打印资源
        System.out.println("\\nResources:");
        for (ResourceRef ref : content.resources()) {
            System.out.println("- " + ref.name() + " (" + ref.type() + ")");
        }
    }
}
```
