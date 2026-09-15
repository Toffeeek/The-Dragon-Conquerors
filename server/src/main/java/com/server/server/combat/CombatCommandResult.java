// File Location: server/src/main/java/com/server/server/combat/CombatCommandResult.java
package com.server.server.combat;

import com.shared.shared.network.MatchState;

/** Accepted authoritative state or a player-facing rejection message. */
public final class CombatCommandResult {
    private final MatchState state;
    private final String error;

    private CombatCommandResult(MatchState state, String error) {
        this.state = state;
        this.error = error;
    }

    public static CombatCommandResult accepted(MatchState state) {
        return new CombatCommandResult(state, null);
    }

    public static CombatCommandResult rejected(String error) {
        return new CombatCommandResult(null, error);
    }

    public boolean isAccepted() { return state != null; }
    public MatchState getState() { return state; }
    public String getError() { return error; }
}
