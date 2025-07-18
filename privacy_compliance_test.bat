@echo off
chcp 65001 > nul
echo 隐私合规监控完整测试脚本
echo ================================

echo 1. 编译并安装监控应用...
call .\gradlew clean assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo 编译失败！请检查代码错误。
    pause
    exit /b 1
)

echo 2. 安装监控应用...
adb install -r app\build\outputs\apk\debug\app-debug.apk
if %ERRORLEVEL% NEQ 0 (
    echo 安装失败！
    pause
    exit /b 1
)

echo 3. 清理旧日志和文件...
adb logcat -c
adb shell rm -rf /sdcard/PrivacyMonitor/*

echo 4. 启动监控应用...
adb shell am start -n com.privacy.monitor/.MainActivity
timeout /t 2 /nobreak > nul

echo 5. 测试各种隐私合规场景...
echo ================================

echo 5.1 测试网络访问监控...
echo 启动浏览器访问网页（测试网络状态检查）
adb shell am start -a android.intent.action.VIEW -d "https://www.baidu.com"
timeout /t 3 /nobreak > nul

echo 5.2 测试设备信息获取...
echo 启动设置应用（可能触发设备信息获取）
adb shell am start -a android.settings.SETTINGS
timeout /t 2 /nobreak > nul

echo 5.3 测试位置相关功能...
echo 启动地图应用（如果有的话）
adb shell am start -a android.intent.action.VIEW -d "geo:0,0?q=北京" 2>nul
timeout /t 2 /nobreak > nul

echo 5.4 测试通讯录访问...
echo 启动联系人应用
adb shell am start -a android.intent.action.VIEW -d "content://contacts/people/" 2>nul
timeout /t 2 /nobreak > nul

echo 5.5 测试相机权限...
echo 启动相机应用
adb shell am start -a android.media.action.IMAGE_CAPTURE 2>nul
timeout /t 2 /nobreak > nul

echo 6. 等待监控数据收集...
timeout /t 5 /nobreak > nul

echo 7. 查看监控结果...
echo ================================

echo 7.1 检查Xposed日志（过滤隐私监控相关）...
echo --------------------------------
adb logcat -d | findstr "隐私监控" | tail -30

echo.
echo 7.2 检查监控文件生成情况...
echo --------------------------------
adb shell ls -la /sdcard/PrivacyMonitor/

echo.
echo 7.3 查看具体的隐私合规记录...
echo --------------------------------
echo 显示最新的监控记录（前20行）：
adb shell find /sdcard/PrivacyMonitor/ -name "*.csv" -exec head -20 {} \;

echo.
echo 7.4 统计各类隐私操作...
echo --------------------------------
echo 高风险操作统计：
adb shell find /sdcard/PrivacyMonitor/ -name "*.csv" -exec grep -c "高风险" {} \; 2>nul

echo 中风险操作统计：
adb shell find /sdcard/PrivacyMonitor/ -name "*.csv" -exec grep -c "中风险" {} \; 2>nul

echo SDK检测统计：
adb shell find /sdcard/PrivacyMonitor/ -name "*.csv" -exec grep -c "阿里" {} \; 2>nul

echo.
echo 7.5 检查中文编码...
echo --------------------------------
echo 检查CSV文件是否包含UTF-8 BOM：
adb shell find /sdcard/PrivacyMonitor/ -name "*.csv" -exec hexdump -C {} \; | head -5

echo.
echo 8. 生成隐私合规报告...
echo ================================

echo 8.1 导出监控数据到本地...
adb pull /sdcard/PrivacyMonitor/ ./privacy_logs/ 2>nul
if %ERRORLEVEL% EQU 0 (
    echo 监控数据已导出到 ./privacy_logs/ 目录
    echo 可以使用Excel或其他工具打开CSV文件进行详细分析
) else (
    echo 数据导出失败，请检查设备连接和权限
)

echo.
echo 9. 隐私合规检查建议...
echo ================================
echo 请检查以下内容：
echo 1. 高风险操作是否有用户明确授权
echo 2. 设备唯一标识符的使用是否合规
echo 3. 位置信息的收集是否必要
echo 4. 网络传输是否使用HTTPS
echo 5. SDK的隐私政策是否完整
echo 6. 是否有不必要的权限申请

echo.
echo 测试完成！
echo 请查看生成的CSV文件了解详细的隐私合规情况。
echo 每个操作都包含风险评估和修复建议。
pause