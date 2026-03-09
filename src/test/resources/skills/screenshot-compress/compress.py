#!/usr/bin/env python3
"""
截图压缩工具 - 减小测试截图文件大小
"""

import os
import sys
from pathlib import Path
from PIL import Image


def compress_screenshot(input_path, output_path=None, quality=70, max_size_kb=500):
    """
    压缩单张截图
    
    Args:
        input_path: 输入文件路径
        output_path: 输出文件路径 (默认覆盖原文件)
        quality: JPEG质量 (1-100)
        max_size_kb: 小于此大小不压缩 (KB)
    """
    input_path = Path(input_path)
    
    if not input_path.exists():
        print(f"❌ 文件不存在: {input_path}")
        return False
    
    # 检查文件大小
    file_size_kb = input_path.stat().st_size / 1024
    if file_size_kb < max_size_kb:
        print(f"⏭️  跳过 (小于{max_size_kb}KB): {input_path.name} ({file_size_kb:.1f}KB)")
        return True
    
    # 设置输出路径
    if output_path is None:
        output_path = input_path.with_suffix('.jpg')
    else:
        output_path = Path(output_path)
    
    try:
        # 打开图片
        with Image.open(input_path) as img:
            # 转换为RGB (去除alpha通道)
            if img.mode in ('RGBA', 'P'):
                img = img.convert('RGB')
            
            # 保存为JPEG
            img.save(output_path, 'JPEG', quality=quality, optimize=True)
        
        # 计算压缩率
        new_size_kb = output_path.stat().st_size / 1024
        ratio = (1 - new_size_kb / file_size_kb) * 100
        
        print(f"✅ {input_path.name}: {file_size_kb:.1f}KB → {new_size_kb:.1f}KB (压缩{ratio:.1f}%)")
        
        # 如果输出路径不同，删除原文件
        if output_path != input_path:
            input_path.unlink()
        
        return True
        
    except Exception as e:
        print(f"❌ 压缩失败 {input_path.name}: {e}")
        return False


def compress_directory(directory, quality=70, max_size_kb=500):
    """
    批量压缩目录中的截图
    
    Args:
        directory: 目录路径
        quality: JPEG质量
        max_size_kb: 小于此大小不压缩 (KB)
    """
    directory = Path(directory)
    
    if not directory.exists():
        print(f"❌ 目录不存在: {directory}")
        return
    
    # 获取所有图片文件
    image_files = list(directory.glob("*.png")) + list(directory.glob("*.jpg"))
    
    if not image_files:
        print(f"⏭️  目录中没有图片: {directory}")
        return
    
    print(f"\n🖼️  找到 {len(image_files)} 张图片，开始压缩...")
    print(f"   质量: {quality}, 阈值: {max_size_kb}KB\n")
    
    success_count = 0
    total_saved = 0
    
    for img_path in sorted(image_files):
        original_size = img_path.stat().st_size / 1024
        
        if original_size < max_size_kb:
            print(f"⏭️  跳过 (小于{max_size_kb}KB): {img_path.name} ({original_size:.1f}KB)")
            continue
        
        if compress_screenshot(img_path, quality=quality, max_size_kb=0):  # max_size_kb=0 确保压缩
            output_path = img_path.with_suffix('.jpg')
            if output_path.exists():
                new_size = output_path.stat().st_size / 1024
                total_saved += original_size - new_size
                success_count += 1
            else:
                print(f"⚠️  输出文件不存在: {output_path}")
    
    print(f"\n📊 完成: {success_count}/{len(image_files)} 张, 节省: {total_saved:.1f}KB")


def main():
    """命令行入口"""
    import argparse
    
    parser = argparse.ArgumentParser(description='压缩测试截图')
    parser.add_argument('path', help='文件或目录路径')
    parser.add_argument('--quality', '-q', type=int, default=70, help='JPEG质量 (1-100)')
    parser.add_argument('--max-size', '-m', type=float, default=500, help='压缩阈值 (KB)')
    parser.add_argument('--output', '-o', help='输出路径 (仅单文件)')
    
    args = parser.parse_args()
    
    path = Path(args.path)
    
    if path.is_file():
        compress_screenshot(path, args.output, args.quality, args.max_size)
    elif path.is_dir():
        compress_directory(path, args.quality, args.max_size)
    else:
        print(f"❌ 路径不存在: {path}")


if __name__ == "__main__":
    main()
