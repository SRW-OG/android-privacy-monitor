package com.privacy.monitor;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Button;
import android.view.View;
import android.widget.Toast;

public class TestActivity extends Activity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            createSimpleUI();
        } catch (Exception e) {
            // 创建最基本的UI
            TextView errorView = new TextView(this);
            errorView.setText("应用启动错误: " + e.getMessage());
            errorView.setPadding(20, 20, 20, 20);
            setContentView(errorView);
        }
    }
    
    private void createSimpleUI() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);
        
        // 标题
        TextView titleView = new TextView(this);
        titleView.setText("隐私监控模块 - 测试版");
        titleView.setTextSize(18);
        titleView.setPadding(0, 0, 0, 20);
        
        // 状态信息
        TextView statusView = new TextView(this);
        statusView.setText("模块状态: 已安装\nXposed状态: 请在LSPosed中激活");
        statusView.setTextSize(14);
        statusView.setPadding(0, 0, 0, 20);
        
        // 测试按钮
        Button testButton = new Button(this);
        testButton.setText("测试配置");
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testConfiguration();
            }
        });
        
        // 说明文本
        TextView infoView = new TextView(this);
        infoView.setText("这是隐私监控模块的测试版本。\n\n" +
                        "功能:\n" +
                        "• 监控应用权限使用\n" +
                        "• 记录隐私数据访问\n" +
                        "• 生成CSV格式日志\n\n" +
                        "使用方法:\n" +
                        "1. 在LSPosed中激活模块\n" +
                        "2. 重启设备\n" +
                        "3. 配置要监控的应用");
        infoView.setTextSize(12);
        infoView.setPadding(0, 0, 0, 20);
        
        layout.addView(titleView);
        layout.addView(statusView);
        layout.addView(testButton);
        layout.addView(infoView);
        
        setContentView(layout);
    }
    
    private void testConfiguration() {
        try {
            // 测试配置管理器
            boolean monitorAll = ConfigManager.isMonitorAll();
            int appCount = ConfigManager.getMonitoredApps().size();
            
            String message = "配置测试结果:\n" +
                           "全局监控: " + monitorAll + "\n" +
                           "监控应用数: " + appCount;
            
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            
        } catch (Exception e) {
            Toast.makeText(this, "配置测试失败: " + e.getMessage(), 
                         Toast.LENGTH_LONG).show();
        }
    }
}