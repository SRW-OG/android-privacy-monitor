package com.privacy.monitor;

import android.hardware.Camera;
import android.media.MediaRecorder;
import android.media.AudioRecord;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MediaMonitor {
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        hookCameraAccess(lpparam);
        hookAudioRecording(lpparam);
        hookMediaRecorder(lpparam);
    }
    
    private static void hookCameraAccess(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook Camera.open
            XposedHelpers.findAndHookMethod(
                Camera.class,
                "open",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        logMediaAccess(lpparam.packageName, "打开相机", "Camera.open()");
                    }
                }
            );
            
            // Hook Camera.open with camera ID
            XposedHelpers.findAndHookMethod(
                Camera.class,
                "open",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        int cameraId = (Integer) param.args[0];
                        logMediaAccess(lpparam.packageName, "打开相机", "Camera.open(id=" + cameraId + ")");
                    }
                }
            );
            
            // Hook Camera.startPreview
            XposedHelpers.findAndHookMethod(
                Camera.class,
                "startPreview",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        logMediaAccess(lpparam.packageName, "开始相机预览", "Camera.startPreview()");
                    }
                }
            );
            
            // Hook Camera.takePicture
            XposedHelpers.findAndHookMethod(
                Camera.class,
                "takePicture",
                Camera.ShutterCallback.class,
                Camera.PictureCallback.class,
                Camera.PictureCallback.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        logMediaAccess(lpparam.packageName, "拍照", "Camera.takePicture()");
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log("Hook相机访问失败: " + t.getMessage());
        }
    }
    
    private static void hookAudioRecording(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook AudioRecord构造函数
            XposedHelpers.findAndHookConstructor(
                AudioRecord.class,
                int.class, int.class, int.class, int.class, int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        int audioSource = (Integer) param.args[0];
                        logMediaAccess(lpparam.packageName, "创建音频录制", "AudioRecord(source=" + audioSource + ")");
                    }
                }
            );
            
            // Hook AudioRecord.startRecording
            XposedHelpers.findAndHookMethod(
                AudioRecord.class,
                "startRecording",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        logMediaAccess(lpparam.packageName, "开始录音", "AudioRecord.startRecording()");
                    }
                }
            );
            
            // Hook AudioRecord.read
            XposedHelpers.findAndHookMethod(
                AudioRecord.class,
                "read",
                byte[].class, int.class, int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        logMediaAccess(lpparam.packageName, "读取音频数据", "AudioRecord.read()");
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log("Hook音频录制失败: " + t.getMessage());
        }
    }
    
    private static void hookMediaRecorder(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook MediaRecorder.setAudioSource
            XposedHelpers.findAndHookMethod(
                MediaRecorder.class,
                "setAudioSource",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        int audioSource = (Integer) param.args[0];
                        logMediaAccess(lpparam.packageName, "设置音频源", "MediaRecorder.setAudioSource(" + audioSource + ")");
                    }
                }
            );
            
            // Hook MediaRecorder.setVideoSource
            XposedHelpers.findAndHookMethod(
                MediaRecorder.class,
                "setVideoSource",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        int videoSource = (Integer) param.args[0];
                        logMediaAccess(lpparam.packageName, "设置视频源", "MediaRecorder.setVideoSource(" + videoSource + ")");
                    }
                }
            );
            
            // Hook MediaRecorder.start
            XposedHelpers.findAndHookMethod(
                MediaRecorder.class,
                "start",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        logMediaAccess(lpparam.packageName, "开始媒体录制", "MediaRecorder.start()");
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log("Hook媒体录制失败: " + t.getMessage());
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
    
    private static void logMediaAccess(String packageName, String action, String detail) {
        String stackTrace = getCleanStackTrace();
        String functionName = getMediaFunction(action);
        String message = String.format(
            "[隐私监控] 应用: %s | 操作: %s | 详情: %s | 调用栈: %s | 时间: %d",
            packageName, action, detail, stackTrace, System.currentTimeMillis()
        );
        XposedBridge.log(message);
        LogManager.writeDetailedLog(packageName, action, "媒体-" + detail, functionName, stackTrace);
    }
    
    private static String getMediaFunction(String action) {
        switch (action) {
            case "打开相机": return "Camera.open()";
            case "开始相机预览": return "Camera.startPreview()";
            case "拍照": return "Camera.takePicture()";
            case "创建音频录制": return "AudioRecord()";
            case "开始录音": return "AudioRecord.startRecording()";
            case "读取音频数据": return "AudioRecord.read()";
            case "设置音频源": return "MediaRecorder.setAudioSource()";
            case "设置视频源": return "MediaRecorder.setVideoSource()";
            case "开始媒体录制": return "MediaRecorder.start()";
            default: return "Media API";
        }
    }
}