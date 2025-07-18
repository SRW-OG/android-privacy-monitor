package com.privacy.monitor;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class SimpleMainActivity extends Activity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        TextView textView = new TextView(this);
        textView.setText("隐私监控模块\n\n" +
                        "状态: 已安装\n" +
                        "版本: 1.0\n\n" +
                        "请在LSPosed中激活此模块，\n" +
                        "然后重启设备使其生效。\n\n" +
                        "监控日志将保存在:\n" +
                        "/sdcard/PrivacyMonitor/");
        textView.setPadding(30, 30, 30, 30);
        textView.setTextSize(16);
        
        setContentView(textView);
    }
}