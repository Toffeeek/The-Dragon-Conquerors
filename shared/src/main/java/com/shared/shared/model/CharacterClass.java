// File Location: shared/src/main/java/com/shared/shared/model/CharacterClass.java
package com.shared.shared.model;

import com.shared.shared.model.stats.StatComponent;
import com.shared.shared.model.stats.StatScale;
import com.shared.shared.model.stats.StatType;

/**
 * Six role-specific classes sharing a 48-tier baseline plus four racial boosts.
 * Equal budgets constrain tuning; they do not guarantee equal win rates.
 * See docs/RELEASE_READINESS.md for the balance rationale and remaining playtesting.
 */
public enum CharacterClass {

    //      display     role label                 Hp Mana Acc Str Spd Insp Wis
    PALADIN("Paladin", "Divine bulwark", 8, 5, 8, 8, 6, 6, 7),
    MAGE   ("Mage",    "Arcane artillery", 6, 8, 8, 4, 7, 7, 8),
    WRAITH ("Wraith",  "Shadow assassin", 6, 6, 8, 8, 9, 5, 6),
    CLERIC ("Cleric",  "Battlefield healer", 7, 7, 8, 5, 6, 7, 8),
    BARD   ("Bard",    "Inspiring support", 7, 7, 8, 5, 6, 8, 7),
    ARCHER ("Archer",  "Precision marksman", 7, 5, 9, 7, 8, 6, 6);

    /**
     * Kept as a public final field (not a getter) because existing UI code
     * reads {@code characterClass.displayName} directly.
     */
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

    public String getDisplayName() {
        return displayName;
    }

    /** Short role description shown on class-selection cards. */
    public String getRoleLabel() {
        return roleLabel;
    }

    /**
     * Builds a fresh, unboosted {@link StatComponent} for this class.
     *
     * <p>Always returns a new instance so two players of the same class never
     * share mutable stats. To apply a race on top of this, use
     * {@code CharacterBuild} rather than calling this directly.</p>
     */
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

    /**
     * Raw design tier for a stat, for UI that wants to show the design-sheet
     * number ("Speed 9") instead of the scaled engine rating ("Speed 18").
     */
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

    /** Sum of all seven tiers — a rough power yardstick used by balance tests. */
    public int totalTiers() {
        int total = 0;
        for (StatType stat : StatType.values()) {
            total += getTier(stat);
        }
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
