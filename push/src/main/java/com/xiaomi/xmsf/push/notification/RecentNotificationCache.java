package com.xiaomi.xmsf.push.notification;

import android.app.Notification;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;

import me.pqpo.librarylog4a.Log4a;

/**
 * @author zts
 */
public class RecentNotificationCache {
    private static final String TAG = RecentNotificationCache.class.getSimpleName();


    private volatile static RecentNotificationCache cache = null;
    private LruCache<String, NotificationItem> cacheInstance;

    private RecentNotificationCache() {
        cacheInstance = new LruCache<>(100);
    }

    public static RecentNotificationCache getInstance() {
        if (cache == null) {
            synchronized (RecentNotificationCache.class) {
                if (cache == null) {
                    cache = new RecentNotificationCache();
                }
            }
        }
        return cache;
    }


    public void put(NotificationItem item) {
        cacheInstance.put(buildNotificationKey(item), item);
        Log4a.i(TAG, String.format("put cache %s ", cacheInstance.toString()));

    }

    public void del(NotificationItem item) {
        cacheInstance.remove(buildNotificationKey(item));
        Log4a.i(TAG, String.format("remove cache %s ", cacheInstance.toString()));

    }

    public boolean hasNotified(NotificationItem item) {
        Log4a.i(TAG, String.format("hasNotified cache %s ", cacheInstance.toString()));
        return cacheInstance.get(buildNotificationKey(item)) != null;
    }

    private static String buildNotificationKey(@NonNull NotificationItem item) {
        int maxTitleLen = item.getTitle().length() < 8 ? item.getTitle().length() : 8;
        int maxContentLen = item.getContent().length() < 15 ? item.getContent().length() : 15;
        String title = item.getTitle().subSequence(0, maxTitleLen).toString();
        String content = item.getContent().subSequence(0, maxContentLen).toString();
        Log4a.i(TAG, String.format("buildNotificationKey %s ", title + content));
        return title + content;
    }


    @Nullable
    public static NotificationItem getNotificationItem(String packageName, Notification notification) {
        if (notification == null) {
            return null;
        }

        Bundle extras = notification.extras;
        if (extras == null || extras.size() == 0) {
            return null;
        }

        CharSequence titleText = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
        if (titleText == null) {
            titleText = notification.extras.getCharSequence(Notification.EXTRA_TITLE_BIG);
        }

        CharSequence contentText = notification.extras.getCharSequence(Notification.EXTRA_TEXT);
        if (contentText == null) {
            contentText = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        }

        if (titleText == null || contentText == null) {
            return null;
        }

        return new NotificationItem(packageName, titleText, contentText);
    }


}
