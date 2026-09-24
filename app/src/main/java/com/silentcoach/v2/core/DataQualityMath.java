package com.silentcoach.v2.core;

/** Non-blocking plausibility check for food composition records. It flags values for review; it never replaces source data. */
public final class DataQualityMath {
    private DataQualityMath() {}

    public static double macroKcalEstimate(double protein, double carbs, double fat) {
        return Math.max(0, protein) * 4.0 + Math.max(0, carbs) * 4.0 + Math.max(0, fat) * 9.0;
    }

    public static String status(double kcal, double protein, double carbs, double fat) {
        if (!Double.isFinite(kcal) || !Double.isFinite(protein) || !Double.isFinite(carbs) || !Double.isFinite(fat)) return "invalid";
        if (kcal == 0 && protein == 0 && carbs == 0 && fat == 0) return "missing";
        double estimate = macroKcalEstimate(protein, carbs, fat);
        if (estimate < 1 && kcal > 1) return "check";
        double delta = Math.abs(kcal - estimate) / Math.max(kcal, estimate);
        return delta <= 0.12 ? "consistent" : "check";
    }
}
