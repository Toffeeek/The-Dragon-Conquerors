// File Location: shared/src/main/java/com/shared/shared/model/Race.java
package com.shared.shared.model;

import com.shared.shared.model.stats.StatType;

/**
 * The four playable races.
 *
 * <p>A race contributes stats in two ways:</p>
 *
 * <ol>
 *   <li>A <b>baseline trait</b> — one boost step on a single stat, granted to
 *       every class of that race. This is the race's identity and guarantees no
 *       race is ever a dead pick.</li>
 *   <li>A <b>synergy or affinity bonus</b> — three further boost steps that
 *       depend on the paired class. The design document's six named synergies
 *       (Paladin+Human, Elf+Mage, ...) are the flavourful spikes; every other
 *       pairing receives a generic affinity spread of the same size. Both live
 *       in {@code RaceClassSynergy}.</li>
 * </ol>
 *
 * <p><b>Baseline stats are chosen to never overlap the race's own synergy
 * stats.</b> Human's baseline is Inspiration while Human+Paladin boosts
 * Strength/HP/Accuracy; Undead's baseline is Strength while Undead+Wraith
 * boosts Speed/Wisdom/HP. This prevents a boost from landing twice on one stat
 * and creating a runaway value — the exact failure mode that produces an
 * overpowered build.</p>
 */
public enum Race {

    HUMAN("Human",
        "Resolute and adaptable; humans keep their nerve when a plan collapses.",
        StatType.INSPIRATION),

    ELF("Elf",
        "Long-lived and precise; elves read a battlefield before they enter it.",
        StatType.ACCURACY),

    DRAGONBORNE("Dragonborne",
        "Draconic blood and scaled hide; born to hold the front line.",
        StatType.HP),

    UNDEAD("Undead",
        "Relentless and unliving; pain is information, not a warning.",
        StatType.STRENGTH);

    /** Public final to mirror {@link CharacterClass#displayName}. */
    public final String displayName;

    private final String description;
    private final StatType baselineStat;

    Race(String displayName, String description, StatType baselineStat) {
        this.displayName = displayName;
        this.description = description;
        this.baselineStat = baselineStat;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Flavour text for the race-selection screen. */
    public String getDescription() {
        return description;
    }

    /** The stat every class of this race gains one boost step in. */
    public StatType getBaselineStat() {
        return baselineStat;
    }

    /** Case-insensitive lookup that returns {@code null} instead of throwing. */
    public static Race fromName(String name) {
        if (name == null) return null;
        for (Race race : values()) {
            if (race.name().equalsIgnoreCase(name)
                || race.displayName.equalsIgnoreCase(name)) {
                return race;
            }
        }
        return null;
    }
}
