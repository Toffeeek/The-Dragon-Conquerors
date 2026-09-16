package com.server.server.config;

import com.server.server.matchmaking.RoomRegistry;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import java.util.Set;

/** Authorizes broker frames as well as application commands. Native clients need no Origin. */
public final class GameChannelInterceptor implements ChannelInterceptor {
    private final RoomRegistry rooms;
    private static final Set<String> PUBLIC = Set.of("/app/game.joinGame", "/app/game.resume");
    private static final Set<String> MEMBER = Set.of("/app/game.takeAction", "/app/game.roomReady",
        "/app/game.startTestMatch", "/app/game.voteEnvironment", "/app/game.voteRematch", "/app/game.sync", "/app/game.leave");
    public GameChannelInterceptor(RoomRegistry rooms) { this.rooms = rooms; }

    @Override public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor headers = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (headers == null || headers.getCommand() == null) return message;
        var command = headers.getCommand();
        if (command != StompCommand.SEND && command != StompCommand.SUBSCRIBE) return message;
        if (message.getPayload() instanceof byte[] bytes && bytes.length > 16384) deny();
        if (headers.getFirstNativeHeader("selector") != null) deny();
        var attributes = headers.getSessionAttributes();
        if (attributes == null || headers.getSessionId() == null) deny();
        synchronized (attributes) {
            long now = System.nanoTime();
            long[] budget = (long[])attributes.computeIfAbsent("GAME_RATE", key -> new long[]{now, 0});
            if (now - budget[0] >= 10_000_000_000L) { budget[0] = now; budget[1] = 0; }
            if (++budget[1] > 60) deny();
        }
        String destination = headers.getDestination();
        if (destination == null) deny();
        var room = rooms.roomForSession(headers.getSessionId());
        if (command == StompCommand.SUBSCRIBE) {
            if (!"/user/queue/private".equals(destination)
                && (room.isEmpty() || !room.get().destination().equals(destination))) deny();
            // Exactly one private and one room subscription per transport session.
            synchronized (attributes) {
                @SuppressWarnings("unchecked")
                Set<String> subscribed = (Set<String>)attributes.computeIfAbsent("GAME_SUBSCRIPTIONS", key -> new java.util.HashSet<String>());
                if (!subscribed.add(destination)) deny();
            }
        } else if (!PUBLIC.contains(destination) && !(MEMBER.contains(destination) && room.isPresent())) deny();
        return message;
    }
    private static void deny() { throw new MessageDeliveryException("Frame not permitted."); }
}
