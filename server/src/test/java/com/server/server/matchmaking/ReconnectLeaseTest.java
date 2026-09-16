package com.server.server.matchmaking;

import com.server.server.selection.EnvironmentVoteResolver;
import com.shared.shared.model.*;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class ReconnectLeaseTest {
    private Packet selection() { return Packet.builder().teamIndex(1).characterClass(CharacterClass.MAGE).race(Race.HUMAN).build(); }
    @Test void resumeKeepsIdentityRotatesTokenAndInvalidatesTheOldSession() {
        var registry = new RoomRegistry(new EnvironmentVoteResolver(), true);
        var assigned = registry.assign(selection(),"old");
        String token = registry.resumeToken("old");
        registry.suspend("old");
        var resumed = registry.resume(token,"new");
        assertEquals(assigned.getPlayer().getId(),resumed.getPlayer().getId());
        assertSame(assigned.getRoom(),resumed.getRoom());
        assertTrue(registry.roomForSession("old").isEmpty());
        assertNotEquals(token,registry.resumeToken("new"));
        assertThrows(IllegalArgumentException.class,()->registry.resume(token,"stolen"));
        registry.suspend("old");
        assertTrue(registry.expireDisconnected().isEmpty());
    }
    @Test void graceExpiresExactlyOnceAndDuplicateDisconnectDoesNotExtendIt() {
        AtomicLong clock = new AtomicLong();
        var registry = new RoomRegistry(new EnvironmentVoteResolver(),true,clock::get);
        registry.assign(selection(),"old"); String token = registry.resumeToken("old");
        registry.suspend("old"); clock.set(20_000_000_000L); registry.suspend("old");
        assertTrue(registry.expireDisconnected().isEmpty());
        clock.set(31_000_000_000L);
        assertThrows(IllegalArgumentException.class,()->registry.resume(token,"new"));
        assertEquals(1,registry.expireDisconnected().size());
        assertTrue(registry.expireDisconnected().isEmpty()); assertEquals(0,registry.roomCount());
    }
    @Test void explicitDepartureInvalidatesResumeToken() {
        var registry = new RoomRegistry(new EnvironmentVoteResolver(),true);
        var assigned = registry.assign(selection(),"old"); String token = registry.resumeToken("old");
        registry.disconnect("old",assigned.getPlayer().getId());
        assertThrows(IllegalArgumentException.class,()->registry.resume(token,"new"));
    }
    @Test void reconnectUpdatesPresenceWithoutResettingBattleStatistics() {
        var registry=new RoomRegistry(new EnvironmentVoteResolver(),true);
        var assigned=registry.assign(selection(),"old");
        assigned.getRoom().getMatches().start(assigned.getRoom().getLobby().players(),com.shared.shared.model.world.Environment.CANYON,true);
        String token=registry.resumeToken("old");registry.suspend("old");
        assertFalse(assigned.getRoom().getMatches().snapshot().getPlayers().getFirst().isConnected());
        registry.resume(token,"new");
        var state=assigned.getRoom().getMatches().snapshot();
        assertTrue(state.getPlayers().getFirst().isConnected());
        assertEquals(1,state.getStatistics().getFirst().getTurnsPlayed());
    }
}
