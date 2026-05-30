package com.github.thedragonconquerors.entities;

import com.github.thedragonconquerors.stats.StatComponent;

/**
 * Defines every playable character class in the game.
 *
 * Each entry bundles:
 *   - a human-readable display name
 *   - a sprite asset path (relative to the assets root, no extension)
 *     that PlayerRenderer will look up via SpriteAssets
 *   - base StatComponent values that represent the class's starting stats
 *
 * To add a new class:
 *   1. Add an entry here with its display name, sprite path, and stats.
 *   2. Drop the matching sprite sheet (PNG) into assets/sprites/<name>.png
 *   3. Add a matching entry to SpriteAssets if you want it pre-loaded.
 */
public enum CharacterClass {

    // ── display name ──── sprite path key ──────── maxHp  maxMana  acc  str  spd  ins  wis
    WARRIOR ("Warrior",  "sprites/warrior",          120,    30,     10,  16,   8,   8,   7),
    MAGE    ("Mage",     "sprites/mage",              70,   100,     12,   6,   9,   9,  18),
    ARCHER  ("Archer",   "sprites/archer",            90,    50,     17,  11,  14,  10,   9),
    PALADIN ("Paladin",  "sprites/paladin",          110,    70,      9,  13,   8,  15,  13),
    ROGUE   ("Rogue",    "sprites/rogue",             80,    40,     15,  12,  17,  11,   8);

    // ──────────────────────────────────────────────────────────────
    //  Fields
    // ──────────────────────────────────────────────────────────────

    /** Human-readable label shown in menus / HUD. */
    public final String displayName;

    /**
     * Path to the sprite PNG relative to the assets root, without extension.
     * E.g. "sprites/warrior" → assets/sprites/warrior.png
     */
    public final String spritePath;

    // Raw base stats stored so we can construct a StatComponent on demand.
    private final int maxHp;
    private final int maxMana;
    private final int accuracy;
    private final int strength;
    private final int speed;
    private final int inspiration;
    private final int wisdom;

    // ──────────────────────────────────────────────────────────────
    //  Constructor
    // ──────────────────────────────────────────────────────────────

    CharacterClass(
        String displayName,
        String spritePath,
        int maxHp, int maxMana,
        int accuracy, int strength, int speed,
        int inspiration, int wisdom
    ) {
        this.displayName  = displayName;
        this.spritePath   = spritePath;
        this.maxHp        = maxHp;
        this.maxMana      = maxMana;
        this.accuracy     = accuracy;
        this.strength     = strength;
        this.speed        = speed;
        this.inspiration  = inspiration;
        this.wisdom       = wisdom;
    }

    // ──────────────────────────────────────────────────────────────
    //  Factory
    // ──────────────────────────────────────────────────────────────

    /**
     * Returns a fresh StatComponent pre-filled with this class's base stats.
     * Call this when constructing a new Player so their stats match their class.
     */
    public StatComponent createBaseStats() {
        return new StatComponent(maxHp, maxMana, accuracy, strength, speed, inspiration, wisdom);
    }
}
