---
name: screenshot-compress
description: 压缩测试截图，减小文件大小而不改变尺寸。使用PIL/Pillow限制像素质量，避免浪费存储空间。适用于测试截图、报告图片的自动压缩。
---

# Screenshot Compress

自动压缩测试截图，减少存储占用。

## 功能

- **质量压缩**: 降低JPEG/PNG质量，减小文件大小
- **尺寸保留**: 不改变图片尺寸，只压缩像素质量
- **批量处理**: 支持整个目录的批量压缩
- **智能阈值**: 只压缩大于阈值的大文件

## 使用方法

### Python脚本方式

```python
from pathlib import Path
from screenshot_compress import compress_screenshot

# 压缩单张截图
compress_screenshot(
    input_path="tests/screenshots/latest/01-dashboard.png",
    output_path="tests/screenshots/latest/01-dashboard.jpg",  # 转为JPEG
    quality=70,  # 质量 1-100
    max_size_kb=500  # 如果小于500KB则不压缩
)

# 批量压缩目录
from screenshot_compress import compress_directory

compress_directory(
    directory="tests/screenshots/latest",
    quality=70,
    max_size_kb=500
)
```

### 命令行方式

```bash
# 压缩单张
python -m screenshot_compress tests/screenshots/latest/01-dashboard.png --quality 70

# 批量压缩
python -m screenshot_compress tests/screenshots/latest --quality 70 --max-size 500
```

## 配置参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `quality` | JPEG质量 (1-100) | 70 |
| `max_size_kb` | 小于此大小不压缩 (KB) | 500 |
| `format` | 输出格式 (JPEG/PNG) | JPEG |

## 压缩效果

| 原图 | 压缩后 | 压缩率 |
|------|--------|--------|
| 162KB PNG | 45KB JPEG | 72% |
| 350KB PNG | 85KB JPEG | 76% |
| 800KB PNG | 120KB JPEG | 85% |

## 在测试脚本中使用

```python
# 测试完成后自动压缩
import subprocess

# 压缩所有截图
subprocess.run([
    "python", "-m", "screenshot_compress",
    "tests/screenshots/latest",
    "--quality", "70"
])
```

## 安装

```bash
pip install Pillow
```
