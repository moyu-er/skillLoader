---
name: python-quality
description: Python 代码质量工具集合，包含 Ruff (lint + format)、Black (format)、MyPy (type check) 的配置和检查脚本。用于后端 Python 代码的质量保障和自动检查。
---

# Python Quality

Python 代码质量保障工具，集成主流 lint、format、type check 工具。

## 快速开始

### 安装工具

```bash
# 在项目根目录执行
pip install ruff black mypy

# 或使用 pipx
pipx install ruff black mypy
```

### 运行代码检查

```bash
# 完整质量检查
python skills/python-quality/scripts/check.py

# 仅检查 backend 目录
python skills/python-quality/scripts/check.py --target backend/

# 自动修复问题
python skills/python-quality/scripts/check.py --fix
```

## 工具说明

| 工具 | 用途 | 配置 |
|------|------|------|
| **Ruff** | 超快的 Python linter | `pyproject.toml` |
| **Black** | 代码格式化 | `pyproject.toml` |
| **MyPy** | 静态类型检查 | `pyproject.toml` |

## 配置文件

### pyproject.toml（已配置）

位于项目根目录，包含：

```toml
[tool.ruff]
target-version = "py311"
line-length = 100
select = ["E", "F", "I", "W", "UP", "B", "C4", "SIM"]
ignore = ["E501"]

[tool.black]
line-length = 100
target-version = ['py311']

[tool.mypy]
python_version = "3.11"
strict = true
warn_return_any = true
warn_unused_configs = true
```

## 检查规则

### Ruff 检查项
- **E** - pycodestyle 错误
- **F** - Pyflakes
- **I** - isort (import 排序)
- **W** - pycodestyle 警告
- **UP** - pyupgrade (Python 升级)
- **B** - flake8-bugbear
- **C4** - flake8-comprehensions
- **SIM** - flake8-simplify

### MyPy 严格模式
- 检查类型注解
- 检查返回值
- 检查未使用配置

## 集成到定时任务

在 `smart_project_push.sh` 中添加：

```bash
# Python 代码质量检查
echo "  Python 代码质量检查..." >> "$REPORT_FILE"
python skills/python-quality/scripts/check.py --target backend/ >> "$REPORT_FILE" 2>&1
```

## CI/CD 集成

```yaml
# .github/workflows/quality.yml
- name: Python Quality Check
  run: |
    pip install ruff black mypy
    ruff check backend/
    black --check backend/
    mypy backend/
```
