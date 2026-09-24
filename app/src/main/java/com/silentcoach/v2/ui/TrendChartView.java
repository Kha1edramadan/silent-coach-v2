package com.silentcoach.v2.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/** Lightweight offline chart so progress remains usable without a charting dependency. */
public final class TrendChartView extends View {
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<Float> values = new ArrayList<>();
    private int accent = 0xFFBAF56F;
    private int muted = 0xFF959EAA;
    private int border = 0xFF353C46;
    private int surface = 0xFF12161B;

    public TrendChartView(Context c) {
        super(c);
        setMinimumHeight(dp(150));
        line.setStrokeWidth(dp(2));
        line.setStyle(Paint.Style.STROKE);
        grid.setStrokeWidth(dp(1));
        text.setTextSize(dp(11));
        text.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        setBackgroundColor(surface);
        setPadding(dp(14), dp(12), dp(14), dp(12));
    }

    public void setValues(List<Float> v) {
        values = new ArrayList<>(v == null ? new ArrayList<Float>() : v);
        invalidate();
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float left = getPaddingLeft();
        float top = getPaddingTop();
        float right = getWidth() - getPaddingRight();
        float bottom = getHeight() - getPaddingBottom();
        float w = Math.max(1, right - left);
        float h = Math.max(1, bottom - top);

        grid.setColor(border);
        for (int i = 0; i < 4; i++) {
            float y = top + (h * i / 3f);
            c.drawLine(left, y, right, y, grid);
        }

        if (values.size() < 2) {
            text.setColor(muted);
            c.drawText("Not enough data yet", left, top + h / 2f, text);
            return;
        }

        float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
        for (Float v : values) {
            if (v == null || !Float.isFinite(v)) continue;
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        if (!Float.isFinite(min) || !Float.isFinite(max)) return;
        if (Math.abs(max - min) < 0.0001f) { max += 1f; min -= 1f; }
        else { float pad = (max - min) * 0.12f; max += pad; min -= pad; }

        Path p = new Path();
        for (int i = 0; i < values.size(); i++) {
            float v = values.get(i);
            float x = left + w * (i / (float)(values.size() - 1));
            float y = bottom - ((v - min) / (max - min)) * h;
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        line.setColor(accent);
        c.drawPath(p, line);

        text.setColor(muted);
        String hi = trim(max);
        String lo = trim(min);
        c.drawText(hi, left, top + text.getTextSize(), text);
        c.drawText(lo, left, bottom, text);
    }

    private String trim(float n) {
        if (Math.abs(n - Math.round(n)) < 0.05f) return String.valueOf(Math.round(n));
        return String.format(java.util.Locale.US, "%.1f", n);
    }

    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
}
