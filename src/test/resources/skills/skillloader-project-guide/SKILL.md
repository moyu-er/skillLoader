---
name: skillloader-project-guide
description: SkillLoader 项目开发规范。包含编码规范、JUnit 5 测试要求、CI/CD 流程、PR 规范等。
tags: [java, junit5, cicd, pr, guidelines]
---

# SkillLoader 项目开发规范

## 1. 编码规范

### 1.1 Java 版本
- **必须使用 Java 21**
- 使用新特性：record、switch expressions、text blocks、pattern matching

### 1.2 代码风格
- 缩进：4 个空格
- 行宽：120 字符
- 括号：Egyptian style (同号同行)
- 类/方法：必须有 Javadoc

### 1.3 命名规范
```java
// 类名：PascalCase
public class SkillLoader { }

// 方法名：camelCase
public SkillContent loadSkill(String name) { }

// 常量：UPPER_SNAKE_CASE
public static final String DEFAULT_MARKER = "SKILL.md";

// 包名：全小写
package com.skillloader.parser;
```

## 2. JUnit 5 测试规范

### 2.1 测试覆盖率要求

| 模块 | 覆盖率要求 |
|------|-----------|
| config | ≥ 80% |
| reader | ≥ 90%（安全关键）|
| parser | ≥ 85% |
| registry | ≥ 80% |
| generator | ≥ 75% |
| **整体** | **≥ 80%** |

### 2.2 测试类命名
```java
// 被测类：SkillLoader
// 测试类：SkillLoaderTest

// 被测类：SecureFileReader
// 测试类：SecureFileReaderTest
```

### 2.3 测试方法命名
```java
// 格式：should<ExpectedBehavior>When<Condition>

@Test
void shouldRejectPathOutsideWhitelistWhenStrictModeEnabled() { }

@Test
void shouldReturnEmptyListWhenDirectoryNotExists() { }

@Test
void shouldThrowSkillNotFoundExceptionWhenSkillNotExists() { }
```

### 2.4 测试结构（Given-When-Then）
```java
@Test
void shouldDiscoverSkillsFromMultiplePaths() {
    // Given
    Path tempDir = Files.createTempDirectory("skills");
    createSkill(tempDir.resolve("pdf"), "pdf");
    createSkill(tempDir.resolve("weather"), "weather");
    
    SkillLoaderConfig config = SkillLoaderConfig.builder()
        .addPath(tempDir, FILESYSTEM, 10)
        .build();
    
    SkillLoader loader = SkillLoader.fromConfig(config);
    
    // When
    List<Skill> skills = loader.discover();
    
    // Then
    assertThat(skills).hasSize(2);
    assertThat(skills).extracting(Skill::name)
        .containsExactlyInAnyOrder("pdf", "weather");
}
```

### 2.5 必备测试场景

#### 安全测试（SecureFileReader）
```java
@Test
void shouldRejectPathOutsideWhitelist() { }

@Test
void shouldRejectPathTraversalAttack() { }

@Test
void shouldRejectSymlinkWhenNotAllowed() { }

@Test
void shouldAllowPathInsideWhitelist() { }

@Test
void shouldThrowExceptionWhenFileTooLarge() { }
```

#### 配置测试（ConfigLoader）
```java
@Test
void shouldThrowExceptionWhenNoPathsConfigured() { }

@Test
void shouldSortPathsByPriority() { }

@Test
void shouldResolveVariables() { }

@Test
void shouldLoadFromYaml() { }

@Test
void shouldLoadFromProperties() { }
```

#### 解析测试（SkillParser）
```java
@Test
void shouldParseValidSkillMd() { }

@Test
void shouldThrowExceptionWhenMissingFrontmatter() { }

@Test
void shouldThrowExceptionWhenInvalidYaml() { }

@Test
void shouldExtractMarkdownContent() { }

@Test
void shouldParseTags() { }
```

### 2.6 测试工具

```xml
<dependencies>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 2.7 测试数据

```java
// 使用 @TempDir 创建临时目录
@TempDir
Path tempDir;

// 使用工厂方法创建测试 skill
private void createSkill(Path dir, String name) throws IOException {
    Path skillDir = dir.resolve(name);
    Files.createDirectories(skillDir);
    String content = """
        ---
        name: %s
        description: Test %s skill
        ---
        # %s Skill
        Test content.
        """.formatted(name, name, name);
    Files.writeString(skillDir.resolve("SKILL.md"), content);
}
```

## 3. CI/CD 流程

### 3.1 GitHub Actions 工作流

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [ master, develop ]
  pull_request:
    branches: [ master, develop ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: maven
    
    - name: Build with Maven
      run: mvn clean compile
    
    - name: Run tests
      run: mvn test
    
    - name: Generate coverage report
      run: mvn jacoco:report
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
      with:
        file: target/site/jacoco/jacoco.xml
        fail_ci_if_error: true

  checkstyle:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
    
    - name: Check code style
      run: mvn checkstyle:check
```

### 3.2 Maven 配置

```xml
<!-- 测试覆盖率插件 -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>

<!-- Checkstyle 插件 -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>3.3.1</version>
    <configuration>
        <configLocation>checkstyle.xml</configLocation>
        <failOnViolation>true</failOnViolation>
    </configuration>
</plugin>
```

## 4. PR 规范

### 4.1 PR 前检查清单

```markdown
## PR Checklist

- [ ] 代码符合项目编码规范
- [ ] 所有测试通过 (`mvn test`)
- [ ] 测试覆盖率 ≥ 80%
- [ ] 新增代码有对应的单元测试
- [ ] 安全相关代码有安全测试
- [ ] 更新了相关文档（如有必要）
- [ ] Commit message 符合规范
```

### 4.2 Commit Message 规范

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type:**
- `feat`: 新功能
- `fix`: Bug 修复
- `test`: 测试相关
- `docs`: 文档
- `refactor`: 重构
- `chore`: 杂项

**示例:**
```
feat(config): add YAML config loader

- Support skillloader.yml and application.yml
- Support variable substitution ${user.home}
- Add validation for required paths

Closes #5
```

### 4.3 分支策略

```
feature/xxx ──PR──► develop ──PR──► master
```

- 所有开发在 `feature/*` 分支
- PR 目标：`develop`
- 禁止直接 push 到 `master`
- PR 必须通过 CI 检查

### 4.4 Code Review 要求

至少 1 人 review 通过，检查项：
- [ ] 代码逻辑正确
- [ ] 有适当的错误处理
- [ ] 有必要的单元测试
- [ ] 安全无隐患
- [ ] 符合编码规范

## 5. 本地开发流程

### 5.1 开发前
```bash
# 1. 更新代码
git checkout develop
git pull origin develop

# 2. 创建功能分支
git checkout -b feature/my-feature develop
```

### 5.2 开发中
```bash
# 1. 编写代码
# 2. 编写测试
# 3. 本地验证
mvn clean test

# 4. 检查覆盖率
mvn jacoco:report
open target/site/jacoco/index.html
```

### 5.3 提交前
```bash
# 1. 格式化代码
mvn spotless:apply

# 2. 检查代码风格
mvn checkstyle:check

# 3. 运行所有测试
mvn clean verify

# 4. 提交
git add -A
git commit -m "feat(xxx): xxx"
```

### 5.4 提交后
```bash
# 1. 推送到远程
git push origin feature/my-feature

# 2. 创建 PR
gh pr create --base develop --head feature/my-feature

# 3. 等待 CI 通过
# 4. 等待 Code Review
# 5. 合并
```

## 6. 安全要求

### 6.1 安全测试必须覆盖
- 路径白名单校验
- 路径遍历攻击防护
- 符号链接检查
- 文件大小限制

### 6.2 安全 Code Review 清单
- [ ] 无路径遍历漏洞
- [ ] 无任意文件读取
- [ ] 无符号链接绕过
- [ ] 输入已验证

## 7. 文档要求

### 7.1 代码文档
```java
/**
 * SkillLoader 门面类。
 * 提供统一的 API 用于发现、加载 skills 和生成 AGENTS.md。
 * 
 * <p>使用示例：
 * <pre>
 * SkillLoader loader = SkillLoader.createDefault();
 * List<Skill> skills = loader.discover();
 * </pre>
 * 
 * @author SkillLoader Team
 * @since 1.0.0
 */
public final class SkillLoader { }
```

### 7.2 更新文档时机
- 新增 API → 更新 DESIGN.md
- 变更配置 → 更新 README.md
- 新增测试要求 → 更新本文件
