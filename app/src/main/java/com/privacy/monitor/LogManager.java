package com.privacy.monitor;

import java.io.File;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogManager {
    
    private static final String LOG_DIR = "/sdcard/PrivacyMonitor/";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private static boolean isXposedEnvironment = false;
    
    // 防止递归调用和频率限制
    private static final java.util.Set<String> writingLogs = new java.util.HashSet<>();
    private static final java.util.Map<String, Long> lastLogTime = new java.util.HashMap<>();
    private static final long MIN_LOG_INTERVAL = 1000; // 1秒内相同日志只记录一次
    
    static {
        // 在类加载时检查是否在Xposed环境中
        try {
            Class.forName("de.robv.android.xposed.XposedBridge");
            isXposedEnvironment = true;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            isXposedEnvironment = false;
        }
    }
    
    public static void writeLog(String packageName, String action, String detail) {
        writeDetailedLog(packageName, action, detail, "", "");
    }
    
    public static void writeDetailedLog(String packageName, String action, String detail, String functionName, String stackTrace) {
        try {
            // 防止递归调用 - 如果正在写入日志，直接返回
            String logKey = packageName + "|" + action + "|" + detail;
            synchronized (writingLogs) {
                if (writingLogs.contains(logKey)) {
                    return; // 防止递归调用
                }
                
                // 频率限制 - 相同日志1秒内只记录一次
                Long lastTime = lastLogTime.get(logKey);
                long currentTime = System.currentTimeMillis();
                if (lastTime != null && (currentTime - lastTime) < MIN_LOG_INTERVAL) {
                    return; // 频率限制
                }
                
                // 过滤掉对监控目录本身的访问
                if (detail.contains("/sdcard/PrivacyMonitor/") || detail.contains("PrivacyMonitor")) {
                    return; // 避免监控自己的日志写入
                }
                
                // 过滤掉系统相关的调用
                if (SdkDetector.isSystemCall(stackTrace)) {
                    return; // 不记录系统调用
                }
                
                // 检测SDK信息，用于判断是否记录
                String detectedSdk = SdkDetector.detectSdk(packageName, functionName);
                if (detectedSdk == null && stackTrace != null) {
                    detectedSdk = SdkDetector.detectSdkFromStackTrace(stackTrace);
                }
                if (detectedSdk == null) {
                    detectedSdk = SdkDetector.detectSdkEnhanced(packageName);
                }
                
                // 检查是否为重要的隐私操作
                boolean isImportant = SdkDetector.isImportantPrivacyAction(action, detail);
                
                // 记录所有隐私合规相关活动：
                // 1. 重要的隐私操作（高风险、中风险）
                // 2. 检测到SDK的操作
                // 3. 网络相关操作（用于合规检查）
                // 4. 权限相关操作
                boolean shouldRecord = isImportant || 
                                     detectedSdk != null || 
                                     action.contains("网络") || 
                                     action.contains("权限") ||
                                     action.contains("设备") ||
                                     action.contains("位置") ||
                                     action.contains("存储") ||
                                     action.contains("媒体") ||
                                     action.contains("通讯录");
                
                if (!shouldRecord) {
                    return; // 不符合记录条件的操作不记录
                }
                
                writingLogs.add(logKey);
                lastLogTime.put(logKey, currentTime);
            }
            
            // 减少调试输出，只在必要时输出
            // logDebug("隐私监控-日志写入: " + packageName + " | " + action + " | " + detail);
            
            // 创建日志目录
            File logDir = getWritableLogDirectory();
            
            // 简化检查，只确保目录存在
            if (logDir == null || !logDir.exists()) {
                logDebug("隐私监控-目录不存在: " + (logDir != null ? logDir.getAbsolutePath() : "null"));
                return;
            }
            
            // 创建日志文件，支持同日期多文件
            String dateStr = DATE_FORMAT.format(new Date());
            File logFile = getLogFile(logDir, packageName, dateStr);
            boolean isNewFile = !logFile.exists();
            
            // 减少调试输出
            // logDebug("隐私监控-日志文件: " + logFile.getAbsolutePath() + " 新文件: " + isNewFile);
            
            // 使用UTF-8编码写入日志，解决中文乱码问题
            BufferedWriter writer = null;
            try {
                // 使用UTF-8编码的OutputStreamWriter
                FileOutputStream fos = new FileOutputStream(logFile, true);
                OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                writer = new BufferedWriter(osw);
                
                // 如果是新文件，添加UTF-8 BOM和CSV头部
                if (isNewFile) {
                    // 添加UTF-8 BOM，确保Excel等工具正确识别编码
                    fos.write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});
                    writer.write("时间,包名,操作类型,详细信息,调用函数,调用栈信息,函数分析,风险评估,SDK信息\n");
                }
                
                String timestamp = TIME_FORMAT.format(new Date());
                
                // 生成函数分析和风险评估
                String functionAnalysis = "";
                try {
                    functionAnalysis = FunctionAnalyzer.getCallerInfo();
                } catch (Exception e) {
                    functionAnalysis = "分析失败: " + e.getMessage();
                }
                
                String riskAssessment = generateRiskAssessment(action, functionName);
                
                // 检测SDK信息 - 使用多种方法检测
                String detectedSdk = null;
                
                // 方法1: 从函数名检测
                if (functionName != null) {
                    detectedSdk = SdkDetector.detectSdk(packageName, functionName);
                    if (detectedSdk == null) {
                        detectedSdk = SdkDetector.detectSdkEnhanced(functionName);
                    }
                }
                
                // 方法2: 从调用栈检测
                if (detectedSdk == null && stackTrace != null) {
                    detectedSdk = SdkDetector.detectSdkFromStackTrace(stackTrace);
                    if (detectedSdk == null) {
                        detectedSdk = SdkDetector.detectSdkEnhanced(stackTrace);
                    }
                }
                
                // 方法3: 从包名检测
                if (detectedSdk == null) {
                    detectedSdk = SdkDetector.detectSdk(packageName, null);
                    if (detectedSdk == null) {
                        detectedSdk = SdkDetector.detectSdkEnhanced(packageName);
                    }
                }
                
                String sdkInfo = detectedSdk != null ? detectedSdk : "应用自身";
                
                // 使用清洁的调用栈，忽略传入的可能包含Xposed信息的stackTrace
                String cleanStackTrace = getCleanStackTrace();
                
                String logEntry = String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                    timestamp, packageName, action, escapeCSV(detail), 
                    escapeCSV(functionName), escapeCSV(cleanStackTrace),
                    escapeCSV(functionAnalysis), escapeCSV(riskAssessment), escapeCSV(sdkInfo));
                
                writer.write(logEntry);
                writer.flush();
                
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (Exception e) {
                        logDebug("隐私监控-关闭文件写入器失败: " + e.getMessage());
                    }
                }
            }
            
            // 减少调试输出，只在出错时输出
            // logDebug("隐私监控-日志写入成功: " + logFile.getAbsolutePath());
            
        } catch (Exception e) {
            logDebug("隐私监控-日志写入失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 清理防递归标记
            synchronized (writingLogs) {
                String logKey = packageName + "|" + action + "|" + detail;
                writingLogs.remove(logKey);
            }
        }
    }
    
    // 安全的日志输出方法
    private static void logDebug(String message) {
        if (isXposedEnvironment) {
            try {
                de.robv.android.xposed.XposedBridge.log(message);
            } catch (Exception e) {
                // 如果XposedBridge调用失败，回退到Android Log
                android.util.Log.d("PrivacyMonitor", message);
            }
        } else {
            // 在普通应用环境中，使用Android Log
            android.util.Log.d("PrivacyMonitor", message);
        }
    }
    
    // 生成详细的风险评估和修复建议
    private static String generateRiskAssessment(String action, String functionName) {
        StringBuilder assessment = new StringBuilder();
        
        // 根据操作类型评估风险并提供修复建议
        if (action.contains("IMEI") || action.contains("设备ID") || action.contains("序列号") || action.contains("Android ID")) {
            assessment.append("🚨 高风险-设备唯一标识 ");
            assessment.append("建议: 使用UUID或其他非永久标识符替代");
        } else if (action.contains("位置") || action.contains("GPS") || action.contains("定位") || action.contains("经纬度")) {
            assessment.append("🚨 高风险-位置信息 ");
            assessment.append("建议: 确保用户明确授权，考虑位置模糊化处理");
        } else if (action.contains("通讯录") || action.contains("联系人") || action.contains("短信") || action.contains("通话")) {
            assessment.append("🚨 高风险-通讯数据 ");
            assessment.append("建议: 仅在必要时访问，加密存储和传输");
        } else if (action.contains("相机") || action.contains("录音") || action.contains("麦克风") || action.contains("拍照")) {
            assessment.append("🚨 高风险-媒体设备 ");
            assessment.append("建议: 明确告知用户使用目的，提供关闭选项");
        } else if (action.contains("MAC地址") || action.contains("BSSID")) {
            assessment.append("⚠️ 高风险-网络标识 ");
            assessment.append("建议: Android 6.0+已限制MAC地址获取，考虑其他方案");
        } else if (action.contains("权限申请") || action.contains("敏感权限")) {
            assessment.append("⚡ 中风险-权限申请 ");
            assessment.append("建议: 在使用前申请，说明使用目的");
        } else if (action.contains("WiFi信息") || action.contains("蓝牙") || action.contains("网络状态")) {
            assessment.append("⚡ 中风险-网络信息 ");
            assessment.append("建议: 仅获取必要信息，避免频繁访问");
        } else if (action.contains("设备信息") || action.contains("系统版本") || action.contains("硬件信息")) {
            assessment.append("ℹ️ 中风险-设备信息 ");
            assessment.append("建议: 用于兼容性检查时可接受");
        } else if (action.contains("存储") || action.contains("文件访问")) {
            assessment.append("ℹ️ 低风险-存储访问 ");
            assessment.append("建议: 仅访问应用私有目录或用户明确选择的文件");
        } else if (action.contains("网络") || action.contains("HTTP") || action.contains("连接")) {
            assessment.append("ℹ️ 低风险-网络访问 ");
            assessment.append("建议: 使用HTTPS，避免传输敏感信息");
        } else {
            assessment.append("❓ 未分类 ");
            assessment.append("建议: 评估是否必要，考虑隐私影响");
        }
        
        // 添加函数信息
        assessment.append(" | 函数: ").append(functionName != null ? functionName : "未知");
        
        return assessment.toString();
    }
    
    // 获取应用调用栈（专注于应用和SDK的调用，减少调试输出）
    private static String getCleanStackTrace() {
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
                    isSystemClassStrict(className)) {
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
            isSystemClassStrict(className)) {
            return false;
        }
        
        // 检查是否为已知SDK
        String sdk = SdkDetector.detectSdk(null, className);
        if (sdk != null) {
            return true;
        }
        
        // 检查是否为第三方应用类（不以系统包名开头）
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
    
    // 更严格的系统类过滤（只过滤真正的系统类）
    private static boolean isSystemClassStrict(String className) {
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
    
    // 获取调用栈信息 - 只保留应用和SDK的调用
    public static String getStackTrace() {
        try {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            StringBuilder sb = new StringBuilder();
            int validCallCount = 0;
            
            // 跳过前几个系统调用，找到应用的调用栈
            for (int i = 0; i < Math.min(stackTrace.length, 30); i++) {
                StackTraceElement element = stackTrace[i];
                String className = element.getClassName();
                String methodName = element.getMethodName();
                String fileName = element.getFileName();
                
                // 跳过框架相关的调用
                if (isFrameworkCall(className, methodName, fileName)) {
                    continue;
                }
                
                // 跳过系统类和监控相关类
                if (isSystemClass(className)) {
                    continue;
                }
                
                // 只保留应用和SDK的调用，限制数量避免过长
                if (validCallCount < 5) {
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
            
            String result = sb.toString().trim();
            return result.isEmpty() ? "无应用调用栈" : result;
        } catch (Exception e) {
            return "调用栈获取失败: " + e.getMessage();
        }
    }
    
    // 检查是否为系统类
    private static boolean isSystemClass(String className) {
        if (className == null) return true;
        
        String[] systemPrefixes = {
            "java.", "javax.", "android.", "androidx.",
            "de.robv.android.xposed", "com.privacy.monitor.",
            "sun.", "com.android.internal.", "dalvik.",
            "libcore.", "org.apache.", "kotlin.", "kotlinx."
        };
        
        for (String prefix : systemPrefixes) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        
        return false;
    }
    
    // 检查是否为框架调用
    private static boolean isFrameworkCall(String className, String methodName, String fileName) {
        if (className == null) return false;
        
        // 框架相关的类名 - 扩展列表
        String[] frameworkClasses = {
            "dalvik.system.VMStack",
            "MaQZpylZXhHvYoEJG.Nr.O.",
            "org.lsposed.lspd.",
            "LSPHooker_",
            "java.lang.reflect.Method",
            "android.os.Handler",
            "android.os.Looper",
            "android.app.ActivityThread",
            "com.android.internal.os.",
            "android.app.LoadedApk",
            "android.app.ContextImpl"
        };
        
        for (String frameworkClass : frameworkClasses) {
            if (className.startsWith(frameworkClass)) {
                return true;
            }
        }
        
        // 框架相关的方法名 - 扩展列表
        if (methodName != null) {
            String[] frameworkMethods = {
                "callback", "handleBefore", "handleAfter", "invoke",
                "getThreadStackTrace", "handleMessage", "dispatchMessage",
                "performCreate", "callActivityOnCreate", "handleBindApplication"
            };
            
            for (String frameworkMethod : frameworkMethods) {
                if (methodName.equals(frameworkMethod) || methodName.startsWith("-$$Nest$")) {
                    return true;
                }
            }
        }
        
        // 框架相关的文件名
        if (fileName != null && (fileName.equals("null") || 
                                fileName.equals("SourceFile") ||
                                fileName.startsWith("Unknown Source") ||
                                fileName.equals("VMStack.java") ||
                                fileName.equals("Handler.java") ||
                                fileName.equals("Looper.java"))) {
            return true;
        }
        
        return false;
    }
    
    private static String escapeCSV(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
    
    // 获取可写的日志目录 - 强制使用 /sdcard/ 目录
    private static File getWritableLogDirectory() {
        // 只使用主要的 /sdcard/ 路径
        String primaryPath = "/sdcard/PrivacyMonitor/";
        File dir = new File(primaryPath);
        
        logDebug("隐私监控-尝试使用目录: " + primaryPath);
        
        try {
            // 强制创建目录
            if (!dir.exists()) {
                boolean created = createDirectoryForcefully(dir);
                logDebug("隐私监控-强制创建目录: " + primaryPath + " 结果: " + created);
            }
            
            // 验证目录是否存在
            if (dir.exists() && dir.isDirectory()) {
                logDebug("隐私监控-成功找到目录: " + primaryPath);
                return dir;
            } else {
                logDebug("隐私监控-目录不存在: " + primaryPath);
            }
            
        } catch (Exception e) {
            logDebug("隐私监控-目录处理异常: " + primaryPath + " 错误: " + e.getMessage());
        }
        
        // 无论如何都返回这个路径，让系统尝试写入
        logDebug("隐私监控-使用默认路径: " + primaryPath);
        return dir;
    }
    
    // 强制创建目录的方法
    private static boolean createDirectoryForcefully(File dir) {
        try {
            // 方法1: 标准创建
            if (dir.mkdirs()) {
                return true;
            }
            
            // 方法2: 逐级创建
            File parent = dir.getParentFile();
            if (parent != null && !parent.exists()) {
                if (parent.mkdirs()) {
                    return dir.mkdir();
                }
            }
            
            // 方法3: 在 Xposed 环境中，尝试使用 Runtime 执行命令
            if (isXposedEnvironment) {
                try {
                    Process process = Runtime.getRuntime().exec("mkdir -p " + dir.getAbsolutePath());
                    int result = process.waitFor();
                    if (result == 0) {
                        logDebug("隐私监控-通过命令创建目录成功: " + dir.getAbsolutePath());
                        return dir.exists();
                    }
                } catch (Exception e) {
                    logDebug("隐私监控-命令创建目录失败: " + e.getMessage());
                }
            }
            
            return false;
            
        } catch (Exception e) {
            logDebug("隐私监控-强制创建目录异常: " + e.getMessage());
            return false;
        }
    }
    
    // 测试目录是否可写
    private static boolean testDirectoryWritable(File dir) {
        try {
            File testFile = new File(dir, ".privacy_monitor_test");
            
            // 尝试创建测试文件
            if (testFile.createNewFile()) {
                // 尝试写入内容
                java.io.FileWriter writer = new java.io.FileWriter(testFile);
                writer.write("test");
                writer.close();
                
                // 清理测试文件
                testFile.delete();
                
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            logDebug("隐私监控-写入测试失败: " + e.getMessage());
            return false;
        }
    }
    
    // 获取日志文件，支持同日期多文件（001, 002, 003...）
    private static File getLogFile(File logDir, String packageName, String dateStr) {
        // 首先尝试不带序号的文件名
        String baseFileName = packageName + "_" + dateStr;
        File logFile = new File(logDir, baseFileName + ".csv");
        
        // 如果文件不存在，直接使用
        if (!logFile.exists()) {
            return logFile;
        }
        
        // 如果文件存在，检查文件大小是否超过限制（比如10MB）
        long maxFileSize = 10 * 1024 * 1024; // 10MB
        if (logFile.length() < maxFileSize) {
            return logFile; // 文件未超过大小限制，继续使用
        }
        
        // 文件超过大小限制，创建新的序号文件
        int sequence = 1;
        do {
            String sequenceStr = String.format("%03d", sequence); // 格式化为001, 002, 003...
            String fileName = baseFileName + "_" + sequenceStr + ".csv";
            logFile = new File(logDir, fileName);
            sequence++;
            
            // 防止无限循环，最多尝试999个文件
            if (sequence > 999) {
                logDebug("隐私监控-警告: 日志文件序号已达到最大值999");
                break;
            }
        } while (logFile.exists() && logFile.length() >= maxFileSize);
        
        logDebug("隐私监控-使用日志文件: " + logFile.getName());
        return logFile;
    }
}