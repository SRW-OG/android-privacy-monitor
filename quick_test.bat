@echo off
chcp 65001 > nul
echo 快速测试隐私监控功能
echo ================================

echo 1. 清理日志...
adb logcat -c

echo 2. 启动监控应用...
adb shell am start -n com.privacy.monitor/.MainActivity

echo 3. 等待2秒...
timeout /t 2 /nobreak > nul

echo 4. 查看最新的监控日志...
echo ================================
adb logcat -d | findstr "隐私监控" | tail -20

echo.
echo 5. 检查监控文件...
adb shell ls -la /sdcard/PrivacyMonitor/ 2>nul

echo.
echo 6. 查看最新的监控记录...
adb shell find /sdcard/PrivacyMonitor/ -name "*.csv" -exec tail -5 {} \; 2>nul

echo.
echo 快速测试完成！
pause