#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Omni-Message 项目构建配置修复工具
针对 Android 10+ (minSdk 29) 和 JDK 17 优化
支持 Windows/macOS/Linux 多平台
"""

import os
import re
import sys
import stat
import json
import shutil
import platform
import subprocess
import warnings
import argparse
from pathlib import Path
from typing import Dict, List, Optional, Any, Tuple
from datetime import datetime

# 禁用特定警告
warnings.filterwarnings("ignore", category=SyntaxWarning)

class EnvironmentDetector:
    """环境检测器 - 自动检测开发环境配置"""
    
    def __init__(self):
        self.system = platform.system()
        self.arch = platform.machine()
        self.env_config = self._detect_environment()
    
    def _detect_environment(self) -> Dict[str, Any]:
        """检测开发环境"""
        config = {
            "system": self.system,
            "arch": self.arch,
            "java": self._detect_java(),
            "android_sdk": self._detect_android_sdk(),
            "android_studio": self._detect_android_studio(),
            "gradle": self._detect_gradle(),
            "memory": self._get_system_memory(),
            "cpu_cores": os.cpu_count()
        }
        return config
    
    def _detect_java(self) -> Dict[str, Any]:
        """检测Java安装"""
        java_config = {
            "installed": False,
            "version": "",
            "home": "",
            "is_jdk_17": False
        }
        
        # 检查常见的JDK安装路径
        common_paths = []
        
        if self.system == "Windows":
            # Windows 常见路径
            common_paths = [
                r"D:\Program Files\Java\jdk-17",
                r"D:\Program Files\Java\jdk-17.0.0",
                r"C:\Program Files\Java\jdk-17",
                r"C:\Program Files\Java\jdk-17.0.0",
                r"D:\Java\jdk-17",
                os.environ.get("JAVA_HOME", ""),
                r"C:\Program Files\Android\Android Studio\jbr"
            ]
        elif self.system == "Darwin":  # macOS
            common_paths = [
                "/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home",
                "/Library/Java/JavaVirtualMachines/jdk-17.0.0.jdk/Contents/Home",
                "/usr/local/opt/openjdk@17",
                os.environ.get("JAVA_HOME", ""),
                "/Applications/Android Studio.app/Contents/jbr/Contents/Home"
            ]
        else:  # Linux
            common_paths = [
                "/usr/lib/jvm/jdk-17",
                "/usr/lib/jvm/java-17-openjdk",
                "/opt/jdk-17",
                os.environ.get("JAVA_HOME", ""),
                "/usr/local/android-studio/jbr"
            ]
        
        # 检查路径是否存在
        for path in common_paths:
            if path and Path(path).exists():
                java_bin = Path(path) / "bin" / ("java.exe" if self.system == "Windows" else "java")
                if java_bin.exists():
                    try:
                        result = subprocess.run(
                            [str(java_bin), "-version"],
                            capture_output=True,
                            text=True,
                            encoding='utf-8'
                        )
                        if result.returncode == 0:
                            for line in result.stderr.split('\n'):
                                if "version" in line.lower():
                                    java_config["installed"] = True
                                    java_config["version"] = line.strip()
                                    java_config["home"] = path
                                    java_config["is_jdk_17"] = "17" in line
                                    return java_config
                    except:
                        continue
        
        return java_config
    
    def _detect_android_sdk(self) -> Dict[str, Any]:
        """检测Android SDK"""
        sdk_config = {
            "installed": False,
            "path": "",
            "platforms": [],
            "build_tools": []
        }
        
        # 检查常见的SDK路径
        common_paths = []
        
        if self.system == "Windows":
            common_paths = [
                r"D:\Program Files\Android\sdk",
                os.environ.get("ANDROID_HOME", ""),
                os.environ.get("ANDROID_SDK_ROOT", ""),
                r"C:\Users\{}\AppData\Local\Android\Sdk".format(os.environ.get("USERNAME", "")),
                r"C:\Android\Sdk"
            ]
        elif self.system == "Darwin":  # macOS
            common_paths = [
                "/Users/{}/Library/Android/sdk".format(os.environ.get("USER", "")),
                os.environ.get("ANDROID_HOME", ""),
                os.environ.get("ANDROID_SDK_ROOT", ""),
                "/usr/local/share/android-sdk"
            ]
        else:  # Linux
            common_paths = [
                "/home/{}/Android/Sdk".format(os.environ.get("USER", "")),
                os.environ.get("ANDROID_HOME", ""),
                os.environ.get("ANDROID_SDK_ROOT", ""),
                "/usr/lib/android-sdk"
            ]
        
        for path in common_paths:
            if path and Path(path).exists():
                platforms_dir = Path(path) / "platforms"
                build_tools_dir = Path(path) / "build-tools"
                
                if platforms_dir.exists():
                    sdk_config["installed"] = True
                    sdk_config["path"] = path
                    
                    # 获取已安装的平台
                    if platforms_dir.exists():
                        sdk_config["platforms"] = [d.name for d in platforms_dir.iterdir() if d.is_dir()]
                    
                    # 获取已安装的构建工具
                    if build_tools_dir.exists():
                        sdk_config["build_tools"] = [d.name for d in build_tools_dir.iterdir() if d.is_dir()]
                    
                    return sdk_config
        
        return sdk_config
    
    def _detect_android_studio(self) -> Dict[str, Any]:
        """检测Android Studio"""
        studio_config = {
            "installed": False,
            "path": "",
            "version": ""
        }
        
        if self.system == "Windows":
            studio_paths = [
                r"D:\Program Files\Android\Android Studio",
                r"C:\Program Files\Android\Android Studio",
                r"C:\Program Files\JetBrains\Android Studio"
            ]
        elif self.system == "Darwin":
            studio_paths = [
                "/Applications/Android Studio.app"
            ]
        else:  # Linux
            studio_paths = [
                "/usr/local/android-studio",
                "/opt/android-studio",
                "/snap/android-studio/current"
            ]
        
        for path in studio_paths:
            if Path(path).exists():
                studio_config["installed"] = True
                studio_config["path"] = path
                
                # 尝试获取版本
                if self.system == "Windows":
                    idea_props = Path(path) / "bin" / "idea.properties"
                elif self.system == "Darwin":
                    idea_props = Path(path) / "Contents" / "bin" / "idea.properties"
                else:
                    idea_props = Path(path) / "bin" / "idea.properties"
                
                if idea_props.exists():
                    with open(idea_props, 'r', encoding='utf-8') as f:
                        content = f.read()
                        version_match = re.search(r'version=(\S+)', content)
                        if version_match:
                            studio_config["version"] = version_match.group(1)
                
                return studio_config
        
        return studio_config
    
    def _detect_gradle(self) -> Dict[str, Any]:
        """检测Gradle"""
        gradle_config = {
            "installed": False,
            "version": "",
            "wrapper_exists": False
        }
        
        # 检查项目根目录是否有gradlew
        if Path("gradlew").exists() or Path("gradlew.bat").exists():
            gradle_config["wrapper_exists"] = True
            
            try:
                cmd = "./gradlew --version" if self.system != "Windows" else "gradlew --version"
                result = subprocess.run(
                    cmd,
                    shell=True,
                    capture_output=True,
                    text=True,
                    encoding='utf-8'
                )
                if result.returncode == 0:
                    for line in result.stdout.split('\n'):
                        if "Gradle" in line and "version" in line.lower():
                            gradle_config["installed"] = True
                            gradle_config["version"] = line.strip()
                            break
            except:
                pass
        
        return gradle_config
    
    def _get_system_memory(self) -> Dict[str, int]:
        """获取系统内存信息"""
        memory = {
            "total_mb": 0,
            "available_mb": 0
        }
        
        try:
            if self.system == "Windows":
                import ctypes
                kernel32 = ctypes.windll.kernel32
                ctypes.windll.kernel32.GetPhysicallyInstalledSystemMemory.restype = ctypes.c_ulonglong
                mem_kb = ctypes.windll.kernel32.GetPhysicallyInstalledSystemMemory()
                memory["total_mb"] = mem_kb // 1024
                memory["available_mb"] = memory["total_mb"]  # 简化处理
            elif self.system == "Darwin":
                import subprocess
                output = subprocess.check_output(["sysctl", "-n", "hw.memsize"]).decode().strip()
                memory["total_mb"] = int(output) // 1024 // 1024
                # 获取可用内存
                vm_stat = subprocess.check_output(["vm_stat"]).decode()
                pages_free = int(re.search(r'Pages free:\s+(\d+)', vm_stat).group(1))
                page_size = int(re.search(r'page size of (\d+) bytes', vm_stat).group(1))
                memory["available_mb"] = (pages_free * page_size) // 1024 // 1024
            else:  # Linux
                with open('/proc/meminfo', 'r') as f:
                    meminfo = f.read()
                    total = int(re.search(r'MemTotal:\s+(\d+) kB', meminfo).group(1))
                    available = int(re.search(r'MemAvailable:\s+(\d+) kB', meminfo).group(1))
                    memory["total_mb"] = total // 1024
                    memory["available_mb"] = available // 1024
        except:
            # 默认值
            memory["total_mb"] = 8192
            memory["available_mb"] = 4096
        
        return memory
    
    def get_environment_report(self) -> str:
        """获取环境检测报告"""
        report = []
        report.append("=" * 70)
        report.append("🔍 环境检测报告")
        report.append("=" * 70)
        report.append(f"操作系统: {self.env_config['system']} ({self.env_config['arch']})")
        report.append(f"CPU核心数: {self.env_config['cpu_cores']}")
        report.append(f"系统内存: {self.env_config['memory']['total_mb']}MB (可用: {self.env_config['memory']['available_mb']}MB)")
        report.append("")
        
        # Java 信息
        java = self.env_config['java']
        report.append("Java 环境:")
        if java['installed']:
            report.append(f"  ✅ 已安装: {java['version']}")
            report.append(f"  路径: {java['home']}")
            if java['is_jdk_17']:
                report.append("  ✅ JDK 17 符合要求")
            else:
                report.append(f"  ⚠️  当前版本不是 JDK 17，建议使用 JDK 17")
        else:
            report.append("  ❌ 未检测到 Java 安装")
        
        # Android SDK 信息
        sdk = self.env_config['android_sdk']
        report.append("\nAndroid SDK:")
        if sdk['installed']:
            report.append(f"  ✅ 已安装，路径: {sdk['path']}")
            if sdk['platforms']:
                report.append(f"  已安装平台: {', '.join(sorted(sdk['platforms'])[-5:])}")
            if sdk['build_tools']:
                report.append(f"  已安装构建工具: {', '.join(sorted(sdk['build_tools'])[-5:])}")
        else:
            report.append("  ❌ 未检测到 Android SDK")
        
        # Android Studio 信息
        studio = self.env_config['android_studio']
        report.append("\nAndroid Studio:")
        if studio['installed']:
            report.append(f"  ✅ 已安装，路径: {studio['path']}")
            if studio['version']:
                report.append(f"  版本: {studio['version']}")
        else:
            report.append("  ⚠️  未检测到 Android Studio")
        
        # Gradle 信息
        gradle = self.env_config['gradle']
        report.append("\nGradle:")
        if gradle['wrapper_exists']:
            report.append("  ✅ Gradle Wrapper 存在")
            if gradle['installed']:
                report.append(f"  版本: {gradle['version']}")
        else:
            report.append("  ⚠️  未检测到 Gradle Wrapper")
        
        report.append("=" * 70)
        return '\n'.join(report)
    
    def check_environment_requirements(self) -> Tuple[bool, List[str]]:
        """检查环境是否满足要求"""
        issues = []
        all_ok = True
        
        # 检查 Java
        java = self.env_config['java']
        if not java['installed']:
            issues.append("❌ Java 未安装")
            all_ok = False
        elif not java['is_jdk_17']:
            issues.append("⚠️  Java 不是 JDK 17")
            # 这里不标记为失败，只是警告
        
        # 检查 Android SDK
        sdk = self.env_config['android_sdk']
        if not sdk['installed']:
            issues.append("❌ Android SDK 未安装")
            all_ok = False
        
        # 检查内存
        memory = self.env_config['memory']
        if memory['total_mb'] < 4096:
            issues.append(f"⚠️  系统内存较低 ({memory['total_mb']}MB)，建议至少 8GB")
        
        return all_ok, issues

class BackupManager:
    """备份管理器 - 提供文件备份和恢复功能"""
    
    def __init__(self, project_root: Path):
        self.project_root = project_root
        self.backup_dir = project_root / ".config_backup"
        self.backup_dir.mkdir(exist_ok=True)
        
        # 创建备份记录文件
        self.backup_log = self.backup_dir / "backup_log.json"
        if not self.backup_log.exists():
            self.backup_log.write_text("[]")
    
    def backup_file(self, file_path: Path, reason: str = "修改前备份") -> Optional[Path]:
        """备份文件"""
        if not file_path.exists():
            return None
        
        # 创建备份文件名（带时间戳）
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        relative_path = file_path.relative_to(self.project_root)
        backup_name = f"{relative_path.name}.backup.{timestamp}"
        
        # 在备份目录中保持相同的目录结构
        backup_path = self.backup_dir / relative_path.parent / backup_name
        backup_path.parent.mkdir(parents=True, exist_ok=True)
        
        # 复制文件
        shutil.copy2(file_path, backup_path)
        
        # 记录备份
        self._log_backup(file_path, backup_path, reason)
        
        return backup_path
    
    def _log_backup(self, original: Path, backup: Path, reason: str):
        """记录备份信息"""
        log_data = json.loads(self.backup_log.read_text())
        log_entry = {
            "timestamp": datetime.now().isoformat(),
            "original": str(original.relative_to(self.project_root)),
            "backup": str(backup.relative_to(self.project_root)),
            "reason": reason,
            "size": original.stat().st_size
        }
        log_data.append(log_entry)
        self.backup_log.write_text(json.dumps(log_data, indent=2, ensure_ascii=False))
    
    def restore_backup(self, backup_path: Path, target_path: Optional[Path] = None) -> bool:
        """恢复备份文件"""
        if not backup_path.exists():
            return False
        
        if target_path is None:
            # 从备份文件名中提取原始文件名
            original_name = backup_path.name.split('.backup.')[0]
            target_path = backup_path.parent.parent / original_name
        
        # 确保目标目录存在
        target_path.parent.mkdir(parents=True, exist_ok=True)
        
        # 恢复文件
        shutil.copy2(backup_path, target_path)
        
        print(f"✅ 已恢复文件: {target_path.relative_to(self.project_root)}")
        return True
    
    def list_backups(self) -> List[Dict]:
        """列出所有备份"""
        if self.backup_log.exists():
            return json.loads(self.backup_log.read_text())
        return []
    
    def cleanup_old_backups(self, keep_days: int = 7):
        """清理旧的备份文件"""
        cutoff_time = datetime.now().timestamp() - (keep_days * 24 * 60 * 60)
        
        for backup_file in self.backup_dir.rglob("*.backup.*"):
            if backup_file.stat().st_mtime < cutoff_time:
                backup_file.unlink()
        
        # 清理空目录
        for dir_path in sorted(self.backup_dir.rglob("*/"), key=lambda x: len(x.parts), reverse=True):
            try:
                if not any(dir_path.iterdir()):
                    dir_path.rmdir()
            except:
                pass
        
        print(f"🧹 已清理超过 {keep_days} 天的旧备份")

class ProjectConfigManager:
    """项目配置管理器 - 针对 Android 10+ 和 JDK 17 优化"""
    
    def __init__(self, project_root: str = "."):
        self.project_root = Path(project_root).resolve()
        self.backup_manager = BackupManager(self.project_root)
        self.env_detector = EnvironmentDetector()
        self.configs = self._load_configs()
        
        # 保存环境配置
        self.env_config_file = self.project_root / ".environment_config.json"
        self._save_environment_config()
    
    def _save_environment_config(self):
        """保存环境配置"""
        env_data = {
            "detected_at": datetime.now().isoformat(),
            "environment": self.env_detector.env_config,
            "project_config": self.configs
        }
        self.env_config_file.write_text(json.dumps(env_data, indent=2, ensure_ascii=False))
    
    def _load_configs(self) -> Dict[str, Any]:
        """加载项目配置 - 针对 Android 10+ 和 JDK 17 优化"""
        env = self.env_detector.env_config
        
        # 根据环境自动调整配置
        memory_mb = env['memory']['total_mb']
        gradle_memory = "4096m" if memory_mb >= 8192 else "2048m" if memory_mb >= 4096 else "1024m"
        
        return {
            # Android 版本配置 - 针对 Android 10+ (API 29+)
            "android": {
                "compileSdk": 34,  # Android 14
                "minSdk": 29,      # Android 10 (不再向下兼容)
                "targetSdk": 34,   # Android 14
                "jvmTarget": "17", # 使用 JDK 17
                "kotlinCompilerExtensionVersion": "1.5.8"
            },
            
            # Gradle 配置
            "gradle": {
                "version": "8.12",
                "agpVersion": "8.3.0",  # 更新到最新稳定版
                "kotlinVersion": "1.9.22",  # 最新 Kotlin 版本
                "memory": gradle_memory
            },
            
            # 依赖版本配置 - 更新到最新稳定版
            "dependencies": {
                "androidx": {
                    "core": "1.12.0",
                    "lifecycle": "2.8.0",      # 更新到最新版
                    "activity": "1.8.2",       # 更新到最新版
                    "navigation": "2.7.6"      # 更新到最新版
                },
                "compose": {
                    "bom": "2024.02.01",       # 更新到最新版
                    "compiler": "1.5.8"
                },
                "hilt": "2.50",                # 更新到最新版
                "room": "2.6.1",               # 更新到最新版
                "retrofit": "2.9.0",
                "okhttp": "4.12.0",
                "coroutines": "1.7.3"
            },
            
            # 系统配置
            "system": {
                "os": env['system'],
                "java_home": env['java']['home'] or r"D:\Program Files\Java\jdk-17",
                "android_sdk": env['android_sdk']['path'] or r"D:\Program Files\Android\sdk",
                "android_studio": env['android_studio']['path'] or r"D:\Program Files\Android\Android Studio"
            }
        }
    
    def analyze_project_structure(self) -> Dict[str, Any]:
        """分析项目结构"""
        print("🔍 分析项目结构...")
        
        structure = {
            "settings": [],
            "modules": {},
            "problems": [],
            "build_files": [],
            "empty_files": [],
            "missing_files": []
        }
        
        # 检查 settings.gradle.kts
        settings_file = self.project_root / "settings.gradle.kts"
        if settings_file.exists():
            try:
                content = settings_file.read_text(encoding='utf-8')
                # 提取所有包含的模块
                includes = re.findall(r'include\("([^"]+)"\)', content)
                structure["settings"] = includes
                
                for include in includes:
                    # 转换为目录路径
                    if include.startswith(":"):
                        include = include[1:]
                    path = include.replace(":", "/")
                    module_dir = self.project_root / path
                    
                    # 检查模块目录是否存在
                    if not module_dir.exists():
                        problem = f"模块目录不存在: {path}"
                        structure["problems"].append(problem)
                        structure["missing_files"].append(f"{path}/build.gradle.kts")
                        continue
                    
                    # 检查构建文件
                    build_file = module_dir / "build.gradle.kts"
                    structure["build_files"].append(str(build_file.relative_to(self.project_root)))
                    
                    if not build_file.exists():
                        problem = f"构建文件不存在: {path}/build.gradle.kts"
                        structure["problems"].append(problem)
                        structure["missing_files"].append(f"{path}/build.gradle.kts")
                    elif build_file.stat().st_size < 100:
                        problem = f"构建文件为空或太小: {path}/build.gradle.kts"
                        structure["problems"].append(problem)
                        structure["empty_files"].append(f"{path}/build.gradle.kts")
                    
                    # 记录模块信息
                    structure["modules"][include] = {
                        "path": path,
                        "dir_exists": module_dir.exists(),
                        "build_file_exists": build_file.exists(),
                        "build_file_size": build_file.stat().st_size if build_file.exists() else 0,
                        "type": self._guess_module_type(include)
                    }
            except Exception as e:
                structure["problems"].append(f"读取settings.gradle.kts失败: {e}")
        else:
            structure["problems"].append("settings.gradle.kts文件不存在")
        
        # 检查根目录构建文件
        root_build_file = self.project_root / "build.gradle.kts"
        if not root_build_file.exists():
            structure["problems"].append("根目录build.gradle.kts不存在")
            structure["missing_files"].append("build.gradle.kts")
        elif root_build_file.stat().st_size < 50:
            structure["problems"].append("根目录build.gradle.kts为空或太小")
            structure["empty_files"].append("build.gradle.kts")
        
        # 检查gradle wrapper
        if self.env_detector.system == "Windows":
            gradlew_file = self.project_root / "gradlew.bat"
        else:
            gradlew_file = self.project_root / "gradlew"
        
        if not gradlew_file.exists():
            structure["problems"].append("Gradle Wrapper不存在")
        
        return structure
    
    def _guess_module_type(self, module_name: str) -> str:
        """猜测模块类型"""
        if module_name == "app":
            return "application"
        elif module_name == "core":
            return "core"
        elif module_name == "shared":
            return "shared"
        elif module_name.startswith("feature:"):
            return "feature"
        elif module_name.startswith("extension:"):
            return "extension"
        elif module_name.startswith("lib:") or module_name.startswith("library:"):
            return "library"
        else:
            return "unknown"
    
    def check_and_install_dependencies(self):
        """检查并安装Python依赖"""
        print("🔧 检查Python依赖...")
        
        required_packages = {
            "colorama": "colorama>=0.4.6",
            "requests": "requests>=2.31.0",
            "psutil": "psutil>=5.9.6"
        }
        
        missing_packages = []
        
        for package in required_packages:
            try:
                __import__(package)
                print(f"  ✅ {package} 已安装")
            except ImportError:
                missing_packages.append(required_packages[package])
                print(f"  ⚠️  {package} 未安装")
        
        if missing_packages:
            print(f"\n📦 安装缺失的依赖...")
            try:
                for package_spec in missing_packages:
                    print(f"  安装 {package_spec}")
                    subprocess.check_call([sys.executable, "-m", "pip", "install", package_spec])
                print("✅ 依赖安装完成")
            except subprocess.CalledProcessError as e:
                print(f"❌ 依赖安装失败: {e}")
                print("请手动安装:")
                for package_spec in missing_packages:
                    print(f"  pip install {package_spec}")
        else:
            print("✅ 所有依赖已安装")
    
    def fix_root_build_config(self):
        """修复根目录的build.gradle.kts"""
        print("🔧 修复根目录构建配置...")
        
        gradle_config = self.configs["gradle"]
        system_config = self.configs["system"]
        
        content = f'''// Top-level build file where you can add configuration options common to all sub-projects/modules.
// 针对 Android 10+ 和 JDK 17 优化配置
// 自动生成时间: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
// 修复内容: 使用推荐的 rootProject.layout.buildDirectory 替换已弃用的 rootProject.buildDir

plugins {{
    id("com.android.application") version "{gradle_config["agpVersion"]}" apply false
    id("com.android.library") version "{gradle_config["agpVersion"]}" apply false
    id("org.jetbrains.kotlin.android") version "{gradle_config["kotlinVersion"]}" apply false
    id("com.google.dagger.hilt.android") version "{self.configs["dependencies"]["hilt"]}" apply false
    id("org.jetbrains.kotlin.kapt") version "{gradle_config["kotlinVersion"]}" apply false
    id("com.google.devtools.ksp") version "{gradle_config["kotlinVersion"]}-1.0.19" apply false
}}

// 清理任务
tasks.register("clean", Delete::class) {{
    delete(rootProject.layout.buildDirectory)
}}
'''
        
        build_file = self.project_root / "build.gradle.kts"
        
        # 备份原文件
        backup_path = self.backup_manager.backup_file(build_file, "修复根构建配置")
        if backup_path:
            print(f"📁 已备份原文件: {backup_path.name}")
        
        build_file.write_text(content)
        print("✅ 已更新根目录构建配置")
    
    def create_unified_dependencies_file(self):
        """创建统一的依赖管理文件"""
        print("📦 创建统一的依赖管理文件...")
        
        deps_file = self.project_root / "dependencies.gradle.kts"
        
        # 备份原文件
        backup_path = self.backup_manager.backup_file(deps_file, "创建统一依赖管理")
        
        content = f'''// 统一依赖管理文件
// 针对 Android 10+ 和 JDK 17 优化
// 自动生成时间: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}

object Versions {{
    // Android - 针对 Android 10+ (minSdk 29)
    const val compileSdk = 34
    const val minSdk = 29      // Android 10，不再向下兼容
    const val targetSdk = 34
    const val jvmTarget = "17" // JDK 17
    const val kotlinCompilerExtensionVersion = "1.5.8"
    
    // Libraries - 使用最新稳定版本
    const val kotlin = "1.9.22"
    const val coroutines = "1.7.3"
    const val coreKtx = "1.12.0"
    const val lifecycle = "2.8.0"
    const val activity = "1.8.2"
    const val navigation = "2.7.6"
    const val hilt = "2.50"
    const val room = "2.6.1"
    const val retrofit = "2.9.0"
    const val okhttp = "4.12.0"
    const val composeBom = "2024.02.01"
    
    // Test
    const val junit = "4.13.2"
    const val testExtJunit = "1.1.5"
    const val espresso = "3.5.1"
}}

object Libraries {{
    // Kotlin
    const val kotlinStdlib = "org.jetbrains.kotlin:kotlin-stdlib:${{Versions.kotlin}}"
    const val coroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${{Versions.coroutines}}"
    const val coroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${{Versions.coroutines}}"
    const val coroutinesTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${{Versions.coroutines}}"
    
    // AndroidX
    const val coreKtx = "androidx.core:core-ktx:${{Versions.coreKtx}}"
    const val lifecycleRuntime = "androidx.lifecycle:lifecycle-runtime-ktx:${{Versions.lifecycle}}"
    const val lifecycleViewModel = "androidx.lifecycle:lifecycle-viewmodel-ktx:${{Versions.lifecycle}}"
    const val lifecycleRuntimeCompose = "androidx.lifecycle:lifecycle-runtime-compose:${{Versions.lifecycle}}"
    const val activityCompose = "androidx.activity:activity-compose:${{Versions.activity}}"
    const val navigationCompose = "androidx.navigation:navigation-compose:${{Versions.navigation}}"
    
    // Compose
    const val composeBom = "androidx.compose:compose-bom:${{Versions.composeBom}}"
    const val composeUi = "androidx.compose.ui:ui"
    const val composeGraphics = "androidx.compose.ui:ui-graphics"
    const val composeToolingPreview = "androidx.compose.ui:ui-tooling-preview"
    const val composeMaterial3 = "androidx.compose.material3:material3"
    const val composeMaterialIcons = "androidx.compose.material:material-icons-extended"
    const val composeUiTooling = "androidx.compose.ui:ui-tooling"
    const val composeUiTestManifest = "androidx.compose.ui:ui-test-manifest"
    const val composeHiltNavigation = "androidx.hilt:hilt-navigation-compose:1.2.0"
    
    // Hilt
    const val hiltAndroid = "com.google.dagger:hilt-android:${{Versions.hilt}}"
    const val hiltCompiler = "com.google.dagger:hilt-compiler:${{Versions.hilt}}"
    const val hiltAndroidTesting = "com.google.dagger:hilt-android-testing:${{Versions.hilt}}"
    
    // Room
    const val roomRuntime = "androidx.room:room-runtime:${{Versions.room}}"
    const val roomKtx = "androidx.room:room-ktx:${{Versions.room}}"
    const val roomCompiler = "androidx.room:room-compiler:${{Versions.room}}"
    const val roomPaging = "androidx.room:room-paging:${{Versions.room}}"
    
    // Network
    const val retrofit = "com.squareup.retrofit2:retrofit:${{Versions.retrofit}}"
    const val retrofitGson = "com.squareup.retrofit2:converter-gson:${{Versions.retrofit}}"
    const val retrofitMoshi = "com.squareup.retrofit2:converter-moshi:${{Versions.retrofit}}"
    const val okhttp = "com.squareup.okhttp3:okhttp:${{Versions.okhttp}}"
    const val okhttpLogging = "com.squareup.okhttp3:logging-interceptor:${{Versions.okhttp}}"
    
    // Accompanist
    const val accompanistPermissions = "com.google.accompanist:accompanist-permissions:0.32.0"
    const val accompanistSystemUiController = "com.google.accompanist:accompanist-systemuicontroller:0.32.0"
    const val accompanistPager = "com.google.accompanist:accompanist-pager:0.32.0"
    
    // Apache POI (Excel处理)
    const val poi = "org.apache.poi:poi:5.2.5"
    const val poiOoxml = "org.apache.poi:poi-ooxml:5.2.5"
    
    // Other
    const val gson = "com.google.code.gson:gson:2.10.1"
    const val timber = "com.jakewharton.timber:timber:5.0.1"
    const val coilCompose = "io.coil-kt:coil-compose:2.5.0"
    const val dataStore = "androidx.datastore:datastore-preferences:1.0.0"
    const val workManager = "androidx.work:work-runtime-ktx:2.9.0"
    
    // Test
    const val junit = "junit:junit:${{Versions.junit}}"
    const val testExtJunit = "androidx.test.ext:junit:${{Versions.testExtJunit}}"
    const val espressoCore = "androidx.test.espresso:espresso-core:${{Versions.espresso}}"
    const val composeTestJunit4 = "androidx.compose.ui:ui-test-junit4"
    const val mockk = "io.mockk:mockk:1.13.9"
    const val truth = "com.google.truth:truth:1.1.5"
}}
'''
        
        deps_file.write_text(content)
        print("✅ 已创建统一的依赖管理文件")
    
    def create_gradle_properties(self):
        """创建gradle.properties文件 - 针对 JDK 17 优化"""
        print("⚙️  创建gradle.properties配置...")
        
        props_file = self.project_root / "gradle.properties"
        system_config = self.configs["system"]
        gradle_config = self.configs["gradle"]
        
        # 修复：使用原始字符串处理Windows路径
        if self.env_detector.system == "Windows":
            # Windows系统使用正斜杠，Gradle支持正斜杠路径
            java_home = system_config["java_home"].replace("\\", "/")
            android_sdk = system_config["android_sdk"].replace("\\", "/")
        else:
            java_home = system_config["java_home"]
            android_sdk = system_config["android_sdk"]
        
        content = f'''# Project-wide Gradle settings
# 针对 JDK 17 和 Android 10+ 优化
# 自动生成时间: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
# 系统: {system_config['os']}

# Java 配置
org.gradle.java.home={java_home}
org.gradle.jvmargs=-Xmx{gradle_config['memory']} -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8

# Android SDK 配置
sdk.dir={android_sdk}
android.useAndroidX=true
android.enableJetifier=false
android.nonTransitiveRClass=true
android.enableComposeReporting=true
android.suppressUnsupportedCompileSdk=34
android.defaults.buildfeatures.buildconfig=true
android.defaults.buildfeatures.viewbinding=true

# Kotlin - JDK 17 兼容性
kotlin.code.style=official
kotlin.incremental=true
kotlin.caching.enabled=true
kotlin.daemon.jvmargs=-Xmx{gradle_config['memory']}
kotlin.experimental.tryK2=false
kotlin.jvm.target.validation.mode=warning

# Compose
kotlin.compiler.execution.strategy=in-process

# 构建性能优化
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
org.gradle.daemon=true
org.gradle.jvmargs=-Xmx{gradle_config['memory']}
org.gradle.unsafe.configuration-cache=true
org.gradle.unsafe.configuration-cache-problems=warn
org.gradle.vfs.watch=true
org.gradle.workers.max={max(4, os.cpu_count() // 2)}

# 调试配置
android.debug.obsoleteApi=true
android.studio.sdk.known=false

# 网络代理配置（如果需要）
# systemProp.http.proxyHost=127.0.0.1
# systemProp.http.proxyPort=1080
# systemProp.https.proxyHost=127.0.0.1
# systemProp.https.proxyPort=1080
# systemProp.http.nonProxyHosts=localhost|127.*|[::1]
'''
        
        # 备份原文件
        backup_path = self.backup_manager.backup_file(props_file, "创建gradle.properties")
        
        props_file.write_text(content)
        print("✅ 已创建gradle.properties文件")
    
    def create_gradle_wrapper(self):
        """创建/更新Gradle Wrapper"""
        print("📦 创建Gradle Wrapper...")
        
        gradle_version = self.configs["gradle"]["version"]
        
        # Gradle Wrapper属性文件
        wrapper_props = self.project_root / "gradle" / "wrapper" / "gradle-wrapper.properties"
        wrapper_props.parent.mkdir(parents=True, exist_ok=True)
        
        content = f'''# Gradle Wrapper配置
# 自动生成时间: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}

distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\\://services.gradle.org/distributions/gradle-{gradle_version}-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
'''
        
        wrapper_props.write_text(content)
        
        # 在Windows上创建gradlew.bat
        if self.env_detector.system == "Windows":
            gradlew_bat = self.project_root / "gradlew.bat"
            # 使用原始字符串避免转义问题
            gradlew_bat_content = r'''@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto execute

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

@rem Execute Gradle
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable GRADLE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%GRADLE_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
'''
            gradlew_bat.write_text(gradlew_bat_content, encoding='utf-8')
        
        print("✅ 已创建/更新Gradle Wrapper")
    
    def fix_all_modules(self):
        """修复所有模块 - 针对 Android 10+ 优化"""
        print("🔄 开始修复所有模块...")
        
        structure = self.analyze_project_structure()
        
        total_modules = 0
        fixed_modules = 0
        skipped_modules = 0
        
        for module_path in structure["settings"]:
            total_modules += 1
            
            if module_path.startswith(":"):
                module_path = module_path[1:]
            
            dir_path = module_path.replace(":", "/")
            module_dir = self.project_root / dir_path
            build_file = module_dir / "build.gradle.kts"
            
            # 确保目录存在
            module_dir.mkdir(parents=True, exist_ok=True)
            
            # 检查是否需要修复
            if not build_file.exists() or build_file.stat().st_size < 100:
                print(f"  🔧 修复模块: {module_path}")
                
                # 备份原构建文件
                if build_file.exists():
                    self.backup_manager.backup_file(build_file, f"修复模块 {module_path}")
                
                # 创建构建文件
                module_type = self._guess_module_type(module_path)
                
                if module_path == "app":
                    self._create_app_config(module_dir, module_path)
                elif module_path == "core":
                    self._create_core_config(module_dir, module_path)
                elif module_path == "shared":
                    self._create_shared_config(module_dir, module_path)
                elif module_type == "feature":
                    self._create_feature_config(module_dir, module_path)
                elif module_type == "extension":
                    self._create_extension_config(module_dir, module_path)
                elif module_type == "library":
                    self._create_library_config(module_dir, module_path)
                else:
                    self._create_general_config(module_dir, module_path)
                
                fixed_modules += 1
            else:
                print(f"  ✅ 模块已配置: {module_path}")
                skipped_modules += 1
        
        # 创建通用的模块结构
        print("\n📁 创建标准模块目录结构...")
        self._create_standard_module_structure()
        
        print(f"\n📊 修复统计:")
        print(f"   总模块数: {total_modules}")
        print(f"   修复模块数: {fixed_modules}")
        print(f"   跳过模块数: {skipped_modules}")
        
        if structure["problems"]:
            print(f"\n⚠️  发现的问题:")
            for problem in structure["problems"]:
                print(f"   - {problem}")
        
        # 清理旧备份
        self.backup_manager.cleanup_old_backups()
    
    def _create_standard_module_structure(self):
        """创建标准模块目录结构"""
        standard_dirs = [
            "src/main/java/com/omni/message",
            "src/main/kotlin/com/omni/message",
            "src/main/res",
            "src/main/assets",
            "src/main/res/values",
            "src/main/res/layout",
            "src/main/res/drawable",
            "src/androidTest/java/com/omni/message",
            "src/androidTest/kotlin/com/omni/message",
            "src/test/java/com/omni/message",
            "src/test/kotlin/com/omni/message",
        ]
        
        for module_type in ["feature", "extension"]:
            module_type_dir = self.project_root / module_type
            if module_type_dir.exists():
                for module_dir in module_type_dir.iterdir():
                    if module_dir.is_dir():
                        for dir_path in standard_dirs:
                            full_path = module_dir / dir_path
                            full_path.mkdir(parents=True, exist_ok=True)
                        
                        # 创建默认的proguard文件
                        proguard_file = module_dir / "proguard-rules.pro"
                        if not proguard_file.exists():
                            proguard_file.write_text("# Add project specific ProGuard rules here.\n")
                        
                        # 创建默认的AndroidManifest.xml
                        manifest_dir = module_dir / "src/main"
                        manifest_file = manifest_dir / "AndroidManifest.xml"
                        if not manifest_file.exists():
                            self._create_android_manifest(manifest_file, module_dir.name)
    
    def _create_app_config(self, module_dir: Path, module_path: str):
        """创建app模块配置 - 针对 Android 10+ 优化"""
        android = self.configs["android"]
        deps = self.configs["dependencies"]
        
        content = f'''plugins {{
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}}

// 应用模块配置 - 针对 Android 10+ 和 JDK 17 优化
// 自动生成时间: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}

android {{
    namespace = "com.omni.message"
    compileSdk = {android["compileSdk"]}

    defaultConfig {{
        applicationId = "com.omni.message"
        minSdk = {android["minSdk"]}      // Android 10，不再向下兼容
        targetSdk = {android["targetSdk"]}
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
        vectorDrawables {{
            useSupportLibrary = true
        }}
        
        // 构建配置字段
        buildConfigField("String", "BUILD_TIME", "\"{datetime.now().strftime("%Y-%m-%d %H:%M:%S")}\"")
        buildConfigField("String", "BUILD_TYPE", "\"DEBUG\"")
    }}

    buildTypes {{
        getByName("release") {{
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // 发布版本构建配置
            buildConfigField("String", "BUILD_TYPE", "\"RELEASE\"")
        }}
        getByName("debug") {{
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            
            // 调试版本构建配置
            buildConfigField("String", "BUILD_TYPE", "\"DEBUG\"")
        }}
    }}
    
    buildFeatures {{
        compose = true
        buildConfig = true
        viewBinding = true
        aidl = true
        renderScript = false
        resValues = true
        shaders = true
    }}
    
    composeOptions {{
        kotlinCompilerExtensionVersion = "{android["kotlinCompilerExtensionVersion"]}"
    }}
    
    // JDK 17 配置
    compileOptions {{
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }}
    
    kotlinOptions {{
        jvmTarget = "{android["jvmTarget"]}"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi"
        )
    }}
    
    packaging {{
        resources {{
            excludes += "/META-INF/{{AL2.0,LGPL2.1}}"
            excludes += "/META-INF/gradle/incremental.annotation.processors"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/*.kotlin_module"
            excludes += "META-INF/services/javax.annotation.processing.Processor"
            excludes += "META-INF/*.version"
        }}
    }}
    
    testOptions {{
        unitTests {{
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }}
        animationsDisabled = true
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }}
    
    // 变体配置
    flavorDimensions += listOf("environment")
    productFlavors {{
        create("dev") {{
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "ENVIRONMENT", "\"DEV\"")
        }}
        create("prod") {{
            dimension = "environment"
            buildConfigField("String", "ENVIRONMENT", "\"PROD\"")
        }}
    }}
}}

dependencies {{
    // 核心库反混淆
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    
    // 项目模块
    implementation(project(":core"))
    implementation(project(":shared"))
    
    // 功能模块
    val includeAllFeatures = true
    if (includeAllFeatures) {{
        implementation(project(":feature:contact"))
        implementation(project(":feature:messaging"))
        implementation(project(":feature:settings"))
        implementation(project(":feature:notification"))
        implementation(project(":feature:excelimport"))
        implementation(project(":feature:voice"))
        implementation(project(":feature:quickactions"))
    }}
    
    // AndroidX Core
    implementation("androidx.core:core-ktx:{deps["androidx"]["core"]}")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:{deps["androidx"]["lifecycle"]}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:{deps["androidx"]["lifecycle"]}")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:{deps["androidx"]["lifecycle"]}")
    implementation("androidx.activity:activity-compose:{deps["androidx"]["activity"]}")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:{deps["compose"]["bom"]}"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.navigation:navigation-compose:{deps["androidx"]["navigation"]}")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:{deps["hilt"]}")
    kapt("com.google.dagger:hilt-compiler:{deps["hilt"]}")
    ksp("com.google.dagger:hilt-compiler:{deps["hilt"]}")
    
    // 网络
    implementation("com.squareup.retrofit2:retrofit:{deps["retrofit"]}")
    implementation("com.squareup.retrofit2:converter-gson:{deps["retrofit"]}")
    implementation("com.squareup.okhttp3:okhttp:{deps["okhttp"]}")
    implementation("com.squareup.okhttp3:logging-interceptor:{deps["okhttp"]}")
    
    // 图片加载
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // 权限处理
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")
    
    // 测试
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:{deps["coroutines"]}")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("com.google.truth:truth:1.1.5")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:{deps["compose"]["bom"]}"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("com.google.dagger:hilt-android-testing:{deps["hilt"]}")
    kaptAndroidTest("com.google.dagger:hilt-compiler:{deps["hilt"]}")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}}
'''
        
        build_file = module_dir / "build.gradle.kts"
        build_file.write_text(content)
        
        # 创建默认的proguard文件
        proguard_file = module_dir / "proguard-rules.pro"
        if not proguard_file.exists():
            proguard_content = '''# ProGuard 规则配置文件
# 主应用模块特定规则

# Hilt 规则
-keep class com.omni.message.Hilt_* { *; }

# Retrofit 规则
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# OkHttp 规则
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-dontwarn okio.**

# Gson 规则
-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Room 规则
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity
-keepclassmembers class * {
    @androidx.room.* *;
}

# Kotlin 协程
-keep class kotlinx.coroutines.** { *; }

# AndroidX
-keep class androidx.lifecycle.** { *; }
-keep class androidx.compose.** { *; }

# 保持数据类
-keepclassmembers class ** {
    public *;
}

# 移除日志
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}
'''
            proguard_file.write_text(proguard_content)
        
        # 创建默认的AndroidManifest.xml
        manifest_dir = module_dir / "src/main"
        manifest_dir.mkdir(parents=True, exist_ok=True)
        manifest_file = manifest_dir / "AndroidManifest.xml"
        if not manifest_file.exists():
            self._create_android_manifest(manifest_file, module_path)
    
    def _create_core_config(self, module_dir: Path, module_path: str):
        """创建core模块配置 - 核心功能模块"""
        android = self.configs["android"]
        deps = self.configs["dependencies"]
        
        content = f'''plugins {{
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.devtools.ksp")
}}

// 核心模块配置 - 包含通用工具、数据访问层等
// 自动生成时间: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}

android {{
    namespace = "com.omni.message.core"
    compileSdk = {android["compileSdk"]}

    defaultConfig {{
        minSdk = {android["minSdk"]}      // Android 10
        targetSdk = {android["targetSdk"]}
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }}

    buildTypes {{
        getByName("release") {{
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }}
    }}
    
    buildFeatures {{
        compose = true
        buildConfig = true
        aidl = false
        renderScript = false
        resValues = true
        shaders = false
    }}
    
    composeOptions {{
        kotlinCompilerExtensionVersion = "{android["kotlinCompilerExtensionVersion"]}"
    }}
    
    // JDK 17 配置
    compileOptions {{
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }}
    
    kotlinOptions {{
        jvmTarget = "{android["jvmTarget"]}"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }}
    
    testOptions {{
        unitTests {{
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }}
    }}
}}

dependencies {{
    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:{deps["coroutines"]}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:{deps["coroutines"]}")
    
    // AndroidX
    implementation("androidx.core:core-ktx:{deps["androidx"]["core"]}")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:{deps["androidx"]["lifecycle"]}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:{deps["androidx"]["lifecycle"]}")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Compose
    implementation(platform("androidx.compose:compose-bom:{deps["compose"]["bom"]}"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    
    // Room
    implementation("androidx.room:room-runtime:{deps["room"]}")
    implementation("androidx.room:room-ktx:{deps["room"]}")
    implementation("androidx.room:room-paging:{deps["room"]}")
    kapt("androidx.room:room-compiler:{deps["room"]}")
    ksp("androidx.room:room-compiler:{deps["room"]}")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:{deps["hilt"]}")
    kapt("com.google.dagger:hilt-compiler:{deps["hilt"]}")
    ksp("com.google.dagger:hilt-compiler:{deps["hilt"]}")
    
    // Network
    implementation("com.squareup.retrofit2:retrofit:{deps["retrofit"]}")
    implementation("com.squareup.retrofit2:converter-gson:{deps["retrofit"]}")
    implementation("com.squareup.okhttp3:okhttp:{deps["okhttp"]}")
    implementation("com.squareup.okhttp3:logging-interceptor:{deps["okhttp"]}")
    
    // Gson
    implementation("com.google.code.gson:gson:2.10.1")
    
    # Timber 日志
    implementation("com.jakewharton.timber:timber:5.0.1")
    
    # Paging
    implementation("androidx.paging:paging-runtime-ktx:3.2.1")
    implementation("androidx.paging:paging-compose:3.3.0-alpha02")
    
    # 测试
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:{deps["coroutines"]}")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("com.google.truth:truth:1.1.5")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:{deps["compose"]["bom"]}"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}}
'''
        
        build_file = module_dir / "build.gradle.kts"
        build_file.write_text(content)
        
        # 创建consumer proguard文件
        consumer_proguard = module_dir / "consumer-rules.pro"
        if not consumer_proguard.exists():
            consumer_proguard.write_text("# 添加消费者特定的 ProGuard 规则\n")
    
    def _create_feature_config(self, module_dir: Path, module_path: str):
        """创建feature模块配置 - 功能模块"""
        android = self.configs["android"]
        deps = self.configs["dependencies"]
        
        module_name = module_path.split(":")[-1]
        namespace = f"com.omni.message.feature.{module_name}"
        
        # 根据模块名添加特定依赖
        extra_deps = ""
        if module_name == "contact":
            extra_deps = '''    # 联系人模块特定依赖
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.foundation:foundation-layout")
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    implementation("androidx.compose.material:material-icons-extended")'''
        elif module_name == "voice":
            extra_deps = '''    # 语音模块特定依赖
    implementation("androidx.compose.animation:animation")
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")'''
        elif module_name == "excelimport":
            extra_deps = '''    # Excel导入模块特定依赖
    implementation("org.apache.poi:poi:5.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.5")
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    implementation("androidx.compose.material:material-icons-extended")'''
        elif module_name == "messaging":
            extra_deps = '''    # 消息模块特定依赖
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material:material-icons-extended")'''
        elif module_name == "notification":
            extra_deps = '''    # 通知模块特定依赖
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.work:work-runtime-ktx:2.9.0")'''
        elif module_name == "settings":
            extra_deps = '''    # 设置模块特定依赖
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.datastore:datastore-preferences:1.0.0")'''
        elif module_name == "quickactions":
            extra_deps = '''    # 快捷操作模块特定依赖
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material:material-icons-extended")'''
        
        content = f'''plugins {{
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}}

// 功能模块配置: {module_name}
// 自动生成时间: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
// 修复内容: 确保应用Hilt插件时同时添加对应的Hilt依赖

android {{
    namespace = "{namespace}"
    compileSdk = {android["compileSdk"]}

    defaultConfig {{
        minSdk = {android["minSdk"]}      // Android 10
        targetSdk = {android["targetSdk"]}
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }}

    buildTypes {{
        getByName("release") {{
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }}
    }}
    
    buildFeatures {{
        compose = true
        buildConfig = true
    }}
    
    composeOptions {{
        kotlinCompilerExtensionVersion = "{android["kotlinCompilerExtensionVersion"]}"
    }}
    
    // JDK 17 配置
    compileOptions {{
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }}
    
    kotlinOptions {{
        jvmTarget = "{android["jvmTarget"]}"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }}
}}

dependencies {{
    // 项目模块依赖
    implementation(project(":core"))
    implementation(project(":shared"))
    
    // AndroidX Core
    implementation("androidx.core:core-ktx:{deps["androidx"]["core"]}")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:{deps["androidx"]["lifecycle"]}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:{deps["androidx"]["lifecycle"]}")
    implementation("androidx.activity:activity-compose:{deps["androidx"]["activity"]}")
    
    // Compose
    implementation(platform("androidx.compose:compose-bom:{deps["compose"]["bom"]}"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:{deps["androidx"]["navigation"]}")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    
    // Hilt - 必须添加此依赖，因为应用了Hilt插件
    implementation("com.google.dagger:hilt-android:{deps["hilt"]}")
    kapt("com.google.dagger:hilt-compiler:{deps["hilt"]}")
    
    // 模块特定依赖
{extra_deps}
    
    // 测试
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:{deps["coroutines"]}")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:{deps["compose"]["bom"]}"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}}
'''
        
        build_file = module_dir / "build.gradle.kts"
        build_file.write_text(content)
    
    def _create_extension_config(self, module_dir: Path, module_path: str):
        """创建extension模块配置 - 扩展模块"""
        android = self.configs["android"]
        
        module_name = module_path.split(":")[-1]
        namespace = f"com.omni.message.extension.{module_name}"
        
        content = f'''plugins {{
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}}

// 扩展模块配置: {module_name}
// 自动生成时间: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}

android {{
    namespace = "{namespace}"
    compileSdk = {android["compileSdk"]}

    defaultConfig {{
        minSdk = {android["minSdk"]}      // Android 10
        targetSdk = {android["targetSdk"]}
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }}

    buildTypes {{
        getByName("release") {{
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }}
    }}
    
    // JDK 17 配置
    compileOptions {{
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }}
    
    kotlinOptions {{
        jvmTarget = "{android["jvmTarget"]}"
    }}
}}

dependencies {{
    // 基础依赖
    implementation(project(":core"))
    
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.12.0")
    
    // 测试
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}}
'''
        
        build_file = module_dir / "build.gradle.kts"
        build_file.write_text(content)
    
    def _create_shared_config(self, module_dir: Path, module_path: str):
        """创建shared模块配置 - 共享模块"""
        android = self.configs["android"]
        
        content = f'''plugins {{
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}}

// 共享模块配置 - 包含共享资源和工具
// 自动生成时间: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}

android {{
    namespace = "com.omni.message.shared"
    compileSdk = {android["compileSdk"]}

    defaultConfig {{
        minSdk = {android["minSdk"]}      // Android 10
        targetSdk = {android["targetSdk"]}
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }}

    buildTypes {{
        getByName("release") {{
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }}
    }}
    
    // JDK 17 配置
    compileOptions {{
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }}
    
    kotlinOptions {{
        jvmTarget = "{android["jvmTarget"]}"
    }}
}}

dependencies {{
    implementation(project(":core"))
    
    // 共享模块可能需要的依赖
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // 测试
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}}
'''
        
        build_file = module_dir / "build.gradle.kts"
        build_file.write_text(content)
    
    def _create_library_config(self, module_dir: Path, module_path: str):
        """创建通用库模块配置"""
        android = self.configs["android"]
        
        content = f'''plugins {{
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}}

// 通用库模块配置
// 自动生成时间: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}

android {{
    namespace = "com.omni.message.{module_path.replace("/", ".")}"
    compileSdk = {android["compileSdk"]}

    defaultConfig {{
        minSdk = {android["minSdk"]}      // Android 10
        targetSdk = {android["targetSdk"]}
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }}

    buildTypes {{
        getByName("release") {{
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }}
    }}
    
    // JDK 17 配置
    compileOptions {{
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }}
    
    kotlinOptions {{
        jvmTarget = "{android["jvmTarget"]}"
    }}
}}

dependencies {{
    implementation(project(":core"))
    
    // 测试
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}}
'''
        
        build_file = module_dir / "build.gradle.kts"
        build_file.write_text(content)
    
    def _create_general_config(self, module_dir: Path, module_path: str):
        """创建通用配置"""
        android = self.configs["android"]
        
        module_name = module_path.split(":")[-1] if ":" in module_path else module_path
        namespace = f"com.omni.message.{module_name}"
        
        content = f'''plugins {{
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}}

android {{
    namespace = "{namespace}"
    compileSdk = {android["compileSdk"]}

    defaultConfig {{
        minSdk = {android["minSdk"]}      // Android 10
        targetSdk = {android["targetSdk"]}
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }}

    buildTypes {{
        getByName("release") {{
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }}
    }}
    
    // JDK 17 配置
    compileOptions {{
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }}
    
    kotlinOptions {{
        jvmTarget = "{android["jvmTarget"]}"
    }}
}}

dependencies {{
    // 基础依赖
    implementation(project(":core"))
    
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.12.0")
    
    // 测试
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}}
'''
        
        build_file = module_dir / "build.gradle.kts"
        build_file.write_text(content)
    
    def _create_android_manifest(self, manifest_file: Path, module_path: str):
        """创建基本的AndroidManifest.xml"""
        if "app" in module_path:
            content = f'''<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">
    
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" 
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    
    <!-- 联系人相关权限 -->
    <uses-permission android:name="android.permission.READ_CONTACTS" />
    <uses-permission android:name="android.permission.WRITE_CONTACTS" />
    
    <!-- 语音相关权限 -->
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    
    <application
        android:name=".OmniMessageApplication"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.OmniMessage"
        android:networkSecurityConfig="@xml/network_security_config"
        android:usesCleartextTraffic="true"
        tools:targetApi="31"
        tools:ignore="GoogleAppIndexingWarning">
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.OmniMessage"
            android:windowSoftInputMode="adjustResize"
            android:configChanges="orientation|screenSize|screenLayout|keyboardHidden"
            android:launchMode="singleTop">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
        <!-- 提供程序声明 -->
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${{applicationId}}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>
    </application>

</manifest>'''
        else:
            # 库模块的简单manifest
            module_name = module_path.split(":")[-1] if ":" in module_path else module_path
            content = f'''<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <application>
        <!-- {module_name} 模块不需要Activity声明 -->
    </application>

</manifest>'''
        
        # 确保目录存在
        manifest_file.parent.mkdir(parents=True, exist_ok=True)
        manifest_file.write_text(content)
    
    def create_project_documentation(self):
        """创建项目文档"""
        print("📚 创建项目文档...")
        
        # 项目结构说明文档
        docs_dir = self.project_root / "docs"
        docs_dir.mkdir(exist_ok=True)
        
        # 创建项目结构说明
        project_structure = '''
# Omni-Message 项目结构说明

## 模块结构
- `:app` - 主应用模块
- `:core` - 核心功能模块（数据库、网络、工具类等）
- `:shared` - 共享资源和UI组件
- `:feature:*` - 功能模块（按功能划分）
- `:extension:*` - 扩展模块（第三方集成、插件等）

## 目录结构

## 构建配置
- **minSdk**: 29 (Android 10+)
- **compileSdk**: 34
- **targetSdk**: 34
- **JDK**: 17
- **Kotlin**: 1.9.22
- **AGP**: 8.3.0
- **Gradle**: 8.12

## 开发环境要求
1. JDK 17 或更高版本
2. Android SDK 34+
3. 至少 8GB RAM
4. Android Studio Flamingo 或更高版本
'''
        
        readme_file = docs_dir / "PROJECT_STRUCTURE.md"
        readme_file.write_text(project_structure)
        print(f"✅ 已创建项目文档: {readme_file.relative_to(self.project_root)}")
    

    def create_build_validation_script(self):
        """创建构建验证脚本"""
        print("🧪 创建构建验证脚本...")
        
        script_file = self.project_root / "validate_build.py"
        
        content = '''#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Omni-Message 项目构建验证脚本
用于验证项目构建状态和配置完整性
针对 Android 10+ 和 JDK 17 环境优化
"""

import os
import sys
import json
import subprocess
from pathlib import Path
from typing import Dict, List, Tuple, Optional
from datetime import datetime

class BuildValidator:
    """构建验证器"""
    
    def __init__(self, project_root: Path = None):
        self.project_root = project_root or Path(".").resolve()
        self.env_config = self._load_environment_config()
        
    def _load_environment_config(self) -> Optional[Dict]:
        """加载环境配置"""
        config_file = self.project_root / ".environment_config.json"
        if config_file.exists():
            try:
                return json.loads(config_file.read_text(encoding='utf-8'))
            except:
                pass
        return None
    
    def check_java_version(self) -> Tuple[bool, str]:
        """检查Java版本"""
        print("检查Java版本...")
        
        if self.env_config and self.env_config.get("environment", {}).get("java", {}).get("installed"):
            java_info = self.env_config["environment"]["java"]
            version = java_info.get("version", "")
            is_jdk_17 = java_info.get("is_jdk_17", False)
            
            if is_jdk_17:
                return True, f"Java版本: {version} (JDK 17)"
            else:
                return False, f"Java版本: {version} (不是JDK 17)"
        
        # 尝试直接检查
        try:
            result = subprocess.run(
                ["java", "-version"],
                capture_output=True,
                text=True,
                encoding="utf-8"
            )
            
            if result.returncode == 0:
                version_info = result.stderr.split("\n")[0]
                if "17" in version_info:
                    return True, f"Java版本: {version_info} (JDK 17)"
                else:
                    return False, f"Java版本: {version_info} (不是JDK 17)"
        except:
            pass
        
        return False, "无法检测Java版本"
    
    def check_gradle_wrapper(self) -> Tuple[bool, str]:
        """检查Gradle Wrapper"""
        print("检查Gradle Wrapper...")
        
        wrapper_exists = False
        if sys.platform == "win32":
            wrapper_exists = (self.project_root / "gradlew.bat").exists()
        else:
            wrapper_exists = (self.project_root / "gradlew").exists()
        
        if wrapper_exists:
            # 检查gradle wrapper版本
            try:
                cmd = ".\\gradlew --version" if sys.platform == "win32" else "./gradlew --version"
                result = subprocess.run(
                    cmd,
                    shell=True,
                    cwd=self.project_root,
                    capture_output=True,
                    text=True,
                    encoding="utf-8"
                )
                
                if result.returncode == 0:
                    for line in result.stdout.split("\n"):
                        if "Gradle" in line and "version" in line.lower():
                            return True, f"{line.strip()}"
            except:
                pass
            
            return True, "Gradle Wrapper存在"
        else:
            return False, "Gradle Wrapper不存在"
    
    def check_project_structure(self) -> Tuple[bool, List[str]]:
        """检查项目结构"""
        print("检查项目结构...")
        
        issues = []
        required_files = [
            "settings.gradle.kts",
            "build.gradle.kts",
            "gradle.properties",
            "dependencies.gradle.kts"
        ]
        
        for file_name in required_files:
            file_path = self.project_root / file_name
            if not file_path.exists():
                issues.append(f"文件不存在: {file_name}")
            elif file_path.stat().st_size < 50:
                issues.append(f"文件可能为空: {file_name}")
            else:
                print(f"  {file_name} 存在")
        
        # 检查settings.gradle.kts中的模块
        settings_file = self.project_root / "settings.gradle.kts"
        if settings_file.exists():
            try:
                content = settings_file.read_text(encoding="utf-8")
                import re
                includes = re.findall(r'include\("([^"]+)"\)', content)
                if not includes:
                    issues.append("settings.gradle.kts中没有模块声明")
                else:
                    print(f"  发现 {len(includes)} 个模块")
                    
                    # 检查模块目录
                    for include in includes:
                        if include.startswith(":"):
                            include = include[1:]
                        path = include.replace(":", "/")
                        module_dir = self.project_root / path
                        
                        if not module_dir.exists():
                            issues.append(f"模块目录不存在: {path}")
                        
                        build_file = module_dir / "build.gradle.kts"
                        if not build_file.exists():
                            issues.append(f"模块构建文件不存在: {path}/build.gradle.kts")
            except Exception as e:
                issues.append(f"读取settings.gradle.kts失败: {e}")
        
        return len(issues) == 0, issues
    
    def check_dependencies(self) -> Tuple[bool, str]:
        """检查依赖解析"""
        print("检查依赖解析...")
        
        try:
            cmd = ".\\gradlew :app:dependencies --configuration debugCompileClasspath" if sys.platform == "win32" else "./gradlew :app:dependencies --configuration debugCompileClasspath"
            result = subprocess.run(
                cmd,
                shell=True,
                cwd=self.project_root,
                capture_output=True,
                text=True,
                encoding="utf-8",
                timeout=300  # 5分钟超时
            )
            
            if result.returncode == 0:
                output = result.stdout
                
                if "FAILED" in output or "Could not resolve" in output:
                    # 提取错误信息
                    errors = []
                    for line in output.split("\n"):
                        if "FAILED" in line or "Could not resolve" in line or "> Could not find" in line:
                            errors.append(line.strip())
                    
                    if errors:
                        return False, f"依赖解析失败:\n" + "\n".join(errors[:5])
                
                return True, "依赖解析成功"
            else:
                return False, f"依赖检查失败: {result.stderr[:200]}"
        except subprocess.TimeoutExpired:
            return False, "依赖检查超时（超过5分钟）"
        except Exception as e:
            return False, f"依赖检查异常: {e}"
    
    def check_android_config(self) -> Tuple[bool, List[str]]:
        """检查Android配置"""
        print("检查Android配置...")
        
        issues = []
        
        # 检查gradle.properties
        gradle_props = self.project_root / "gradle.properties"
        if gradle_props.exists():
            content = gradle_props.read_text(encoding="utf-8")
            
            required_configs = [
                ("org.gradle.java.home", "JDK路径配置"),
                ("sdk.dir", "Android SDK路径"),
                ("android.useAndroidX=true", "AndroidX启用"),
                ("kotlin.code.style=official", "Kotlin代码风格")
            ]
            
            for config, description in required_configs:
                if config not in content:
                    issues.append(f"gradle.properties缺少: {description}")
            
            # 检查JDK 17配置
            if "jvmTarget=17" not in content and 'jvmTarget = "17"' not in content:
                issues.append("gradle.properties中缺少JDK 17配置")
        
        # 检查dependencies.gradle.kts中的minSdk
        deps_file = self.project_root / "dependencies.gradle.kts"
        if deps_file.exists():
            content = deps_file.read_text(encoding="utf-8")
            if "minSdk = 29" not in content:
                issues.append("dependencies.gradle.kts中minSdk不是29")
        
        return len(issues) == 0, issues
    
    def run_build_test(self) -> Tuple[bool, str]:
        """运行构建测试"""
        print("运行构建测试...")
        
        test_steps = [
            ("清理构建", ".\\gradlew clean" if sys.platform == "win32" else "./gradlew clean"),
            ("同步项目", ".\\gradlew sync" if sys.platform == "win32" else "./gradlew sync"),
            ("编译调试版本", ".\\gradlew :app:assembleDebug" if sys.platform == "win32" else "./gradlew :app:assembleDebug"),
        ]
        
        results = []
        for description, command in test_steps:
            print(f"  执行: {description}")
            try:
                result = subprocess.run(
                    command,
                    shell=True,
                    cwd=self.project_root,
                    capture_output=True,
                    text=True,
                    encoding="utf-8",
                    timeout=600  # 10分钟超时
                )
                
                if result.returncode == 0:
                    results.append(f"{description} 成功")
                else:
                    error_msg = result.stderr[:500] if result.stderr else result.stdout[:500]
                    results.append(f"{description} 失败: {error_msg}")
                    # 如果一个步骤失败，后面的步骤可能也会失败
                    break
            except subprocess.TimeoutExpired:
                results.append(f"{description} 超时（超过10分钟）")
                break
            except Exception as e:
                results.append(f"{description} 异常: {e}")
                break
        
        all_success = all("成功" in result for result in results)
        return all_success, "\n".join(results)
    
    def generate_report(self) -> str:
        """生成验证报告"""
        report = []
        report.append("=" * 70)
        report.append("项目构建验证报告")
        report.append("=" * 70)
        report.append(f"项目路径: {self.project_root}")
        report.append(f"验证时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        report.append("")
        
        # 运行各项检查
        checks = [
            ("Java版本检查", self.check_java_version),
            ("Gradle Wrapper检查", self.check_gradle_wrapper),
            ("项目结构检查", self.check_project_structure),
            ("Android配置检查", self.check_android_config),
            ("依赖解析检查", self.check_dependencies),
            ("构建测试", self.run_build_test),
        ]
        
        overall_success = True
        
        for check_name, check_func in checks:
            report.append(f"{check_name}")
            report.append("-" * 40)
            
            try:
                if check_name in ["项目结构检查", "Android配置检查"]:
                    success, issues = check_func()
                    if success:
                        report.append("通过")
                    else:
                        report.append("失败")
                        for issue in issues:
                            report.append(f"   {issue}")
                        overall_success = False
                else:
                    success, message = check_func()
                    report.append(message)
                    if not success:
                        overall_success = False
            except Exception as e:
                report.append(f"检查异常: {e}")
                overall_success = False
            
            report.append("")
        
        # 总结
        report.append("=" * 70)
        report.append("验证总结")
        report.append("=" * 70)
        
        if overall_success:
            report.append("所有检查通过！项目构建配置正常。")
            report.append("   项目已针对 Android 10+ 和 JDK 17 优化配置。")
            report.append("")
            report.append("下一步建议:")
            report.append("   1. 使用Android Studio打开项目")
            report.append("   2. 运行 ./gradlew build 进行完整构建")
            report.append("   3. 在设备或模拟器上运行应用")
        else:
            report.append("部分检查未通过，请根据上述信息修复问题。")
            report.append("")
            report.append("修复建议:")
            report.append("   1. 检查Java版本是否为JDK 17")
            report.append("   2. 确保Android SDK路径正确配置")
            report.append("   3. 检查网络连接，确保可以下载依赖")
            report.append("   4. 运行 ./gradlew clean 清理构建")
        
        report.append("=" * 70)
        return "\n".join(report)

def main():
    """主函数"""
    import argparse
    
    parser = argparse.ArgumentParser(description="Omni-Message 项目构建验证")
    parser.add_argument("--path", type=str, default=".", help="项目路径")
    parser.add_argument("--report", type=str, help="保存报告到文件")
    parser.add_argument("--json", action="store_true", help="输出JSON格式")
    
    args = parser.parse_args()
    
    try:
        validator = BuildValidator(Path(args.path))
        
        if args.json:
            # 输出JSON格式结果（简化版）
            import json
            result = {
                "timestamp": datetime.now().isoformat(),
                "project_path": str(validator.project_root),
                "checks": {}
            }
            
            checks = [
                ("java", validator.check_java_version),
                ("gradle", validator.check_gradle_wrapper),
                ("dependencies", validator.check_dependencies),
            ]
            
            for check_id, check_func in checks:
                try:
                    success, message = check_func()
                    result["checks"][check_id] = {
                        "success": success,
                        "message": message
                    }
                except Exception as e:
                    result["checks"][check_id] = {
                        "success": False,
                        "error": str(e)
                    }
            
            print(json.dumps(result, indent=2, ensure_ascii=False))
        else:
            # 输出详细报告
            report = validator.generate_report()
            print(report)
            
            # 保存报告到文件
            if args.report:
                report_file = Path(args.report)
                report_file.write_text(report, encoding="utf-8")
                print(f"\n报告已保存到: {report_file}")
        
        # 返回适当的退出码
        return 0
    except Exception as e:
        print(f"验证过程出错: {e}")
        import traceback
        traceback.print_exc()
        return 1

if __name__ == "__main__":
    sys.exit(main())
'''
        
        # 修复：指定编码为utf-8
        script_file.write_text(content, encoding='utf-8')
        
        # 添加执行权限（非Windows系统）
        if os.name != 'nt':
            script_file.chmod(script_file.stat().st_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)
        
        print("✅ 已创建构建验证脚本")


def main():
    """主函数"""
    # 检查Python版本
    if sys.version_info < (3, 8):
        print("❌ 需要 Python 3.8 或更高版本")
        sys.exit(1)
    
    parser = argparse.ArgumentParser(description="修复Omni-Message项目配置")
    parser.add_argument("--path", default=".", help="项目路径")
    parser.add_argument("--check-only", action="store_true", help="仅检查不修改")
    parser.add_argument("--restore", help="恢复指定的备份文件")
    parser.add_argument("--list-backups", action="store_true", help="列出所有备份")
    parser.add_argument("--clean-backups", action="store_true", help="清理旧备份")
    args = parser.parse_args()
    
    try:
        # 检查并安装依赖
        manager = ProjectConfigManager(args.path)
        manager.check_and_install_dependencies()
        
        print("\n" + "=" * 50)
        print(manager.env_detector.get_environment_report())
        print("=" * 50)
        
        # 检查环境要求
        all_ok, issues = manager.env_detector.check_environment_requirements()
        if issues:
            print("\n⚠️  环境检查问题:")
            for issue in issues:
                print(f"  {issue}")
        
        if not all_ok and not args.check_only:
            print("\n❌ 环境不满足要求，无法继续")
            sys.exit(1)
        
        if args.check_only:
            print("\n🔍 仅检查模式，不进行修改")
            return
        
        if args.restore:
            # 恢复备份
            from pathlib import Path
            backup_path = Path(args.restore)
            if backup_path.exists():
                manager.backup_manager.restore_backup(backup_path)
            else:
                print(f"❌ 备份文件不存在: {backup_path}")
            return
        
        if args.list_backups:
            # 列出备份
            backups = manager.backup_manager.list_backups()
            if backups:
                print("\n📁 可用备份:")
                for i, backup in enumerate(backups):
                    print(f"{i+1}. {backup['timestamp']} - {backup['original']}")
                    print(f"   原因: {backup['reason']}")
                    print(f"   备份: {backup['backup']}")
            else:
                print("📁 没有找到备份")
            return
        
        if args.clean_backups:
            # 清理备份
            manager.backup_manager.cleanup_old_backups()
            return
        
        # 用户确认
        print("\n⚠️  即将开始修复项目配置，建议先备份重要文件！")
        response = input("是否继续？ (y/N): ").strip().lower()
        if response not in ['y', 'yes']:
            print("操作已取消")
            return
        
        print("\n" + "=" * 50)
        print("🛠️  开始修复项目配置...")
        
        # 修复步骤
        manager.create_gradle_properties()
        manager.create_gradle_wrapper()
        manager.create_unified_dependencies_file()
        manager.fix_root_build_config()
        manager.fix_all_modules()
        manager.create_project_documentation()
        manager.create_build_validation_script()
        
        print("\n" + "=" * 50)
        print("🎉 项目配置修复完成！")
        print("=" * 50)
        
        # 运行验证
        print("\n🧪 运行构建验证...")
        validate_script = manager.project_root / "validate_build.py"
        if validate_script.exists():
            subprocess.run([sys.executable, str(validate_script)])
        else:
            print("⚠️  验证脚本未找到，请手动运行构建测试")
        
        print("\n🚀 下一步:")
        print("1. 使用 Android Studio 打开项目")
        print("2. 等待 Gradle 同步完成")
        print("3. 运行 ./gradlew build 测试构建")
        print("4. 连接设备或启动模拟器")
        print("5. 运行应用")
        
    except KeyboardInterrupt:
        print("\n\n操作被用户中断")
        sys.exit(0)
    except Exception as e:
        print(f"\n❌ 修复过程中出现错误: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()
    