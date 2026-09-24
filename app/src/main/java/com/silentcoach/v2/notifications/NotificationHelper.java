package com.silentcoach.v2.notifications;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import com.silentcoach.v2.MainActivity;
import com.silentcoach.v2.R;

public final class NotificationHelper {
    public static final String CHANNEL_ID="silent_coach_reminders";
    private static final int QUOTE_ID=7101, REMINDER_ID=7102;
    private NotificationHelper(){}
    public static void ensureChannel(Context c){NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);if(nm==null)return;NotificationChannel ch=new NotificationChannel(CHANNEL_ID,"Silent Coach reminders",NotificationManager.IMPORTANCE_DEFAULT);ch.setDescription("Daily ideas and personal reminders");nm.createNotificationChannel(ch);}
    public static void show(Context c,String title,String body,boolean quote){ensureChannel(c);if(android.os.Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;Intent open=new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);PendingIntent pi=PendingIntent.getActivity(c,quote?710:711,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);Notification n=new Notification.Builder(c,CHANNEL_ID).setSmallIcon(R.drawable.ic_launcher).setContentTitle(title).setContentText(body).setStyle(new Notification.BigTextStyle().bigText(body)).setContentIntent(pi).setAutoCancel(true).setOnlyAlertOnce(true).build();NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);if(nm!=null)nm.notify(quote?QUOTE_ID:REMINDER_ID,n);}
}
