package org.cion.eclipse.mqtt5.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;

import org.cion.eclipse.mqtt5.R;


/**
 * @author: kern
 * @date: 2021/11/27
 * @Description: java类作用描述
 */
public class NotificationBuilder {

    private Context context;
    String CHANNEL_ONE_ID = "cion_eclipse_mqtt5";
    String CHANNEL_ONE_NAME = "cion_eclipse_mqtt5";
    NotificationChannel notificationChannel = null;

    public NotificationBuilder(Context context) {
        this.context = context;
    }

    public Notification create() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel(CHANNEL_ONE_ID,
                    CHANNEL_ONE_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setShowBadge(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(notificationChannel);
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.jianshu.com/p/14ba95c6c3e2"));
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        Notification notification = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notification = new Notification.Builder(context).setChannelId(CHANNEL_ONE_ID)
                    .setTicker("Nature")
                    //.setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("这是一个测试标题")
                    .setContentIntent(pendingIntent)
                    .setContentText("这是一个测试内容")
                    .build();
        }
        notification.flags |= Notification.FLAG_NO_CLEAR;
        return notification;
    }
}
