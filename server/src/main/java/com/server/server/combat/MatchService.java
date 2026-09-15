// File Location: server/src/main/java/com/server/server/combat/MatchService.java
package com.server.server.combat;

import com.badlogic.gdx.math.Vector2;
import com.server.server.selection.LobbyPlayer;
import com.shared.shared.model.Action;
import com.shared.shared.model.Packet;
import com.shared.shared.model.world.Environment;
import com.shared.shared.network.MatchState;

import java.util.List;

/** Owns the current authoritative match and serializes access from WebSocket threads. */
public class MatchService {
    private AuthoritativeMatch match;

    public synchronized MatchState start(List<LobbyPlayer> players, Environment environment) {
        return start(players, environment, false);
    }

    public synchronized MatchState start(List<LobbyPlayer> players, Environment environment,
                                         boolean testingMode) {
        match = new AuthoritativeMatch(players, environment, testingMode);
        return match.snapshot();
    }

    public synchronized CombatCommandResult handle(int authenticatedPlayerId, Packet packet) {
        if (match == null) return CombatCommandResult.rejected("No match is running.");
        if (packet == null || packet.getID() != authenticatedPlayerId) {
            return CombatCommandResult.rejected("Player identity does not match this connection.");
        }

        Action action = packet.getAction();
        if (action == Action.MOVE) {
            return match.move(authenticatedPlayerId, copy(packet.getFinalPosition()));
        }
        if (action == Action.USE_ABILITY) {
            return match.useAbility(authenticatedPlayerId, packet.getAbility(),
                packet.getTargetPlayerID(), copy(packet.getTargetPosition()));
        }
        if (action == Action.END_TURN) return match.endTurn(authenticatedPlayerId);
        return CombatCommandResult.rejected("Unsupported combat command.");
    }

    public synchronized MatchState disconnect(int playerId) {
        return match == null ? null : match.disconnect(playerId);
    }

    public synchronized MatchState snapshot() {
        return match == null ? null : match.snapshot();
    }

    public synchronized boolean isRunning() {
        return match != null;
    }

    public synchronized boolean isComplete() {
        return match != null && match.snapshot().isMatchOver();
    }

    /** Replaces a completed match with pristine combat state on the same battlefield. */
    public synchronized MatchState restart(List<LobbyPlayer> players) {
        if (!isComplete()) throw new IllegalStateException("The current match is not over.");
        Environment environment = match.snapshot().getEnvironment();
        match = new AuthoritativeMatch(players, environment, match.snapshot().isTestingMode());
        return match.snapshot();
    }

    private Vector2 copy(Vector2 value) {
        return value == null ? null : new Vector2(value);
    }
}
