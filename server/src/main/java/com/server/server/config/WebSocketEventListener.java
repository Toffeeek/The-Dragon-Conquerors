// File Location: server/src/main/java/com/server/server/config/WebSocketEventListener.java
package com.server.server.config;

import com.server.server.matchmaking.MatchRoom;
import com.server.server.matchmaking.RoomDisconnectResult;
import com.server.server.matchmaking.RoomRegistry;
import com.shared.shared.model.Action;
import com.shared.shared.model.Packet;
import com.shared.shared.model.world.Environment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

/** Removes disconnected sessions from only their assigned room. */
@Component
@RequiredArgsConstructor
@Slf4j
@org.springframework.scheduling.annotation.EnableScheduling
public class WebSocketEventListener {
    private final SimpMessageSendingOperations messages;
    private final RoomRegistry rooms;

    @EventListener
    public void handleWebSocketListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        if (headers.getSessionAttributes() == null) return;
        Object playerId = headers.getSessionAttributes().get("ID");
        if (!(playerId instanceof Integer) || (Integer) playerId < 0) return;

        rooms.suspend(headers.getSessionId());
        rooms.roomForSession(headers.getSessionId()).ifPresent(room -> {
            var state = room.getMatches().snapshot();
            if (state != null) messages.convertAndSend(room.destination(), Packet.builder()
                .action(Action.MATCH_STATE).roomId(room.getId()).matchState(state).build());
        });
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 1000)
    public void expireDisconnected() {
        rooms.expireDisconnected().forEach(this::broadcastDeparture);
    }

    public void broadcastDeparture(RoomDisconnectResult result) {
        if (result == null) return;
        int playerId = result.getPlayerId();

        MatchRoom room = result.getRoom();
        log.info("Player {} disconnected from {}", playerId, room.getId());
        messages.convertAndSend(room.destination(), Packet.builder()
            .action(Action.LEAVE)
            .roomId(room.getId())
            .ID((Integer) playerId)
            .build());

        if (result.getMatchState() != null) {
            messages.convertAndSend(room.destination(), Packet.builder()
                .action(Action.MATCH_STATE)
                .roomId(room.getId())
                .matchState(result.getMatchState())
                .build());

            if (result.getMatchState().isMatchOver()) {
                messages.convertAndSend(room.destination(), Packet.builder()
                    .action(Action.REMATCH_UPDATE)
                    .ID((Integer) playerId)
                    .roomId(room.getId())
                    .connectedPlayers(room.getLobby().size())
                    .rematchVotes(room.rematchVoteCount())
                    .testingMode(room.isTestingMode())
                    .message(room.isTestingMode()
                        ? "The remaining players can vote to restart the test."
                        : "A 2v2 rematch requires all four players to remain connected.")
                    .build());
            }
        }

        Map<Environment, Integer> counts = room.getLobby().voteCounts();
        messages.convertAndSend(room.destination(), Packet.builder()
            .action(Action.ENVIRONMENT_VOTE_UPDATE)
            .roomId(room.getId())
            .connectedPlayers(room.getLobby().size())
            .bogVotes(counts.get(Environment.BOG))
            .lavaVotes(counts.get(Environment.LAVA))
            .canyonVotes(counts.get(Environment.CANYON))
            .build());
    }
}
