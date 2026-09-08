// File Location: server/src/test/java/com/server/server/matchmaking/TestingModeTest.java
package com.server.server.matchmaking;

import com.badlogic.gdx.math.Vector2;
import com.server.server.selection.EnvironmentVoteResolver;
import com.shared.shared.model.Action;
import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.Packet;
import com.shared.shared.model.Race;
import com.shared.shared.model.world.Environment;
import com.shared.shared.network.MatchState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestingModeTest {
    @Test
    void soloPracticeStartsWithoutVotesAndCanMoveAndRepeatTurns() {
        RoomRegistry registry = new RoomRegistry(new EnvironmentVoteResolver(), true);
        RoomAssignment solo = registry.assign(selection(1), "solo");
        MatchRoom room = solo.getRoom();
        int id = solo.getPlayer().getId();
        room.markReady("solo");
        MatchState state = room.startTestMatch(id);

        assertTrue(state.isTestingMode());
        assertEquals(Environment.CANYON, state.getEnvironment());
        assertEquals(1, state.getPlayers().size());
        assertFalse(state.isMatchOver());
        assertEquals(id, state.getActivePlayerId());
        assertTrue(room.getMatches().handle(id, Packet.builder().ID(id).action(Action.MOVE)
            .finalPosition(new Vector2(3f, 5f)).build()).isAccepted());
        for (int turn = 0; turn < 3; turn++) {
            var next = room.getMatches().handle(id, Packet.builder().ID(id).action(Action.END_TURN).build());
            assertTrue(next.isAccepted());
            assertFalse(next.getState().isMatchOver());
            assertEquals(id, next.getState().getActivePlayerId());
        }
        assertThrows(IllegalArgumentException.class, () -> room.startTestMatch(id));
        assertNotEquals(room.getId(), registry.assign(selection(2), "late").getRoom().getId());
        registry.disconnect("solo", id);
        assertTrue(registry.room(room.getId()).isEmpty());
    }

    @Test
    void startsWithTwoThreeOrFourPlayersAndKeepsNormalVictoryRules() {
        for (int count = 2; count <= 4; count++) {
            MatchRoom room = new MatchRoom("test", new EnvironmentVoteResolver(), true);
            for (int i = 0; i < count; i++) {
                room.getLobby().addPlayer(selection(i % 2 + 1), "s" + i);
                room.markReady("s" + i);
            }
            MatchState state = room.startTestMatch(0);
            assertEquals(count, state.getPlayers().size());
            assertFalse(state.isMatchOver());
            for (int i = 1; i < count; i += 2) state = room.getMatches().disconnect(i);
            assertTrue(state.isMatchOver());
            assertEquals(1, state.getWinningTeam());
        }
    }

    @Test
    void testStartRejectsUnreadyAndUnknownPlayersAndNormalMode() {
        MatchRoom room = new MatchRoom("test", new EnvironmentVoteResolver(), true);
        room.getLobby().addPlayer(selection(1), "s0");
        assertThrows(IllegalArgumentException.class, () -> room.startTestMatch(0));
        assertThrows(IllegalArgumentException.class, () -> room.startTestMatch(99));
        assertFalse(room.getMatches().isRunning());
        MatchRoom normal = new MatchRoom("normal", new EnvironmentVoteResolver());
        normal.getLobby().addPlayer(selection(1), "s0");
        normal.markReady("s0");
        assertThrows(IllegalArgumentException.class, () -> normal.startTestMatch(0));
        assertTrue(normal.getLobby().startIfReady().isEmpty());
    }

    @Test
    void testRematchWorksWithRemainingPlayerAndStaysInPracticeMode() {
        RoomRegistry registry = new RoomRegistry(new EnvironmentVoteResolver(), true);
        RoomAssignment first = registry.assign(selection(1), "one");
        RoomAssignment second = registry.assign(selection(2), "two");
        MatchRoom room = first.getRoom();
        room.markReady("one");
        room.markReady("two");
        room.startTestMatch(first.getPlayer().getId());
        registry.disconnect("two", second.getPlayer().getId());
        assertTrue(room.getMatches().isComplete());
        RematchDecision restart = room.requestRematch(first.getPlayer().getId());
        assertTrue(restart.isStarted());
        assertEquals(1, restart.getRequiredVotes());
        assertTrue(restart.getState().isTestingMode());
        assertFalse(restart.getState().isMatchOver());
    }

    private Packet selection(int team) {
        return Packet.builder().username("Tester").teamIndex(team)
            .characterClass(CharacterClass.PALADIN).race(Race.HUMAN).build();
    }
}
