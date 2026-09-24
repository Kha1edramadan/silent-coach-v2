package com.silentcoach.v2.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import com.silentcoach.v2.data.CoachDb;
import com.silentcoach.v2.data.QuoteRepository;

public class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){CoachDb db=new CoachDb(c);try{String a=i.getAction();if(ReminderScheduler.ACTION_QUOTE.equals(a)){if(!"1".equals(db.getSetting("quote_notifications_enabled","0")))return;if(inQuietHours(db))return;Cursor q=db.nextQuote();try{if(q.moveToFirst()){String body=QuoteRepository.displayText(q,db.getSetting("lang","en"));String author=q.getString(q.getColumnIndexOrThrow("author"));String work=q.getString(q.getColumnIndexOrThrow("work"));NotificationHelper.show(c,"Silent Coach · "+author,body+"\n— "+work,true);db.markQuoteShown(q.getLong(q.getColumnIndexOrThrow("id")));}}finally{q.close();}}else if(ReminderScheduler.ACTION_DAILY.equals(a)){if(!"1".equals(db.getSetting("daily_reminder_enabled","0")))return;NotificationHelper.show(c,"Silent Coach",db.getSetting("daily_reminder_text","Time to check in with your plan."),false);ReminderScheduler.configureDaily(c,true,p(db.getSetting("daily_reminder_hour","20"),20),p(db.getSetting("daily_reminder_minute","0"),0));}}finally{db.close();}}
    private boolean inQuietHours(CoachDb db){if(!"1".equals(db.getSetting("quote_quiet_enabled","0")))return false;int s=p(db.getSetting("quote_quiet_start","23"),23),e=p(db.getSetting("quote_quiet_end","7"),7);int h=java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);return s<e?h>=s&&h<e:h>=s||h<e;}
    private int p(String x,int d){try{return Integer.parseInt(x);}catch(Exception e){return d;}}
}
