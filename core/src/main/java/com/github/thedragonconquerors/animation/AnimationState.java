package com.github.thedragonconquerors.animation;

/**
 * Each state owns four sprite-sheet rows in DOWN, LEFT, RIGHT, UP order.
 * Every row contains six 96x96 high-detail frames.
 */
public enum AnimationState {
    IDLE(0, 0.18f, true),
    WALK(4, 0.10f, true),
    ATTACK(8, 0.075f, false),
    CAST(12, 0.09f, false),
    HURT(16, 0.10f, false),
    DEATH(20, 0.12f, false);

    private final int firstRow;
    private final float frameDuration;
    private final boolean looping;

    AnimationState(int firstRow, float frameDuration, boolean looping) {
        this.firstRow = firstRow;
        this.frameDuration = frameDuration;
        this.looping = looping;
    }

    public int rowFor(FacingDirection direction) {
        return firstRow + direction.getRowOffset();
    }

    public float getFrameDuration() {
        return frameDuration;
    }

    public boolean isLooping() {
        return looping;
    }
}
