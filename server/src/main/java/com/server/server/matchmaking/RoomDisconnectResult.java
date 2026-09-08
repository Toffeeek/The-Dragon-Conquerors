// File Location: server/src/main/java/com/server/server/matchmaking/RoomDisconnectResult.java
package com.server.server.matchmaking;

import com.shared.shared.network.MatchState;

/** Room-scoped information needed to notify peers after a disconnect. */
public final class RoomDisconnectResult {
    private final MatchRoom room;
    private final int playerId;
    private final MatchState matchState;

    RoomDisconnectResult(MatchRoom room, int playerId, MatchState matchState) {
        this.room = room;
        this.playerId = playerId;
        this.matchState = matchState;
    }

    public MatchRoom getRoom() {
        return room;
    }

    public int getPlayerId() {
        return playerId;
    }

    public MatchState getMatchState() {
        return matchState;
    }
}
