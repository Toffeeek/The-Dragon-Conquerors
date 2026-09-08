// File Location: server/src/main/java/com/server/server/matchmaking/MatchRoom.java
package com.server.server.matchmaking;

import com.server.server.combat.MatchService;
import com.server.server.selection.EnvironmentVoteResolver;
import com.server.server.selection.LobbyStateService;
import com.shared.shared.network.MatchState;
import com.shared.shared.model.world.Environment;

import java.util.HashSet;
import java.util.Set;

/** Isolated selection lobby and authoritative match for a party of up to four players. */
public final class MatchRoom {
    private final String id;
    private final boolean testingMode;
    private final LobbyStateService lobby;
    private final MatchService matches = new MatchService();
    private final Set<String> readySessions = new HashSet<>();
    private final Set<Integer> rematchVotes = new HashSet<>();

    MatchRoom(String id, EnvironmentVoteResolver voteResolver) {
        this(id, voteResolver, false);
    }

    MatchRoom(String id, EnvironmentVoteResolver voteResolver, boolean testingMode) {
        this.id = id;
        this.testingMode = testingMode;
        this.lobby = new LobbyStateService(voteResolver);
    }

    public boolean isTestingMode() { return testingMode; }

    public synchronized MatchState startTestMatch(int playerId) {
        if (!testingMode) throw new IllegalArgumentException("Testing mode is disabled.");
        if (!lobby.contains(playerId)) throw new IllegalArgumentException("Join before starting a test.");
        if (matches.isRunning()) throw new IllegalArgumentException("A match is already in progress.");
        if (lobby.players().stream().anyMatch(player -> !isReady(player.getSessionId()))) {
            throw new IllegalArgumentException("A player is still joining. Try Start Test again shortly.");
        }
        lobby.startTesting();
        return matches.start(lobby.players(), Environment.CANYON, true);
    }

    public String getId() {
        return id;
    }

    public String destination() {
        return "/match/rooms/" + id;
    }

    public LobbyStateService getLobby() {
        return lobby;
    }

    public MatchService getMatches() {
        return matches;
    }

    public synchronized boolean markReady(String sessionId) {
        return readySessions.add(sessionId);
    }

    public synchronized boolean isReady(String sessionId) {
        return readySessions.contains(sessionId);
    }

    public synchronized void removePlayer(int playerId, String sessionId) {
        readySessions.remove(sessionId);
        rematchVotes.remove(playerId);
        if (testingMode) rematchVotes.clear();
    }

    public synchronized RematchDecision requestRematch(int playerId) {
        int required = lobby.size();
        if (!lobby.contains(playerId)) {
            return RematchDecision.rejected("Unknown player.", rematchVotes.size(), required);
        }
        if (!matches.isComplete()) {
            return RematchDecision.rejected("The match is still in progress.",
                rematchVotes.size(), required);
        }
        if (!testingMode && required != LobbyStateService.MAX_PLAYERS) {
            return RematchDecision.rejected(
                "A 2v2 rematch requires all four players to remain connected.",
                rematchVotes.size(), required);
        }

        rematchVotes.add(playerId);
        int votes = rematchVotes.size();
        if (votes < required) return RematchDecision.waiting(votes, required);

        MatchState state = matches.restart(lobby.players());
        rematchVotes.clear();
        return RematchDecision.started(votes, state);
    }

    public synchronized int rematchVoteCount() {
        return rematchVotes.size();
    }
}
