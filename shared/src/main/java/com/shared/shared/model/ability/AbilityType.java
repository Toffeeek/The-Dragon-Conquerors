// File Location: shared/src/main/java/com/shared/shared/model/ability/AbilityType.java
package com.shared.shared.model.ability;

import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.effect.StatusEffectType;
import com.shared.shared.model.stats.StatBoost;
import com.shared.shared.model.stats.StatType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The built-in ability catalogue — every ability named in the design document.
 *
 * <p>Each constant is pure data; {@code CombatResolver} reads these fields and
 * needs no per-ability branching. Entries are built through {@link Spec} because
 * an ability carries a dozen parameters and a positional constructor with twelve
 * arguments would be unreadable and easy to mis-order in review.</p>
 *
 * <h2>Balance budget</h2>
 *
 * <p>The design document gives the classes unequal stat totals (Cleric 52 tiers,
 * Archer 42 — see {@link CharacterClass}). Ability tuning is where that is paid
 * back, so the numbers below deliberately compensate:</p>
 *
 * <ul>
 *   <li><b>Archer</b> (lowest stats) gets the longest ranges, the cheapest
 *       ranged attack, and the only damaging AoE ultimate.</li>
 *   <li><b>Cleric and Bard</b> (highest stats) deal the least damage; their
 *       power is entirely in support, and their ultimates carry the longest
 *       cooldowns because Revive and Encore are the strongest tempo swings in
 *       the game.</li>
 *   <li><b>Wraith</b> pairs the highest mobility with the game's only instant
 *       kill, so Curse is a coin flip the victim can escape by hitting back, and
 *       its damage abilities are the weakest per-hit of any attacker.</li>
 *   <li><b>Paladin</b> (lowest mana pool, 4 tiers) gets the cheapest ultimate so
 *       it remains castable.</li>
 *   <li><b>Mage</b> has the highest raw damage but pays the highest mana costs,
 *       and its stats give it the second-lowest HP.</li>
 * </ul>
 *
 * <h2>Adding an ability</h2>
 *
 * <ol>
 *   <li>Add a constant with a {@link Spec}.</li>
 *   <li>Give it an {@link AbilitySlot}; the four slots map to hotkeys 1-4.</li>
 *   <li>Nothing else — {@link #forClass(CharacterClass)} discovers it
 *       automatically from {@link #getOwnerClass()}.</li>
 * </ol>
 */
public enum AbilityType implements Ability {

    // ── Universal ─────────────────────────────────────────────────────────
    BASIC_ATTACK(new Spec("Basic Attack", "A quick strike against an adjacent enemy.")
        .slot(AbilitySlot.PRIMARY).damage(10).range(1.5f).target(TargetType.ENEMY)),

    // ── Paladin: durable frontline, cheap ultimate to offset a 4-tier mana pool ──
    PALADIN_BASIC(new Spec("Holy Strike", "A blessed melee strike.")
        .owner(CharacterClass.PALADIN).slot(AbilitySlot.PRIMARY)
        .damage(14).range(1.5f).target(TargetType.ENEMY)),

    PALADIN_RANGED(new Spec("Sacred Bolt", "A bolt of light hurled at a distant foe.")
        .owner(CharacterClass.PALADIN).slot(AbilitySlot.SECONDARY)
        .damage(11).mana(8).range(4.5f).target(TargetType.ENEMY)),

    DIVINE_SMITE(new Spec("Divine Smite",
        "Ultimate: calls down radiant judgement on a single enemy.")
        .owner(CharacterClass.PALADIN).slot(AbilitySlot.ULTIMATE)
        .damage(48).mana(24).range(2f).cooldown(4).target(TargetType.ENEMY)),

    // ── Mage: highest damage, highest mana cost, fragile ───────────────────
    MAGE_BASIC(new Spec("Arcane Bolt", "A cantrip that costs no mana.")
        .owner(CharacterClass.MAGE).slot(AbilitySlot.PRIMARY)
        .damage(9).range(4f).target(TargetType.ENEMY)),

    FIREBALL(new Spec("Fireball", "Hurls flame — 50% chance to set the target burning.")
        .owner(CharacterClass.MAGE).slot(AbilitySlot.SECONDARY)
        .damage(26).mana(22).range(5.5f)
        .effect(StatusEffectType.BURN, 50).target(TargetType.ENEMY)),

    ICE_ATTACK(new Spec("Ice Attack", "Freezing shard — 50% chance to chill the target.")
        .owner(CharacterClass.MAGE).slot(AbilitySlot.TERTIARY)
        .damage(18).mana(16).range(5f)
        .effect(StatusEffectType.SUB_ZERO, 50).target(TargetType.ENEMY)),

    ELDRITCH_BLAST(new Spec("Eldritch Blast",
        "Ultimate: pushes the target back and stuns it for one turn.")
        .owner(CharacterClass.MAGE).slot(AbilitySlot.ULTIMATE)
        .damage(30).mana(30).range(5f).cooldown(4).push(3f)
        .effect(StatusEffectType.STUN, 100).target(TargetType.ENEMY)),

    // ── Wraith: fastest, only instant kill, weakest per-hit damage ─────────
    WRAITH_BASIC(new Spec("Shadow Claw", "A raking strike from the dark.")
        .owner(CharacterClass.WRAITH).slot(AbilitySlot.PRIMARY)
        .damage(11).range(1.5f).target(TargetType.ENEMY)),

    CURSE(new Spec("Curse",
        "50% chance to mark a foe for death in 2 turns — broken if you are struck.")
        .owner(CharacterClass.WRAITH).slot(AbilitySlot.SECONDARY)
        .mana(24).range(4f).cooldown(3)
        .effect(StatusEffectType.CURSE, 50).target(TargetType.ENEMY)),

    POISON_JAB(new Spec("Poison Jab", "A venomous jab — 50% chance to poison.")
        .owner(CharacterClass.WRAITH).slot(AbilitySlot.TERTIARY)
        .damage(12).mana(10).range(1.5f)
        .effect(StatusEffectType.POISON, 50).target(TargetType.ENEMY)),

    TELEPORT(new Spec("Teleport", "Ultimate: blink to any tile on the battlefield.")
        .owner(CharacterClass.WRAITH).slot(AbilitySlot.ULTIMATE)
        .mana(20).range(Float.MAX_VALUE).cooldown(3).target(TargetType.TILE)),

    // ── Cleric: lowest damage in the game; power is entirely in support ────
    CLERIC_BASIC(new Spec("Mace Strike", "A solid swing of a blessed mace.")
        .owner(CharacterClass.CLERIC).slot(AbilitySlot.PRIMARY)
        .damage(9).range(1.5f).target(TargetType.ENEMY)),

    HEAL(new Spec("Heal", "Restores health to a wounded ally.")
        .owner(CharacterClass.CLERIC).slot(AbilitySlot.SECONDARY)
        .healing(35).mana(18).range(5f).target(TargetType.ALLY)),

    REVIVE(new Spec("Revive",
        "Ultimate: returns a fallen teammate to the fight at partial health.")
        .owner(CharacterClass.CLERIC).slot(AbilitySlot.ULTIMATE)
        .healing(45).mana(35).range(2f).cooldown(6).target(TargetType.DOWNED_ALLY)),

    // ── Bard: lowest strength, highest utility, longest ultimate cooldown ──
    BARD_BASIC(new Spec("Dissonant Chord", "A jarring note that rattles a nearby foe.")
        .owner(CharacterClass.BARD).slot(AbilitySlot.PRIMARY)
        .damage(8).range(2f).target(TargetType.ENEMY)),

    STAT_BOOST(new Spec("Inspiring Verse", "Raises an ally's strength for the fight.")
        .owner(CharacterClass.BARD).slot(AbilitySlot.SECONDARY)
        .mana(16).range(5f).cooldown(2)
        .boost(new StatBoost(StatType.STRENGTH, 2)).target(TargetType.ALLY)),

    ENCORE(new Spec("Encore", "Ultimate: resets a teammate's ultimate cooldown.")
        .owner(CharacterClass.BARD).slot(AbilitySlot.ULTIMATE)
        .mana(32).range(5f).cooldown(6).restoresUltimate().target(TargetType.ALLY)),

    // ── Archer: weakest stats, so the longest reach and the damaging AoE ult ──
    ARROW_SHOT(new Spec("Arrow Shot", "A precise shot at long range.")
        .owner(CharacterClass.ARCHER).slot(AbilitySlot.PRIMARY)
        .damage(15).mana(4).range(7f).target(TargetType.ENEMY)),

    ACCURACY_BOOST(new Spec("Steady Aim", "Sharpens your aim (+2 Accuracy).")
        .owner(CharacterClass.ARCHER).slot(AbilitySlot.SECONDARY)
        .mana(10).cooldown(2)
        .boost(new StatBoost(StatType.ACCURACY, 2)).target(TargetType.SELF)),

    RAIN_OF_ARROWS(new Spec("Rain of Arrows",
        "Ultimate: a volley striking every enemy in a wide area.")
        .owner(CharacterClass.ARCHER).slot(AbilitySlot.ULTIMATE)
        .damage(26).mana(28).range(7f).area(2.5f).cooldown(4)
        .target(TargetType.AREA_ENEMIES));

    // ──────────────────────────────────────────────────────────────────────
    //  Fields
    // ──────────────────────────────────────────────────────────────────────

    private final String displayName;
    private final String description;
    private final CharacterClass ownerClass;
    private final AbilitySlot slot;
    private final int manaCost;
    private final int baseDamage;
    private final int baseHealing;
    private final float range;
    private final float areaRadius;
    private final TargetType targetType;
    private final int cooldownTurns;
    private final StatusEffectType appliedEffect;
    private final int effectChancePercent;
    private final StatBoost grantedBoost;
    private final float pushDistance;
    private final boolean restoresUltimate;

    AbilityType(Spec spec) {
        this.displayName = spec.displayName;
        this.description = spec.description;
        this.ownerClass = spec.ownerClass;
        this.slot = spec.slot;
        this.manaCost = spec.manaCost;
        this.baseDamage = spec.baseDamage;
        this.baseHealing = spec.baseHealing;
        this.range = spec.range;
        this.areaRadius = spec.areaRadius;
        this.targetType = spec.targetType;
        this.cooldownTurns = spec.cooldownTurns;
        this.appliedEffect = spec.appliedEffect;
        this.effectChancePercent = spec.effectChancePercent;
        this.grantedBoost = spec.grantedBoost;
        this.pushDistance = spec.pushDistance;
        this.restoresUltimate = spec.restoresUltimate;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Ability contract
    // ──────────────────────────────────────────────────────────────────────

    @Override public String getDisplayName()            { return displayName; }
    @Override public String getDescription()            { return description; }
    @Override public CharacterClass getOwnerClass()     { return ownerClass; }
    @Override public AbilitySlot getSlot()              { return slot; }
    @Override public int getManaCost()                  { return manaCost; }
    @Override public int getBaseDamage()                { return baseDamage; }
    @Override public int getBaseHealing()               { return baseHealing; }
    @Override public float getRange()                   { return range; }
    @Override public float getAreaRadius()              { return areaRadius; }
    @Override public TargetType getTargetType()         { return targetType; }
    @Override public int getCooldownTurns()             { return cooldownTurns; }
    @Override public StatusEffectType getAppliedEffect(){ return appliedEffect; }
    @Override public int getEffectChancePercent()       { return effectChancePercent; }
    @Override public StatBoost getGrantedBoost()        { return grantedBoost; }
    @Override public float getPushDistance()            { return pushDistance; }
    @Override public boolean restoresUltimate()         { return restoresUltimate; }

    // ──────────────────────────────────────────────────────────────────────
    //  Class -> ability lookup
    // ──────────────────────────────────────────────────────────────────────

    /** Cached so the HUD can call {@link #forClass} every frame without allocating. */
    private static final Map<CharacterClass, List<AbilityType>> BY_CLASS =
        new EnumMap<>(CharacterClass.class);

    static {
        for (CharacterClass characterClass : CharacterClass.values()) {
            List<AbilityType> abilities = new ArrayList<>();
            for (AbilityType ability : values()) {
                if (ability.ownerClass == characterClass) abilities.add(ability);
            }
            // Ordered by slot so index 0 is hotkey [1] and the ultimate is last.
            abilities.sort((left, right) ->
                Integer.compare(left.slot.ordinal(), right.slot.ordinal()));

            // Every class needs at least one free attack.
            if (abilities.isEmpty()) abilities.add(BASIC_ATTACK);

            BY_CLASS.put(characterClass, Collections.unmodifiableList(abilities));
        }
    }

    /**
     * Abilities available to a class, ordered by slot.
     *
     * <p>Index 0 is hotkey {@code [1]}; the ultimate is always last.</p>
     */
    public static List<AbilityType> forClass(CharacterClass characterClass) {
        if (characterClass == null) return List.of(BASIC_ATTACK);
        return BY_CLASS.getOrDefault(characterClass, List.of(BASIC_ATTACK));
    }

    /** The class's ultimate, or {@code null} if it somehow has none. */
    public static AbilityType ultimateFor(CharacterClass characterClass) {
        for (AbilityType ability : forClass(characterClass)) {
            if (ability.isUltimate()) return ability;
        }
        return null;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Spec builder
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Mutable builder used only inside this enum's constant list.
     *
     * <p>Exists so each constant reads as named parameters rather than a long
     * positional argument list — a reviewer can see that {@code .mana(22)} is
     * mana without counting commas. Defaults cover the common case: no owner
     * (universal), primary slot, free, melee range, single enemy target.</p>
     */
    private static final class Spec {
        private final String displayName;
        private final String description;
        private CharacterClass ownerClass;
        private AbilitySlot slot = AbilitySlot.PRIMARY;
        private int manaCost;
        private int baseDamage;
        private int baseHealing;
        private float range = 1.5f;
        private float areaRadius;
        private TargetType targetType = TargetType.ENEMY;
        private int cooldownTurns;
        private StatusEffectType appliedEffect;
        private int effectChancePercent;
        private StatBoost grantedBoost;
        private float pushDistance;
        private boolean restoresUltimate;

        private Spec(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        private Spec owner(CharacterClass ownerClass) { this.ownerClass = ownerClass; return this; }
        private Spec slot(AbilitySlot slot)           { this.slot = slot;             return this; }
        private Spec mana(int manaCost)               { this.manaCost = manaCost;     return this; }
        private Spec damage(int baseDamage)           { this.baseDamage = baseDamage; return this; }
        private Spec healing(int baseHealing)         { this.baseHealing = baseHealing; return this; }
        private Spec range(float range)               { this.range = range;           return this; }
        private Spec area(float areaRadius)           { this.areaRadius = areaRadius; return this; }
        private Spec target(TargetType targetType)    { this.targetType = targetType; return this; }
        private Spec cooldown(int turns)              { this.cooldownTurns = turns;   return this; }
        private Spec boost(StatBoost boost)           { this.grantedBoost = boost;    return this; }
        private Spec push(float distance)             { this.pushDistance = distance; return this; }
        private Spec restoresUltimate()               { this.restoresUltimate = true; return this; }

        private Spec effect(StatusEffectType effect, int chancePercent) {
            this.appliedEffect = effect;
            this.effectChancePercent = chancePercent;
            return this;
        }
    }
}
