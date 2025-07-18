@echo off
echo Android隐私监控应用调试脚本
echo ================================

echo 1. 编译并安装应用...
call .\gradlew clean assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo 编译失败！请检查代码错误。
    pause
    exit /b 1
)

echo 2. 安装APK...
adb install -r app\build\outputs\apk\debug\app-debug.apk
if %ERRORLEVEL% NEQ 0 (
    echo 安装失败！
    pause
    exit /b 1
)

echo 3. 检查应用是否已安装...
adb shell pm list packages | findstr privacy.monitor
if %ERRORLEVEL% NEQ 0 (
    echo 应用未正确安装！
    pause
    exit /b 1
)

echo 4. 启动应用...
echo 正在启动应用..."
adb shell am start -n com.privacy.monitor/.MainActivity

echo 5. 检查应用是否正常启动...
timeout /t 3 /nobreak > nul
adb shell dumpsys activity activities | findstr privacy.monitor

echo 6. 检查监控配置状态...
echo 查看当前配置:
adb shell cat /sdcard/PrivacyMonitor/config.txt
echo.

echo 7. 检查监控是否正常工作...
echo 查看最近的监控日志:
adb logcat -d 

pause