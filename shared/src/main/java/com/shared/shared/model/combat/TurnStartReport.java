// File Location: shared/src/main/java/com/shared/shared/model/combat/TurnStartReport.java
package com.shared.shared.model.combat;

import com.shared.shared.model.effect.StatusEffectType;

import java.util.Collections;
import java.util.List;

/**
 * What happened to a combatant between the moment its turn came up and the moment
 * it got control.
 *
 * <p>{@link StatusEffectEngine#beginTurn} mutates the combatant and returns one of
 * these. Callers need it for three different reasons, which is why it reports more
 * than a single boolean:</p>
 *
 * <ul>
 *   <li>The HUD prints {@link #describe()} to the combat log, so a player who
 *       lost 8 HP to a burn knows why.</li>
 *   <li>The turn manager checks {@link #isTurnSkipped()} — a stunned or newly
 *       dead combatant must be advanced past rather than handed control.</li>
 *   <li>The match controller checks {@link #isDied()} to test for a team wipe.</li>
 * </ul>
 *
 * <p>Immutable, so it can be handed to the renderer and the network layer without
 * either being able to corrupt it.</p>
 */
public final class TurnStartReport {

    private final int combatantId;
    private final int damageTaken;
    private final boolean died;
    private final boolean turnSkipped;
    private final List<StatusEffectType> expiredEffects;
    private final StatusEffectType lethalEffect;

    TurnStartReport(int combatantId, int damageTaken, boolean died, boolean turnSkipped,
                    List<StatusEffectType> expiredEffects, StatusEffectType lethalEffect) {
        this.combatantId = combatantId;
        this.damageTaken = damageTaken;
        this.died = died;
        this.turnSkipped = turnSkipped;
        this.expiredEffects = expiredEffects == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(expiredEffects);
        this.lethalEffect = lethalEffect;
    }

    /** A turn that began with nothing happening at all. */
    static TurnStartReport uneventful(int combatantId) {
        return new TurnStartReport(combatantId, 0, false, false, null, null);
    }

    public int getCombatantId() {
        return combatantId;
    }

    /** Total damage dealt by all damage-over-time effects this turn. */
    public int getDamageTaken() {
        return damageTaken;
    }

    /** True when the combatant was downed on the way into its turn. */
    public boolean isDied() {
        return died;
    }

    /**
     * True when the combatant does not get to act.
     *
     * <p>Either it was stunned, or it was downed before it could act. The turn
     * manager should advance immediately rather than wait for input.</p>
     */
    public boolean isTurnSkipped() {
        return turnSkipped;
    }

    /** Effects whose timers ran out this turn and have been removed. */
    public List<StatusEffectType> getExpiredEffects() {
        return expiredEffects;
    }

    /**
     * The effect that killed the combatant as it expired, or {@code null}.
     *
     * <p>In practice this is always {@link StatusEffectType#CURSE} — it is the
     * only effect in the game that is lethal on expiry.</p>
     */
    public StatusEffectType getLethalEffect() {
        return lethalEffect;
    }

    /** True when there is nothing worth telling the player about. */
    public boolean isUneventful() {
        return damageTaken == 0 && !died && !turnSkipped && expiredEffects.isEmpty();
    }

    /** One-line combat-log summary; empty when {@link #isUneventful()}. */
    public String describe() {
        if (isUneventful()) return "";

        StringBuilder builder = new StringBuilder();
        if (damageTaken > 0) {
            builder.append("Suffers ").append(damageTaken).append(" damage from status effects");
        }

        if (lethalEffect != null) {
            append(builder, lethalEffect.getDisplayName() + " claims its target");
        } else if (died) {
            append(builder, "is downed");
        }

        if (turnSkipped && !died) {
            append(builder, "turn skipped");
        }

        for (StatusEffectType expired : expiredEffects) {
            if (expired == lethalEffect) continue;
            append(builder, expired.getDisplayName() + " wears off");
        }

        return builder.toString();
    }

    private static void append(StringBuilder builder, String fragment) {
        if (builder.length() > 0) builder.append("; ");
        builder.append(fragment);
    }

    @Override
    public String toString() {
        return "TurnStartReport{id=" + combatantId
            + ", damage=" + damageTaken
            + ", died=" + died
            + ", skipped=" + turnSkipped
            + ", expired=" + expiredEffects + "}";
    }
}
