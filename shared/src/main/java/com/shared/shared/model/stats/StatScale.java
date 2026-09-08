// File Location: shared/src/main/java/com/shared/shared/model/stats/StatScale.java
package com.shared.shared.model.stats;

/**
 * Single source of truth for converting design-sheet "tiers" into engine values.
 *
 * <p>The game design document specifies class stats on a small tier scale
 * (roughly 4-9), for example {@code Paladin: Hp-8, Mana-4, Accuracy-8 ...}.
 * The runtime {@link StatComponent}, however, works with two different kinds of
 * numbers:</p>
 *
 * <ul>
 *   <li><b>HP / Mana</b> are absolute resource pools (a Paladin needs ~120 HP,
 *       not 8 HP).</li>
 *   <li><b>Accuracy, Strength, Speed, Inspiration, Wisdom</b> are 1-20 rating
 *       values. {@link StatCalculator} treats <b>10 as the neutral midpoint</b>
 *       (see {@code effectiveMaxMana}, which uses {@code (wisdom - 10) * 5}).</li>
 * </ul>
 *
 * <p>That is why secondary stats are multiplied by {@link #SECONDARY_PER_TIER}:
 * a design tier of 5 becomes a rating of 10 (neutral), tier 8 becomes 16
 * (strong), tier 4 becomes 8 (weak). Scaling here rather than rewriting
 * {@link StatCalculator} keeps the already-tuned stamina, hit-chance, evasion
 * and movement formulas intact.</p>
 *
 * <p><b>Tuning note for the team:</b> these four constants are the only place
 * balance-wide numeric scaling should be changed. Changing a constant here
 * rescales every class and every racial boost consistently.</p>
 */
public final class StatScale {

    /** Engine HP granted per design tier of HP. Tier 8 -> 120 HP. */
    public static final int HP_PER_TIER = 15;

    /** Engine mana granted per design tier of Mana. Tier 8 -> 96 mana. */
    public static final int MANA_PER_TIER = 12;

    /**
     * Rating points granted per design tier for the five secondary stats.
     * Tier 5 -> 10 (the neutral midpoint assumed by {@link StatCalculator}).
     */
    public static final int SECONDARY_PER_TIER = 2;

    /**
     * Size of a single "+" boost step, expressed in tiers.
     *
     * <p>A "+" in the design document is one step; a "++" is two steps
     * (see {@link RaceClassSynergy}).</p>
     */
    public static final int BOOST_STEP_IN_TIERS = 1;

    private StatScale() {
        // Static utility holder — never instantiated.
    }

    /** Converts a design HP tier into an absolute HP pool. */
    public static int hp(int tier) {
        return Math.max(1, tier * HP_PER_TIER);
    }

    /** Converts a design Mana tier into an absolute mana pool. */
    public static int mana(int tier) {
        return Math.max(0, tier * MANA_PER_TIER);
    }

    /** Converts a design tier into a 1-20 secondary stat rating. */
    public static int secondary(int tier) {
        return Math.max(1, tier * SECONDARY_PER_TIER);
    }

    /**
     * Engine value of one boost step for the given stat.
     *
     * <p>Used by {@link RaceClassSynergy} so that "Strength+" and "Hp+" both
     * mean "one design tier", even though they land on differently scaled
     * fields.</p>
     */
    public static int stepValue(StatType stat) {
        switch (stat) {
            case HP:
                return BOOST_STEP_IN_TIERS * HP_PER_TIER;
            case MANA:
                return BOOST_STEP_IN_TIERS * MANA_PER_TIER;
            default:
                return BOOST_STEP_IN_TIERS * SECONDARY_PER_TIER;
        }
    }
}
