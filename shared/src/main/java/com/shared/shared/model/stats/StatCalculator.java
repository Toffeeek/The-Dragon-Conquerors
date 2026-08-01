package com.shared.shared.model.stats;

/**
 * derives gameplay values from raw stats
 */

public class StatCalculator {
    public static int deriveStamina(StatComponent stats){
        int base = 2+(stats.getSpeed()/4);
        int bonus = inspirationBonus(stats);

        return Math.min(8, base+bonus);
    }

    public static int hitChances(StatComponent stats){
        int base = 40+(stats.getAccuracy()*3);
        int bonus = inspirationBonus(stats)*5;

        return Math.min(95, base+bonus);
    }

    public static int effectiveMaxMana(StatComponent stats){
        int base = stats.getMaxMana()+(stats.getWisdom()-10)*5;
        int bonus = inspirationBonus(stats)*5;

        return Math.max(10, base+bonus);
    }

    public static int manaRegenPerTurn(StatComponent stats){
        int base = 2+(stats.getWisdom()/5);

        return base+inspirationBonus(stats);
    }

    public static int evasionChance(StatComponent stats){
        int base = Math.max(0, (stats.getSpeed()-5)*3);
        int bonus = inspirationBonus(stats)*5;

        return Math.min(60, base+bonus);
    }

    public static int initiative(StatComponent stats){
        int base = stats.getSpeed()+(stats.getAccuracy()/2);
        return base+inspirationBonus(stats);
    }

    public static int inspirationBonus(StatComponent stats){
        return stats.getInspiration() > 10 ? 1:0;
    }

    public static float deriveMaxMovementDistance(StatComponent stats){
        float base = 3f+(stats.getSpeed()/5f);
        return base + (inspirationBonus(stats)*.5f);
    }
}
