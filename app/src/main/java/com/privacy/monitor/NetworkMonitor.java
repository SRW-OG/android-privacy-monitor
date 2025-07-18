package com.privacy.monitor;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class NetworkMonitor {
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        hookNetworkAccess(lpparam);
        hookWifiInfo(lpparam);
        hookMobileNetworkInfo(lpparam);
        hookHttpConnection(lpparam);
    }
    
    // Hook 网络连接状态检查
    private static void hookNetworkAccess(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // 减少调试输出
            // XposedBridge.log("隐私监控-开始Hook网络访问: " + lpparam.packageName);
            
            // Hook ConnectivityManager.getActiveNetworkInfo()
            XposedHelpers.findAndHookMethod(
                ConnectivityManager.class,
                "getActiveNetworkInfo",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        // 获取应用调用栈
                        String appCallStack = getAppCallStack();
                        String functionName = "ConnectivityManager.getActiveNetworkInfo";
                        
                        // 构建详细信息，包含调用栈信息
                        String detail = "网络-设备信息-ConnectivityManager.getActiveNetworkInfo() " + appCallStack;
                        
                        logNetworkAccess(lpparam.packageName, "获取网络状态", 
                                       detail, functionName, appCallStack);
                    }
                }
            );
            
            // Hook ConnectivityManager.getAllNetworkInfo()
            XposedHelpers.findAndHookMethod(
                ConnectivityManager.class,
                "getAllNetworkInfo",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        NetworkInfo[] results = (NetworkInfo[]) param.getResult();
                        int networkCount = results != null ? results.length : 0;
                        
                        String stackTrace = getCleanStackTrace();
                        String functionName = "ConnectivityManager.getAllNetworkInfo";
                        
                        logNetworkAccess(lpparam.packageName, "获取所有网络信息", 
                                       "网络数量: " + networkCount, 
                                       functionName, stackTrace);
                    }
                }
            );
            
            // 减少成功日志输出
            // XposedBridge.log("隐私监控-网络访问Hook成功: " + lpparam.packageName);
            
        } catch (Throwable t) {
            XposedBridge.log("隐私监控-Hook网络访问失败: " + lpparam.packageName + " 错误: " + t.getMessage());
        }
    }
    
    // Hook WiFi 信息获取
    private static void hookWifiInfo(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook WifiManager.getConnectionInfo()
            XposedHelpers.findAndHookMethod(
                WifiManager.class,
                "getConnectionInfo",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String stackTrace = getCleanStackTrace();
                        String functionName = "WifiManager.getConnectionInfo";
                        
                        logNetworkAccess(lpparam.packageName, "WiFi连接信息", 
                                       "获取WiFi连接详情", 
                                       functionName, stackTrace);
                    }
                }
            );
            
            // Hook WifiManager.getScanResults()
            XposedHelpers.findAndHookMethod(
                WifiManager.class,
                "getScanResults",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String stackTrace = getCleanStackTrace();
                        String functionName = "WifiManager.getScanResults";
                        
                        logNetworkAccess(lpparam.packageName, "WiFi扫描", 
                                       "获取附近WiFi列表", 
                                       functionName, stackTrace);
                    }
                }
            );
            
            // Hook WifiManager.getConfiguredNetworks()
            XposedHelpers.findAndHookMethod(
                WifiManager.class,
                "getConfiguredNetworks",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String stackTrace = getCleanStackTrace();
                        String functionName = "WifiManager.getConfiguredNetworks";
                        
                        logNetworkAccess(lpparam.packageName, "WiFi配置信息", 
                                       "获取已配置WiFi网络", 
                                       functionName, stackTrace);
                    }
                }
            );
            
        } catch (Throwable t) {
            XposedBridge.log("Hook WiFi信息失败: " + t.getMessage());
        }
    }
    
    // Hook 移动网络信息
    private static void hookMobileNetworkInfo(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook TelephonyManager.getNetworkOperator()
            XposedHelpers.findAndHookMethod(
                TelephonyManager.class,
                "getNetworkOperator",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String result = (String) param.getResult();
                        String stackTrace = getCleanStackTrace();
                        String functionName = "TelephonyManager.getNetworkOperator";
                        
                        logNetworkAccess(lpparam.packageName, "网络运营商", 
                                       "运营商代码: " + (result != null ? result : "未知"), 
                                       functionName, stackTrace);
                    }
                }
            );
            
            // Hook TelephonyManager.getNetworkOperatorName()
            XposedHelpers.findAndHookMethod(
                TelephonyManager.class,
                "getNetworkOperatorName",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String result = (String) param.getResult();
                        String stackTrace = getCleanStackTrace();
                        String functionName = "TelephonyManager.getNetworkOperatorName";
                        
                        logNetworkAccess(lpparam.packageName, "运营商名称", 
                                       "运营商: " + (result != null ? result : "未知"), 
                                       functionName, stackTrace);
                    }
                }
            );
            
            // Hook TelephonyManager.getNetworkType()
            XposedHelpers.findAndHookMethod(
                TelephonyManager.class,
                "getNetworkType",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        int result = (Integer) param.getResult();
                        String networkType = getNetworkTypeName(result);
                        String stackTrace = getCleanStackTrace();
                        String functionName = "TelephonyManager.getNetworkType";
                        
                        logNetworkAccess(lpparam.packageName, "网络类型", 
                                       "类型: " + networkType + " (" + result + ")", 
                                       functionName, stackTrace);
                    }
                }
            );
            
        } catch (Throwable t) {
            XposedBridge.log("Hook移动网络信息失败: " + t.getMessage());
        }
    }
    
    // Hook HTTP 连接
    private static void hookHttpConnection(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook HttpURLConnection.connect()
            XposedHelpers.findAndHookMethod(
                "java.net.HttpURLConnection",
                lpparam.classLoader,
                "connect",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) param.thisObject;
                            String url = conn.getURL().toString();
                            String method = conn.getRequestMethod();
                            
                            String stackTrace = getCleanStackTrace();
                            String functionName = "HttpURLConnection.connect";
                            
                            logNetworkAccess(lpparam.packageName, "HTTP连接", 
                                           "方法: " + method + ", URL: " + url, 
                                           functionName, stackTrace);
                        } catch (Exception e) {
                            // 忽略获取URL失败的情况
                        }
                    }
                }
            );
            
            // Hook HttpsURLConnection.connect()
            XposedHelpers.findAndHookMethod(
                "javax.net.ssl.HttpsURLConnection",
                lpparam.classLoader,
                "connect",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            javax.net.ssl.HttpsURLConnection conn = (javax.net.ssl.HttpsURLConnection) param.thisObject;
                            String url = conn.getURL().toString();
                            String method = conn.getRequestMethod();
                            
                            String stackTrace = getCleanStackTrace();
                            String functionName = "HttpsURLConnection.connect";
                            
                            logNetworkAccess(lpparam.packageName, "HTTPS连接", 
                                           "方法: " + method + ", URL: " + url, 
                                           functionName, stackTrace);
                        } catch (Exception e) {
                            // 忽略获取URL失败的情况
                        }
                    }
                }
            );
            
        } catch (Throwable t) {
            XposedBridge.log("Hook HTTP连接失败: " + t.getMessage());
        }
    }
    
    // 获取网络类型名称
    private static String getNetworkTypeName(int networkType) {
        switch (networkType) {
            case 0: return "未知";
            case 1: return "GPRS";
            case 2: return "EDGE";
            case 3: return "UMTS";
            case 4: return "CDMA";
            case 5: return "EVDO_0";
            case 6: return "EVDO_A";
            case 7: return "1xRTT";
            case 8: return "HSDPA";
            case 9: return "HSUPA";
            case 10: return "HSPA";
            case 11: return "iDEN";
            case 12: return "EVDO_B";
            case 13: return "LTE";
            case 14: return "EHRPD";
            case 15: return "HSPAP";
            case 16: return "GSM";
            case 17: return "TD_SCDMA";
            case 18: return "IWLAN";
            case 20: return "NR (5G)";
            default: return "未知类型(" + networkType + ")";
        }
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
                if (isFrameworkOrSystemCall(className, methodName, fileName)) {
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
                    
                    // 检测SDK
                    String sdk = SdkDetector.detectSdk(null, className);
                    if (sdk != null) {
                        sb.append("[").append(sdk).append("]");
                    }
                    
                    validCallCount++;
                }
            }
            
            return sb.length() > 0 ? sb.toString() : "应用自身";
        } catch (Exception e) {
            return "调用栈获取失败";
        }
    }
    
    // 获取清洁的调用栈（减少调试输出）
    private static String getCleanStackTrace() {
        return getAppCallStack();
    }
    
    // 检查是否为框架或系统调用
    private static boolean isFrameworkOrSystemCall(String className, String methodName, String fileName) {
        if (className == null) return true;
        
        // 框架相关的类名
        String[] frameworkClasses = {
            "dalvik.system.VMStack",
            "MaQZpylZXhHvYoEJG.Nr.O.",
            "org.lsposed.lspd.",
            "de.robv.android.xposed.",
            "LSPHooker_",
            "java.lang.reflect.Method",
            "android.os.Handler",
            "android.os.Looper",
            "android.app.ActivityThread",
            "com.android.internal.",
            "android.app.LoadedApk",
            "android.app.ContextImpl",
            "com.privacy.monitor."
        };
        
        for (String frameworkClass : frameworkClasses) {
            if (className.startsWith(frameworkClass)) {
                return true;
            }
        }
        
        // 系统类
        String[] systemPrefixes = {
            "java.lang.", "java.util.", "java.io.", "java.net.",
            "javax.", "android.os.", "android.app.",
            "sun.", "libcore.", "kotlin.", "kotlinx."
        };
        
        for (String prefix : systemPrefixes) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        
        // 框架相关的方法名
        if (methodName != null) {
            String[] frameworkMethods = {
                "callback", "handleBefore", "handleAfter", "invoke",
                "getThreadStackTrace", "handleMessage", "dispatchMessage"
            };
            
            for (String frameworkMethod : frameworkMethods) {
                if (methodName.equals(frameworkMethod) || methodName.startsWith("-$Nest$")) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    // 检查是否为应用或SDK调用
    private static boolean isAppOrSdkCall(String className) {
        if (className == null) return false;
        
        // 排除系统和框架类
        if (isFrameworkOrSystemCall(className, null, null)) {
            return false;
        }
        
        // 检查是否为已知SDK
        String sdk = SdkDetector.detectSdk(null, className);
        if (sdk != null) {
            return true;
        }
        
        // 检查是否为第三方应用类（不以android.或java.开头）
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
        if (sdk != null) {
            return sdk;
        }
        
        // 返回类名的最后一部分
        String[] parts = fullClassName.split("\\.");
        if (parts.length > 0) {
            return parts[parts.length - 1];
        }
        
        return fullClassName;
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
    
    // 检查是否为系统类（最宽松的过滤条件）
    private static boolean isSystemClass(String className) {
        if (className == null) return true;
        
        // 只过滤监控模块自身，其他所有类都保留
        return className.startsWith("com.privacy.monitor.");
    }
    
    // 记录网络访问日志（记录所有网络相关的隐私合规活动）
    private static void logNetworkAccess(String packageName, String action, String detail, 
                                       String functionName, String stackTrace) {
        // 记录所有网络访问，用于隐私合规检查
        LogManager.writeDetailedLog(packageName, action, detail, functionName, stackTrace);
    }
}