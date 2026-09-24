package com.silentcoach.v2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.integration.IntentResult;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.database.Cursor;

import com.silentcoach.v2.core.GoalMath;
import com.silentcoach.v2.core.DataQualityMath;
import com.silentcoach.v2.data.CoachDb;
import com.silentcoach.v2.data.BackupManager;
import com.silentcoach.v2.data.QuoteRepository;
import com.silentcoach.v2.notifications.NotificationHelper;
import com.silentcoach.v2.notifications.ReminderScheduler;
import com.silentcoach.v2.ui.TrendChartView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private static final int REQUEST_EXPORT_BACKUP = 4201;
    private static final int REQUEST_IMPORT_BACKUP = 4202;
    private CoachDb db;
    private LinearLayout root, content, nav;
    private String lang = "en";
    private String units = "metric";
    private int page = 0;
    private long activeSession = -1;
    private CountDownTimer restTimer;
    private TextView restLabel;
    private EditText barcodeField;
    private LinearLayout barcodeResults;
    private boolean onboarding=false;

    private static final int BG=Color.rgb(10,12,15), SURFACE=Color.rgb(18,22,27), SURFACE2=Color.rgb(28,33,40);
    private static final int TEXT=Color.rgb(247,248,250), MUTED=Color.rgb(149,158,170), ACCENT=Color.rgb(186,245,111);
    private static final int RED=Color.rgb(255,105,105), BLUE=Color.rgb(126,188,255), BORDER=Color.rgb(53,60,70);
    private final Typeface regular=Typeface.create("sans-serif",Typeface.NORMAL);
    private final Typeface bold=Typeface.create("sans-serif",Typeface.BOLD);

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        db=new CoachDb(this);
        lang=db.getSetting("lang","en");
        Cursor p=db.profile();
        try{if(p.moveToFirst()){int li=p.getColumnIndex("language");int ui=p.getColumnIndex("units");if(li>=0&&!p.isNull(li))lang=p.getString(li);if(ui>=0&&!p.isNull(ui))units=p.getString(ui);}}finally{p.close();}
        applyDirection();
        buildShell();
        Cursor active=db.activeUnfinishedSession();
        try{if(active.moveToFirst())activeSession=active.getLong(0);}finally{active.close();}
        NotificationHelper.ensureChannel(this);
        onboarding=!"1".equals(db.getSetting("onboarding_complete","0"));
        if(onboarding) showOnboarding(); else showPage(0);
    }

    private void applyDirection(){
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setLayoutDirection("ar".equals(lang)?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);
    }
    private String t(String en,String ar){return "ar".equals(lang)?ar:en;}
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private TextView tv(String s,float size,int color,boolean isBold){TextView v=new TextView(this);v.setText(s);v.setTextColor(color);v.setTextSize(size);v.setTypeface(isBold?bold:regular);v.setGravity("ar".equals(lang)?Gravity.RIGHT:Gravity.LEFT);v.setIncludeFontPadding(false);return v;}
    private TextView centered(String s,float size,int color,boolean isBold){TextView v=tv(s,size,color,isBold);v.setGravity(Gravity.CENTER);return v;}
    private TextView title(String s){TextView v=tv(s,29,TEXT,true);v.setPadding(0,0,0,dp(2));return v;}
    private GradientDrawable bg(int color,float radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private GradientDrawable stroke(int color,float radius,int line){GradientDrawable g=bg(color,radius);g.setStroke(dp(1),BORDER);return g;}
    private View gap(int h){Space s=new Space(this);s.setLayoutParams(new LinearLayout.LayoutParams(1,dp(h)));return s;}
    private View hgap(int w){Space s=new Space(this);s.setLayoutParams(new LinearLayout.LayoutParams(dp(w),1));return s;}
    private LinearLayout card(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(16),dp(16),dp(16),dp(16));l.setBackground(stroke(SURFACE,18,1));return l;}
    private Button primary(String s){Button b=new Button(this);b.setText(s);b.setTextColor(BG);b.setTextSize(14);b.setTypeface(bold);b.setAllCaps(false);b.setMinHeight(dp(46));b.setPadding(dp(12),0,dp(12),0);b.setBackground(bg(ACCENT,14));return b;}
    private Button secondary(String s){Button b=primary(s);b.setTextColor(TEXT);b.setBackground(bg(SURFACE2,14));return b;}
    private EditText input(String hint,boolean numeric){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(MUTED);e.setTextColor(TEXT);e.setTextSize(15);e.setSingleLine(true);e.setPadding(dp(12),0,dp(12),0);e.setBackground(stroke(SURFACE2,12,1));if(numeric)e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);return e;}
    private TextView small(String s){TextView v=tv(s,12,MUTED,false);v.setPadding(0,dp(4),0,dp(4));return v;}

    private void buildShell(){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);setContentView(root);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(18),dp(22),dp(18),dp(18));scroll.addView(content,new ScrollView.LayoutParams(-1,-1));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);nav.setGravity(Gravity.CENTER);nav.setPadding(dp(8),dp(8),dp(8),dp(11));nav.setBackground(stroke(SURFACE,24,1));root.addView(nav,new LinearLayout.LayoutParams(-1,dp(78)));
        refreshNav();
    }
    private void refreshNav(){
        nav.removeAllViews();
        String[] en={"Home","Workout","Nutrition","Progress","Profile"};String[] ar={"الرئيسية","التمرين","التغذية","التقدم","حسابي"};
        for(int i=0;i<5;i++){final int p=i;TextView v=centered("ar".equals(lang)?ar[i]:en[i],11,p==page?ACCENT:MUTED,true);v.setPadding(0,dp(8),0,0);nav.addView(v,new LinearLayout.LayoutParams(0,-1,1));v.setOnClickListener(x->showPage(p));}
    }
    private void showOnboarding(){
        onboarding=true; nav.setVisibility(View.GONE); content.removeAllViews();
        content.addView(title(t("Set up your plan","ظبط خطتك")));content.addView(small(t("A few basics first. You can edit everything later.","كام معلومة أساسية الأول، وتقدر تعدل كل حاجة بعد كده.")));content.addView(gap(18));
        LinearLayout box=card();EditText w=input(t("Weight "+weightUnit(),"الوزن "+weightUnit()),true),h=input(t("Height cm","الطول سم"),true),a=input(t("Age","السن"),true);w.setText("70");h.setText("175");a.setText("22");box.addView(w);box.addView(gap(8));box.addView(h);box.addView(gap(8));box.addView(a);
        box.addView(gap(10));box.addView(tv(t("Sex","النوع"),14,TEXT,true));final String[] sx={"male"};Button bm=secondary("✓ "+t("Male","ذكر"));Button bf=secondary(t("Female","أنثى"));bm.setOnClickListener(v->{sx[0]="male";bm.setText("✓ "+t("Male","ذكر"));bf.setText(t("Female","أنثى"));});bf.setOnClickListener(v->{sx[0]="female";bf.setText("✓ "+t("Female","أنثى"));bm.setText(t("Male","ذكر"));});box.addView(bm);box.addView(bf);
        box.addView(gap(8));box.addView(tv(t("Activity","النشاط"),14,TEXT,true));final String[] act={"moderate"};String[] acts={"sedentary","light","moderate","high","very_high"};String[] al={t("Low","قليل"),t("Light","خفيف"),t("Moderate","متوسط"),t("High","عالي"),t("Very high","عالي جدًا")};LinearLayout ab=new LinearLayout(this);ab.setOrientation(LinearLayout.VERTICAL);for(int i=0;i<acts.length;i++){final int k=i;Button b=secondary((i==2?"✓ ":"")+al[i]);b.setOnClickListener(v->{act[0]=acts[k];for(int j=0;j<ab.getChildCount();j+=2){Button z=(Button)ab.getChildAt(j);z.setText((j/2==k?"✓ ":"")+al[j/2]);}});ab.addView(b);ab.addView(gap(4));}box.addView(ab);
        box.addView(gap(8));box.addView(tv(t("Goal","الهدف"),14,TEXT,true));final String[] goal={"gain"};String[] gs={"gain","lose","maintain"};String[] gl={t("Build muscle / gain","بناء عضلات / زيادة"),t("Lose fat","خسارة دهون"),t("Maintain","ثبات")};LinearLayout gb=new LinearLayout(this);gb.setOrientation(LinearLayout.VERTICAL);for(int i=0;i<gs.length;i++){final int k=i;Button b=secondary((i==0?"✓ ":"")+gl[i]);b.setOnClickListener(v->{goal[0]=gs[k];for(int j=0;j<gb.getChildCount();j+=2){Button z=(Button)gb.getChildAt(j);z.setText((j/2==k?"✓ ":"")+gl[j/2]);}});gb.addView(b);gb.addView(gap(4));}box.addView(gb);
        Button done=primary(t("Create my plan","اعمل خطتي"));done.setOnClickListener(v->{double kg=read(w,70),cm=read(h,175);int age=(int)read(a,22);if(kg<=0||cm<=0||age<=0){toast(t("Enter valid profile values.","اكتب بيانات صحيحة."));return;}GoalMath.Targets z=GoalMath.initialTargets(kg,cm,age,"male".equals(sx[0]),act[0],goal[0]);db.updateProfile(kg,cm,age,"male".equals(sx[0]),z.kcal,z.protein,z.carbs,z.fat,goal[0],act[0],lang,units);db.setSetting("onboarding_complete","1");onboarding=false;nav.setVisibility(View.VISIBLE);showPage(0);});box.addView(gap(10));box.addView(done);content.addView(box);
    }

    private void showPage(int p){page=p;refreshNav();if(p==0)home();else if(p==1)workout();else if(p==2)nutrition();else if(p==3)progress();else profile();}
    private void clear(String heading,String subtitle){content.removeAllViews();LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.VERTICAL);head.addView(title(heading));if(subtitle!=null&&!subtitle.isEmpty())head.addView(small(subtitle));content.addView(head);content.addView(gap(14));}

    private double[] profileTargets(){double[] v={2500,96,320,75};Cursor c=db.profile();try{if(c.moveToFirst()){v[0]=c.getDouble(c.getColumnIndexOrThrow("calorie_target"));v[1]=c.getDouble(c.getColumnIndexOrThrow("protein_target"));v[2]=c.getDouble(c.getColumnIndexOrThrow("carb_target"));v[3]=c.getDouble(c.getColumnIndexOrThrow("fat_target"));}}finally{c.close();}return v;}
    private double toDisplayWeight(double kg){return "imperial".equals(units)?kg*2.2046226218:kg;}
    private double fromDisplayWeight(double v){return "imperial".equals(units)?v/2.2046226218:v;}
    private double toDisplayHeight(double cm){return "imperial".equals(units)?cm/2.54:cm;}
    private double fromDisplayHeight(double v){return "imperial".equals(units)?v*2.54:v;}
    private String weightUnit(){return "imperial".equals(units)?"lb":"kg";}
    private String heightUnit(){return "imperial".equals(units)?"in":"cm";}
    private String measureUnit(){return "imperial".equals(units)?"in":"cm";}
    private double toDisplayMeasure(double cm){return "imperial".equals(units)?cm/2.54:cm;}
    private double fromDisplayMeasure(double v){return "imperial".equals(units)?v*2.54:v;}

    private double profileWeight(){Cursor c=db.profile();try{return c.moveToFirst()?c.getDouble(c.getColumnIndexOrThrow("weight_kg")):60;}finally{c.close();}}
    private String activeProgramName(){Cursor c=db.activeProgram();try{return c.moveToFirst()?c.getString(1):t("No program","مفيش برنامج");}finally{c.close();}}

    private void home(){
        clear(t("Good evening","مساء الخير"),activeProgramName());
        double[] trg=profileTargets(), tot=db.todayTotals();
        LinearLayout hero=card();hero.addView(tv(t("Today, at a glance","ملخص النهارده"),16,TEXT,true));hero.addView(gap(6));
        hero.addView(tv(fmt(tot[0])+" / "+fmt(trg[0])+" kcal",28,ACCENT,true));hero.addView(small(t("Calories","السعرات")));
        addProgress(hero,tot[0],trg[0]);hero.addView(gap(8));
        macroLine(hero,t("Protein","البروتين"),tot[1],trg[1]);macroLine(hero,t("Carbs","الكارب"),tot[2],trg[2]);macroLine(hero,t("Fat","الدهون"),tot[3],trg[3]);content.addView(hero);
        content.addView(gap(12));
        LinearLayout workoutCard=card();workoutCard.addView(tv(t("Next workout","التمرين الجاي"),16,TEXT,true));workoutCard.addView(tv(activeDayTitle(),22,TEXT,true));workoutCard.addView(small(t(activeDayMeta(),activeDayMeta()+" · قابل للتعديل")));Button start=primary(activeSession>0?t("Resume workout","كمّل التمرين"):t("Start workout","ابدأ التمرين"));start.setOnClickListener(v->{if(activeSession<0)activeSession=db.startSession(firstDayId(),activeDayTitle());showActiveWorkout();});workoutCard.addView(start);content.addView(workoutCard);
        content.addView(gap(12));
        LinearLayout stats=card();stats.addView(tv(t("This week","الأسبوع ده"),16,TEXT,true));stats.addView(gap(6));TextView a=tv(db.completedWorkoutsThisWeek()+" "+t("workouts","تمرين"),23,TEXT,true);stats.addView(a);stats.addView(small(t("Completed sessions","التمارين المكتملة")));content.addView(stats);content.addView(gap(12));LinearLayout q=card();q.addView(tv(t("Idea for today","فكرة النهارده"),15,TEXT,true));Cursor qc=db.nextQuote();if(qc.moveToFirst()){String kind=qc.getString(qc.getColumnIndexOrThrow("kind"));String label="idea".equals(kind)?t("Idea","فكرة"):t("Quote","اقتباس");q.addView(small(label+" · "+qc.getString(qc.getColumnIndexOrThrow("category"))));q.addView(tv(QuoteRepository.displayText(qc,lang),17,TEXT,false));q.addView(small("— "+qc.getString(qc.getColumnIndexOrThrow("author"))+" · "+qc.getString(qc.getColumnIndexOrThrow("work"))));String src=qc.getString(qc.getColumnIndexOrThrow("source_url"));if(src!=null&&!src.isEmpty()){TextView source=tv(t("View source","عرض المصدر"),12,ACCENT,true);source.setPadding(0,dp(8),0,0);source.setOnClickListener(v->{try{startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(src)));}catch(Exception ignored){}});q.addView(source);}}qc.close();content.addView(q);
    }
    private void addProgress(LinearLayout parent,double current,double target){ProgressBar p=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);p.setMax(1000);p.setProgress((int)Math.min(1000,Math.round(target<=0?0:(current/target)*1000)));p.setProgressTintList(android.content.res.ColorStateList.valueOf(ACCENT));parent.addView(p,new LinearLayout.LayoutParams(-1,dp(5)));}
    private void macroLine(LinearLayout parent,String name,double current,double target){LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);TextView a=tv(name,13,TEXT,true);TextView b=tv(fmt(current)+" / "+fmt(target)+" g",13,MUTED,true);r.addView(a,new LinearLayout.LayoutParams(0,dp(28),1));r.addView(b,new LinearLayout.LayoutParams(-2,dp(28)));parent.addView(r);}
    private String activeDayTitle(){Cursor c=db.activeDays();try{return c.moveToFirst()?c.getString(1):t("Workout","التمرين");}finally{c.close();}}
    private String activeDayMeta(){Cursor c=db.activeDays();try{if(c.moveToFirst()){Cursor e=db.programExercises(c.getLong(0));int n=0;while(e.moveToNext())n++;e.close();return n+" "+t("exercises","تمارين");}return "0";}finally{c.close();}}
    private long firstDayId(){Cursor c=db.activeDays();try{return c.moveToFirst()?c.getLong(0):0;}finally{c.close();}}
    private String fmt(double v){if(v==Math.rint(v))return String.valueOf((long)v);return String.format(Locale.US,"%.1f",v);}

    private void workout(){
        clear(t("Workout","التمرين"),t("Build it your way. Your plan is never locked.","عدّل خطتك زي ما تحب. مفيش حاجة مقفولة."));
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);Button prog=secondary(t("Programs","البرامج"));prog.setOnClickListener(v->programManager());Button lib=secondary(t("Exercise library","مكتبة التمارين"));lib.setOnClickListener(v->exerciseLibrary(-1,false));actions.addView(prog,new LinearLayout.LayoutParams(0,dp(48),1));actions.addView(hgap(8));actions.addView(lib,new LinearLayout.LayoutParams(0,dp(48),1));content.addView(actions);content.addView(gap(12));
        Cursor days=db.activeDays();if(!days.moveToFirst()){content.addView(tv(t("Create a program to start.","اعمل برنامج عشان تبدأ."),15,TEXT,false));days.close();return;}do{long did=days.getLong(0);String title=days.getString(1);LinearLayout c=card();LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.addView(tv(title,20,TEXT,true),new LinearLayout.LayoutParams(0,-2,1));Button more=secondary("⋯");more.setOnClickListener(v->dayMenu(did,title));row.addView(more,new LinearLayout.LayoutParams(dp(52),dp(42)));c.addView(row);Cursor e=db.programExercises(did);int count=0;while(e.moveToNext()){count++;String nm="ar".equals(lang)?e.getString(3):e.getString(2);c.addView(tv(nm+"  ·  "+e.getInt(4)+" × "+e.getInt(5)+"–"+e.getInt(6),13,TEXT,false));if(count>=6){c.addView(small("+ "+t("more","كمان")));break;}}e.close();c.addView(gap(8));LinearLayout buttons=new LinearLayout(this);Button start=primary(t("Start","ابدأ"));start.setOnClickListener(v->{activeSession=db.startSession(did,title);showActiveWorkout();});Button edit=secondary(t("Edit day","تعديل اليوم"));edit.setOnClickListener(v->editDay(did,title));buttons.addView(start,new LinearLayout.LayoutParams(0,dp(46),1));buttons.addView(hgap(8));buttons.addView(edit,new LinearLayout.LayoutParams(0,dp(46),1));c.addView(buttons);content.addView(c);content.addView(gap(10));}while(days.moveToNext());days.close();
        Button scratch=secondary(t("+ Create workout from scratch","+ اعمل تمرين من الصفر"));scratch.setOnClickListener(v->createProgramDialog());content.addView(scratch);
    }

    private void dayMenu(long dayId,String title){
        String[] items={t("Edit exercises","تعديل التمارين"),t("Rename day","تغيير اسم اليوم"),t("Move up","طلع اليوم فوق"),t("Move down","نزل اليوم تحت"),t("Delete day","احذف اليوم"),t("Start this day","ابدأ اليوم ده")};
        new AlertDialog.Builder(this).setTitle(title).setItems(items,(d,w)->{if(w==0)editDay(dayId,title);else if(w==1)renameDayDialog(dayId,title);else if(w==2){db.moveDay(dayId,-1);showPage(1);}else if(w==3){db.moveDay(dayId,1);showPage(1);}else{activeSession=db.startSession(dayId,title);showActiveWorkout();}}).show();
    }
    private void editDay(long dayId,String day){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(10),dp(4),dp(10),dp(4));
        Cursor c=db.programExercises(dayId);while(c.moveToNext()){final long pe=c.getLong(0), exid=c.getLong(1);String name="ar".equals(lang)?c.getString(3):c.getString(2);Button b=secondary(name+"  ·  "+c.getInt(4)+" × "+c.getInt(5)+"–"+c.getInt(6));b.setGravity("ar".equals(lang)?Gravity.RIGHT:Gravity.LEFT);b.setOnClickListener(v->programExerciseMenu(pe,exid,dayId,name));box.addView(b,new LinearLayout.LayoutParams(-1,dp(50)));box.addView(gap(6));}c.close();Button add=primary(t("+ Add exercise","+ أضف تمرين"));add.setOnClickListener(v->exerciseLibrary(dayId,false));box.addView(add);new AlertDialog.Builder(this).setTitle(day).setView(box).setNegativeButton(t("Close","إغلاق"),null).show();
    }
    private void programExerciseMenu(long pe,long exid,long dayId,String name){
        String[] items={t("Edit sets / reps / rest","تعديل المجموعات والتكرارات والراحة"),t("Replace exercise","استبدل التمرين"),t("Move up","طلّعه فوق"),t("Move down","نزّله تحت"),t("Remove exercise","احذف التمرين")};
        new AlertDialog.Builder(this).setTitle(name).setItems(items,(d,w)->{if(w==0)editProgramExerciseDialog(pe);else if(w==1)exerciseLibrary(dayId,true,pe);else if(w==2){db.moveProgramExercise(pe,-1);showPage(1);}else if(w==3){db.moveProgramExercise(pe,1);showPage(1);}else{db.deleteProgramExercise(pe);showPage(1);}}).show();
    }
    private void editProgramExerciseDialog(long pe){
        Cursor c=db.getReadableDatabase().rawQuery("SELECT target_sets,rep_min,rep_max,rest_sec,COALESCE(target_rpe,0),COALESCE(target_rir,0),COALESCE(note,'') FROM program_exercise WHERE id=?",new String[]{String.valueOf(pe)});
        if(!c.moveToFirst()){c.close();return;}int sets=c.getInt(0),min=c.getInt(1),max=c.getInt(2),rest=c.getInt(3);double rpe=c.getDouble(4),rir=c.getDouble(5);String note=c.getString(6);c.close();
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);EditText a=input(t("Sets","المجموعات"),true);a.setText(String.valueOf(sets));EditText b=input(t("Min reps","أقل تكرارات"),true);b.setText(String.valueOf(min));EditText cc=input(t("Max reps","أقصى تكرارات"),true);cc.setText(String.valueOf(max));EditText d=input(t("Rest seconds","الراحة بالثواني"),true);d.setText(String.valueOf(rest));EditText e=input("RPE",true);e.setText(rpe>0?fmt(rpe):"");EditText f=input("RIR",true);f.setText(rir>0?fmt(rir):"");EditText n=input(t("Note","ملاحظة"),false);n.setText(note);box.addView(a);box.addView(gap(7));box.addView(b);box.addView(gap(7));box.addView(cc);box.addView(gap(7));box.addView(d);box.addView(gap(7));box.addView(e);box.addView(gap(7));box.addView(f);box.addView(gap(7));box.addView(n);
        new AlertDialog.Builder(this).setTitle(t("Exercise settings","إعدادات التمرين")).setView(box).setPositiveButton(t("Save","حفظ"),(x,w)->{db.updateProgramExercise(pe,(int)read(a,sets),(int)read(b,min),(int)read(cc,max),(int)read(d,rest),read(e,0),read(f,0),n.getText().toString());showPage(1);}).setNegativeButton(t("Cancel","إلغاء"),null).show();
    }
    private void renameDayDialog(long id,String current){EditText e=input(t("Day name","اسم اليوم"),false);e.setText(current);new AlertDialog.Builder(this).setTitle(t("Rename day","تغيير اسم اليوم")).setView(e).setPositiveButton(t("Save","حفظ"),(d,w)->{if(e.getText().length()>0){db.renameDay(id,e.getText().toString());showPage(1);}}).setNegativeButton(t("Cancel","إلغاء"),null).show();}

    private void programManager(){Cursor c=db.programs();final List<Long> ids=new ArrayList<>();final List<String> names=new ArrayList<>();long ap=-1;while(c.moveToNext()){ids.add(c.getLong(0));names.add(c.getString(1));if(c.getInt(4)==1)ap=c.getLong(0);}c.close();LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);final long active=ap;for(int i=0;i<names.size();i++){final int k=i;Button b=secondary(names.get(i)+(ids.get(i)==active?"  ✓":""));b.setGravity("ar".equals(lang)?Gravity.RIGHT:Gravity.LEFT);b.setOnClickListener(v->{db.setActiveProgram(ids.get(k));showPage(1);});b.setOnLongClickListener(v->{programEditMenu(ids.get(k),names.get(k));return true;});box.addView(b,new LinearLayout.LayoutParams(-1,dp(50)));box.addView(gap(6));}Button add=primary(t("+ New program","+ برنامج جديد"));add.setOnClickListener(v->createProgramDialog());box.addView(add);box.addView(small(t("Tap to activate. Long-press to rename or delete.","اضغط لتفعيل البرنامج. ودوس مطول للتسمية أو الحذف.")));new AlertDialog.Builder(this).setTitle(t("Programs","البرامج")).setView(box).setNegativeButton(t("Close","إغلاق"),null).show();}
    private void programEditMenu(long id,String name){String[] it={t("Rename","تغيير الاسم"),t("Delete","حذف")};new AlertDialog.Builder(this).setTitle(name).setItems(it,(d,w)->{if(w==0){EditText e=input(t("Program name","اسم البرنامج"),false);e.setText(name);new AlertDialog.Builder(this).setTitle(t("Rename program","تغيير اسم البرنامج")).setView(e).setPositiveButton(t("Save","حفظ"),(x,y)->{if(e.getText().length()>0){db.renameProgram(id,e.getText().toString());showPage(1);}}).setNegativeButton(t("Cancel","إلغاء"),null).show();}else new AlertDialog.Builder(this).setTitle(t("Delete program?","تحذف البرنامج؟")).setMessage(t("Completed workout history stays intact.","سجل التمارين المكتملة هيفضل محفوظ.")).setPositiveButton(t("Delete","حذف"),(x,y)->{db.deleteProgram(id);showPage(1);}).setNegativeButton(t("Cancel","إلغاء"),null).show();}).show();}
    private void createProgramDialog(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);EditText name=input(t("Program name","اسم البرنامج"),false);EditText days=input(t("Days per week","عدد الأيام في الأسبوع"),true);days.setText("3");box.addView(name);box.addView(gap(8));box.addView(days);new AlertDialog.Builder(this).setTitle(t("New program","برنامج جديد")).setView(box).setPositiveButton(t("Create","إنشاء"),(d,w)->{String n=name.getText().toString().trim();int dy=(int)read(days,3);if(n.isEmpty())n=t("My Program","برنامجي");if(dy<1)dy=1;if(dy>7)dy=7;long id=db.createProgram(n,"custom",dy);db.setActiveProgram(id);showPage(1);}).setNegativeButton(t("Cancel","إلغاء"),null).show();
    }

    private void exerciseLibrary(long dayId,boolean replace){exerciseLibrary(dayId,replace,-1);}
    private void exerciseLibrary(long dayId,boolean replace,long programExerciseId){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);EditText q=input(t("Search exercise, muscle or equipment","ابحث عن تمرين أو عضلة أو جهاز"),false);box.addView(q);Button search=secondary(t("Search","بحث"));box.addView(gap(8));box.addView(search);LinearLayout results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);barcodeResults=results;box.addView(gap(8));box.addView(results);
        Runnable load=()->{results.removeAllViews();Cursor c=db.exercises(q.getText().toString());int count=0;while(c.moveToNext()&&count<80){final long id=c.getLong(0);String name="ar".equals(lang)?c.getString(2):c.getString(1);Button b=secondary(name+"  ·  "+c.getString(3)+" · "+c.getString(4));b.setGravity("ar".equals(lang)?Gravity.RIGHT:Gravity.LEFT);b.setOnClickListener(v->{if(dayId>0){if(replace)replaceExercise(programExerciseId,id);else{db.addProgramExercise(dayId,id);showPage(1);}}else exerciseDetail(id);});results.addView(b,new LinearLayout.LayoutParams(-1,dp(52)));results.addView(gap(5));count++;}c.close();if(count==0)results.addView(small(t("No exercise found.","ملقيناش تمرين بالبحث ده.")));};search.setOnClickListener(v->load.run());load.run();
        TextView create=tv(t("+ Create custom exercise","+ اعمل تمرين مخصص"),14,ACCENT,true);create.setPadding(0,dp(12),0,dp(8));create.setOnClickListener(v->customExercise(dayId,replace,programExerciseId));box.addView(create);
        ScrollView sc=new ScrollView(this);sc.addView(box);new AlertDialog.Builder(this).setTitle(replace?t("Replace exercise","استبدال التمرين"):t("Exercise library","مكتبة التمارين")).setView(sc).setNegativeButton(t("Close","إغلاق"),null).show();
    }
    private void replaceExercise(long pe,long newEx){db.replaceProgramExercise(pe,newEx);showPage(1);}
    private void exerciseDetail(long id){Cursor c=db.exercise(id);if(!c.moveToFirst()){c.close();return;}String name="ar".equals(lang)?c.getString(c.getColumnIndexOrThrow("arabic_name")):c.getString(c.getColumnIndexOrThrow("name"));String muscle=c.getString(c.getColumnIndexOrThrow("primary_muscle"));String equip=c.getString(c.getColumnIndexOrThrow("equipment"));String inst=c.getString(c.getColumnIndexOrThrow("instructions"));boolean fav=db.isFavorite(id);c.close();LinearLayout box=card();box.addView(tv(name,22,TEXT,true));box.addView(small(muscle+" · "+equip));box.addView(tv(inst==null?"":inst,14,TEXT,false));box.addView(gap(10));Button f=secondary(fav?t("★ Remove favorite","★ شيل من المفضلة"):t("☆ Add favorite","☆ أضف للمفضلة"));f.setOnClickListener(v->{db.toggleFavorite(id);exerciseDetail(id);});box.addView(f);new AlertDialog.Builder(this).setTitle(t("Exercise details","تفاصيل التمرين")).setView(box).setNegativeButton(t("Close","إغلاق"),null).show();}
    private void customExercise(long dayId,boolean replace,long pe){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);EditText en=input("Exercise name",false),ar=input("اسم التمرين",false),mus=input(t("Primary muscle","العضلة الأساسية"),false),eq=input(t("Equipment","الجهاز"),false),notes=input(t("Instructions / note","تعليمات / ملاحظة"),false);box.addView(en);box.addView(gap(7));box.addView(ar);box.addView(gap(7));box.addView(mus);box.addView(gap(7));box.addView(eq);box.addView(gap(7));box.addView(notes);new AlertDialog.Builder(this).setTitle(t("Custom exercise","تمرين مخصص")).setView(box).setPositiveButton(t("Save","حفظ"),(d,w)->{if(en.getText().length()==0)return;db.addCustomExercise(en.getText().toString(),ar.getText().toString(),mus.getText().toString(),eq.getText().toString(),"WEIGHT_REPS",false,notes.getText().toString());if(dayId>0){Cursor c=db.exercises(en.getText().toString());if(c.moveToFirst()){long id=c.getLong(0);if(replace)replaceExercise(pe,id);else{db.addProgramExercise(dayId,id);showPage(1);}}c.close();}}).setNegativeButton(t("Cancel","إلغاء"),null).show();}

    private void showActiveWorkout(){
        clear(t("Live workout","التمرين الحالي"),t("Every set is editable. Check it when you finish.","كل Set قابل للتعديل. علّم عليه لما تخلص."));
        if(activeSession<0){showPage(1);return;}
        LinearLayout timer=card();timer.addView(tv(t("Rest timer","مؤقت الراحة"),14,TEXT,true));restLabel=tv("00:00",30,ACCENT,true);restLabel.setGravity(Gravity.CENTER);timer.addView(restLabel);
        Button r=secondary(t("Start 90 sec","ابدأ 90 ثانية"));r.setOnClickListener(v->startRest(90));timer.addView(r);content.addView(timer);content.addView(gap(12));
        Cursor ex=db.sessionExercises(activeSession);
        while(ex.moveToNext()){
            long wid=ex.getLong(0),eid=ex.getLong(1);String tracking=ex.getString(8);int restForExercise=db.sessionExerciseRest(activeSession,wid);String name="ar".equals(lang)?ex.getString(3):ex.getString(2);
            LinearLayout c=card();LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);head.addView(tv(name,19,TEXT,true),new LinearLayout.LayoutParams(0,-2,1));
            Button skip=secondary(ex.getInt(7)==1?t("Skipped","تم التخطي"):t("Skip","تخطي"));skip.setOnClickListener(v->{db.setExerciseSkipped(wid,ex.getInt(7)==0);showActiveWorkout();});head.addView(skip,new LinearLayout.LayoutParams(dp(72),dp(42)));c.addView(head);
            c.addView(small(t(ex.getString(4)+" · "+ex.getString(5),"العضلة: "+ex.getString(4)+" · "+ex.getString(5))));
            Button rr=secondary(t("Rest "+restForExercise+" sec","راحة "+restForExercise+" ثانية"));rr.setOnClickListener(v->startRest(restForExercise));c.addView(rr);
            long startedAt=db.sessionStartedAt(activeSession);Cursor best=db.previousTopSet(eid,startedAt);double prevWeight=0;int prevReps=0;if(best.moveToFirst()){if(!best.isNull(0))prevWeight=best.getDouble(0);if(!best.isNull(1))prevReps=best.getInt(1);}best.close();
            Cursor sets=db.sets(wid);
            while(sets.moveToNext()){
                long sid=sets.getLong(0);int order=sets.getInt(1);String setType=sets.getString(2);CheckBox done=new CheckBox(this);done.setText(t("Done","تم"));done.setTextColor(TEXT);done.setChecked(sets.getInt(8)==1);
                LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.addView(tv(String.valueOf(order),12,MUTED,true),new LinearLayout.LayoutParams(dp(24),dp(48)));row.addView(tv(setType,10,MUTED,true),new LinearLayout.LayoutParams(dp(58),dp(48)));
                if("TIMED".equalsIgnoreCase(tracking)){
                    EditText dur=input(t("seconds","ثانية"),true);if(!sets.isNull(5))dur.setText(String.valueOf(sets.getInt(5)));row.addView(dur,new LinearLayout.LayoutParams(0,dp(46),1));row.addView(done,new LinearLayout.LayoutParams(dp(70),dp(46)));
                    done.setOnCheckedChangeListener((b,checked)->{db.updateSet(sid,0,null,safeInt(dur),0,0,checked,"");if(checked)startRest(restForExercise);});
                }else if("REPS".equalsIgnoreCase(tracking)){
                    EditText reps=input(t("reps","تكرار"),true);if(!sets.isNull(4))reps.setText(String.valueOf(sets.getInt(4)));else if(prevReps>0)reps.setText(String.valueOf(prevReps));row.addView(reps,new LinearLayout.LayoutParams(0,dp(46),1));row.addView(done,new LinearLayout.LayoutParams(dp(70),dp(46)));
                    done.setOnCheckedChangeListener((b,checked)->{db.updateSet(sid,0,safeInt(reps),null,0,0,checked,"");if(checked)startRest(restForExercise);});
                }else{
                    EditText weight=input(weightUnit(),true),reps=input(t("reps","تكرار"),true),rpe=input("RPE",true);if(!sets.isNull(3))weight.setText(fmt(toDisplayWeight(sets.getDouble(3))));else if(prevWeight>0)weight.setText(fmt(toDisplayWeight(prevWeight)));if(!sets.isNull(4))reps.setText(String.valueOf(sets.getInt(4)));else if(prevReps>0)reps.setText(String.valueOf(prevReps));if(!sets.isNull(6))rpe.setText(fmt(sets.getDouble(6)));row.addView(weight,new LinearLayout.LayoutParams(0,dp(46),1));row.addView(hgap(5));row.addView(reps,new LinearLayout.LayoutParams(0,dp(46),1));row.addView(hgap(5));row.addView(rpe,new LinearLayout.LayoutParams(0,dp(46),1));row.addView(done,new LinearLayout.LayoutParams(dp(64),dp(46)));
                    done.setOnCheckedChangeListener((b,checked)->{db.updateSet(sid,fromDisplayWeight(read(weight,0)),safeInt(reps),null,read(rpe,0),0,checked,"");if(checked)startRest(restForExercise);});
                }
                c.addView(row);
            }
            sets.close();Button add=secondary(t("+ Add set","+ أضف Set"));add.setOnClickListener(v->addSetTypeDialog(wid));c.addView(gap(6));c.addView(add);content.addView(c);content.addView(gap(10));
        }
        ex.close();
        LinearLayout finish=new LinearLayout(this);Button end=primary(t("Finish workout","إنهاء التمرين"));end.setOnClickListener(v->finishWorkout());Button abort=secondary(t("Discard","إلغاء الجلسة"));abort.setOnClickListener(v->confirmDiscard());finish.addView(end,new LinearLayout.LayoutParams(0,dp(48),1));finish.addView(hgap(8));finish.addView(abort,new LinearLayout.LayoutParams(0,dp(48),1));content.addView(finish);
    }
    private void addSetTypeDialog(long wid){String[] en={"Working","Warm-up","Drop set","AMRAP","Failure","Assisted"};String[] ar={"أساسي","إحماء","دروب سِت","AMRAP","للفشل","مساعد"};new AlertDialog.Builder(this).setTitle(t("Set type","نوع الـSet")).setItems("ar".equals(lang)?ar:en,(d,w)->{try{db.addSet(wid,en[w].toLowerCase(java.util.Locale.US).replace("-","_"));showActiveWorkout();}catch(Exception e){toast(t("Could not add the set.","حصلت مشكلة في إضافة الـSet."));}}).setNegativeButton(t("Cancel","إلغاء"),null).show();}
    private void startRest(int sec){if(restTimer!=null)restTimer.cancel();restTimer=new CountDownTimer(sec*1000L,1000){public void onTick(long left){if(restLabel!=null)restLabel.setText(String.format(Locale.US,"%02d:%02d",left/60000,(left/1000)%60));}public void onFinish(){if(restLabel!=null)restLabel.setText(t("Done","خلصت"));toast(t("Rest finished","الراحة خلصت"));}};restTimer.start();}
    private void finishWorkout(){EditText n=input(t("Workout note (optional)","ملاحظة التمرين (اختياري)"),false);new AlertDialog.Builder(this).setTitle(t("Finish workout?","تخلص التمرين؟")).setView(n).setPositiveButton(t("Finish","إنهاء"),(d,w)->{db.finishSession(activeSession,n.getText().toString());activeSession=-1;showPage(1);}).setNegativeButton(t("Keep training","كمّل"),null).show();}
    private void confirmDiscard(){new AlertDialog.Builder(this).setTitle(t("Discard this workout?","تلغي الجلسة دي؟")).setMessage(t("The current session will be deleted.","الجلسة الحالية هتتحذف بالكامل.")).setPositiveButton(t("Discard","إلغاء الجلسة"),(d,w)->{db.deleteSession(activeSession);activeSession=-1;showPage(1);}).setNegativeButton(t("Keep","خليك"),null).show();}
    private int safeInt(EditText e){try{return Integer.parseInt(e.getText().toString().trim());}catch(Exception x){return 0;}}
    private double read(EditText e,double fallback){try{String s=e.getText().toString().trim();return s.isEmpty()?fallback:Double.parseDouble(s);}catch(Exception x){return fallback;}}

    private void lookupBarcode(String code, LinearLayout results){
        results.removeAllViews();
        results.addView(small(t("Looking up product…","جاري البحث عن المنتج…")));
        ExecutorService ex=Executors.newSingleThreadExecutor();
        ex.execute(()->{
            try{
                OpenFoodFactsClient.Product p=new OpenFoodFactsClient().get(code);
                runOnUiThread(()->{
                    results.removeAllViews();
                    if(!p.found||p.name==null||p.name.trim().isEmpty()){
                        results.addView(small(t("Product not found. Add it from the package label.","المنتج مش موجود. ضيفه من الملصق الغذائي.")));
                        return;
                    }
                    LinearLayout card=card();
                    card.addView(tv(p.name,17,TEXT,true));
                    if(p.brand!=null&&!p.brand.isEmpty()) card.addView(small(p.brand));
                    card.addView(small(fmt(p.kcal)+" kcal · "+fmt(p.protein)+"g protein · "+fmt(p.carbs)+"g carbs · "+fmt(p.fat)+"g fat / 100g"));
                    card.addView(small(t("Imported from Open Food Facts. Verify against the physical label before relying on it.","مستورَد من Open Food Facts. راجع الملصق الفعلي قبل الاعتماد عليه.")));
                    Button save=primary(t("Add as imported food","أضفه كمنتج مستورد"));
                    save.setOnClickListener(v->{long src=db.sourceIdByType("barcode");try{db.addImportedFood(p.name,"",p.brand,p.barcode,p.kcal,p.protein,p.carbs,p.fat,p.fiber,"Imported from Open Food Facts; verify package label.",src);toast(t("Added as unverified imported data.","اتضاف كبيانات مستوردة غير موثقة."));results.removeAllViews();results.addView(small(t("Added. Search it again to log it.","اتضاف. دور عليه تاني عشان تسجله.")));}catch(Exception e){toast(t("Could not add this product.","مش قادر أضيف المنتج ده."));}});
                    card.addView(gap(8));card.addView(save);results.addView(card);
                });
            }catch(Exception e){runOnUiThread(()->{results.removeAllViews();results.addView(small(t("Lookup failed. Check the barcode and connection.","البحث فشل. راجع الباركود والاتصال.")));});}
            finally{ex.shutdown();}
        });
    }

    private void logFoodDialog(long foodId,String foodName){
        LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);
        EditText grams=input(t("Amount in grams","الكمية بالجرام"),true);grams.setText("100");
        EditText meal=input(t("Meal name","اسم الوجبة"),false);meal.setText(t("Meal","وجبة"));
        b.addView(grams);b.addView(gap(8));b.addView(meal);
        new AlertDialog.Builder(this).setTitle(foodName).setView(b).setPositiveButton(t("Add","إضافة"),(d,w)->{double g=read(grams,100);if(g<=0){toast(t("Enter a positive amount.","اكتب كمية أكبر من صفر."));return;}try{db.logFood(foodId,g,meal.getText().toString().trim().isEmpty()?t("Meal","وجبة"):meal.getText().toString().trim());nutrition();}catch(Exception e){toast(t("Could not log food.","مش قادر أسجل الأكل."));}}).setNegativeButton(t("Cancel","إلغاء"),null).show();
    }

    private void editMealItemDialog(long itemId,String name,double current){
        EditText g=input(t("Amount in grams","الكمية بالجرام"),true);g.setText(fmt(current));
        new AlertDialog.Builder(this).setTitle(name).setView(g).setPositiveButton(t("Save","حفظ"),(d,w)->{try{double v=read(g,current);if(v<=0)throw new IllegalArgumentException();db.updateMealItem(itemId,v);nutrition();}catch(Exception e){toast(t("Enter a valid positive amount.","اكتب كمية صحيحة أكبر من صفر."));}}).setNegativeButton(t("Cancel","إلغاء"),null).show();
    }

    private void customFoodDialog(){
        LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);
        EditText name=input(t("Food / product name","اسم الأكل / المنتج"),false);EditText ar=input(t("Arabic name","الاسم بالعربي"),false);EditText brand=input(t("Brand (optional)","البراند (اختياري)"),false);EditText barcode=input(t("Barcode (optional)","الباركود (اختياري)"),true);
        EditText kcal=input("kcal / 100g",true), protein=input("Protein g / 100g",true), carbs=input("Carbs g / 100g",true), fat=input("Fat g / 100g",true), fiber=input("Fiber g / 100g",true), sugar=input("Sugar g / 100g",true), sodium=input("Sodium mg / 100g",true);
        EditText notes=input(t("Source / label notes","ملاحظات المصدر / الملصق"),false);
        for(EditText x:new EditText[]{name,ar,brand,barcode,kcal,protein,carbs,fat,fiber,sugar,sodium,notes}){b.addView(x);b.addView(gap(6));}
        new AlertDialog.Builder(this).setTitle(t("Custom food","أكل مخصص")).setView(b).setPositiveButton(t("Save","حفظ"),(d,w)->{
            try{if(name.getText().toString().trim().isEmpty())throw new IllegalArgumentException();db.addCustomFood(name.getText().toString().trim(),ar.getText().toString().trim(),brand.getText().toString().trim(),"custom","as_served",barcode.getText().toString().trim(),read(kcal,0),read(protein,0),read(carbs,0),read(fat,0),read(fiber,0),read(sugar,0),read(sodium,0),notes.getText().toString().trim());toast(t("Saved as user-entered food.","اتحفظ كأكل مدخّل من المستخدم."));nutrition();}catch(Exception e){toast(t("Check the required name and nutrition values.","راجع الاسم وقيم التغذية."));}
        }).setNegativeButton(t("Cancel","إلغاء"),null).show();
    }

    private void foodReviewDialog(long fid){
        Cursor c=db.food(fid);if(!c.moveToFirst()){c.close();return;}
        String name=c.getString(c.getColumnIndexOrThrow("name"));String verification=c.getString(c.getColumnIndexOrThrow("verification"));String notes=c.getString(c.getColumnIndexOrThrow("notes"));String srcId=c.isNull(c.getColumnIndexOrThrow("source_id"))?"":String.valueOf(c.getLong(c.getColumnIndexOrThrow("source_id")));c.close();
        String[] choices={"verified","reference","imported","user","rejected"};String[] labels={t("Verified","موثق"),t("Reference","مرجعي"),t("Imported","مستورد"),t("User entered","مدخل يدويًا"),t("Rejected","مرفوض")};
        int selected=0;for(int i=0;i<choices.length;i++)if(choices[i].equals(verification))selected=i;final int[] pick={selected};
        new AlertDialog.Builder(this).setTitle(t("Review food data · "+name,"مراجعة بيانات الأكل · "+name)).setSingleChoiceItems(labels,selected,(d,w)->pick[0]=w).setMessage(t("Change the verification state only when you have a traceable source or physical label.","غيّر حالة التحقق فقط لما يكون عندك مصدر قابل للتتبع أو ملصق فعلي."))
                .setPositiveButton(t("Save","حفظ"),(d,w)->{String v=choices[pick[0]];db.getWritableDatabase().execSQL("UPDATE food SET verification=?, last_verified=? WHERE id=?",new Object[]{v,"verified".equals(v)?System.currentTimeMillis():null,fid});toast(t("Food status updated.","حالة الأكل اتحدثت."));nutrition();}).setNegativeButton(t("Close","إغلاق"),null).show();
    }

    private void savedMealsDialog(){
        Cursor c=db.savedMeals();LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);int n=0;while(c.moveToNext()){n++;final long id=c.getLong(0);String name=c.getString(1);Button b=secondary(name);b.setGravity("ar".equals(lang)?Gravity.RIGHT:Gravity.LEFT);b.setOnClickListener(v->{db.logSavedMeal(id,name);toast(t("Meal added.","الوجبة اتضافت."));nutrition();});box.addView(b,new LinearLayout.LayoutParams(-1,dp(48)));box.addView(gap(6));}c.close();if(n==0)box.addView(small(t("Save a meal from today's food log to reuse it later.","احفظ وجبة من سجل النهارده عشان تستخدمها بعدين.")));new AlertDialog.Builder(this).setTitle(t("Saved meals","الوجبات المحفوظة")).setView(box).setNegativeButton(t("Close","إغلاق"),null).show();
    }

    private void saveMealDialog(long mealId,String mealName){EditText n=input(t("Saved meal name","اسم الوجبة المحفوظة"),false);n.setText(mealName);new AlertDialog.Builder(this).setTitle(t("Save meal","حفظ الوجبة")).setView(n).setPositiveButton(t("Save","حفظ"),(d,w)->{String name=n.getText().toString().trim();if(name.isEmpty())name=mealName;db.saveMeal(mealId,name);toast(t("Saved meal.","الوجبة اتحفظت."));}).setNegativeButton(t("Cancel","إلغاء"),null).show();}

    private void progress(){
        clear(t("Progress","التقدم"),t("See what changed instead of guessing.","شوف إيه اللي اتغير بدل ما تعتمد على الإحساس بس."));
        LinearLayout summary=card();summary.addView(tv(t("Training","التمرين"),16,TEXT,true));summary.addView(tv(db.completedWorkoutsThisWeek()+" "+t("workouts this week","تمرين هذا الأسبوع"),24,ACCENT,true));summary.addView(small(fmt(db.volumeSince(System.currentTimeMillis()-7L*24*3600*1000))+" kg·reps · "+t("weekly volume","حجم أسبوعي")));content.addView(summary);content.addView(gap(10));
        Cursor rs=db.recentSessions(12);while(rs.moveToNext()){long sid=rs.getLong(0);String nm=rs.getString(1);long ts=rs.getLong(2);LinearLayout c=card();c.addView(tv(nm,16,TEXT,true));c.addView(small(new SimpleDateFormat("EEE, d MMM · HH:mm",Locale.US).format(new Date(ts))));c.addView(small(fmt(db.sessionVolume(sid))+" kg·reps · "+db.completedSets(sid)+" "+t("sets","مجموعات")));content.addView(c);content.addView(gap(7));}rs.close();
        Cursor w=db.weights();List<Float> vals=new ArrayList<>();List<String> labels=new ArrayList<>();while(w.moveToNext()){vals.add(0,(float)toDisplayWeight(w.getDouble(1)));labels.add(0,new SimpleDateFormat("MM/dd",Locale.US).format(new Date(w.getLong(6))));}w.close();if(!vals.isEmpty()){content.addView(tv(t("Weight trend","اتجاه الوزن"),16,TEXT,true));TrendChartView chart=new TrendChartView(this);chart.setValues(vals);content.addView(chart,new LinearLayout.LayoutParams(-1,dp(190)));content.addView(small(t("Latest: "+fmt(vals.get(vals.size()-1))+" "+weightUnit(),"آخر وزن: "+fmt(vals.get(vals.size()-1))+" "+weightUnit())));}
        Button add=primary(t("Log measurements","سجل قياسات"));add.setOnClickListener(v->measurementDialog());content.addView(gap(10));content.addView(add);
    }

    private void measurementDialog(){
        LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);EditText w=input(t("Weight "+weightUnit(),"الوزن "+weightUnit()),true);EditText waist=input(t("Waist "+measureUnit()+" (optional)","الخصر "+measureUnit()+" (اختياري)"),true);EditText chest=input(t("Chest "+measureUnit()+" (optional)","الصدر "+measureUnit()+" (اختياري)"),true);EditText arm=input(t("Arm "+measureUnit()+" (optional)","الذراع "+measureUnit()+" (اختياري)"),true);EditText thigh=input(t("Thigh "+measureUnit()+" (optional)","الفخذ "+measureUnit()+" (اختياري)"),true);EditText note=input(t("Note","ملاحظة"),false);b.addView(w);b.addView(gap(6));b.addView(waist);b.addView(gap(6));b.addView(chest);b.addView(gap(6));b.addView(arm);b.addView(gap(6));b.addView(thigh);b.addView(gap(6));b.addView(note);new AlertDialog.Builder(this).setTitle(t("Measurements","القياسات")).setView(b).setPositiveButton(t("Save","حفظ"),(d,x)->{try{double ww=read(w,0);if(ww<=0)throw new IllegalArgumentException();double wa=read(waist,0),ch=read(chest,0),ar=read(arm,0),th=read(thigh,0);db.addMeasurements(fromDisplayWeight(ww),wa>0?fromDisplayMeasure(wa):0,ch>0?fromDisplayMeasure(ch):0,ar>0?fromDisplayMeasure(ar):0,th>0?fromDisplayMeasure(th):0,note.getText().toString().trim());progress();}catch(Exception e){toast(t("Enter a valid weight.","اكتب وزن صحيح."));}}).setNegativeButton(t("Cancel","إلغاء"),null).show();}

    private void profile(){
        clear(t("Profile","حسابي"),t("Your plan, preferences and reminders.","خطتك، إعداداتك، وتذكيراتك."));
        Cursor c=db.profile();
        if(!c.moveToFirst()){ c.close(); return; }
        double weight=c.getDouble(c.getColumnIndexOrThrow("weight_kg"));
        double height=c.getDouble(c.getColumnIndexOrThrow("height_cm"));
        int age=c.getInt(c.getColumnIndexOrThrow("age"));
        String sex=c.getString(c.getColumnIndexOrThrow("sex"));
        String goal=c.getString(c.getColumnIndexOrThrow("goal"));
        String activity=c.getString(c.getColumnIndexOrThrow("activity"));
        double kcal=c.getDouble(c.getColumnIndexOrThrow("calorie_target"));
        double protein=c.getDouble(c.getColumnIndexOrThrow("protein_target"));
        double carbs=c.getDouble(c.getColumnIndexOrThrow("carb_target"));
        double fat=c.getDouble(c.getColumnIndexOrThrow("fat_target"));
        c.close();

        LinearLayout plan=card();
        plan.addView(tv(t("Current plan","الخطة الحالية"),16,TEXT,true));
        String p1=t("Weight: ","الوزن: ")+fmt(toDisplayWeight(weight))+" "+weightUnit()+" · "+t("Height: ","الطول: ")+fmt(toDisplayHeight(height))+" "+heightUnit();
        plan.addView(small(p1));
        String p2=t("Age: ","السن: ")+age+" · "+t("Goal: ","الهدف: ")+goalLabel(goal)+" · "+t("Activity: ","النشاط: ")+activityLabel(activity);
        plan.addView(small(p2));
        String p3=t("Targets: ","الأهداف: ")+fmt(kcal)+" kcal · "+fmt(protein)+"g protein · "+fmt(carbs)+"g carbs · "+fmt(fat)+"g fat";
        plan.addView(small(p3));
        Button edit=primary(t("Edit profile & targets","تعديل البيانات والأهداف"));
        edit.setOnClickListener(v->editProfileDialog());
        plan.addView(gap(8));
        plan.addView(edit);
        content.addView(plan);
        content.addView(gap(10));

        LinearLayout pref=card();
        pref.addView(tv(t("Preferences","الإعدادات"),16,TEXT,true));
        Button langBtn=secondary("ar".equals(lang)?"العربية  ✓":"English  ✓");
        langBtn.setOnClickListener(v->{
            lang="ar".equals(lang)?"en":"ar";
            db.setSetting("lang",lang);
            getWindow().getDecorView().setLayoutDirection("ar".equals(lang)?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);
            applyDirection();
            showPage(4);
        });
        pref.addView(langBtn);
        pref.addView(gap(6));
        Button unit=secondary("imperial".equals(units)?t("Units: lb / in","الوحدات: lb / in"):t("Units: kg / cm","الوحدات: kg / cm"));
        unit.setOnClickListener(v->{
            units="imperial".equals(units)?"metric":"imperial";
            db.setSetting("units",units);
            db.updateProfile(weight,height,age,"male".equals(sex),kcal,protein,carbs,fat,goal,activity,lang,units);
            showPage(4);
        });
        pref.addView(unit);
        content.addView(pref);
        content.addView(gap(8));

        Button quote=secondary(t("Quote notifications","إشعارات الاقتباسات"));
        quote.setOnClickListener(v->quoteNotificationsDialog());
        content.addView(quote);
        content.addView(gap(6));
        Button reminder=secondary(t("Daily reminder","التذكير اليومي"));
        reminder.setOnClickListener(v->dailyReminderDialog());
        content.addView(reminder);
        content.addView(gap(6));
        Button export=secondary(t("Export backup","تصدير نسخة احتياطية"));
        export.setOnClickListener(v->exportBackup());
        content.addView(export);
        content.addView(gap(6));
        Button restore=secondary(t("Restore backup","استعادة نسخة احتياطية"));
        restore.setOnClickListener(v->importBackupPicker());
        content.addView(restore);
    }

    private String goalLabel(String g){if("ar".equals(lang)){if("gain".equals(g))return "زيادة / بناء عضلات";if("lose".equals(g))return "خسارة دهون";return "ثبات";}return g;}
    private String activityLabel(String a){if("ar".equals(lang)){if("sedentary".equals(a))return "قليل";if("light".equals(a))return "خفيف";if("moderate".equals(a))return "متوسط";if("high".equals(a))return "عالي";return "عالي جدًا";}return a;}

    private void editProfileDialog(){
        Cursor c=db.profile();if(!c.moveToFirst()){c.close();return;}double weight=c.getDouble(c.getColumnIndexOrThrow("weight_kg")),height=c.getDouble(c.getColumnIndexOrThrow("height_cm"));int age=c.getInt(c.getColumnIndexOrThrow("age"));boolean male="male".equals(c.getString(c.getColumnIndexOrThrow("sex")));String goal=c.getString(c.getColumnIndexOrThrow("goal")),activity=c.getString(c.getColumnIndexOrThrow("activity"));double kcal=c.getDouble(c.getColumnIndexOrThrow("calorie_target")),pro=c.getDouble(c.getColumnIndexOrThrow("protein_target")),carb=c.getDouble(c.getColumnIndexOrThrow("carb_target")),fat=c.getDouble(c.getColumnIndexOrThrow("fat_target"));c.close();
        LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);EditText w=input(weightUnit(),true);w.setText(fmt(toDisplayWeight(weight)));EditText h=input(heightUnit(),true);h.setText(fmt(toDisplayHeight(height)));EditText a=input(t("Age","السن"),true);a.setText(String.valueOf(age));EditText kc=input("kcal",true);kc.setText(fmt(kcal));EditText pp=input("Protein g",true);pp.setText(fmt(pro));EditText cb=input("Carbs g",true);cb.setText(fmt(carb));EditText ff=input("Fat g",true);ff.setText(fmt(fat));for(EditText x:new EditText[]{w,h,a,kc,pp,cb,ff}){b.addView(x);b.addView(gap(6));}new AlertDialog.Builder(this).setTitle(t("Edit profile","تعديل البيانات")).setView(b).setPositiveButton(t("Save","حفظ"),(d,x)->{try{double ww=fromDisplayWeight(read(w,weight)),hh=fromDisplayHeight(read(h,height));int aa=(int)read(a,age);double kk=read(kc,kcal),p=read(pp,pro),cc=read(cb,carb),f=read(ff,fat);if(ww<=0||hh<=0||aa<=0||kk<=0||p<0||cc<0||f<0)throw new IllegalArgumentException();db.updateProfile(ww,hh,aa,male,kk,p,cc,f,goal,activity,lang,units);profile();}catch(Exception e){toast(t("Check your values.","راجع القيم."));}}).setNegativeButton(t("Cancel","إلغاء"),null).show();
    }

    private void quoteNotificationsDialog(){
        boolean enabled="1".equals(db.getSetting("quote_notifications_enabled","0"));
        int hours=parseInt(db.getSetting("quote_interval_hours","2"),2);
        boolean quiet="1".equals(db.getSetting("quote_quiet_enabled","1"));
        LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);
        String[] opts={t("Off","إيقاف"),t("Every 1 hour","كل ساعة"),t("Every 2 hours","كل ساعتين"),t("Every 3 hours","كل 3 ساعات"),t("Every 4 hours","كل 4 ساعات")};
        final int[] pick={enabled?hours:0};
        for(int i=0;i<opts.length;i++){final int k=i;Button x=secondary((pick[0]==k?"✓ ":"")+opts[i]);x.setGravity("ar".equals(lang)?Gravity.RIGHT:Gravity.LEFT);x.setOnClickListener(v->{pick[0]=k;for(int j=0;j<b.getChildCount();j++){View child=b.getChildAt(j);if(child instanceof Button){Button bb=(Button)child;int idx=j;bb.setText((idx==k?"✓ ":"")+opts[idx]);}}});b.addView(x,new LinearLayout.LayoutParams(-1,dp(46)));b.addView(gap(5));}
        CheckBox q=new CheckBox(this);q.setText(t("Quiet hours 23:00–07:00","ساعات هدوء 23:00–07:00"));q.setTextColor(TEXT);q.setChecked(quiet);b.addView(q);
        b.addView(gap(6));b.addView(small(t("Notifications use a single ongoing slot, and quiet hours only suppress quote notifications.","الإشعارات بتستخدم خانة واحدة، وساعات الهدوء بتوقف إشعارات الاقتباسات فقط.")));
        new AlertDialog.Builder(this).setTitle(t("Quote notifications","إشعارات الاقتباسات")).setView(b).setPositiveButton(t("Save","حفظ"),(d,w)->{boolean en=pick[0]>0;int h=en?pick[0]:2;if(en&&!notificationsAllowed()){requestNotificationPermission();return;}db.setSetting("quote_notifications_enabled",en?"1":"0");db.setSetting("quote_interval_hours",String.valueOf(h));db.setSetting("quote_quiet_enabled",q.isChecked()?"1":"0");ReminderScheduler.configureQuotes(this,en,h);profile();}).setNegativeButton(t("Cancel","إلغاء"),null).show();
    }

    private void importBackupPicker(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");try{startActivityForResult(i,REQUEST_IMPORT_BACKUP);}catch(Exception e){toast(t("Could not open file picker.","مش قادر أفتح اختيار الملفات."));}}
    private void restoreBackup(Uri uri){new Thread(()->{try{java.io.InputStream in=getContentResolver().openInputStream(uri);if(in==null)throw new java.io.IOException();java.io.ByteArrayOutputStream b=new java.io.ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)b.write(buf,0,n);in.close();BackupManager.importJson(db,b.toString(java.nio.charset.StandardCharsets.UTF_8));runOnUiThread(()->{toast(t("Backup restored. Restarting view…","تمت استعادة النسخة. جاري تحديث التطبيق…"));lang=db.getSetting("lang","en");units=db.getSetting("units","metric");applyDirection();showPage(0);});}catch(Exception e){runOnUiThread(()->toast(t("Restore failed.","فشلت الاستعادة.")));}}).start();}

    private void nutrition(){
        clear(t("Nutrition","التغذية"),t("Log by grams. Exact products can be added by barcode or label.","سجل بالجرام. والمنتجات المعبأة تتسجل بالباركود أو الملصق."));double[] trg=profileTargets(),tot=db.todayTotals();LinearLayout s=card();s.addView(tv(t("Today","النهارده"),16,TEXT,true));s.addView(tv(fmt(tot[0])+" / "+fmt(trg[0])+" kcal",27,ACCENT,true));addProgress(s,tot[0],trg[0]);s.addView(gap(8));macroLine(s,t("Protein","البروتين"),tot[1],trg[1]);macroLine(s,t("Carbs","الكارب"),tot[2],trg[2]);macroLine(s,t("Fat","الدهون"),tot[3],trg[3]);content.addView(s);content.addView(gap(10));
        LinearLayout actions=new LinearLayout(this);Button add=primary(t("+ Add food","+ أضف أكل"));add.setOnClickListener(v->foodSearch());Button custom=secondary(t("+ Custom food","+ أكل مخصص"));custom.setOnClickListener(v->customFoodDialog());actions.addView(add,new LinearLayout.LayoutParams(0,dp(48),1));actions.addView(hgap(8));actions.addView(custom,new LinearLayout.LayoutParams(0,dp(48),1));content.addView(actions);content.addView(gap(8));Button saved=secondary(t("Saved meals","الوجبات المحفوظة"));saved.setOnClickListener(v->savedMealsDialog());content.addView(saved);content.addView(gap(12));
        Cursor meals=db.todayMeals();while(meals.moveToNext()){long mid=meals.getLong(0);String mealName=meals.getString(1);LinearLayout m=card();LinearLayout mh=new LinearLayout(this);mh.setGravity(Gravity.CENTER_VERTICAL);mh.addView(tv(mealName,16,TEXT,true),new LinearLayout.LayoutParams(0,-2,1));Button saveMeal=secondary(t("Save","حفظ"));saveMeal.setOnClickListener(v->saveMealDialog(mid,mealName));mh.addView(saveMeal,new LinearLayout.LayoutParams(dp(64),dp(42)));m.addView(mh);m.addView(small(fmt(meals.getDouble(3))+" kcal · "+fmt(meals.getDouble(2))+" g"));Cursor items=db.mealItems(mid);while(items.moveToNext()){final long itemId=items.getLong(0);String nm="ar".equals(lang)?items.getString(2):items.getString(1);Button it=secondary(nm+" · "+fmt(items.getDouble(4))+" g · "+fmt(items.getDouble(5))+" kcal");it.setGravity("ar".equals(lang)?Gravity.RIGHT:Gravity.LEFT);it.setOnClickListener(v->editMealItemDialog(itemId,nm,items.getDouble(4)));it.setOnLongClickListener(v->{db.deleteMealItem(itemId);nutrition();return true;});m.addView(it,new LinearLayout.LayoutParams(-1,dp(45)));m.addView(gap(4));}items.close();content.addView(m);content.addView(gap(10));}meals.close();
        LinearLayout q=card();q.addView(tv(t("Data quality","جودة البيانات"),15,TEXT,true));q.addView(small(t("Verified packaged values should come from a current product label or traceable source. Reference foods are visibly labeled and are not presented as exact supermarket products.","المنتج المعبأ الموثق لازم يبقى مبني على ملصق حالي أو مصدر قابل للتتبع. الأطعمة المرجعية واضحة ومش بتتقدم كأنها منتج سوبرماركت بعينه.")));content.addView(q);
    }
    private void foodSearch(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);EditText q=input(t("Food / product name","اسم الأكل أو المنتج"),false);EditText barcode=input(t("Barcode (optional)","الباركود (اختياري)"),true);barcodeField=barcode;box.addView(q);box.addView(gap(7));box.addView(barcode);LinearLayout scanRow=new LinearLayout(this);Button scan=secondary(t("Scan barcode","امسح الباركود"));Button findRemote=secondary(t("Lookup","بحث"));scanRow.addView(scan,new LinearLayout.LayoutParams(0,dp(45),1));scanRow.addView(hgap(6));scanRow.addView(findRemote,new LinearLayout.LayoutParams(0,dp(45),1));box.addView(gap(7));box.addView(scanRow);TextView hint=small(t("Packaged products: verify the nutrition label before treating imported data as trusted.","المنتجات المعبأة: راجع الملصق الغذائي قبل اعتماد البيانات المستوردة."));box.addView(hint);Button search=primary(t("Search food","ابحث"));box.addView(gap(7));box.addView(search);LinearLayout results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);barcodeResults=results;box.addView(gap(8));box.addView(results);
        Runnable load=()->{results.removeAllViews();Cursor c=db.foods(q.getText().toString());int count=0;while(c.moveToNext()&&count<100){final long fid=c.getLong(0);String nm="ar".equals(lang)?c.getString(2):c.getString(1);String tag=c.getString(13);String dq=DataQualityMath.status(c.getDouble(6),c.getDouble(7),c.getDouble(8),c.getDouble(9));Button b=secondary(nm+" · "+fmt(c.getDouble(6))+" kcal/100g · "+tag+" · "+dq);b.setGravity("ar".equals(lang)?Gravity.RIGHT:Gravity.LEFT);b.setOnClickListener(v->logFoodDialog(fid,nm));b.setOnLongClickListener(v->{foodReviewDialog(fid);return true;});results.addView(b,new LinearLayout.LayoutParams(-1,dp(51)));results.addView(gap(5));count++;}c.close();if(count==0)results.addView(small(t("No result. Add it using the package label.","مفيش نتيجة. أضفه من الملصق الغذائي.")));};
        search.setOnClickListener(v->load.run());findRemote.setOnClickListener(v->{String code=barcode.getText().toString().trim();if(code.length()<7){toast(t("Enter a valid barcode.","اكتب باركود صحيح."));return;}lookupBarcode(code,results);});scan.setOnClickListener(v->{new IntentIntegrator(this).setDesiredBarcodeFormats(IntentIntegrator.ONE_D_CODE_TYPES).setPrompt(t("Scan the product barcode","امسح باركود المنتج")).setBeepEnabled(true).setOrientationLocked(false).initiateScan();});load.run();ScrollView sc=new ScrollView(this);sc.addView(box);new AlertDialog.Builder(this).setTitle(t("Food library","مكتبة الأكل")).setView(sc).setNegativeButton(t("Close","إغلاق"),null).show();}

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==REQUEST_EXPORT_BACKUP&&resultCode==RESULT_OK&&data!=null&&data.getData()!=null){
            try{String json=BackupManager.exportJson(db);try(OutputStream out=getContentResolver().openOutputStream(data.getData())){if(out==null)throw new java.io.IOException("No output stream");out.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));out.flush();}toast(t("Backup exported.","النسخة الاحتياطية اتحفظت."));}
            catch(Exception e){toast(t("Backup export failed.","فشل تصدير النسخة الاحتياطية."));}
            return;
        }
        if(requestCode==REQUEST_IMPORT_BACKUP&&resultCode==RESULT_OK&&data!=null&&data.getData()!=null){
            Uri uri=data.getData();
            new AlertDialog.Builder(this).setTitle(t("Restore this backup?","تستعيد النسخة الاحتياطية؟"))
                    .setMessage(t("This will replace the app's current local data. Export a backup first if you want to keep it.","ده هيستبدل البيانات الحالية داخل التطبيق. صدّر نسخة احتياطية الأول لو محتاج تحتفظ بيها."))
                    .setPositiveButton(t("Restore","استعادة"),(d,w)->restoreBackup(uri))
                    .setNegativeButton(t("Cancel","إلغاء"),null).show();
            return;
        }
        IntentResult r=IntentIntegrator.parseActivityResult(requestCode,resultCode,data);
        if(r!=null&&r.getContents()!=null){if(barcodeField!=null)barcodeField.setText(r.getContents());if(barcodeResults!=null)lookupBarcode(r.getContents(),barcodeResults);}
    }

    private void exportBackup(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"silent-coach-backup.json");try{startActivityForResult(i,REQUEST_EXPORT_BACKUP);}catch(Exception e){toast(t("Could not open the file picker.","مش قادر أفتح حفظ الملفات."));}}
    private boolean notificationsAllowed(){return android.os.Build.VERSION.SDK_INT<33||checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==android.content.pm.PackageManager.PERMISSION_GRANTED;}
    private void requestNotificationPermission(){if(android.os.Build.VERSION.SDK_INT>=33)requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},9001);else toast(t("Notifications are available.","الإشعارات متاحة."));}
    private int parseInt(String s,int d){try{return Integer.parseInt(s);}catch(Exception e){return d;}}
    private void dailyReminderDialog(){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);int h=parseInt(db.getSetting("daily_reminder_hour","20"),20),m=parseInt(db.getSetting("daily_reminder_minute","0"),0);EditText time=input(t("Time HH:MM","الوقت HH:MM"),false);time.setText(String.format(Locale.US,"%02d:%02d",h,m));EditText msg=input(t("Reminder text","نص التذكير"),false);msg.setText(db.getSetting("daily_reminder_text",t("Time to check in with your plan.","وقت تراجع خطتك النهارده.")));b.addView(time);b.addView(gap(8));b.addView(msg);new AlertDialog.Builder(this).setTitle(t("Daily reminder","التذكير اليومي")).setView(b).setPositiveButton(t("Save","حفظ"),(d,w)->{if(!notificationsAllowed()){requestNotificationPermission();return;}String[] x=time.getText().toString().trim().split(":");int hh=parseInt(x.length>0?x[0]:"20",20),mm=parseInt(x.length>1?x[1]:"0",0);hh=Math.max(0,Math.min(23,hh));mm=Math.max(0,Math.min(59,mm));String text=msg.getText().toString().trim();if(text.isEmpty())text=t("Time to check in with your plan.","وقت تراجع خطتك النهارده.");db.setSetting("daily_reminder_enabled","1");db.setSetting("daily_reminder_hour",String.valueOf(hh));db.setSetting("daily_reminder_minute",String.valueOf(mm));db.setSetting("daily_reminder_text",text);ReminderScheduler.configureDaily(this,true,hh,mm);profile();}).setNegativeButton(t("Disable","إيقاف"),(d,w)->{db.setSetting("daily_reminder_enabled","0");ReminderScheduler.configureDaily(this,false,20,0);profile();}).show();}
    private String t(String s){return s;}
    @Override protected void onDestroy(){if(restTimer!=null)restTimer.cancel();if(db!=null)db.close();super.onDestroy();}

    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
