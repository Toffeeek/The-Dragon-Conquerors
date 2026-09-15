package com.github.thedragonconquerors.animation;

/** Animation states backed by separate horizontal sprite strips. */
public enum AnimationState {
    IDLE(0.18f, true),
    WALK(0.10f, true),
    ATTACK_1(0.085f, false),
    ATTACK_2(0.09f, false),
    HURT(0.10f, false),
    DEATH(0.12f, false);

    private final float frameDuration;
    private final boolean looping;

    AnimationState(float frameDuration, boolean looping) {
        this.frameDuration = frameDuration;
        this.looping = looping;
    }

    public float getFrameDuration() { return frameDuration; }
    public boolean isLooping() { return looping; }
}
