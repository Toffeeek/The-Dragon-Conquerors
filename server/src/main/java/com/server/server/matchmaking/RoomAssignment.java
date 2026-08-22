// File Location: server/src/main/java/com/server/server/matchmaking/RoomAssignment.java
package com.server.server.matchmaking;

import com.server.server.selection.LobbyPlayer;

/** Result of atomically assigning a WebSocket session to a waiting room. */
public final class RoomAssignment {
    private final MatchRoom room;
    private final LobbyPlayer player;

    RoomAssignment(MatchRoom room, LobbyPlayer player) {
        this.room = room;
        this.player = player;
    }

    public MatchRoom getRoom() {
        return room;
    }

    public LobbyPlayer getPlayer() {
        return player;
    }
}
