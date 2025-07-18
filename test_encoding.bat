@echo off
chcp 65001 > nul
echo 测试CSV文件中文编码
echo ================================

echo 1. 编译并安装应用...
call .\gradlew clean assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo 编译失败！
    pause
    exit /b 1
)

adb install -r app\build\outputs\apk\debug\app-debug.apk

echo 2. 清理旧文件并启动监控...
adb shell rm -rf /sdcard/PrivacyMonitor/*
adb logcat -c
adb shell am start -n com.privacy.monitor/.MainActivity
timeout /t 2 /nobreak > nul

echo 3. 触发一些隐私操作...
adb shell am start -a android.intent.action.VIEW -d "https://www.baidu.com"
timeout /t 3 /nobreak > nul

echo 4. 检查生成的CSV文件...
echo ================================

echo 4.1 列出生成的文件：
adb shell ls -la /sdcard/PrivacyMonitor/

echo.
echo 4.2 检查文件编码（查看UTF-8 BOM）：
adb shell find /sdcard/PrivacyMonitor/ -name "*.csv" -exec hexdump -C {} \; | head -3

echo.
echo 4.3 查看CSV文件内容（原始）：
adb shell cat /sdcard/PrivacyMonitor/*.csv 2>nul | head -10

echo.
echo 4.4 导出文件到本地进行测试：
adb pull /sdcard/PrivacyMonitor/ ./encoding_test/ 2>nul

if exist "./encoding_test/*.csv" (
    echo 文件已导出到 ./encoding_test/ 目录
    echo.
    echo 4.5 本地文件内容预览：
    type "encoding_test\*.csv" | more
    echo.
    echo 请使用以下工具测试中文显示：
    echo 1. 记事本 - 应该能正确显示中文
    echo 2. Excel - 打开时选择UTF-8编码
    echo 3. VS Code - 应该自动识别UTF-8编码
) else (
    echo 文件导出失败或没有生成CSV文件
)

echo.
echo 编码测试完成！
echo 如果中文显示正常，说明编码修复成功。
pause