// File Location: shared/src/main/java/com/shared/shared/model/CharacterClass.java
package com.shared.shared.model;

import com.shared.shared.model.stats.StatComponent;
import com.shared.shared.model.stats.StatScale;
import com.shared.shared.model.stats.StatType;

/**
 * The six playable classes and their base stat tiers.
 *
 * <p>Stats are stored as compact design tiers and converted to engine values by
 * {@link StatScale}. Soldier uses a sturdy, weapon-focused frontline profile.</p>
 *
 * <pre>
 *            Hp Mana Acc Str Spd Insp Wis   tier total
 *  Paladin    8   4    8   8   6    5   5      44
 *  Mage       7   8    9   4   7    7   8      50
 *  Wraith     6   7    9   8   9    5   7      51
 *  Cleric     7   7    9   6   7    8   8      52
 *  Soldier    8   5    8   8   7    6   5      47
 *  Archer     7   5    9   5   6    5   5      42
 * </pre>
 *
 * <p>Class parity is not based only on the tier totals above. Ability power,
 * mana cost, range, and cooldowns are tuned in {@code AbilityType}. Race choice
 * adds a fixed-size boost budget through {@code RaceClassSynergy}.</p>
 */
public enum CharacterClass {

    //       display      role label                  Hp Mana Acc Str Spd Insp Wis
    PALADIN ("Paladin",  "Divine bulwark",            8,  4,   8,  8,  6,   5,  5),
    MAGE    ("Mage",     "Arcane artillery",          7,  8,   9,  4,  7,   7,  8),
    WRAITH  ("Wraith",   "Shadow assassin",           6,  7,   9,  8,  9,   5,  7),
    CLERIC  ("Cleric",   "Battlefield healer",        7,  7,   9,  6,  7,   8,  8),
    SOLDIER ("Soldier",  "Disciplined frontline",     8,  5,   8,  8,  7,   6,  5),
    ARCHER  ("Archer",   "Precision marksman",        7,  5,   9,  5,  6,   5,  5);

    /** Kept public because existing UI code reads this field directly. */
    public final String displayName;

    private final String roleLabel;
    private final int hpTier;
    private final int manaTier;
    private final int accuracyTier;
    private final int strengthTier;
    private final int speedTier;
    private final int inspirationTier;
    private final int wisdomTier;

    CharacterClass(String displayName, String roleLabel,
                   int hpTier, int manaTier, int accuracyTier, int strengthTier,
                   int speedTier, int inspirationTier, int wisdomTier) {
        this.displayName = displayName;
        this.roleLabel = roleLabel;
        this.hpTier = hpTier;
        this.manaTier = manaTier;
        this.accuracyTier = accuracyTier;
        this.strengthTier = strengthTier;
        this.speedTier = speedTier;
        this.inspirationTier = inspirationTier;
        this.wisdomTier = wisdomTier;
    }

    public String getDisplayName() { return displayName; }

    /** Short role description shown on class-selection cards. */
    public String getRoleLabel() { return roleLabel; }

    /** Builds a fresh, unboosted stat block for this class. */
    public StatComponent createStats() {
        return new StatComponent(
            StatScale.hp(hpTier),
            StatScale.mana(manaTier),
            StatScale.secondary(accuracyTier),
            StatScale.secondary(strengthTier),
            StatScale.secondary(speedTier),
            StatScale.secondary(inspirationTier),
            StatScale.secondary(wisdomTier)
        );
    }

    /** Raw design tier for UI previews and balancing tools. */
    public int getTier(StatType stat) {
        switch (stat) {
            case HP:          return hpTier;
            case MANA:        return manaTier;
            case ACCURACY:    return accuracyTier;
            case STRENGTH:    return strengthTier;
            case SPEED:       return speedTier;
            case INSPIRATION: return inspirationTier;
            case WISDOM:      return wisdomTier;
            default:          throw new IllegalStateException("Unhandled stat: " + stat);
        }
    }

    /** Sum of all seven design tiers. */
    public int totalTiers() {
        int total = 0;
        for (StatType stat : StatType.values()) total += getTier(stat);
        return total;
    }

    /** Case-insensitive lookup that returns {@code null} instead of throwing. */
    public static CharacterClass fromName(String name) {
        if (name == null) return null;
        for (CharacterClass characterClass : values()) {
            if (characterClass.name().equalsIgnoreCase(name)
                || characterClass.displayName.equalsIgnoreCase(name)) {
                return characterClass;
            }
        }
        return null;
    }
}
