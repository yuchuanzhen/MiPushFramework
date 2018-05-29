package top.trumeet.mipushframework.xposed.util;

import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import com.alibaba.fastjson.JSON;

import java.util.HashMap;
import java.util.Map;

import top.trumeet.mipushframework.MiPushFramework;

/**
 * for xposed module only
 */
public class SharedPreferencesHelper {

    private static final SharedPreferencesHelper instance = new SharedPreferencesHelper();
    public static String PREF = "top.trumeet.xposed_preferences";
    public static String Authority = "top.trumeet.mipushframework.xposed_preferences";

    private SharedPreferencesHelper() {
    }

    public static SharedPreferencesHelper getInstance() {
        return instance;
    }

    //  PreferencesUtils.Authority  , PreferencesUtils.MainPrefs
    public SharedPreferences getSharedPreferences() {
        return MiPushFramework.getInstance().getSharedPreferences(PREF, 0);
    }

    @Nullable
    public <T> T get(String key, @Nullable String defValue, Class<T> cl) {
        return JSON.toJavaObject(JSON.parseObject(getSharedPreferences().getString(key, null)), cl);
    }

    public <T> Map<String, T> getAll(Class<T> cl) {
        Map<String, T> r = new HashMap<>();
        Map<String, ?> all = getSharedPreferences().getAll();
        for (Map.Entry<String, ?> stringEntry : all.entrySet()) {
            try {
                r.put(stringEntry.getKey(), JSON.toJavaObject(JSON.parseObject((String) stringEntry.getValue()), cl));
            } catch (Exception ignored) {
            }
        }
        return r;
    }

    public void update(String key,Object object) {
        getSharedPreferences().edit().putString(key, JSON.toJSONString(object, false)).apply();
    }


}
