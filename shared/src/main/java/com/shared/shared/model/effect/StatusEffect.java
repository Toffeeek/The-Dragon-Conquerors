// File Location: shared/src/main/java/com/shared/shared/model/effect/StatusEffect.java
package com.shared.shared.model.effect;

import lombok.NoArgsConstructor;

/**
 * A live status effect attached to one character.
 *
 * <p>{@link StatusEffectType} is the immutable rulebook; this is the mutable
 * instance that counts down. Keeping them separate means two players can both
 * be poisoned with independent timers, and a single effect definition can be
 * reused without copying.</p>
 *
 * <p>Has a no-args constructor and plain setters because instances are
 * serialised to JSON and sent over STOMP to keep clients in sync with the
 * authoritative server state.</p>
 */
@NoArgsConstructor
public class StatusEffect {

    private StatusEffectType type;

    /** Turns left before the effect resolves and is removed. */
    private int remainingTurns;

    /**
     * Player ID that applied this effect.
     *
     * <p>Needed by Curse, which must know <em>which</em> Wraith has to be struck
     * for the target to survive.</p>
     */
    private int sourcePlayerId = -1;

    /**
     * Set once the victim lands a hit on {@link #sourcePlayerId}.
     *
     * <p>Only meaningful for Curse: when true, the curse is escaped rather than
     * lethal on expiry.</p>
     */
    private boolean counterHitLanded;

    public StatusEffect(StatusEffectType type, int remainingTurns, int sourcePlayerId) {
        this.type = type;
        this.remainingTurns = remainingTurns;
        this.sourcePlayerId = sourcePlayerId;
    }

    public StatusEffectType getType() {
        return type;
    }

    public void setType(StatusEffectType type) {
        this.type = type;
    }

    public int getRemainingTurns() {
        return remainingTurns;
    }

    public void setRemainingTurns(int remainingTurns) {
        this.remainingTurns = remainingTurns;
    }

    public int getSourcePlayerId() {
        return sourcePlayerId;
    }

    public void setSourcePlayerId(int sourcePlayerId) {
        this.sourcePlayerId = sourcePlayerId;
    }

    public boolean isCounterHitLanded() {
        return counterHitLanded;
    }

    public void setCounterHitLanded(boolean counterHitLanded) {
        this.counterHitLanded = counterHitLanded;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Advances the timer by one turn.
     *
     * @return remaining turns after the tick
     */
    public int tick() {
        if (remainingTurns > 0) remainingTurns--;
        return remainingTurns;
    }

    public boolean isExpired() {
        return remainingTurns <= 0;
    }

    /**
     * Re-applying an effect refreshes its timer rather than stacking a second
     * copy, so repeated Poison Jabs extend the poison instead of doubling its
     * damage.
     */
    public void refresh(int duration) {
        this.remainingTurns = Math.max(this.remainingTurns, duration);
    }

    /** Records that the victim struck the effect's caster — this escapes a Curse. */
    public void markCounterHit() {
        this.counterHitLanded = true;
    }

    /**
     * True when this effect should kill its host as it expires.
     *
     * <p>Curse is lethal unless the victim landed a hit on the casting Wraith
     * while the curse was active.</p>
     */
    public boolean killsOnExpiry() {
        return type != null && type.isLethalOnExpiry() && !counterHitLanded;
    }

    /** Damage this effect deals on the host's turn start. */
    public int damageThisTurn() {
        return type == null ? 0 : type.getDamagePerTurn();
    }

    /** Short HUD label, e.g. {@code "Poison (2)"}. */
    public String toLabel() {
        if (type == null) return "";
        return type.getDisplayName() + " (" + remainingTurns + ")";
    }

    @Override
    public String toString() {
        return "StatusEffect{" + type + ", turns=" + remainingTurns
            + ", source=" + sourcePlayerId
            + (counterHitLanded ? ", escaped" : "") + "}";
    }
}
