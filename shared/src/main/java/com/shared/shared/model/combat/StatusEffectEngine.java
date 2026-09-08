// File Location: shared/src/main/java/com/shared/shared/model/combat/StatusEffectEngine.java
package com.shared.shared.model.combat;

import com.shared.shared.model.effect.StatusEffect;
import com.shared.shared.model.effect.StatusEffectType;
import com.shared.shared.model.stats.StatBoost;
import com.shared.shared.model.world.Environment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Applies, expires and resolves status effects, and owns the turn-start sequence.
 *
 * <p>Stateless: every method reads and writes the combatant it is given, so one
 * instance can serve a whole match, and the server and client can each hold their
 * own without needing to synchronise the engine itself — only the combatants.</p>
 *
 * <h2>The turn-start sequence</h2>
 *
 * <p>{@link #beginTurn} is the single entry point, and the order of its five steps
 * is load-bearing rather than incidental:</p>
 *
 * <ol>
 *   <li><b>Environment</b> applies first, so a Lava map's burn is dealt on the
 *       same turn it is inflicted rather than a turn later.</li>
 *   <li><b>Damage-over-time</b> is dealt next, from every active effect.</li>
 *   <li><b>Stun</b> is checked <em>before</em> timers advance. A 1-turn stun must
 *       cost the turn it is checked on; ticking first would expire it unused.</li>
 *   <li><b>Timers advance and expiries resolve</b> — Curse kills here, and
 *       Sub-zero's speed penalty is reversed here.</li>
 *   <li><b>Cooldowns tick and resources reset</b>, and only if the combatant is
 *       still standing.</li>
 * </ol>
 *
 * <h2>Fire and ice</h2>
 *
 * <p>The design document says Burn is "removed by ice" and Sub-zero is "removed by
 * fire". That leaves one thing unstated: does the fire that thaws a chill also set
 * the target alight? This engine says no — the incoming effect is consumed doing
 * the cleansing (see {@link #CLEANSE_CONSUMES_INCOMING}). The ability's damage
 * still lands; only its rider is spent. That makes an elemental follow-up a real
 * decision instead of a strict upgrade, and it is one constant to flip if the team
 * disagrees.</p>
 */
public class StatusEffectEngine {

    /**
     * Source ID recorded for effects inflicted by the map rather than by a player.
     *
     * <p>Negative so it can never collide with a server-assigned player ID, which
     * matters because Curse compares this field against a real ID.</p>
     */
    public static final int ENVIRONMENT_SOURCE_ID = -2;

    /**
     * When true, an effect that cleanses its opposite is spent doing so and is not
     * itself applied. See the class notes on fire and ice.
     */
    public static final boolean CLEANSE_CONSUMES_INCOMING = true;

    // ──────────────────────────────────────────────────────────────────────
    //  Applying and removing
    // ──────────────────────────────────────────────────────────────────────

    /** Applies an effect for its default duration. */
    public boolean apply(Combatant target, StatusEffectType type, int sourcePlayerId) {
        return type == null
            ? false
            : apply(target, type, sourcePlayerId, type.getDefaultDuration());
    }

    /**
     * Applies an effect to a combatant.
     *
     * <p>Three outcomes, in priority order: the effect cleanses something the
     * target already has and is consumed doing so; the target already has this
     * effect and its timer is refreshed rather than stacked; or the effect is added
     * and its stat modifier applied.</p>
     *
     * @return true when the target now carries the effect. False means the effect
     *         did not land — either it was spent cleansing, or the target was not
     *         a valid recipient
     */
    public boolean apply(Combatant target, StatusEffectType type, int sourcePlayerId,
                         int duration) {
        if (target == null || type == null || !target.isAlive()) return false;

        List<StatusEffect> effects = target.getActiveEffects();
        if (effects == null) return false;

        StatusEffectType opposite = type.cleanses();
        if (opposite != null && remove(target, opposite) && CLEANSE_CONSUMES_INCOMING) {
            return false;
        }

        StatusEffect existing = find(target, type);
        if (existing != null) {
            // Refresh, never stack: repeated Poison Jabs extend the poison rather
            // than doubling its damage, which would make it the strongest DoT in
            // the game against a single target.
            existing.refresh(duration);
            return true;
        }

        effects.add(new StatusEffect(type, duration, sourcePlayerId));

        StatBoost modifier = type.getStatModifier();
        if (modifier != null) modifier.applyTo(target.getStats());

        return true;
    }

    /**
     * Removes an effect and reverses its stat modifier.
     *
     * @return true when the effect was present
     */
    public boolean remove(Combatant target, StatusEffectType type) {
        if (target == null || type == null) return false;

        List<StatusEffect> effects = target.getActiveEffects();
        if (effects == null) return false;

        boolean removed = false;
        Iterator<StatusEffect> iterator = effects.iterator();
        while (iterator.hasNext()) {
            StatusEffect effect = iterator.next();
            if (effect.getType() != type) continue;

            iterator.remove();
            removed = true;
        }

        if (removed) revertModifier(target, type);
        return removed;
    }

    /** Removes every effect, reversing each stat modifier. Used when a match ends. */
    public void clear(Combatant target) {
        if (target == null || target.getActiveEffects() == null) return;

        for (StatusEffect effect : new ArrayList<>(target.getActiveEffects())) {
            remove(target, effect.getType());
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Queries
    // ──────────────────────────────────────────────────────────────────────

    public boolean has(Combatant target, StatusEffectType type) {
        return find(target, type) != null;
    }

    /** The live instance of an effect on a combatant, or {@code null}. */
    public StatusEffect find(Combatant target, StatusEffectType type) {
        if (target == null || type == null || target.getActiveEffects() == null) return null;

        for (StatusEffect effect : target.getActiveEffects()) {
            if (effect.getType() == type) return effect;
        }
        return null;
    }

    /** True when the combatant will lose its next turn. */
    public boolean isStunned(Combatant target) {
        return has(target, StatusEffectType.STUN);
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Curse counterplay
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Records that {@code attacker} landed damage on {@code target}, which is how a
     * cursed character escapes death.
     *
     * <p>The design document lets a cursed target survive by hitting the Wraith
     * that cursed it. So this inspects the <em>attacker's</em> curses and marks any
     * whose caster is the combatant just struck. Call it from the resolver on every
     * successful damaging hit; it is a no-op when no curse is involved.</p>
     */
    public boolean noteDamageDealt(Combatant attacker, Combatant target) {
        if (attacker == null || target == null) return false;
        if (attacker.getActiveEffects() == null) return false;

        boolean escaped = false;
        for (StatusEffect effect : attacker.getActiveEffects()) {
            if (effect.getType() != StatusEffectType.CURSE) continue;
            if (effect.getSourcePlayerId() != target.getId()) continue;

            effect.markCounterHit();
            escaped = true;
        }
        return escaped;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Turn start
    // ──────────────────────────────────────────────────────────────────────

    /** Turn start on a map with no hazards. */
    public TurnStartReport beginTurn(Combatant combatant, CooldownTracker cooldowns) {
        return beginTurn(combatant, cooldowns, null, false);
    }

    /**
     * Runs the full turn-start sequence for one combatant. See the class notes for
     * why the steps are ordered the way they are.
     *
     * @param cooldowns        the combatant's own cooldowns; may be {@code null}
     * @param environment      the battlefield, or {@code null} for no hazards
     * @param standingOnHazard whether the combatant occupies a hazard tile. Passed
     *                         in rather than computed here because tile lookup
     *                         needs the loaded map, which this module deliberately
     *                         knows nothing about. Ignored unless the environment
     *                         is tile-based
     */
    public TurnStartReport beginTurn(Combatant combatant, CooldownTracker cooldowns,
                                     Environment environment, boolean standingOnHazard) {
        if (combatant == null) return TurnStartReport.uneventful(-1);

        int id = combatant.getId();
        if (!combatant.isAlive()) {
            // Downed combatants are skipped by the turn queue, so this is a guard
            // rather than a normal path. Report a skipped turn and change nothing.
            return new TurnStartReport(id, 0, false, true, null, null);
        }

        List<StatusEffect> effects = combatant.getActiveEffects();
        if (effects == null) {
            if (cooldowns != null) cooldowns.tick();
            combatant.onTurnStart();
            return TurnStartReport.uneventful(id);
        }

        // 1. Environment hazards land before anything is resolved.
        StatusEffectType hazard = hazardEffectFor(environment, standingOnHazard);
        if (hazard != null) apply(combatant, hazard, ENVIRONMENT_SOURCE_ID);

        // 2. Damage over time.
        int damage = 0;
        for (StatusEffect effect : effects) {
            damage += effect.damageThisTurn();
        }
        boolean died = false;
        if (damage > 0) {
            // applyDamage reports whether the combatant is still standing.
            died = !combatant.getStats().applyDamage(damage);
        }

        // 3. Stun is read before timers move, so a 1-turn stun costs this turn.
        boolean stunned = !died && isStunned(combatant);

        // 4. Advance timers and resolve expiries.
        List<StatusEffectType> expired = new ArrayList<>();
        StatusEffectType lethal = null;
        Iterator<StatusEffect> iterator = effects.iterator();
        while (iterator.hasNext()) {
            StatusEffect effect = iterator.next();
            effect.tick();
            if (!effect.isExpired()) continue;

            StatusEffectType type = effect.getType();
            if (effect.killsOnExpiry()) {
                combatant.getStats().setHp(0);
                died = true;
                lethal = type;
            }

            iterator.remove();
            expired.add(type);
            revertModifier(combatant, type);
        }

        // 5. Cooldowns and per-turn resources, only for a combatant still standing.
        if (cooldowns != null) cooldowns.tick();
        if (!died) combatant.onTurnStart();

        return new TurnStartReport(id, damage, died, stunned || died, expired, lethal);
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Internals
    // ──────────────────────────────────────────────────────────────────────

    /**
     * The effect an environment inflicts on a combatant this turn, or {@code null}.
     *
     * <p>Lava affects everyone wherever they stand; Bog only affects a combatant on
     * a hazard tile; Canyon inflicts nothing, since its danger is falling off,
     * which is a movement concern rather than a turn-start one.</p>
     */
    private StatusEffectType hazardEffectFor(Environment environment, boolean standingOnHazard) {
        if (environment == null || environment.getHazardEffect() == null) return null;
        if (environment.affectsEveryone()) return environment.getHazardEffect();
        if (environment.affectsHazardTilesOnly() && standingOnHazard) {
            return environment.getHazardEffect();
        }
        return null;
    }

    /**
     * Reverses an effect's stat modifier, but only once the last copy is gone.
     *
     * <p>{@link #apply} never stacks a duplicate, so in practice there is only ever
     * one copy; the guard exists so this stays correct if stacking is ever
     * introduced.</p>
     */
    private void revertModifier(Combatant target, StatusEffectType type) {
        StatBoost modifier = type.getStatModifier();
        if (modifier == null) return;
        if (has(target, type)) return;

        modifier.inverted().applyTo(target.getStats());
    }
}
