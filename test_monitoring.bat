@echo off
chcp 65001 > nul
echo 隐私监控应用测试脚本
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

echo 3. 清理旧日志...
adb logcat -c

echo 4. 启动应用...
adb shell am start -n com.privacy.monitor/.MainActivity

echo 5. 等待应用启动...
timeout /t 3 /nobreak > nul

echo 6. 测试网络监控功能...
echo 启动一个测试应用来触发网络访问...
adb shell am start -n com.wandoujia.phoenix2/.MainActivity 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo 测试应用未安装，使用系统浏览器测试...
    adb shell am start -a android.intent.action.VIEW -d "http://www.baidu.com"
)

echo 7. 等待监控数据收集...
timeout /t 5 /nobreak > nul

echo 8. 查看监控日志（过滤关键信息）...
echo ================================
echo Xposed框架日志:
adb logcat -d | findstr "隐私监控"
echo.

echo 9. 查看应用监控文件...
echo ================================
echo 检查监控文件是否生成:
adb shell ls -la /sdcard/PrivacyMonitor/
echo.

echo 10. 查看具体监控内容...
echo ================================
adb shell find /sdcard/PrivacyMonitor/ -name "*.csv" -exec head -20 {} \;

echo.
echo 测试完成！
echo 如果看到监控日志和CSV文件，说明监控功能正常工作。
echo 注意检查调用栈信息是否正确显示应用和SDK信息。
pause