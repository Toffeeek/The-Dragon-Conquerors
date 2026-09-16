package com.github.thedragonconquerors.assets;

/** Standalone effects from the supplied pack; actor strips do not duplicate these effects. */
public enum EffectAssets {
    ARCANE("Wizard_Attack01_Effect.png", 10),
    FLAME("Wizard_Attack02_Effect.png", 7),
    SHADOW("Necromancer_Attack02_Effect.png", 6),
    PORTAL("Necromancer_Sumon_Effect.png", 7),
    RADIANT("Priest_Attack_effect.png", 5),
    HEAL("Priest_Heal_effect.png", 4),
    ARROW("Arrow02(100x100).png", 1);
    public final SpriteAssets.Clip clip;
    EffectAssets(String name, int frames) {
        clip = new SpriteAssets.Clip("characters/animated/effects/" + name, frames, .085f, false);
    }
}
