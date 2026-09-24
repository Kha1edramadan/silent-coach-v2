package com.silentcoach.v2.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/** Offline-first database. Completed logs are snapshot based: later edits to programs/foods
 * never rewrite past sessions or meal items. */
public class CoachDb extends SQLiteOpenHelper {
    public static final int VERSION = 6;
    private static final String DB = "silent_coach_v2.db";

    public CoachDb(Context c) { super(c, DB, null, VERSION); }

    @Override public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        createSchema(db);
        seed(db);
    }

    private void createSchema(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE profile(id INTEGER PRIMARY KEY, age INTEGER, height_cm REAL, weight_kg REAL, sex TEXT, goal TEXT, activity TEXT, language TEXT DEFAULT 'en', units TEXT DEFAULT 'metric', calorie_target REAL, protein_target REAL, carb_target REAL, fat_target REAL, created_at INTEGER, updated_at INTEGER)");
        db.execSQL("CREATE TABLE program(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, goal TEXT, days_per_week INTEGER DEFAULT 3, active INTEGER DEFAULT 0, created_at INTEGER, updated_at INTEGER)");
        db.execSQL("CREATE TABLE program_day(id INTEGER PRIMARY KEY AUTOINCREMENT, program_id INTEGER NOT NULL, title TEXT NOT NULL, day_order INTEGER NOT NULL DEFAULT 1, FOREIGN KEY(program_id) REFERENCES program(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE exercise(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, arabic_name TEXT, primary_muscle TEXT, secondary_muscle TEXT, equipment TEXT, movement TEXT, tracking TEXT NOT NULL DEFAULT 'WEIGHT_REPS', difficulty TEXT, unilateral INTEGER DEFAULT 0, instructions TEXT, custom INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE program_exercise(id INTEGER PRIMARY KEY AUTOINCREMENT, day_id INTEGER NOT NULL, exercise_id INTEGER NOT NULL, ord INTEGER NOT NULL, target_sets INTEGER NOT NULL DEFAULT 3, rep_min INTEGER DEFAULT 8, rep_max INTEGER DEFAULT 12, target_weight REAL, target_rpe REAL, target_rir REAL, rest_sec INTEGER DEFAULT 90, note TEXT, FOREIGN KEY(day_id) REFERENCES program_day(id) ON DELETE CASCADE, FOREIGN KEY(exercise_id) REFERENCES exercise(id))");
        db.execSQL("CREATE TABLE workout_session(id INTEGER PRIMARY KEY AUTOINCREMENT, program_id INTEGER, day_id INTEGER, name TEXT, started_at INTEGER NOT NULL, finished_at INTEGER, duration_sec INTEGER, status TEXT NOT NULL DEFAULT 'active', notes TEXT, FOREIGN KEY(program_id) REFERENCES program(id))");
        db.execSQL("CREATE TABLE workout_exercise(id INTEGER PRIMARY KEY AUTOINCREMENT, session_id INTEGER NOT NULL, exercise_id INTEGER NOT NULL, ord INTEGER NOT NULL, skipped INTEGER DEFAULT 0, FOREIGN KEY(session_id) REFERENCES workout_session(id) ON DELETE CASCADE, FOREIGN KEY(exercise_id) REFERENCES exercise(id))");
        db.execSQL("CREATE TABLE workout_set(id INTEGER PRIMARY KEY AUTOINCREMENT, workout_exercise_id INTEGER NOT NULL, set_order INTEGER NOT NULL, set_type TEXT NOT NULL DEFAULT 'working', weight_kg REAL, reps INTEGER, duration_sec INTEGER, rpe REAL, rir REAL, completed INTEGER DEFAULT 0, completed_at INTEGER, notes TEXT, FOREIGN KEY(workout_exercise_id) REFERENCES workout_exercise(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE food_source(id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT NOT NULL, name TEXT NOT NULL, version TEXT, url TEXT, verified_at INTEGER)");
        db.execSQL("CREATE TABLE food(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, arabic_name TEXT, category TEXT, state TEXT, brand TEXT, barcode TEXT, verification TEXT NOT NULL DEFAULT 'unverified', source_id INTEGER, last_verified INTEGER, revision INTEGER NOT NULL DEFAULT 1, serving_g REAL, notes TEXT, FOREIGN KEY(source_id) REFERENCES food_source(id))");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_food_barcode ON food(barcode) WHERE barcode IS NOT NULL AND barcode <> ''");
        db.execSQL("CREATE TABLE food_nutrient(food_id INTEGER NOT NULL, revision INTEGER NOT NULL, basis TEXT NOT NULL DEFAULT 'per_100g', kcal REAL, protein REAL, carbs REAL, fat REAL, fiber REAL, sugar REAL, sodium_mg REAL, current INTEGER DEFAULT 1, source_id INTEGER, FOREIGN KEY(food_id) REFERENCES food(id) ON DELETE CASCADE, FOREIGN KEY(source_id) REFERENCES food_source(id), PRIMARY KEY(food_id, revision, basis))");
        db.execSQL("CREATE TABLE meal(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, meal_time TEXT, created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE meal_item(id INTEGER PRIMARY KEY AUTOINCREMENT, meal_id INTEGER NOT NULL, food_id INTEGER NOT NULL, food_revision INTEGER NOT NULL, grams REAL NOT NULL, kcal REAL NOT NULL, protein REAL NOT NULL, carbs REAL NOT NULL, fat REAL NOT NULL, fiber REAL NOT NULL, created_at INTEGER NOT NULL, FOREIGN KEY(meal_id) REFERENCES meal(id) ON DELETE CASCADE, FOREIGN KEY(food_id) REFERENCES food(id))");
        db.execSQL("CREATE TABLE body_measurement(id INTEGER PRIMARY KEY AUTOINCREMENT, weight_kg REAL, waist_cm REAL, chest_cm REAL, arm_cm REAL, thigh_cm REAL, note TEXT, measured_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE settings(key TEXT PRIMARY KEY, value TEXT)");
        db.execSQL("CREATE TABLE quote(id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT NOT NULL, author TEXT NOT NULL, work TEXT NOT NULL, text_en TEXT NOT NULL, text_ar TEXT NOT NULL, category TEXT, source_url TEXT, verification TEXT NOT NULL DEFAULT 'curated', shown_count INTEGER NOT NULL DEFAULT 0, last_shown_at INTEGER, created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX idx_quote_rotation ON quote(shown_count,last_shown_at)");
        db.execSQL("CREATE TABLE saved_meal(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE saved_meal_item(saved_meal_id INTEGER NOT NULL, food_id INTEGER NOT NULL, grams REAL NOT NULL, FOREIGN KEY(saved_meal_id) REFERENCES saved_meal(id) ON DELETE CASCADE, FOREIGN KEY(food_id) REFERENCES food(id))");
        db.execSQL("CREATE TABLE favorite_exercise(exercise_id INTEGER PRIMARY KEY, created_at INTEGER NOT NULL, FOREIGN KEY(exercise_id) REFERENCES exercise(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE reminder(id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, message TEXT NOT NULL, hour INTEGER NOT NULL, minute INTEGER NOT NULL, enabled INTEGER NOT NULL DEFAULT 1, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_reminder_enabled_time ON reminder(enabled,hour,minute)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_program_day_program ON program_day(program_id, day_order)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_workout_session_started ON workout_session(started_at DESC)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_food_name ON food(name, arabic_name)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_meal_item_created ON meal_item(created_at DESC)");
    }

    private void seed(SQLiteDatabase db) {
        long now = System.currentTimeMillis();
        insert(db, "profile", cv("id",1,"age",22,"height_cm",175.0,"weight_kg",60.0,"sex","male","goal","gain","activity","moderate","language","en","units","metric","calorie_target",2500.0,"protein_target",96.0,"carb_target",320.0,"fat_target",75.0,"created_at",now,"updated_at",now));
        long sourceEgypt = insert(db,"food_source",cv("type","food_table","name","Food Composition Tables for Egypt / NNI","version","2nd edition","url","https://www.fao.org/infoods/infoods/tables-and-databases/egypt/en/","verified_at",now));
        long sourceFdc = insert(db,"food_source",cv("type","database","name","USDA FoodData Central","version","reference","url","https://fdc.nal.usda.gov/","verified_at",now));
        long sourceOff = insert(db,"food_source",cv("type","barcode","name","Open Food Facts","version","live","url","https://world.openfoodfacts.org/","verified_at",now));

        String[][] ex = {
            {"Barbell Bench Press","ضغط بنش بالبار","Chest","Triceps, Front delts","Barbell","Horizontal push","WEIGHT_REPS","intermediate","0","Bench with controlled range. Keep shoulder blades stable."},
            {"Incline Dumbbell Press","ضغط دمبل مائل","Chest","Triceps, Front delts","Dumbbells","Incline push","WEIGHT_REPS","beginner","0","Press up with a stable ribcage and controlled descent."},
            {"Cable Fly","فلاي كيبل","Chest","","Cable","Horizontal adduction","WEIGHT_REPS","beginner","0","Control the stretch and bring the hands together without swinging."},
            {"Lat Pulldown","سحب علوي","Back","Biceps","Cable","Vertical pull","WEIGHT_REPS","beginner","0","Pull toward the upper chest and avoid excessive torso swing."},
            {"Seated Cable Row","سحب كيبل جالس","Back","Biceps","Cable","Horizontal pull","WEIGHT_REPS","beginner","0","Keep torso stable and pull toward lower ribs."},
            {"Barbell Squat","سكوات بالبار","Quads","Glutes, Adductors","Barbell","Squat","WEIGHT_REPS","intermediate","0","Brace, descend under control, and keep a stable foot position."},
            {"Leg Press","ضغط رجلين","Quads","Glutes","Machine","Squat","WEIGHT_REPS","beginner","0","Use a comfortable depth and controlled lockout."},
            {"Romanian Deadlift","RDL","Hamstrings","Glutes, Back","Barbell","Hip hinge","WEIGHT_REPS","intermediate","0","Hinge from the hips and keep the load close to the legs."},
            {"Lateral Raise","رفرفة جانبي","Side delts","Traps","Dumbbells","Shoulder abduction","WEIGHT_REPS","beginner","1","Use controlled reps and avoid momentum."},
            {"Dumbbell Shoulder Press","ضغط كتف دمبل","Shoulders","Triceps","Dumbbells","Vertical push","WEIGHT_REPS","beginner","0","Press overhead with controlled descent."},
            {"EZ Bar Curl","بايسبس EZ","Biceps","Forearms","EZ Bar","Elbow flexion","WEIGHT_REPS","beginner","0","Keep elbows stable and control the lowering phase."},
            {"Cable Triceps Pushdown","تراي كيبل","Triceps","","Cable","Elbow extension","WEIGHT_REPS","beginner","0","Keep elbows near the ribs and control the return."},
            {"Pull-Up","عقلة","Back","Biceps","Pull-up Bar","Vertical pull","REPS","intermediate","0","Move through a controlled range without kipping."},
            {"Dumbbell Row","سحب دمبل","Back","Biceps","Dumbbell","Horizontal pull","WEIGHT_REPS","beginner","0","Brace and pull the elbow toward the hip."},
            {"Leg Curl","Leg Curl","Hamstrings","","Machine","Knee flexion","WEIGHT_REPS","beginner","0","Keep hips stable and use a controlled range."},
            {"Calf Raise","رفع سمانة","Calves","","Machine","Plantar flexion","WEIGHT_REPS","beginner","0","Pause briefly at the top and stretch under control."},
            {"Plank","بلانك","Core","","Bodyweight","Isometric","TIMED","beginner","0","Keep ribs down and body in a straight line."},
            {"Cable Crunch","كرنش كيبل","Abs","","Cable","Spinal flexion","WEIGHT_REPS","beginner","0","Move through the trunk rather than pulling with the arms."}
        };
        List<Long> eids=new ArrayList<>();
        for(String[] e:ex){eids.add(insert(db,"exercise",cv("name",e[0],"arabic_name",e[1],"primary_muscle",e[2],"secondary_muscle",e[3],"equipment",e[4],"movement",e[5],"tracking",e[6],"difficulty",e[7],"unilateral",Integer.parseInt(e[8]),"instructions",e[9],"custom",0)));}

        long pid=insert(db,"program",cv("name","My Program","goal","gain","days_per_week",3,"active",1,"created_at",now,"updated_at",now));
        long push=addDay(db,pid,"Push",1), pull=addDay(db,pid,"Pull",2), legs=addDay(db,pid,"Legs",3);
        addPE(db,push,eids.get(0),1,3,6,10,120); addPE(db,push,eids.get(1),2,3,8,12,90); addPE(db,push,eids.get(2),3,3,10,15,75); addPE(db,push,eids.get(8),4,3,12,20,60); addPE(db,push,eids.get(11),5,3,8,15,60);
        addPE(db,pull,eids.get(3),1,3,8,12,90); addPE(db,pull,eids.get(4),2,3,8,12,90); addPE(db,pull,eids.get(10),3,3,8,12,75); addPE(db,pull,eids.get(13),4,3,8,12,90);
        addPE(db,legs,eids.get(5),1,3,6,10,120); addPE(db,legs,eids.get(6),2,3,10,15,90); addPE(db,legs,eids.get(7),3,3,8,12,120); addPE(db,legs,eids.get(15),4,4,8,15,60);

        // Reference foods. These are intentionally marked "reference" or "starter" unless they come from a traceable product label.
        Object[][] foods={
            {"Egg, whole","بيض كامل","Protein foods","raw","","","reference",143.0,12.56,0.72,9.51,0.0,1.5,142.0,sourceFdc,"Generic reference; verify preparation/brand."},
            {"Chicken breast","صدر دجاج","Protein foods","cooked","","","reference",165.0,31.0,0.0,3.6,0.0,0.0,74.0,sourceFdc,"Cooked reference; actual values vary by cooking method."},
            {"Beef, lean","لحم بقري قليل الدهن","Protein foods","cooked","","","reference",217.0,26.1,0.0,12.0,0.0,0.0,72.0,sourceFdc,"Generic cooked reference."},
            {"White rice","أرز أبيض","Grains","cooked","","","reference",130.0,2.38,28.17,0.28,0.4,0.05,1.0,sourceFdc,"Generic cooked reference."},
            {"Baladi bread","عيش بلدي","Bakery","as_served","","","starter",250.0,8.5,49.0,2.5,4.0,1.5,430.0,sourceEgypt,"Recipe and bakery variation is substantial; verify local product when possible."},
            {"Fava beans","فول مدمس","Legumes","cooked","","","reference",110.0,7.6,19.7,0.4,5.4,1.8,4.0,sourceFdc,"Plain cooked beans; oil/toppings must be logged separately."},
            {"Falafel","طعمية","Prepared","fried","","","starter",333.0,13.3,31.8,17.8,4.9,2.3,294.0,sourceEgypt,"Recipe-dependent; oil absorption changes energy density."},
            {"Lentils","عدس","Legumes","cooked","","","reference",116.0,9.0,20.1,0.38,7.9,1.8,2.0,sourceFdc,"Generic cooked reference."},
            {"Potato","بطاطس","Vegetables","raw","","","reference",77.0,2.02,17.49,0.09,2.1,0.82,6.0,sourceFdc,"Raw reference."},
            {"Sweet potato","بطاطا","Vegetables","raw","","","reference",86.0,1.57,20.12,0.05,3.0,4.18,55.0,sourceFdc,"Raw reference."},
            {"Tomato","طماطم","Vegetables","raw","","","reference",18.0,0.88,3.89,0.2,1.2,2.63,5.0,sourceFdc,"Generic raw reference."},
            {"Cucumber","خيار","Vegetables","raw","","","reference",15.0,0.65,3.63,0.11,0.5,1.67,2.0,sourceFdc,"Generic raw reference."},
            {"Carrot","جزر","Vegetables","raw","","","reference",41.0,0.93,9.58,0.24,2.8,4.74,69.0,sourceFdc,"Generic raw reference."},
            {"Spinach","سبانخ","Vegetables","raw","","","reference",23.0,2.86,3.63,0.39,2.2,0.42,79.0,sourceFdc,"Generic raw reference."},
            {"Banana","موز","Fruit","raw","","","reference",89.0,1.09,22.84,0.33,2.6,12.23,1.0,sourceFdc,"Generic raw reference."},
            {"Apple","تفاح","Fruit","raw","","","reference",52.0,0.26,13.81,0.17,2.4,10.39,1.0,sourceFdc,"Generic raw reference."},
            {"Orange","برتقال","Fruit","raw","","","reference",47.0,0.94,11.75,0.12,2.4,9.35,0.0,sourceFdc,"Generic raw reference."},
            {"Dates","بلح","Fruit","as_served","","","reference",282.0,2.45,75.03,0.39,8.0,63.35,2.0,sourceFdc,"Generic dried/date reference; variety matters."},
            {"Olive oil","زيت زيتون","Fats","as_served","","","reference",884.0,0.0,0.0,100.0,0.0,0.0,2.0,sourceFdc,"Pure fat reference; measure by grams."},
            {"Peanut butter","زبدة فول سوداني","Fats","as_served","","","reference",588.0,25.1,20.0,50.0,6.0,9.2,486.0,sourceFdc,"Brand recipes vary; verify the exact jar."},
            {"Almonds","لوز","Nuts","raw","","","reference",579.0,21.15,21.55,49.93,12.5,4.35,1.0,sourceFdc,"Generic raw reference."},
            {"Pasta, cooked","مكرونة مطبوخة","Grains","cooked","","","reference",158.0,5.8,30.9,0.93,1.8,0.56,1.0,sourceFdc,"Plain cooked pasta; sauce is separate."},
            {"Oats","شوفان","Grains","dry","","","reference",389.0,16.9,66.3,6.9,10.6,0.99,2.0,sourceFdc,"Dry reference; serving weight should be entered before cooking."},
            {"Milk, whole","لبن كامل الدسم","Dairy","as_served","","","reference",61.0,3.15,4.8,3.25,0.0,5.05,43.0,sourceFdc,"Generic reference; brand and fat level matter."},
            {"Greek yogurt","زبادي يوناني","Dairy","as_served","","","reference",73.0,9.95,3.94,2.0,0.0,3.56,36.0,sourceFdc,"Generic plain reference; verify brand."},
            {"Tuna, canned in water","تونة معلبة بالماء","Fish","drained","","","reference",116.0,25.5,0.0,0.82,0.0,0.0,338.0,sourceFdc,"Generic reference; brand and drained weight matter."},
            {"Chickpeas","حمص مسلوق","Legumes","cooked","","","reference",164.0,8.86,27.42,2.59,7.6,4.8,7.0,sourceFdc,"Generic cooked reference."},
            {"Kidney beans","فاصوليا حمراء","Legumes","cooked","","","reference",127.0,8.67,22.8,0.5,6.4,0.32,2.0,sourceFdc,"Generic cooked reference."},
            {"Green peas","بسلة","Vegetables","cooked","","","reference",84.0,5.36,15.63,0.22,5.5,5.67,317.0,sourceFdc,"Generic cooked reference."},
            {"Onion","بصل","Vegetables","raw","","","reference",40.0,1.1,9.34,0.1,1.7,4.24,4.0,sourceFdc,"Generic raw reference."}
        };
        for(Object[] f:foods){
            Long verifiedAt = "verified".equals(String.valueOf(f[6])) ? now : null;
            long fid=insert(db,"food",cv("name",f[0],"arabic_name",f[1],"category",f[2],"state",f[3],"brand",f[4],"barcode",f[5],"verification",f[6],"source_id",f[15],"last_verified",verifiedAt,"revision",1,"serving_g",100.0,"notes",f[16]));
            insert(db,"food_nutrient",cv("food_id",fid,"revision",1,"basis","per_100g","kcal",f[7],"protein",f[8],"carbs",f[9],"fat",f[10],"fiber",f[11],"sugar",f[12],"sodium_mg",f[13],"current",1,"source_id",f[15]));
        }
        insert(db,"settings",cv("key","lang","value","en"));
        insert(db,"settings",cv("key","onboarding_complete","value","0"));
        insert(db,"body_measurement",cv("weight_kg",60.0,"measured_at",now));
        // Keep sourceOff referenced so migrations can safely use it later.
        insert(db,"settings",cv("key","barcode_source","value",String.valueOf(sourceOff)));
        seedQuotes(db);
        insert(db,"settings",cv("key","quote_notifications_enabled","value","0"));
        insert(db,"settings",cv("key","quote_interval_hours","value","2"));
        insert(db,"settings",cv("key","quote_quiet_enabled","value","1"));
        insert(db,"settings",cv("key","quote_quiet_start","value","23"));
        insert(db,"settings",cv("key","quote_quiet_end","value","7"));
        insert(db,"settings",cv("key","daily_reminder_enabled","value","0"));
        insert(db,"settings",cv("key","daily_reminder_hour","value","20"));
        insert(db,"settings",cv("key","daily_reminder_minute","value","0"));
        insert(db,"settings",cv("key","daily_reminder_text","value","Time to check in with your plan."));
    }

    private static long addDay(SQLiteDatabase db,long pid,String title,int order){return insert(db,"program_day",cv("program_id",pid,"title",title,"day_order",order));}
    private static long addPE(SQLiteDatabase db,long day,long ex,int ord,int sets,int min,int max,int rest){return insert(db,"program_exercise",cv("day_id",day,"exercise_id",ex,"ord",ord,"target_sets",sets,"rep_min",min,"rep_max",max,"rest_sec",rest));}
    private static long insert(SQLiteDatabase db,String table,ContentValues cv){return db.insertOrThrow(table,null,cv);}

    private static ContentValues cv(Object... xs){ContentValues c=new ContentValues();for(int i=0;i<xs.length;i+=2){String k=(String)xs[i];Object v=xs[i+1];if(v==null)c.putNull(k);else if(v instanceof Integer)c.put(k,(Integer)v);else if(v instanceof Long)c.put(k,(Long)v);else if(v instanceof Double)c.put(k,(Double)v);else if(v instanceof Float)c.put(k,(Float)v);else c.put(k,String.valueOf(v));}return c;}

    @Override public void onUpgrade(SQLiteDatabase db,int oldV,int newV){
        // Original V2 builds shipped with VERSION 1. To avoid destroying existing user logs,
        // rebuild only missing indexes/columns safely where possible, and preserve old tables.
        if(oldV<2){safeAdd(db,"program","days_per_week INTEGER DEFAULT 3"); safeAdd(db,"profile","created_at INTEGER");}
        if(oldV<3){safeAdd(db,"food","serving_g REAL"); safeAdd(db,"food","notes TEXT"); safeAdd(db,"food_nutrient","sugar REAL"); safeAdd(db,"food_nutrient","sodium_mg REAL"); safeAdd(db,"workout_session","duration_sec INTEGER");}
        if(oldV<4){safeAdd(db,"profile","created_at INTEGER"); safeAdd(db,"meal_item","created_at INTEGER");}
        if(oldV<5){db.execSQL("CREATE TABLE IF NOT EXISTS quote(id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT NOT NULL, author TEXT NOT NULL, work TEXT NOT NULL, text_en TEXT NOT NULL, text_ar TEXT NOT NULL, category TEXT, source_url TEXT, verification TEXT NOT NULL DEFAULT 'curated', shown_count INTEGER NOT NULL DEFAULT 0, last_shown_at INTEGER, created_at INTEGER NOT NULL)");db.execSQL("CREATE INDEX IF NOT EXISTS idx_quote_rotation ON quote(shown_count,last_shown_at)");if(QuoteCount(db)==0)seedQuotes(db);putDefault(db,"quote_notifications_enabled","0");putDefault(db,"quote_interval_hours","2");putDefault(db,"quote_quiet_enabled","1");putDefault(db,"quote_quiet_start","23");putDefault(db,"quote_quiet_end","7");putDefault(db,"daily_reminder_enabled","0");putDefault(db,"daily_reminder_hour","20");putDefault(db,"daily_reminder_minute","0");putDefault(db,"daily_reminder_text","Time to check in with your plan.");}
        if(oldV<6){db.execSQL("CREATE TABLE IF NOT EXISTS reminder(id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, message TEXT NOT NULL, hour INTEGER NOT NULL, minute INTEGER NOT NULL, enabled INTEGER NOT NULL DEFAULT 1, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)");}
        db.execSQL("CREATE TABLE IF NOT EXISTS saved_meal(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS saved_meal_item(saved_meal_id INTEGER NOT NULL, food_id INTEGER NOT NULL, grams REAL NOT NULL, FOREIGN KEY(saved_meal_id) REFERENCES saved_meal(id) ON DELETE CASCADE, FOREIGN KEY(food_id) REFERENCES food(id))");
        db.execSQL("CREATE TABLE IF NOT EXISTS favorite_exercise(exercise_id INTEGER PRIMARY KEY, created_at INTEGER NOT NULL, FOREIGN KEY(exercise_id) REFERENCES exercise(id) ON DELETE CASCADE)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_food_barcode ON food(barcode) WHERE barcode IS NOT NULL AND barcode <> ''");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_program_day_program ON program_day(program_id, day_order)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_workout_session_started ON workout_session(started_at DESC)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_food_name ON food(name, arabic_name)");
    }
    private void safeAdd(SQLiteDatabase db,String table,String definition){try{db.execSQL("ALTER TABLE "+table+" ADD COLUMN "+definition);}catch(Exception ignored){}}

    public String getSetting(String key,String def){Cursor c=getReadableDatabase().rawQuery("SELECT value FROM settings WHERE key=?",new String[]{key});try{return c.moveToFirst()?c.getString(0):def;}finally{c.close();}}
    public void setSetting(String key,String value){getWritableDatabase().insertWithOnConflict("settings",null,cv("key",key,"value",value),SQLiteDatabase.CONFLICT_REPLACE);}
    public Cursor profile(){return getReadableDatabase().rawQuery("SELECT * FROM profile WHERE id=1",null);}
    private void requireNonEmpty(String v,String field){if(v==null||v.trim().isEmpty())throw new IllegalArgumentException(field+" is required");}
    private void requireFiniteNonNegative(double v,String field){if(!Double.isFinite(v)||v<0)throw new IllegalArgumentException(field+" must be finite and >= 0");}
    private void requirePositiveFinite(double v,String field){if(!Double.isFinite(v)||v<=0)throw new IllegalArgumentException(field+" must be > 0");}
    public Cursor activeProgram(){return getReadableDatabase().rawQuery("SELECT id,name,goal,days_per_week FROM program WHERE active=1 LIMIT 1",null);}
    public Cursor programs(){return getReadableDatabase().rawQuery("SELECT id,name,goal,days_per_week,active,created_at FROM program ORDER BY active DESC, created_at DESC",null);}
    public long createProgram(String name,String goal,int days){SQLiteDatabase db=getWritableDatabase();ContentValues v=cv("name",name,"goal",goal,"days_per_week",days,"active",0,"created_at",System.currentTimeMillis(),"updated_at",System.currentTimeMillis());long pid=db.insert("program",null,v);for(int i=1;i<=days;i++)insert(db,"program_day",cv("program_id",pid,"title","Day "+i,"day_order",i));return pid;}
    public void renameProgram(long pid,String name){requireNonEmpty(name,"name");getWritableDatabase().update("program",cv("name",name,"updated_at",System.currentTimeMillis()),"id=?",new String[]{String.valueOf(pid)});}
    public void deleteProgram(long pid){getWritableDatabase().delete("program","id=?",new String[]{String.valueOf(pid)});Cursor c=programs();boolean any=c.moveToFirst();c.close();if(!any){long id=createProgram("My Program","custom",3);setActiveProgram(id);}}
    public void setActiveProgram(long pid){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{db.update("program",cv("active",0,"updated_at",System.currentTimeMillis()),null,null);db.update("program",cv("active",1,"updated_at",System.currentTimeMillis()),"id=?",new String[]{String.valueOf(pid)});db.setTransactionSuccessful();}finally{db.endTransaction();}}
    public Cursor activeDays(){return getReadableDatabase().rawQuery("SELECT id,title,day_order FROM program_day WHERE program_id=(SELECT id FROM program WHERE active=1 LIMIT 1) ORDER BY day_order",null);}
    public Cursor daysForProgram(long pid){return getReadableDatabase().rawQuery("SELECT id,title,day_order FROM program_day WHERE program_id=? ORDER BY day_order",new String[]{String.valueOf(pid)});}
    public void moveDay(long dayId,int direction){SQLiteDatabase db=getWritableDatabase();Cursor c=db.rawQuery("SELECT program_id,day_order FROM program_day WHERE id=?",new String[]{String.valueOf(dayId)});if(!c.moveToFirst()){c.close();return;}long pid=c.getLong(0);int ord=c.getInt(1);c.close();int target=ord+direction;if(target<1)return;Cursor x=db.rawQuery("SELECT id FROM program_day WHERE program_id=? AND day_order=?",new String[]{String.valueOf(pid),String.valueOf(target)});if(x.moveToFirst()){long other=x.getLong(0);db.update("program_day",cv("day_order",-99),"id=?",new String[]{String.valueOf(other)});db.update("program_day",cv("day_order",target),"id=?",new String[]{String.valueOf(dayId)});db.update("program_day",cv("day_order",ord),"id=?",new String[]{String.valueOf(other)});}x.close();}
    public void renameDay(long dayId,String title){getWritableDatabase().update("program_day",cv("title",title),"id=?",new String[]{String.valueOf(dayId)});}
    public long addDayToProgram(long programId,String title){requireNonEmpty(title,"title");SQLiteDatabase db=getWritableDatabase();Cursor c=db.rawQuery("SELECT COALESCE(MAX(day_order),0)+1 FROM program_day WHERE program_id=?",new String[]{String.valueOf(programId)});int order=1;if(c.moveToFirst())order=c.getInt(0);c.close();return db.insert("program_day",null,cv("program_id",programId,"title",title,"day_order",order));}
    public boolean deleteDay(long dayId){SQLiteDatabase db=getWritableDatabase();Cursor c=db.rawQuery("SELECT program_id FROM program_day WHERE id=?",new String[]{String.valueOf(dayId)});if(!c.moveToFirst()){c.close();return false;}long pid=c.getLong(0);c.close();Cursor n=db.rawQuery("SELECT COUNT(*) FROM program_day WHERE program_id=?",new String[]{String.valueOf(pid)});int count=n.moveToFirst()?n.getInt(0):0;n.close();if(count<=1)return false;db.delete("program_day","id=?",new String[]{String.valueOf(dayId)});Cursor days=db.rawQuery("SELECT id FROM program_day WHERE program_id=? ORDER BY day_order",new String[]{String.valueOf(pid)});int i=1;while(days.moveToNext())db.update("program_day",cv("day_order",i++),"id=?",new String[]{String.valueOf(days.getLong(0))});days.close();return true;}
    public Cursor programExercises(long dayId){return getReadableDatabase().rawQuery("SELECT pe.id,e.id,e.name,e.arabic_name,pe.target_sets,pe.rep_min,pe.rep_max,pe.rest_sec,e.primary_muscle,e.equipment,pe.note FROM program_exercise pe JOIN exercise e ON e.id=pe.exercise_id WHERE pe.day_id=? ORDER BY pe.ord",new String[]{String.valueOf(dayId)});}
    public void addProgramExercise(long dayId,long exId){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(MAX(ord),0)+1 FROM program_exercise WHERE day_id=?",new String[]{String.valueOf(dayId)});int ord=1;if(c.moveToFirst())ord=c.getInt(0);c.close();getWritableDatabase().insert("program_exercise",null,cv("day_id",dayId,"exercise_id",exId,"ord",ord,"target_sets",3,"rep_min",8,"rep_max",12,"rest_sec",90));}
    public void updateProgramExercise(long id,int sets,int min,int max,int rest,double rpe,double rir,String note){if(sets<1||sets>50||min<1||max<min||max>100||rest<0||rest>3600||rpe<0||rpe>10||rir<0||rir>10)throw new IllegalArgumentException("Invalid workout prescription");getWritableDatabase().update("program_exercise",cv("target_sets",sets,"rep_min",min,"rep_max",max,"rest_sec",rest,"target_rpe",rpe>0?rpe:null,"target_rir",rir>0?rir:null,"note",note),"id=?",new String[]{String.valueOf(id)});}
    public void deleteProgramExercise(long peId){getWritableDatabase().delete("program_exercise","id=?",new String[]{String.valueOf(peId)});}
    public void replaceProgramExercise(long programExerciseId,long exerciseId){getWritableDatabase().update("program_exercise",cv("exercise_id",exerciseId),"id=?",new String[]{String.valueOf(programExerciseId)});}
    public void moveProgramExercise(long peId,int direction){SQLiteDatabase db=getWritableDatabase();Cursor c=db.rawQuery("SELECT day_id,ord FROM program_exercise WHERE id=?",new String[]{String.valueOf(peId)});if(!c.moveToFirst()){c.close();return;}long day=c.getLong(0);int ord=c.getInt(1);c.close();int target=ord+direction;Cursor s=db.rawQuery("SELECT id,ord FROM program_exercise WHERE day_id=? AND ord=?",new String[]{String.valueOf(day),String.valueOf(target)});if(s.moveToFirst()){long other=s.getLong(0);db.update("program_exercise",cv("ord",-99),"id=?",new String[]{String.valueOf(other)});db.update("program_exercise",cv("ord",target),"id=?",new String[]{String.valueOf(peId)});db.update("program_exercise",cv("ord",ord),"id=?",new String[]{String.valueOf(other)});}s.close();}

    public Cursor exercises(String q){String s=q==null?"":q.trim();String like="%"+s+"%";return getReadableDatabase().rawQuery("SELECT id,name,arabic_name,primary_muscle,equipment,tracking,difficulty,unilateral,custom,instructions FROM exercise WHERE name LIKE ? COLLATE NOCASE OR arabic_name LIKE ? OR primary_muscle LIKE ? COLLATE NOCASE OR equipment LIKE ? COLLATE NOCASE ORDER BY custom, name",new String[]{like,like,like,like});}
    public Cursor exercise(long id){return getReadableDatabase().rawQuery("SELECT * FROM exercise WHERE id=?",new String[]{String.valueOf(id)});}
    public void addCustomExercise(String en,String ar,String muscle,String equipment,String tracking,boolean unilateral,String notes){getWritableDatabase().insert("exercise",null,cv("name",en,"arabic_name",ar,"primary_muscle",muscle,"equipment",equipment,"tracking",tracking,"difficulty","custom","unilateral",unilateral?1:0,"instructions",notes,"custom",1));}
    public void toggleFavorite(long exerciseId){SQLiteDatabase db=getWritableDatabase();Cursor c=db.rawQuery("SELECT 1 FROM favorite_exercise WHERE exercise_id=?",new String[]{String.valueOf(exerciseId)});boolean exists=c.moveToFirst();c.close();if(exists)db.delete("favorite_exercise","exercise_id=?",new String[]{String.valueOf(exerciseId)});else db.insert("favorite_exercise",null,cv("exercise_id",exerciseId,"created_at",System.currentTimeMillis()));}
    public boolean isFavorite(long id){Cursor c=getReadableDatabase().rawQuery("SELECT 1 FROM favorite_exercise WHERE exercise_id=?",new String[]{String.valueOf(id)});try{return c.moveToFirst();}finally{c.close();}}

    public long startSession(long dayId,String name){Cursor active=activeUnfinishedSession();if(active.moveToFirst()){long existing=active.getLong(0);active.close();return existing;}active.close();SQLiteDatabase db=getWritableDatabase();Cursor c=db.rawQuery("SELECT program_id FROM program_day WHERE id=?",new String[]{String.valueOf(dayId)});long pid=0;if(c.moveToFirst())pid=c.getLong(0);c.close();long sid=db.insert("workout_session",null,cv("program_id",pid,"day_id",dayId,"name",name,"started_at",System.currentTimeMillis(),"status","active"));Cursor ex=programExercises(dayId);int ord=1;while(ex.moveToNext()){long eid=ex.getLong(1);long wx=db.insert("workout_exercise",null,cv("session_id",sid,"exercise_id",eid,"ord",ord++));int sets=ex.getInt(4),min=ex.getInt(5);for(int s=1;s<=sets;s++)db.insert("workout_set",null,cv("workout_exercise_id",wx,"set_order",s,"set_type","working","reps",min,"completed",0));}ex.close();return sid;}
    public Cursor activeUnfinishedSession(){return getReadableDatabase().rawQuery("SELECT id,name,started_at FROM workout_session WHERE status='active' ORDER BY started_at DESC LIMIT 1",null);}
    public long sessionStartedAt(long sid){Cursor c=getReadableDatabase().rawQuery("SELECT started_at FROM workout_session WHERE id=?",new String[]{String.valueOf(sid)});try{return c.moveToFirst()?c.getLong(0):System.currentTimeMillis();}finally{c.close();}}
    public Cursor sessionExercises(long sid){return getReadableDatabase().rawQuery("SELECT we.id,e.id,e.name,e.arabic_name,e.primary_muscle,e.equipment,we.ord,we.skipped,e.tracking FROM workout_exercise we JOIN exercise e ON e.id=we.exercise_id WHERE we.session_id=? ORDER BY we.ord",new String[]{String.valueOf(sid)});}
    public int sessionExerciseRest(long sid,long wid){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(pe.rest_sec,90) FROM workout_exercise we JOIN workout_session ws ON ws.id=we.session_id JOIN program_exercise pe ON pe.day_id=ws.day_id AND pe.exercise_id=we.exercise_id WHERE we.id=? AND ws.id=? LIMIT 1",new String[]{String.valueOf(wid),String.valueOf(sid)});try{return c.moveToFirst()?c.getInt(0):90;}finally{c.close();}}
    public Cursor sets(long wid){return getReadableDatabase().rawQuery("SELECT id,set_order,set_type,weight_kg,reps,duration_sec,rpe,rir,completed,completed_at,notes FROM workout_set WHERE workout_exercise_id=? ORDER BY set_order",new String[]{String.valueOf(wid)});}
    public Cursor previousBest(long exerciseId,long beforeTs){return getReadableDatabase().rawQuery("SELECT MAX(weight_kg), MAX(reps), MAX(rpe), MAX(workout_set.id) FROM workout_set JOIN workout_exercise ON workout_exercise.id=workout_set.workout_exercise_id JOIN workout_session ON workout_session.id=workout_exercise.session_id WHERE workout_exercise.exercise_id=? AND workout_session.status='completed' AND workout_session.started_at<? AND workout_set.completed=1",new String[]{String.valueOf(exerciseId),String.valueOf(beforeTs)});}
    public Cursor previousTopSet(long exerciseId,long beforeTs){return getReadableDatabase().rawQuery("SELECT ws.weight_kg,ws.reps,ws.rpe,ws.completed_at FROM workout_set ws JOIN workout_exercise we ON we.id=ws.workout_exercise_id JOIN workout_session s ON s.id=we.session_id WHERE we.exercise_id=? AND s.status='completed' AND s.started_at<? AND ws.completed=1 AND ws.weight_kg IS NOT NULL ORDER BY (ws.weight_kg*COALESCE(ws.reps,0)) DESC, ws.weight_kg DESC, COALESCE(ws.reps,0) DESC LIMIT 1",new String[]{String.valueOf(exerciseId),String.valueOf(beforeTs)});}
    public void updateSet(long id,double weight,Integer reps,Integer duration,double rpe,double rir,boolean done,String notes){ContentValues v=cv("completed",done?1:0,"completed_at",done?System.currentTimeMillis():null,"notes",notes);if(weight>=0)v.put("weight_kg",weight);else v.putNull("weight_kg");if(reps!=null)v.put("reps",reps);if(duration!=null)v.put("duration_sec",duration);else v.putNull("duration_sec");if(rpe>0)v.put("rpe",rpe);else v.putNull("rpe");if(rir>0)v.put("rir",rir);else v.putNull("rir");getWritableDatabase().update("workout_set",v,"id=?",new String[]{String.valueOf(id)});}
    public long addSet(long wid){return addSet(wid,"working");}
    public long addSet(long wid,String setType){requireNonEmpty(setType,"setType");Cursor c=sets(wid);int max=0;while(c.moveToNext())max=Math.max(max,c.getInt(1));c.close();return getWritableDatabase().insert("workout_set",null,cv("workout_exercise_id",wid,"set_order",max+1,"set_type",setType,"completed",0));}
    public void deleteSet(long id){getWritableDatabase().delete("workout_set","id=?",new String[]{String.valueOf(id)});}
    public void setExerciseSkipped(long workoutExerciseId,boolean skipped){getWritableDatabase().update("workout_exercise",cv("skipped",skipped?1:0),"id=?",new String[]{String.valueOf(workoutExerciseId)});}
    public void finishSession(long sid,String notes){Cursor c=getReadableDatabase().rawQuery("SELECT started_at FROM workout_session WHERE id=?",new String[]{String.valueOf(sid)});long started=System.currentTimeMillis();if(c.moveToFirst())started=c.getLong(0);c.close();long now=System.currentTimeMillis();getWritableDatabase().update("workout_session",cv("finished_at",now,"duration_sec",Math.max(0,(now-started)/1000),"status","completed","notes",notes),"id=?",new String[]{String.valueOf(sid)});}
    public void deleteSession(long sid){getWritableDatabase().delete("workout_session","id=?",new String[]{String.valueOf(sid)});}
    public Cursor recentSessions(int limit){return getReadableDatabase().rawQuery("SELECT id,name,started_at,finished_at,duration_sec,status,notes FROM workout_session ORDER BY started_at DESC LIMIT ?",new String[]{String.valueOf(limit)});}
    public double sessionVolume(long sid){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM(weight_kg*reps),0) FROM workout_set ws JOIN workout_exercise we ON we.id=ws.workout_exercise_id WHERE we.session_id=? AND ws.completed=1",new String[]{String.valueOf(sid)});try{return c.moveToFirst()?c.getDouble(0):0;}finally{c.close();}}
    public int completedSets(long sid){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM workout_set ws JOIN workout_exercise we ON we.id=ws.workout_exercise_id WHERE we.session_id=? AND ws.completed=1",new String[]{String.valueOf(sid)});try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}}
    public int completedWorkoutsThisWeek(){long week=System.currentTimeMillis()-7L*24*3600*1000;Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM workout_session WHERE status='completed' AND finished_at>=?",new String[]{String.valueOf(week)});try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}}
    public double volumeSince(long since){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM(weight_kg*reps),0) FROM workout_set ws JOIN workout_exercise we ON we.id=ws.workout_exercise_id JOIN workout_session s ON s.id=we.session_id WHERE s.status='completed' AND s.finished_at>=? AND ws.completed=1",new String[]{String.valueOf(since)});try{return c.moveToFirst()?c.getDouble(0):0;}finally{c.close();}}

    public Cursor foods(String q){String like="%"+(q==null?"":q.trim())+"%";return getReadableDatabase().rawQuery("SELECT f.id,f.name,f.arabic_name,f.state,f.brand,f.barcode,n.kcal,n.protein,n.carbs,n.fat,n.fiber,n.sugar,n.sodium_mg,f.verification,f.notes,f.revision FROM food f JOIN food_nutrient n ON n.food_id=f.id AND n.current=1 WHERE f.name LIKE ? COLLATE NOCASE OR f.arabic_name LIKE ? OR COALESCE(f.brand,'') LIKE ? COLLATE NOCASE OR COALESCE(f.barcode,'') LIKE ? ORDER BY CASE WHEN f.verification='verified' THEN 0 WHEN f.verification='reference' THEN 1 ELSE 2 END, f.name",new String[]{like,like,like,like});}
    public Cursor food(long fid){return getReadableDatabase().rawQuery("SELECT f.*,n.kcal,n.protein,n.carbs,n.fat,n.fiber,n.sugar,n.sodium_mg FROM food f JOIN food_nutrient n ON n.food_id=f.id AND n.current=1 WHERE f.id=?",new String[]{String.valueOf(fid)});}
    public Cursor foodByBarcode(String barcode){return getReadableDatabase().rawQuery("SELECT f.id,f.name,f.arabic_name,f.brand,f.barcode,n.kcal,n.protein,n.carbs,n.fat,n.fiber,n.sugar,n.sodium_mg,f.verification FROM food f JOIN food_nutrient n ON n.food_id=f.id AND n.current=1 WHERE f.barcode=? LIMIT 1",new String[]{barcode});}
    public long sourceIdByType(String type){Cursor c=getReadableDatabase().rawQuery("SELECT id FROM food_source WHERE type=? ORDER BY id DESC LIMIT 1",new String[]{type});try{return c.moveToFirst()?c.getLong(0):-1;}finally{c.close();}}
    public long addCustomFood(String name,String ar,String brand,String category,String state,String barcode,double kcal,double protein,double carbs,double fat,double fiber,double sugar,double sodium,String notes){requireNonEmpty(name,"name");double[] a={kcal,protein,carbs,fat,fiber,sugar,sodium};for(double v:a)requireFiniteNonNegative(v,"nutrition");SQLiteDatabase db=getWritableDatabase();long fid=db.insert("food",null,cv("name",name,"arabic_name",ar,"brand",brand,"category",category,"state",state,"barcode",barcode,"verification","user","revision",1,"serving_g",100.0,"notes",notes));db.insert("food_nutrient",null,cv("food_id",fid,"revision",1,"basis","per_100g","kcal",kcal,"protein",protein,"carbs",carbs,"fat",fat,"fiber",fiber,"sugar",sugar,"sodium_mg",sodium,"current",1));return fid;}
    public long addImportedFood(String name,String ar,String brand,String barcode,double kcal,double protein,double carbs,double fat,double fiber,String notes,long sourceId){requireNonEmpty(name,"name");double[] a={kcal,protein,carbs,fat,fiber};for(double v:a)requireFiniteNonNegative(v,"nutrition");SQLiteDatabase db=getWritableDatabase();long fid=db.insert("food",null,cv("name",name,"arabic_name",ar,"brand",brand,"barcode",barcode,"verification","imported","source_id",sourceId,"revision",1,"serving_g",100.0,"notes",notes));if(fid<0)return -1;db.insert("food_nutrient",null,cv("food_id",fid,"revision",1,"basis","per_100g","kcal",kcal,"protein",protein,"carbs",carbs,"fat",fat,"fiber",fiber,"current",1,"source_id",sourceId));return fid;}
    public long logFood(long foodId,double grams,String mealName){requirePositiveFinite(grams,"grams");requireNonEmpty(mealName,"mealName");SQLiteDatabase db=getWritableDatabase();Cursor c=db.rawQuery("SELECT n.revision,n.kcal,n.protein,n.carbs,n.fat,n.fiber FROM food_nutrient n WHERE n.food_id=? AND n.current=1 LIMIT 1",new String[]{String.valueOf(foodId)});if(!c.moveToFirst()){c.close();return -1;}int rev=c.getInt(0);double k=c.getDouble(1)*grams/100,p=c.getDouble(2)*grams/100,cb=c.getDouble(3)*grams/100,f=c.getDouble(4)*grams/100,fi=c.getDouble(5)*grams/100;c.close();long mid=getOrCreateTodayMeal(db,mealName);return db.insert("meal_item",null,cv("meal_id",mid,"food_id",foodId,"food_revision",rev,"grams",grams,"kcal",k,"protein",p,"carbs",cb,"fat",f,"fiber",fi,"created_at",System.currentTimeMillis()));}
    private long getOrCreateTodayMeal(SQLiteDatabase db,String name){String n=name==null||name.trim().isEmpty()?"Today":name.trim();long dayStart=java.time.ZonedDateTime.now().toLocalDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();long dayEnd=dayStart+24L*3600*1000;Cursor c=db.rawQuery("SELECT id FROM meal WHERE created_at>=? AND created_at<? AND name=? LIMIT 1",new String[]{String.valueOf(dayStart),String.valueOf(dayEnd),n});if(c.moveToFirst()){long id=c.getLong(0);c.close();return id;}c.close();return db.insert("meal",null,cv("name",n,"meal_time",n,"created_at",System.currentTimeMillis()));}
    public double[] todayTotals(){double[] x={0,0,0,0,0};long start=java.time.ZonedDateTime.now().toLocalDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();long end=start+24L*3600*1000;Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM(kcal),0),COALESCE(SUM(protein),0),COALESCE(SUM(carbs),0),COALESCE(SUM(fat),0),COALESCE(SUM(fiber),0) FROM meal_item WHERE created_at>=? AND created_at<?",new String[]{String.valueOf(start),String.valueOf(end)});if(c.moveToFirst())for(int i=0;i<5;i++)x[i]=c.getDouble(i);c.close();return x;}
    public Cursor todayMeals(){long start=java.time.ZonedDateTime.now().toLocalDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();long end=start+24L*3600*1000;return getReadableDatabase().rawQuery("SELECT m.id,m.name,COALESCE(SUM(mi.grams),0),COALESCE(SUM(mi.kcal),0) FROM meal m LEFT JOIN meal_item mi ON mi.meal_id=m.id WHERE m.created_at>=? AND m.created_at<? GROUP BY m.id,m.name ORDER BY m.created_at",new String[]{String.valueOf(start),String.valueOf(end)});}
    public long saveMeal(long mealId,String name){SQLiteDatabase db=getWritableDatabase();long id=db.insert("saved_meal",null,cv("name",name,"created_at",System.currentTimeMillis()));if(id<0)return -1;Cursor c=mealItems(mealId);while(c.moveToNext()){long foodId=getMealFoodId(c.getLong(0));db.insert("saved_meal_item",null,cv("saved_meal_id",id,"food_id",foodId,"grams",c.getDouble(4)));}c.close();return id;}
    private long getMealFoodId(long mealItemId){Cursor c=getReadableDatabase().rawQuery("SELECT food_id FROM meal_item WHERE id=?",new String[]{String.valueOf(mealItemId)});try{return c.moveToFirst()?c.getLong(0):-1;}finally{c.close();}}
    public Cursor savedMeals(){return getReadableDatabase().rawQuery("SELECT id,name,created_at FROM saved_meal ORDER BY created_at DESC",null);}
    public void logSavedMeal(long savedMealId,String mealName){Cursor c=getReadableDatabase().rawQuery("SELECT food_id,grams FROM saved_meal_item WHERE saved_meal_id=? ORDER BY rowid",new String[]{String.valueOf(savedMealId)});try{while(c.moveToNext())logFood(c.getLong(0),c.getDouble(1),mealName);}finally{c.close();}}
    public Cursor mealItems(long mealId){return getReadableDatabase().rawQuery("SELECT mi.id,f.name,f.arabic_name,f.brand,mi.grams,mi.kcal,mi.protein,mi.carbs,mi.fat,mi.fiber FROM meal_item mi JOIN food f ON f.id=mi.food_id WHERE mi.meal_id=? ORDER BY mi.id",new String[]{String.valueOf(mealId)});}
    public void updateMealItem(long id,double grams){requirePositiveFinite(grams,"grams");SQLiteDatabase db=getWritableDatabase();Cursor c=db.rawQuery("SELECT food_id,food_revision FROM meal_item WHERE id=?",new String[]{String.valueOf(id)});if(!c.moveToFirst()){c.close();return;}long foodId=c.getLong(0);int rev=c.getInt(1);c.close();Cursor n=db.rawQuery("SELECT kcal,protein,carbs,fat,fiber FROM food_nutrient WHERE food_id=? AND revision=? AND current=1 LIMIT 1",new String[]{String.valueOf(foodId),String.valueOf(rev)});if(!n.moveToFirst()){n.close();return;}double k=n.getDouble(0)*grams/100,p=n.getDouble(1)*grams/100,cb=n.getDouble(2)*grams/100,f=n.getDouble(3)*grams/100,fi=n.getDouble(4)*grams/100;n.close();db.update("meal_item",cv("grams",grams,"kcal",k,"protein",p,"carbs",cb,"fat",f,"fiber",fi),"id=?",new String[]{String.valueOf(id)});}
    public void deleteMealItem(long id){getWritableDatabase().delete("meal_item","id=?",new String[]{String.valueOf(id)});}

    public void addWeight(double weight,String note){getWritableDatabase().insert("body_measurement",null,cv("weight_kg",weight,"note",note,"measured_at",System.currentTimeMillis()));getWritableDatabase().update("profile",cv("weight_kg",weight,"updated_at",System.currentTimeMillis()),"id=1",null);}
    public void addMeasurements(double weight,double waist,double chest,double arm,double thigh,String note){getWritableDatabase().insert("body_measurement",null,cv("weight_kg",weight>0?weight:null,"waist_cm",waist>0?waist:null,"chest_cm",chest>0?chest:null,"arm_cm",arm>0?arm:null,"thigh_cm",thigh>0?thigh:null,"note",note,"measured_at",System.currentTimeMillis()));if(weight>0)getWritableDatabase().update("profile",cv("weight_kg",weight,"updated_at",System.currentTimeMillis()),"id=1",null);}
    public Cursor weights(){return getReadableDatabase().rawQuery("SELECT id,weight_kg,waist_cm,chest_cm,arm_cm,thigh_cm,measured_at,note FROM body_measurement WHERE weight_kg IS NOT NULL ORDER BY measured_at DESC LIMIT 60",null);}
    public void updateProfile(double weight,double height,int age,boolean male,double calories,double protein,double carbs,double fat,String goal,String activity,String lang,String units){getWritableDatabase().update("profile",cv("weight_kg",weight,"height_cm",height,"age",age,"sex",male?"male":"female","calorie_target",calories,"protein_target",protein,"carb_target",carbs,"fat_target",fat,"goal",goal,"activity",activity,"language",lang,"units",units,"updated_at",System.currentTimeMillis()),"id=1",null);}
    public Cursor profileTargets(){return getReadableDatabase().rawQuery("SELECT weight_kg,height_cm,age,sex,goal,activity,language,units,calorie_target,protein_target,carb_target,fat_target FROM profile WHERE id=1",null);}
    public long addQuote(String kind,String author,String work,String textEn,String textAr,String category,String verification){return getWritableDatabase().insert("quote",null,cv("kind",kind,"author",author,"work",work,"text_en",textEn,"text_ar",textAr,"category",category,"verification",verification,"created_at",System.currentTimeMillis()));}
    public int quoteCount(){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM quote",null);try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}}
    public Cursor nextQuote(){return getReadableDatabase().rawQuery("SELECT id,kind,author,work,text_en,text_ar,category,verification,source_url,shown_count FROM quote WHERE verification IS NOT NULL ORDER BY shown_count ASC,COALESCE(last_shown_at,0) ASC,id ASC LIMIT 1",null);}
    public void markQuoteShown(long id){getWritableDatabase().execSQL("UPDATE quote SET shown_count=shown_count+1,last_shown_at=? WHERE id=?",new Object[]{System.currentTimeMillis(),id});}
    private static int QuoteCount(SQLiteDatabase db){Cursor c=db.rawQuery("SELECT COUNT(*) FROM quote",null);try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}}
    private static void putDefault(SQLiteDatabase db,String key,String value){db.insertWithOnConflict("settings",null,cv("key",key,"value",value),SQLiteDatabase.CONFLICT_IGNORE);}
    private static void seedQuotes(SQLiteDatabase db){
        String[][] q={
            {"idea","Epictetus","Enchiridion","Focus your effort on what is actually yours to act on; let the rest stop consuming your attention.","خلي طاقتك في اللي تقدر تتصرف فيه فعلًا، وسيب اللي خارج إيدك من غير ما ياخد كل انتباهك.","Mindset","curated","https://classics.mit.edu/Epictetus/epicench.html"},
            {"quote","Seneca","Moral Letters to Lucilius, Letter 13 §4","We suffer more often in imagination than in reality.","أحيانًا القلق على اللي ممكن يحصل بيتعبنا أكتر من الحدث نفسه.","Mindset","curated","https://en.wikisource.org/wiki/Moral_letters_to_Lucilius/Letter_13"},
            {"idea","Ibn al-Qayyim","Al-Fawaid","التغيير الحقيقي لا يعتمد على لحظة حماس واحدة؛ يحتاج تكرارًا ومراجعة وصبرًا على الطريق.","التغيير الحقيقي مش لحظة حماس واحدة؛ محتاج تكرار ومراجعة وصبر على الطريق.","Discipline","book_idea","https://books.google.com/books/about/AL_Fawaid.html?id=ldjgzQEACAAJ"},
            {"idea","Naguib Mahfouz","The Thief and the Dogs","فكرة مستخلصة: قراءة دوافعك بصدق مهمة، لأن تفسيرك لنفسك يغيّر الطريقة التي تتصرف بها.","فكرة مستخلصة: فهم دوافعك بصدق مهم، لأن تفسيرك لنفسك بيغيّر طريقة تصرفك.","Reflection","work_idea","https://books.google.com/books/about/The_Thief_and_the_Dogs.html?id=jyGUPBhG2poC"},
            {"idea","Ahmed Khaled Tawfik","Utopia","فكرة مستخلصة: الظروف الاجتماعية المحيطة بالإنسان قد تغيّر خياراته، لذلك راقب البيئة التي تصنع عاداتك.","فكرة مستخلصة: البيئة اللي حواليك بتأثر في اختياراتك؛ عشان كده راقب المكان والعادات اللي بيصنعهملك.","Environment","work_idea","https://www.shoroukbookstores.com/books/view.aspx?id=17466e48-ec61-4e66-9425-817e099f3ea0"},
            {"idea","Robert Greene","Mastery","فكرة مستخلصة: المعرفة تبدأ تتحول إلى مهارة عندما تدخل في ممارسة وملاحظة وتصحيح متكرر.","فكرة مستخلصة: المعرفة بتبدأ تبقى مهارة لما تدخل في ممارسة وملاحظة وتصحيح متكرر.","Learning","book_idea","https://www.penguinrandomhouse.com/books/302099/mastery-by-robert-greene/"},
            {"idea","David Goggins","Can't Hurt Me","فكرة مستخلصة: لا تجعل مزاج اللحظة هو العامل الوحيد الذي يقرر هل ستلتزم بما خططت له.","فكرة مستخلصة: ما تخليش مزاج اللحظة هو اللي يقرر لو هتلتزم بخطتك ولا لأ.","Discipline","book_idea","https://books.google.com/books/about/Can_t_Hurt_Me.html?id=ng84vQEACAAJ"},
            {"idea","Rocky Balboa","Rocky Balboa (2006)","فكرة مستخلصة: بعد الضربة أو التعثر، الاستمرار في الحركة أهم من انتظار طريق بلا صدمات.","فكرة مستخلصة: بعد التعثر، الاستمرار أهم من إنك تستنى طريق مفيهوش صدمات.","Resilience","film_idea","https://www.revolutionstudios.com/film/rocky-balboa/"},
            {"idea","Batman Begins","Batman Begins (2005)","فكرة مستخلصة: فهم مصدر الخوف قد يكون أول خطوة لتقليل سيطرته على قراراتك.","فكرة مستخلصة: فهم مصدر الخوف ممكن يكون أول خطوة عشان مايبقاش هو اللي بيسوق قراراتك.","Courage","film_idea","https://www.warnerbros.com/movies/batman-begins"},
            {"idea","Viktor Frankl","Man's Search for Meaning","فكرة مستخلصة: وجود معنى واضح لا يلغي صعوبة الطريق، لكنه يعطي الصعوبة سياقًا يمكن احتماله والعمل داخله.","فكرة مستخلصة: وجود معنى واضح مش بيلغي صعوبة الطريق، لكنه بيدي الصعوبة سياق تقدر تتحمله وتتحرك جواه.","Purpose","book_idea","https://www.viktorfrankl.org/standard_publist.html"},
            {"idea","Marcus Aurelius","Meditations","فكرة مستخلصة: راقب حكمك على الحدث نفسه؛ أحيانًا الجزء الذي يرهقك هو تفسيرك له أكثر من الحدث.","فكرة مستخلصة: راقب حكمك على الحدث؛ ساعات اللي بيتعبك هو تفسيرك للحاجة أكتر من الحاجة نفسها.","Mindset","book_idea","https://classics.mit.edu/Antoninus/meditations.html"},
            {"idea","Aristotle","Nicomachean Ethics","فكرة مستخلصة: العادة تتكون من أفعال تتكرر، لذلك التغيير العملي يبدأ من السلوك الذي تكرره يومًا بعد يوم.","فكرة مستخلصة: العادة بتتكوّن من أفعال بتتكرر؛ عشان كده التغيير العملي بيبدأ من السلوك اللي بتكرره كل يوم.","Habits","book_idea","https://classics.mit.edu/Aristotle/nicomachaen.html"}
        };
        long now=System.currentTimeMillis();
        for(String[] x:q)db.insert("quote",null,cv("kind",x[0],"author",x[1],"work",x[2],"text_en",x[3],"text_ar",x[4],"category",x[5],"verification",x[6],"source_url",x[7],"created_at",now));
    }

}
