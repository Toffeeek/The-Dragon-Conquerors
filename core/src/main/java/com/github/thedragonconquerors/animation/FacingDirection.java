package com.github.thedragonconquerors.animation;

import com.badlogic.gdx.math.Vector2;

/** Direction order matches the four rows used inside each animation group. */
public enum FacingDirection {
    DOWN(0),
    LEFT(1),
    RIGHT(2),
    UP(3);

    private final int rowOffset;

    FacingDirection(int rowOffset) {
        this.rowOffset = rowOffset;
    }

    public int getRowOffset() {
        return rowOffset;
    }

    public static FacingDirection fromVector(Vector2 direction, FacingDirection fallback) {
        if (direction == null || direction.isZero(0.001f)) return fallback;

        if (Math.abs(direction.x) > Math.abs(direction.y)) {
            return direction.x < 0f ? LEFT : RIGHT;
        }
        return direction.y < 0f ? DOWN : UP;
    }
}
