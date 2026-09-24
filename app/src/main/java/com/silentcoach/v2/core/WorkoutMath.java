package com.silentcoach.v2.core;

public final class WorkoutMath {
    private WorkoutMath() {}
    public static double volume(double weightKg, int reps) { return Math.max(0,weightKg)*Math.max(0,reps); }
    public static double e1rmEpley(double weightKg,int reps) { if(weightKg<=0||reps<=0)return 0; return weightKg*(1+reps/30.0); }
    public static boolean isRepPR(double weightKg,int reps,double previousBestWeight,int previousBestReps){
        if(weightKg<=0||reps<=0)return false;
        if(previousBestWeight<=0)return true;
        return weightKg>previousBestWeight || (Double.compare(weightKg,previousBestWeight)==0 && reps>previousBestReps);
    }
}
