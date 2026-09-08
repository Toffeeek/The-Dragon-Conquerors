// File Location: shared/src/main/java/com/shared/shared/model/combat/AbilityResolver.java
package com.shared.shared.model.combat;

import com.badlogic.gdx.math.Vector2;
import com.shared.shared.model.ability.Ability;
import com.shared.shared.model.ability.AbilityType;
import com.shared.shared.model.ability.TargetType;
import com.shared.shared.model.combat.AbilityOutcome.Rejection;
import com.shared.shared.model.combat.AbilityOutcome.TargetResult;
import com.shared.shared.model.effect.StatusEffectType;
import com.shared.shared.model.stats.StatBoost;
import com.shared.shared.model.stats.StatCalculator;

import java.util.List;
import java.util.Random;

/**
 * Turns "player 3 used Fireball on player 1" into what actually happens.
 *
 * <p>This is the single rulebook for using an ability: legality, mana, cooldown,
 * hit chance against evasion, damage, healing, status-effect rolls, displacement
 * and relocation. It is written entirely against {@link Ability} and
 * {@link Combatant}, so the same compiled code runs on the server as the
 * authority and on each client for prediction.</p>
 *
 * <h2>No per-ability branching</h2>
 *
 * <p>There is not a single {@code if (ability == FIREBALL)} below, and that is the
 * point — an ability is a bundle of declarative numbers, so a new one is a new enum
 * constant and nothing here changes. Even the awkward cases are expressed as general
 * rules:</p>
 *
 * <ul>
 *   <li><b>Teleport</b> is "a {@link TargetType#TILE} ability relocates its actor".</li>
 *   <li><b>Revive</b> is healing aimed at a {@link TargetType#DOWNED_ALLY}; healing
 *       from 0 HP makes a combatant alive again with no special case at all.</li>
 *   <li><b>Encore</b> is {@link Ability#restoresUltimate()}, with one general
 *       guard: an ability may never reset itself.</li>
 *   <li><b>Eldritch Blast</b> is a push distance plus a 100% status effect.</li>
 * </ul>
 *
 * <h2>Randomness and authority</h2>
 *
 * <p>Hit rolls and the 50% effect rolls make resolution non-deterministic, so the
 * <b>server's outcome is the truth</b> — a client that resolves locally for
 * responsiveness must reconcile against the broadcast result rather than trust its
 * own roll. Pass a seed to the constructor to make a run reproducible for tests.</p>
 *
 * <h2>What this deliberately does not know</h2>
 *
 * <p>Terrain. The resolver has no map, so a push or a teleport can put a combatant
 * inside a wall or over a cliff edge. Both destinations are reported in the outcome
 * ({@link TargetResult#getPushedTo()}, {@link AbilityOutcome#getActorMovedTo()}) for
 * the caller — which does have the navigation grid — to clamp, and on the Canyon to
 * rule a fatal fall.</p>
 */
public final class AbilityResolver {

    /** Rolls are percentile: {@code nextInt(100)} against a 0-100 threshold. */
    private static final int PERCENT = 100;

    private final Random random;

    /** Production constructor: unseeded randomness. */
    public AbilityResolver() {
        this(new Random());
    }

    /** Reproducible resolution, for tests and for replaying a recorded match. */
    public AbilityResolver(long seed) {
        this(new Random(seed));
    }

    public AbilityResolver(Random random) {
        this.random = random == null ? new Random() : random;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Entry points
    // ──────────────────────────────────────────────────────────────────────

    /** Uses an ability on a combatant — every target type except TILE and AREA_ENEMIES. */
    public AbilityOutcome resolve(Combatant actor, Ability ability, Combatant target,
                                  CombatContext context) {
        return resolve(actor, ability, target, null, context);
    }

    /** Uses an ability on a point on the ground — TILE and AREA_ENEMIES. */
    public AbilityOutcome resolve(Combatant actor, Ability ability, Vector2 point,
                                  CombatContext context) {
        return resolve(actor, ability, null, point, context);
    }

    /**
     * Uses an ability, validating it first and mutating nothing if it is illegal.
     *
     * <p>Mana is spent and the cooldown starts <em>before</em> any roll, so a missed
     * attack still costs what it cost. That is the intended risk of a low-accuracy
     * character taking a shot at a fast one.</p>
     *
     * @param target  the combatant aimed at, or {@code null} for a ground ability
     * @param point   the position aimed at, or {@code null} for a combatant ability
     * @param context the match state; must not be {@code null}
     */
    public AbilityOutcome resolve(Combatant actor, Ability ability, Combatant target,
                                  Vector2 point, CombatContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null — resolution "
                + "needs the match's cooldowns, effects and roster");
        }

        Rejection rejection = validate(actor, ability, target, point, context);
        if (rejection != null) return AbilityOutcome.rejected(actor, ability, rejection);

        AbilityOutcome outcome = AbilityOutcome.accepted(actor, ability);

        int manaCost = ability.getManaCost();
        if (manaCost > 0 && actor.getStats().spendMana(manaCost)) {
            outcome.manaSpent = manaCost;
        }
        context.cooldownsFor(actor).start(ability);

        switch (ability.getTargetType()) {
            case SELF:
                applySupport(actor, actor, ability, outcome, context);
                break;

            case ALLY:
            case DOWNED_ALLY:
                applySupport(actor, target, ability, outcome, context);
                break;

            case ENEMY:
                applyOffence(actor, target, ability, outcome, context);
                break;

            case TILE:
                actor.getPosition().set(point);
                outcome.actorMovedTo = new Vector2(point);
                break;

            case AREA_ENEMIES:
                List<Combatant> caught =
                    context.enemiesWithin(actor, point, ability.getAreaRadius());
                // An empty list is a legal outcome, not an error: the volley was
                // aimed at open ground and the mana is spent regardless.
                for (Combatant enemy : caught) {
                    applyOffence(actor, enemy, ability, outcome, context);
                }
                break;

            default:
                break;
        }

        return outcome;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Validation
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Checks whether an action is legal, mutating nothing.
     *
     * <p>The server calls this on every incoming action, and the client calls it to
     * grey out action buttons and reject a click before sending it. Same code, so
     * the two can never disagree about what is allowed.</p>
     *
     * @return the reason the action is illegal, or {@code null} when it is legal
     */
    public Rejection validate(Combatant actor, Ability ability, Combatant target,
                              Vector2 point, CombatContext context) {
        if (actor == null) return Rejection.NO_ACTOR;
        if (actor.isDowned()) return Rejection.ACTOR_DOWNED;
        if (ability == null) return Rejection.NO_ABILITY;
        if (!canUse(actor, ability)) return Rejection.ABILITY_NOT_OWNED;

        if (context != null) {
            if (context.getEffects().isStunned(actor)) return Rejection.ACTOR_STUNNED;
            if (!context.cooldownsFor(actor).isReady(ability)) return Rejection.ON_COOLDOWN;
        }

        if (actor.getStats().getMana() < ability.getManaCost()) {
            return Rejection.NOT_ENOUGH_MANA;
        }

        return validateTarget(actor, ability, target, point);
    }

    /**
     * True when this combatant is allowed to use this ability.
     *
     * <p>Compares owner classes rather than searching {@link Combatant#abilities()}
     * so a hand-written {@link Ability} implementation that is not in the enum
     * catalogue still validates correctly. A {@code null} owner is universal.</p>
     */
    public boolean canUse(Combatant actor, Ability ability) {
        if (actor == null || ability == null) return false;
        return ability.getOwnerClass() == null
            || ability.getOwnerClass() == actor.getCharacterClass();
    }

    private Rejection validateTarget(Combatant actor, Ability ability, Combatant target,
                                     Vector2 point) {
        TargetType targetType = ability.getTargetType();
        if (targetType == null) return Rejection.INVALID_TARGET;

        switch (targetType) {
            case SELF:
                // Resolves on the actor; whatever was clicked is irrelevant, and
                // range cannot be exceeded.
                return null;

            case ALLY:
                if (target == null) return Rejection.NO_TARGET;
                if (!actor.isAllyOf(target)) return Rejection.INVALID_TARGET;
                if (target.isDowned()) return Rejection.TARGET_DOWNED;
                return rangeTo(actor, target.getPosition(), ability);

            case DOWNED_ALLY:
                if (target == null) return Rejection.NO_TARGET;
                if (!actor.isAllyOf(target)) return Rejection.INVALID_TARGET;
                if (target.isAlive()) return Rejection.TARGET_NOT_DOWNED;
                return rangeTo(actor, target.getPosition(), ability);

            case ENEMY:
                if (target == null) return Rejection.NO_TARGET;
                if (!actor.isEnemyOf(target)) return Rejection.INVALID_TARGET;
                if (target.isDowned()) return Rejection.TARGET_DOWNED;
                return rangeTo(actor, target.getPosition(), ability);

            case TILE:
            case AREA_ENEMIES:
                if (point == null) return Rejection.NO_TARGET;
                return rangeTo(actor, point, ability);

            default:
                return Rejection.INVALID_TARGET;
        }
    }

    private Rejection rangeTo(Combatant actor, Vector2 destination, Ability ability) {
        if (destination == null) return Rejection.NO_TARGET;
        if (actor.getPosition().dst(destination) > ability.getRange()) {
            return Rejection.OUT_OF_RANGE;
        }
        return null;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Friendly abilities
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Healing, buffs and cooldown restoration.
     *
     * <p>No hit roll: a healer does not miss an ally who is standing still and
     * wants the help, and making support dodgeable would punish the two classes
     * whose entire kit is support.</p>
     */
    private void applySupport(Combatant actor, Combatant target, Ability ability,
                              AbilityOutcome outcome, CombatContext context) {
        if (target == null) return;

        TargetResult result = outcome.addTarget(target);
        result.hit = true;

        boolean wasDowned = target.isDowned();

        int healing = StatCalculator.scaleHealing(ability.getBaseHealing(), actor.getStats());
        if (healing > 0) {
            target.getStats().heal(healing);
            result.healing = healing;
            // Revive needs no special case: healing a combatant off 0 HP is a
            // revival by definition.
            result.revived = wasDowned && target.isAlive();
        }

        StatBoost boost = ability.getGrantedBoost();
        if (boost != null) {
            boost.applyTo(target.getStats());
            result.boostGranted = boost;
        }

        if (ability.restoresUltimate()) {
            restoreUltimate(target, ability, outcome, context);
        }

        rollStatusEffect(actor, target, ability, result, context);
    }

    /**
     * Clears the target's ultimate cooldown — the Bard's Encore.
     *
     * <p>Refuses to reset the ability doing the resetting. Encore <em>is</em> a Bard
     * ultimate, so without this a Bard could target itself, clear Encore's own
     * six-turn cooldown, and recast it every turn for the mana alone.</p>
     */
    private void restoreUltimate(Combatant target, Ability ability, AbilityOutcome outcome,
                                 CombatContext context) {
        AbilityType ultimate = AbilityType.ultimateFor(target.getCharacterClass());
        if (ultimate == null || ultimate.equals(ability)) return;

        context.cooldownsFor(target).reset(ultimate);
        outcome.ultimateRestored = true;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Hostile abilities
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Damage, status effects and displacement against one enemy.
     *
     * <p>Only damaging abilities roll to hit. A pure debuff such as Curse already
     * carries its own 50% chance to land; stacking a hit roll on top would take a
     * 24-mana ability on a three-turn cooldown down to roughly a one-in-four
     * success rate, which is not a threat anyone would play around.</p>
     */
    private void applyOffence(Combatant actor, Combatant target, Ability ability,
                              AbilityOutcome outcome, CombatContext context) {
        if (target == null) return;

        TargetResult result = outcome.addTarget(target);

        if (ability.isOffensive() && !rollHit(actor, target)) {
            result.hit = false;
            return;
        }
        result.hit = true;

        int damage = StatCalculator.scaleDamage(ability.getBaseDamage(), actor.getStats());
        if (damage > 0) {
            boolean survived = target.getStats().applyDamage(damage);
            result.damage = damage;
            result.downed = !survived;

            // Landing a hit is how a cursed character escapes its curse — this
            // marks any curse on the actor that this target cast.
            context.getEffects().noteDamageDealt(actor, target);
        }

        // A combatant that just went down is not additionally poisoned or shoved.
        if (target.isDowned()) return;

        rollStatusEffect(actor, target, ability, result, context);
        applyPush(actor, target, ability, result);
    }

    /** Percentile hit roll: accuracy against the target's evasion. */
    private boolean rollHit(Combatant actor, Combatant target) {
        int chance = StatCalculator.hitChanceAgainst(actor.getStats(), target.getStats());
        return random.nextInt(PERCENT) < chance;
    }

    /**
     * Rolls the ability's status effect and records what happened.
     *
     * <p>Reports a cleanse separately from an application because they are mutually
     * exclusive: ice thrown at a burning target puts the fire out and is spent doing
     * it, so the HUD needs to say "Burn removed" rather than "Sub-zero applied".</p>
     */
    private void rollStatusEffect(Combatant actor, Combatant target, Ability ability,
                                  TargetResult result, CombatContext context) {
        if (!ability.hasStatusEffect() || !target.isAlive()) return;
        if (random.nextInt(PERCENT) >= ability.getEffectChancePercent()) return;

        StatusEffectType effect = ability.getAppliedEffect();
        StatusEffectType opposite = effect.cleanses();
        boolean hadOpposite = opposite != null && context.getEffects().has(target, opposite);

        boolean landed = context.getEffects().apply(target, effect, actor.getId());

        if (hadOpposite) result.effectCleansed = opposite;
        if (landed) result.effectApplied = effect;
    }

    /**
     * Shoves the target directly away from the actor.
     *
     * <p>Not terrain-checked — see the class notes. The destination is recorded so
     * the caller can clamp it against the navigation grid, or on the Canyon decide
     * the target was pushed off the edge.</p>
     */
    private void applyPush(Combatant actor, Combatant target, Ability ability,
                           TargetResult result) {
        if (!ability.pushesTarget()) return;

        Vector2 direction = new Vector2(target.getPosition()).sub(actor.getPosition());
        if (direction.isZero(0.0001f)) {
            // Occupying the same point leaves no direction to push along; pick one
            // rather than divide by zero and produce a NaN position.
            direction.set(1f, 0f);
        }

        target.getPosition().add(direction.nor().scl(ability.getPushDistance()));
        result.pushedTo = new Vector2(target.getPosition());
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Read-only helpers for the HUD
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Percent chance {@code actor} would hit {@code target}, for the targeting
     * tooltip. Rolls nothing.
     */
    public int previewHitChance(Combatant actor, Combatant target) {
        if (actor == null) return 0;
        if (target == null) return StatCalculator.hitChances(actor.getStats());
        return StatCalculator.hitChanceAgainst(actor.getStats(), target.getStats());
    }

    /** Damage this actor would deal with this ability on a hit, for the tooltip. */
    public int previewDamage(Combatant actor, Ability ability) {
        if (actor == null || ability == null) return 0;
        return StatCalculator.scaleDamage(ability.getBaseDamage(), actor.getStats());
    }

    /** Healing this actor would apply with this ability, for the tooltip. */
    public int previewHealing(Combatant actor, Ability ability) {
        if (actor == null || ability == null) return 0;
        return StatCalculator.scaleHealing(ability.getBaseHealing(), actor.getStats());
    }
}
