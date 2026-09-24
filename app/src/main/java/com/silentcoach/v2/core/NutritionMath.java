package com.silentcoach.v2.core;

public final class NutritionMath {
    private NutritionMath() {}

    public static double scalePer100g(double per100g, double grams) {
        if (!Double.isFinite(per100g) || !Double.isFinite(grams) || grams < 0 || per100g < 0) throw new IllegalArgumentException("Invalid nutrition input");
        return per100g * grams / 100.0;
    }
    public static MacroTotals scale(MacroPer100g p, double grams) {
        return new MacroTotals(
                scalePer100g(p.kcal, grams),
                scalePer100g(p.protein, grams),
                scalePer100g(p.carbs, grams),
                scalePer100g(p.fat, grams),
                scalePer100g(p.fiber, grams));
    }
    public static double round1(double v) { return Math.round(v * 10.0) / 10.0; }
    public static class MacroPer100g { public final double kcal, protein, carbs, fat, fiber; public MacroPer100g(double kcal,double protein,double carbs,double fat,double fiber){this.kcal=kcal;this.protein=protein;this.carbs=carbs;this.fat=fat;this.fiber=fiber;} }
    public static class MacroTotals { public final double kcal, protein, carbs, fat, fiber; public MacroTotals(double kcal,double protein,double carbs,double fat,double fiber){this.kcal=kcal;this.protein=protein;this.carbs=carbs;this.fiber=fiber;this.fat=fat;} }
}
