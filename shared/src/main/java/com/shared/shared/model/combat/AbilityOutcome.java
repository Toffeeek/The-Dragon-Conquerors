// File Location: shared/src/main/java/com/shared/shared/model/combat/AbilityOutcome.java
package com.shared.shared.model.combat;

import com.badlogic.gdx.math.Vector2;
import com.shared.shared.model.ability.Ability;
import com.shared.shared.model.effect.StatusEffectType;
import com.shared.shared.model.stats.StatBoost;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Everything that happened when one ability was used — or the reason nothing did.
 *
 * <p>{@link AbilityResolver} returns one of these per action. It carries a
 * per-target breakdown rather than a single damage number because Rain of Arrows
 * can hit two enemies, miss one and kill the other, and all four facts need to
 * reach the HUD.</p>
 *
 * <h2>Why the rejection is data, not an exception</h2>
 *
 * <p>An illegal action is a routine event, not a bug: the server validates every
 * incoming action and most rejections are just a client that had stale state. So a
 * rejection is a returned value the server can log and reply with, and the client
 * can print as "Not enough mana" — throwing would force a try/catch around the
 * ordinary path.</p>
 *
 * <h2>Mutability</h2>
 *
 * <p>Assembled field by field by the resolver, which lives in this package, and
 * exposes only getters to everyone else. Once returned it cannot be altered.</p>
 */
public final class AbilityOutcome {

    /** Why an action was refused. */
    public enum Rejection {
        NO_ACTOR("There is no one to act"),
        ACTOR_DOWNED("You are down and cannot act"),
        ACTOR_STUNNED("You are stunned"),
        NO_ABILITY("No ability was chosen"),
        ABILITY_NOT_OWNED("Your class cannot use that ability"),
        ON_COOLDOWN("That ability is still recharging"),
        NOT_ENOUGH_MANA("Not enough mana"),
        NO_TARGET("That ability needs a target"),
        INVALID_TARGET("That is not a legal target"),
        TARGET_DOWNED("That target is already down"),
        TARGET_NOT_DOWNED("That target does not need reviving"),
        OUT_OF_RANGE("Target is out of range");

        private final String message;

        Rejection(String message) {
            this.message = message;
        }

        /** Player-facing text, ready to print to the combat log. */
        public String getMessage() {
            return message;
        }
    }

    /**
     * What one ability did to one combatant.
     *
     * <p>Fields are package-private so the resolver can fill them in as it works;
     * outside this package the class is read-only.</p>
     */
    public static final class TargetResult {

        final int targetId;
        final String targetName;
        boolean hit;
        int damage;
        int healing;
        StatusEffectType effectApplied;
        StatusEffectType effectCleansed;
        StatBoost boostGranted;
        boolean downed;
        boolean revived;
        Vector2 pushedTo;

        TargetResult(Combatant target) {
            this.targetId = target.getId();
            this.targetName = target.getUsername();
        }

        public int getTargetId() {
            return targetId;
        }

        public String getTargetName() {
            return targetName;
        }

        /** False means the attack missed; damage and effects will both be zero. */
        public boolean isHit() {
            return hit;
        }

        public int getDamage() {
            return damage;
        }

        public int getHealing() {
            return healing;
        }

        /** Status effect that landed on this target, or {@code null}. */
        public StatusEffectType getEffectApplied() {
            return effectApplied;
        }

        /**
         * Effect removed from this target because the incoming one cancelled it —
         * fire clearing a chill, or ice clearing a burn. {@code null} otherwise.
         */
        public StatusEffectType getEffectCleansed() {
            return effectCleansed;
        }

        /**
         * Stat change granted to this target, or {@code null}.
         *
         * <p>Lasts the rest of the match rather than a set number of turns — the
         * design document describes Inspiring Verse as raising strength "for the
         * fight", so it is applied straight to the stat block with nothing to tick
         * down.</p>
         */
        public StatBoost getBoostGranted() {
            return boostGranted;
        }

        /** True when this ability reduced the target to 0 HP. */
        public boolean isDowned() {
            return downed;
        }

        /** True when this ability brought the target back from 0 HP. */
        public boolean isRevived() {
            return revived;
        }

        /**
         * Where the target was pushed to, or {@code null} if it was not displaced.
         *
         * <p>The resolver has no map, so this position is not terrain-checked. The
         * caller — which does have the {@code NavGrid} — is responsible for clamping
         * it to walkable ground, and on the Canyon for deciding it was a fatal
         * fall.</p>
         */
        public Vector2 getPushedTo() {
            return pushedTo;
        }

        @Override
        public String toString() {
            if (!hit) return targetName + ": miss";

            StringBuilder builder = new StringBuilder(targetName).append(": ");
            if (damage > 0) builder.append(damage).append(" damage");
            if (healing > 0) builder.append(healing).append(" healed");
            if (boostGranted != null) builder.append(' ').append(boostGranted);
            if (effectApplied != null) builder.append(" +").append(effectApplied.getDisplayName());
            if (effectCleansed != null) builder.append(" -").append(effectCleansed.getDisplayName());
            if (revived) builder.append(" REVIVED");
            if (downed) builder.append(" DOWNED");
            return builder.toString();
        }
    }

    private final int actorId;
    private final String actorName;
    private final Ability ability;
    private final Rejection rejection;
    private final List<TargetResult> targets;

    int manaSpent;
    boolean ultimateRestored;
    Vector2 actorMovedTo;

    private AbilityOutcome(int actorId, String actorName, Ability ability, Rejection rejection,
                           List<TargetResult> targets) {
        this.actorId = actorId;
        this.actorName = actorName;
        this.ability = ability;
        this.rejection = rejection;
        this.targets = targets;
    }

    /** A refused action. Nothing was mutated; the reason is in {@code rejection}. */
    static AbilityOutcome rejected(Combatant actor, Ability ability, Rejection rejection) {
        return new AbilityOutcome(
            actor == null ? -1 : actor.getId(),
            actor == null ? "?" : actor.getUsername(),
            ability, rejection, Collections.emptyList());
    }

    /** An accepted action, whose per-target results the resolver fills in. */
    static AbilityOutcome accepted(Combatant actor, Ability ability) {
        return new AbilityOutcome(actor.getId(), actor.getUsername(), ability, null,
            new ArrayList<>());
    }

    /** Adds and returns a fresh result slot for one affected combatant. */
    TargetResult addTarget(Combatant target) {
        TargetResult result = new TargetResult(target);
        targets.add(result);
        return result;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Reading the outcome
    // ──────────────────────────────────────────────────────────────────────

    public int getActorId() {
        return actorId;
    }

    public String getActorName() {
        return actorName;
    }

    public Ability getAbility() {
        return ability;
    }

    /** True when the action was accepted and applied. */
    public boolean isLegal() {
        return rejection == null;
    }

    /** Why the action was refused, or {@code null} when it was accepted. */
    public Rejection getRejection() {
        return rejection;
    }

    /** Mana actually deducted from the actor; 0 for a rejected action. */
    public int getManaSpent() {
        return manaSpent;
    }

    /** True when this action cleared an ally's ultimate cooldown (Encore). */
    public boolean isUltimateRestored() {
        return ultimateRestored;
    }

    /**
     * Where the actor relocated to, or {@code null} if it did not move.
     *
     * <p>Teleport is the only built-in ability that sets this. Like
     * {@link TargetResult#getPushedTo()} it is not terrain-checked here.</p>
     */
    public Vector2 getActorMovedTo() {
        return actorMovedTo;
    }

    /** Per-target breakdown, in the order the resolver processed them. */
    public List<TargetResult> getTargets() {
        return Collections.unmodifiableList(targets);
    }

    /** Convenience for single-target abilities: the first result, or {@code null}. */
    public TargetResult firstTarget() {
        return targets.isEmpty() ? null : targets.get(0);
    }

    /** Total damage dealt across every target. */
    public int totalDamage() {
        int total = 0;
        for (TargetResult result : targets) total += result.damage;
        return total;
    }

    /** Total healing applied across every target. */
    public int totalHealing() {
        int total = 0;
        for (TargetResult result : targets) total += result.healing;
        return total;
    }

    /** Combatants this action reduced to 0 HP. */
    public List<Integer> downedTargetIds() {
        List<Integer> ids = new ArrayList<>();
        for (TargetResult result : targets) {
            if (result.downed) ids.add(result.targetId);
        }
        return ids;
    }

    /** True when every target was missed — the action was spent for nothing. */
    public boolean missedEverything() {
        if (targets.isEmpty()) return false;
        for (TargetResult result : targets) {
            if (result.hit) return false;
        }
        return true;
    }

    /** One-line combat-log entry. */
    public String describe() {
        String abilityName = ability == null ? "unknown ability" : ability.getDisplayName();
        if (!isLegal()) {
            return actorName + " cannot use " + abilityName + ": " + rejection.getMessage();
        }

        StringBuilder builder = new StringBuilder(actorName).append(" uses ").append(abilityName);
        if (manaSpent > 0) builder.append(" (-").append(manaSpent).append(" mana)");

        if (actorMovedTo != null) {
            builder.append(" and blinks to ")
                .append(Math.round(actorMovedTo.x)).append(',')
                .append(Math.round(actorMovedTo.y));
        }
        if (ultimateRestored) builder.append(", restoring an ultimate");

        for (int i = 0; i < targets.size(); i++) {
            builder.append(i == 0 ? " — " : ", ").append(targets.get(i));
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        return describe();
    }
}
