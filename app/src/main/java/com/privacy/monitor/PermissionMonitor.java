package com.privacy.monitor;

import android.content.Context;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class PermissionMonitor {
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        hookPermissionCheck(lpparam);
        hookPermissionRequest(lpparam);
    }
    
    private static void hookPermissionCheck(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // 减少调试输出
            // XposedBridge.log("隐私监控-开始Hook权限检查: " + lpparam.packageName);
            
            // Hook ContextImpl.checkSelfPermission 而不是抽象的 Context.checkSelfPermission
            XposedHelpers.findAndHookMethod(
                "android.app.ContextImpl",
                lpparam.classLoader,
                "checkSelfPermission",
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String permission = (String) param.args[0];
                        String functionName = "ContextImpl.checkSelfPermission()";
                        String stackTrace = getAppCallStack();
                        
                        // 只记录敏感权限的检查
                        if (isSensitivePermission(permission)) {
                            LogManager.writeDetailedLog(lpparam.packageName, "权限检查", 
                                                      "敏感权限-" + permission, functionName, stackTrace);
                        }
                    }
                }
            );
            
            // Hook PackageManager.checkPermission
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager",
                lpparam.classLoader,
                "checkPermission",
                String.class,
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String permission = (String) param.args[0];
                        String packageName = (String) param.args[1];
                        String functionName = "PackageManager.checkPermission()";
                        String stackTrace = getAppCallStack();
                        
                        // 只记录敏感权限的检查
                        if (isSensitivePermission(permission)) {
                            LogManager.writeDetailedLog(lpparam.packageName, "权限检查", 
                                                      "检查其他应用权限-" + permission + " 目标:" + packageName, 
                                                      functionName, stackTrace);
                        }
                    }
                }
            );
            
            // 减少成功日志输出
            // XposedBridge.log("隐私监控-权限检查Hook成功: " + lpparam.packageName);
            
        } catch (Throwable t) {
            XposedBridge.log("隐私监控-Hook权限检查失败: " + lpparam.packageName + " 错误: " + t.getMessage());
        }
    }
    
    private static void hookPermissionRequest(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook ActivityCompat.requestPermissions
            XposedHelpers.findAndHookMethod(
                "androidx.core.app.ActivityCompat",
                lpparam.classLoader,
                "requestPermissions",
                android.app.Activity.class,
                String[].class,
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String[] permissions = (String[]) param.args[1];
                        int requestCode = (Integer) param.args[2];
                        String functionName = "ActivityCompat.requestPermissions()";
                        String stackTrace = getAppCallStack();
                        
                        for (String permission : permissions) {
                            String detail = "权限申请-" + permission + " (requestCode=" + requestCode + ")";
                            LogManager.writeDetailedLog(lpparam.packageName, "权限申请", detail, 
                                                      functionName, stackTrace);
                        }
                    }
                }
            );
            
            // Hook Activity.requestPermissions (API 23+)
            XposedHelpers.findAndHookMethod(
                android.app.Activity.class,
                "requestPermissions",
                String[].class,
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String[] permissions = (String[]) param.args[0];
                        int requestCode = (Integer) param.args[1];
                        String functionName = "Activity.requestPermissions()";
                        String stackTrace = getAppCallStack();
                        
                        for (String permission : permissions) {
                            String detail = "权限申请-" + permission + " (requestCode=" + requestCode + ")";
                            LogManager.writeDetailedLog(lpparam.packageName, "权限申请", detail, 
                                                      functionName, stackTrace);
                        }
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log("Hook权限申请失败: " + t.getMessage());
        }
    }
    
    // 检查是否为敏感权限
    private static boolean isSensitivePermission(String permission) {
        if (permission == null) return false;
        
        // 高风险权限（必须记录）
        String[] highRiskPermissions = {
            "android.permission.READ_PHONE_STATE",
            "android.permission.READ_PHONE_NUMBERS", 
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_CONTACTS",
            "android.permission.READ_SMS",
            "android.permission.SEND_SMS",
            "android.permission.READ_CALL_LOG",
            "android.permission.WRITE_CALL_LOG",
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.ACCESS_WIFI_STATE",
            "android.permission.CHANGE_WIFI_STATE",
            "android.permission.BLUETOOTH",
            "android.permission.BLUETOOTH_ADMIN",
            "android.permission.GET_ACCOUNTS"
        };
        
        // 中风险权限（隐私合规需要关注）
        String[] mediumRiskPermissions = {
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.INTERNET",
            "android.permission.VIBRATE",
            "android.permission.WAKE_LOCK",
            "android.permission.RECEIVE_BOOT_COMPLETED",
            "android.permission.SYSTEM_ALERT_WINDOW",
            "android.permission.WRITE_SETTINGS"
        };
        
        // 检查高风险权限
        for (String riskPermission : highRiskPermissions) {
            if (permission.equals(riskPermission)) {
                return true;
            }
        }
        
        // 检查中风险权限
        for (String riskPermission : mediumRiskPermissions) {
            if (permission.equals(riskPermission)) {
                return true;
            }
        }
        
        // 检查自定义权限或其他敏感权限
        return permission.contains("LOCATION") || 
               permission.contains("PHONE") || 
               permission.contains("SMS") || 
               permission.contains("CONTACTS") || 
               permission.contains("CAMERA") || 
               permission.contains("MICROPHONE") || 
               permission.contains("STORAGE") ||
               permission.contains("BLUETOOTH") ||
               permission.contains("WIFI");
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
    
    // 检查是否为系统类
    private static boolean isSystemClass(String className) {
        if (className == null) return true;
        
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