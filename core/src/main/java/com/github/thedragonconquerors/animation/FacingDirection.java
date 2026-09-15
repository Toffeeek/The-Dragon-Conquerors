package com.github.thedragonconquerors.animation;

import com.badlogic.gdx.math.Vector2;

/** Movement/combat facing. The supplied side-view art is mirrored for LEFT. */
public enum FacingDirection {
    DOWN,
    LEFT,
    RIGHT,
    UP;

    public static FacingDirection fromVector(Vector2 direction, FacingDirection fallback) {
        if (direction == null || direction.isZero(0.001f)) return fallback;

        if (Math.abs(direction.x) > Math.abs(direction.y)) {
            return direction.x < 0f ? LEFT : RIGHT;
        }
        return direction.y < 0f ? DOWN : UP;
    }
}
