// File Location: server/src/main/java/com/server/server/matchmaking/RoomRegistry.java
package com.server.server.matchmaking;

import com.server.server.selection.EnvironmentVoteResolver;
import com.server.server.selection.LobbyPlayer;
import com.server.server.selection.LobbyStateService;
import com.shared.shared.model.Packet;
import com.shared.shared.network.MatchState;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Assigns sessions to compatible waiting rooms and owns their complete lifecycle. */
@Service
public class RoomRegistry {
    private final EnvironmentVoteResolver voteResolver;
    private final Map<String, MatchRoom> rooms = new LinkedHashMap<>();
    private final Map<String, String> sessionRooms = new LinkedHashMap<>();
    private int nextRoomNumber = 1;

    public RoomRegistry(EnvironmentVoteResolver voteResolver) {
        this.voteResolver = voteResolver;
    }

    public synchronized RoomAssignment assign(Packet selection, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("A WebSocket session is required.");
        }
        if (sessionRooms.containsKey(sessionId)) {
            throw new IllegalArgumentException("This connection has already joined a room.");
        }
        String selectionError = LobbyStateService.validateSelection(selection);
        if (selectionError != null) throw new IllegalArgumentException(selectionError);

        MatchRoom room = rooms.values().stream()
            .filter(candidate -> candidate.getLobby().validateJoin(selection) == null)
            .findFirst()
            .orElseGet(this::createRoom);

        LobbyPlayer player = room.getLobby().addPlayer(selection, sessionId);
        sessionRooms.put(sessionId, room.getId());
        return new RoomAssignment(room, player);
    }

    public synchronized Optional<MatchRoom> room(String roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }

    public synchronized Optional<MatchRoom> roomForSession(String sessionId) {
        return room(sessionRooms.get(sessionId));
    }

    public synchronized RoomDisconnectResult disconnect(String sessionId, int playerId) {
        String roomId = sessionRooms.remove(sessionId);
        MatchRoom room = rooms.get(roomId);
        if (room == null) return null;

        room.removeSession(sessionId);
        LobbyPlayer removed = room.getLobby().remove(playerId);
        if (removed == null || !removed.getSessionId().equals(sessionId)) return null;
        MatchState matchState = room.getMatches().disconnect(playerId);
        RoomDisconnectResult result = new RoomDisconnectResult(room, playerId, matchState);
        if (room.getLobby().size() == 0) rooms.remove(roomId);
        return result;
    }

    public synchronized int roomCount() {
        return rooms.size();
    }

    private MatchRoom createRoom() {
        MatchRoom room = new MatchRoom("room-" + nextRoomNumber++, voteResolver);
        rooms.put(room.getId(), room);
        return room;
    }
}
