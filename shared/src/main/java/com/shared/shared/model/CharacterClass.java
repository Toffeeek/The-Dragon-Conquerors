// File Location: shared/src/main/java/com/shared/shared/model/CharacterClass.java
package com.shared.shared.model;

import com.shared.shared.model.stats.StatComponent;
import com.shared.shared.model.stats.StatScale;
import com.shared.shared.model.stats.StatType;

/**
 * The six playable classes and their base stats, exactly as specified in the
 * game design document.
 *
 * <p>Stats are stored as design <b>tiers</b> (the small 4-9 numbers from the
 * design sheet) rather than engine values, so this enum stays readable against
 * the document and reviewable in a pull request. {@link StatScale} performs the
 * tier -> engine conversion in {@link #createStats()}.</p>
 *
 * <pre>
 *            Hp Mana Acc Str Spd Insp Wis   tier total
 *  Paladin    8   4    8   8   6    5   5      44
 *  Mage       7   8    9   4   7    7   8      50
 *  Wraith     6   7    9   8   9    5   7      51
 *  Cleric     7   7    9   6   7    8   8      52
 *  Bard       7   7    9   4   7    8   8      50
 *  Archer     7   5    9   5   6    5   5      42
 * </pre>
 *
 * <p><b>On balance:</b> the tier totals above are deliberately left as the
 * design document specifies them, so they are not equal (Cleric 52 vs Archer
 * 42). Class parity is therefore <em>not</em> achieved through raw stats; it is
 * achieved through ability power, mana cost and cooldown budgets, which live in
 * {@code AbilityType}. Race choice adds no power gap at all — see
 * {@code RaceClassSynergy}, which grants every race/class pair exactly the same
 * boost budget.</p>
 *
 * <p><b>Adding a class:</b> add an entry here, add its sprite sheet to
 * {@code SpriteAssets}, add its abilities to {@code AbilityType}, and add a
 * fallback colour in {@code PlayerRenderer}. Nothing else switches on this
 * enum.</p>
 */
public enum CharacterClass {

    //      display     role label                 Hp Mana Acc Str Spd Insp Wis
    PALADIN("Paladin", "Divine bulwark",            8,  4,   8,  8,  6,   5,  5),
    MAGE   ("Mage",    "Arcane artillery",          7,  8,   9,  4,  7,   7,  8),
    WRAITH ("Wraith",  "Shadow assassin",           6,  7,   9,  8,  9,   5,  7),
    CLERIC ("Cleric",  "Battlefield healer",        7,  7,   9,  6,  7,   8,  8),
    BARD   ("Bard",    "Inspiring support",         7,  7,   9,  4,  7,   8,  8),
    ARCHER ("Archer",  "Precision marksman",        7,  5,   9,  5,  6,   5,  5);

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
