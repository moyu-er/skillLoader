# Maven 项目 resources/skills 配置指南

## 问题

`ClasspathReader` 无法直接列出目录内容（Java ClassLoader 限制），导致无法自动发现 `resources/skills` 下的 skills。

## 临时解决方案（手动配置）

### 方案 1：使用 Filesystem 路径（推荐用于开发）

在开发阶段，使用文件系统路径指向 resources 目录：

```java
SkillLoader loader = SkillLoader.builder()
    .addFilesystemPath("project-skills", "src/main/resources/skills", 10, false)
    .build();
```

### 方案 2：在配置中明确列出所有 Skills

创建 `skillloader.yml`：

```yaml
skillloader:
  paths:
    - name: project-skills
      path: classpath:/skills
      priority: 10
      type: classpath
  # 显式列出所有 skills（临时方案）
  explicit-skills:
    - git-workflow
    - python-style
    - docker-guide
```

### 方案 3：使用 SkillLoaderFactory（自动检测）

```java
// 尝试文件系统，如果不存在则回退到 classpath
Path fsPath = Paths.get("src/main/resources/skills");
SkillLoader loader;

if (Files.exists(fsPath)) {
    loader = SkillLoader.builder()
        .addFilesystemPath("project", fsPath.toString())
        .build();
} else {
    // 生产环境 - 使用 classpath 但需要显式加载
    loader = SkillLoader.createDefault();
}
```

### 方案 4：手动指定 Skill 名称列表

```java
SkillLoader loader = SkillLoader.createDefault();

// 手动列出所有可用的 skill 名称
List<String> skillNames = List.of("git-workflow", "python-style", "docker-guide");

// 手动加载每个 skill
List<SkillContent> skills = skillNames.stream()
    .map(name -> loader.load(name))
    .collect(Collectors.toList());
```

## 最佳实践

1. **开发阶段**：使用 Filesystem 路径便于调试
2. **生产阶段**：打包后将 skills 复制到外部目录，使用 Filesystem 读取
3. **混合模式**：同时配置 Filesystem 和 Classpath，Filesystem 优先级高

```java
SkillLoader loader = SkillLoader.builder()
    .addFilesystemPath("external", "/opt/app/skills", 10, false)  // 优先级高
    .addClasspathPath("builtin", "skills", 20, false)              // 优先级低
    .build();
```
