package com.silentcoach.v2.notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import java.util.Calendar;

public final class ReminderScheduler {
    public static final String ACTION_QUOTE="com.silentcoach.v2.QUOTE";
    public static final String ACTION_DAILY="com.silentcoach.v2.DAILY";
    private static final int QUOTE_PI=8101, DAILY_PI=8102;
    private ReminderScheduler(){}
    private static PendingIntent pi(Context c,String action,int req){return PendingIntent.getBroadcast(c,req,new Intent(c,ReminderReceiver.class).setAction(action),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    public static void cancel(Context c,String action,int req){AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(am!=null)am.cancel(pi(c,action,req));}
    public static void configureQuotes(Context c,boolean enabled,int hours){cancel(c,ACTION_QUOTE,QUOTE_PI);if(!enabled)return;long interval=Math.max(1,Math.min(4,hours))*60L*60L*1000L;AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(am!=null)am.setInexactRepeating(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+interval,interval,pi(c,ACTION_QUOTE,QUOTE_PI));}
    public static void configureDaily(Context c,boolean enabled,int hour,int minute){cancel(c,ACTION_DAILY,DAILY_PI);if(!enabled)return;Calendar x=Calendar.getInstance();x.set(Calendar.HOUR_OF_DAY,Math.max(0,Math.min(23,hour)));x.set(Calendar.MINUTE,Math.max(0,Math.min(59,minute)));x.set(Calendar.SECOND,0);x.set(Calendar.MILLISECOND,0);if(x.getTimeInMillis()<=System.currentTimeMillis())x.add(Calendar.DAY_OF_YEAR,1);AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(am!=null)am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,x.getTimeInMillis(),pi(c,ACTION_DAILY,DAILY_PI));}
}
