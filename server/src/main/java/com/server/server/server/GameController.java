// File Location: server/src/main/java/com/server/server/server/GameController.java
package com.server.server.server;

import com.server.server.combat.CombatCommandResult;
import com.server.server.matchmaking.MatchRoom;
import com.server.server.matchmaking.RoomAssignment;
import com.server.server.matchmaking.RoomRegistry;
import com.server.server.matchmaking.RematchDecision;
import com.server.server.selection.LobbyPlayer;
import com.shared.shared.model.Action;
import com.shared.shared.model.Packet;
import com.shared.shared.model.world.Environment;
import com.shared.shared.network.MatchState;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.Optional;

/** WebSocket entry point with room-scoped selection, voting, and combat. */
@Controller
public class GameController {
    static final String PLAYER_ID_ATTRIBUTE = "ID";
    static final String ROOM_ID_ATTRIBUTE = "ROOM_ID";

    private final SimpMessageSendingOperations messages;
    private final RoomRegistry rooms;

    public GameController(SimpMessageSendingOperations messages, RoomRegistry rooms) {
        this.messages = messages;
        this.rooms = rooms;
    }

    @MessageMapping("/game.takeAction")
    public synchronized void takeAction(@Payload Packet packet,
                                        SimpMessageHeaderAccessor headers) {
        Optional<MatchRoom> assignedRoom = authenticatedRoom(headers);
        Object playerId = headers.getSessionAttributes().get(PLAYER_ID_ATTRIBUTE);
        if (assignedRoom.isEmpty() || !(playerId instanceof Integer)) return;

        CombatCommandResult result = assignedRoom.get().getMatches()
            .handle((Integer) playerId, packet);
        if (!result.isAccepted()) {
            sendPrivate(headers.getSessionId(), Packet.builder()
                .action(Action.ERROR).message(result.getError()).build());
            return;
        }
        broadcast(assignedRoom.get(), Packet.builder()
            .action(Action.MATCH_STATE)
            .matchState(result.getState())
            .build());
    }

    @MessageMapping("/game.joinGame")
    public synchronized void addPlayer(@Payload Packet packet,
                                       SimpMessageHeaderAccessor headers) {
        String sessionId = headers.getSessionId();
        final RoomAssignment assignment;
        try {
            assignment = rooms.assign(packet, sessionId);
        } catch (IllegalArgumentException exception) {
            sendPrivate(sessionId, Packet.builder()
                .action(Action.ERROR).message(exception.getMessage()).build());
            return;
        }

        MatchRoom room = assignment.getRoom();
        LobbyPlayer player = assignment.getPlayer();
        headers.getSessionAttributes().put(PLAYER_ID_ATTRIBUTE, player.getId());
        headers.getSessionAttributes().put(ROOM_ID_ATTRIBUTE, room.getId());

        // The client subscribes to the returned room topic, then acknowledges ROOM_READY.
        sendPrivate(sessionId, Packet.builder()
            .ID(player.getId())
            .roomId(room.getId())
            .testingMode(room.isTestingMode())
            .connectedPlayers(room.getLobby().size())
            .action(Action.PRIVATE_JOIN_CONFIRMATION)
            .build());
    }

    @MessageMapping("/game.roomReady")
    public synchronized void roomReady(@Payload Packet packet,
                                       SimpMessageHeaderAccessor headers) {
        Optional<MatchRoom> assignedRoom = authenticatedRoom(headers);
        Object playerId = headers.getSessionAttributes().get(PLAYER_ID_ATTRIBUTE);
        String sessionId = headers.getSessionId();
        if (assignedRoom.isEmpty() || !(playerId instanceof Integer)) return;

        MatchRoom room = assignedRoom.get();
        if (!room.markReady(sessionId)) return;

        for (LobbyPlayer existing : room.getLobby().players()) {
            if (existing.getId() != (Integer) playerId) {
                sendPrivate(sessionId,
                    withRoom(existing.toPacket(Action.PLAYER_COORDINATE, room.getLobby().size()), room));
            }
        }
        sendPrivate(sessionId, Packet.builder().action(Action.EOF).roomId(room.getId()).build());

        LobbyPlayer joined = room.getLobby().players().stream()
            .filter(player -> player.getId() == (Integer) playerId)
            .findFirst().orElse(null);
        if (joined != null) {
            broadcast(room, joined.toPacket(Action.JOIN, room.getLobby().size()));
            broadcastVoteUpdate(room);
            sendPrivate(sessionId, Packet.builder().action(Action.ROOM_READY)
                .ID(joined.getId()).roomId(room.getId()).testingMode(room.isTestingMode())
                .connectedPlayers(room.getLobby().size()).build());
        }
    }

    @MessageMapping("/game.startTestMatch")
    public synchronized void startTestMatch(@Payload Packet packet,
                                            SimpMessageHeaderAccessor headers) {
        Optional<MatchRoom> assignedRoom = authenticatedRoom(headers);
        Object playerId = headers.getSessionAttributes().get(PLAYER_ID_ATTRIBUTE);
        if (assignedRoom.isEmpty() || !(playerId instanceof Integer)
            || packet == null || packet.getID() != (Integer) playerId) return;
        MatchRoom room = assignedRoom.get();
        try {
            MatchState state = room.startTestMatch((Integer) playerId);
            broadcast(room, Packet.builder().action(Action.MATCH_START)
                .environment(state.getEnvironment()).matchState(state)
                .connectedPlayers(room.getLobby().size()).build());
        } catch (IllegalArgumentException exception) {
            sendPrivate(headers.getSessionId(), Packet.builder().action(Action.ERROR)
                .message(exception.getMessage()).build());
        }
    }

    @MessageMapping("/game.voteEnvironment")
    public synchronized void voteEnvironment(@Payload Packet packet,
                                             SimpMessageHeaderAccessor headers) {
        Optional<MatchRoom> assignedRoom = authenticatedRoom(headers);
        Object playerId = headers.getSessionAttributes().get(PLAYER_ID_ATTRIBUTE);
        if (assignedRoom.isEmpty() || !(playerId instanceof Integer)
            || ((Integer) playerId) != packet.getID()) return;

        MatchRoom room = assignedRoom.get();
        if (room.isTestingMode()) return;
        if (!room.isReady(headers.getSessionId())) return;
        try {
            room.getLobby().recordVote(packet.getID(), packet.getEnvironment());
        } catch (IllegalArgumentException exception) {
            sendPrivate(headers.getSessionId(), Packet.builder()
                .action(Action.ERROR).message(exception.getMessage()).build());
            return;
        }

        broadcastVoteUpdate(room);
        room.getLobby().startIfReady().ifPresent(environment -> {
            var initialState = room.getMatches().start(room.getLobby().players(), environment);
            Packet start = votePacket(room, Action.MATCH_START, environment);
            start.setMatchState(initialState);
            broadcast(room, start);
        });
    }

    @MessageMapping("/game.voteRematch")
    public synchronized void voteRematch(@Payload Packet packet,
                                         SimpMessageHeaderAccessor headers) {
        Optional<MatchRoom> assignedRoom = authenticatedRoom(headers);
        Object playerId = headers.getSessionAttributes().get(PLAYER_ID_ATTRIBUTE);
        if (assignedRoom.isEmpty() || !(playerId instanceof Integer)
            || packet == null || ((Integer) playerId) != packet.getID()) return;

        MatchRoom room = assignedRoom.get();
        RematchDecision decision = room.requestRematch((Integer) playerId);
        if (!decision.isAccepted()) {
            sendPrivate(headers.getSessionId(), Packet.builder()
                .action(Action.ERROR).message(decision.getError()).build());
            return;
        }

        broadcast(room, Packet.builder()
            .action(Action.REMATCH_UPDATE)
            .connectedPlayers(decision.getRequiredVotes())
            .rematchVotes(decision.getVotes())
            .message(decision.getVotes() + "/" + decision.getRequiredVotes()
                + " players are ready for a rematch.")
            .build());

        if (decision.isStarted()) {
            MatchState initialState = decision.getState();
            broadcast(room, Packet.builder()
                .action(Action.REMATCH_START)
                .environment(initialState.getEnvironment())
                .connectedPlayers(room.getLobby().size())
                .rematchVotes(0)
                .matchState(initialState)
                .build());
        }
    }

    private Optional<MatchRoom> authenticatedRoom(SimpMessageHeaderAccessor headers) {
        Object roomId = headers.getSessionAttributes().get(ROOM_ID_ATTRIBUTE);
        if (!(roomId instanceof String)) return Optional.empty();
        return rooms.roomForSession(headers.getSessionId())
            .filter(room -> room.getId().equals(roomId));
    }

    private void broadcastVoteUpdate(MatchRoom room) {
        broadcast(room, votePacket(room, Action.ENVIRONMENT_VOTE_UPDATE, null));
    }

    private Packet votePacket(MatchRoom room, Action action, Environment selected) {
        Map<Environment, Integer> counts = room.getLobby().voteCounts();
        return Packet.builder()
            .action(action)
            .roomId(room.getId())
            .environment(selected)
            .connectedPlayers(room.getLobby().size())
            .bogVotes(counts.get(Environment.BOG))
            .lavaVotes(counts.get(Environment.LAVA))
            .canyonVotes(counts.get(Environment.CANYON))
            .build();
    }

    private void broadcast(MatchRoom room, Packet packet) {
        messages.convertAndSend(room.destination(), withRoom(packet, room));
    }

    private Packet withRoom(Packet packet, MatchRoom room) {
        packet.setRoomId(room.getId());
        packet.setTestingMode(room.isTestingMode());
        return packet;
    }

    private void sendPrivate(String sessionId, Packet packet) {
        messages.convertAndSendToUser(sessionId, "/queue/private", packet,
            createHeaders(sessionId));
    }

    private MessageHeaders createHeaders(String sessionId) {
        SimpMessageHeaderAccessor accessor =
            SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        accessor.setSessionId(sessionId);
        accessor.setLeaveMutable(true);
        return accessor.getMessageHeaders();
    }
}
