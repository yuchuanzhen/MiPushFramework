package top.trumeet.mipushframework.xposed.hook;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.crossbowffs.remotepreferences.RemotePreferences;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import top.trumeet.common.utils.PreferencesUtils;
import top.trumeet.mipush.BuildConfig;
import top.trumeet.mipushframework.xposed.model.SimpleAppInfo;
import top.trumeet.mipushframework.xposed.util.CommonUtil;
import top.trumeet.mipushframework.xposed.util.SharedPreferencesHelper;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public class MiPushEnhanceHook implements IXposedHookLoadPackage {

    private static final String TAG = "MiPushEnhanceHook";

    private Set<String> blackList = new CopyOnWriteArraySet<>();
    private Map<String, String> fakeMap = new HashMap<>();

    private static String BRAND = "Xiaomi";
    private static String PRODUCT = "MIX";

    {
        blackList.add("android");
        blackList.add("de.robv.android.xposed.installer");
        blackList.add("com.xiaomi.xmsf");

        fakeMap.put("ro.miui.ui.version.name", "V9");
        fakeMap.put("ro.miui.ui.version.code", "7");
        fakeMap.put("ro.miui.version.code_time", "1527550858");
        fakeMap.put("ro.product.manufacturer", BRAND);
        fakeMap.put("ro.product.brand", BRAND);
        fakeMap.put("ro.product.name", BRAND);


        fakeMap.put("ro.product.model", PRODUCT);
        fakeMap.put("ro.product.device", PRODUCT);
        fakeMap.put("ro.hardware", PRODUCT);
    }

    private boolean inBlackList(String pkgName) {
        return blackList.contains(pkgName.toLowerCase());
    }

    private synchronized void initBlackList(Context context) {
        SharedPreferences prefs = new RemotePreferences(context, SharedPreferencesHelper.Authority, SharedPreferencesHelper.PREF);

        List<ApplicationInfo> installedApplications = context.getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo applicationInfo : installedApplications) {
            if (CommonUtil.isUserApplication(applicationInfo)) {
                String prefVal = prefs.getString(applicationInfo.packageName, null);
                if (prefVal != null) {
                    XposedBridge.log(applicationInfo.packageName + ":" + prefVal);
                    SimpleAppInfo appInfo = JSON.toJavaObject(JSON.parseObject(prefVal), SimpleAppInfo.class);
                    if (appInfo != null && appInfo.skipEnhance) {
                        blackList.add(applicationInfo.packageName);
                    }
                }
            }
        }

        XposedBridge.log("init blackList : " + JSON.toJSONString(blackList));

    }

    private volatile Context applicationContext = null;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        try {
            if (lpparam.packageName.equals(BuildConfig.APPLICATION_ID)) {
                XposedHelpers.findAndHookMethod("top.trumeet.mipushframework.MiPushFramework", lpparam.classLoader, "isXposedWork",
                        new XC_MethodReplacement() {
                            @Override
                            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                                return true;
                            }
                        });


                Class<?> ContextClass = findClass("android.content.ContextWrapper", lpparam.classLoader);
                findAndHookMethod(ContextClass, "getApplicationContext", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        if (applicationContext != null) {
                            return;
                        }

                        if (param.getResult() != null) {
                            applicationContext = (Context) param.getResult();
                            XposedBridge.log("getApplicationContext now");
                            initBlackList(applicationContext);
                        }
                    }
                });

                return;
            }


            if (!CommonUtil.isUserApplication(lpparam.appInfo)) {
                return;
            }

            if (inBlackList(lpparam.appInfo.packageName)) {
                return;
            }


            XposedBridge.hookAllMethods(XposedHelpers.findClass("android.os.SystemProperties", lpparam.classLoader), "get", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (fakeMap.containsKey(param.args[0].toString())) {
                        param.setResult(fakeMap.get(param.args[0].toString()));
                    }
                }
            });

            XposedHelpers.setStaticObjectField(android.os.Build.class, "MANUFACTURER", BRAND);
            XposedHelpers.setStaticObjectField(android.os.Build.class, "BRAND", BRAND);
            XposedHelpers.setStaticObjectField(android.os.Build.class, "PRODUCT", BRAND);


            XposedHelpers.setStaticObjectField(android.os.Build.class, "MODEL", PRODUCT);
            XposedHelpers.setStaticObjectField(android.os.Build.class, "DEVICE", PRODUCT);
            XposedHelpers.setStaticObjectField(android.os.Build.class, "HARDWARE", PRODUCT);


        } catch (Throwable throwable) {
            Log.e(TAG, "hook meet exception : " + throwable.getLocalizedMessage(), throwable);
            XposedBridge.log(throwable);
        }

    }


}
