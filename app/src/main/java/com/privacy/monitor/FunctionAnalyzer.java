package com.privacy.monitor;

import java.util.HashMap;
import java.util.Map;

public class FunctionAnalyzer {
    
    // 隐私相关函数的详细信息映射
    private static final Map<String, PrivacyFunctionInfo> PRIVACY_FUNCTIONS = new HashMap<>();
    
    static {
        // 权限相关函数
        PRIVACY_FUNCTIONS.put("checkSelfPermission", new PrivacyFunctionInfo(
            "Context.checkSelfPermission()", "权限检查", "检查应用是否具有指定权限", "高"));
        PRIVACY_FUNCTIONS.put("requestPermissions", new PrivacyFunctionInfo(
            "ActivityCompat.requestPermissions()", "权限申请", "向用户申请权限", "高"));
        
        // 设备标识相关函数
        PRIVACY_FUNCTIONS.put("getDeviceId", new PrivacyFunctionInfo(
            "TelephonyManager.getDeviceId()", "设备标识", "获取设备唯一标识符", "高"));
        PRIVACY_FUNCTIONS.put("getImei", new PrivacyFunctionInfo(
            "TelephonyManager.getImei()", "设备标识", "获取IMEI号码", "高"));
        PRIVACY_FUNCTIONS.put("getSerial", new PrivacyFunctionInfo(
            "Build.getSerial()", "设备标识", "获取设备序列号", "高"));
        PRIVACY_FUNCTIONS.put("ANDROID_ID", new PrivacyFunctionInfo(
            "Settings.Secure.ANDROID_ID", "设备标识", "获取Android设备ID", "中"));
        
        // 位置相关函数
        PRIVACY_FUNCTIONS.put("getLastKnownLocation", new PrivacyFunctionInfo(
            "LocationManager.getLastKnownLocation()", "位置信息", "获取最后已知位置", "高"));
        PRIVACY_FUNCTIONS.put("requestLocationUpdates", new PrivacyFunctionInfo(
            "LocationManager.requestLocationUpdates()", "位置信息", "请求位置更新", "高"));
        PRIVACY_FUNCTIONS.put("requestSingleUpdate", new PrivacyFunctionInfo(
            "LocationManager.requestSingleUpdate()", "位置信息", "请求单次位置更新", "高"));
        
        // 网络和设备信息相关函数
        PRIVACY_FUNCTIONS.put("getMacAddress", new PrivacyFunctionInfo(
            "WifiInfo.getMacAddress()", "网络标识", "获取WiFi MAC地址", "中"));
        PRIVACY_FUNCTIONS.put("getSSID", new PrivacyFunctionInfo(
            "WifiInfo.getSSID()", "网络信息", "获取WiFi网络名称", "中"));
        PRIVACY_FUNCTIONS.put("getBSSID", new PrivacyFunctionInfo(
            "WifiInfo.getBSSID()", "网络信息", "获取WiFi基站标识", "中"));
        PRIVACY_FUNCTIONS.put("getBluetoothAddress", new PrivacyFunctionInfo(
            "BluetoothAdapter.getAddress()", "设备标识", "获取蓝牙MAC地址", "中"));
        
        // 通讯录相关函数
        PRIVACY_FUNCTIONS.put("queryContacts", new PrivacyFunctionInfo(
            "ContentResolver.query(ContactsContract)", "通讯录", "查询联系人信息", "高"));
        PRIVACY_FUNCTIONS.put("queryCallLog", new PrivacyFunctionInfo(
            "ContentResolver.query(CallLog)", "通话记录", "查询通话记录", "高"));
        PRIVACY_FUNCTIONS.put("querySms", new PrivacyFunctionInfo(
            "ContentResolver.query(Sms)", "短信", "查询短信内容", "高"));
        
        // 媒体设备相关函数
        PRIVACY_FUNCTIONS.put("openCamera", new PrivacyFunctionInfo(
            "Camera.open()", "相机", "打开相机设备", "高"));
        PRIVACY_FUNCTIONS.put("startRecording", new PrivacyFunctionInfo(
            "AudioRecord.startRecording()", "麦克风", "开始录音", "高"));
        PRIVACY_FUNCTIONS.put("takePicture", new PrivacyFunctionInfo(
            "Camera.takePicture()", "相机", "拍摄照片", "高"));
        
        // 存储相关函数
        PRIVACY_FUNCTIONS.put("getExternalStorageDirectory", new PrivacyFunctionInfo(
            "Environment.getExternalStorageDirectory()", "存储访问", "获取外部存储目录", "中"));
        PRIVACY_FUNCTIONS.put("queryMediaStore", new PrivacyFunctionInfo(
            "ContentResolver.query(MediaStore)", "媒体文件", "查询媒体文件", "中"));
    }
    
    public static class PrivacyFunctionInfo {
        public final String functionName;
        public final String category;
        public final String description;
        public final String riskLevel;
        
        public PrivacyFunctionInfo(String functionName, String category, String description, String riskLevel) {
            this.functionName = functionName;
            this.category = category;
            this.description = description;
            this.riskLevel = riskLevel;
        }
    }
    
    // 分析函数调用并返回详细信息
    public static String analyzeFunctionCall(String methodName, String className, Object[] args) {
        StringBuilder analysis = new StringBuilder();
        
        // 基本函数信息
        analysis.append("函数: ").append(className).append(".").append(methodName).append("(");
        
        // 参数信息
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                if (i > 0) analysis.append(", ");
                if (args[i] != null) {
                    analysis.append(args[i].getClass().getSimpleName()).append("=").append(args[i].toString());
                } else {
                    analysis.append("null");
                }
            }
        }
        analysis.append(")");
        
        // 查找隐私函数信息
        PrivacyFunctionInfo info = PRIVACY_FUNCTIONS.get(methodName);
        if (info != null) {
            analysis.append(" | 类别: ").append(info.category);
            analysis.append(" | 描述: ").append(info.description);
            analysis.append(" | 风险级别: ").append(info.riskLevel);
        }
        
        return analysis.toString();
    }
    
    // 获取调用者信息 - 使用清洁的调用栈
    public static String getCallerInfo() {
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
                
                // 跳过系统类和监控相关类
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
            return "调用栈获取失败: " + e.getMessage();
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
    
    // 检查是否为系统类
    private static boolean isSystemClass(String className) {
        if (className == null) return true;
        
        String[] systemPrefixes = {
            "java.", "javax.", "android.", "androidx.",
            "com.privacy.monitor.", "sun.", "com.android.internal.",
            "libcore.", "org.apache.", "kotlin.", "kotlinx."
        };
        
        for (String prefix : systemPrefixes) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        
        return false;
    }
    
    // 生成隐私风险评估
    public static String generateRiskAssessment(String functionName, String packageName) {
        PrivacyFunctionInfo info = PRIVACY_FUNCTIONS.get(functionName);
        if (info == null) {
            return "未知风险";
        }
        
        StringBuilder assessment = new StringBuilder();
        assessment.append("风险评估: ");
        
        switch (info.riskLevel) {
            case "高":
                assessment.append("⚠️ 高风险 - 涉及敏感个人信息，需要用户明确授权");
                break;
            case "中":
                assessment.append("⚡ 中风险 - 涉及设备标识信息，可能用于追踪");
                break;
            case "低":
                assessment.append("ℹ️ 低风险 - 一般功能调用");
                break;
            default:
                assessment.append("❓ 未评估");
        }
        
        return assessment.toString();
    }
}