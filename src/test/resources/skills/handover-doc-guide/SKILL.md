---
name: handover-doc-guide
description: MultiDemo 项目交接文档编写指南。规范交接文档结构、角色分工、测试用例要求，确保项目可维护性和团队协作品质。
---

# 项目交接文档编写指南

> 本指南规范交接文档的结构和内容，确保：
> 1. 信息完整，便于后续开发者快速上手
> 2. 角色分工清晰，责任明确
> 3. 测试用例充分，质量保证
> 4. 环境搭建简单，开箱即用

---

## 📋 交接文档标准结构

```
HANDOVER.md
├── 项目概述
├── 本次变更/新增功能
├── 角色分工
│   ├── 开发角色
│   ├── 测试角色
│   └── 产品角色
├── 环境配置
├── 测试用例清单
├── Git 分支管理
├── 已知问题与限制
├── 回滚策略
└── 交接确认清单
```

---

## 🎯 各部分详细规范

### 1. 项目概述

**必须包含**：
- 项目名称、版本、日期、分支
- PR 链接（如果有）
- 核心功能状态表

**模板**：
```markdown
## 📋 项目概述

> **版本**: v1.x  
> **日期**: YYYY-MM-DD  
> **分支**: feature/xxx  
> **PR**: https://github.com/xxsddm/multiDemo/pull/X

### 核心功能状态

| 功能 | 说明 | 状态 |
|------|------|------|
| 功能A | 描述 | ✅ 稳定 |
| 功能B | 描述 | 🔄 开发中 |
```

---

### 2. 本次变更/新增功能

**必须包含**：
- 变更类型（新增/修复/优化）
- 技术实现细节
- 核心文件位置
- 配置项说明

**模板**：
```markdown
## 🎯 新增功能

### 1. 功能名称

**功能描述**: 一句话说明

**技术实现**:
```
核心文件:
- backend/app/xxx/xxx.py
- frontend/src/xxx.tsx

配置项:
- ENV_VAR=xxx (说明)
```

**使用示例**:
```python
# 代码示例
```
```

---

### 3. 角色分工（重点！）

**必须明确三大角色**：

#### 3.1 开发角色 (Developer)

**职责范围**：
- 代码实现
- 架构设计
- 代码审查
- 文档编写

**必须遵循的规范**：

| 规范项 | 要求 | 检查方式 |
|--------|------|----------|
| 分支管理 | 禁止直接合入 master，必须通过 PR | git log |
| 代码质量 | 符合 python-quality skill | 自动检查 |
| 测试覆盖 | 新功能必须有对应测试 | pytest |
| 文档同步 | 大改动必须更新文档 | 人工检查 |

**分支管理规范（强制）**：
```bash
# ✅ 正确做法
git checkout -b feature/xxx master
# 开发...提交...推送
git push -u origin feature/xxx
# 创建 PR（必须！）
gh pr create --base master --head feature/xxx

# ❌ 错误做法
git checkout master
git commit -m "xxx"  # 绝对禁止！
git push origin master  # 绝对禁止！
```

#### 3.2 测试角色 (QA)

**职责范围**：
- 测试用例编写
- 测试执行
- 缺陷跟踪
- 截图验证

**测试规范**：

| 测试类型 | 要求 | 输出 |
|----------|------|------|
| 单元测试 | 核心功能 >70% 覆盖 | pytest 报告 |
| 集成测试 | 模块间交互必须测试 | 测试脚本 |
| E2E 测试 | 真实浏览器操作 | Playwright 截图 |
| 性能测试 | 敏感功能需基准测试 | 性能报告 |

**截图要求**：
- 使用 Playwright 真实截图（禁止手动生成）
- 保存到 `test-screenshots/`
- **必须用户确认后才能标记通过**
- Git 忽略截图目录（.gitignore）

#### 3.3 产品角色 (Product)

**职责范围**：
- 需求对齐
- 功能验收
- 用户体验
- 优先级管理

**验收标准**：

| 检查项 | 标准 |
|--------|------|
| 需求对齐 | 实现与需求文档一致 |
| 用户体验 | 操作流程顺畅，提示友好 |
| 功能边界 | 明确功能边界，超出范围需确认 |
| P0 功能 | 必须完整可用 |

---

### 4. 环境配置（必须详细！）

**目标**：新人 5 分钟内可搭建环境

**模板**：
```markdown
## 🔧 环境配置

### 快速启动

```bash
# 1. 克隆项目
git clone <repo>
cd multiDemo

# 2. 后端
python3 -m venv backend/.venv
source backend/.venv/bin/activate
pip install -r backend/requirements.txt

# 3. 前端
cd frontend
npm install

# 4. 启动服务
# 终端1: 后端
cd backend && uvicorn app.main:app --reload --port 8000

# 终端2: 前端
cd frontend && npm run dev
```

### 环境变量

| 变量 | 说明 | 必需 |
|------|------|------|
| OPENAI_API_KEY | LLM API Key | ✅ |
| CHART_SANDBOX_TYPE | 沙箱类型(local/e2b/docker) | ❌ |

### 依赖检查

```bash
# Python
pip freeze | grep key-package

# Node
npm list key-package
```
```

---

### 5. 测试用例清单（重点！）

**必须按角色分类**：

```markdown
## 🧪 测试用例清单

### 开发角色测试

| 测试 | 命令 | 预期结果 |
|------|------|----------|
| 语法检查 | `python -m py_compile xxx.py` | 无错误 |
| 导入测试 | `python -c "from xxx import yyy"` | 成功 |
| 单元测试 | `pytest tests/test_xxx.py -v` | 全部通过 |

### 测试角色测试

| 测试 | 工具 | 验证方式 |
|------|------|----------|
| API 测试 | curl/httpx | 响应正确 |
| E2E 测试 | Playwright | 截图确认 |
| 性能测试 | pytest-benchmark | 基准通过 |

### 产品角色测试

| 测试 | 方法 | 验收标准 |
|------|------|----------|
| 功能验收 | 手工测试 | 需求对齐 |
| 用户体验 | 真实操作 | 流程顺畅 |
```

---

### 6. Git 分支管理（强制执行）

```markdown
## 🌿 Git 分支管理

### 核心规则（违反者直接回滚！）

1. **禁止直接修改 master**（绝对禁止！）
2. **所有工作在 feature 分支**
3. **必须通过 PR 合并**
4. **用户确认后才能合并**

### 分支生命周期

```
feature/xxx       → 开发分支（从这里开始）
  │
  ├── 开发 → 提交 → 推送
  │
  └── PR → 代码审查 → 用户确认 → 合并到 master
```

### 分支清理规范

- 检查频率：每周/每两周
- 清理对象：2周无活动的分支
- 处理方式：重命名为 `nouse/` 前缀
- 示例：`feature/old` → `nouse/feature/old`

### 提交信息规范

```
<type>: <subject>

<body>

type:
- feat: 新功能
- fix: 修复
- docs: 文档
- test: 测试
- refactor: 重构
```
```

---

### 7. 已知问题与限制

**必须诚实记录**：

```markdown
## ⚠️ 已知问题

| 问题 | 影响 | 解决方案 | 状态 |
|------|------|----------|------|
| 问题A | 描述 | 临时方案 | 🔄 跟进中 |
| 问题B | 描述 | 长期方案 | ⏸️ 延期 |
```

---

### 8. 回滚策略

```markdown
## 🔄 回滚策略

如果测试失败：
1. 记录错误信息
2. `git reset --soft HEAD~1` 回滚
3. 修复问题
4. 重新测试
```

---

### 9. 交接确认清单

**三方签字确认**：

```markdown
## ✅ 交接确认

### 开发确认
- [ ] 代码已提交到 feature 分支
- [ ] PR 已创建
- [ ] 关键文件位置已了解
- [ ] 所有测试通过

### 测试确认
- [ ] 测试用例已完善
- [ ] 所有测试通过
- [ ] 截图已确认

### 产品确认
- [ ] 当前功能状态已了解
- [ ] 新功能已验收
- [ ] 已知问题已确认

**交接完成日期**: YYYY-MM-DD  
**分支**: feature/xxx  
**PR链接**: xxx
```

---

## 📝 编写检查清单

提交交接文档前，检查：

- [ ] 项目概述完整（版本、日期、分支、PR）
- [ ] 新增功能有技术实现细节
- [ ] 角色分工明确（开发/测试/产品）
- [ ] 环境配置详细（5分钟可搭建）
- [ ] 测试用例充分（单元/集成/E2E）
- [ ] Git 分支管理规范清晰
- [ ] 已知问题诚实记录
- [ ] 回滚策略明确
- [ ] 三方确认清单完整

---

## 📎 示例参考

参考项目中的 `HANDOVER.md` 文件：
- `/Users/gyt/.openclaw/workspace/projects/multiDemo/HANDOVER.md`

---

## 🔍 质量检查

使用以下命令检查文档质量：

```bash
# 检查结构完整性
grep -E "^## " HANDOVER.md | wc -l  # 应 >= 9

# 检查角色分工
grep -E "开发角色|测试角色|产品角色" HANDOVER.md

# 检查测试用例
grep -E "测试用例|pytest|Playwright" HANDOVER.md

# 检查 Git 规范
grep -E "禁止直接|feature/|PR" HANDOVER.md
```

---

> 💡 **提示**: 交接文档是项目的"使用说明书"，写得好不好直接影响后续维护成本。请认真对待！
