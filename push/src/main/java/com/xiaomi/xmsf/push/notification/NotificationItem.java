package com.xiaomi.xmsf.push.notification;

/**
 * @author zts
 */
public class NotificationItem {

    private String packageName;
    private CharSequence title;
    private CharSequence content;

    public NotificationItem(String packageName, CharSequence title, CharSequence content) {
        this.packageName = packageName;
        this.title = title;
        this.content = content;
    }

    public String getPackageName() {
        return packageName;
    }

    public CharSequence getTitle() {
        return title;
    }

    public CharSequence getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "NotificationItem{" +
                "packageName='" + packageName + '\'' +
                ", title=" + title +
                ", content=" + content +
                '}';
    }
}