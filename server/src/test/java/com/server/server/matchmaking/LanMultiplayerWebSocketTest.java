package com.server.server.matchmaking;

import com.badlogic.gdx.math.Vector2;
import com.client.client.NetworkClient;
import com.shared.shared.model.*;
import com.shared.shared.model.world.Environment;
import com.shared.shared.network.MatchState;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Four real clients through the production (non-practice) transport and controller. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LanMultiplayerWebSocketTest {
    @LocalServerPort int port;

    @ParameterizedTest @EnumSource(Environment.class)
    void fourPlayersVotePlayRecoverAndFinishOnEveryMap(Environment environment) throws Exception {
        List<Peer> peers = new ArrayList<>();
        try {
            for (int i = 0; i < 4; i++) {
                Peer peer = new Peer(); peers.add(peer);
                peer.client.connect();
                peer.client.join("LAN " + i, new Vector2(), i < 2 ? 1 : 2,
                    CharacterClass.values()[(environment.ordinal() * 2 + i) % 6], Race.values()[i]);
                Packet joined = await(peer, Action.PRIVATE_JOIN_CONFIRMATION);
                peer.id = joined.getID(); peer.token = joined.getResumeToken();
                assertFalse(joined.isTestingMode(), "Normal hosting must never default to practice");
                await(peer, Action.ROOM_READY);
                if (i == 0) {
                    peer.client.startTestMatch(peer.id, environment);
                    assertNotNull(await(peer, Action.ERROR).getMessage());
                }
            }
            for (Peer peer : peers) peer.client.voteEnvironment(peer.id, environment);
            MatchState state = null;
            for (Peer peer : peers) {
                state = await(peer, Action.MATCH_START).getMatchState();
                assertEquals(4, state.getPlayers().size());
                assertEquals(environment, state.getEnvironment());
                assertFalse(state.isTestingMode()); assertFalse(state.isMatchOver());
            }
            for (int turn = 0; turn < 8; turn++) {
                int active = state.getActivePlayerId();
                peers.stream().filter(p -> p.id == active).findFirst().orElseThrow().client.endTurn(active);
                for (Peer peer : peers) {
                    MatchState update = await(peer, Action.MATCH_STATE).getMatchState();
                    assertNotEquals(active, update.getActivePlayerId());
                    assertFalse(update.isMatchOver()); state = update;
                }
            }
            Peer returning = peers.get(0);
            returning.client.interruptTransport();
            // Recover retries while the server registers the lost transport.
            returning.client.recover();
            Packet rejoined = await(returning, Action.PRIVATE_JOIN_CONFIRMATION);
            assertEquals(returning.id, rejoined.getID());
            assertNotEquals(returning.token, rejoined.getResumeToken());
            MatchState recovered = await(returning, Action.MATCH_START).getMatchState();
            assertEquals(state.getRoundNumber(), recovered.getRoundNumber());
            assertEquals(4, recovered.getPlayers().size());
            peers.get(2).client.disconnect(); peers.get(3).client.disconnect();
            for (Peer peer : peers.subList(0, 2)) {
                MatchState end;
                do { end = await(peer, Action.MATCH_STATE).getMatchState(); } while (!end.isMatchOver());
                assertEquals(1, end.getWinningTeam());
            }
            returning.client.requestRematch(returning.id);
            assertNotNull(await(returning, Action.ERROR).getMessage());
        } finally { for (Peer peer : peers) peer.client.disconnect(); }
    }

    private final class Peer {
        final NetworkClient client = new NetworkClient("ws://localhost:" + port + "/ws");
        final BlockingQueue<Packet> packets = new LinkedBlockingQueue<>();
        int id; String token;
        Peer() { client.setPacketHandler(packets::add); }
    }
    private Packet await(Peer peer, Action action) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(12);
        while (System.nanoTime() < deadline) {
            Packet packet = peer.packets.poll(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
            assertNotNull(packet, "No " + action);
            if (packet.getAction() == action) return packet;
            assertNotEquals(Action.ERROR, packet.getAction(), packet.getMessage());
        }
        throw new AssertionError("No " + action);
    }
}
