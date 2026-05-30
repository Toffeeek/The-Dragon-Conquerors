package com.github.thedragonconquerors.combat;

import com.github.thedragonconquerors.entities.CharacterClass;

/**
 * Every action a player can perform during their turn.
 *
 * Fields per action:
 *   displayName  — shown on the HUD action bar
 *   manaCost     — mana spent on use (0 = free)
 *   baseDamage   — raw damage before stat scaling
 *   range        — max world-unit distance to target
 *   targetsSelf  — true for heals/buffs (no enemy needed)
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
    BASIC_ATTACK       ("Basic Attack",     0,   10,  1.5f, false, "Strike a nearby enemy"),
    DEFEND             ("Defend",           0,    0,  0f,   true,  "Brace — reduce incoming damage this turn"),

    // ── Warrior ───────────────────────────────────────────────────────────
    SHIELD_BASH        ("Shield Bash",      8,   14,  1.5f, false, "Bash enemy — deals bonus damage"),
    WAR_CRY            ("War Cry",         12,    0,  0f,   true,  "Boost strength for 1 turn"),
    CLEAVE             ("Cleave",          15,   20,  2.0f, false, "Wide swing hitting all nearby enemies"),

    // ── Mage ──────────────────────────────────────────────────────────────
    FIREBALL           ("Fireball",        20,   30,  5.0f, false, "Launch a fireball at a distant enemy"),
    ICE_SHARD          ("Ice Shard",       12,   18,  4.0f, false, "Slow and damage a target"),
    ARCANE_SHIELD      ("Arcane Shield",   15,    0,  0f,   true,  "Absorb the next hit with a mana barrier"),

    // ── Archer ────────────────────────────────────────────────────────────
    ARROW_SHOT         ("Arrow Shot",       5,   15,  6.0f, false, "Fire an arrow at a distant enemy"),
    POISON_ARROW       ("Poison Arrow",    10,   10,  5.0f, false, "Arrow that poisons target over time"),
    EVASIVE_ROLL       ("Evasive Roll",     8,    0,  0f,   true,  "Greatly boost evasion for 1 turn"),

    // ── Paladin ───────────────────────────────────────────────────────────
    HOLY_STRIKE        ("Holy Strike",     10,   18,  1.5f, false, "Blessed strike — bonus vs undead"),
    LAY_ON_HANDS       ("Lay on Hands",   20,    0,  0f,   true,  "Restore 30 HP to yourself"),
    DIVINE_SHIELD      ("Divine Shield",  25,    0,  0f,   true,  "Become immune to damage for 1 turn"),

    // ── Rogue ─────────────────────────────────────────────────────────────
    BACKSTAB           ("Backstab",        10,   25,  1.5f, false, "High damage if attacking from behind"),
    SMOKE_BOMB         ("Smoke Bomb",      12,    0,  0f,   true,  "Become untargetable for 1 turn"),
    DUAL_SLASH         ("Dual Slash",      15,   18,  1.5f, false, "Two quick strikes in succession");

    // ──────────────────────────────────────────────────────────────────────
    //  Fields
    // ──────────────────────────────────────────────────────────────────────

    public final String  displayName;
    public final int     manaCost;
    public final int     baseDamage;
    public final float   range;
    public final boolean targetsSelf;
    public final String  description;

    ActionType(String displayName, int manaCost, int baseDamage,
               float range, boolean targetsSelf, String description) {
        this.displayName  = displayName;
        this.manaCost     = manaCost;
        this.baseDamage   = baseDamage;
        this.range        = range;
        this.targetsSelf  = targetsSelf;
        this.description  = description;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Class → action mapping
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Returns the ordered list of actions available to a given class.
     * Slot 0 = key [1], slot 1 = key [2], etc.
     * Always starts with BASIC_ATTACK so every class has a free attack.
     */
    public static ActionType[] availableFor(CharacterClass cls) {
        switch (cls) {
            case WARRIOR: return new ActionType[]{ BASIC_ATTACK, SHIELD_BASH, WAR_CRY,       CLEAVE       };
            case MAGE:    return new ActionType[]{ BASIC_ATTACK, FIREBALL,    ICE_SHARD,     ARCANE_SHIELD};
            case ARCHER:  return new ActionType[]{ BASIC_ATTACK, ARROW_SHOT,  POISON_ARROW,  EVASIVE_ROLL };
            case PALADIN: return new ActionType[]{ BASIC_ATTACK, HOLY_STRIKE, LAY_ON_HANDS,  DIVINE_SHIELD};
            case ROGUE:   return new ActionType[]{ BASIC_ATTACK, BACKSTAB,    DUAL_SLASH,    SMOKE_BOMB   };
            default:      return new ActionType[]{ BASIC_ATTACK, DEFEND };
        }
    }
}
