// File Location: shared/src/main/java/com/shared/shared/model/combat/CombatContext.java
package com.shared.shared.model.combat;

import com.badlogic.gdx.math.Vector2;
import com.shared.shared.model.world.Environment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The mutable state of one match in progress.
 *
 * <p>Holds the four things combat resolution has to consult and nobody else owns:
 * the turn order, each combatant's cooldowns, the status-effect engine, and the
 * battlefield. The server builds the authoritative instance; each client keeps its
 * own copy and reconciles it against the server's broadcasts.</p>
 *
 * <h2>Why cooldowns live here and not on the combatant</h2>
 *
 * <p>{@link Combatant} is implemented by the client's rendered entity and by a plain
 * server object, and neither should have to carry a cooldown map to satisfy the
 * interface. Keying cooldowns by player ID here means the combatant stays a
 * description of a character while the context owns everything that is true only
 * inside a match.</p>
 *
 * <p>This is deliberately not a turn <em>driver</em>. It answers questions and
 * stores state; deciding when to advance the queue and whose input to wait for
 * belongs to the turn manager, which is the client's job.</p>
 */
public final class CombatContext {

    private final TurnQueue turnQueue;
    private final StatusEffectEngine effects = new StatusEffectEngine();

    /**
     * Cooldowns per player ID, created on first use.
     *
     * <p>Keyed by ID rather than by {@link Combatant} so a combatant object that is
     * replaced by a fresh instance from a network update — which the client does on
     * every full state sync — keeps its cooldowns.</p>
     */
    private final Map<Integer, CooldownTracker> cooldowns = new HashMap<>();

    private Environment environment;

    public CombatContext(Collection<? extends Combatant> combatants, Environment environment) {
        this.turnQueue = new TurnQueue(combatants);
        this.environment = environment;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  State
    // ──────────────────────────────────────────────────────────────────────

    public TurnQueue getTurnQueue() {
        return turnQueue;
    }

    public StatusEffectEngine getEffects() {
        return effects;
    }

    public Environment getEnvironment() {
        return environment;
    }

    /** Set once the environment vote resolves, before the first turn begins. */
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    /** This combatant's cooldowns, created empty on first request. */
    public CooldownTracker cooldownsFor(Combatant combatant) {
        if (combatant == null) return new CooldownTracker();
        return cooldowns.computeIfAbsent(combatant.getId(), id -> new CooldownTracker());
    }

    /** Everyone in the match, downed included. */
    public List<Combatant> getParticipants() {
        return turnQueue.getParticipants();
    }

    /**
     * Looks up a combatant by ID.
     *
     * <p>Needed because network packets carry IDs, never object references, so the
     * server's "player 3 attacked player 1" has to be resolved back to combatants
     * on arrival.</p>
     */
    public Combatant findById(int id) {
        for (Combatant combatant : turnQueue.getParticipants()) {
            if (combatant.getId() == id) return combatant;
        }
        return null;
    }

    /** The combatant currently holding the turn, or {@code null} before turn one. */
    public Combatant current() {
        return turnQueue.current();
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Turn start
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Runs the turn-start sequence for a combatant, using this match's environment.
     *
     * @param standingOnHazard whether the combatant is on a hazard tile. The caller
     *                         supplies it because tile lookup needs the loaded map,
     *                         which this module knows nothing about. Irrelevant on
     *                         Lava, which burns everyone, and on Canyon, which has
     *                         no turn-start hazard at all
     */
    public TurnStartReport beginTurn(Combatant combatant, boolean standingOnHazard) {
        return effects.beginTurn(combatant, cooldownsFor(combatant), environment, standingOnHazard);
    }

    /** Turn start for a battlefield with no tile hazards to check. */
    public TurnStartReport beginTurn(Combatant combatant) {
        return beginTurn(combatant, false);
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Spatial queries
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Living enemies of {@code actor} within {@code radius} of a point — the
     * targets of an area ability such as Rain of Arrows.
     *
     * <p>Sorted by player ID so every machine applies the volley's damage in the
     * same order. With random hit rolls in play, a different order would consume
     * the random sequence differently and desynchronise the result.</p>
     */
    public List<Combatant> enemiesWithin(Combatant actor, Vector2 point, float radius) {
        List<Combatant> found = new ArrayList<>();
        if (actor == null || point == null) return found;

        for (Combatant combatant : turnQueue.getParticipants()) {
            if (!actor.isEnemyOf(combatant) || !combatant.isAlive()) continue;
            if (combatant.getPosition().dst(point) > radius) continue;
            found.add(combatant);
        }

        found.sort(Comparator.comparingInt(Combatant::getId));
        return found;
    }

    /** Clears every status effect and cooldown. Called when the match ends. */
    public void reset() {
        for (Combatant combatant : turnQueue.getParticipants()) {
            effects.clear(combatant);
        }
        cooldowns.clear();
    }

    @Override
    public String toString() {
        return "CombatContext{" + (environment == null ? "no environment" : environment.getDisplayName())
            + ", " + turnQueue.size() + " combatants, " + turnQueue.describeOrder() + "}";
    }
}
