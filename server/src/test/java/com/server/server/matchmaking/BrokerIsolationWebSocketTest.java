package com.server.server.matchmaking;

import com.client.client.NetworkClient;
import com.badlogic.gdx.math.Vector2;
import com.shared.shared.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import java.lang.reflect.Type;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Exercise the real inbound channel, not just the interceptor in isolation. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "game.testing-mode=true")
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class BrokerIsolationWebSocketTest {
    @LocalServerPort int port;

    @Test void anotherRoomCannotSubscribeToAnActiveMatch() throws Exception {
        var packets = new LinkedBlockingQueue<Packet>();
        var owner = new NetworkClient(url());
        owner.setPacketHandler(packets::add);
        try {
            owner.connect(); owner.join("Owner", new Vector2(), 1, CharacterClass.ARCHER, Race.HUMAN);
            Packet joined = await(packets, Action.PRIVATE_JOIN_CONFIRMATION);
            await(packets, Action.ROOM_READY);
            owner.startTestMatch(joined.getID()); await(packets, Action.MATCH_START);
            try (var stranger = new Peer()) {
                stranger.session.send("/app/game.joinGame", Packet.builder().action(Action.JOIN)
                    .username("Other room").teamIndex(1).characterClass(CharacterClass.MAGE).race(Race.HUMAN).build());
                Packet other = await(stranger.packets, Action.PRIVATE_JOIN_CONFIRMATION);
                assertNotEquals(joined.getRoomId(), other.getRoomId());
                stranger.session.subscribe("/match/rooms/" + other.getRoomId(), stranger.frames);
                stranger.session.send("/app/game.roomReady", Packet.builder().action(Action.ROOM_READY).build());
                await(stranger.packets, Action.ROOM_READY); // Own-room subscription is allowed.
                stranger.session.subscribe("/match/rooms/" + joined.getRoomId(), stranger.frames);
                // Ordered inbound dispatch drops rejected frames instead of emitting ERROR.
                // A subsequent own-room sync is the processing barrier for the rejected subscribe.
                stranger.session.send("/app/game.sync", Packet.builder().action(Action.SYNC).build());
                await(stranger.packets, Action.ROOM_READY);
                owner.endTurn(joined.getID());
                assertNotNull(await(packets, Action.MATCH_STATE).getMatchState());
                assertNull(stranger.packets.poll(300, TimeUnit.MILLISECONDS), "Foreign room state leaked");
            }
            owner.endTurn(joined.getID());
            assertNotNull(await(packets, Action.MATCH_STATE).getMatchState());
        } finally { owner.disconnect(); }
    }

    @Test void clientCannotPublishForgedBrokerState() throws Exception {
        var packets = new LinkedBlockingQueue<Packet>();
        var owner = new NetworkClient(url()); owner.setPacketHandler(packets::add);
        try {
            owner.connect(); owner.join("Observer", new Vector2(), 1, CharacterClass.ARCHER, Race.HUMAN);
            Packet joined = await(packets, Action.PRIVATE_JOIN_CONFIRMATION); await(packets, Action.ROOM_READY);
            owner.startTestMatch(joined.getID()); await(packets, Action.MATCH_START);
            try (var stranger = new Peer()) {
                stranger.session.send("/match/rooms/" + joined.getRoomId(), Packet.builder().roomId(joined.getRoomId())
                    .action(Action.MATCH_STATE).message("FORGED").build());
                // The subsequent accepted join proves the preceding SEND has been processed.
                stranger.session.send("/app/game.joinGame", Packet.builder().action(Action.JOIN)
                    .username("Barrier").teamIndex(1).characterClass(CharacterClass.MAGE).race(Race.HUMAN).build());
                await(stranger.packets, Action.PRIVATE_JOIN_CONFIRMATION);
                owner.endTurn(joined.getID());
                assertNotNull(await(packets, Action.MATCH_STATE).getMatchState(), "Forged state reached the room");
            }
        } finally { owner.disconnect(); }
    }

    private String url() { return "ws://localhost:" + port + "/ws"; }
    private final class Peer implements AutoCloseable {
        final BlockingQueue<Packet> packets = new LinkedBlockingQueue<>();
        final CompletableFuture<String> rejected = new CompletableFuture<>();
        final WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        final StompSession session;
        final StompFrameHandler frames = new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders headers) { return Packet.class; }
            @Override public void handleFrame(StompHeaders headers, Object payload) { packets.add((Packet)payload); }
        };
        Peer() throws Exception {
            client.setMessageConverter(new org.springframework.messaging.converter.CompositeMessageConverter(java.util.List.of(
                new org.springframework.messaging.converter.ByteArrayMessageConverter(), new JacksonJsonMessageConverter())));
            session = client.connectAsync(url(), new StompSessionHandlerAdapter() {
                @Override public Type getPayloadType(StompHeaders headers) { return byte[].class; }
                @Override public void handleFrame(StompHeaders headers, Object payload) { rejected.complete("ERROR"); }
                @Override public void handleException(StompSession session, StompCommand command,
                    StompHeaders headers, byte[] payload, Throwable error) { rejected.completeExceptionally(error); }
                @Override public void handleTransportError(StompSession session, Throwable error) {
                    rejected.completeExceptionally(error);
                }
            }).get(8, TimeUnit.SECONDS);
            session.subscribe("/user/queue/private", frames);
        }
        @Override public void close() {
            if (session.isConnected()) {
                session.send("/app/game.leave", Packet.builder().action(Action.LEAVE).build());
                session.disconnect();
            }
            client.stop();
        }
    }
    private static Packet await(BlockingQueue<Packet> packets, Action action) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            Packet packet = packets.poll(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
            assertNotNull(packet, "No " + action);
            assertNotEquals(Action.ERROR, packet.getAction(), packet.getMessage());
            if (packet.getAction() == action) return packet;
        }
        throw new AssertionError("No " + action);
    }
}
