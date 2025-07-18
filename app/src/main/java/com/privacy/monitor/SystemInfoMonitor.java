package com.privacy.monitor;

import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class SystemInfoMonitor {
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        hookDeviceId(lpparam);
        hookIMEI(lpparam);
        hookAndroidId(lpparam);
        hookSystemProperties(lpparam);
        hookSerialNumber(lpparam);
    }
    
    private static void hookDeviceId(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                TelephonyManager.class,
                "getDeviceId",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        logSystemInfoAccess(lpparam.packageName, "获取设备ID", "TelephonyManager.getDeviceId()");
                    }
                    
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String result = (String) param.getResult();
                        if (result != null) {
                            String stackTrace = getCleanStackTrace();
                            LogManager.writeDetailedLog(lpparam.packageName, "获取设备ID成功", 
                                "设备ID: " + result.substring(0, Math.min(result.length(), 8)) + "***", 
                                "TelephonyManager.getDeviceId()", stackTrace);
                        }
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log("Hook设备ID失败: " + t.getMessage());
        }
    }
    
    private static void hookIMEI(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook getImei方法
            XposedHelpers.findAndHookMethod(
                TelephonyManager.class,
                "getImei",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        logSystemInfoAccess(lpparam.packageName, "获取IMEI", "TelephonyManager.getImei()");
                    }
                    
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String result = (String) param.getResult();
                        if (result != null) {
                            String stackTrace = getCleanStackTrace();
                            LogManager.writeDetailedLog(lpparam.packageName, "获取IMEI成功", 
                                "IMEI: " + result.substring(0, Math.min(result.length(), 6)) + "***", 
                                "TelephonyManager.getImei()", stackTrace);
                        }
                    }
                }
            );
            
            // Hook带参数的getImei方法
            XposedHelpers.findAndHookMethod(
                TelephonyManager.class,
                "getImei",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        int slotIndex = (Integer) param.args[0];
                        String stackTrace = getCleanStackTrace();
                        LogManager.writeDetailedLog(lpparam.packageName, "获取IMEI", 
                            "slotIndex=" + slotIndex, "TelephonyManager.getImei(int)", stackTrace);
                    }
                    
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String result = (String) param.getResult();
                        if (result != null) {
                            String stackTrace = getCleanStackTrace();
                            LogManager.writeDetailedLog(lpparam.packageName, "获取IMEI成功", 
                                "IMEI: " + result.substring(0, Math.min(result.length(), 6)) + "***", 
                                "TelephonyManager.getImei(int)", stackTrace);
                        }
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log("Hook IMEI失败: " + t.getMessage());
        }
    }
    
    private static void hookAndroidId(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                Settings.Secure.class,
                "getString",
                android.content.ContentResolver.class,
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String name = (String) param.args[1];
                        if (Settings.Secure.ANDROID_ID.equals(name)) {
                            logSystemInfoAccess(lpparam.packageName, "获取Android ID", "Settings.Secure.ANDROID_ID");
                        }
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log("Hook Android ID失败: " + t.getMessage());
        }
    }
    
    private static void hookSystemProperties(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook Build类的静态字段访问
            Class<?> buildClass = Build.class;
            XposedHelpers.findAndHookMethod(
                "java.lang.reflect.Field",
                lpparam.classLoader,
                "get",
                Object.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            java.lang.reflect.Field field = (java.lang.reflect.Field) param.thisObject;
                            if (field.getDeclaringClass() == buildClass) {
                                String fieldName = field.getName();
                                logSystemInfoAccess(lpparam.packageName, "获取系统属性", "Build." + fieldName);
                            }
                        } catch (Exception e) {
                            // 忽略异常
                        }
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log("Hook系统属性失败: " + t.getMessage());
        }
    }
    
    private static void hookSerialNumber(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                Build.class,
                "getSerial",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        logSystemInfoAccess(lpparam.packageName, "获取序列号", "Build.getSerial()");
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log("Hook序列号失败: " + t.getMessage());
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
                
                if (isXposedFrameworkCall(className, methodName, fileName) || isSystemClass(className)) {
                    continue;
                }
                
                if (validCallCount < 3) {
                    if (sb.length() > 0) sb.append(" -> ");
                    sb.append(className).append(".").append(methodName).append("(")
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
    
    private static boolean isXposedFrameworkCall(String className, String methodName, String fileName) {
        if (className == null) return false;
        String[] xposedClasses = {"dalvik.system.VMStack", "MaQZpylZXhHvYoEJG.Nr.O.", "org.lsposed.lspd.", "de.robv.android.xposed.", "LSPHooker_"};
        for (String xposedClass : xposedClasses) {
            if (className.startsWith(xposedClass)) return true;
        }
        if (methodName != null) {
            String[] xposedMethods = {"callback", "handleBefore", "handleAfter", "invoke", "getThreadStackTrace"};
            for (String xposedMethod : xposedMethods) {
                if (methodName.equals(xposedMethod)) return true;
            }
        }
        return fileName != null && (fileName.equals("null") || fileName.equals("VMStack.java"));
    }
    
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
            if (className.startsWith(prefix)) return true;
        }
        return false;
    }
    
    private static void logSystemInfoAccess(String packageName, String action, String info) {
        String stackTrace = getCleanStackTrace();
        String message = String.format(
            "[隐私监控] 应用: %s | 操作: %s | 信息: %s | 调用栈: %s | 时间: %d",
            packageName, action, info, stackTrace, System.currentTimeMillis()
        );
        XposedBridge.log(message);
        LogManager.writeDetailedLog(packageName, action, info, info, stackTrace);
    }
}