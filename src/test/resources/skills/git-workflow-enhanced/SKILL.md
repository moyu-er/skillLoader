---
name: git-workflow-enhanced
description: MultiDemo 项目增强版 Git 工作流规范。包含 Fork 工作流、分支管理、PR流程、代码审查、冲突解决、多人协作等完整规范。
---

# Git 工作流规范（增强版）

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

## 🍴 Fork 工作流（重点！）

### ⚠️ 绝对规则（强制执行）

| 场景 | 正确做法 | 禁止做法 | 后果 |
|------|----------|----------|------|
| 修改代码 | 在 **Fork 的分支** 上修改 | ❌ 直接修改原仓库分支 | PR 被拒绝，要求重做 |
| 推送代码 | `git push myfork 分支名` | ❌ `git push origin 分支名` | 权限错误，无法推送 |
| 同步代码 | 从原仓库拉取更新到本地 | 只允许同步，不允许修改 | - |
| 合入代码 | 发起 PR: Fork分支 → 原仓库分支 | ❌ 直接合并到原仓库 | 无权限，必须通过 PR |

### 🔒 核心原则

1. **Fork 是你的工作区**
   - 所有开发在 **你的 Fork** 上进行
   - 原仓库分支（`origin/develop`, `origin/master`）**只读**，仅用于同步

2. **主仓分支不可直接修改**
   ```bash
   # ❌ 禁止 - 直接修改主仓分支
   git checkout origin/develop
   # 修改文件...
   git push origin develop  # 会失败或造成问题
   
   # ✅ 正确 - 在 Fork 上工作
   git checkout develop
   git pull origin develop    # 仅同步
   git checkout -b feature/xxx develop  # 创建功能分支
   # 修改文件...
   git push myfork feature/xxx  # 推送到你的 Fork
   gh pr create --repo xxsddm/multiDemo --base develop --head YOUR-NAME:feature/xxx
   ```

3. **所有变更必须通过 PR**
   ```
   你的 Fork/feature/xxx ──PR──► 原仓库/develop
   ```
   - 不允许直接 `git push` 到原仓库的任何分支
   - 不允许直接修改原仓库文件（GitHub Web 界面也不行）

### 为什么使用 Fork？

- **权限隔离**: 每个人都有自己的完整仓库副本
- **安全审查**: 所有变更必须通过 PR 审查后才能合并
- **独立开发**: 不影响原仓库，自由推送分支
- **协作友好**: 适合多人开源/团队协作

### Fork 工作流架构

```
原仓库 (upstream)
    │
    ├── develop ◄───────────────┐
    │     │                      │
    │     └── feature/A          │
    │                            │
    └── master                   │
                                 │
你的 Fork (origin)               │
    │                            │
    ├── develop ─────────────────┘
    │     │
    │     └── feature/your-feature
    │
    └── master
```

### 初始设置（一次性）

```bash
# 1. Fork 原仓库到个人账户（通过 GitHub Web 或 gh CLI）
gh repo fork xxsddm/multiDemo --remote --remote-name myfork

# 2. 确认 remote 配置
git remote -v
# 应显示：
# origin   https://github.com/xxsddm/multiDemo.git (fetch/push)
# myfork   https://github.com/YOUR-USERNAME/multiDemo.git (fetch/push)

# 3. 设置 upstream 跟踪原仓库
git remote add upstream https://github.com/xxsddm/multiDemo.git

# 4. 禁用直接推送到 upstream（防止误操作）
git remote set-url --push upstream no_push
```

### 日常开发流程（严格遵循）

```bash
# ========== 步骤1: 同步原仓库（只读操作）==========
# 目的：获取原仓库最新代码，保持同步
# 注意：origin/develop 和 origin/master 只用于同步，永不修改！

git fetch origin
git checkout develop
git merge origin/develop    # 仅同步，不修改
git push myfork develop     # 同步到你的 Fork

# ========== 步骤2: 创建功能分支（在你的 Fork 上）==========
# 重要：所有开发都在你的 Fork 分支上进行！

git checkout -b feature/your-feature develop

# ========== 步骤3: 开发和提交 ==========
git add -A
git commit -m "feat: 你的功能描述"

# ========== 步骤4: 推送到你的 Fork（不是原仓库！）==========
# ✅ 正确：推送到你的 Fork
git push myfork feature/your-feature

# ❌ 禁止：推送到原仓库
git push origin feature/your-feature  # 会失败！

# ========== 步骤5: 创建 PR 到原仓库 ==========
# 这是将代码合入主仓的唯一方式！

gh pr create \
  --repo xxsddm/multiDemo \
  --base develop \
  --head YOUR-USERNAME:feature/your-feature \
  --title "feat: 功能描述"
```

### ❌ 常见错误（禁止）

```bash
# 错误1: 直接修改原仓库分支
git checkout origin/develop
# 修改文件...
git add .
git commit -m "fix: xxx"
git push origin develop  # ❌ 失败！无权限

# 错误2: 试图推送到原仓库
git push origin feature/xxx  # ❌ 失败！

# 错误3: 在 GitHub Web 上直接编辑原仓库文件
# 这会直接在原仓库创建分支，违反 Fork 工作流
```

### 保持 Fork 同步（仅从原仓库拉取）

```bash
# ========== 每天/每次开发前执行 ==========
# 目的：将原仓库最新代码同步到你的 Fork
# 原则：只拉取，不修改！

git fetch origin
git checkout develop
git rebase origin/develop  # 或 git merge origin/develop

# 同步到你的 Fork（推送到你的仓库）
git push myfork develop

# ========== feature 分支同步 ==========
# 如果 feature 分支落后于 develop，需要同步

git checkout feature/your-feature
git rebase develop
# 解决冲突...
git push myfork feature/your-feature --force-with-lease
```

### 🔄 主仓分支同步规则

| 操作 | 原仓库分支 | 你的 Fork 分支 | 说明 |
|------|-----------|---------------|------|
| **拉取/同步** | ✅ `git fetch origin` | ✅ `git pull myfork` | 从原仓库获取最新代码 |
| **推送** | ❌ **禁止** | ✅ `git push myfork` | 只能推送到你的 Fork |
| **修改** | ❌ **禁止** | ✅ 自由修改 | 原仓库分支只读 |
| **创建分支** | ❌ **禁止** | ✅ `git checkout -b` | 在 Fork 上创建 |

### 完整的仓库关系图

```
原仓库 (xxsddm/multiDemo)          你的 Fork (YOUR-NAME/multiDemo)
    │                                      │
    ├── master (只读/同步)                 ├── master (可推送)
    │                                      │
    ├── develop (只读/同步) ◄─────────────┼── develop (可推送)
    │         ▲                          │         │
    │         │                          │         │
    │         └──────────────────────────┘         │
    │              定期同步                          │
    │                                              │
    │                                              ├── feature/xxx (你的开发分支)
    │                                              │
    │ ◄────────────────────────────────────────────┘
    │        PR #X: 请求合并
    │
    └── develop (合并后更新)
```

**关键理解**:
- `origin/develop` ←→ `myfork/develop`: 同步关系（只读→可写）
- `myfork/feature/xxx` → `origin/develop`: PR 关系（请求审查合并）

---

## 🚫 绝对禁止（红线！）

| 禁止行为 | 后果 | 检查方式 |
|----------|------|----------|
| 直接提交到 `master` | 立即回滚 | git log --oneline master |
| 直接推送代码到 `master` | 禁止推送 | Git 分支保护 |
| 未经审查合并 PR | 代码质量风险 | PR 审查流程 |
| 在他人分支上强制推送 | 覆盖他人工作 | git push --force |
| 提交敏感信息（密码/密钥） | 安全风险 | git-secrets 扫描 |

---

## 🌿 分支策略

### 分支模型（Git Flow 简化版）

```
master                 # 生产分支（保护分支，只接受PR）
  │
  ├── develop          # 开发分支（集成测试，基于feature分支）
  │     │
  │     ├── feature/xxx  # 功能分支（从这里开始开发）
  │     └── feature/yyy
  │
  ├── bugfix/xxx       # 修复分支
  ├── hotfix/xxx       # 紧急修复（可直接从master切）
  └── nouse/xxx        # 废弃分支（不再维护）
```

### 分支职责说明

| 分支 | 职责 | 来源 | 合并目标 |
|------|------|------|----------|
| `master` | 生产代码，稳定版本 | - | - |
| `develop` | 开发集成，功能测试 | feature/* | master（通过PR） |
| `feature/*` | 新功能开发 | develop | develop |
| `bugfix/*` | Bug修复 | develop | develop |
| `hotfix/*` | 紧急生产修复 | master | master + develop |

### 为什么使用 develop 分支？

1. **隔离开发**：develop 作为集成测试环境，不影响 master 稳定性
2. **并行开发**：多个 feature 分支可独立开发，完成后合并到 develop
3. **版本控制**：develop 可随时创建 release 分支，准备发布
4. **团队协作**：其他开发者基于 develop 创建分支，保持代码同步

### 分支命名规范

| 类型 | 命名格式 | 示例 |
|------|----------|------|
| 功能 | `feature/功能描述` | `feature/chart-generation` |
| 修复 | `bugfix/问题描述` | `bugfix/fix-chart-timeout` |
| 紧急修复 | `hotfix/问题描述` | `hotfix/fix-security-issue` |
| 废弃 | `nouse/原分支名` | `nouse/feature/old-thing` |

---

## 🔄 标准工作流程

### 1. 开始新功能（基于 develop）

```bash
# 1. 切换到 develop 分支并更新
git checkout develop
git pull origin develop

# 2. 创建功能分支（从 develop 切出）
git checkout -b feature/xxx develop

# 3. 开发...开发...开发

# 4. 及时提交（小步提交）
git add -A
git commit -m "feat: 添加图表生成功能"

# 5. 及时推送（备份）
git push -u origin feature/xxx
```

### 2. 同步 develop 更新（重要！）

当 develop 有更新时，需要同步到自己的 feature 分支：

```bash
# 1. 保存当前工作
git add -A
git commit -m "wip: 保存当前进度"  # 或 git stash

# 2. 更新 develop
git checkout develop
git pull origin develop

# 3. 切回 feature 分支并合并 develop
git checkout feature/xxx
git merge develop

# 4. 解决冲突（如果有）
# ... 解决冲突 ...
git add -A
git commit -m "merge: 同步 develop 更新"

# 5. 推送
git push
```

### 3. 完成开发，合并到 develop

```bash
# 1. 确保功能完成并通过测试
# 2. 创建 PR 到 develop（不是 master！）
gh pr create \
  --title "feat: 添加图表生成功能" \
  --base develop \
  --head feature/xxx

# 3. 等待代码审查通过
# 4. 合并 PR 到 develop
git checkout develop
git pull origin develop  # 获取合并后的代码

# 5. 删除已合并的 feature 分支
git branch -d feature/xxx
git push origin --delete feature/xxx
```

### 4. develop 合并到 master（发布时）

```bash
# 1. 确保 develop 稳定
git checkout develop
git pull origin develop

# 2. 创建 PR 到 master
gh pr create \
  --title "release: 发布 v1.x" \
  --base master \
  --head develop \
  --body "本次发布包含功能A、B、C"

# 3. 经过严格测试和审查后合并
# 4. 打标签
git checkout master
git pull origin master
git tag -a v1.2.0 -m "版本 1.2.0"
git push origin v1.2.0
```

---

## 🍴 Fork 工作流详细指南

### 为什么必须使用 Fork？

1. **安全隔离**: 你无法直接修改原仓库，所有变更必须通过 PR 审查
2. **自由开发**: 在自己的 fork 中可以任意推送分支、提交代码
3. **代码审查**: 所有代码在合并前必须经过 review
4. **协作规范**: 这是开源项目和团队协作的标准做法

### Fork 工作流步骤

#### 1. 初始设置（每个人只需做一次）

```bash
# Fork 原仓库到个人账户
git clone https://github.com/xxsddm/multiDemo.git
cd multiDemo

# Fork 到个人账户（使用 gh CLI 或 GitHub Web）
gh repo fork --remote --remote-name myfork

# 检查 remote 配置
git remote -v
# 应显示：
# myfork  https://github.com/YOUR-USERNAME/multiDemo.git (fetch/push)
# origin  https://github.com/xxsddm/multiDemo.git (fetch/push)
```

#### 2. 日常开发流程

```bash
# Step 1: 同步原仓库 develop 分支
git fetch origin
git checkout develop
git merge origin/develop
git push myfork develop

# Step 2: 基于 develop 创建功能分支
git checkout -b feature/my-feature develop

# Step 3: 开发并提交
git add -A
git commit -m "feat: 我的功能"

# Step 4: 推送到你的 fork
git push myfork feature/my-feature

# Step 5: 创建 PR 到原仓库的 develop 分支
gh pr create \
  --repo xxsddm/multiDemo \
  --base develop \
  --head YOUR-USERNAME:feature/my-feature \
  --title "feat: 我的功能描述"
```

#### 3. 保持 Fork 同步（重要！）

```bash
# 定期同步原仓库更新
git fetch origin
git checkout develop
git rebase origin/develop
git push myfork develop --force-with-lease

# 同步 feature 分支
git checkout feature/my-feature
git rebase develop
# 解决冲突...
git push myfork feature/my-feature --force-with-lease
```

#### 4. 完整的 PR 检查清单

创建 PR 前检查：
- [ ] 代码在本地测试通过
- [ ] 已从 develop 分支 rebase
- [ ] 所有冲突已解决
- [ ] 推送到自己的 fork
- [ ] PR 目标是 `xxsddm/multiDemo` 的 `develop` 分支
- [ ] PR 描述清晰，包含变更说明

### 🔀 多人协作流程图

```
原仓库 (xxsddm/multiDemo)
    │
    ├── develop ◄──────────────────────────────────┐
    │     │                                         │
    │     └── PR #1 (review & merge)                │
    │                                               │
开发者A的 Fork              开发者B的 Fork          │
    │                              │                │
    ├── feature/A ──PR─────────────┘                │
    │                                               │
    └── develop ◄───────────────────────────────────┘
           │
           └── feature/B ──PR──► (等待审查)
```

---

## 📝 提交信息规范

```
<type>(<scope>): <subject>

<body>

<footer>
```

**type 类型**：

| 类型 | 用途 | 示例 |
|------|------|------|
| `feat` | 新功能 | `feat: 添加GIF生成功能` |
| `fix` | 修复 | `fix: 修复图表超时问题` |
| `docs` | 文档 | `docs: 更新API文档` |
| `test` | 测试 | `test: 添加并行执行测试` |
| `refactor` | 重构 | `refactor: 优化图表代码结构` |
| `chore` | 杂项 | `chore: 更新依赖版本` |

**完整示例**：
```bash
git commit -m "feat(chart): 添加异步图表生成功能

- 使用 asyncio.Queue 实现真正的并行流式
- 支持配置化并发控制
- 添加自动清理机制

Closes #6"
```

### 5. 创建 PR（Pull Request）

**重要**：feature 分支的 PR 目标是 `develop`，不是 `master`！

```bash
# 1. 确保分支已推送
git push origin feature/xxx

# 2. 创建 PR 到 develop（推荐用 gh CLI）
gh pr create \
  --title "feat: 添加图表生成功能" \
  --body "## 变更内容
- 功能A
- 功能B

## 测试
- [x] 单元测试通过
- [x] 集成测试通过
- [x] E2E测试通过

## 截图
<截图>" \
  --base develop \
  --head feature/xxx
```

**PR 流程图**：
```
feature/xxx ──PR──> develop ──PR──> master
     │                    │
     └─ 开发测试          └─ 集成测试
```

**PR 描述模板**：

```markdown
## 📋 变更内容

### 新增
- 功能A描述
- 功能B描述

### 修复
- 问题C修复

### 优化
- 性能优化D

## 🧪 测试

- [ ] 单元测试通过
- [ ] 集成测试通过
- [ ] E2E测试通过
- [ ] 截图已确认

## 📸 截图

<如有UI变更，附截图>

## 🔗 关联

- Closes #6
- Related to #5
```

### 4. 代码审查（Code Review）

**审查清单**：

- [ ] 代码逻辑正确
- [ ] 符合项目编码规范
- [ ] 有适当的错误处理
- [ ] 有必要的注释
- [ ] 没有引入不必要的依赖
- [ ] 性能影响评估
- [ ] 安全风险检查

**审查命令**：

```bash
# 查看变更
git diff master..feature/xxx

# 统计变更行数
git diff --stat master..feature/xxx

# 检查敏感信息
git-secrets --scan
```

### 5. 合并 PR

**必须条件**：
- [ ] 所有审查者通过
- [ ] 所有测试通过
- [ ] 用户确认可以合并
- [ ] 无冲突

**合并方式**（推荐 Squash）：

```bash
# 使用 gh CLI 合并
gh pr merge --squash --delete-branch

# 或手动合并后删除分支
git checkout master
git merge --squash feature/xxx
git commit -m "feat: 添加图表生成功能"
git push origin master
git push origin --delete feature/xxx
```

---

## 🔄 多人协作

### 场景1：多人同时开发不同功能

```bash
# 开发者A
git checkout -b feature/A master
# 开发A功能...

# 开发者B
git checkout -b feature/B master
# 开发B功能...
```

**关键点**：各自独立分支，互不干扰

### 场景2：多人协作同一功能

```bash
# 主开发者创建分支
git checkout -b feature/shared master
git push -u origin feature/shared

# 协作者A
git fetch origin
git checkout -b feature/shared origin/feature/shared
# 开发...
git push

# 协作者B
git fetch origin
git checkout -b feature/shared origin/feature/shared
# 开发...
git push
```

**关键点**：
- 主分支 push 前必须先 pull
- 冲突解决后再次 push

### 场景3：解决冲突

```bash
# 1. 获取最新代码
git fetch origin

# 2. 尝试合并
git checkout feature/xxx
git merge origin/master

# 3. 如果有冲突，解决冲突
#    - 打开冲突文件
#    - 找到 <<<<<<< / ======= / >>>>>>> 标记
#    - 保留需要的代码
#    - 删除标记

# 4. 标记冲突已解决
git add -A
git commit -m "fix: 解决与master的冲突"

# 5. 推送
git push
```

---

## 🧹 分支清理

### 自动清理脚本

```bash
#!/bin/bash
# clean-branches.sh

echo "检查过期分支..."

# 获取所有远程分支
git fetch --prune

# 列出30天未更新的分支
git for-each-ref --sort=committerdate refs/remotes/origin/feature/ \
  --format='%(committerdate:short) %(refname:short)' |
  while read date branch; do
    if [[ $(date -d "$date" +%s) -lt $(date -d "30 days ago" +%s) ]]; then
      echo "过期分支: $branch (最后更新: $date)"
    fi
  done
```

### 手动清理流程

```bash
# 1. 列出本地分支
git branch

# 2. 删除已合并的分支
git branch -d feature/xxx

# 3. 删除远程分支
git push origin --delete feature/xxx

# 4. 重命名废弃分支
git branch -m feature/old nouse/feature/old
```

---

## 🆘 常见问题

### Q1: 不小心提交到了 master 怎么办？

**不要慌，立即回滚**：

```bash
# 1. 记录当前 commit（用于恢复）
git log --oneline -1
# 记录 hash: abc1234

# 2. 重置 master 到上次提交
git checkout master
git reset --hard HEAD~1  # 回滚1个提交

# 3. 强制推送（需要权限）
git push --force origin master

# 4. 创建 feature 分支重新提交
git checkout -b feature/xxx abc1234
git push -u origin feature/xxx
# 然后创建 PR
```

### Q2: 代码冲突太多，想放弃重新来

```bash
# 1. 保存当前工作（以防万一）
git checkout -b backup/feature/xxx

# 2. 回到 feature 分支
git checkout feature/xxx

# 3. 重置到 master（放弃所有变更）
git reset --hard origin/master

# 4. 重新开发（或者从 backup 分支 cherry-pick）
```

### Q3: 敏感信息提交到仓库了

**立即处理**：

```bash
# 1. 删除敏感文件
git rm .env

# 2. 修改密码/密钥（立即！）

# 3. 使用 git-filter-branch 清除历史
#    或 BFG Repo-Cleaner

# 4. 强制推送
git push --force
```

---

## ✅ 每日检查清单

每天工作前检查：

```bash
# 1. 本地 master 是否最新？
git checkout master
git pull origin master

# 2. 当前在哪个分支？
git branch --show-current

# 3. 是否有未提交的变更？
git status

# 4. 远程分支是否有更新？
git fetch origin
git log --oneline HEAD..origin/master
```

---

## 📊 质量指标

| 指标 | 目标 | 检查命令 |
|------|------|----------|
| 分支及时清理 | <10 个活跃分支 | `git branch -r | wc -l` |
| PR 平均大小 | <500 行 | `git diff --stat` |
| 合并前审查率 | 100% | PR 记录 |
| 冲突解决时间 | <30 分钟 | 人工统计 |

---

## 📖 PR 流程示例

### 示例：更新文档并提交 PR

**场景**: 你修改了 Git 工作流规范，需要提交到 develop

```bash
# 1. 确保本地 develop 是最新的
git checkout develop
git pull origin develop

# 2. 创建功能分支
git checkout -b feature/update-docs develop

# 3. 修改文件...
# vim skills/git-workflow-enhanced/SKILL.md

# 4. 提交更改
git add skills/git-workflow-enhanced/SKILL.md
git commit -m "docs: 添加PR流程示例"

# 5. 推送到你的 Fork
git push myfork feature/update-docs

# 6. 创建 PR 到原仓库的 develop 分支
gh pr create \
  --repo xxsddm/multiDemo \
  --base develop \
  --head YOUR-USERNAME:feature/update-docs \
  --title "docs: 添加PR流程示例" \
  --body "## 变更\n- 添加PR流程示例\n- 完善文档"

# 7. 等待代码审查通过后被合并
# 8. 合并后删除本地分支
git checkout develop
git pull origin develop
git branch -d feature/update-docs
```

---

## 🔗 相关文档

- [项目约束规范](./project-constraints/SKILL.md)
- [交接文档指南](./handover-doc-guide/SKILL.md)
- [Python 代码质量](./python-quality/SKILL.md)

---

> ⚠️ **记住**: Git 工作流规范是团队协作的基础，严格执行才能保证代码质量和开发效率！
