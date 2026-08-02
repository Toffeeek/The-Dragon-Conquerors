package com.github.thedragonconquerors.assets;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.graphics.Texture;
import com.github.thedragonconquerors.entities.CharacterClass;

public enum SpriteAssets implements Asset<Texture> {

    WARRIOR (CharacterClass.WARRIOR),
    MAGE    (CharacterClass.MAGE),
    ARCHER  (CharacterClass.ARCHER),
    PALADIN (CharacterClass.PALADIN),
    ROGUE   (CharacterClass.ROGUE);

    // still used for static portrait in CharacterSelectScreen
    private final AssetDescriptor<Texture> descriptor;
    // walk sheet: 4 frames side-by-side in one 64x16 PNG
    private final AssetDescriptor<Texture> walkDescriptor;
    private final CharacterClass characterClass;

    SpriteAssets(CharacterClass characterClass) {
        this.characterClass = characterClass;
        this.descriptor     = new AssetDescriptor<>(characterClass.spritePath + ".png",      Texture.class);
        this.walkDescriptor = new AssetDescriptor<>(characterClass.spritePath + "_walk.png", Texture.class);
    }

    @Override
    public AssetDescriptor<Texture> getDescriptor() { return descriptor; }

    public AssetDescriptor<Texture> getWalkDescriptor() { return walkDescriptor; }

    public static SpriteAssets forClass(CharacterClass cls) {
        for (SpriteAssets e : values()) if (e.characterClass == cls) return e;
        return null;
    }

    public CharacterClass getCharacterClass() { return characterClass; }
}
