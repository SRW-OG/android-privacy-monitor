package com.privacy.monitor;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.*;
import android.view.View;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {
    
    private static final int REQUEST_STORAGE_PERMISSION = 1001;
    private TextView statusView;
    private TextView configView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 检查并申请存储权限
        checkStoragePermission();
        
        try {
            createUI();
        } catch (Exception e) {
            // 如果UI创建失败，显示简单的错误信息
            showSimpleErrorUI(e);
        }
    }
    
    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 需要管理外部存储权限
            if (!Environment.isExternalStorageManager()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("需要存储权限");
                builder.setMessage("为了保存监控日志，需要授予管理外部存储权限。\n\n点击确定跳转到设置页面。");
                builder.setPositiveButton("确定", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        try {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "无法打开设置页面", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                builder.setNegativeButton("取消", null);
                builder.show();
            } else {
                createLogDirectory();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }, REQUEST_STORAGE_PERMISSION);
            } else {
                createLogDirectory();
            }
        } else {
            createLogDirectory();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "存储权限已授予，可以保存日志文件", Toast.LENGTH_SHORT).show();
                // 权限获得后，尝试创建目录
                createLogDirectory();
            } else {
                Toast.makeText(this, "存储权限被拒绝，无法保存日志文件", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void createLogDirectory() {
        try {
            // 尝试多个可能的路径
            String[] possiblePaths = {
                "/sdcard/PrivacyMonitor/",
                Environment.getExternalStorageDirectory() + "/PrivacyMonitor/",
                getExternalFilesDir(null) + "/PrivacyMonitor/"
            };
            
            boolean success = false;
            String successPath = "";
            
            for (String path : possiblePaths) {
                try {
                    File logDir = new File(path);
                    if (!logDir.exists()) {
                        success = logDir.mkdirs();
                    } else {
                        success = logDir.canWrite();
                    }
                    
                    if (success) {
                        successPath = path;
                        break;
                    }
                } catch (Exception e) {
                    // 尝试下一个路径
                    continue;
                }
            }
            
            if (success) {
                Toast.makeText(this, "日志目录就绪: " + successPath, Toast.LENGTH_SHORT).show();
                // 更新ConfigManager中的路径
                updateLogPath(successPath);
            } else {
                Toast.makeText(this, "无法创建日志目录，请检查存储权限", Toast.LENGTH_LONG).show();
            }
            
        } catch (Exception e) {
            Toast.makeText(this, "创建日志目录时出错: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void updateLogPath(String path) {
        // 这里可以通知ConfigManager和LogManager使用新的路径
        // 暂时先记录日志
        android.util.Log.d("PrivacyMonitor", "日志目录设置为: " + path);
    }
    
    private void createUI() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 30, 30, 30);
        
        // 标题
        TextView titleView = new TextView(this);
        titleView.setText("Android隐私合规监控模块");
        titleView.setTextSize(20);
        titleView.setPadding(0, 0, 0, 20);
        
        // 状态信息
        statusView = new TextView(this);
        statusView.setTextSize(14);
        statusView.setPadding(0, 0, 0, 15);
        
        // 配置信息
        configView = new TextView(this);
        configView.setTextSize(12);
        configView.setPadding(0, 0, 0, 20);
        
        // 按钮布局
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setPadding(0, 0, 0, 20);
        
        // 启用/禁用监控按钮
        Button enableButton = new Button(this);
        enableButton.setText("启用监控");
        enableButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    enableMonitoring();
                } catch (Exception e) {
                    showError("启用监控失败", e);
                }
            }
        });
        
        // 配置应用按钮
        Button configButton = new Button(this);
        configButton.setText("配置应用");
        configButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    showAppSelectionDialog();
                } catch (Exception e) {
                    showError("配置应用失败", e);
                }
            }
        });
        
        // 查看日志按钮
        Button logButton = new Button(this);
        logButton.setText("查看日志");
        logButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    showLogInfo();
                } catch (Exception e) {
                    showError("查看日志失败", e);
                }
            }
        });
        
        // 测试监控按钮
        Button testButton = new Button(this);
        testButton.setText("测试监控");
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    testMonitoring();
                } catch (Exception e) {
                    showError("测试监控失败", e);
                }
            }
        });
        
        buttonLayout.addView(enableButton);
        buttonLayout.addView(configButton);
        buttonLayout.addView(logButton);
        buttonLayout.addView(testButton);
        
        // 监控功能说明
        TextView infoView = new TextView(this);
        infoView.setText("监控功能包括:\n" +
                        "• 权限申请和检查\n" +
                        "• 设备信息获取 (IMEI, Android ID等)\n" +
                        "• 位置信息访问\n" +
                        "• 通讯录和短信访问\n" +
                        "• 存储文件访问\n" +
                        "• 相机和麦克风使用\n" +
                        "• 网络和设备标识符\n\n" +
                        "日志格式: CSV文件\n" +
                        "保存路径: /sdcard/PrivacyMonitor/\n" +
                        "文件命名: 包名_日期.csv");
        infoView.setTextSize(12);
        infoView.setPadding(0, 0, 0, 20);
        
        layout.addView(titleView);
        layout.addView(statusView);
        layout.addView(configView);
        layout.addView(buttonLayout);
        layout.addView(infoView);
        
        setContentView(layout);
        
        // 更新状态显示
        try {
            updateStatus();
        } catch (Exception e) {
            statusView.setText("状态获取失败: " + e.getMessage());
        }
    }
    
    private void showSimpleErrorUI(Exception e) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 30, 30, 30);
        
        TextView titleView = new TextView(this);
        titleView.setText("隐私监控模块");
        titleView.setTextSize(18);
        titleView.setPadding(0, 0, 0, 20);
        
        TextView errorView = new TextView(this);
        errorView.setText("应用启动时发生错误:\n" + e.getMessage() + 
                         "\n\n模块可能仍然正常工作，请检查LSPosed日志。");
        errorView.setTextSize(14);
        
        layout.addView(titleView);
        layout.addView(errorView);
        
        setContentView(layout);
    }
    
    private void showError(String title, Exception e) {
        try {
            Toast.makeText(this, title + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            // 如果Toast也失败了，就忽略
        }
    }
    
    private void updateStatus() {
        boolean monitorAll = ConfigManager.isMonitorAll();
        Set<String> monitoredApps = ConfigManager.getMonitoredApps();
        
        if (monitorAll) {
            statusView.setText("状态: 监控所有第三方应用");
            configView.setText("配置: 全局监控模式");
        } else if (!monitoredApps.isEmpty()) {
            statusView.setText("状态: 监控指定应用 (" + monitoredApps.size() + "个)");
            StringBuilder sb = new StringBuilder("监控应用: ");
            int count = 0;
            for (String pkg : monitoredApps) {
                if (count > 0) sb.append(", ");
                sb.append(pkg);
                count++;
                if (count >= 3) {
                    sb.append("...");
                    break;
                }
            }
            configView.setText(sb.toString());
        } else {
            statusView.setText("状态: 监控已禁用");
            configView.setText("配置: 请启用监控或选择要监控的应用");
        }
    }
    
    private void enableMonitoring() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("启用监控");
        
        String[] options = {"监控所有第三方应用", "仅监控指定应用", "禁用监控"};
        int currentSelection = ConfigManager.isMonitorAll() ? 0 : 
                              (ConfigManager.getMonitoredApps().isEmpty() ? 2 : 1);
        
        builder.setSingleChoiceItems(options, currentSelection, null);
        
        builder.setPositiveButton("确定", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                ListView listView = ((AlertDialog) dialog).getListView();
                int selectedPosition = listView.getCheckedItemPosition();
                
                switch (selectedPosition) {
                    case 0: // 监控所有应用
                        ConfigManager.setMonitorAll(true);
                        break;
                    case 1: // 仅监控指定应用
                        ConfigManager.setMonitorAll(false);
                        if (ConfigManager.getMonitoredApps().isEmpty()) {
                            showAppSelectionDialog();
                        }
                        break;
                    case 2: // 禁用监控
                        ConfigManager.setMonitorAll(false);
                        ConfigManager.clearMonitoredApps();
                        break;
                }
                updateStatus();
                Toast.makeText(MainActivity.this, "配置已保存", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("取消", null);
        builder.show();
    }
    
    private void showAppSelectionDialog() {
        try {
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            
            // 过滤第三方应用
            List<AppInfo> userApps = new ArrayList<>();
            for (ApplicationInfo app : apps) {
                if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    String appName = pm.getApplicationLabel(app).toString();
                    userApps.add(new AppInfo(app.packageName, appName));
                }
            }
            
            Collections.sort(userApps, new java.util.Comparator<AppInfo>() {
                @Override
                public int compare(AppInfo a, AppInfo b) {
                    return a.name.compareToIgnoreCase(b.name);
                }
            });
            
            Set<String> monitoredApps = ConfigManager.getMonitoredApps();
            String[] appNames = new String[userApps.size()];
            boolean[] checkedItems = new boolean[userApps.size()];
            
            for (int i = 0; i < userApps.size(); i++) {
                AppInfo appInfo = userApps.get(i);
                appNames[i] = appInfo.name + "\n(" + appInfo.packageName + ")";
                checkedItems[i] = monitoredApps.contains(appInfo.packageName);
            }
            
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("选择要监控的应用");
            builder.setMultiChoiceItems(appNames, checkedItems, 
                new android.content.DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which, boolean isChecked) {
                        // 处理选择变化
                    }
                });
            
            builder.setPositiveButton("确定", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    ConfigManager.clearMonitoredApps();
                    for (int i = 0; i < checkedItems.length; i++) {
                        if (checkedItems[i]) {
                            ConfigManager.addMonitoredApp(userApps.get(i).packageName);
                        }
                    }
                    updateStatus();
                    Toast.makeText(MainActivity.this, "应用选择已保存", Toast.LENGTH_SHORT).show();
                }
            });
            
            builder.setNegativeButton("取消", null);
            builder.show();
            
        } catch (Exception e) {
            Toast.makeText(this, "获取应用列表失败: " + e.getMessage(), 
                         Toast.LENGTH_LONG).show();
        }
    }
    
    private void showLogInfo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("监控日志信息");
        
        StringBuilder info = new StringBuilder();
        info.append("日志保存位置: /sdcard/PrivacyMonitor/\n\n");
        
        File logDir = new File("/sdcard/PrivacyMonitor/");
        if (logDir.exists()) {
            File[] files = logDir.listFiles(new java.io.FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".csv");
                }
            });
            if (files != null && files.length > 0) {
                info.append("已生成的日志文件:\n");
                for (File file : files) {
                    info.append("• ").append(file.getName()).append("\n");
                }
            } else {
                info.append("暂无日志文件\n请先运行一些应用生成日志");
            }
        } else {
            info.append("日志目录不存在\n请先启用监控并运行应用");
        }
        
        builder.setMessage(info.toString());
        builder.setPositiveButton("确定", null);
        builder.show();
    }
    
    private void testMonitoring() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("测试监控功能");
        builder.setMessage("这将触发一些测试事件来验证监控系统是否正常工作。\n\n测试内容包括:\n• 写入测试日志\n• 检查权限状态\n• 获取设备信息\n• 测试网络连接");
        
        builder.setPositiveButton("开始测试", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                performMonitoringTest();
            }
        });
        
        builder.setNegativeButton("取消", null);
        builder.show();
    }
    
    private void performMonitoringTest() {
        try {
            // 1. 写入测试日志
            LogManager.writeDetailedLog("com.privacy.monitor.test", "手动测试", 
                                      "用户触发的监控测试", "MainActivity.performMonitoringTest", 
                                      "手动测试调用栈");
            
            // 2. 检查一些权限状态
            try {
                int cameraPermission = checkSelfPermission(Manifest.permission.CAMERA);
                LogManager.writeDetailedLog("com.privacy.monitor.test", "权限检查", 
                                          "相机权限状态: " + (cameraPermission == PackageManager.PERMISSION_GRANTED ? "已授权" : "未授权"), 
                                          "MainActivity.checkSelfPermission", "权限测试");
                
                int locationPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
                LogManager.writeDetailedLog("com.privacy.monitor.test", "权限检查", 
                                          "位置权限状态: " + (locationPermission == PackageManager.PERMISSION_GRANTED ? "已授权" : "未授权"), 
                                          "MainActivity.checkSelfPermission", "权限测试");
            } catch (Exception e) {
                LogManager.writeDetailedLog("com.privacy.monitor.test", "权限检查失败", 
                                          "错误: " + e.getMessage(), "MainActivity.checkSelfPermission", "权限测试异常");
            }
            
            // 3. 获取一些基本的设备信息
            try {
                String androidId = android.provider.Settings.Secure.getString(getContentResolver(), 
                                                                            android.provider.Settings.Secure.ANDROID_ID);
                LogManager.writeDetailedLog("com.privacy.monitor.test", "设备信息", 
                                          "Android ID: " + (androidId != null ? "已获取" : "未获取"), 
                                          "Settings.Secure.getString", "设备信息测试");
            } catch (Exception e) {
                LogManager.writeDetailedLog("com.privacy.monitor.test", "设备信息获取失败", 
                                          "错误: " + e.getMessage(), "Settings.Secure.getString", "设备信息测试异常");
            }
            
            // 4. 测试网络连接状态
            try {
                android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                if (cm != null) {
                    android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                    String networkStatus = activeNetwork != null ? activeNetwork.getTypeName() : "无网络";
                    LogManager.writeDetailedLog("com.privacy.monitor.test", "网络状态", 
                                              "网络类型: " + networkStatus, 
                                              "ConnectivityManager.getActiveNetworkInfo", "网络测试");
                }
            } catch (Exception e) {
                LogManager.writeDetailedLog("com.privacy.monitor.test", "网络状态获取失败", 
                                          "错误: " + e.getMessage(), "ConnectivityManager.getActiveNetworkInfo", "网络测试异常");
            }
            
            // 5. 显示测试结果
            Toast.makeText(this, "监控测试完成！请查看日志文件验证结果。", Toast.LENGTH_LONG).show();
            
            // 6. 自动显示日志信息
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    showLogInfo();
                }
            }, 1000);
            
        } catch (Exception e) {
            Toast.makeText(this, "测试失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }
    
    private static class AppInfo {
        String packageName;
        String name;
        
        AppInfo(String packageName, String name) {
            this.packageName = packageName;
            this.name = name;
        }
    }
}