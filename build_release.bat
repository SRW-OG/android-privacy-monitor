@echo off
chcp 65001 > nul
echo 构建发布版本APK
echo ================================

echo 1. 清理旧的构建文件...
call .\gradlew clean

echo 2. 构建Debug版本...
call .\gradlew assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo Debug版本构建失败！
    pause
    exit /b 1
)

echo 3. 构建Release版本...
call .\gradlew assembleRelease
if %ERRORLEVEL% NEQ 0 (
    echo Release版本构建失败！
    pause
    exit /b 1
)

echo 4. 检查生成的APK文件...
echo ================================

if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo ✅ Debug APK: app\build\outputs\apk\debug\app-debug.apk
    for %%I in ("app\build\outputs\apk\debug\app-debug.apk") do echo    大小: %%~zI bytes
) else (
    echo ❌ Debug APK未生成
)

if exist "app\build\outputs\apk\release\app-release-unsigned.apk" (
    echo ✅ Release APK: app\build\outputs\apk\release\app-release-unsigned.apk
    for %%I in ("app\build\outputs\apk\release\app-release-unsigned.apk") do echo    大小: %%~zI bytes
) else (
    echo ❌ Release APK未生成
)

echo.
echo 5. 创建发布目录...
if not exist "release" mkdir release
copy "app\build\outputs\apk\debug\app-debug.apk" "release\" >nul 2>&1
copy "app\build\outputs\apk\release\app-release-unsigned.apk" "release\" >nul 2>&1

echo 6. 生成APK信息...
echo APK构建信息 > release\build_info.txt
echo 构建时间: %date% %time% >> release\build_info.txt
echo. >> release\build_info.txt

if exist "release\app-debug.apk" (
    echo Debug版本: >> release\build_info.txt
    for %%I in ("release\app-debug.apk") do echo   文件大小: %%~zI bytes >> release\build_info.txt
    echo   文件路径: release\app-debug.apk >> release\build_info.txt
    echo. >> release\build_info.txt
)

if exist "release\app-release-unsigned.apk" (
    echo Release版本: >> release\build_info.txt
    for %%I in ("release\app-release-unsigned.apk") do echo   文件大小: %%~zI bytes >> release\build_info.txt
    echo   文件路径: release\app-release-unsigned.apk >> release\build_info.txt
    echo   注意: 此版本未签名，需要自行签名后使用 >> release\build_info.txt
    echo. >> release\build_info.txt
)

echo 7. 复制相关文档...
copy "README.md" "release\" >nul 2>&1
copy "RELEASE_NOTES.md" "release\" >nul 2>&1
copy "隐私合规监控系统说明.md" "release\" >nul 2>&1
copy "项目架构.md" "release\" >nul 2>&1

echo 8. 复制测试脚本...
copy "*.bat" "release\" >nul 2>&1

echo.
echo ================================
echo 构建完成！
echo ================================
echo.
echo 发布文件位于 release\ 目录：
dir release\ /b
echo.
echo 上传到GitHub的步骤：
echo 1. 将项目推送到GitHub仓库
echo 2. 创建新的Release
echo 3. 上传release目录中的APK文件
echo 4. 添加RELEASE_NOTES.md中的发布说明
echo.
echo 注意事项：
echo - Debug版本可直接使用
echo - Release版本需要签名后才能安装
echo - 确保在LSPosed中启用模块
echo.
pause