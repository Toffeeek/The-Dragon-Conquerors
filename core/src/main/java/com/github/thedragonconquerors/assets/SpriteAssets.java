package com.github.thedragonconquerors.assets;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.graphics.Texture;
import com.shared.shared.model.CharacterClass;

/**
 * One 96x96-frame high-detail sprite sheet per playable character class.
 *
 * <p>Only five HD sheets have been authored (Warrior, Mage, Archer, Paladin,
 * Rogue) but the design document calls for six classes, so Wraith, Cleric and
 * Bard borrow the closest existing sheet as a <b>placeholder</b>. Every sheet
 * shares the identical 24-row by 6-column layout documented in
 * {@code docs/SPRITE_SHEETS.md}, so reuse is safe — the animation controller
 * cannot tell the difference.</p>
 *
 * <p>When real art lands, drop the file in {@code assets/characters/} and change
 * only the file name on the relevant constant.</p>
 */
public enum SpriteAssets implements Asset<Texture> {

    // ── Classes with their own dedicated sheet ────────────────────────────
    PALADIN(CharacterClass.PALADIN, "Paladin_HD.png"),
    MAGE   (CharacterClass.MAGE,    "Mage_HD.png"),
    ARCHER (CharacterClass.ARCHER,  "Archer_HD.png"),

    // ── Placeholders: awaiting dedicated art ──────────────────────────────
    /** Placeholder: the Rogue sheet stands in for the shadow-assassin silhouette. */
    WRAITH(CharacterClass.WRAITH, "Rogue_HD.png"),
    /** Placeholder: the plate-armoured Warrior sheet reads acceptably as a war cleric. */
    CLERIC(CharacterClass.CLERIC, "Warrior_HD.png"),
    /** Placeholder: the Mage sheet supplies a robed caster silhouette. */
    BARD  (CharacterClass.BARD,   "Mage_HD.png");

    private final CharacterClass characterClass;
    private final AssetDescriptor<Texture> descriptor;

    SpriteAssets(CharacterClass characterClass, String fileName) {
        this.characterClass = characterClass;
        this.descriptor = new AssetDescriptor<>("characters/" + fileName, Texture.class);
    }

    @Override
    public AssetDescriptor<Texture> getDescriptor() {
        return descriptor;
    }

    public CharacterClass getCharacterClass() {
        return characterClass;
    }

    /**
     * Sheet for a class, falling back to {@link #PALADIN} for {@code null} or an
     * unmapped class so rendering degrades to a visible sprite rather than a
     * crash mid-match.
     */
    public static SpriteAssets forClass(CharacterClass characterClass) {
        for (SpriteAssets asset : values()) {
            if (asset.characterClass == characterClass) return asset;
        }
        return PALADIN;
    }
}
