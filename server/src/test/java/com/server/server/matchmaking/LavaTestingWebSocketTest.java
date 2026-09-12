// File Location: server/src/test/java/com/server/server/matchmaking/LavaTestingWebSocketTest.java
package com.server.server.matchmaking;

import com.badlogic.gdx.math.Vector2;
import com.client.client.NetworkClient;
import com.shared.shared.model.*;
import com.shared.shared.model.world.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties={"game.testing-mode=true","game.testing-environment=LAVA"})
class LavaTestingWebSocketTest {
    @LocalServerPort private int port;
    @ParameterizedTest
    @EnumSource(Environment.class)
    void soloMapChoiceReachesServerAndStartsSelectedBattlefield(Environment environment) throws Exception {
        BlockingQueue<Packet> packets = new LinkedBlockingQueue<>();
        NetworkClient client = new NetworkClient("ws://localhost:" + port + "/ws");
        client.setPacketHandler(packets::add);
        try {
            client.connect();
            client.join("Map tester", new Vector2(), 1, CharacterClass.PALADIN, Race.HUMAN);
            Packet joined = await(packets, Action.PRIVATE_JOIN_CONFIRMATION);
            await(packets, Action.ROOM_READY);
            client.startTestMatch(joined.getID(), environment);
            Packet started = await(packets, Action.MATCH_START);
            assertEquals(environment, started.getEnvironment());
            assertEquals(environment, started.getMatchState().getEnvironment());
            assertEquals(1, started.getMatchState().getPlayers().size());
            assertFalse(started.getMatchState().isMatchOver());
            Vector2 destination = environment == Environment.LAVA ? new Vector2(5.2f, 11.8f) : new Vector2(3f, 5f);
            client.move(joined.getID(), destination);
            Packet moved = await(packets, Action.MATCH_STATE);
            assertEquals(destination, moved.getMatchState().getPlayers().get(0).getPosition());
        } finally { client.disconnect(); }
    }

    @Test void configuredLavaMapIsAnnouncedStartedAndPlayableOverTheNetwork() throws Exception {
        BlockingQueue<Packet> packets=new LinkedBlockingQueue<>();
        NetworkClient client=new NetworkClient("ws://localhost:"+port+"/ws");
        client.setPacketHandler(packets::add);
        try {
            client.connect();
            client.join("Lava tester",new Vector2(),1,CharacterClass.PALADIN,Race.HUMAN);
            Packet joined=await(packets,Action.PRIVATE_JOIN_CONFIRMATION);
            assertEquals(Environment.LAVA,joined.getEnvironment());
            await(packets,Action.ROOM_READY);
            client.startTestMatch(joined.getID());
            Packet started=await(packets,Action.MATCH_START);
            assertEquals(Environment.LAVA,started.getMatchState().getEnvironment());
            assertFalse(started.getMatchState().isMatchOver());
            client.move(joined.getID(),new Vector2(5.2f,11.8f));
            Packet moved=await(packets,Action.MATCH_STATE);
            assertEquals(11.8f,moved.getMatchState().getPlayers().get(0).getPosition().y,0.001f);
            client.endTurn(joined.getID());
            Packet next=await(packets,Action.MATCH_STATE);
            assertFalse(next.getMatchState().isMatchOver());
            assertEquals(1,next.getMatchState().getPlayers().get(0).getActionPoints());
        } finally { client.disconnect(); }
    }
    private Packet await(BlockingQueue<Packet> packets,Action action) throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
        while(System.nanoTime()<deadline) {
            Packet packet=packets.poll(Math.max(1,deadline-System.nanoTime()),TimeUnit.NANOSECONDS);
            assertNotNull(packet,"Timed out waiting for "+action);
            assertNotEquals(Action.ERROR,packet.getAction(),packet.getMessage());
            if(packet.getAction()==action)return packet;
        }
        throw new AssertionError("Timed out waiting for "+action);
    }
}
