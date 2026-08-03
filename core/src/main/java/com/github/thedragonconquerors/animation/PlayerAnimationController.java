package com.github.thedragonconquerors.animation;

import com.badlogic.gdx.math.Vector2;
import com.github.thedragonconquerors.movement.MovementController;

/** Keeps animation state independent from rendering and combat code. */
public class PlayerAnimationController {
    public static final int FRAMES_PER_ROW = 6;

    private AnimationState state = AnimationState.IDLE;
    private FacingDirection facing = FacingDirection.DOWN;
    private float stateTime = 0f;
    private boolean permanentlyDead = false;

    public void update(float delta, Vector2 currentPosition, MovementController movementController) {
        if (permanentlyDead) {
            stateTime += delta;
            return;
        }

        if (!state.isLooping()) {
            stateTime += delta;
            float duration = state.getFrameDuration() * FRAMES_PER_ROW;
            if (stateTime < duration) return;
            state = movementController.isMoving() ? AnimationState.WALK : AnimationState.IDLE;
            stateTime = 0f;
        }

        if (movementController.isMoving()) {
            Vector2 waypoint = movementController.getCurrentWaypoint();
            if (waypoint != null) {
                facing = FacingDirection.fromVector(
                    new Vector2(waypoint).sub(currentPosition), facing);
            }
            if (state != AnimationState.WALK) {
                state = AnimationState.WALK;
                stateTime = 0f;
            } else {
                stateTime += delta;
            }
        } else {
            if (state != AnimationState.IDLE) {
                state = AnimationState.IDLE;
                stateTime = 0f;
            } else {
                stateTime += delta;
            }
        }
    }

    public void playAttack(Vector2 from, Vector2 target, boolean castAnimation) {
        if (permanentlyDead) return;
        if (from != null && target != null) {
            facing = FacingDirection.fromVector(new Vector2(target).sub(from), facing);
        }
        state = castAnimation ? AnimationState.CAST : AnimationState.ATTACK;
        stateTime = 0f;
    }

    public void playHurt(Vector2 source, Vector2 currentPosition) {
        if (permanentlyDead) return;
        if (source != null && currentPosition != null) {
            facing = FacingDirection.fromVector(new Vector2(source).sub(currentPosition), facing);
        }
        state = AnimationState.HURT;
        stateTime = 0f;
    }

    public void playDeath() {
        permanentlyDead = true;
        state = AnimationState.DEATH;
        stateTime = 0f;
    }

    public AnimationState getState() {
        return state;
    }

    public FacingDirection getFacing() {
        return facing;
    }

    public int getCurrentFrame() {
        int frame = (int) (stateTime / state.getFrameDuration());
        if (state.isLooping()) return frame % FRAMES_PER_ROW;
        return Math.min(frame, FRAMES_PER_ROW - 1);
    }

    public int getCurrentRow() {
        return state.rowFor(facing);
    }
}
