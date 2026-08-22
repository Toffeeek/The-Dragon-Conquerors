// File Location: shared/src/main/java/com/shared/shared/model/stats/StatType.java
package com.shared.shared.model.stats;

/**
 * Addressable handle for the seven stats every character has.
 *
 * <p>Without this enum, anything that wants to modify "some stat chosen at
 * runtime" (racial boosts, Bard buffs, Archer's Accuracy Boost, Sub-zero's
 * speed penalty) would need a hard-coded {@code if/else} chain against
 * {@link StatComponent} setters. With it, a boost becomes plain data:
 * {@code new StatBoost(StatType.STRENGTH, 2)}.</p>
 *
 * <p>{@link #read(StatComponent)} and {@link #add(StatComponent, int)} are the
 * only two operations callers need; both hide the fact that HP and Mana are
 * paired with a maximum while the other five are single rating values.</p>
 */
public enum StatType {

    HP("HP"),
    MANA("Mana"),
    ACCURACY("Accuracy"),
    STRENGTH("Strength"),
    SPEED("Speed"),
    INSPIRATION("Inspiration"),
    WISDOM("Wisdom");

    private final String displayName;

    StatType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Reads the current engine value of this stat.
     *
     * <p>For {@link #HP} and {@link #MANA} this returns the <em>maximum</em>,
     * not the current pool, because callers ask this question when displaying
     * or comparing builds rather than mid-combat resources.</p>
     */
    public int read(StatComponent stats) {
        switch (this) {
            case HP:
                return stats.getMaxHp();
            case MANA:
                return stats.getMaxMana();
            case ACCURACY:
                return stats.getAccuracy();
            case STRENGTH:
                return stats.getStrength();
            case SPEED:
                return stats.getSpeed();
            case INSPIRATION:
                return stats.getInspiration();
            case WISDOM:
                return stats.getWisdom();
            default:
                throw new IllegalStateException("Unhandled stat: " + this);
        }
    }

    /**
     * Adds {@code amount} engine points to this stat, in place.
     *
     * <p>HP and Mana raise both the pool and its maximum so a build-time boost
     * is immediately usable. The five secondary stats are clamped to 1-20 by
     * {@link StatComponent}'s own setters.</p>
     *
     * @param amount may be negative, which is how debuffs such as Sub-zero's
     *               speed penalty are applied
     */
    public void add(StatComponent stats, int amount) {
        switch (this) {
            case HP:
                stats.addMaxHp(amount);
                break;
            case MANA:
                stats.addMaxMana(amount);
                break;
            case ACCURACY:
                stats.setAccuracy(stats.getAccuracy() + amount);
                break;
            case STRENGTH:
                stats.setStrength(stats.getStrength() + amount);
                break;
            case SPEED:
                stats.setSpeed(stats.getSpeed() + amount);
                break;
            case INSPIRATION:
                stats.setInspiration(stats.getInspiration() + amount);
                break;
            case WISDOM:
                stats.setWisdom(stats.getWisdom() + amount);
                break;
            default:
                throw new IllegalStateException("Unhandled stat: " + this);
        }
    }
}
