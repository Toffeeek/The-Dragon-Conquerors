// File Location: server/src/test/java/com/server/server/combat/AuthoritativeMatchTest.java
package com.server.server.combat;

import com.badlogic.gdx.math.Vector2;
import com.server.server.selection.LobbyPlayer;
import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.Race;
import com.shared.shared.model.ability.AbilityType;
import com.shared.shared.model.combat.AbilityResolver;
import com.shared.shared.model.world.Environment;
import com.shared.shared.network.PlayerCombatState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthoritativeMatchTest {
    @Test
    void highestSpeedActsFirstAndIdBreaksTies() {
        AuthoritativeMatch match = match();
        assertEquals(0, match.getActivePlayerId(), "Wraith has the highest Speed");

        CombatCommandResult end = match.endTurn(0);
        assertTrue(end.isAccepted());
        assertEquals(1, end.getState().getActivePlayerId(),
            "Mage and Cleric tie on Speed, so lower player ID acts first");
    }

    @Test
    void rejectsCommandsFromAPlayerWhoDoesNotOwnTheTurn() {
        AuthoritativeMatch match = match();
        CombatCommandResult result = match.move(2, new Vector2(6f, 7f));
        assertFalse(result.isAccepted());
        assertTrue(result.getError().contains("not your turn"));
    }

    @Test
    void acceptedAbilitySpendsActionManaAndStartsCooldown() {
        AuthoritativeMatch match = match();
        CombatCommandResult result = match.useAbility(
            0, AbilityType.CURSE, 2, null);

        assertTrue(result.isAccepted());
        PlayerCombatState wraith = player(result, 0);
        assertTrue(wraith.isActionUsed());
        assertEquals(60, wraith.getMana());
        assertEquals(3, wraith.getCooldowns().get("Curse"));

        CombatCommandResult second = match.useAbility(
            0, AbilityType.POISON_JAB, 2, null);
        assertFalse(second.isAccepted());
        assertTrue(second.getError().contains("already spent"));
    }

    @Test
    void movementIsBoundedByStaminaAndPlayerCollision() {
        AuthoritativeMatch match = match();
        CombatCommandResult tooFar = match.move(0, new Vector2(20f, 5f));
        assertFalse(tooFar.isAccepted());

        CombatCommandResult occupied = match.move(0, new Vector2(6f, 5f));
        assertFalse(occupied.isAccepted());
        assertTrue(occupied.getError().contains("occupies"));

        CombatCommandResult accepted = match.move(0, new Vector2(5f, 7f));
        assertTrue(accepted.isAccepted());
        assertEquals(7f, player(accepted, 0).getPosition().y, 0.001f);
    }

    @Test
    void hostileAbilitiesCannotTargetATeammate() {
        AuthoritativeMatch match = match();
        CombatCommandResult result = match.useAbility(
            0, AbilityType.CURSE, 1, null);
        assertFalse(result.isAccepted());
        assertTrue(result.getError().contains("legal target"));
    }

    private AuthoritativeMatch match() {
        List<LobbyPlayer> players = List.of(
            player(0, CharacterClass.WRAITH, Race.UNDEAD, 1, 5f, 5f),
            player(1, CharacterClass.MAGE, Race.ELF, 1, 5f, 9f),
            player(2, CharacterClass.PALADIN, Race.HUMAN, 2, 6f, 5f),
            player(3, CharacterClass.CLERIC, Race.DRAGONBORNE, 2, 6f, 9f));
        return new AuthoritativeMatch(players, Environment.CANYON,
            new AbilityResolver(19L));
    }

    private LobbyPlayer player(int id, CharacterClass characterClass, Race race,
                               int team, float x, float y) {
        return new LobbyPlayer(id, "session-" + id, "Player " + id,
            characterClass, race, team, new Vector2(x, y));
    }

    private PlayerCombatState player(CombatCommandResult result, int id) {
        return result.getState().getPlayers().stream()
            .filter(player -> player.getId() == id)
            .findFirst().orElseThrow();
    }
}
