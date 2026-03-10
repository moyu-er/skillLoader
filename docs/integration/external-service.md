# 外部服务集成指南

本文档面向**AI 平台/Agent 框架开发者**，介绍如何将 SkillLoader 集成到外部服务中。

## 目录

- [快速开始](#快速开始)
- [核心概念](#核心概念)
- [工具设计](#工具设计)
- [完整示例](#完整示例)
- [最佳实践](#最佳实践)

---

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.skillloader</groupId>
    <artifactId>skill-loader-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 初始化 SkillLoader

```java
@Service
public class SkillService {
    private final SkillLoader loader;
    
    public SkillService() {
        // 默认扫描 classpath:resources/skills/
        this.loader = SkillLoader.createDefault();
    }
    
    // 或自定义配置
    public SkillService(Path configPath) {
        this.loader = SkillLoader.fromConfig(configPath);
    }
}
```

---

## 核心概念

### Skill 结构

```
skill-name/
├── SKILL.md              # 主文档（必须）
├── references/           # 参考文档（可选）
│   └── api-reference.md
├── scripts/              # 可执行脚本（可选）
│   └── setup.sh
└── assets/               # 静态资源（可选）
    └── diagram.png
```

### 数据流

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  用户请求    │────▶│  模型决策    │────▶│  工具调用    │
└─────────────┘     └─────────────┘     └──────┬──────┘
                                                │
                       ┌────────────────────────┘
                       ▼
              ┌─────────────────┐
              │   SkillLoader   │
              │  - load(skill)  │
              │  - getResources │
              └────────┬────────┘
                       │
         ┌─────────────┼─────────────┐
         ▼             ▼             ▼
    ┌─────────┐  ┌─────────┐  ┌─────────┐
    │SKILL.md │  │reference│  │ scripts │
    │(主文档)  │  │(参考文档) │  │(可执行)  │
    └─────────┘  └─────────┘  └─────────┘
```

---

## 工具设计

### 需要提供给模型的工具

#### 工具 1：listSkills（列出所有 Skills）

**用途**：让模型知道有哪些技能可用

**实现**：

```java
@Tool(name = "listSkills", description = "列出所有可用的 skills")
public class ListSkillsTool {
    
    private final SkillLoader loader;
    
    public String execute() {
        List<Skill> skills = loader.discover();
        
        StringBuilder result = new StringBuilder();
        result.append("可用 skills 列表：\\n\\n");
        
        for (Skill skill : skills) {
            result.append("名称: ").append(skill.name()).append("\\n");
            result.append("描述: ").append(skill.description()).append("\\n");
            result.append("标签: ").append(skill.tags()).append("\\n");
            result.append("---\\n");
        }
        
        return result.toString();
    }
}
```

**模型使用示例**：

```
用户：我想学习 Git 工作流
模型：让我查看可用的 skills...
[调用 listSkills]
模型：找到 "git-workflow" skill，让我加载详细内容...
[调用 loadSkill]
```

---

#### 工具 2：loadSkill（加载 Skill 内容）

**用途**：获取 skill 的完整内容和元数据

**实现**：

```java
@Tool(name = "loadSkill", description = "加载指定 skill 的完整内容")
public class LoadSkillTool {
    
    private final SkillLoader loader;
    
    public String execute(@Param(name = "skillName") String skillName) {
        try {
            SkillContent content = loader.load(skillName);
            SkillMetadata metadata = content.metadata();
            
            StringBuilder result = new StringBuilder();
            
            // 1. 元数据
            result.append("# ").append(metadata.name()).append("\\n\\n");
            result.append("描述: ").append(metadata.description()).append("\\n");
            result.append("上下文: ").append(metadata.context()).append("\\n");
            result.append("标签: ").append(metadata.tags()).append("\\n\\n");
            
            // 2. 主文档内容
            result.append("## 文档内容\\n\\n");
            result.append(content.markdownContent()).append("\\n\\n");
            
            // 3. 资源文件列表
            List<ResourceRef> resources = content.resources();
            if (!resources.isEmpty()) {
                result.append("## 资源文件\\n\\n");
                for (ResourceRef ref : resources) {
                    result.append("- ").append(ref.name())
                          .append(" (").append(ref.type()).append(")\\n");
                }
            }
            
            return result.toString();
            
        } catch (SkillNotFoundException e) {
            return "Skill not found: " + skillName;
        }
    }
}
```

---

#### 工具 3：loadResource（加载资源文件内容）⭐ 关键

**用途**：获取 reference/script 中的文件内容

**实现**：

```java
@Tool(name = "loadResource", description = "加载 skill 中的资源文件内容")
public class LoadResourceTool {
    
    private final SkillLoader loader;
    
    public String execute(
            @Param(name = "skillName") String skillName,
            @Param(name = "resourceName") String resourceName) {
        
        try {
            SkillContent content = loader.load(skillName);
            
            // 查找资源
            ResourceRef resource = content.resources().stream()
                .filter(r -> r.name().equals(resourceName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Resource not found: " + resourceName));
            
            // 读取资源内容
            Path resourcePath = Paths.get(resource.uri());
            String resourceContent = Files.readString(resourcePath);
            
            StringBuilder result = new StringBuilder();
            result.append("资源文件: ").append(resourceName).append("\\n");
            result.append("类型: ").append(resource.type()).append("\\n");
            result.append("路径: ").append(resource.uri()).append("\\n\\n");
            result.append("内容:\\n");
            result.append("```\\n");
            result.append(resourceContent);
            result.append("\\n```\\n");
            
            return result.toString();
            
        } catch (Exception e) {
            return "Error loading resource: " + e.getMessage();
        }
    }
}
```

**模型使用示例**：

```
用户：按照 git-workflow 规范创建分支
模型：[调用 loadSkill skillName="git-workflow"]
模型：我看到有分支命名规范，让我查看具体的脚本...
[调用 loadResource skillName="git-workflow" resourceName="branch-check.sh"]
模型：根据规范和脚本内容，你应该执行以下步骤...
```

---

#### 工具 4：executeScript（执行脚本）⭐ 可选但强大

**用途**：执行 skill 中的可执行脚本

**⚠️ 安全警告**：需要严格的沙箱和安全控制

```java
@Tool(name = "executeScript", description = "执行 skill 中的脚本（需谨慎）")
public class ExecuteScriptTool {
    
    private final SkillLoader loader;
    private final ScriptExecutor executor;  // 沙箱执行器
    
    public String execute(
            @Param(name = "skillName") String skillName,
            @Param(name = "scriptName") String scriptName,
            @Param(name = "args") List<String> args) {
        
        try {
            // 1. 加载 skill
            SkillContent content = loader.load(skillName);
            
            // 2. 查找脚本
            ResourceRef script = content.resources().stream()
                .filter(r -> r.name().equals(scriptName) && r.type() == ResourceType.SCRIPT)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Script not found: " + scriptName));
            
            // 3. 安全检查（白名单、参数校验等）
            if (!isScriptAllowed(scriptName)) {
                return "Script execution not allowed for security reasons";
            }
            
            // 4. 在沙箱中执行
            Path scriptPath = Paths.get(script.uri());
            ExecutionResult result = executor.execute(scriptPath, args);
            
            return "执行结果:\\n" + result.getOutput();
            
        } catch (Exception e) {
            return "Execution failed: " + e.getMessage();
        }
    }
    
    private boolean isScriptAllowed(String scriptName) {
        // 白名单检查
        Set<String> allowedScripts = Set.of(
            "check-style.sh",
            "run-tests.sh"
            // 明确允许执行的脚本
        );
        return allowedScripts.contains(scriptName);
    }
}
```

---

## 完整示例

### Spring Boot 集成示例

```java
@RestController
@RequestMapping("/api/skills")
public class SkillController {
    
    private final SkillLoader loader;
    
    public SkillController() {
        this.loader = SkillLoader.createDefault();
    }
    
    @GetMapping
    public List<SkillInfo> listSkills() {
        return loader.discover().stream()
            .map(skill -> new SkillInfo(
                skill.name(),
                skill.description(),
                skill.tags()
            ))
            .collect(Collectors.toList());
    }
    
    @GetMapping("/{name}")
    public SkillDetail getSkill(@PathVariable String name) {
        SkillContent content = loader.load(name);
        return new SkillDetail(
            content.metadata(),
            content.markdownContent(),
            content.resources()
        );
    }
    
    @GetMapping("/{name}/resources/{resourceName}")
    public String getResource(
            @PathVariable String name,
            @PathVariable String resourceName) throws IOException {
        
        SkillContent content = loader.load(name);
        ResourceRef resource = content.resources().stream()
            .filter(r -> r.name().equals(resourceName))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException(resourceName));
        
        return Files.readString(Paths.get(resource.uri()));
    }
}
```

### OpenAI Function Calling 集成

```java
public class OpenAIIntegration {
    
    private final SkillLoader loader = SkillLoader.createDefault();
    
    // 定义工具列表
    private final List<FunctionTool> tools = List.of(
        FunctionTool.builder()
            .name("list_skills")
            .description("列出所有可用的 skills")
            .build(),
            
        FunctionTool.builder()
            .name("load_skill")
            .description("加载指定 skill 的完整内容")
            .parameters(Parameters.builder()
                .addProperty("skill_name", "string", "skill 名称")
                .build())
            .build(),
            
        FunctionTool.builder()
            .name("load_resource")
            .description("加载 skill 中的资源文件")
            .parameters(Parameters.builder()
                .addProperty("skill_name", "string", "skill 名称")
                .addProperty("resource_name", "string", "资源文件名")
                .build())
            .build()
    );
    
    public String chat(String userMessage) {
        // 1. 调用 OpenAI API，传入 tools
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model("gpt-4")
            .messages(List.of(
                new SystemMessage(buildSystemPrompt()),
                new UserMessage(userMessage)
            ))
            .tools(tools)
            .build();
        
        ChatCompletionResponse response = openAiClient.chat(request);
        
        // 2. 处理工具调用
        if (response.hasToolCalls()) {
            for (ToolCall toolCall : response.getToolCalls()) {
                String result = executeTool(toolCall);
                // 将结果返回给模型
                return continueConversation(response, result);
            }
        }
        
        return response.getContent();
    }
    
    private String executeTool(ToolCall toolCall) {
        return switch (toolCall.getName()) {
            case "list_skills" -> listSkills();
            case "load_skill" -> loadSkill(toolCall.getArguments());
            case "load_resource" -> loadResource(toolCall.getArguments());
            default -> throw new IllegalArgumentException("Unknown tool: " + toolCall.getName());
        };
    }
    
    private String listSkills() {
        return loader.discover().stream()
            .map(s -> s.name() + ": " + s.description())
            .collect(Collectors.joining("\\n"));
    }
    
    private String loadSkill(Map<String, Object> args) {
        String skillName = (String) args.get("skill_name");
        SkillContent content = loader.load(skillName);
        return content.markdownContent();
    }
    
    private String loadResource(Map<String, Object> args) throws IOException {
        String skillName = (String) args.get("skill_name");
        String resourceName = (String) args.get("resource_name");
        
        SkillContent content = loader.load(skillName);
        ResourceRef resource = content.resources().stream()
            .filter(r -> r.name().equals(resourceName))
            .findFirst()
            .orElseThrow();
        
        return Files.readString(Paths.get(resource.uri()));
    }
}
```

---

## 最佳实践

### 1. 工具设计原则

- **原子性**：每个工具只做一件事
- **安全性**：脚本执行需要白名单控制
- **幂等性**：多次调用结果一致

### 2. 提示词设计

```
你是一个智能助手，可以使用以下工具：

1. list_skills - 查看所有可用的 skills
2. load_skill - 加载 skill 的详细内容
3. load_resource - 加载 skill 中的资源文件（如脚本、配置模板）

使用流程：
1. 当用户提出需求时，先用 list_skills 查看是否有匹配的 skill
2. 如果有，使用 load_skill 加载详细内容
3. 如果需要参考具体配置或脚本，使用 load_resource 加载
4. 根据 skill 内容帮助用户完成任务
```

### 3. 缓存策略

```java
@Service
public class CachedSkillService {
    
    private final SkillLoader loader;
    private final Map<String, SkillContent> cache = new ConcurrentHashMap<>();
    
    public SkillContent getSkill(String name) {
        return cache.computeIfAbsent(name, loader::load);
    }
    
    public void refresh() {
        cache.clear();
        loader.registry().refresh();
    }
}
```

### 4. 错误处理

```java
try {
    SkillContent content = loader.load(skillName);
} catch (SkillNotFoundException e) {
    return "Skill 不存在，请使用 list_skills 查看可用选项";
} catch (SkillParseException e) {
    return "Skill 格式错误，请联系管理员";
} catch (SecurityException e) {
    return "访问被拒绝，可能超出允许的路径范围";
}
```

---

## 相关文档

- [快速开始](../README.md)
- [API 参考](./api-reference.md)
- [部署指南](./deployment-guide.md)
