// File Location: shared/src/main/java/com/shared/shared/model/combat/Combatant.java
package com.shared.shared.model.combat;

import com.badlogic.gdx.math.Vector2;
import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.Race;
import com.shared.shared.model.ability.AbilityType;
import com.shared.shared.model.effect.StatusEffect;
import com.shared.shared.model.stats.StatComponent;

import java.util.List;

/**
 * Anything that can take a turn in combat.
 *
 * <h2>Why this interface exists</h2>
 *
 * <p>The design document requires <b>server-side turn validation</b>, which means
 * the server has to run the same turn order, cooldown and status-effect rules the
 * client does. The client's character is a libGDX-aware entity with sprites and
 * animation state; the server's is a plain object. Writing the rules against this
 * interface instead of against either one means the engine in this package is
 * compiled once and executed identically on both sides — no duplicated logic to
 * drift out of sync, which is the classic source of "it worked on my screen"
 * multiplayer bugs.</p>
 *
 * <h2>Implementing it</h2>
 *
 * <p>Implementations own their own state; the engine never stores anything about a
 * combatant. In particular {@link #getActiveEffects()} must return the
 * <em>live, mutable</em> list — {@link StatusEffectEngine} adds and removes
 * entries in place, and the HUD reads the same list to draw effect icons.</p>
 */
public interface Combatant {

    /** Team index used by the lobby's Azure team. */
    int TEAM_AZURE = 1;

    /** Team index used by the lobby's Crimson team. */
    int TEAM_CRIMSON = 2;

    /** Server-assigned player ID. Stable for the whole match and unique within it. */
    int getId();

    /** Display name shown on the HUD and in combat log lines. */
    String getUsername();

    /** Class, which determines this combatant's ability list. */
    CharacterClass getCharacterClass();

    /** Live stats. Mutated in place by damage, healing and temporary modifiers. */
    StatComponent getStats();

    /**
     * Current world position, used for range and area-of-effect checks.
     *
     * <p>Must be the <em>live</em> vector, not a defensive copy:
     * {@link AbilityResolver} relocates combatants by mutating it — Teleport moves
     * the actor, Eldritch Blast pushes its target — so an implementation that
     * returns a copy would silently swallow both.</p>
     */
    Vector2 getPosition();

    /** Which side this combatant fights for — see {@link #TEAM_AZURE}. */
    int getTeamIndex();

    /**
     * The live list of status effects currently attached to this combatant.
     *
     * <p>Must be mutable and must be the same list every call;
     * {@link StatusEffectEngine} adds and removes entries directly.</p>
     */
    List<StatusEffect> getActiveEffects();

    /**
     * Per-turn resource reset — movement budget, mana regeneration, and anything
     * else that refreshes when this combatant's turn begins.
     *
     * <p>Owned by the implementation rather than the engine because the client
     * resets a movement controller the server does not have.
     * {@link StatusEffectEngine#beginTurn} calls this last, and skips it entirely
     * if the combatant died to a status effect on the way in.</p>
     */
    void onTurnStart();

    /**
     * Race, or {@code null} when this combatant was created without one.
     *
     * <p>Defaulted because race selection is a later slice: existing code can
     * satisfy this interface today and start reporting a real race once the race
     * screen exists.</p>
     */
    default Race getRace() {
        return null;
    }

    /** True while this combatant still has HP. */
    default boolean isAlive() {
        return getStats() != null && getStats().isAlive();
    }

    /**
     * True when this combatant is at 0 HP.
     *
     * <p>"Downed" rather than "dead" deliberately — the Cleric's ultimate brings
     * these combatants back, so they remain valid targets and stay in the turn
     * queue's roster.</p>
     */
    default boolean isDowned() {
        return !isAlive();
    }

    /** True when {@code other} is on the same team. A combatant is its own ally. */
    default boolean isAllyOf(Combatant other) {
        return other != null && other.getTeamIndex() == getTeamIndex();
    }

    /** True when {@code other} is on the opposing team. */
    default boolean isEnemyOf(Combatant other) {
        return other != null && other.getTeamIndex() != getTeamIndex();
    }

    /** This combatant's abilities in action-bar order; index 0 is hotkey {@code [1]}. */
    default List<AbilityType> abilities() {
        return AbilityType.forClass(getCharacterClass());
    }

    /**
     * Turn-order score. Higher acts first.
     *
     * <p>The design document sorts strictly by Speed. {@link TurnQueue} uses the
     * server-assigned player ID as the deterministic tie-break.</p>
     */
    default int initiative() {
        return getStats() == null ? 0 : getStats().getSpeed();
    }
}
