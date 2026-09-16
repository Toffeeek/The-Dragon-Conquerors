package com.github.thedragonconquerors.assets;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.graphics.Texture;
import com.github.thedragonconquerors.animation.AnimationState;
import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.ability.AbilityType;
import java.util.EnumMap;
import java.util.Collection;

/** Original 100-pixel strips, with their actual (not padded) frame counts. */
public enum SpriteAssets {
    PALADIN(CharacterClass.PALADIN, "Knight", true, 8, 4, "Attack01", 7, "Attack03", 11, "Attack02", 10),
    WRAITH(CharacterClass.WRAITH, "Necromancer", false, 6, 9, "Attack01", 9, "Attack02", 10, "Summon", 10),
    ARCHER(CharacterClass.ARCHER, "Archer", true, 8, 4, "Attack01", 9, "Idle", 6, "Attack02", 12),
    CLERIC(CharacterClass.CLERIC, "Priest", true, 8, 4, "Attack", 9, "Heal", 6, "Heal", 6),
    MAGE(CharacterClass.MAGE, "Wizard", true, 8, 4, "Attack01", 6, "Attack02", 9, "Attack02", 9),
    BARD(CharacterClass.BARD, "Orc", true, 8, 4, "Attack01", 6, "Attack02", 6, "Attack02", 6);

    public static final int FRAME_SIZE = 100;
    public record Clip(String path, int frames, float frameDuration, boolean looping) implements Asset<Texture> {
        @Override public AssetDescriptor<Texture> getDescriptor() { return new AssetDescriptor<>(path, Texture.class); }
        public float duration() { return frames * frameDuration; }
        public int frameAt(float time) {
            int frame = Math.max(0, (int)(time / frameDuration));
            return looping ? frame % frames : Math.min(frame, frames - 1);
        }
    }
    private final CharacterClass characterClass;
    public final String character;
    public final boolean hasShadow;
    private final EnumMap<AnimationState, Clip> clips = new EnumMap<>(AnimationState.class);

    SpriteAssets(CharacterClass type, String character, boolean shadow, int walking, int death,
                 String attack, int attacks, String cast, int casts, String special, int specials) {
        this.characterClass = type;
        this.character = character;
        this.hasShadow = shadow;
        add(AnimationState.IDLE, "Idle", 6, .15f, true);
        add(AnimationState.WALK, "Walk", walking, .13f, true);
        add(AnimationState.ATTACK, attack, attacks, .085f, false);
        add(AnimationState.CAST, cast, casts, .10f, false);
        add(AnimationState.SPECIAL, special, specials, .09f, false);
        add(AnimationState.HURT, "Hurt", 4, .09f, false);
        add(AnimationState.DEATH, character.equals("Necromancer") ? "DEATH" : "Death", death, .12f, false);
    }
    private void add(AnimationState state, String suffix, int frames, float seconds, boolean loop) {
        clips.put(state, new Clip("characters/animated/" + character + "/" + character + "_" + suffix + ".png", frames, seconds, loop));
    }
    public Clip clip(AnimationState state) { return clips.get(state); }
    public Collection<Clip> clips() { return clips.values(); }
    public CharacterClass getCharacterClass() { return characterClass; }
    public static SpriteAssets forClass(CharacterClass type) {
        for (SpriteAssets assets : values()) if (assets.characterClass == type) return assets;
        return PALADIN;
    }
    /** Animation choice only: no ability costs, ranges, effects or cooldowns change. */
    public static AnimationState stateFor(AbilityType ability) {
        if (ability == null) return AnimationState.ATTACK;
        return switch (ability) {
            case DIVINE_SMITE, TELEPORT, REVIVE, ENCORE, RAIN_OF_ARROWS, ELDRITCH_BLAST -> AnimationState.SPECIAL;
            case PALADIN_RANGED, FIREBALL, ICE_ATTACK, CURSE, POISON_JAB, HEAL, STAT_BOOST, ACCURACY_BOOST -> AnimationState.CAST;
            default -> AnimationState.ATTACK;
        };
    }
}
