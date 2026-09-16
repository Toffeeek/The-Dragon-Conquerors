// File Location: server/src/main/java/com/server/server/matchmaking/RoomRegistry.java
package com.server.server.matchmaking;

import com.server.server.selection.EnvironmentVoteResolver;
import com.server.server.selection.LobbyPlayer;
import com.server.server.selection.LobbyStateService;
import com.shared.shared.model.Packet;
import com.shared.shared.network.MatchState;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Assigns sessions to compatible waiting rooms and owns their complete lifecycle. */
@Service
public class RoomRegistry {
    private final EnvironmentVoteResolver voteResolver;
    private final boolean testingMode;
    private final java.util.function.LongSupplier clock;
    private final Map<String, MatchRoom> rooms = new LinkedHashMap<>();
    private final Map<String, String> sessionRooms = new LinkedHashMap<>();
    private int nextRoomNumber = 1;
    private static final long RECONNECT_NANOS = java.util.concurrent.TimeUnit.SECONDS.toNanos(30);
    private static final class Lease {
        String sessionId;
        final String roomId;
        final int playerId;
        long expires = Long.MAX_VALUE;
        Lease(String sessionId, String roomId, int playerId) {
            this.sessionId = sessionId; this.roomId = roomId; this.playerId = playerId;
        }
    }
    private final Map<String, Lease> leases = new LinkedHashMap<>();

    public RoomRegistry(EnvironmentVoteResolver voteResolver) {
        this(voteResolver, false);
    }

    @Autowired
    public RoomRegistry(EnvironmentVoteResolver voteResolver,
                        @Value("${game.testing-mode:false}") boolean testingMode) {
        this(voteResolver, testingMode, System::nanoTime);
    }

    RoomRegistry(EnvironmentVoteResolver voteResolver, boolean testingMode, java.util.function.LongSupplier clock) {
        this.voteResolver = voteResolver;
        this.testingMode = testingMode;
        this.clock = clock;
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

        synchronized (room) {
            LobbyPlayer player = room.getLobby().addPlayer(selection, sessionId);
            sessionRooms.put(sessionId, room.getId());
            leases.put(java.util.UUID.randomUUID().toString(), new Lease(sessionId, room.getId(), player.getId()));
            return new RoomAssignment(room, player);
        }
    }

    public synchronized Optional<MatchRoom> room(String roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }

    public synchronized Optional<MatchRoom> roomForSession(String sessionId) {
        return room(sessionRooms.get(sessionId));
    }

    public synchronized RoomDisconnectResult disconnect(String sessionId, int playerId) {
        leases.values().removeIf(lease -> lease.sessionId.equals(sessionId));
        String roomId = sessionRooms.remove(sessionId);
        MatchRoom room = rooms.get(roomId);
        if (room == null) return null;

        synchronized (room) {
            room.removePlayer(playerId, sessionId);
            LobbyPlayer removed = room.getLobby().remove(playerId);
            if (removed == null || !removed.getSessionId().equals(sessionId)) return null;
            MatchState matchState = room.getMatches().disconnect(playerId);
            RoomDisconnectResult result = new RoomDisconnectResult(room, playerId, matchState);
            if (room.getLobby().size() == 0) rooms.remove(roomId);
            return result;
        }
    }

    public synchronized int roomCount() {
        return rooms.size();
    }

    public synchronized String resumeToken(String sessionId) {
        return leases.entrySet().stream().filter(entry -> entry.getValue().sessionId.equals(sessionId))
            .map(Map.Entry::getKey).findFirst().orElse(null);
    }

    public synchronized void suspend(String sessionId) {
        leases.values().stream().filter(lease -> lease.sessionId.equals(sessionId)).forEach(lease -> {
            // Duplicate disconnect notifications must not extend the grace period.
            if (lease.expires == Long.MAX_VALUE) lease.expires = clock.getAsLong() + RECONNECT_NANOS;
            MatchRoom room = rooms.get(lease.roomId);
            if (room != null) {
                room.suspendSession(sessionId);
                room.getMatches().setConnected(lease.playerId, false);
            }
        });
    }

    public synchronized RoomAssignment resume(String token, String sessionId) {
        Lease lease = leases.get(token);
        if (sessionId == null || sessionId.isBlank() || sessionRooms.containsKey(sessionId)
            || lease == null || lease.expires < clock.getAsLong()) {
            throw new IllegalArgumentException("The reconnect window expired. Return to the menu to join a new game.");
        }
        MatchRoom room = rooms.get(lease.roomId);
        if (room == null) throw new IllegalArgumentException("The room is no longer available.");
        LobbyPlayer player = room.getLobby().players().stream().filter(p -> p.getId() == lease.playerId).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("The player is no longer in this room."));
        sessionRooms.remove(lease.sessionId);
        room.suspendSession(lease.sessionId);
        player.rebindSession(sessionId);
        room.getMatches().setConnected(lease.playerId, true);
        lease.sessionId = sessionId;
        lease.expires = Long.MAX_VALUE;
        sessionRooms.put(sessionId, room.getId());
        leases.remove(token);
        leases.put(java.util.UUID.randomUUID().toString(), lease);
        return new RoomAssignment(room, player);
    }

    public synchronized java.util.List<RoomDisconnectResult> expireDisconnected() {
        var expired = leases.values().stream().filter(lease -> lease.expires < clock.getAsLong()).toList();
        var results = new java.util.ArrayList<RoomDisconnectResult>();
        for (Lease lease : expired) {
            var result = disconnect(lease.sessionId, lease.playerId);
            if (result != null) results.add(result);
        }
        return results;
    }

    private MatchRoom createRoom() {
        MatchRoom room = new MatchRoom("room-" + nextRoomNumber++, voteResolver, testingMode);
        rooms.put(room.getId(), room);
        return room;
    }
}
