package com.github.thedragonconquerors.animation;

import com.badlogic.gdx.math.Vector2;
import com.github.thedragonconquerors.assets.SpriteAssets;
import com.github.thedragonconquerors.movement.MovementController;
import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.ability.AbilityType;

/** Per-character timing, non-looping reactions, and stable facing for side-view art. */
public class PlayerAnimationController {
    private final SpriteAssets sprites;
    private AnimationState state = AnimationState.IDLE;
    private boolean facingLeft, dead;
    private float stateTime;
    private AnimationState pendingReaction;
    private float reactionDelay;

    public PlayerAnimationController() { this(CharacterClass.PALADIN); }
    public PlayerAnimationController(CharacterClass type) { sprites = SpriteAssets.forClass(type); }

    public void update(float delta, Vector2 position, MovementController movement) {
        delta = Math.max(0, delta);
        if (pendingReaction != null) {
            reactionDelay -= delta;
            if (reactionDelay <= 0) {
                AnimationState reaction = pendingReaction;
                pendingReaction = null;
                if (reaction == AnimationState.DEATH) playDeath(); else setState(reaction);
                stateTime = Math.min(-reactionDelay, getClip().duration());
                return;
            }
        }
        stateTime += delta;
        if (dead || (!getClip().looping() && stateTime < getClip().duration())) return;
        AnimationState next = movement.isMoving() ? AnimationState.WALK : AnimationState.IDLE;
        if (movement.isMoving()) face(position, movement.getCurrentWaypoint());
        if (state != next) setState(next);
    }
    public void playAbility(Vector2 from, Vector2 target, AbilityType ability) {
        if (dead) return;
        face(from, target);
        setState(SpriteAssets.stateFor(ability));
    }
    public void playAttack(Vector2 from, Vector2 target, boolean cast) {
        if (dead) return;
        face(from, target);
        setState(cast ? AnimationState.CAST : AnimationState.ATTACK);
    }
    public void playHurt(Vector2 source, Vector2 position) {
        if (dead) return;
        face(position, source);
        setState(AnimationState.HURT);
    }
    public void queueReaction(boolean lethal, float delay) {
        if (dead) return;
        pendingReaction = lethal ? AnimationState.DEATH : AnimationState.HURT;
        reactionDelay = Math.max(0, delay);
    }
    public void playDeath() {
        if (dead) return;
        dead = true;
        pendingReaction = null;
        setState(AnimationState.DEATH);
    }
    public void revive() {
        dead = false;
        pendingReaction = null;
        setState(AnimationState.IDLE);
    }
    private void face(Vector2 from, Vector2 target) {
        if (from != null && target != null && Math.abs(target.x - from.x) > .001f) facingLeft = target.x < from.x;
    }
    private void setState(AnimationState value) { state = value; stateTime = 0; }
    public AnimationState getState() { return state; }
    public boolean isFacingLeft() { return facingLeft; }
    public SpriteAssets.Clip getClip() { return sprites.clip(state); }
    public int getCurrentFrame() { return getClip().frameAt(stateTime); }
    public float getStateTime() { return stateTime; }
    public boolean isBusy() { return pendingReaction != null || (!getClip().looping() && stateTime < getClip().duration()); }
}
