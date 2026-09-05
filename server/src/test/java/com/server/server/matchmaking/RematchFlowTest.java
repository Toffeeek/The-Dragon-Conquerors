// File Location: server/src/test/java/com/server/server/matchmaking/RematchFlowTest.java
package com.server.server.matchmaking;

import com.server.server.selection.EnvironmentVoteResolver;
import com.server.server.selection.LobbyPlayer;
import com.shared.shared.model.Action;
import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.Packet;
import com.shared.shared.model.Race;
import com.shared.shared.model.ability.AbilityType;
import com.shared.shared.model.world.BattlefieldDefinition;
import com.shared.shared.model.world.Environment;
import com.shared.shared.network.MatchState;
import com.shared.shared.network.PlayerCombatState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RematchFlowTest {
    @Test
    void unanimousVoteCreatesAPristineAuthoritativeMatch() {
        MatchRoom room = completedRoom();

        assertFalse(room.requestRematch(0).isStarted());
        assertFalse(room.requestRematch(1).isStarted());
        assertFalse(room.requestRematch(2).isStarted());
        RematchDecision decision = room.requestRematch(3);

        assertTrue(decision.isAccepted());
        assertTrue(decision.isStarted());
        assertEquals(4, decision.getVotes());
        assertEquals(0, room.rematchVoteCount());

        MatchState state = decision.getState();
        assertFalse(state.isMatchOver());
        assertEquals(Environment.BOG, state.getEnvironment());
        assertEquals(1, state.getRoundNumber());
        assertEquals(4, state.getPlayers().size());
        for (PlayerCombatState player : state.getPlayers()) {
            assertEquals(player.getMaxHp(), player.getHp());
            assertTrue(player.getEffects().isEmpty());
            assertTrue(player.getCooldowns().isEmpty());
            assertFalse(player.isActionUsed());
            int slot = player.getId() % 2;
            assertTrue(player.getPosition().epsilonEquals(
                BattlefieldDefinition.forEnvironment(Environment.BOG)
                    .spawnFor(player.getTeamIndex(), slot), 0.001f));
        }
    }

    @Test
    void duplicateVoteCountsOnlyOnce() {
        MatchRoom room = completedRoom();

        RematchDecision first = room.requestRematch(0);
        RematchDecision duplicate = room.requestRematch(0);

        assertEquals(1, first.getVotes());
        assertEquals(1, duplicate.getVotes());
        assertFalse(duplicate.isStarted());
    }

    @Test
    void rematchIsRejectedBeforeCompletionOrAfterAPlayerLeaves() {
        MatchRoom room = fullRoom();
        room.getMatches().start(room.getLobby().players(), Environment.BOG);

        RematchDecision early = room.requestRematch(0);
        assertFalse(early.isAccepted());
        assertTrue(early.getError().contains("still in progress"));

        room.getMatches().disconnect(2);
        room.getMatches().disconnect(3);
        LobbyPlayer removed = room.getLobby().remove(3);
        room.removePlayer(3, removed.getSessionId());

        RematchDecision uneven = room.requestRematch(0);
        assertFalse(uneven.isAccepted());
        assertTrue(uneven.getError().contains("all four"));
    }

    private MatchRoom completedRoom() {
        MatchRoom room = fullRoom();
        room.getMatches().start(room.getLobby().players(), Environment.BOG);

        assertTrue(room.getMatches().handle(0, Packet.builder()
            .ID(0).action(Action.USE_ABILITY).ability(AbilityType.TELEPORT)
            .targetPosition(new com.badlogic.gdx.math.Vector2(8f, 3f)).build()).isAccepted());
        for (int turn = 0; turn < 4; turn++) {
            int active = room.getMatches().snapshot().getActivePlayerId();
            assertTrue(room.getMatches().handle(active,
                Packet.builder().ID(active).action(Action.END_TURN).build()).isAccepted());
        }
        PlayerCombatState mutated = room.getMatches().snapshot().getPlayers().stream()
            .filter(player -> player.getId() == 0).findFirst().orElseThrow();
        assertFalse(mutated.getEffects().isEmpty());
        assertFalse(mutated.getCooldowns().isEmpty());

        room.getMatches().disconnect(2);
        room.getMatches().disconnect(3);
        assertTrue(room.getMatches().isComplete());
        return room;
    }

    private MatchRoom fullRoom() {
        MatchRoom room = new MatchRoom("test-room", new EnvironmentVoteResolver());
        room.getLobby().addPlayer(selection("A1", 1, CharacterClass.WRAITH, Race.UNDEAD), "s0");
        room.getLobby().addPlayer(selection("A2", 1, CharacterClass.MAGE, Race.ELF), "s1");
        room.getLobby().addPlayer(selection("C1", 2, CharacterClass.PALADIN, Race.HUMAN), "s2");
        room.getLobby().addPlayer(selection("C2", 2, CharacterClass.CLERIC, Race.DRAGONBORNE), "s3");
        return room;
    }

    private Packet selection(String username, int team, CharacterClass type, Race race) {
        return Packet.builder().username(username).teamIndex(team)
            .characterClass(type).race(race).build();
    }
}
