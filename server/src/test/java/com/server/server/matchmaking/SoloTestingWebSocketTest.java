// File Location: server/src/test/java/com/server/server/matchmaking/SoloTestingWebSocketTest.java
package com.server.server.matchmaking;

import com.badlogic.gdx.math.Vector2;
import com.client.client.NetworkClient;
import com.shared.shared.model.Action;
import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.Packet;
import com.shared.shared.model.Race;
import com.shared.shared.model.ability.AbilityType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import com.shared.shared.model.world.Environment;
import com.shared.shared.model.world.BattlefieldArtwork;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "game.testing-mode=true")
class SoloTestingWebSocketTest {
    @LocalServerPort private int port;

    @ParameterizedTest
    @EnumSource(Environment.class)
    void soloPlayerCanSelectEachMapAndMove(Environment environment) throws Exception {
        BlockingQueue<Packet> packets = new LinkedBlockingQueue<>();
        NetworkClient client = new NetworkClient("ws://localhost:" + port + "/ws");
        client.setPacketHandler(packets::add);
        try {
            client.connect();
            client.join("Map tester", new Vector2(), 1, CharacterClass.PALADIN, Race.HUMAN);
            int id = await(packets, Action.PRIVATE_JOIN_CONFIRMATION).getID();
            await(packets, Action.ROOM_READY);
            client.startTestMatch(id, environment);
            Packet start = await(packets, Action.MATCH_START);
            assertEquals(environment, start.getEnvironment());
            assertEquals(environment, start.getMatchState().getEnvironment());
            assertFalse(start.getMatchState().isMatchOver());
            Vector2 goal = environment == Environment.CANYON ? new Vector2(4.3f, 8.6f)
                : environment == Environment.BOG ? BattlefieldArtwork.MAP3.worldPoint(220,510)
                : BattlefieldArtwork.MAP2.worldPoint(260,240);
            client.move(id, goal);
            Packet moved = await(packets, Action.MATCH_STATE);
            assertEquals(goal, moved.getMatchState().getPlayers().get(0).getPosition());
        } finally { client.disconnect(); }
    }

    @Test
    void oneRealClientCanJoinStartMoveAndEndTurnWithoutVoting() throws Exception {
        BlockingQueue<Packet> packets = new LinkedBlockingQueue<>();
        NetworkClient client = new NetworkClient("ws://localhost:" + port + "/ws");
        client.setPacketHandler(packets::add);
        try {
            client.connect();
            client.join("Solo Tester", new Vector2(), 1, CharacterClass.ARCHER, Race.HUMAN);
            Packet confirmation = await(packets, Action.PRIVATE_JOIN_CONFIRMATION);
            assertTrue(confirmation.isTestingMode());
            int id = confirmation.getID();
            await(packets, Action.ROOM_READY);
            client.startTestMatch(id);
            Packet start = await(packets, Action.MATCH_START);
            assertEquals(1, start.getMatchState().getPlayers().size());
            assertFalse(start.getMatchState().isMatchOver());
            client.move(id, new Vector2(4.3f, 8.6f));
            Packet moved = await(packets, Action.MATCH_STATE);
            assertEquals(4.3f, moved.getMatchState().getPlayers().get(0).getPosition().x, 0.001f);
            assertFalse(moved.getMatchState().getPlayers().get(0).getMovementPath().isEmpty());
            client.useAbility(id, AbilityType.ACCURACY_BOOST, id, null);
            Packet action = await(packets, Action.MATCH_STATE);
            assertEquals(0, action.getMatchState().getPlayers().get(0).getActionPoints());
            client.move(id, new Vector2(15f, 8f));
            Packet automatic = await(packets, Action.MATCH_STATE);
            assertTrue(automatic.getMatchState().getMessage().contains("automatically"));
            assertEquals(1, automatic.getMatchState().getPlayers().get(0).getActionPoints());
            assertTrue(automatic.getMatchState().getRoundNumber() > start.getMatchState().getRoundNumber());
            client.endTurn(id);
            Packet next = await(packets, Action.MATCH_STATE);
            assertFalse(next.getMatchState().isMatchOver());
            assertEquals(id, next.getMatchState().getActivePlayerId());
        } finally {
            client.disconnect();
        }
    }

    private Packet await(BlockingQueue<Packet> packets, Action action) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            Packet packet = packets.poll(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
            assertNotNull(packet, "Timed out waiting for " + action);
            assertNotEquals(Action.ERROR, packet.getAction(), packet.getMessage());
            if (packet.getAction() == action) return packet;
        }
        throw new AssertionError("Timed out waiting for " + action);
    }
}
