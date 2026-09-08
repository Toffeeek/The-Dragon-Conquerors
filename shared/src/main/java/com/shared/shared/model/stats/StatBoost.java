// File Location: shared/src/main/java/com/shared/shared/model/stats/StatBoost.java
package com.shared.shared.model.stats;

/**
 * One immutable "+N to a stat" instruction.
 *
 * <p>This is the atom the whole boost system is built from. The design document
 * writes boosts as {@code Str+} and {@code Mana++}; those become
 * {@code new StatBoost(STRENGTH, 1)} and {@code new StatBoost(MANA, 2)}.</p>
 *
 * <p>Because a boost is data rather than code, the same type serves racial
 * synergies, the Bard's Stat Boost ability, the Archer's Accuracy Boost (+2),
 * and Sub-zero's negative speed step — and a balance test can sum the
 * {@link #points()} of any collection to verify a budget.</p>
 */
public final class StatBoost {

    private final StatType stat;
    private final int steps;

    /**
     * @param stat  which stat to modify
     * @param steps how many boost steps; one step equals one design tier.
     *              Negative values are debuffs.
     */
    public StatBoost(StatType stat, int steps) {
        if (stat == null) throw new IllegalArgumentException("stat must not be null");
        this.stat = stat;
        this.steps = steps;
    }

    /** Convenience factory for a single "+" step. */
    public static StatBoost plus(StatType stat) {
        return new StatBoost(stat, 1);
    }

    /** Convenience factory for a "++" double step. */
    public static StatBoost plusPlus(StatType stat) {
        return new StatBoost(stat, 2);
    }

    public StatType getStat() {
        return stat;
    }

    public int getSteps() {
        return steps;
    }

    /**
     * Budget cost of this boost.
     *
     * <p>Uses the absolute step count so a bundle cannot be smuggled past a
     * budget check by pairing a large buff with a large debuff.</p>
     */
    public int points() {
        return Math.abs(steps);
    }

    /** Engine value this boost adds, resolved through {@link StatScale}. */
    public int engineAmount() {
        return steps * StatScale.stepValue(stat);
    }

    /**
     * The exact opposite of this boost.
     *
     * <p>Used to reverse a temporary modifier when it wears off — Sub-zero
     * applies {@code Speed-2} on gain and {@code inverted()} on expiry, so the
     * effect engine never needs to remember the original value.</p>
     *
     * <p><b>Caveat:</b> {@link StatComponent} clamps the five secondary stats to
     * 1-20, so a boost that was clamped on the way down does not restore exactly.
     * In practice this cannot happen — the lowest Speed in the roster is design
     * tier 6 (engine 12) and Sub-zero costs 4 engine points — but a future
     * stacking debuff would need to track the pre-boost value instead.</p>
     */
    public StatBoost inverted() {
        return new StatBoost(stat, -steps);
    }

    /** Applies this boost to {@code stats} in place. */
    public void applyTo(StatComponent stats) {
        stat.add(stats, engineAmount());
    }

    /** Renders as {@code "Strength++"} / {@code "Speed-"} for UI display. */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(stat.getDisplayName());
        char symbol = steps < 0 ? '-' : '+';
        for (int i = 0; i < Math.abs(steps); i++) {
            builder.append(symbol);
        }
        return builder.toString();
    }
}
