package com.cjs.hegui30;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.location.Criteria;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Looper;
import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 合规检测、方法调用栈追踪xposed模块
 *
 * @author JasonChen
 * @email chenjunsen@outlook.com
 * @createTime 2021/7/12 8:29
 */
public class HookTrack implements IXposedHookLoadPackage {
    private static final String TAG = "HookTrack";

    /**
     * 需要Hook的包名白名单
     */
    private static final String[] whiteList = {
            "com.cjs.drv",
            "com.bw30.zsch",
            "com.bw30.zsch.magic",
            "com.cjs.hegui30.demo"
    };

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {

        if (lpparam == null) {
            return;
        }

        Log.e(TAG, "开始加载package:" + lpparam.packageName);
        /*判断hook的包名*/
        boolean res = false;
        for (String pkgName : whiteList) {
            if (pkgName.equals(lpparam.packageName)) {
                res = true;
                break;
            }
        }
        if (!res) {
            Log.e(TAG, "不符合的包:" + lpparam.packageName);
            return;
        }

        //固定格式
        XposedHelpers.findAndHookMethod(
                android.telephony.TelephonyManager.class.getName(), // 需要hook的方法所在类的完整类名
                lpparam.classLoader,                            // 类加载器，固定这么写就行了
                "getDeviceId",                     // 需要hook的方法名
                new DumpMethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log(lpparam.packageName + "调用getDeviceId()获取了imei");
                    }
                }
        );
        XposedHelpers.findAndHookMethod(
                android.telephony.TelephonyManager.class.getName(),
                lpparam.classLoader,
                "getDeviceId",
                int.class,
                new DumpMethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log(lpparam.packageName + "调用getDeviceId(int)获取了imei");
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                android.telephony.TelephonyManager.class.getName(),
                lpparam.classLoader,
                "getSubscriberId",
                int.class,
                new DumpMethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log(lpparam.packageName + "调用getSubscriberId获取了imsi");
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                android.telephony.TelephonyManager.class.getName(),
                lpparam.classLoader,
                "getImei",
                new DumpMethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log(lpparam.packageName + "调用getImei获取了imei");
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                android.telephony.TelephonyManager.class.getName(),
                lpparam.classLoader,
                "getImei",
                int.class,
                new DumpMethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log(lpparam.packageName + "调用getImei(int)获取了imei");
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                android.net.wifi.WifiInfo.class.getName(),
                lpparam.classLoader,
                "getMacAddress",
                new DumpMethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log(lpparam.packageName + "调用getMacAddress()获取了mac地址");
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                java.net.NetworkInterface.class.getName(),
                lpparam.classLoader,
                "getHardwareAddress",
                new DumpMethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log(lpparam.packageName + "调用getHardwareAddress()获取了mac地址");
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                android.provider.Settings.Secure.class.getName(),
                lpparam.classLoader,
                "getString",
                ContentResolver.class,
                String.class,
                new DumpMethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log(lpparam.packageName + "调用Settings.Secure.getstring获取了" + param.args[1]);
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                LocationManager.class.getName(),
                lpparam.classLoader,
                "getLastKnownLocation",
                String.class,
                new DumpMethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log(lpparam.packageName + "调用getLastKnownLocation获取了GPS地址");
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                LocationManager.class.getName(),
                lpparam.classLoader,
                "requestLocationUpdates",
                String.class,
                long.class,//注意，如果是基础类型，不要使用其对应的包装类，否则会找不到这个方法
                float.class,
                LocationListener.class,
                new DumpMethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log(lpparam.packageName + "调用requestLocationUpdates获取了GPS地址");
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                LocationManager.class.getName(),
                lpparam.classLoader,
                "requestLocationUpdates",
                String.class,
                long.class,
                float.class,
                LocationListener.class,
                Looper.class,
                new DumpMethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log(lpparam.packageName + "调用requestLocationUpdates获取了GPS地址");
                    }
                }
        );
    }
}
