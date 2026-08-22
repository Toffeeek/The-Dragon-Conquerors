// File Location: server/src/test/java/com/server/server/selection/LobbyStateServiceTest.java
package com.server.server.selection;

import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.Packet;
import com.shared.shared.model.Race;
import com.shared.shared.model.world.Environment;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LobbyStateServiceTest {
    @Test
    void enforcesTwoPlayersPerTeam() {
        LobbyStateService lobby = lobby();
        lobby.addPlayer(selection(1, CharacterClass.PALADIN, Race.HUMAN), "one");
        lobby.addPlayer(selection(1, CharacterClass.MAGE, Race.ELF), "two");

        String error = lobby.validateJoin(selection(1, CharacterClass.ARCHER, Race.UNDEAD));
        assertNotNull(error);
        assertTrue(error.contains("Azure"));
    }

    @Test
    void startsOnlyAfterFourPlayersHaveVoted() {
        LobbyStateService lobby = lobby();
        LobbyPlayer one = lobby.addPlayer(selection(1, CharacterClass.PALADIN, Race.HUMAN), "one");
        LobbyPlayer two = lobby.addPlayer(selection(1, CharacterClass.MAGE, Race.ELF), "two");
        LobbyPlayer three = lobby.addPlayer(selection(2, CharacterClass.WRAITH, Race.UNDEAD), "three");
        LobbyPlayer four = lobby.addPlayer(selection(2, CharacterClass.CLERIC, Race.DRAGONBORNE), "four");

        lobby.recordVote(one.getId(), Environment.CANYON);
        lobby.recordVote(two.getId(), Environment.CANYON);
        lobby.recordVote(three.getId(), Environment.BOG);
        assertFalse(lobby.startIfReady().isPresent());

        lobby.recordVote(four.getId(), Environment.LAVA);
        assertEquals(Environment.CANYON, lobby.startIfReady().orElseThrow());
    }

    private LobbyStateService lobby() {
        return new LobbyStateService(new EnvironmentVoteResolver(new Random(11)));
    }

    private Packet selection(int team, CharacterClass characterClass, Race race) {
        return Packet.builder()
            .username("Player")
            .teamIndex(team)
            .characterClass(characterClass)
            .race(race)
            .build();
    }
}
