package com.github.thedragonconquerors.combat;

import com.shared.shared.model.CharacterClass;

/**
 * Every action a player can perform during their turn.
 *
 * Fields per action:
 *   displayName  — shown on the HUD action bar
 *   manaCost     — mana spent on use (0 = free)
 *   baseDamage   — raw damage before stat scaling
 *   range        — max world-unit distance to target
 *   targetsSelf  — true for heals/buffs (no enemy needed)
 *   animation    — sprite-sheet animation triggered by the action
 *   description  — tooltip line shown on the HUD
 *
 * Adding a new action:
 *   1. Add an entry here
 *   2. Add it to the availableFor() list for the relevant class(es)
 *   3. ActionSystem.execute() will handle the rest
 */
public enum ActionType {

    // ── universal ──────────────────────────────────────────────────────────
    //                     name            mana  dmg  range  self   description
    BASIC_ATTACK       ("Basic Attack",     0,   10,  1.5f, false, ActionAnimation.ATTACK, "Strike a nearby enemy"),
    DEFEND             ("Defend",           0,    0,  0f,   true,  ActionAnimation.CAST, "Brace — reduce incoming damage this turn"),

    // ── Warrior ───────────────────────────────────────────────────────────
    SHIELD_BASH        ("Shield Bash",      8,   14,  1.5f, false, ActionAnimation.ATTACK, "Bash enemy — deals bonus damage"),
    WAR_CRY            ("War Cry",         12,    0,  0f,   true,  ActionAnimation.CAST, "Boost strength for 1 turn"),
    CLEAVE             ("Cleave",          15,   20,  2.0f, false, ActionAnimation.ATTACK, "Wide swing hitting all nearby enemies"),

    // ── Mage ──────────────────────────────────────────────────────────────
    FIREBALL           ("Fireball",        20,   30,  5.0f, false, ActionAnimation.CAST, "Launch a fireball at a distant enemy"),
    ICE_SHARD          ("Ice Shard",       12,   18,  4.0f, false, ActionAnimation.CAST, "Slow and damage a target"),
    ARCANE_SHIELD      ("Arcane Shield",   15,    0,  0f,   true,  ActionAnimation.CAST, "Absorb the next hit with a mana barrier"),

    // ── Archer ────────────────────────────────────────────────────────────
    ARROW_SHOT         ("Arrow Shot",       5,   15,  6.0f, false, ActionAnimation.ATTACK, "Fire an arrow at a distant enemy"),
    POISON_ARROW       ("Poison Arrow",    10,   10,  5.0f, false, ActionAnimation.ATTACK, "Arrow that poisons target over time"),
    EVASIVE_ROLL       ("Evasive Roll",     8,    0,  0f,   true,  ActionAnimation.CAST, "Greatly boost evasion for 1 turn"),

    // ── Paladin ───────────────────────────────────────────────────────────
    HOLY_STRIKE        ("Holy Strike",     10,   18,  1.5f, false, ActionAnimation.ATTACK, "Blessed strike — bonus vs undead"),
    LAY_ON_HANDS       ("Lay on Hands",   20,    0,  0f,   true,  ActionAnimation.CAST, "Restore 30 HP to yourself"),
    DIVINE_SHIELD      ("Divine Shield",  25,    0,  0f,   true,  ActionAnimation.CAST, "Become immune to damage for 1 turn"),

    // ── Rogue ─────────────────────────────────────────────────────────────
    BACKSTAB           ("Backstab",        10,   25,  1.5f, false, ActionAnimation.ATTACK, "High damage if attacking from behind"),
    SMOKE_BOMB         ("Smoke Bomb",      12,    0,  0f,   true,  ActionAnimation.CAST, "Become untargetable for 1 turn"),
    DUAL_SLASH         ("Dual Slash",      15,   18,  1.5f, false, ActionAnimation.ATTACK, "Two quick strikes in succession");

    // ──────────────────────────────────────────────────────────────────────
    //  Fields
    // ──────────────────────────────────────────────────────────────────────

    public final String  displayName;
    public final int     manaCost;
    public final int     baseDamage;
    public final float   range;
    public final boolean targetsSelf;
    public final ActionAnimation animation;
    public final String  description;

    ActionType(String displayName, int manaCost, int baseDamage,
               float range, boolean targetsSelf, ActionAnimation animation, String description) {
        this.displayName  = displayName;
        this.manaCost     = manaCost;
        this.baseDamage   = baseDamage;
        this.range        = range;
        this.targetsSelf  = targetsSelf;
        this.animation    = animation;
        this.description  = description;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Class → action mapping
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Returns the ordered list of actions available to a given class.
     * Slot 0 = key [1], slot 1 = key [2], etc.
     * Always starts with BASIC_ATTACK so every class has a free attack.
     *
     * <p><b>PROVISIONAL MAPPING.</b> This enum predates the design document's
     * roster and its actions do not match it: there is no Curse, Poison Jab,
     * Teleport, Heal, Revive, Inspiring Verse, Encore or Rain of Arrows here,
     * and nothing in this package applies status effects, cooldowns or area
     * damage. The authoritative catalogue is
     * {@code com.shared.shared.model.ability.AbilityType}, which already
     * describes all twenty design abilities as data.</p>
     *
     * <p>The mapping below exists only so the client keeps a working action bar
     * until {@code CombatResolver} lands and this package is retired. Wraith
     * inherits the old Rogue kit, Cleric the old self-heal, Bard the old buff —
     * placeholders, not balance decisions. Do not tune these numbers; tune
     * {@code AbilityType} instead.</p>
     */
    public static ActionType[] availableFor(CharacterClass cls) {
        if (cls == null) return new ActionType[]{ BASIC_ATTACK, DEFEND };
        switch (cls) {
            case PALADIN: return new ActionType[]{ BASIC_ATTACK, HOLY_STRIKE, LAY_ON_HANDS,  DIVINE_SHIELD};
            case MAGE:    return new ActionType[]{ BASIC_ATTACK, FIREBALL,    ICE_SHARD,     ARCANE_SHIELD};
            case ARCHER:  return new ActionType[]{ BASIC_ATTACK, ARROW_SHOT,  POISON_ARROW,  EVASIVE_ROLL };
            // Placeholder kits — see the note above.
            case WRAITH:  return new ActionType[]{ BASIC_ATTACK, BACKSTAB,    DUAL_SLASH,    SMOKE_BOMB   };
            case CLERIC:  return new ActionType[]{ BASIC_ATTACK, SHIELD_BASH, LAY_ON_HANDS,  DEFEND       };
            case BARD:    return new ActionType[]{ BASIC_ATTACK, WAR_CRY,     ARCANE_SHIELD, DEFEND       };
            default:      return new ActionType[]{ BASIC_ATTACK, DEFEND };
        }
    }
}
