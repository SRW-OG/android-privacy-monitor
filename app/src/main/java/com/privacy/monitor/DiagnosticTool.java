package com.privacy.monitor;

import java.io.File;
import de.robv.android.xposed.XposedBridge;

public class DiagnosticTool {
    
    public static void runDiagnostic() {
        XposedBridge.log("=== 隐私监控诊断开始 ===");
        
        // 1. 检查配置
        checkConfiguration();
        
        // 2. 检查目录权限
        checkDirectoryPermissions();
        
        // 3. 测试日志写入
        testLogWriting();
        
        XposedBridge.log("=== 隐私监控诊断结束 ===");
    }
    
    private static void checkConfiguration() {
        XposedBridge.log("--- 配置检查 ---");
        
        try {
            ConfigManager.loadConfig();
            boolean monitorAll = ConfigManager.isMonitorAll();
            int appCount = ConfigManager.getMonitoredApps().size();
            
            XposedBridge.log("监控模式: " + (monitorAll ? "全局监控" : "指定应用"));
            XposedBridge.log("监控应用数量: " + appCount);
            
            if (!monitorAll && appCount == 0) {
                XposedBridge.log("⚠️ 警告: 未启用全局监控且未指定监控应用");
            }
            
            // 检查配置文件
            File configFile = new File("/sdcard/PrivacyMonitor/config.txt");
            XposedBridge.log("配置文件存在: " + configFile.exists());
            if (configFile.exists()) {
                XposedBridge.log("配置文件大小: " + configFile.length() + " 字节");
                XposedBridge.log("配置文件可读: " + configFile.canRead());
            }
            
        } catch (Exception e) {
            XposedBridge.log("配置检查失败: " + e.getMessage());
        }
    }
    
    private static void checkDirectoryPermissions() {
        XposedBridge.log("--- 目录权限检查 ---");
        
        String[] testPaths = {
            "/sdcard/PrivacyMonitor/",
            "/storage/emulated/0/PrivacyMonitor/",
            "/sdcard/",
            "/storage/emulated/0/"
        };
        
        for (String path : testPaths) {
            File dir = new File(path);
            XposedBridge.log("路径: " + path);
            XposedBridge.log("  存在: " + dir.exists());
            XposedBridge.log("  是目录: " + dir.isDirectory());
            XposedBridge.log("  可读: " + dir.canRead());
            XposedBridge.log("  可写: " + dir.canWrite());
            
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                XposedBridge.log("  创建结果: " + created);
            }
        }
    }
    
    private static void testLogWriting() {
        XposedBridge.log("--- 日志写入测试 ---");
        
        try {
            String testPackage = "com.test.diagnostic";
            String testAction = "诊断测试";
            String testDetail = "这是一个诊断测试日志";
            
            LogManager.writeDetailedLog(testPackage, testAction, testDetail, 
                "DiagnosticTool.testLogWriting()", "测试调用栈");
            
            // 检查文件是否创建
            String dateStr = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date());
            File testLogFile = new File("/sdcard/PrivacyMonitor/" + testPackage + "_" + dateStr + ".csv");
            
            XposedBridge.log("测试日志文件: " + testLogFile.getAbsolutePath());
            XposedBridge.log("测试日志文件存在: " + testLogFile.exists());
            if (testLogFile.exists()) {
                XposedBridge.log("测试日志文件大小: " + testLogFile.length() + " 字节");
            }
            
        } catch (Exception e) {
            XposedBridge.log("日志写入测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void logAppMonitoringStatus(String packageName) {
        XposedBridge.log("=== 应用监控状态检查: " + packageName + " ===");
        
        boolean shouldMonitor = ConfigManager.shouldMonitorApp(packageName);
        XposedBridge.log("是否应该监控: " + shouldMonitor);
        
        if (shouldMonitor) {
            XposedBridge.log("✅ 应用已配置为监控");
        } else {
            XposedBridge.log("❌ 应用未配置为监控");
            XposedBridge.log("请检查配置: 全局监控=" + ConfigManager.isMonitorAll() + 
                           " 监控应用列表=" + ConfigManager.getMonitoredApps());
        }
    }
}