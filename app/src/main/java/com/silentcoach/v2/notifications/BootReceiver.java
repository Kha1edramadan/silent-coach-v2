package com.silentcoach.v2.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.silentcoach.v2.data.CoachDb;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){if(!Intent.ACTION_BOOT_COMPLETED.equals(i.getAction()))return;CoachDb db=new CoachDb(c);try{ReminderScheduler.configureQuotes(c,"1".equals(db.getSetting("quote_notifications_enabled","0")),p(db.getSetting("quote_interval_hours","2"),2));ReminderScheduler.configureDaily(c,"1".equals(db.getSetting("daily_reminder_enabled","0")),p(db.getSetting("daily_reminder_hour","20"),20),p(db.getSetting("daily_reminder_minute","0"),0));}finally{db.close();}}
    private int p(String x,int d){try{return Integer.parseInt(x);}catch(Exception e){return d;}}
}
