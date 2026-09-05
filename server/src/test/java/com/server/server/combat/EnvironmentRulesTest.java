// File Location: server/src/test/java/com/server/server/combat/EnvironmentRulesTest.java
package com.server.server.combat;

import com.badlogic.gdx.math.Vector2;
import com.server.server.selection.LobbyPlayer;
import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.Race;
import com.shared.shared.model.ability.AbilityType;
import com.shared.shared.model.combat.AbilityResolver;
import com.shared.shared.model.effect.StatusEffectType;
import com.shared.shared.model.world.Environment;
import com.shared.shared.network.PlayerCombatState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvironmentRulesTest {
    @Test
    void bogPoisonsAPlayerAtTheStartOfTheirTurn() {
        AuthoritativeMatch match = match(Environment.BOG, defaultPlayers(), resolver());
        assertTrue(match.move(0, new Vector2(8f, 3f)).isAccepted());

        endCurrentTurn(match);
        endCurrentTurn(match);
        endCurrentTurn(match);
        CombatCommandResult backToWraith = endCurrentTurn(match);

        PlayerCombatState wraith = player(backToWraith, 0);
        assertTrue(wraith.getEffects().stream()
            .anyMatch(effect -> effect.getType() == StatusEffectType.POISON));
        assertTrue(wraith.getHp() < wraith.getMaxHp());
    }

    @Test
    void lavaBurnsTheFirstPlayerBeforeTheyAct() {
        AuthoritativeMatch match = match(Environment.LAVA, defaultPlayers(), resolver());
        PlayerCombatState active = match.snapshot().getPlayers().stream()
            .filter(PlayerCombatState::isActiveTurn).findFirst().orElseThrow();
        assertEquals(0, active.getId());
        assertTrue(active.getHp() < active.getMaxHp());
        assertTrue(active.getEffects().stream()
            .anyMatch(effect -> effect.getType() == StatusEffectType.BURN));
    }

    @Test
    void serverRejectsMovementAcrossACanyonGap() {
        AuthoritativeMatch match = match(Environment.CANYON, defaultPlayers(), resolver());
        CombatCommandResult result = match.move(0, new Vector2(22.5f, 9.5f));
        assertFalse(result.isAccepted());
        assertTrue(result.getError().contains("unsafe"));
    }

    @Test
    void eldritchBlastCanPushATargetIntoTheCanyon() {
        List<LobbyPlayer> players = List.of(
            player(0, CharacterClass.MAGE, Race.ELF, 1, 7.5f, 4.5f),
            player(1, CharacterClass.PALADIN, Race.HUMAN, 1, 4f, 12f),
            player(2, CharacterClass.PALADIN, Race.DRAGONBORNE, 2, 8.5f, 4.5f),
            player(3, CharacterClass.ARCHER, Race.HUMAN, 2, 25f, 12f));
        AuthoritativeMatch match = match(Environment.CANYON, players, resolver());

        CombatCommandResult result = match.useAbility(0, AbilityType.ELDRITCH_BLAST, 2, null);
        assertTrue(result.isAccepted());
        assertEquals(0, player(result, 2).getHp());
        assertTrue(result.getState().getMessage().contains("pushed into the canyon"));
    }

    private CombatCommandResult endCurrentTurn(AuthoritativeMatch match) {
        int active = match.getActivePlayerId();
        CombatCommandResult result = match.endTurn(active);
        assertTrue(result.isAccepted());
        return result;
    }

    private List<LobbyPlayer> defaultPlayers() {
        return List.of(
            player(0, CharacterClass.WRAITH, Race.UNDEAD, 1, 6f, 3f),
            player(1, CharacterClass.MAGE, Race.ELF, 1, 4f, 12f),
            player(2, CharacterClass.PALADIN, Race.HUMAN, 2, 26f, 3f),
            player(3, CharacterClass.CLERIC, Race.DRAGONBORNE, 2, 26f, 12f));
    }

    private AuthoritativeMatch match(Environment environment, List<LobbyPlayer> players,
                                     AbilityResolver resolver) {
        return new AuthoritativeMatch(players, environment, resolver, false);
    }

    private AbilityResolver resolver() {
        return new AbilityResolver(new Random(1L) {
            @Override public int nextInt(int bound) { return 0; }
        });
    }

    private LobbyPlayer player(int id, CharacterClass characterClass, Race race,
                               int team, float x, float y) {
        return new LobbyPlayer(id, "session-" + id, "Player " + id,
            characterClass, race, team, new Vector2(x, y));
    }

    private PlayerCombatState player(CombatCommandResult result, int id) {
        return result.getState().getPlayers().stream()
            .filter(player -> player.getId() == id).findFirst().orElseThrow();
    }
}
