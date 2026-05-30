package com.github.thedragonconquerors.assets;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.graphics.Texture;
import com.github.thedragonconquerors.entities.CharacterClass;

/**
 * Enumeration of every character-sprite asset in the game.
 *
 * Follows the same Asset<T> contract used by MapAssets so that
 * AssetService.load() / queue() / get() all work identically.
 *
 * Each entry corresponds to one CharacterClass; the PNG is expected at
 *   assets/sprites/<name>.png
 * inside the application's asset root.
 *
 * Usage:
 * <pre>
 *   // Pre-load all sprites at startup
 *   for (SpriteAssets s : SpriteAssets.values()) assetService.queue(s);
 *
 *   // Later, look up by class
 *   Texture tex = assetService.get(SpriteAssets.forClass(player.getCharacterClass()));
 * </pre>
 */
public enum SpriteAssets implements Asset<Texture> {

    WARRIOR (CharacterClass.WARRIOR),
    MAGE    (CharacterClass.MAGE),
    ARCHER  (CharacterClass.ARCHER),
    PALADIN (CharacterClass.PALADIN),
    ROGUE   (CharacterClass.ROGUE);

    // ──────────────────────────────────────────────────────────────
    //  Fields
    // ──────────────────────────────────────────────────────────────

    private final AssetDescriptor<Texture> descriptor;
    private final CharacterClass characterClass;

    // ──────────────────────────────────────────────────────────────
    //  Constructor
    // ──────────────────────────────────────────────────────────────

    SpriteAssets(CharacterClass characterClass) {
        this.characterClass = characterClass;
        // e.g. "sprites/warrior.png"
        this.descriptor = new AssetDescriptor<>(characterClass.spritePath + ".png", Texture.class);
    }

    // ──────────────────────────────────────────────────────────────
    //  Asset<Texture> contract
    // ──────────────────────────────────────────────────────────────

    @Override
    public AssetDescriptor<Texture> getDescriptor() {
        return descriptor;
    }

    // ──────────────────────────────────────────────────────────────
    //  Helpers
    // ──────────────────────────────────────────────────────────────

    /**
     * Returns the SpriteAssets entry that matches the given CharacterClass,
     * or null if no sprite has been registered for that class yet.
     */
    public static SpriteAssets forClass(CharacterClass characterClass) {
        for (SpriteAssets entry : values()) {
            if (entry.characterClass == characterClass) {
                return entry;
            }
        }
        return null;   // graceful: renderer falls back to shape-based drawing
    }

    public CharacterClass getCharacterClass() {
        return characterClass;
    }
}
