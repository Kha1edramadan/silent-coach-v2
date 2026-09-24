package com.silentcoach.v2.data;

import android.database.Cursor;

public final class QuoteRepository {
    private QuoteRepository() {}
    public static String displayText(Cursor c, String lang) {
        String en=c.getString(c.getColumnIndexOrThrow("text_en"));
        String ar=c.getString(c.getColumnIndexOrThrow("text_ar"));
        return "ar".equals(lang)?ar:en;
    }
}
