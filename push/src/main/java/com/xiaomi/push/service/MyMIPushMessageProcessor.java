package com.xiaomi.push.service;

import android.accounts.Account;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.support.v4.util.LruCache;
import android.text.TextUtils;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.xiaomi.channel.commonutils.android.AppInfoUtils;
import com.xiaomi.channel.commonutils.android.MIIDUtils;
import com.xiaomi.channel.commonutils.logger.MyLog;
import com.xiaomi.xmpush.thrift.ActionType;
import com.xiaomi.xmpush.thrift.PushMetaInfo;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;
import com.xiaomi.xmsf.BuildConfig;
import com.xiaomi.xmsf.R;

import java.util.List;
import java.util.Map;

import top.trumeet.common.cache.ApplicationNameCache;

import static com.xiaomi.push.service.MiPushMsgAck.geoMessageIsValidated;
import static com.xiaomi.push.service.MiPushMsgAck.processGeoMessage;
import static com.xiaomi.push.service.MiPushMsgAck.sendAckMessage;
import static com.xiaomi.push.service.MiPushMsgAck.sendAppAbsentAck;
import static com.xiaomi.push.service.MiPushMsgAck.sendAppNotInstallNotification;
import static com.xiaomi.push.service.MiPushMsgAck.sendErrorAck;
import static com.xiaomi.push.service.MiPushMsgAck.shouldSendBroadcast;
import static com.xiaomi.push.service.MiPushMsgAck.verifyGeoMessage;
import static com.xiaomi.push.service.PushServiceConstants.PREF_KEY_REGISTERED_PKGS;


/**
 *
 * @author zts1993
 * @date 2018/2/8
 */

public class MyMIPushMessageProcessor {
    private static Logger logger = XLog.tag("MyMIPushMessageProcessor").build();

    public static void process(XMPushService paramXMPushService, XmPushActionContainer buildContainer, byte[] paramArrayOfByte, long var2, Intent localIntent) {
        try {
            String targetPackage = MIPushNotificationHelper.getTargetPackage(buildContainer);
            Long current = System.currentTimeMillis();
            PushMetaInfo localPushMetaInfo = buildContainer.getMetaInfo();
            if (localPushMetaInfo != null) {
                localPushMetaInfo.putToExtra("mrt", Long.toString(current));
            }
            if (ActionType.SendMessage == buildContainer.getAction() && MIPushAppInfo.getInstance(paramXMPushService).isUnRegistered(buildContainer.packageName) && !MIPushNotificationHelper.isBusinessMessage(buildContainer)) {
                String var20 = "";
                if (localPushMetaInfo != null) {
                    var20 = localPushMetaInfo.getId();
                }

                logger.w("Drop a message for unregistered, msgid=" + var20);
                sendAppAbsentAck(paramXMPushService, buildContainer, buildContainer.packageName);
            } else if (ActionType.SendMessage == buildContainer.getAction() && MIPushAppInfo.getInstance(paramXMPushService).isPushDisabled4User(buildContainer.packageName) && !MIPushNotificationHelper.isBusinessMessage(buildContainer)) {
                String var19 = "";
                if (localPushMetaInfo != null) {
                    var19 = localPushMetaInfo.getId();
                }

                logger.w("Drop a message for push closed, msgid=" + var19);
                sendAppAbsentAck(paramXMPushService, buildContainer, buildContainer.packageName);
            } else if (ActionType.SendMessage == buildContainer.getAction() && !TextUtils.equals(paramXMPushService.getPackageName(), PushConstants.PUSH_SERVICE_PACKAGE_NAME) && !TextUtils.equals(paramXMPushService.getPackageName(), buildContainer.packageName)) {
                logger.w("Receive a message with wrong package name, expect " + paramXMPushService.getPackageName() + ", received " + buildContainer.packageName);
                sendErrorAck(paramXMPushService, buildContainer, "unmatched_package", "package should be " + paramXMPushService.getPackageName() + ", but got " + buildContainer.packageName);
            } else {
                if (localPushMetaInfo != null && localPushMetaInfo.getId() != null) {
                    logger.i(String.format("receive a message, appid=%s, msgid= %s", buildContainer.getAppid(), localPushMetaInfo.getId()));
                }

                if (localPushMetaInfo != null) {
                    Map<String, String> var17 = localPushMetaInfo.getExtra();
                    if (var17 != null && var17.containsKey("hide") && "true".equalsIgnoreCase(var17.get("hide"))) {
                        logger.i(String.format("hide a message, appid=%s, msgid= %s", buildContainer.getAppid(), localPushMetaInfo.getId()));
                        sendAckMessage(paramXMPushService, buildContainer);
                        return;
                    }
                }

                if ((localPushMetaInfo != null) && (localPushMetaInfo.getExtra() != null) && (localPushMetaInfo.getExtra().containsKey("__miid"))) {
                    String str2 = localPushMetaInfo.getExtra().get("__miid");
                    Account localAccount = MIIDUtils.getXiaomiAccount(paramXMPushService);
                    String oldAccount = "";
                    if (localAccount == null) {
                        // xiaomi account login ?
                        oldAccount = "nothing";
                    } else {
                        if (TextUtils.equals(str2, localAccount.name)) {

                        } else {
                            oldAccount = localAccount.name;
                            logger.w(str2 + " should be login, but got " + localAccount);
                        }
                    }

                    if (!oldAccount.isEmpty()) {
                        logger.w("miid already logout or anther already login :" + oldAccount);
                        sendErrorAck(paramXMPushService, buildContainer, "miid already logout or anther already login", oldAccount);
                    }
                }

                boolean isGeoMessage = (localPushMetaInfo != null && verifyGeoMessage(localPushMetaInfo.getExtra()));
                if (isGeoMessage) {
                    if (!geoMessageIsValidated(paramXMPushService, buildContainer)) {
                        return;
                    }

                    boolean var10 = processGeoMessage(paramXMPushService, localPushMetaInfo, paramArrayOfByte);
                    MIPushEventProcessor.sendGeoAck(paramXMPushService, buildContainer, true, false, false);
                    if (!var10) {
                        return;
                    }
                }

                userProcessMIPushMessage(paramXMPushService, buildContainer, paramArrayOfByte, var2, localIntent, isGeoMessage);
            }


        } catch (RuntimeException e2) {
            logger.e("fallbackProcessMIPushMessage failed at" + System.currentTimeMillis(), e2);
        }
    }


    /**
     * @see MIPushEventProcessor#postProcessMIPushMessage
     */
    private static void userProcessMIPushMessage(XMPushService paramXMPushService, XmPushActionContainer buildContainer, byte[] paramArrayOfByte, long var2, Intent paramIntent, boolean isGeoMessage) {
        //var5 buildContainer
        //var6 metaInfo
        boolean shouldNotify = true;

        boolean pkgInstalled = AppInfoUtils.isPkgInstalled(paramXMPushService, buildContainer.packageName);
        if (!pkgInstalled) {
            sendAppNotInstallNotification(paramXMPushService, buildContainer);
            return;
        }

        String targetPackage = MIPushNotificationHelper.getTargetPackage(buildContainer);

        if (MIPushNotificationHelper.isBusinessMessage(buildContainer)) {
            if (ActionType.Registration == buildContainer.getAction()) {
                String str2 = buildContainer.getPackageName();
                SharedPreferences.Editor localEditor = paramXMPushService.getSharedPreferences(PREF_KEY_REGISTERED_PKGS, 0).edit();
                localEditor.putString(str2, buildContainer.appid);
                localEditor.apply();
                com.xiaomi.tinyData.TinyDataManager.getInstance(paramXMPushService).processPendingData("Register Success, package name is " + str2);
            }
        }

        PushMetaInfo metaInfo = buildContainer.getMetaInfo();

        //abtest
        if (BuildConfig.APPLICATION_ID.contains(buildContainer.packageName) && !buildContainer.isEncryptAction() &&
                metaInfo != null && metaInfo.getExtra() != null && metaInfo.getExtra().containsKey("ab")) {
            sendAckMessage(paramXMPushService, buildContainer);
            MyLog.i("receive abtest message. ack it." + metaInfo.getId());
            return;
        }

        if (metaInfo != null) {
            String title = metaInfo.getTitle();
            String description = metaInfo.getDescription();

            if (TextUtils.isEmpty(title) && TextUtils.isEmpty(description)) {
            } else {

                if (TextUtils.isEmpty(title)) {
                    CharSequence appName = ApplicationNameCache.getInstance().getAppName(paramXMPushService, buildContainer.packageName);
                    if (appName == null) {
                        appName = buildContainer.packageName;
                    }
                    metaInfo.setTitle(appName.toString());
                }

                if (TextUtils.isEmpty(description)) {
                    metaInfo.setDescription(paramXMPushService.getString(R.string.see_pass_though_msg));
                }

                if (isDupTextMsg(targetPackage, title, description)) {
                    logger.w("drop a duplicate message, judge by text from : " + buildContainer.packageName);
                    shouldNotify = false;
                }
            }

        }

        if (metaInfo != null && !TextUtils.isEmpty(metaInfo.getTitle()) && !TextUtils.isEmpty(metaInfo.getDescription())) {

            String var8 = null;
            if (metaInfo.extra != null) {
                var8 = metaInfo.extra.get("jobkey");
            }
            if (TextUtils.isEmpty(var8)) {
                var8 = metaInfo.getId();
            }

            boolean var7 = MiPushMessageDuplicate.isDuplicateMessage(paramXMPushService, buildContainer.packageName, var8);
            if (var7) {
                logger.w("drop a duplicate message, key=" + var8);
            } else {

                if (shouldNotify) {
                    MyMIPushNotificationHelper.notifyPushMessage(paramXMPushService, buildContainer, paramArrayOfByte, var2);
                }

                //send broadcast
                if (!MIPushNotificationHelper.isBusinessMessage(buildContainer)) {


                    Intent localIntent = new Intent(PushConstants.MIPUSH_ACTION_MESSAGE_ARRIVED);
                    localIntent.putExtra(PushConstants.MIPUSH_EXTRA_PAYLOAD, paramArrayOfByte);
                    localIntent.putExtra(MIPushNotificationHelper.FROM_NOTIFICATION, true);
                    localIntent.setPackage(buildContainer.packageName);

                    try {
                        List<ResolveInfo> localList = paramXMPushService.getPackageManager().queryBroadcastReceivers(localIntent, 0);
                        if ((localList != null) && (!localList.isEmpty())) {
                            paramXMPushService.sendBroadcast(localIntent, ClientEventDispatcher.getReceiverPermission(buildContainer.getPackageName()));
                        }
                    } catch (Exception ignore) {
                    }

                }

            }

            if (isGeoMessage) {
                MIPushEventProcessor.sendGeoAck(paramXMPushService, buildContainer, false, true, false);
            } else {
                sendAckMessage(paramXMPushService, buildContainer);

            }
        } else if (shouldSendBroadcast(paramXMPushService, targetPackage, buildContainer, metaInfo)) {

            paramXMPushService.sendBroadcast(paramIntent, ClientEventDispatcher.getReceiverPermission(buildContainer.packageName));

        }

    }


    private static LruCache<String, CacheItem> cacheInstance = new LruCache<>(15);

    private static class CacheItem {
        String title; String content; long time;

        CacheItem(String title, String content, long time) {
            this.title = title;
            this.content = content;
            this.time = time;
        }
    }
    private static boolean isDupTextMsg(String packageName, String title, String content) {
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) {
            return false;
        }
        long currentTimeMillis = System.currentTimeMillis();
        CacheItem cached = cacheInstance.get(packageName);
        boolean isDup = false;

        if (cached != null) {
            if (TextUtils.equals(title + content, cached.title + cached.content)) {
                if ((currentTimeMillis - cached.time) < (60 * 1000)) {
                    isDup = true;
                }
            }
        }
        if (!isDup) {
            cacheInstance.put(packageName, new CacheItem(title, content, currentTimeMillis));
        }

        return isDup;
    }

}
