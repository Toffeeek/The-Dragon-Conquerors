// File Location: shared/src/main/java/com/shared/shared/model/combat/TurnQueue.java
package com.shared.shared.model.combat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Whose turn it is, and whose turn is next.
 *
 * <p>The design document says turn order is decided by Speed, highest first. This
 * class owns that ordering plus the round counter, and nothing else — it does not
 * know what an ability is and never touches HP. {@link StatusEffectEngine} handles
 * what happens <em>at</em> a turn's start; this decides <em>whose</em> it is.</p>
 *
 * <h2>Determinism</h2>
 *
 * <p>Four clients and one server all compute this order independently, so it must
 * come out identical everywhere or players will disagree about whose turn it is.
 * Two rules make that hold: initiative descending, then <b>player ID ascending</b>
 * as a tie-break. Sorting on any collection-dependent property — insertion order, a
 * hash — would let two machines produce different orders from the same combatants.
 * The ID tie-break is arbitrary but it is arbitrary <em>in the same way</em> for
 * everyone.</p>
 *
 * <h2>When the order is recomputed</h2>
 *
 * <p>Only at the start of a round. Sub-zero lowers Speed, so initiative changes
 * mid-match; re-sorting after every single turn would let a character act twice in
 * a row or be skipped entirely by shuffling underneath the cursor. Fixing the order
 * for the length of a round keeps it predictable and still lets a slow debuff
 * matter — from the next round onward.</p>
 */
public final class TurnQueue {

    /** Returned by {@link #winningTeam()} while the match is still undecided. */
    public static final int NO_WINNER = 0;

    /**
     * Initiative descending, player ID ascending. See the class notes on
     * determinism for why the tie-break is mandatory rather than cosmetic.
     */
    private static final Comparator<Combatant> ROUND_ORDER =
        Comparator.<Combatant>comparingInt(Combatant::initiative)
            .reversed()
            .thenComparingInt(Combatant::getId);

    /**
     * Everyone in the match, including the downed — the Cleric's ultimate revives
     * them, so they are not removed from the roster when they fall.
     */
    private final List<Combatant> participants = new ArrayList<>();

    /** This round's acting order, rebuilt from the roster at each round start. */
    private final List<Combatant> order = new ArrayList<>();

    /**
     * Position within {@link #order}.
     *
     * <p>Starts at -1 so the first {@link #advance()} lands on index 0 without
     * wrapping, which would otherwise bump the round counter before round 1 had
     * even begun.</p>
     */
    private int index = -1;

    private int roundNumber = 1;

    public TurnQueue(Collection<? extends Combatant> combatants) {
        if (combatants != null) {
            for (Combatant combatant : combatants) {
                if (combatant != null) participants.add(combatant);
            }
        }
        sortIntoRoundOrder();
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Advancing
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Hands the turn to the next combatant that can act.
     *
     * <p>Skips the downed, and starts a new round when it runs off the end.</p>
     *
     * @return the combatant now holding the turn, or {@code null} when nobody in
     *         the queue is still standing — which is a finished match, not an error
     */
    public Combatant advance() {
        if (order.isEmpty() && participants.isEmpty()) return null;

        // Bounded rather than "loop until someone is alive": with every combatant
        // downed there is no valid answer, and a bare while(true) would hang the
        // render thread. The bound covers a full pass of the current order plus a
        // full pass of a freshly rebuilt one, which is every candidate there is.
        int limit = Math.max(order.size(), participants.size()) * 2 + 2;
        for (int examined = 0; examined < limit; examined++) {
            index++;
            if (index >= order.size()) beginNewRound();
            if (order.isEmpty()) return null;

            Combatant next = order.get(index);
            if (next.isAlive()) return next;
        }

        return null;
    }

    /** The combatant currently holding the turn, or {@code null} before the first turn. */
    public Combatant current() {
        if (index < 0 || index >= order.size()) return null;
        return order.get(index);
    }

    /** Rounds elapsed, starting at 1. Useful for the HUD and for logging. */
    public int getRoundNumber() {
        return roundNumber;
    }

    /** True when the current turn is the first of a new round. */
    public boolean isRoundStart() {
        return index == 0;
    }

    private void beginNewRound() {
        roundNumber++;
        sortIntoRoundOrder();
        index = 0;
    }

    /**
     * Rebuilds {@link #order} from the roster.
     *
     * <p>Everyone is included, downed combatants too — {@link #advance()} skips
     * them, and keeping them in the list means a revived ally slots back into the
     * position its Speed earns rather than the end of the queue.</p>
     */
    private void sortIntoRoundOrder() {
        order.clear();
        order.addAll(participants);
        order.sort(ROUND_ORDER);
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Roster
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Adds a combatant to the roster; it starts taking turns next round.
     *
     * <p>Deferred deliberately — splicing someone into the middle of a round in
     * progress would either give them a free turn or skip whoever they displaced.
     * It also means a late-joining player cannot act before the round they joined
     * in has finished resolving.</p>
     */
    public void add(Combatant combatant) {
        if (combatant == null || participants.contains(combatant)) return;
        participants.add(combatant);
    }

    /**
     * Removes a combatant entirely — a disconnect, not a knockout. A downed
     * combatant stays in the queue so it can be revived.
     *
     * <p>Adjusts {@link #index} when the removal happened at or before the current
     * position, so the combatant that shifts into the gap is not skipped.</p>
     */
    public boolean remove(Combatant combatant) {
        if (combatant == null) return false;

        boolean removed = participants.remove(combatant);

        int at = order.indexOf(combatant);
        if (at >= 0) {
            order.remove(at);
            if (at <= index) index--;
            removed = true;
        }

        return removed;
    }

    /** Everyone in the match, downed included. Unmodifiable. */
    public List<Combatant> getParticipants() {
        return Collections.unmodifiableList(participants);
    }

    /** This round's acting order, first to last. Unmodifiable. */
    public List<Combatant> getOrder() {
        return Collections.unmodifiableList(order);
    }

    public int size() {
        return participants.size();
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Match state
    // ──────────────────────────────────────────────────────────────────────

    /** Combatants on a team that are still standing. */
    public List<Combatant> livingOnTeam(int teamIndex) {
        List<Combatant> living = new ArrayList<>();
        for (Combatant combatant : participants) {
            if (combatant.getTeamIndex() == teamIndex && combatant.isAlive()) {
                living.add(combatant);
            }
        }
        return living;
    }

    /**
     * True once at most one team has anyone left standing.
     *
     * <p>Safe despite the Cleric's Revive: a team with every member downed has
     * nobody able to take the turn that would cast it, so the state is terminal.</p>
     */
    public boolean isMatchOver() {
        return teamsStillFighting().size() <= 1;
    }

    /**
     * The winning team's index, or {@link #NO_WINNER} while the match is still
     * running — and also for a mutual wipe, where nobody won.
     */
    public int winningTeam() {
        Set<Integer> alive = teamsStillFighting();
        if (alive.size() != 1) return NO_WINNER;
        return alive.iterator().next();
    }

    private Set<Integer> teamsStillFighting() {
        Set<Integer> teams = new HashSet<>();
        for (Combatant combatant : participants) {
            if (combatant.isAlive()) teams.add(combatant.getTeamIndex());
        }
        return teams;
    }

    /** Combat-log line: {@code "Round 3: Wraith(14) > Archer(12) > Paladin(9)"}. */
    public String describeOrder() {
        StringBuilder builder = new StringBuilder("Round ").append(roundNumber).append(": ");
        for (int i = 0; i < order.size(); i++) {
            Combatant combatant = order.get(i);
            if (i > 0) builder.append(" > ");
            builder.append(combatant.getUsername())
                .append('(').append(combatant.initiative()).append(')');
            if (combatant.isDowned()) builder.append("[down]");
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return describeOrder();
    }
}
