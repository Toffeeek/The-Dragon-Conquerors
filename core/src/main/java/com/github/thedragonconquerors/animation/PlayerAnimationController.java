package com.github.thedragonconquerors.animation;

import com.badlogic.gdx.math.Vector2;
import com.github.thedragonconquerors.assets.SpriteAssets;
import com.github.thedragonconquerors.movement.MovementController;
import com.shared.shared.model.CharacterClass;

/** Keeps animation state independent from rendering and combat code. */
public class PlayerAnimationController {
    private final CharacterClass characterClass;
    private AnimationState state = AnimationState.IDLE;
    private FacingDirection facing = FacingDirection.RIGHT;
    private float stateTime = 0f;
    private boolean permanentlyDead = false;

    public PlayerAnimationController(CharacterClass characterClass) {
        this.characterClass = characterClass == null ? CharacterClass.PALADIN : characterClass;
    }

    public void update(float delta, Vector2 currentPosition, MovementController movementController) {
        if (permanentlyDead) {
            stateTime += delta;
            return;
        }

        if (!state.isLooping()) {
            stateTime += delta;
            float duration = state.getFrameDuration() * frameCount();
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

    /**
     * Plays Attack 1 for free/basic actions and Attack 2 for mana/skill actions.
     * The boolean name is retained for call-site compatibility with the existing
     * combat flow, where {@code true} previously selected the cast row.
     */
    public void playAttack(Vector2 from, Vector2 target, boolean secondAttack) {
        if (permanentlyDead) return;
        if (from != null && target != null) {
            facing = FacingDirection.fromVector(new Vector2(target).sub(from), facing);
        }
        state = secondAttack ? AnimationState.ATTACK_2 : AnimationState.ATTACK_1;
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

    public AnimationState getState() { return state; }
    public FacingDirection getFacing() { return facing; }

    public int getCurrentFrame() {
        int count = frameCount();
        int frame = (int) (stateTime / state.getFrameDuration());
        if (state.isLooping()) return frame % count;
        return Math.min(frame, count - 1);
    }

    private int frameCount() {
        return Math.max(1, SpriteAssets.frameCountFor(characterClass, state));
    }
}
