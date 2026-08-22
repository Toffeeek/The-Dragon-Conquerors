// File Location: server/src/test/java/com/server/server/matchmaking/RoomRegistryTest.java
package com.server.server.matchmaking;

import com.server.server.selection.EnvironmentVoteResolver;
import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.Packet;
import com.shared.shared.model.Race;
import com.shared.shared.model.world.Environment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomRegistryTest {
    @Test
    void assignsPlayersToTheFirstCompatibleWaitingRoom() {
        RoomRegistry registry = registry();
        RoomAssignment azureOne = assign(registry, "a1", 1);
        RoomAssignment azureTwo = assign(registry, "a2", 1);
        RoomAssignment overflowAzure = assign(registry, "a3", 1);
        RoomAssignment crimsonOne = assign(registry, "c1", 2);

        assertEquals(azureOne.getRoom().getId(), azureTwo.getRoom().getId());
        assertNotEquals(azureOne.getRoom().getId(), overflowAzure.getRoom().getId());
        assertEquals(azureOne.getRoom().getId(), crimsonOne.getRoom().getId(),
            "A Crimson player should fill the compatible space in the oldest room");
        assertEquals(2, registry.roomCount());
    }

    @Test
    void fullRoomsAreIsolatedAndUseRoomLocalPlayerIds() {
        RoomRegistry registry = registry();
        MatchRoom first = fillRoom(registry, "first-");
        MatchRoom second = fillRoom(registry, "second-");

        assertNotEquals(first.getId(), second.getId());
        assertEquals(4, first.getLobby().size());
        assertEquals(4, second.getLobby().size());
        assertEquals(0, second.getLobby().players().get(0).getId());

        first.getLobby().players().forEach(player ->
            first.getLobby().recordVote(player.getId(), Environment.BOG));
        Environment selected = first.getLobby().startIfReady().orElseThrow();
        first.getMatches().start(first.getLobby().players(), selected);

        assertTrue(first.getMatches().isRunning());
        assertFalse(second.getMatches().isRunning());
        assertEquals(4, first.getMatches().snapshot().getPlayers().size());
    }

    @Test
    void disconnectRemovesOnlyThatSessionAndDeletesAnEmptyRoom() {
        RoomRegistry registry = registry();
        RoomAssignment first = assign(registry, "one", 1);
        RoomAssignment second = assign(registry, "two", 2);

        RoomDisconnectResult result = registry.disconnect("one", first.getPlayer().getId());
        assertEquals(first.getRoom().getId(), result.getRoom().getId());
        assertEquals(1, first.getRoom().getLobby().size());
        assertTrue(registry.roomForSession("one").isEmpty());
        assertTrue(registry.roomForSession("two").isPresent());

        registry.disconnect("two", second.getPlayer().getId());
        assertEquals(0, registry.roomCount());
    }

    @Test
    void rejectsASecondJoinFromTheSameConnection() {
        RoomRegistry registry = registry();
        assign(registry, "same-session", 1);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> assign(registry, "same-session", 2));
        assertTrue(error.getMessage().contains("already joined"));
    }

    private MatchRoom fillRoom(RoomRegistry registry, String prefix) {
        MatchRoom room = assign(registry, prefix + "a1", 1).getRoom();
        assign(registry, prefix + "a2", 1);
        assign(registry, prefix + "c1", 2);
        assign(registry, prefix + "c2", 2);
        return room;
    }

    private RoomAssignment assign(RoomRegistry registry, String sessionId, int team) {
        return registry.assign(Packet.builder()
            .username(sessionId)
            .teamIndex(team)
            .characterClass(team == 1 ? CharacterClass.MAGE : CharacterClass.PALADIN)
            .race(team == 1 ? Race.ELF : Race.HUMAN)
            .build(), sessionId);
    }

    private RoomRegistry registry() {
        return new RoomRegistry(new EnvironmentVoteResolver());
    }
}
