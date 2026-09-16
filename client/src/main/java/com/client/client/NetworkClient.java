package com.client.client;

import com.badlogic.gdx.math.Vector2;
import com.shared.shared.model.*;
import com.shared.shared.model.ability.AbilityType;
import com.shared.shared.model.world.Environment;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Bounded transport lifecycle. Recovery reads a snapshot; it never replays commands. */
public class NetworkClient {
    public enum State { CONNECTING, CONNECTED, RECOVERING, FAILED, CLOSED }
    private final String url;
    private WebSocketStompClient stompClient;
    private ThreadPoolTaskScheduler scheduler;
    private volatile StompSession session;
    private volatile Consumer<Packet> packetHandler;
    private volatile State state = State.CLOSED;
    private volatile String failure = "";
    private volatile long pendingSince;
    private volatile long generation;
    private volatile String roomId, resumeToken;
    private Packet lastJoin;
    private boolean roomSubscribed;

    public NetworkClient(String url) { this.url = url; }
    public void setPacketHandler(Consumer<Packet> handler) { packetHandler = handler; }
    public State getState() { return state; }
    public String getFailure() { return failure; }
    public boolean isReady() { return state == State.CONNECTED; }
    public String getResumeToken() { return resumeToken; }

    public synchronized void connect() throws Exception { open(false); }
    private void open(boolean recovering) throws Exception {
        closeTransport();
        state = recovering ? State.RECOVERING : State.CONNECTING;
        roomId = null;
        failure = "";
        long attempt = generation;
        java.util.concurrent.CompletableFuture<StompSession> connecting = null;
        try {
        scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setDaemon(true);
        scheduler.setThreadNamePrefix("tdc-heartbeat-");
        scheduler.initialize();
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setTaskScheduler(scheduler);
        stompClient.setDefaultHeartbeat(new long[]{5000, 5000});
        stompClient.setMessageConverter(new JacksonJsonMessageConverter());
        connecting = stompClient.connectAsync(url, new StompSessionHandlerAdapter() {
            @Override public void handleTransportError(StompSession transport, Throwable error) {
                if (attempt == generation) fail("Connection lost. Retry within 30 seconds to resume, or return to menu.");
            }
            @Override public void handleException(StompSession transport, StompCommand command,
                StompHeaders headers, byte[] payload, Throwable error) {
                if (attempt == generation) fail("The server returned an unreadable response. Retry to synchronize.");
            }
            @Override public void handleFrame(StompHeaders headers, Object payload) {
                if (attempt == generation) fail("The server closed the connection. Retry or return to menu.");
            }
        });
            session = connecting.get(8, TimeUnit.SECONDS);
            session.subscribe("/user/queue/private", handler(attempt, true));
            if (!recovering) state = State.CONNECTED;
        } catch (Exception error) {
            if (connecting != null) connecting.cancel(true);
            closeTransport();
            fail("Could not connect within 8 seconds. Check the host address and retry.");
            throw error;
        }
    }

    private StompFrameHandler handler(long attempt, boolean privateQueue) {
        return new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders headers) { return Packet.class; }
            @Override public void handleFrame(StompHeaders headers, Object payload) {
                if (attempt != generation || !(payload instanceof Packet packet) || packet.getAction() == null) return;
                if (!privateQueue && !java.util.Objects.equals(roomId, packet.getRoomId())) return;
                if (packet.getAction() == Action.PRIVATE_JOIN_CONFIRMATION) {
                    roomId = packet.getRoomId();
                    resumeToken = packet.getResumeToken();
                    if (!roomSubscribed) {
                        roomSubscribed = true;
                        session.subscribe("/match/rooms/" + roomId, handler(attempt, false));
                    }
                    deliver(packet);
                    transmit("/app/game.roomReady", Packet.builder().ID(packet.getID()).action(Action.ROOM_READY).build());
                    return;
                }
                if (packet.getAction() == Action.ERROR && state == State.RECOVERING) {
                    fail(packet.getMessage() == null ? "Recovery failed. Return to menu." : packet.getMessage());
                } else if (packet.getAction() == Action.MATCH_START || packet.getAction() == Action.MATCH_STATE
                    || packet.getAction() == Action.REMATCH_START || packet.getAction() == Action.REMATCH_UPDATE
                    || packet.getAction() == Action.ROOM_READY || packet.getAction() == Action.ERROR
                    || packet.getAction() == Action.ENVIRONMENT_VOTE_UPDATE) {
                    boolean recovered = privateQueue && (packet.getAction() == Action.MATCH_START
                        || packet.getAction() == Action.ROOM_READY && packet.getMatchState() == null);
                    if (state != State.RECOVERING || recovered) { pendingSince = 0; state = State.CONNECTED; }
                }
                deliver(packet);
            }
        };
    }
    private void deliver(Packet packet) { var handler = packetHandler; if (handler != null) handler.accept(packet); }
    private void fail(String message) { failure = message; state = State.FAILED; pendingSince = 0; }
    public void pollTimeout() {
        long since = pendingSince;
        if (since != 0 && System.nanoTime() - since > TimeUnit.SECONDS.toNanos(10))
            fail("No server response. Retry to read the current match state; your last action will not be repeated.");
    }
    private void transmit(String destination, Packet packet) {
        StompSession transport = session;
        if (transport == null || !transport.isConnected()) { fail("Disconnected. Retry to resume, or return to menu."); return; }
        try {
            packet.setRoomId(roomId);
            pendingSince = System.nanoTime();
            transport.send(destination, packet);
        } catch (RuntimeException error) { fail("Could not send to the server. Retry to synchronize."); }
    }
    public void send(Packet packet) { if (isReady()) transmit("/app/game.takeAction", packet); }
    public synchronized void recover() throws Exception {
        state = State.RECOVERING;
        if (session != null && session.isConnected() && roomId != null) {
            transmit("/app/game.sync", Packet.builder().action(Action.SYNC).build());
            return;
        }
        open(true);
        if (resumeToken != null) transmit("/app/game.resume", Packet.builder().action(Action.RESUME).resumeToken(resumeToken).build());
        else if (lastJoin != null) transmit("/app/game.joinGame", lastJoin);
        else state = State.CONNECTED;
    }
    /** Abrupt transport loss preserves the reconnect lease; normal disconnect leaves the room. */
    public synchronized void interruptTransport() {
        closeTransport();
        fail("Connection interrupted. Retry within 30 seconds to resume.");
    }
    public void join(String username, Vector2 point, CharacterClass type) { join(username, point, 0, type, Race.HUMAN); }
    public void join(String username, Vector2 point, int team, CharacterClass type, Race race) {
        lastJoin = Packet.builder().username(username).finalPosition(point).teamIndex(team)
            .characterClass(type).race(race).action(Action.JOIN).build();
        transmit("/app/game.joinGame", lastJoin);
    }
    public void voteEnvironment(int id, Environment environment) {
        transmit("/app/game.voteEnvironment", Packet.builder().ID(id).environment(environment).action(Action.ENVIRONMENT_VOTE).build());
    }
    public void move(int id, Vector2 point) { send(Packet.builder().ID(id).finalPosition(point == null ? null : new Vector2(point)).action(Action.MOVE).build()); }
    public void startTestMatch(int id) { startTestMatch(id, null); }
    public void startTestMatch(int id, Environment environment) {
        transmit("/app/game.startTestMatch", Packet.builder().ID(id).environment(environment).action(Action.START_TEST_MATCH).build());
    }
    public void useAbility(int id, AbilityType ability, int target, Vector2 point) {
        send(Packet.builder().ID(id).ability(ability).targetPlayerID(target)
            .targetPosition(point == null ? null : new Vector2(point)).action(Action.USE_ABILITY).build());
    }
    public void endTurn(int id) { send(Packet.builder().ID(id).action(Action.END_TURN).build()); }
    public void requestRematch(int id) { transmit("/app/game.voteRematch", Packet.builder().ID(id).action(Action.REMATCH_VOTE).build()); }
    public synchronized void disconnect() {
        if (session != null && session.isConnected() && roomId != null) {
            try { session.send("/app/game.leave", Packet.builder().action(Action.LEAVE).build()); } catch (RuntimeException ignored) {}
        }
        closeTransport();
        roomId = null; resumeToken = null; lastJoin = null; state = State.CLOSED;
    }
    private void closeTransport() {
        generation++;
        var transport = session; session = null;
        try { if (transport != null && transport.isConnected()) transport.disconnect(); } catch (RuntimeException ignored) {}
        try { if (stompClient != null) stompClient.stop(); } finally {
            if (scheduler != null) scheduler.shutdown();
            stompClient = null; scheduler = null; roomSubscribed = false; pendingSince = 0;
        }
    }
}
