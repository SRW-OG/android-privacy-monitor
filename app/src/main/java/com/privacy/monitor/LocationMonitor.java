package com.privacy.monitor;

import android.location.LocationManager;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class LocationMonitor {
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        hookLocationAccess(lpparam);
        hookGpsAccess(lpparam);
        hookLocationSettings(lpparam);
    }
    
    private static void hookLocationAccess(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook LocationManager.getLastKnownLocation
            XposedHelpers.findAndHookMethod(
                LocationManager.class,
                "getLastKnownLocation",
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String provider = (String) param.args[0];
                        logLocationAccess(lpparam.packageName, "获取最后位置", provider);
                    }
                }
            );
            
            // Hook LocationManager.requestLocationUpdates
            XposedHelpers.findAndHookMethod(
                LocationManager.class,
                "requestLocationUpdates",
                String.class,
                long.class,
                float.class,
                android.location.LocationListener.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String provider = (String) param.args[0];
                        logLocationAccess(lpparam.packageName, "请求位置更新", provider);
                    }
                }
            );
            
            // Hook LocationManager.requestSingleUpdate
            XposedHelpers.findAndHookMethod(
                LocationManager.class,
                "requestSingleUpdate",
                String.class,
                android.location.LocationListener.class,
                android.os.Looper.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String provider = (String) param.args[0];
                        logLocationAccess(lpparam.packageName, "请求单次位置", provider);
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log("Hook位置访问失败: " + t.getMessage());
        }
    }
    
    private static void hookGpsAccess(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook GPS状态检查
            XposedHelpers.findAndHookMethod(
                LocationManager.class,
                "isProviderEnabled",
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String provider = (String) param.args[0];
                        if (LocationManager.GPS_PROVIDER.equals(provider) || 
                            LocationManager.NETWORK_PROVIDER.equals(provider)) {
                            logLocationAccess(lpparam.packageName, "检查位置服务状态", provider);
                        }
                    }
                }
            );
            
            // Hook getAllProviders
            XposedHelpers.findAndHookMethod(
                LocationManager.class,
                "getAllProviders",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        logLocationAccess(lpparam.packageName, "获取位置提供者列表", "getAllProviders");
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log("Hook GPS访问失败: " + t.getMessage());
        }
    }
    
    private static void hookLocationSettings(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook Settings.Secure位置相关设置
            XposedHelpers.findAndHookMethod(
                android.provider.Settings.Secure.class,
                "getString",
                android.content.ContentResolver.class,
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String name = (String) param.args[1];
                        if ("location_providers_allowed".equals(name)) {
                            logLocationAccess(lpparam.packageName, "获取位置设置", "location_providers_allowed");
                        }
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log("Hook位置设置失败: " + t.getMessage());
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
        
        String[] xposedClasses = {
            "dalvik.system.VMStack", "MaQZpylZXhHvYoEJG.Nr.O.", "org.lsposed.lspd.",
            "de.robv.android.xposed.", "LSPHooker_"
        };
        
        for (String xposedClass : xposedClasses) {
            if (className.startsWith(xposedClass)) {
                return true;
            }
        }
        
        if (methodName != null) {
            String[] xposedMethods = {"callback", "handleBefore", "handleAfter", "invoke", "getThreadStackTrace"};
            for (String xposedMethod : xposedMethods) {
                if (methodName.equals(xposedMethod)) {
                    return true;
                }
            }
        }
        
        if (fileName != null && (fileName.equals("null") || fileName.equals("VMStack.java"))) {
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
    
    private static void logLocationAccess(String packageName, String action, String provider) {
        String stackTrace = getAppCallStack();
        String functionName = getCurrentLocationFunction(action);
        
        // 记录所有位置相关访问，用于隐私合规检查
        LogManager.writeDetailedLog(packageName, action, "位置-" + provider, functionName, stackTrace);
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
    
    private static String getCurrentLocationFunction(String action) {
        switch (action) {
            case "获取最后位置": return "LocationManager.getLastKnownLocation()";
            case "请求位置更新": return "LocationManager.requestLocationUpdates()";
            case "请求单次位置": return "LocationManager.requestSingleUpdate()";
            case "检查位置服务状态": return "LocationManager.isProviderEnabled()";
            case "获取位置提供者列表": return "LocationManager.getAllProviders()";
            case "获取位置设置": return "Settings.Secure.getString()";
            default: return "LocationManager";
        }
    }
}