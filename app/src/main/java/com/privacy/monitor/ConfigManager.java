package com.privacy.monitor;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public class ConfigManager {
    
    private static final String PREF_NAME = "privacy_monitor_config";
    private static final String KEY_MONITORED_APPS = "monitored_apps";
    private static final String KEY_MONITOR_ALL = "monitor_all";
    private static final String CONFIG_FILE = "/sdcard/PrivacyMonitor/config.txt";
    
    private static Set<String> monitoredApps = new HashSet<>();
    private static boolean monitorAll = false;
    private static boolean configLoaded = false;
    private static boolean isXposedEnvironment = false;
    
    static {
        // 在类加载时检查是否在Xposed环境中
        try {
            Class.forName("de.robv.android.xposed.XposedBridge");
            isXposedEnvironment = true;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            isXposedEnvironment = false;
        }
    }
    
    public static void loadConfig() {
        if (configLoaded) {
            logDebug("隐私监控-配置已加载，跳过");
            return;
        }
        
        logDebug("隐私监控-开始加载配置文件: " + CONFIG_FILE);
        
        try {
            java.io.File configFile = new java.io.File(CONFIG_FILE);
            if (!configFile.exists()) {
                logDebug("隐私监控-配置文件不存在，创建默认配置");
                // 默认不启用监控，需要手动配置
                monitorAll = false;
                saveConfig();
                configLoaded = true;
                return;
            }
            
            logDebug("隐私监控-读取配置文件: " + configFile.getAbsolutePath());
            
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.FileReader(configFile));
            String line;
            int lineCount = 0;
            
            while ((line = reader.readLine()) != null) {
                lineCount++;
                line = line.trim();
                if (line.startsWith("monitor_all=")) {
                    monitorAll = Boolean.parseBoolean(line.substring(12));
                    logDebug("隐私监控-配置读取: monitor_all=" + monitorAll);
                } else if (line.startsWith("app=")) {
                    String appName = line.substring(4);
                    monitoredApps.add(appName);
                    logDebug("隐私监控-配置读取: 监控应用=" + appName);
                }
            }
            reader.close();
            configLoaded = true;
            
            logDebug("隐私监控-配置加载完成: 读取" + lineCount + "行, monitorAll=" + 
                monitorAll + ", 监控应用数=" + monitoredApps.size());
            
        } catch (Exception e) {
            logDebug("隐私监控-配置加载失败: " + e.getMessage());
            // 加载失败，使用默认配置
            monitorAll = false;
            configLoaded = true;
        }
    }
    
    public static void saveConfig() {
        try {
            // 使用与LogManager相同的强制创建目录方法
            java.io.File logDir = new java.io.File("/sdcard/PrivacyMonitor/");
            if (!logDir.exists()) {
                boolean created = createDirectoryForcefully(logDir);
                logDebug("隐私监控-配置目录创建: " + created);
            }
            
            java.io.File configFile = new java.io.File(CONFIG_FILE);
            java.io.FileWriter writer = new java.io.FileWriter(configFile);
            
            writer.write("# 隐私监控配置文件\n");
            writer.write("# monitor_all=true 表示监控所有应用\n");
            writer.write("# monitor_all=false 表示只监控指定应用\n");
            writer.write("monitor_all=" + monitorAll + "\n");
            writer.write("\n# 指定要监控的应用包名（当monitor_all=false时生效）\n");
            
            for (String packageName : monitoredApps) {
                writer.write("app=" + packageName + "\n");
            }
            
            writer.close();
        } catch (Exception e) {
            // 忽略保存错误
        }
    }
    
    public static boolean shouldMonitorApp(String packageName) {
        loadConfig();
        
        // 输出调试信息
        logDebug("隐私监控-配置检查: " + packageName + 
            " monitorAll=" + monitorAll + " 监控应用数=" + monitoredApps.size());
        
        if (monitorAll) {
            logDebug("隐私监控-全局监控模式: " + packageName);
            return true;
        }
        
        boolean contains = monitoredApps.contains(packageName);
        logDebug("隐私监控-指定应用检查: " + packageName + " 结果=" + contains);
        
        return contains;
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
    
    public static void addMonitoredApp(String packageName) {
        loadConfig();
        monitoredApps.add(packageName);
        saveConfig();
        // 强制重新加载配置
        forceReloadConfig();
    }
    
    public static void removeMonitoredApp(String packageName) {
        loadConfig();
        monitoredApps.remove(packageName);
        saveConfig();
        // 强制重新加载配置
        forceReloadConfig();
    }
    
    public static void setMonitorAll(boolean monitorAll) {
        loadConfig();
        ConfigManager.monitorAll = monitorAll;
        saveConfig();
        // 强制重新加载配置
        forceReloadConfig();
    }
    
    public static boolean isMonitorAll() {
        loadConfig();
        return monitorAll;
    }
    
    public static Set<String> getMonitoredApps() {
        loadConfig();
        return new HashSet<>(monitoredApps);
    }
    
    public static void clearMonitoredApps() {
        loadConfig();
        monitoredApps.clear();
        saveConfig();
    }
    
    // 强制重新加载配置
    public static void forceReloadConfig() {
        configLoaded = false;
        monitoredApps.clear();
        monitorAll = false;
        loadConfig();
        logDebug("隐私监控-强制重新加载配置完成: monitorAll=" + monitorAll + ", 监控应用数=" + monitoredApps.size());
    }
    
    // 强制创建目录的方法（与LogManager中的方法相同）
    private static boolean createDirectoryForcefully(java.io.File dir) {
        try {
            // 方法1: 标准创建
            if (dir.mkdirs()) {
                return true;
            }
            
            // 方法2: 逐级创建
            java.io.File parent = dir.getParentFile();
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
                        logDebug("隐私监控-通过命令创建配置目录成功: " + dir.getAbsolutePath());
                        return dir.exists();
                    }
                } catch (Exception e) {
                    logDebug("隐私监控-命令创建配置目录失败: " + e.getMessage());
                }
            }
            
            return false;
            
        } catch (Exception e) {
            logDebug("隐私监控-强制创建配置目录异常: " + e.getMessage());
            return false;
        }
    }
}