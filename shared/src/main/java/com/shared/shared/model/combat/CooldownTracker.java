// File Location: shared/src/main/java/com/shared/shared/model/combat/CooldownTracker.java
package com.shared.shared.model.combat;

import com.shared.shared.model.ability.Ability;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One combatant's ability cooldowns.
 *
 * <p>Every ultimate in the game has a cooldown, and several secondary abilities
 * do too ({@code Curse} 3 turns, {@code Inspiring Verse} 2). This class owns that
 * countdown and nothing else, so the turn engine, the HUD and the server's
 * validation all read cooldown state the same way.</p>
 *
 * <h2>Timing</h2>
 *
 * <p>{@link #tick()} is called once at the start of the owner's turn, not once per
 * global turn — a 4-turn cooldown means "four of <em>your</em> turns", which is
 * what a player intuitively expects and what keeps a slow character's ultimate
 * from being punished twice for low Speed.</p>
 *
 * <h2>On the map key</h2>
 *
 * <p>Keyed on {@link Ability} rather than a name string so a typo cannot silently
 * create a second, separate cooldown. The built-in catalogue is an enum, so
 * identity comparison is exact. A hand-written {@code Ability} implementation must
 * therefore be a singleton, or implement {@code equals}/{@code hashCode} — the
 * same requirement any map key has.</p>
 */
public final class CooldownTracker {

    /**
     * Insertion-ordered so {@link #snapshot()} and debug output list cooldowns in
     * the order they were triggered, which makes a log easier to read than the
     * arbitrary order a {@code HashMap} would give.
     */
    private final Map<Ability, Integer> remaining = new LinkedHashMap<>();

    /**
     * Puts an ability on cooldown for its full duration.
     *
     * <p>Abilities with no cooldown are ignored rather than stored with a zero,
     * so {@link #isEmpty()} means what it says.</p>
     */
    public void start(Ability ability) {
        if (ability == null) return;

        int turns = ability.getCooldownTurns();
        if (turns <= 0) return;

        remaining.put(ability, turns);
    }

    /** True when the ability is off cooldown and may be used this turn. */
    public boolean isReady(Ability ability) {
        return remainingTurns(ability) <= 0;
    }

    /** Turns left before the ability is usable again; 0 when it is ready now. */
    public int remainingTurns(Ability ability) {
        if (ability == null) return 0;

        Integer turns = remaining.get(ability);
        return turns == null ? 0 : turns;
    }

    /**
     * Advances every cooldown by one turn, dropping the ones that finish.
     *
     * <p>Call once at the start of the owner's turn.</p>
     */
    public void tick() {
        Iterator<Map.Entry<Ability, Integer>> entries = remaining.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<Ability, Integer> entry = entries.next();
            int turnsLeft = entry.getValue() - 1;
            if (turnsLeft <= 0) {
                entries.remove();
            } else {
                entry.setValue(turnsLeft);
            }
        }
    }

    /**
     * Clears one ability's cooldown immediately.
     *
     * <p>This is the Bard's ultimate, Encore: "restore teammate's ult", which is
     * implemented as resetting the teammate's ultimate cooldown rather than as a
     * special case inside the resolver.</p>
     */
    public void reset(Ability ability) {
        if (ability != null) remaining.remove(ability);
    }

    /** Clears every cooldown — used when a match ends or a combatant is rebuilt. */
    public void clear() {
        remaining.clear();
    }

    /** True when nothing is on cooldown. */
    public boolean isEmpty() {
        return remaining.isEmpty();
    }

    /** Read-only view of the live cooldowns, for the HUD to grey out action buttons. */
    public Map<Ability, Integer> active() {
        return Collections.unmodifiableMap(remaining);
    }

    /**
     * Name-keyed copy for logging and network sync.
     *
     * <p>A plain {@code Map<String, Integer>} serialises cleanly over STOMP,
     * whereas an {@code Ability}-keyed map does not.</p>
     */
    public Map<String, Integer> snapshot() {
        Map<String, Integer> copy = new LinkedHashMap<>();
        for (Map.Entry<Ability, Integer> entry : remaining.entrySet()) {
            copy.put(entry.getKey().getDisplayName(), entry.getValue());
        }
        return copy;
    }

    @Override
    public String toString() {
        return "CooldownTracker" + snapshot();
    }
}
