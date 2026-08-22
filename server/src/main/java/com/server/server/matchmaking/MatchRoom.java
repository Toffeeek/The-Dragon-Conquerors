// File Location: server/src/main/java/com/server/server/matchmaking/MatchRoom.java
package com.server.server.matchmaking;

import com.server.server.combat.MatchService;
import com.server.server.selection.EnvironmentVoteResolver;
import com.server.server.selection.LobbyStateService;

import java.util.HashSet;
import java.util.Set;

/** Isolated selection lobby and authoritative match for one four-player party. */
public final class MatchRoom {
    private final String id;
    private final LobbyStateService lobby;
    private final MatchService matches = new MatchService();
    private final Set<String> readySessions = new HashSet<>();

    MatchRoom(String id, EnvironmentVoteResolver voteResolver) {
        this.id = id;
        this.lobby = new LobbyStateService(voteResolver);
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

    public synchronized void removeSession(String sessionId) {
        readySessions.remove(sessionId);
    }
}
