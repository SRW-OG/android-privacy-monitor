# GitHub上传指南

## 📋 准备工作

### 1. 构建APK文件
```bash
# 运行构建脚本
.\build_release.bat
```

这将生成：
- `release/app-debug.apk` - 调试版本
- `release/app-release-unsigned.apk` - 发布版本（未签名）
- 相关文档和测试脚本

### 2. 检查文件结构
确保以下文件存在：
```
├── app/                          # 应用源码
├── .github/workflows/build.yml   # GitHub Actions配置
├── .gitignore                    # Git忽略文件
├── README.md                     # 项目说明
├── LICENSE                       # 许可证
├── RELEASE_NOTES.md             # 发布说明
├── build_release.bat            # 构建脚本
└── release/                     # 发布文件目录
    ├── app-debug.apk
    ├── app-release-unsigned.apk
    └── *.md                     # 文档文件
```

## 🚀 上传到GitHub

### 步骤1: 创建GitHub仓库

1. 登录GitHub
2. 点击右上角的"+"，选择"New repository"
3. 填写仓库信息：
   - **Repository name**: `android-privacy-monitor`
   - **Description**: `Android隐私合规监控系统 - 基于Xposed框架的隐私行为监控工具`
   - **Visibility**: Public（推荐）或Private
   - 不要勾选"Initialize this repository with a README"

### 步骤2: 初始化本地Git仓库

```bash
# 在项目根目录执行
git init
git add .
git commit -m "Initial commit: Android Privacy Monitor v1.0.0"
```

### 步骤3: 连接远程仓库

```bash
# 替换为你的GitHub用户名
git remote add origin https://github.com/YOUR_USERNAME/android-privacy-monitor.git
git branch -M main
git push -u origin main
```

### 步骤4: 创建Release

1. 在GitHub仓库页面，点击"Releases"
2. 点击"Create a new release"
3. 填写Release信息：
   - **Tag version**: `v1.0.0`
   - **Release title**: `Android隐私合规监控系统 v1.0.0`
   - **Description**: 复制`RELEASE_NOTES.md`中的内容
4. 上传APK文件：
   - 拖拽`release/app-debug.apk`
   - 拖拽`release/app-release-unsigned.apk`
5. 点击"Publish release"

## 📁 推荐的仓库结构

```
android-privacy-monitor/
├── .github/
│   └── workflows/
│       └── build.yml              # 自动构建配置
├── app/                           # Android应用源码
│   ├── src/main/java/com/privacy/monitor/
│   │   ├── XposedInit.java
│   │   ├── LogManager.java
│   │   ├── SdkDetector.java
│   │   └── ...
│   ├── src/main/res/
│   └── build.gradle
├── gradle/
├── docs/                          # 文档目录（可选）
│   ├── 隐私合规监控系统说明.md
│   ├── 项目架构.md
│   └── 应用图标说明.md
├── scripts/                       # 脚本目录（可选）
│   ├── quick_test.bat
│   ├── privacy_compliance_test.bat
│   ├── test_encoding.bat
│   └── build_release.bat
├── .gitignore
├── README.md
├── LICENSE
├── RELEASE_NOTES.md
├── build.gradle
├── settings.gradle
└── gradlew.bat
```

## 🔧 GitHub Actions自动构建

项目已配置GitHub Actions，每次推送代码时会自动：
1. 构建Debug和Release版本的APK
2. 将APK作为Artifacts上传
3. 在创建Release时自动附加APK文件

## 📝 仓库设置建议

### 1. 分支保护
在Settings > Branches中设置：
- 保护`main`分支
- 要求Pull Request审查
- 要求状态检查通过

### 2. Issues模板
创建`.github/ISSUE_TEMPLATE/`目录，添加：
- `bug_report.md` - Bug报告模板
- `feature_request.md` - 功能请求模板

### 3. Pull Request模板
创建`.github/pull_request_template.md`

### 4. 项目标签
添加相关标签：
- `android`
- `xposed`
- `privacy`
- `security`
- `monitoring`
- `compliance`

## 📊 README徽章

在README.md中添加状态徽章：
```markdown
[![Build Status](https://github.com/YOUR_USERNAME/android-privacy-monitor/workflows/Build%20APK/badge.svg)](https://github.com/YOUR_USERNAME/android-privacy-monitor/actions)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-5.0%2B-green.svg)](https://android-arsenal.com/api?level=21)
[![Xposed](https://img.shields.io/badge/Xposed-LSPosed-orange.svg)](https://github.com/LSPosed/LSPosed)
[![Release](https://img.shields.io/github/v/release/YOUR_USERNAME/android-privacy-monitor)](https://github.com/YOUR_USERNAME/android-privacy-monitor/releases)
[![Downloads](https://img.shields.io/github/downloads/YOUR_USERNAME/android-privacy-monitor/total)](https://github.com/YOUR_USERNAME/android-privacy-monitor/releases)
```

## 🎯 发布检查清单

发布前确认：
- [ ] 代码已测试，功能正常
- [ ] APK文件已构建并测试
- [ ] README.md内容完整准确
- [ ] RELEASE_NOTES.md已更新
- [ ] 许可证文件存在
- [ ] .gitignore配置正确
- [ ] GitHub Actions配置正常
- [ ] 所有敏感信息已移除
- [ ] 版本号已更新

## 📞 后续维护

### 版本更新流程
1. 修改代码
2. 更新版本号
3. 更新RELEASE_NOTES.md
4. 提交代码
5. 创建新的Release
6. 上传新的APK文件

### 社区管理
- 及时回复Issues
- 审查Pull Requests
- 更新文档
- 发布安全更新

---

**记住替换所有的`YOUR_USERNAME`为你的实际GitHub用户名！**