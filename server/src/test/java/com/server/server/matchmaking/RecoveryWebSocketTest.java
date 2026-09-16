package com.server.server.matchmaking;

import com.client.client.NetworkClient;
import com.badlogic.gdx.math.Vector2;
import com.shared.shared.model.*;
import com.shared.shared.model.ability.AbilityType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "game.testing-mode=true")
class RecoveryWebSocketTest {
    @LocalServerPort int port;
    @Test void reconnectRestoresSpentActionAndDoesNotReplayIt() throws Exception {
        var packets = new LinkedBlockingQueue<Packet>();
        var client = new NetworkClient("ws://localhost:"+port+"/ws"); client.setPacketHandler(packets::add);
        try {
            client.connect(); client.join("Recovery",new Vector2(),1,CharacterClass.ARCHER,Race.HUMAN);
            Packet joined = await(packets,Action.PRIVATE_JOIN_CONFIRMATION); await(packets,Action.ROOM_READY);
            client.startTestMatch(joined.getID()); await(packets,Action.MATCH_START);
            client.useAbility(joined.getID(),AbilityType.ACCURACY_BOOST,joined.getID(),null);
            var before = await(packets,Action.MATCH_STATE).getMatchState();
            assertEquals(0,before.getPlayers().getFirst().getActionPoints());
            String token = client.getResumeToken();
            client.interruptTransport(); client.recover();
            var resumed = await(packets,Action.PRIVATE_JOIN_CONFIRMATION);
            assertEquals(joined.getID(),resumed.getID()); assertEquals(joined.getRoomId(),resumed.getRoomId());
            var after = await(packets,Action.MATCH_START).getMatchState();
            assertEquals(before.getActionSequence(),after.getActionSequence());
            assertEquals(before.getPlayers().getFirst().getMana(),after.getPlayers().getFirst().getMana());
            assertEquals(0,after.getPlayers().getFirst().getActionPoints());
            assertNotEquals(token,client.getResumeToken()); assertTrue(client.isReady());
            client.endTurn(joined.getID()); assertEquals(1,await(packets,Action.MATCH_STATE).getMatchState().getPlayers().getFirst().getActionPoints());
        } finally { client.disconnect(); }
    }
    @Test void repeatedUnavailableHostAttemptsFailAndCloseCleanly() throws Exception {
        int unavailable;
        try (var socket = new java.net.ServerSocket(0)) { unavailable = socket.getLocalPort(); }
        var client = new NetworkClient("ws://localhost:"+unavailable+"/ws");
        try {
            for (int i=0;i<3;i++) {
                assertThrows(Exception.class,client::connect);
                assertEquals(NetworkClient.State.FAILED,client.getState());
            }
        } finally { client.disconnect(); }
        assertEquals(NetworkClient.State.CLOSED,client.getState());
    }
    private Packet await(BlockingQueue<Packet> packets, Action action) throws Exception {
        long deadline = System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime()<deadline) {
            Packet packet = packets.poll(Math.max(1,deadline-System.nanoTime()),TimeUnit.NANOSECONDS);
            assertNotNull(packet,"No "+action); assertNotEquals(Action.ERROR,packet.getAction(),packet.getMessage());
            if (packet.getAction()==action) return packet;
        }
        throw new AssertionError("No "+action);
    }
}
