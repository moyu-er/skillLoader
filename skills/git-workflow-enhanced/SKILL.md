---
name: git-workflow-enhanced
description: SkillLoader 项目 Git 工作流规范。包含分支管理、PR 流程、代码审查、冲突解决等完整规范。
tags: [git, workflow, skillloader, pr]
---

# Git 工作流规范

> 本规范强制执行，违反可能导致代码回滚或 PR 被拒绝。

---

## 🎯 PR 目标规则（重要！）

### 必须遵守的流程

```
feature/xxx ──PR──► develop ──PR──► master
      │                  │
      └─ 必须先到develop  └─ 发布时才到master
```

| 分支类型 | 允许 PR 目标 | 禁止 PR 目标 | 说明 |
|----------|-------------|-------------|------|
| `feature/*` | `develop` | `master` | ❌ 绝对禁止直接 PR 到 master |
| `bugfix/*` | `develop` | `master` | ❌ 绝对禁止直接 PR 到 master |
| `hotfix/*` | `master` + `develop` | - | 紧急修复可同时提两个 PR |
| `develop` | `master` | - | 发布时由管理员操作 |

### ⚠️ 红线警告

- **绝对禁止** `feature/*` 或 `bugfix/*` 分支直接 PR 到 `master`
- **绝对禁止** 未经 review 直接合并到 `develop` 或 `master`
- 违反以上规则将导致 PR 被关闭并要求重新提交

---

## 📝 提交规范

### Commit Message 格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type 说明

| Type | 含义 | 使用场景 |
|------|------|----------|
| `feat` | 新功能 | 新增功能、特性 |
| `fix` | Bug 修复 | 修复问题、错误 |
| `docs` | 文档 | 仅修改文档 |
| `test` | 测试 | 添加或修改测试 |
| `refactor` | 重构 | 代码重构，不影响功能 |
| `chore` | 杂项 | 构建、依赖、工具等 |
| `ci` | CI/CD | 持续集成配置修改 |

### 示例

```bash
feat(scanner): add description parsing from SKILL.md

- Parse YAML frontmatter to extract name and description
- Add fallback to directory name when name not specified

Fixes #10
```

---

## 🔍 代码审查规范

### PR 检查清单

```markdown
## PR Checklist

- [ ] 代码符合项目编码规范
- [ ] 所有测试通过 (`mvn test`)
- [ ] 测试覆盖率 ≥ 80%
- [ ] 新增代码有对应的单元测试
- [ ] 本地验证通过 (`mvn clean verify`)
- [ ] Commit message 符合规范
```

### 审查要求

1. 至少 1 人 review 通过
2. CI 检查必须全部通过
3. 代码覆盖率不能下降

---

## 🔄 开发流程

### 1. 开始新功能

```bash
# 从 develop 创建功能分支
git checkout develop
git pull origin develop
git checkout -b feature/my-feature develop

# 开发...开发...开发

# 提交前本地验证（必须）
mvn clean verify

# 提交
git add -A
git commit -m "feat(xxx): xxx"
git push origin feature/my-feature

# 创建 PR 到 develop
gh pr create --base develop --head feature/my-feature
```

### 2. 本地验证（提交前必做）

```bash
# 完整验证流程
mvn clean verify

# 包含：
# - checkstyle:check（代码风格）
# - compile（编译）
# - test（单元测试，必须全部通过）
# - jacoco:report（覆盖率报告）
```

### 3. 合并流程

```bash
# CI 通过后合并
gh pr merge <pr-number> --squash

# 删除功能分支
git branch -d feature/my-feature
git push origin --delete feature/my-feature
```

---

## 🆘 冲突解决

### 遇到冲突时

```bash
# 1. 同步目标分支最新代码
git checkout develop
git pull origin develop

# 2. 切换回功能分支并 rebase
git checkout feature/my-feature
git rebase develop

# 3. 解决冲突后继续
git add .
git rebase --continue

# 4. 强制推送（因为是 rebase）
git push --force-with-lease origin feature/my-feature
```

---

## ⚡ 紧急修复（Hotfix）

### 场景
生产环境发现严重 Bug，需要立即修复

### 流程

```bash
# 1. 从 master 创建 hotfix 分支
git checkout master
git pull origin master
git checkout -b hotfix/critical-bug master

# 2. 修复代码
# 3. 本地验证
mvn clean verify

# 4. 提交
git commit -m "fix: critical bug description"

# 5. 同时创建两个 PR
git push origin hotfix/critical-bug

# PR 1: hotfix → master（紧急发布）
gh pr create --base master --head hotfix/critical-bug

# PR 2: hotfix → develop（同步修复）
gh pr create --base develop --head hotfix/critical-bug
```

---

## 📋 分支命名规范

| 类型 | 命名格式 | 示例 |
|------|----------|------|
| 功能 | `feature/<描述>` | `feature/add-config-loader` |
| Bug修复 | `bugfix/<issue号>-<描述>` | `bugfix/15-fix-escaping` |
| 热修复 | `hotfix/<描述>` | `hotfix/critical-security-fix` |
| 文档 | `docs/<描述>` | `docs/update-readme` |
| 重构 | `refactor/<描述>` | `refactor/simplify-parser` |

---

## ❌ 禁止行为

1. **直接推送 master/develop** - 必须通过 PR
2. **跳过本地验证** - 必须 `mvn clean verify` 通过
3. **提交失败测试** - 所有测试必须绿色
4. **忽视代码审查** - 必须经过 review
5. **随意命名分支** - 必须符合命名规范
