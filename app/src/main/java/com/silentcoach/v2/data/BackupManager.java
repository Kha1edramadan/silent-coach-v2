package com.silentcoach.v2.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

/** Portable on-device export. The export is JSON so it can later be migrated to cloud sync without changing user data semantics. */
public final class BackupManager {
    private static final String[] TABLES = {
            "profile","program","program_day","exercise","program_exercise",
            "workout_session","workout_exercise","workout_set","food_source","food",
            "food_nutrient","meal","meal_item","body_measurement","settings","quote",
            "saved_meal","saved_meal_item","favorite_exercise","reminder"
    };
    private BackupManager() {}

    public static void importJson(CoachDb coachDb, String json) throws Exception {
        JSONObject root = new JSONObject(json);
        if (!"silent_coach_backup".equals(root.optString("format")))
            throw new IllegalArgumentException("Unsupported backup format");
        int version = root.optInt("format_version", 0);
        if (version < 1) throw new IllegalArgumentException("Unsupported backup version");
        JSONObject tables = root.getJSONObject("tables");
        if (!tables.has("profile") || !tables.has("settings"))
            throw new IllegalArgumentException("Incomplete backup");
        SQLiteDatabase db = coachDb.getWritableDatabase();
        db.beginTransaction();
        try {
            // Delete children before parents while foreign keys are enabled.
            String[] deleteOrder = {
                    "meal_item","saved_meal_item","workout_set","workout_exercise",
                    "program_exercise","program_day","body_measurement","meal",
                    "saved_meal","workout_session","favorite_exercise","reminder",
                    "food_nutrient","food","food_source","exercise","program",
                    "quote","settings","profile"
            };
            for (String table : deleteOrder) db.delete(table, null, null);
            for (String table : TABLES) {
                JSONArray rows = tables.optJSONArray(table);
                if (rows == null) continue;
                for (int i = 0; i < rows.length(); i++) {
                    JSONObject row = rows.getJSONObject(i);
                    android.content.ContentValues values = new android.content.ContentValues();
                    java.util.Iterator<String> keys = row.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        Object value = row.get(key);
                        if (value == JSONObject.NULL) values.putNull(key);
                        else if (value instanceof Integer || value instanceof Long) values.put(key, ((Number) value).longValue());
                        else if (value instanceof Number) values.put(key, ((Number) value).doubleValue());
                        else values.put(key, String.valueOf(value));
                    }
                    if (db.insertOrThrow(table, null, values) < 0) throw new IllegalStateException("Import failed: " + table);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public static String exportJson(CoachDb coachDb) throws Exception {
        SQLiteDatabase db = coachDb.getReadableDatabase();
        JSONObject root = new JSONObject();
        root.put("format", "silent_coach_backup");
        root.put("format_version", 1);
        root.put("exported_at", System.currentTimeMillis());
        JSONObject tables = new JSONObject();
        for (String table : TABLES) {
            JSONArray rows = new JSONArray();
            Cursor c = db.rawQuery("SELECT * FROM " + table, null);
            try {
                int count = c.getColumnCount();
                String[] cols = c.getColumnNames();
                while (c.moveToNext()) {
                    JSONObject row = new JSONObject();
                    for (int i = 0; i < count; i++) {
                        if (c.isNull(i)) row.put(cols[i], JSONObject.NULL);
                        else {
                            switch (c.getType(i)) {
                                case Cursor.FIELD_TYPE_INTEGER: row.put(cols[i], c.getLong(i)); break;
                                case Cursor.FIELD_TYPE_FLOAT: row.put(cols[i], c.getDouble(i)); break;
                                case Cursor.FIELD_TYPE_BLOB: row.put(cols[i], new String(c.getBlob(i), java.nio.charset.StandardCharsets.UTF_8)); break;
                                default: row.put(cols[i], c.getString(i));
                            }
                        }
                    }
                    rows.put(row);
                }
            } finally { c.close(); }
            tables.put(table, rows);
        }
        root.put("tables", tables);
        return root.toString(2);
    }
}
