package com.github.thedragonconquerors.assets;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.graphics.Texture;
import com.shared.shared.model.CharacterClass;

/** One 96x96-frame high-detail sprite sheet per playable character class. */
public enum SpriteAssets implements Asset<Texture> {
    WARRIOR(CharacterClass.WARRIOR, "Warrior_HD.png"),
    MAGE(CharacterClass.MAGE, "Mage_HD.png"),
    ARCHER(CharacterClass.ARCHER, "Archer_HD.png"),
    PALADIN(CharacterClass.PALADIN, "Paladin_HD.png"),
    ROGUE(CharacterClass.ROGUE, "Rogue_HD.png");

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

    public static SpriteAssets forClass(CharacterClass characterClass) {
        for (SpriteAssets asset : values()) {
            if (asset.characterClass == characterClass) return asset;
        }
        return WARRIOR;
    }
}
