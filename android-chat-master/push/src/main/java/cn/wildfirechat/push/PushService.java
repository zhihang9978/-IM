/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.push;

import static com.google.firebase.messaging.Constants.MessageNotificationKeys.TAG;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.heytap.mcssdk.PushManager;
import com.heytap.mcssdk.callback.PushCallback;
import com.heytap.mcssdk.mode.SubscribeResult;
import com.huawei.agconnect.config.AGConnectServicesConfig;
import com.huawei.hms.aaid.HmsInstanceId;
import com.huawei.hms.api.HuaweiApiClient;
import com.huawei.hms.common.ApiException;
import com.meizu.cloud.pushsdk.util.MzSystemUtils;
import com.vivo.push.IPushActionListener;
import com.vivo.push.PushClient;
import com.xiaomi.mipush.sdk.MiPushClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

import cn.wildfirechat.remote.ChatManager;

/**
 * Created by heavyrain.lee on 2018/2/26.
 */

public class PushService {
    private HuaweiApiClient HMSClient;
    private boolean hasHMSToken;
    private int pushServiceType;
    private static PushService INST = new PushService();
    private static String applicationId;

    public interface PushServiceType {
        int Unknown = 0;
        int Xiaomi = 1;
        int HMS = 2;
        int MeiZu = 3;
        int VIVO = 4;
        int OPPO = 5;
        int Google = 6;
        int GeTui = 7;
    }

    public static void init(Application gContext, String applicationId) {
        PushService.applicationId = applicationId;
        String sys = getSystem();
        if (SYS_EMUI.equals(sys)) {
            INST.pushServiceType = PushServiceType.HMS;
            INST.initHMS(gContext);
        } else if (/*SYS_FLYME.equals(sys) && INST.isMZConfigured(gContext)*/MzSystemUtils.isBrandMeizu()) {
            INST.pushServiceType = PushServiceType.MeiZu;
            INST.initMZ(gContext);
        } else if (SYS_VIVO.equalsIgnoreCase(sys)) {
            INST.pushServiceType = PushServiceType.VIVO;
            INST.initVIVO(gContext);
        } else if (PushManager.isSupportPush(gContext)) {
            INST.pushServiceType = PushServiceType.OPPO;
            INST.initOPPO(gContext);
        } else if (SYS_MIUI.equals(sys) && INST.isXiaomiConfigured(gContext)) {
            INST.pushServiceType = PushServiceType.Xiaomi;
            INST.initXiaomi(gContext);
        } else if (useGoogleFCM(gContext)) {
            INST.pushServiceType = PushServiceType.Google;
            INST.initFCM(gContext);
        } else {
            //其它使用小米推送
            INST.pushServiceType = PushServiceType.Xiaomi;
            INST.initXiaomi(gContext);
        }

        ProcessLifecycleOwner.get().getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            public void onForeground() {
                clearNotification(gContext);
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            public void onBackground() {
            }
        });

    }

    private static boolean useGoogleFCM(Context context) {
        if (hasGooglePlayServices(context)) { //判断是否支持google服务框架
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String no = tm.getNetworkOperator();
            String simNo = tm.getSimOperator();

            if (!TextUtils.isEmpty(no) && TextUtils.isEmpty(simNo)) {
                if (no.startsWith("460") && simNo.startsWith("460")) {
                    //当sim卡和网络都是大陆运营商时，不能使用fcm
                    return false;
                } else {
                    //当sim卡和网络有一个不是大陆运营商时，可以使用fcm
                    return true;
                }
            }

            //区域是中国大陆简体中文且是中国标准时区，不用fcm
            Locale locale = context.getResources().getConfiguration().locale;
            if ("zh".equals(locale.getLanguage()) && "CN".equals(locale.getCountry()) && "Asia/Shanghai".equals(TimeZone.getDefault().getID())) {
                return false;
            }

            return true;
        }
        return false;
    }

    private static void clearNotification(Context context) {
        if (INST.pushServiceType == PushServiceType.Xiaomi) {
            MiPushClient.clearNotification(context);
        } else {
            // TODO
        }
    }

    public static void showMainActivity(Context context) {
        String action = applicationId + ".main";
        Intent intent = new Intent(action);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void didReceiveIMPushMessage(Context context, AndroidPushMessage pushMessage, int pushServiceType) {
        PushMessageHandler.didReceiveIMPushMessage(context, pushMessage, pushServiceType);
    }

    public static void didReceivePushMessageData(Context context, String pushData) {
        PushMessageHandler.didReceivePushMessageData(context, pushData);
    }

    public static void destroy() {
        if (INST.HMSClient != null) {
            INST.HMSClient.disconnect();
            INST.HMSClient = null;
        }
    }

    private boolean initXiaomi(Context context) {

        String packageName = context.getPackageName();
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                String appid = appInfo.metaData.getString("MIPUSH_APPID").substring(7);
                String appkey = appInfo.metaData.getString("MIPUSH_APPKEY").substring(7);
                if (!TextUtils.isEmpty(appid) && !TextUtils.isEmpty(appkey)) {
                    if (shouldInitXiaomi(context)) {
                        MiPushClient.registerPush(context, appid, appkey);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean isXiaomiConfigured(Context context) {
        String packageName = context.getPackageName();
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                String appid = appInfo.metaData.getString("MIPUSH_APPID").substring(7);
                String appkey = appInfo.metaData.getString("MIPUSH_APPKEY").substring(7);
                return !TextUtils.isEmpty(appid) && !TextUtils.isEmpty(appkey);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean shouldInitXiaomi(Context context) {
        ActivityManager am = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE));
        List<ActivityManager.RunningAppProcessInfo> processInfos = am.getRunningAppProcesses();
        String mainProcessName = context.getPackageName();
        int myPid = android.os.Process.myPid();
        for (ActivityManager.RunningAppProcessInfo info : processInfos) {
            if (info.pid == myPid && mainProcessName.equals(info.processName)) {
                return true;
            }
        }
        return false;
    }

    private void initHMS(final Context context) {
        String appId = AGConnectServicesConfig.fromContext(context).getString("client/app_id");
        ChatManager.Instance().getWorkHandler().post(new Runnable() {
            @Override
            public void run() {
                try {
                    String token = HmsInstanceId.getInstance(context).getToken(appId, "HCM");
                    if (!TextUtils.isEmpty(token)) {
                        ChatManager.Instance().setDeviceToken(token, PushServiceType.HMS);
                    }
                } catch (ApiException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private boolean isMZConfigured(Context context) {
        String packageName = context.getPackageName();
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                String appid = appInfo.metaData.getString("MEIZU_APP_ID");
                String appkey = appInfo.metaData.getString("MEIZU_APP_KEY");
                return !TextUtils.isEmpty(appid) && !TextUtils.isEmpty(appkey);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean initMZ(Context context) {
        String packageName = context.getPackageName();
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                String appId = "" + appInfo.metaData.get("MEIZU_PUSH_APP_ID");
                String appKey = appInfo.metaData.getString("MEIZU_PUSH_APP_KEY");
                if (!TextUtils.isEmpty(appId) && !TextUtils.isEmpty(appKey)) {
                    String pushId = com.meizu.cloud.pushsdk.PushManager.getPushId(context);
                    com.meizu.cloud.pushsdk.PushManager.register(context, String.valueOf(appId), appKey);
                    com.meizu.cloud.pushsdk.PushManager.switchPush(context, String.valueOf(appId), appKey, pushId, 1, true);
                    ChatManager.Instance().setDeviceToken(pushId, PushServiceType.MeiZu);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void initVIVO(Context context) {

        // 在当前工程入口函数，建议在Application的onCreate函数中，添加以下代码:
        PushClient.getInstance(context).initialize();
        // 当需要打开推送服务时，调用以下代码:
        PushClient.getInstance(context).turnOnPush(new IPushActionListener() {
            @Override
            public void onStateChanged(int state) {
                Log.d("PushService", "vivo turnOnPush " + state);
                String regId = PushClient.getInstance(context).getRegId();
                if (!TextUtils.isEmpty(regId)) {
                    ChatManager.Instance().setDeviceToken(regId, PushServiceType.VIVO);
                }
            }
        });
    }

    private void initOPPO(Context context) {
        String packageName = context.getPackageName();
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                String appsecret = appInfo.metaData.getString("OPPO_APP_PUSH_SECRET");
                String appkey = appInfo.metaData.getString("OPPO_APP_PUSH_KEY");
                PushManager.getInstance().register(context, appkey, appsecret, new PushCallback() {
                    @Override
                    public void onRegister(int i, String s) {
                        ChatManager.Instance().setDeviceToken(s, PushServiceType.OPPO);
                    }

                    @Override
                    public void onUnRegister(int i) {

                    }

                    @Override
                    public void onSetPushTime(int i, String s) {

                    }

                    @Override
                    public void onGetPushStatus(int i, int i1) {

                    }

                    @Override
                    public void onGetNotificationStatus(int i, int i1) {

                    }

                    @Override
                    public void onGetAliases(int i, List<SubscribeResult> list) {

                    }

                    @Override
                    public void onSetAliases(int i, List<SubscribeResult> list) {

                    }

                    @Override
                    public void onUnsetAliases(int i, List<SubscribeResult> list) {

                    }

                    @Override
                    public void onSetUserAccounts(int i, List<SubscribeResult> list) {

                    }

                    @Override
                    public void onUnsetUserAccounts(int i, List<SubscribeResult> list) {

                    }

                    @Override
                    public void onGetUserAccounts(int i, List<SubscribeResult> list) {

                    }

                    @Override
                    public void onSetTags(int i, List<SubscribeResult> list) {

                    }

                    @Override
                    public void onUnsetTags(int i, List<SubscribeResult> list) {

                    }

                    @Override
                    public void onGetTags(int i, List<SubscribeResult> list) {

                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initFCM(Context context) {
        FirebaseApp.initializeApp(context);
        FirebaseMessaging.getInstance().setAutoInitEnabled(true);
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(new OnCompleteListener<String>() {
                @Override
                public void onComplete(@NonNull Task<String> task) {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();

                    // Log and toast
                    String msg = token;
                    Log.d(TAG, msg);
//                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                    ChatManager.Instance().setDeviceToken(token, PushServiceType.Google);
                }
            });
    }

    /**
     * 检查 Google Play 服务
     */
    private static boolean hasGooglePlayServices(Context context) {
        // 验证是否已在此设备上安装并启用Google Play服务，以及此设备上安装的旧版本是否为此客户端所需的版本
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
    }


    public static final String SYS_EMUI = "sys_emui";
    public static final String SYS_MIUI = "sys_miui";
    public static final String SYS_FLYME = "sys_flyme";
    public static final String SYS_VIVO = "sys_vivo";
    private static final String KEY_MIUI_VERSION_CODE = "ro.miui.ui.version.code";
    private static final String KEY_MIUI_VERSION_NAME = "ro.miui.ui.version.name";
    private static final String KEY_MIUI_INTERNAL_STORAGE = "ro.miui.internal.storage";
    private static final String KEY_EMUI_API_LEVEL = "ro.build.hw_emui_api_level";
    private static final String KEY_EMUI_VERSION = "ro.build.version.emui";
    private static final String KEY_EMUI_CONFIG_HW_SYS_VERSION = "ro.confg.hw_systemversion";
    private static final String KEY_VERSION_VIVO = "ro.vivo.os.version";

    public static String getSystem() {
        String SYS = null;
        try {
            Properties prop = new Properties();

            prop.load(new FileInputStream(new File(Environment.getRootDirectory(), "build.prop")));
            if (prop.getProperty(KEY_MIUI_VERSION_CODE, null) != null
                || prop.getProperty(KEY_MIUI_VERSION_NAME, null) != null
                || prop.getProperty(KEY_MIUI_INTERNAL_STORAGE, null) != null) {
                SYS = SYS_MIUI;//小米
            } else if (prop.getProperty(KEY_EMUI_API_LEVEL, null) != null
                || prop.getProperty(KEY_EMUI_VERSION, null) != null
                || prop.getProperty(KEY_EMUI_CONFIG_HW_SYS_VERSION, null) != null) {
                SYS = SYS_EMUI;//华为
            } else if (getMeizuFlymeOSFlag().toLowerCase().contains("flyme")) {
                SYS = SYS_FLYME;//魅族
            } else if (!TextUtils.isEmpty(getProp(KEY_VERSION_VIVO))) {
                SYS = SYS_VIVO;
            }
        } catch (Exception e) {
            e.printStackTrace();

            if (Build.MANUFACTURER.equalsIgnoreCase("HUAWEI")) {
                SYS = SYS_EMUI;
            } else if (Build.MANUFACTURER.equalsIgnoreCase("xiaomi")) {
                SYS = SYS_MIUI;
            } else if (Build.MANUFACTURER.equalsIgnoreCase("meizu")) {
                SYS = SYS_FLYME;
            } else if (Build.MANUFACTURER.equalsIgnoreCase("vivo")) {
                SYS = SYS_VIVO;
            }

            return SYS;
        }
        return SYS;
    }

    public static String getMeizuFlymeOSFlag() {
        return getSystemProperty("ro.build.display.id", "");
    }

    private static String getSystemProperty(String key, String defaultValue) {
        try {
            Class<?> clz = Class.forName("android.os.SystemProperties");
            Method get = clz.getMethod("get", String.class, String.class);
            return (String) get.invoke(clz, key, defaultValue);
        } catch (Exception e) {
        }
        return defaultValue;
    }

    public static String getProp(String name) {
        String line = null;
        BufferedReader input = null;
        try {
            Process p = Runtime.getRuntime().exec("getprop " + name);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            line = input.readLine();
            input.close();
        } catch (IOException ex) {
            Log.e("getProp", "Unable to read prop " + name, ex);
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return line;
    }

    public static int getPushServiceType() {
        return INST.pushServiceType;
    }
}
