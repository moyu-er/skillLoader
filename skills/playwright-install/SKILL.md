---
name: playwright-install
description: Playwright 安装指南。用于 multiDemo 项目端到端浏览器测试，支持 Chromium 自动化截图和前后端联调测试。
---

# Playwright 安装指南

> 用于 multiDemo 项目端到端浏览器测试

## 安装步骤

### 1. 激活虚拟环境

```bash
cd /Users/gyt/.openclaw/workspace/projects/multiDemo/backend
source .venv/bin/activate
```

### 2. 安装 Playwright Python 包

```bash
pip install playwright
```

### 3. 安装浏览器二进制文件

```bash
# 只安装 Chromium（推荐，测试用）
playwright install chromium

# 或安装所有浏览器（Chromium + Firefox + WebKit）
playwright install
```

### 4. 验证安装

```bash
python3 -c "from playwright.async_api import async_playwright; print('✅ Playwright 安装成功')"
```

## 使用示例

```python
from playwright.async_api import async_playwright
import asyncio

async def test_example():
    async with async_playwright() as p:
        browser = await p.chromium.launch()
        page = await browser.new_page()
        
        # 访问前端页面
        await page.goto("http://localhost:3000")
        
        # 截图
        await page.screenshot(path="tests/screenshots/latest/test.png")
        
        await browser.close()

asyncio.run(test_example())
```

## 核心测试文件

| 测试文件 | 说明 |
|---------|------|
| `tests/test_frontend_backend_integration.py` | 前后端联调测试（最重要） |
| `tests/test_session_strict.py` | Session MCP 测试 |
| `tests/test_agent_strict.py` | 多Agent测试（不刷新页面） |

## 运行测试

```bash
cd /Users/gyt/.openclaw/workspace/projects/multiDemo
source backend/.venv/bin/activate

# 核心测试 - 前后端联调
python3 tests/test_frontend_backend_integration.py
```

## 常见问题

### 问题1: `playwright` 命令找不到

**解决**: 确保在虚拟环境中安装

```bash
which python3  # 确认是虚拟环境的 python
pip install playwright
```

### 问题2: 浏览器下载失败

**解决**: 使用国内镜像

```bash
# 设置环境变量后重新安装
export PLAYWRIGHT_DOWNLOAD_HOST=https://playwright.azureedge.net
playwright install chromium
```

### 问题3: 缺少系统依赖

**解决**: macOS 安装依赖

```bash
# macOS 通常无需额外依赖
# 如遇到错误，尝试：
brew install --cask chromium
```

## 截图保存路径

```python
from pathlib import Path

SCREENSHOT_DIR = Path("tests/screenshots/latest")
SCREENSHOT_DIR.mkdir(parents=True, exist_ok=True)

# 测试前清理旧截图
for f in SCREENSHOT_DIR.glob("*.png"):
    f.unlink()

# 保存新截图
await page.screenshot(path=SCREENSHOT_DIR / "01-dashboard.png")
```

## 测试超时设置

```python
# 页面导航超时
await page.goto(url, timeout=30000)  # 30秒

# 元素点击超时
await page.click("button", timeout=10000)  # 10秒

# 整个测试超时
import asyncio
await asyncio.wait_for(test_function(), timeout=300)  # 5分钟
```
