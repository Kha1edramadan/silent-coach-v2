package com.silentcoach.v2.core;

public final class CoreSelfTest {
    public static void main(String[] args) {
        NutritionMath.MacroTotals x=NutritionMath.scale(new NutritionMath.MacroPer100g(130,23,0,3,0),200);
        check(close(x.kcal,260)&&close(x.protein,46)&&close(x.fat,6),"nutrition scaling");
        GoalMath.Targets t=GoalMath.initialTargets(70,175,22,true,"moderate","gain");
        check(t.kcal>0 && t.protein>0 && t.carbs>=0 && t.fat>0,"goal math");
        check(close(WorkoutMath.volume(70,10),700),"volume");
        check(WorkoutMath.isRepPR(75,8,70,10),"PR");
        check(!WorkoutMath.isRepPR(70,8,70,10),"non-PR");
        check("consistent".equals(DataQualityMath.status(130,23,0,3)),"food quality consistent");
        check("check".equals(DataQualityMath.status(500,2,30,30)),"food quality check");
        check("missing".equals(DataQualityMath.status(0,0,0,0)),"food quality missing");
        boolean threw=false; try { NutritionMath.scalePer100g(10,-1); } catch (IllegalArgumentException ok) { threw=true; }
        check(threw,"nutrition rejects negative grams");
        threw=false; try { GoalMath.bmrMifflin(0,175,22,true); } catch (IllegalArgumentException ok) { threw=true; }
        check(threw,"goal math rejects invalid profile");
        System.out.println("CORE_SELF_TEST=PASS");
    }
    private static boolean close(double a,double b){return Math.abs(a-b)<1e-9;}
    private static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);}
}
