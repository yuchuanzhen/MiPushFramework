package top.trumeet.mipushframework.xposed.provider;

import com.crossbowffs.remotepreferences.RemotePreferenceProvider;

import top.trumeet.common.utils.PreferencesUtils;
import top.trumeet.mipushframework.xposed.util.SharedPreferencesHelper;

/**
 * Created by zts1993 on 2018/3/21.
 */

public class XposedPreferenceProvider extends RemotePreferenceProvider {
    public XposedPreferenceProvider() {
        super(PreferencesUtils.Authority, new String[]{SharedPreferencesHelper.PREF});
    }

    @Override
    protected boolean checkAccess(String prefName, String prefKey, boolean write) {
        if (write) {
            return false;
        }

        return true;
    }
}