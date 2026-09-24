package com.silentcoach.v2.core;

public final class GoalMath {
    private GoalMath() {}
    public static double bmrMifflin(double weightKg, double heightCm, int age, boolean male) {
        if (weightKg<=0||heightCm<=0||age<=0) throw new IllegalArgumentException("Invalid profile");
        return 10*weightKg + 6.25*heightCm - 5*age + (male?5:-161);
    }
    public static double activityFactor(String activity) {
        if ("sedentary".equals(activity)) return 1.20;
        if ("light".equals(activity)) return 1.375;
        if ("moderate".equals(activity)) return 1.55;
        if ("high".equals(activity)) return 1.725;
        if ("very_high".equals(activity)) return 1.90;
        return 1.375;
    }
    public static Targets initialTargets(double weightKg,double heightCm,int age,boolean male,String activity,String goal) {
        double bmr=bmrMifflin(weightKg,heightCm,age,male), tdee=bmr*activityFactor(activity), cal=tdee;
        if ("gain".equals(goal)) cal += 250; else if ("lose".equals(goal)) cal -= 350;
        double protein=weightKg*1.6;
        double fat=weightKg*0.8;
        double carbs=Math.max(0,(cal-protein*4-fat*9)/4);
        return new Targets(Math.round(cal),Math.round(protein*10)/10.0,Math.round(carbs*10)/10.0,Math.round(fat*10)/10.0);
    }
    public static class Targets { public final double kcal,protein,carbs,fat; public Targets(double k,double p,double c,double f){kcal=k;protein=p;carbs=c;fat=f;} }
}
