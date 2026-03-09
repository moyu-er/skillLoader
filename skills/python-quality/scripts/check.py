#!/usr/bin/env python3
"""
Python 代码质量检查脚本
集成 Ruff、Black、MyPy 进行全面的代码质量检查
"""

import argparse
import subprocess
import sys
from pathlib import Path
from typing import List, Tuple

# 默认检查目录
DEFAULT_TARGETS = ["backend/"]


def run_command(cmd: List[str], description: str) -> Tuple[int, str, str]:
    """运行命令并返回结果"""
    print(f"\n🔍 {description}...")
    try:
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=60
        )
        return result.returncode, result.stdout, result.stderr
    except subprocess.TimeoutExpired:
        return -1, "", "Command timed out"
    except FileNotFoundError:
        return -1, "", f"Command not found: {cmd[0]}"


def check_ruff(targets: List[str], fix: bool = False) -> Tuple[bool, List[str]]:
    """使用 Ruff 检查代码"""
    cmd = ["ruff", "check"] + targets
    if fix:
        cmd.append("--fix")
    
    returncode, stdout, stderr = run_command(cmd, "Ruff Lint 检查")
    
    issues = []
    if stdout:
        lines = stdout.strip().split('\n')
        for line in lines:
            if line.strip() and not line.startswith('All checks passed'):
                issues.append(line)
    
    if stderr:
        issues.append(f"Error: {stderr}")
    
    if returncode == 0 and not issues:
        print("  ✅ Ruff 检查通过")
        return True, []
    else:
        print(f"  ⚠️  发现 {len(issues)} 个问题")
        return False, issues[:10]  # 只显示前10个


def check_black(targets: List[str], fix: bool = False) -> Tuple[bool, List[str]]:
    """使用 Black 检查代码格式"""
    cmd = ["black", "--check", "--diff"] + targets
    if fix:
        cmd = ["black"] + targets
    
    returncode, stdout, stderr = run_command(cmd, "Black 格式检查")
    
    issues = []
    if not fix and returncode != 0:
        if "would reformat" in stdout:
            lines = stdout.strip().split('\n')
            for line in lines:
                if "would reformat" in line:
                    issues.append(line.replace("would reformat", "需要格式化"))
    
    if stderr and "error" in stderr.lower():
        issues.append(f"Error: {stderr}")
    
    if returncode == 0 or (fix and returncode == 0):
        print("  ✅ Black 检查通过")
        return True, []
    else:
        print(f"  ⚠️  发现 {len(issues)} 个格式问题")
        return False, issues[:5]


def check_mypy(targets: List[str]) -> Tuple[bool, List[str]]:
    """使用 MyPy 检查类型"""
    cmd = ["mypy"] + targets
    
    returncode, stdout, stderr = run_command(cmd, "MyPy 类型检查")
    
    issues = []
    if stdout:
        lines = stdout.strip().split('\n')
        for line in lines:
            if line.strip() and not line.startswith('Success'):
                issues.append(line)
    
    if stderr:
        issues.append(f"Error: {stderr}")
    
    if returncode == 0:
        print("  ✅ MyPy 检查通过")
        return True, []
    else:
        print(f"  ⚠️  发现 {len(issues)} 个类型问题")
        return False, issues[:10]


def check_imports(targets: List[str], fix: bool = False) -> Tuple[bool, List[str]]:
    """使用 Ruff 检查 import 排序"""
    cmd = ["ruff", "check", "--select", "I"] + targets
    if fix:
        cmd.extend(["--fix"])
    
    returncode, stdout, stderr = run_command(cmd, "Import 排序检查")
    
    issues = []
    if stdout and "I001" in stdout:
        lines = stdout.strip().split('\n')
        for line in lines:
            if "I001" in line:
                issues.append(line)
    
    if returncode == 0:
        print("  ✅ Import 检查通过")
        return True, []
    else:
        print(f"  ⚠️  发现 {len(issues)} 个 import 问题")
        return False, issues[:5]


def generate_report(results: dict) -> str:
    """生成检查报告"""
    report = ["\n" + "="*50]
    report.append("📊 Python 代码质量检查报告")
    report.append("="*50)
    
    total_passed = 0
    total_failed = 0
    
    for check_name, (passed, issues) in results.items():
        status = "✅ 通过" if passed else "❌ 失败"
        report.append(f"\n{check_name}: {status}")
        
        if issues:
            report.append("  问题列表:")
            for issue in issues[:5]:  # 只显示前5个
                report.append(f"    - {issue}")
            if len(issues) > 5:
                report.append(f"    ... 还有 {len(issues) - 5} 个问题")
        
        if passed:
            total_passed += 1
        else:
            total_failed += 1
    
    report.append(f"\n{'='*50}")
    report.append(f"总计: {total_passed} 项通过, {total_failed} 项失败")
    report.append(f"质量评分: {int(total_passed / len(results) * 10)}/10")
    
    return '\n'.join(report)


def main():
    parser = argparse.ArgumentParser(description="Python 代码质量检查")
    parser.add_argument("--target", nargs="+", default=DEFAULT_TARGETS,
                       help="检查目标目录")
    parser.add_argument("--fix", action="store_true",
                       help="自动修复可修复的问题")
    parser.add_argument("--quiet", action="store_true",
                       help="安静模式，只输出结果")
    
    args = parser.parse_args()
    
    targets = args.target
    
    print(f"🐍 Python 代码质量检查")
    print(f"目标目录: {', '.join(targets)}")
    
    results = {}
    
    # 运行各项检查
    results["Ruff Lint"] = check_ruff(targets, fix=args.fix)
    results["Black Format"] = check_black(targets, fix=args.fix)
    results["Import Sort"] = check_imports(targets, fix=args.fix)
    results["MyPy Types"] = check_mypy(targets)
    
    # 生成报告
    report = generate_report(results)
    print(report)
    
    # 返回码
    all_passed = all(passed for passed, _ in results.values())
    sys.exit(0 if all_passed else 1)


if __name__ == "__main__":
    main()
