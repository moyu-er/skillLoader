---
name: skillloader-git-workflow
description: SkillLoader 项目 Git 工作流规范。基于 Git Flow 简化版，包含分支策略、PR 流程、提交规范。
tags: [git, workflow, skillloader]
---

# SkillLoader Git 工作流规范

## 分支策略

### 核心分支

| 分支 | 用途 | 保护 |
|------|------|------|
| `master` | 生产代码，稳定版本 | ✅ 保护 |
| `develop` | 开发集成，功能测试 | ✅ 保护 |

### 临时分支

| 分支前缀 | 用途 | 来源 | 合并目标 |
|----------|------|------|----------|
| `feature/*` | 新功能开发 | develop | develop |
| `bugfix/*` | Bug 修复 | develop | develop |
| `hotfix/*` | 紧急生产修复 | master | master + develop |
| `release/*` | 版本发布准备 | develop | master |

## 工作流程

### 1. 开始新功能

```bash
# 从 develop 创建功能分支
git checkout develop
git pull origin develop
git checkout -b feature/my-feature develop

# 开发...开发...开发
git add -A
git commit -m "feat: add new feature"
git push -u origin feature/my-feature
```

### 2. 创建 PR

```bash
gh pr create \
  --base develop \
  --head feature/my-feature \
  --title "feat: my feature description" \
  --body "## Changes\n- Change 1\n- Change 2"
```

**PR 必须通过 CI 检查后才能合并**

### 3. 合并流程

```
feature/my-feature ──PR──► develop ──PR──► master
      │                         │
      └─ CI 检查通过            └─ CI 检查 + 代码审查通过
```

## 提交信息规范

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type

- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档
- `test`: 测试
- `refactor`: 重构
- `chore`: 杂项
- `ci`: CI/CD 配置

### 示例

```
feat(config): add YAML config loader

- Support skillloader.yml
- Support variable substitution
- Add validation

Closes #5
```

## 当前项目状态

- ✅ `master` - 基础框架完成
- ✅ `develop` - 开发分支已创建
- 🔄 `feature/*` - 新功能在此分支开发

## CI 要求

所有 PR 必须通过：
- [x] Maven 编译
- [x] JUnit 测试
- [x] 代码覆盖率
- [x] Checkstyle（警告级别）

## 禁止操作

- ❌ 直接推送 master
- ❌ 直接推送 develop  
- ❌ 合并失败的 PR
- ❌ 绕过代码审查
