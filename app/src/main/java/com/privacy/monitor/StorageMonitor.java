package com.privacy.monitor;

import java.io.File;
import android.os.Environment;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class StorageMonitor {
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        hookFileAccess(lpparam);
        hookExternalStorage(lpparam);
        hookMediaAccess(lpparam);
    }
    
    private static void hookFileAccess(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook File构造函数
            XposedHelpers.findAndHookConstructor(
                File.class,
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String path = (String) param.args[0];
                        if (isSensitivePath(path)) {
                            logStorageAccess(lpparam.packageName, "文件访问", path);
                        }
                    }
                }
            );
            
            // Hook File.listFiles
            XposedHelpers.findAndHookMethod(
                File.class,
                "listFiles",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        File file = (File) param.thisObject;
                        String path = file.getAbsolutePath();
                        if (isSensitivePath(path)) {
                            logStorageAccess(lpparam.packageName, "列出文件", path);
                        }
                    }
                }
            );
            
            // Hook File.exists
            XposedHelpers.findAndHookMethod(
                File.class,
                "exists",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        File file = (File) param.thisObject;
                        String path = file.getAbsolutePath();
                        if (isSensitivePath(path)) {
                            logStorageAccess(lpparam.packageName, "检查文件存在", path);
                        }
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log("Hook文件访问失败: " + t.getMessage());
        }
    }
    
    private static void hookExternalStorage(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook Environment.getExternalStorageDirectory
            XposedHelpers.findAndHookMethod(
                Environment.class,
                "getExternalStorageDirectory",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        logStorageAccess(lpparam.packageName, "获取外部存储目录", "Environment.getExternalStorageDirectory()");
                    }
                }
            );
            
            // Hook Context.getExternalFilesDir
            XposedHelpers.findAndHookMethod(
                android.content.Context.class,
                "getExternalFilesDir",
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String type = (String) param.args[0];
                        logStorageAccess(lpparam.packageName, "获取外部文件目录", "type: " + type);
                    }
                }
            );
            
            // Hook Environment.getExternalStoragePublicDirectory
            XposedHelpers.findAndHookMethod(
                Environment.class,
                "getExternalStoragePublicDirectory",
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String type = (String) param.args[0];
                        logStorageAccess(lpparam.packageName, "获取外部公共目录", "type: " + type);
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log("Hook外部存储失败: " + t.getMessage());
        }
    }
    
    private static void hookMediaAccess(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook MediaStore访问
            XposedHelpers.findAndHookMethod(
                android.content.ContentResolver.class,
                "query",
                android.net.Uri.class,
                String[].class,
                String.class,
                String[].class,
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        android.net.Uri uri = (android.net.Uri) param.args[0];
                        if (uri != null && isMediaUri(uri)) {
                            logStorageAccess(lpparam.packageName, "查询媒体文件", uri.toString());
                        }
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log("Hook媒体访问失败: " + t.getMessage());
        }
    }
    
    private static boolean isSensitivePath(String path) {
        if (path == null) return false;
        
        return path.contains("/sdcard/") ||
               path.contains("/storage/") ||
               path.contains("/external/") ||
               path.contains("DCIM") ||
               path.contains("Pictures") ||
               path.contains("Documents") ||
               path.contains("Download") ||
               path.contains("Music") ||
               path.contains("Movies") ||
               path.contains("Android/data");
    }
    
    private static boolean isMediaUri(android.net.Uri uri) {
        String authority = uri.getAuthority();
        return "media".equals(authority) ||
               android.provider.MediaStore.AUTHORITY.equals(authority);
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
    
    private static void logStorageAccess(String packageName, String action, String path) {
        // 过滤掉对监控目录的访问，避免递归监控
        if (path != null && (path.contains("PrivacyMonitor") || path.contains("/sdcard/PrivacyMonitor/"))) {
            return; // 不监控自己的日志目录
        }
        
        // 过滤掉系统临时文件和缓存文件
        if (path != null && (path.contains("/.temp") || path.contains("/cache/") || 
                           path.contains("/.cache") || path.endsWith(".tmp") ||
                           path.contains("/Android/data/"))) {
            return; // 不监控临时文件和应用数据目录
        }
        
        String stackTrace = getCleanStackTrace();
        String functionName = getStorageFunction(action);
        String message = String.format(
            "[隐私监控] 应用: %s | 操作: %s | 路径: %s | 调用栈: %s | 时间: %d",
            packageName, action, path, stackTrace, System.currentTimeMillis()
        );
        XposedBridge.log(message);
        LogManager.writeDetailedLog(packageName, action, "存储-" + path, functionName, stackTrace);
    }
    
    private static String getStorageFunction(String action) {
        switch (action) {
            case "文件访问": return "File()";
            case "列出文件": return "File.listFiles()";
            case "检查文件存在": return "File.exists()";
            case "获取外部存储目录": return "Environment.getExternalStorageDirectory()";
            case "获取外部文件目录": return "Context.getExternalFilesDir()";
            case "获取外部公共目录": return "Environment.getExternalStoragePublicDirectory()";
            case "查询媒体文件": return "ContentResolver.query()";
            default: return "Storage API";
        }
    }
}