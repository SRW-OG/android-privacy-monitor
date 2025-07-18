package com.privacy.monitor;

import java.util.HashMap;
import java.util.Map;

public class SdkDetector {
    
    private static final Map<String, String> SDK_MAP = new HashMap<>();
    
    static {
        // 初始化SDK映射表（基于你提供的数据）
        initSdkMap();
    }
    
    // 根据包名或类名检测SDK
    public static String detectSdk(String packageName, String className) {
        if (className != null) {
            // 优先检查类名
            for (Map.Entry<String, String> entry : SDK_MAP.entrySet()) {
                if (className.startsWith(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        
        if (packageName != null) {
            // 检查包名
            for (Map.Entry<String, String> entry : SDK_MAP.entrySet()) {
                if (packageName.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        
        return null; // 未检测到已知SDK
    }
    
    // 从调用栈中检测SDK
    public static String detectSdkFromStackTrace(String stackTrace) {
        if (stackTrace == null || stackTrace.isEmpty()) {
            return null;
        }
        
        // 从调用栈中提取类名并检测SDK
        String[] lines = stackTrace.split(" ");
        for (String line : lines) {
            if (line.contains(".")) {
                // 提取类名
                String className = extractClassName(line);
                if (className != null) {
                    String sdk = detectSdk(null, className);
                    if (sdk != null) {
                        return sdk;
                    }
                }
            }
        }
        
        return null;
    }
    
    // 从调用栈行中提取类名
    private static String extractClassName(String stackLine) {
        try {
            // 格式: com.example.Class.method(File.java:123)
            if (stackLine.contains("(") && stackLine.contains(")")) {
                String beforeParen = stackLine.substring(0, stackLine.indexOf("("));
                if (beforeParen.contains(".")) {
                    // 提取完整的类名（包含包名）
                    return beforeParen.trim();
                }
            }
            
            // 如果没有括号，直接尝试提取类名
            if (stackLine.contains(".") && !stackLine.contains(" ")) {
                return stackLine.trim();
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
        return null;
    }
    
    // 增强的 SDK 检测方法，支持更灵活的匹配
    public static String detectSdkEnhanced(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        
        // 检查完整匹配
        for (Map.Entry<String, String> entry : SDK_MAP.entrySet()) {
            if (input.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        // 特殊处理一些常见的 SDK 模式
        if (input.contains("alibaba")) {
            if (input.contains("one")) return "阿里ONE SDK";
            if (input.contains("fastjson")) return "阿里FastJSON";
            if (input.contains("mtl")) return "阿里移动数据分析";
            return "阿里系SDK";
        }
        
        if (input.contains("tencent")) {
            if (input.contains("stat")) return "腾讯统计";
            if (input.contains("bugly")) return "腾讯Bugly";
            if (input.contains("map")) return "腾讯地图";
            return "腾讯系SDK";
        }
        
        if (input.contains("baidu")) {
            if (input.contains("location")) return "百度定位";
            if (input.contains("map")) return "百度地图";
            if (input.contains("mobstat")) return "百度统计";
            return "百度系SDK";
        }
        
        if (input.contains("umeng")) {
            if (input.contains("analytics")) return "友盟统计";
            if (input.contains("message")) return "友盟推送";
            return "友盟系SDK";
        }
        
        return null;
    }
    
    // 检查是否为系统相关的调用
    public static boolean isSystemCall(String stackTrace) {
        if (stackTrace == null) return false;
        
        // 检查是否包含框架相关的调用
        String[] frameworkKeywords = {
            // Xposed 相关
            "XposedInit", "XposedBridge", "XposedHelpers",
            "LSPosed", "LSPHooker", "MaQZpylZXhHvYoEJG",
            "IXposedHookLoadPackage", "LegacyApiSupport",
            "NativeHooker", "VMStack.getThreadStackTrace",
            "com.privacy.monitor.", "dalvik.system.VMStack",
            "org.lsposed.lspd", "handleBefore", "handleAfter",
            "callback(null:", "SourceFile:", "-$$Nest$sm",
            
            // Frida 相关
            "frida", "Frida", "FRIDA",
            "com.frida.", "re.frida.", "frida.runtime",
            "frida_agent_main", "frida_rpc", "frida_script",
            "_frida_", "FridaScript", "FridaAgent",
            "gum_", "gumjs_", "GumJS", "GumInvocationListener",
            "Java.perform", "Java.use", "Java.choose",
            "Interceptor.attach", "Interceptor.replace",
            "NativeFunction", "NativePointer", "Memory.alloc"
        };
        
        for (String keyword : frameworkKeywords) {
            if (stackTrace.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    // 检查是否为重要的隐私操作（扩展版本，覆盖更多隐私合规场景）
    public static boolean isImportantPrivacyAction(String action, String detail) {
        if (action == null) return false;
        
        // 高风险隐私操作（必须记录）
        String[] highRiskActions = {
            "IMEI", "设备ID", "序列号", "Android ID", "MAC地址", "BSSID",
            "位置", "GPS", "定位", "经纬度", "地理位置",
            "通讯录", "联系人", "短信", "通话记录", "电话号码", "手机号",
            "相机", "录音", "麦克风", "拍照", "录制", "音频", "视频",
            "权限申请", "敏感权限", "危险权限",
            "剪贴板", "粘贴板", "复制", "粘贴",
            "传感器", "加速度", "陀螺仪", "指纹", "生物识别"
        };
        
        // 中风险操作（隐私合规需要关注）
        String[] mediumRiskActions = {
            "网络状态", "WiFi信息", "蓝牙", "运营商", "网络运营商",
            "设备信息", "系统版本", "硬件信息", "设备型号", "品牌",
            "应用列表", "安装包", "已安装应用",
            "存储", "文件访问", "外部存储", "SD卡",
            "屏幕", "截屏", "录屏", "显示",
            "电池", "充电状态", "电量",
            "SIM卡", "ICCID", "运营商信息"
        };
        
        // 低风险但需要记录的操作（用于完整的隐私合规审计）
        String[] lowRiskActions = {
            "网络", "HTTP", "HTTPS", "连接",
            "时间", "时区", "语言", "区域设置",
            "内存", "CPU", "进程", "线程"
        };
        
        // 检查高风险操作
        for (String riskAction : highRiskActions) {
            if (action.contains(riskAction) || (detail != null && detail.contains(riskAction))) {
                return true;
            }
        }
        
        // 检查中风险操作
        for (String riskAction : mediumRiskActions) {
            if (action.contains(riskAction) || (detail != null && detail.contains(riskAction))) {
                return true;
            }
        }
        
        // 检查低风险操作
        for (String riskAction : lowRiskActions) {
            if (action.contains(riskAction) || (detail != null && detail.contains(riskAction))) {
                return true;
            }
        }
        
        // 检查详细信息中的敏感内容
        if (detail != null) {
            // 检查敏感路径访问
            if (action.contains("文件访问") || action.contains("存储")) {
                return isSensitiveStorageAccess(detail);
            }
            
            // 检查URL中的敏感信息
            if (detail.contains("http") || detail.contains("url")) {
                return isSensitiveUrlAccess(detail);
            }
        }
        
        return false;
    }
    
    // 检查是否为敏感的URL访问
    private static boolean isSensitiveUrlAccess(String detail) {
        if (detail == null) return false;
        
        String lowerDetail = detail.toLowerCase();
        
        // 敏感URL关键词
        String[] sensitiveUrlKeywords = {
            "api", "login", "auth", "token", "key", "secret",
            "user", "profile", "account", "personal", "private",
            "location", "gps", "position", "coordinate",
            "upload", "download", "sync", "backup",
            "analytics", "track", "collect", "report", "stat"
        };
        
        for (String keyword : sensitiveUrlKeywords) {
            if (lowerDetail.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    // 检查是否为敏感的存储访问
    private static boolean isSensitiveStorageAccess(String detail) {
        if (detail == null) return false;
        
        // 敏感路径关键词
        String[] sensitivePaths = {
            "/data/data/", "/databases/", "/shared_prefs/",
            "/cache/", "/files/", "password", "token", "key",
            "account", "login", "user", "profile", "contact",
            "message", "sms", "call", "photo", "image", "video"
        };
        
        String lowerDetail = detail.toLowerCase();
        for (String sensitivePath : sensitivePaths) {
            if (lowerDetail.contains(sensitivePath.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    private static void initSdkMap() {
        // 腾讯系列
        SDK_MAP.put("com.tencent.stat", "腾讯统计");
        SDK_MAP.put("com.tencent.bugly", "腾讯Bugly");
        SDK_MAP.put("com.tencent.mm.sdk", "微信支付");
        SDK_MAP.put("com.tencent.map", "腾讯地图Android SDK");
        SDK_MAP.put("com.tencent.android.tpush", "腾讯信鸽推送");
        SDK_MAP.put("com.tencent.smtt", "腾讯X5Web");
        SDK_MAP.put("com.tencent.connect", "腾讯QQ互联");
        SDK_MAP.put("com.tencent.tauth", "腾讯QQ互联");
        SDK_MAP.put("com.tencent.midas", "腾讯QQ钱包");
        SDK_MAP.put("com.tencent.mid.api", "腾讯MTA");
        
        // 百度系列
        SDK_MAP.put("com.baidu.mobstat", "百度移动统计SDK");
        SDK_MAP.put("com.baidu.location", "百度定位");
        SDK_MAP.put("com.baidu.mapapi", "百度地图");
        SDK_MAP.put("com.baidu.android.pushservice", "百度云推送");
        SDK_MAP.put("com.baidu.mobads", "百度移动推广SDK");
        SDK_MAP.put("com.baidu.lbsapi", "百度Android全景SDK");
        SDK_MAP.put("com.baidu.tts", "百度语音");
        
        // 阿里系列
        SDK_MAP.put("com.alipay.sdk", "支付宝支付");
        SDK_MAP.put("com.taobao.agoo", "淘宝推送");
        SDK_MAP.put("com.alibaba.sdk", "支付宝 Nebula");
        SDK_MAP.put("com.taobao.accs", "淘宝推送");
        SDK_MAP.put("com.alipay.mobile.scan", "支付宝扫一扫");
        SDK_MAP.put("com.alibaba.one", "阿里巴巴百川SDK");
        SDK_MAP.put("com.alibaba.baichuan", "阿里巴巴百川SDK");
        SDK_MAP.put("com.alibaba.android", "阿里Android SDK");
        SDK_MAP.put("com.alibaba.fastjson", "阿里FastJSON");
        SDK_MAP.put("com.alibaba.mtl", "阿里移动数据分析");
        SDK_MAP.put("com.alibaba.wireless", "阿里无线SDK");
        SDK_MAP.put("com.alibaba.security", "阿里安全SDK");
        SDK_MAP.put("com.alibaba.analytics", "阿里数据分析SDK");
        SDK_MAP.put("com.alibaba.ha", "阿里移动热修复");
        SDK_MAP.put("com.alibaba.cloudapi", "阿里云API网关");
        
        // 友盟系列
        SDK_MAP.put("com.umeng.analytics", "友盟统计分析平台");
        SDK_MAP.put("com.umeng.message", "友盟推送");
        SDK_MAP.put("com.umeng.socialize", "友盟社会化组件");
        SDK_MAP.put("com.umeng.fb", "友盟反馈");
        SDK_MAP.put("com.umeng.update", "友盟自动更新");
        
        // 极光推送
        SDK_MAP.put("cn.jpush.android", "极光推送");
        SDK_MAP.put("cn.jiguang", "极光推送");
        
        // 个推
        SDK_MAP.put("com.igexin.sdk", "个推");
        SDK_MAP.put("com.getui.gtc", "个推一键登录");
        SDK_MAP.put("com.geetest.onelogin", "个推一键登录");
        
        // 华为系列
        SDK_MAP.put("com.huawei.hms", "华为HMS");
        SDK_MAP.put("com.huawei.push", "华为推送");
        SDK_MAP.put("com.huawei.android.pushagent", "华为推送");
        SDK_MAP.put("com.huawei.agconnect", "华为推送");
        SDK_MAP.put("com.huawei.pay.ui", "华为支付");
        
        // 小米系列
        SDK_MAP.put("com.xiaomi.push", "小米推送");
        SDK_MAP.put("com.xiaomi.mipush", "小米推送");
        SDK_MAP.put("com.xiaomi.account", "小米帐号开放平台");
        SDK_MAP.put("com.xiaomi.market.sdk", "小米应用商店sdk");
        
        // OPPO/VIVO
        SDK_MAP.put("com.heytap.mcssdk", "OPPO 推送");
        SDK_MAP.put("com.coloros.mcssdk", "OPPO 推送");
        SDK_MAP.put("com.vivo.push", "Vivo 推送");
        
        // 网络库
        SDK_MAP.put("okhttp3", "OkHttp3");
        SDK_MAP.put("com.squareup.okhttp", "OkHttp");
        SDK_MAP.put("retrofit2", "Retrofit");
        SDK_MAP.put("com.android.volley", "Volley");
        SDK_MAP.put("com.loopj.android.http", "Android Async HTTP");
        
        // 图片库
        SDK_MAP.put("com.bumptech.glide", "Glide图片加载");
        SDK_MAP.put("com.squareup.picasso", "Picasso图片加载");
        SDK_MAP.put("com.facebook.imagepipeline", "Fresco图片加载");
        SDK_MAP.put("com.nostra13.universalimageloader", "Universal Image Loader");
        
        // 数据库
        SDK_MAP.put("org.litepal", "LitePal数据库");
        SDK_MAP.put("de.greenrobot.dao", "GreenDAO");
        SDK_MAP.put("io.realm", "Realm数据库");
        SDK_MAP.put("net.sqlcipher", "SQLCipher加密数据库");
        
        // JSON解析
        SDK_MAP.put("com.google.gson", "Gson");
        SDK_MAP.put("com.alibaba.fastjson", "FastJSON");
        SDK_MAP.put("com.fasterxml.jackson", "Jackson");
        
        // 事件总线
        SDK_MAP.put("de.greenrobot.event", "EventBus");
        SDK_MAP.put("com.squareup.otto", "Otto");
        
        // 谷歌系列
        SDK_MAP.put("com.google.firebase", "Firebase");
        SDK_MAP.put("com.google.android.gms", "Google Play Services");
        SDK_MAP.put("com.google.analytics", "Google Analytics");
        SDK_MAP.put("com.google.zxing", "ZXing二维码");
        
        // Facebook系列
        SDK_MAP.put("com.facebook.sdk", "Facebook SDK");
        SDK_MAP.put("com.facebook.react", "React Native");
        
        // 微博
        SDK_MAP.put("com.sina.weibo.sdk", "新浪微博SDK");
        
        // 语音识别
        SDK_MAP.put("com.iflytek", "科大讯飞SDK");
        SDK_MAP.put("com.baidu.speech", "百度语音识别");
        
        // 地图定位
        SDK_MAP.put("com.amap.api", "高德地图SDK");
        SDK_MAP.put("com.tencent.map.geolocation", "腾讯地图定位SDK");
        
        // 支付相关
        SDK_MAP.put("com.jdpaysdk", "京东支付");
        SDK_MAP.put("com.yeepay.android", "易宝支付");
        SDK_MAP.put("com.pingplusplus", "Ping++支付");
        
        // 直播推流
        SDK_MAP.put("com.qiniu.android", "七牛云存储");
        SDK_MAP.put("com.tencent.rtmp", "腾讯云直播");
        
        // 崩溃统计
        SDK_MAP.put("com.crashlytics", "Crashlytics");
        SDK_MAP.put("com.bugsnag.android", "Bugsnag");
        
        // 其他常用SDK
        SDK_MAP.put("org.apache.cordova", "Cordova");
        SDK_MAP.put("com.unity3d.player", "Unity3D");
        SDK_MAP.put("io.dcloud", "DCloud");
        SDK_MAP.put("com.sensorsdata.analytics", "神策数据");
        SDK_MAP.put("com.talkingdata.sdk", "TalkingData");
        SDK_MAP.put("com.flurry.android", "Flurry");
        
        // 添加更多常见的第三方库
        SDK_MAP.put("androidx", "AndroidX支持库");
        SDK_MAP.put("android.support", "Android支持库");
        SDK_MAP.put("com.github", "GitHub开源库");
    }
}