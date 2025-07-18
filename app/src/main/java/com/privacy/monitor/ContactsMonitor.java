package com.privacy.monitor;

import android.content.ContentResolver;
import android.provider.ContactsContract;
import android.provider.CallLog;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class ContactsMonitor {
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        hookContactsAccess(lpparam);
        hookCallLogAccess(lpparam);
        hookSmsAccess(lpparam);
    }
    
    private static void hookContactsAccess(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook ContentResolver.query方法
            XposedHelpers.findAndHookMethod(
                ContentResolver.class,
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
                        if (uri != null && isContactsUri(uri)) {
                            logContactsAccess(lpparam.packageName, "查询通讯录", uri.toString());
                        }
                    }
                }
            );
            
            // Hook带CancellationSignal的query方法
            XposedHelpers.findAndHookMethod(
                ContentResolver.class,
                "query",
                android.net.Uri.class,
                String[].class,
                String.class,
                String[].class,
                String.class,
                android.os.CancellationSignal.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        android.net.Uri uri = (android.net.Uri) param.args[0];
                        if (uri != null && isContactsUri(uri)) {
                            logContactsAccess(lpparam.packageName, "查询通讯录", uri.toString());
                        }
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log("Hook通讯录访问失败: " + t.getMessage());
        }
    }
    
    private static void hookCallLogAccess(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook ContentResolver.query方法检查通话记录
            XposedHelpers.findAndHookMethod(
                ContentResolver.class,
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
                        if (uri != null && isCallLogUri(uri)) {
                            logContactsAccess(lpparam.packageName, "查询通话记录", uri.toString());
                        }
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log("Hook通话记录访问失败: " + t.getMessage());
        }
    }
    
    private static void hookSmsAccess(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook ContentResolver.query方法检查短信
            XposedHelpers.findAndHookMethod(
                ContentResolver.class,
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
                        if (uri != null && isSmsUri(uri)) {
                            logContactsAccess(lpparam.packageName, "查询短信", uri.toString());
                        }
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log("Hook短信访问失败: " + t.getMessage());
        }
    }
    
    private static boolean isContactsUri(android.net.Uri uri) {
        String authority = uri.getAuthority();
        return ContactsContract.AUTHORITY.equals(authority);
    }
    
    private static boolean isCallLogUri(android.net.Uri uri) {
        String authority = uri.getAuthority();
        return CallLog.AUTHORITY.equals(authority) || 
               "call_log".equals(authority);
    }
    
    private static boolean isSmsUri(android.net.Uri uri) {
        String authority = uri.getAuthority();
        return "sms".equals(authority) || 
               "mms".equals(authority) ||
               "mms-sms".equals(authority);
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
    
    private static void logContactsAccess(String packageName, String action, String uri) {
        String stackTrace = getCleanStackTrace();
        String functionName = "ContentResolver.query()";
        String message = String.format(
            "[隐私监控] 应用: %s | 操作: %s | URI: %s | 调用栈: %s | 时间: %d",
            packageName, action, uri, stackTrace, System.currentTimeMillis()
        );
        XposedBridge.log(message);
        LogManager.writeDetailedLog(packageName, action, "通讯录-" + uri, functionName, stackTrace);
    }
}