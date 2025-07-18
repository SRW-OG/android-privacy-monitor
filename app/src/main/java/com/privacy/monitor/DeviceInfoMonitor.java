package com.privacy.monitor;

import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.bluetooth.BluetoothAdapter;
import android.net.ConnectivityManager;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class DeviceInfoMonitor {
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        hookWifiInfo(lpparam);
        hookBluetoothInfo(lpparam);
        // hookNetworkInfo(lpparam); // 移除，由NetworkMonitor处理
        hookMacAddress(lpparam);
    }
    
    private static void hookWifiInfo(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook WiFi MAC地址获取
            XposedHelpers.findAndHookMethod(
                WifiInfo.class,
                "getMacAddress",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        logDeviceInfoAccess(lpparam.packageName, "获取WiFi MAC地址", "WifiInfo.getMacAddress()");
                    }
                }
            );
            
            // Hook WiFi SSID获取
            XposedHelpers.findAndHookMethod(
                WifiInfo.class,
                "getSSID",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        logDeviceInfoAccess(lpparam.packageName, "获取WiFi SSID", "WifiInfo.getSSID()");
                    }
                }
            );
            
            // Hook WiFi BSSID获取
            XposedHelpers.findAndHookMethod(
                WifiInfo.class,
                "getBSSID",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        logDeviceInfoAccess(lpparam.packageName, "获取WiFi BSSID", "WifiInfo.getBSSID()");
                    }
                }
            );
            
            // Hook WiFi信息获取
            XposedHelpers.findAndHookMethod(
                WifiManager.class,
                "getConnectionInfo",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        logDeviceInfoAccess(lpparam.packageName, "获取WiFi连接信息", "WifiManager.getConnectionInfo()");
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log("Hook WiFi信息失败: " + t.getMessage());
        }
    }
    
    private static void hookBluetoothInfo(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook蓝牙MAC地址获取
            XposedHelpers.findAndHookMethod(
                BluetoothAdapter.class,
                "getAddress",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        logDeviceInfoAccess(lpparam.packageName, "获取蓝牙MAC地址", "BluetoothAdapter.getAddress()");
                    }
                }
            );
            
            // Hook蓝牙名称获取
            XposedHelpers.findAndHookMethod(
                BluetoothAdapter.class,
                "getName",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        logDeviceInfoAccess(lpparam.packageName, "获取蓝牙名称", "BluetoothAdapter.getName()");
                    }
                }
            );
            
            // Hook蓝牙状态获取
            XposedHelpers.findAndHookMethod(
                BluetoothAdapter.class,
                "getDefaultAdapter",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        logDeviceInfoAccess(lpparam.packageName, "获取蓝牙适配器", "BluetoothAdapter.getDefaultAdapter()");
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log("Hook蓝牙信息失败: " + t.getMessage());
        }
    }
    
    private static void hookNetworkInfo(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook网络状态获取
            XposedHelpers.findAndHookMethod(
                ConnectivityManager.class,
                "getActiveNetworkInfo",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        logDeviceInfoAccess(lpparam.packageName, "获取网络状态", "ConnectivityManager.getActiveNetworkInfo()");
                    }
                }
            );
            
            // Hook所有网络信息获取
            XposedHelpers.findAndHookMethod(
                ConnectivityManager.class,
                "getAllNetworkInfo",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        logDeviceInfoAccess(lpparam.packageName, "获取所有网络信息", "ConnectivityManager.getAllNetworkInfo()");
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log("Hook网络信息失败: " + t.getMessage());
        }
    }
    
    private static void hookMacAddress(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook NetworkInterface获取MAC地址
            XposedHelpers.findAndHookMethod(
                "java.net.NetworkInterface",
                lpparam.classLoader,
                "getHardwareAddress",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        logDeviceInfoAccess(lpparam.packageName, "获取网络接口MAC地址", "NetworkInterface.getHardwareAddress()");
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log("Hook MAC地址失败: " + t.getMessage());
        }
    }
    
    // 获取清洁的调用栈（过滤掉Xposed框架信息）
    private static String getCleanStackTrace() {
        try {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            StringBuilder sb = new StringBuilder();
            int validCallCount = 0;
            
            for (int i = 0; i < Math.min(stackTrace.length, 30); i++) {
                StackTraceElement element = stackTrace[i];
                String className = element.getClassName();
                String methodName = element.getMethodName();
                String fileName = element.getFileName();
                
                // 跳过Xposed框架相关的调用
                if (isXposedFrameworkCall(className, methodName, fileName)) {
                    continue;
                }
                
                // 跳过系统类
                if (isSystemClass(className)) {
                    continue;
                }
                
                // 只保留应用和SDK的调用
                if (validCallCount < 3) {
                    if (sb.length() > 0) {
                        sb.append(" -> ");
                    }
                    sb.append(className).append(".")
                      .append(methodName).append("(")
                      .append(fileName != null ? fileName : "Unknown").append(":")
                      .append(element.getLineNumber()).append(")");
                    validCallCount++;
                }
            }
            
            return sb.length() > 0 ? sb.toString() : "应用调用栈";
        } catch (Exception e) {
            return "调用栈获取失败";
        }
    }
    
    // 检查是否为Xposed框架调用
    private static boolean isXposedFrameworkCall(String className, String methodName, String fileName) {
        if (className == null) return false;
        
        // Xposed框架相关的类名
        String[] xposedClasses = {
            "dalvik.system.VMStack",
            "MaQZpylZXhHvYoEJG.Nr.O.",
            "org.lsposed.lspd.",
            "de.robv.android.xposed.",
            "LSPHooker_"
        };
        
        for (String xposedClass : xposedClasses) {
            if (className.startsWith(xposedClass)) {
                return true;
            }
        }
        
        // Xposed相关的方法名
        if (methodName != null) {
            String[] xposedMethods = {
                "callback", "handleBefore", "handleAfter", "invoke",
                "getThreadStackTrace"
            };
            
            for (String xposedMethod : xposedMethods) {
                if (methodName.equals(xposedMethod)) {
                    return true;
                }
            }
        }
        
        // Xposed相关的文件名
        if (fileName != null && (fileName.equals("null") || 
                                fileName.equals("VMStack.java"))) {
            return true;
        }
        
        return false;
    }
    
    // 检查是否为系统类（放宽过滤条件）
    private static boolean isSystemClass(String className) {
        if (className == null) return true;
        
        // 只过滤真正的系统类，不过滤应用和SDK
        String[] systemPrefixes = {
            "java.lang.", "java.util.", "java.io.", "java.net.",
            "javax.", "android.os.", "android.app.ActivityThread",
            "android.app.LoadedApk", "android.app.ContextImpl",
            "com.privacy.monitor.", "sun.", "com.android.internal.",
            "libcore.", "kotlin.", "kotlinx."
        };
        
        for (String prefix : systemPrefixes) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        
        return false;
    }
    
    private static void logDeviceInfoAccess(String packageName, String action, String info) {
        String stackTrace = getAppCallStack();
        
        // 记录所有设备信息访问，用于隐私合规检查
        LogManager.writeDetailedLog(packageName, action, "设备信息-" + info, info, stackTrace);
    }
    
    // 获取应用调用栈（专注于应用和SDK的调用）
    private static String getAppCallStack() {
        try {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            StringBuilder sb = new StringBuilder();
            int validCallCount = 0;
            boolean foundAppCall = false;
            
            for (int i = 0; i < Math.min(stackTrace.length, 25); i++) {
                StackTraceElement element = stackTrace[i];
                String className = element.getClassName();
                String methodName = element.getMethodName();
                String fileName = element.getFileName();
                
                // 跳过框架和系统调用
                if (isXposedFrameworkCall(className, methodName, fileName) || 
                    isSystemClass(className)) {
                    continue;
                }
                
                // 找到第一个应用调用后开始记录
                if (!foundAppCall && isAppOrSdkCall(className)) {
                    foundAppCall = true;
                }
                
                if (foundAppCall && validCallCount < 3) {
                    if (sb.length() > 0) {
                        sb.append(" -> ");
                    }
                    
                    // 简化类名显示
                    String simpleClassName = getSimpleClassName(className);
                    sb.append(simpleClassName).append(".").append(methodName);
                    
                    // 检测SDK并标注
                    String sdk = SdkDetector.detectSdk(null, className);
                    if (sdk != null && !sdk.equals("应用自身")) {
                        sb.append("[").append(sdk).append("]");
                    }
                    
                    validCallCount++;
                }
            }
            
            return sb.length() > 0 ? sb.toString() : "应用自身，不能明确实际的调用链";
        } catch (Exception e) {
            return "调用栈获取失败";
        }
    }
    
    // 检查是否为应用或SDK调用
    private static boolean isAppOrSdkCall(String className) {
        if (className == null) return false;
        
        // 排除系统和框架类
        if (isXposedFrameworkCall(className, null, null) || 
            isSystemClass(className)) {
            return false;
        }
        
        // 检查是否为已知SDK
        String sdk = SdkDetector.detectSdk(null, className);
        if (sdk != null) {
            return true;
        }
        
        // 检查是否为第三方应用类
        return !className.startsWith("android.") && 
               !className.startsWith("java.") && 
               !className.startsWith("javax.") &&
               !className.startsWith("com.android.") &&
               !className.startsWith("androidx.");
    }
    
    // 获取简化的类名
    private static String getSimpleClassName(String fullClassName) {
        if (fullClassName == null) return "Unknown";
        
        // 如果是已知SDK，返回SDK名称
        String sdk = SdkDetector.detectSdk(null, fullClassName);
        if (sdk != null && !sdk.equals("应用自身")) {
            return sdk;
        }
        
        // 返回类名的最后一部分
        String[] parts = fullClassName.split("\\.");
        if (parts.length > 0) {
            return parts[parts.length - 1];
        }
        
        return fullClassName;
    }
}