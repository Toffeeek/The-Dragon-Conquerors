package com.github.thedragonconquerors.assets;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.graphics.Texture;
import com.github.thedragonconquerors.animation.AnimationState;
import com.shared.shared.model.CharacterClass;

/**
 * Runtime sprite strips from the supplied character art pack.
 *
 * <p>Each file is a single horizontal strip made from 100x100 cells. The art is
 * authored facing right, so {@code PlayerRenderer} mirrors the selected frame
 * when a character faces left. Vertical movement keeps the right-facing art,
 * because this pack does not contain separate up/down strips.</p>
 */
public enum SpriteAssets implements Asset<Texture> {

    PALADIN_IDLE   (CharacterClass.PALADIN, AnimationState.IDLE,     "paladin/idle.png",    6),
    PALADIN_WALK   (CharacterClass.PALADIN, AnimationState.WALK,     "paladin/walk.png",    8),
    PALADIN_ATTACK1(CharacterClass.PALADIN, AnimationState.ATTACK_1, "paladin/attack1.png", 7),
    PALADIN_ATTACK2(CharacterClass.PALADIN, AnimationState.ATTACK_2, "paladin/attack2.png", 8),
    PALADIN_HURT   (CharacterClass.PALADIN, AnimationState.HURT,     "paladin/hurt.png",    4),
    PALADIN_DEATH  (CharacterClass.PALADIN, AnimationState.DEATH,    "paladin/death.png",   4),

    MAGE_IDLE      (CharacterClass.MAGE, AnimationState.IDLE,        "mage/idle.png",       6),
    MAGE_WALK      (CharacterClass.MAGE, AnimationState.WALK,        "mage/walk.png",       8),
    MAGE_ATTACK1   (CharacterClass.MAGE, AnimationState.ATTACK_1,    "mage/attack1.png",   15),
    MAGE_ATTACK2   (CharacterClass.MAGE, AnimationState.ATTACK_2,    "mage/attack2.png",   14),
    MAGE_HURT      (CharacterClass.MAGE, AnimationState.HURT,        "mage/hurt.png",       4),
    MAGE_DEATH     (CharacterClass.MAGE, AnimationState.DEATH,       "mage/death.png",      4),

    WRAITH_IDLE    (CharacterClass.WRAITH, AnimationState.IDLE,      "wraith/idle.png",     6),
    WRAITH_WALK    (CharacterClass.WRAITH, AnimationState.WALK,      "wraith/walk.png",     6),
    WRAITH_ATTACK1 (CharacterClass.WRAITH, AnimationState.ATTACK_1,  "wraith/attack1.png",  9),
    WRAITH_ATTACK2 (CharacterClass.WRAITH, AnimationState.ATTACK_2,  "wraith/attack2.png", 10),
    WRAITH_HURT    (CharacterClass.WRAITH, AnimationState.HURT,      "wraith/hurt.png",     4),
    WRAITH_DEATH   (CharacterClass.WRAITH, AnimationState.DEATH,     "wraith/death.png",    9),

    CLERIC_IDLE    (CharacterClass.CLERIC, AnimationState.IDLE,      "cleric/idle.png",     6),
    CLERIC_WALK    (CharacterClass.CLERIC, AnimationState.WALK,      "cleric/walk.png",     8),
    CLERIC_ATTACK1 (CharacterClass.CLERIC, AnimationState.ATTACK_1,  "cleric/attack1.png",  9),
    CLERIC_ATTACK2 (CharacterClass.CLERIC, AnimationState.ATTACK_2,  "cleric/attack2.png",  6),
    CLERIC_HURT    (CharacterClass.CLERIC, AnimationState.HURT,      "cleric/hurt.png",     4),
    CLERIC_DEATH   (CharacterClass.CLERIC, AnimationState.DEATH,     "cleric/death.png",    4),

    ARCHER_IDLE    (CharacterClass.ARCHER, AnimationState.IDLE,      "archer/idle.png",     6),
    ARCHER_WALK    (CharacterClass.ARCHER, AnimationState.WALK,      "archer/walk.png",     8),
    ARCHER_ATTACK1 (CharacterClass.ARCHER, AnimationState.ATTACK_1,  "archer/attack1.png",  9),
    ARCHER_ATTACK2 (CharacterClass.ARCHER, AnimationState.ATTACK_2,  "archer/attack2.png", 12),
    ARCHER_HURT    (CharacterClass.ARCHER, AnimationState.HURT,      "archer/hurt.png",     4),
    ARCHER_DEATH   (CharacterClass.ARCHER, AnimationState.DEATH,     "archer/death.png",    4),

    SOLDIER_IDLE   (CharacterClass.SOLDIER, AnimationState.IDLE,     "soldier/idle.png",    6),
    SOLDIER_WALK   (CharacterClass.SOLDIER, AnimationState.WALK,     "soldier/walk.png",    8),
    SOLDIER_ATTACK1(CharacterClass.SOLDIER, AnimationState.ATTACK_1, "soldier/attack1.png", 6),
    SOLDIER_ATTACK2(CharacterClass.SOLDIER, AnimationState.ATTACK_2, "soldier/attack2.png", 6),
    SOLDIER_HURT   (CharacterClass.SOLDIER, AnimationState.HURT,     "soldier/hurt.png",    4),
    SOLDIER_DEATH  (CharacterClass.SOLDIER, AnimationState.DEATH,    "soldier/death.png",   4);

    public static final int FRAME_WIDTH = 100;
    public static final int FRAME_HEIGHT = 100;

    private final CharacterClass characterClass;
    private final AnimationState animationState;
    private final int frameCount;
    private final AssetDescriptor<Texture> descriptor;

    SpriteAssets(CharacterClass characterClass, AnimationState animationState,
                 String relativePath, int frameCount) {
        this.characterClass = characterClass;
        this.animationState = animationState;
        this.frameCount = frameCount;
        this.descriptor = new AssetDescriptor<>(
            "characters/sprites/" + relativePath, Texture.class);
    }

    @Override
    public AssetDescriptor<Texture> getDescriptor() { return descriptor; }

    public CharacterClass getCharacterClass() { return characterClass; }
    public AnimationState getAnimationState() { return animationState; }
    public int getFrameCount() { return frameCount; }

    /** Finds the strip used by a class for the requested animation state. */
    public static SpriteAssets forAnimation(CharacterClass characterClass,
                                            AnimationState animationState) {
        CharacterClass safeClass = characterClass == null ? CharacterClass.PALADIN : characterClass;
        AnimationState safeState = animationState == null ? AnimationState.IDLE : animationState;
        for (SpriteAssets asset : values()) {
            if (asset.characterClass == safeClass && asset.animationState == safeState) return asset;
        }
        return PALADIN_IDLE;
    }

    public static int frameCountFor(CharacterClass characterClass, AnimationState animationState) {
        return forAnimation(characterClass, animationState).frameCount;
    }
}
