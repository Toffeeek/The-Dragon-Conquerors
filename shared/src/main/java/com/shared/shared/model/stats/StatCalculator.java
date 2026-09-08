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

    /**
     * the value at which a stat neither helps nor hurts
     *
     * <p>already the assumption baked into {@link #effectiveMaxMana}, which reads
     * {@code (wisdom-10)*5}; naming it keeps the combat scaling below consistent
     * with that instead of repeating a bare 10</p>
     */
    public static final int NEUTRAL_STAT = 10;

    /** a roll can never be a certainty, and can never be hopeless */
    public static final int MIN_HIT_CHANCE = 5;
    public static final int MAX_HIT_CHANCE = 95;

    /**
     * how much one point of strength or wisdom is worth
     *
     * <p>deliberately small: at 4% a point, the Paladin's 16 strength is +24%
     * damage, which is felt without letting a stat spread eclipse the ability
     * numbers those spreads were tuned against</p>
     */
    private static final float DAMAGE_PER_STRENGTH = .04f;
    private static final float HEALING_PER_WISDOM = .03f;

    /** an ability's base damage adjusted for the attacker's strength */
    public static int scaleDamage(int baseDamage, StatComponent attacker){
        if(baseDamage <= 0 || attacker == null) return Math.max(0, baseDamage);

        float multiplier = 1f+((attacker.getStrength()-NEUTRAL_STAT)*DAMAGE_PER_STRENGTH);
        return Math.max(1, Math.round(baseDamage*multiplier));
    }

    /** an ability's base healing adjusted for the healer's wisdom */
    public static int scaleHealing(int baseHealing, StatComponent healer){
        if(baseHealing <= 0 || healer == null) return Math.max(0, baseHealing);

        float multiplier = 1f+((healer.getWisdom()-NEUTRAL_STAT)*HEALING_PER_WISDOM);
        return Math.max(1, Math.round(baseHealing*multiplier));
    }

    /**
     * percent chance the attacker lands a hit on this defender
     *
     * <p>accuracy pushes it up, the defender's speed pulls it down, and the result
     * is clamped so no matchup is ever an auto-hit or an auto-miss</p>
     */
    public static int hitChanceAgainst(StatComponent attacker, StatComponent defender){
        if(attacker == null) return MIN_HIT_CHANCE;
        if(defender == null) return hitChances(attacker);

        int chance = hitChances(attacker)-evasionChance(defender);
        return Math.min(MAX_HIT_CHANCE, Math.max(MIN_HIT_CHANCE, chance));
    }
}
