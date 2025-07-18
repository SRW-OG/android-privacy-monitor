package com.privacy.monitor;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedInit implements IXposedHookLoadPackage {
    
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // 减少调试输出，只在必要时输出
        // XposedBridge.log("隐私监控-应用加载: " + lpparam.packageName);
        
        // 如果是我们自己的应用，运行诊断
        if ("com.privacy.monitor".equals(lpparam.packageName)) {
            XposedBridge.log("隐私监控-检测到自身应用，运行诊断");
            DiagnosticTool.runDiagnostic();
            return;
        }
        
        // 过滤系统应用
        if (isSystemApp(lpparam.packageName)) {
            // 减少系统应用的日志输出
            return;
        }
        
        // 记录应用监控状态（减少输出）
        // DiagnosticTool.logAppMonitoringStatus(lpparam.packageName);
        
        // 检查是否需要监控此应用
        boolean shouldMonitor = ConfigManager.shouldMonitorApp(lpparam.packageName);
        
        if (!shouldMonitor) {
            // 减少跳过应用的日志输出
            return;
        }
        
        // 只在开始监控重要应用时输出日志
        XposedBridge.log("隐私监控-开始监控应用: " + lpparam.packageName);
        
        try {
            // 初始化所有监控模块，减少调试输出，只在出错时输出
            int successCount = 0;
            
            try {
                PermissionMonitor.init(lpparam);
                successCount++;
            } catch (Exception e) {
                XposedBridge.log("隐私监控-权限监控初始化失败: " + lpparam.packageName + " 错误: " + e.getMessage());
            }
            
            try {
                SystemInfoMonitor.init(lpparam);
                successCount++;
            } catch (Exception e) {
                XposedBridge.log("隐私监控-系统信息监控初始化失败: " + lpparam.packageName + " 错误: " + e.getMessage());
            }
            
            try {
                LocationMonitor.init(lpparam);
                successCount++;
            } catch (Exception e) {
                XposedBridge.log("隐私监控-位置监控初始化失败: " + lpparam.packageName + " 错误: " + e.getMessage());
            }
            
            try {
                DeviceInfoMonitor.init(lpparam);
                successCount++;
            } catch (Exception e) {
                XposedBridge.log("隐私监控-设备信息监控初始化失败: " + lpparam.packageName + " 错误: " + e.getMessage());
            }
            
            try {
                ContactsMonitor.init(lpparam);
                successCount++;
            } catch (Exception e) {
                XposedBridge.log("隐私监控-通讯录监控初始化失败: " + lpparam.packageName + " 错误: " + e.getMessage());
            }
            
            try {
                StorageMonitor.init(lpparam);
                successCount++;
            } catch (Exception e) {
                XposedBridge.log("隐私监控-存储监控初始化失败: " + lpparam.packageName + " 错误: " + e.getMessage());
            }
            
            try {
                MediaMonitor.init(lpparam);
                successCount++;
            } catch (Exception e) {
                XposedBridge.log("隐私监控-媒体监控初始化失败: " + lpparam.packageName + " 错误: " + e.getMessage());
            }
            
            try {
                NetworkMonitor.init(lpparam);
                successCount++;
            } catch (Exception e) {
                XposedBridge.log("隐私监控-网络监控初始化失败: " + lpparam.packageName + " 错误: " + e.getMessage());
            }
            
            // 只输出总体初始化结果
            XposedBridge.log("隐私监控-监控模块初始化完成: " + lpparam.packageName + " (成功: " + successCount + "/8)");
            
        } catch (Exception e) {
            XposedBridge.log("隐私监控-整体初始化失败: " + lpparam.packageName + " 错误: " + e.getMessage());
        }
    }
    
    private boolean isSystemApp(String packageName) {
        return packageName.startsWith("android.") ||
               packageName.startsWith("com.android.") ||
               packageName.startsWith("com.google.") ||
               packageName.startsWith("com.sec.") ||
               packageName.startsWith("com.samsung.") ||
               packageName.startsWith("com.miui.") ||
               packageName.startsWith("com.xiaomi.") ||
               packageName.equals("system") ||
               packageName.equals("com.privacy.monitor"); // 排除自己
    }
}