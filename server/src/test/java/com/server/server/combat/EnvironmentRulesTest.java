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
import com.shared.shared.model.world.BattlefieldArtwork;
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
        List<LobbyPlayer> players = defaultPlayers();
        players.get(0).setPosition(BattlefieldArtwork.MAP3.worldPoint(1110, 440));
        AuthoritativeMatch match = match(Environment.BOG, players, resolver());
        assertTrue(match.move(0, BattlefieldArtwork.MAP3.worldPoint(1200, 510)).isAccepted());

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
    void lavaDoesNotBurnPlayersOnSafeSpawnGround() {
        AuthoritativeMatch match = new AuthoritativeMatch(defaultPlayers(), Environment.LAVA, resolver(), true);
        PlayerCombatState active = match.snapshot().getPlayers().stream()
            .filter(PlayerCombatState::isActiveTurn).findFirst().orElseThrow();
        assertEquals(0, active.getId());
        assertEquals(active.getMaxHp(), active.getHp());
        assertFalse(active.getEffects().stream()
            .anyMatch(effect -> effect.getType() == StatusEffectType.BURN));
    }

    @Test void crossingACrackBurnsForExactlyTwoOwnTurnsAfterLeavingIt() {
        List<LobbyPlayer> players = lavaPlayers();
        players.get(0).setPosition(p2(460,130));
        AuthoritativeMatch match = match(Environment.LAVA, players, resolver());
        CombatCommandResult moved = match.move(0, p2(460,210));
        assertTrue(moved.isAccepted(), moved.getError());
        PlayerCombatState burned = player(moved, 0);
        assertEquals(2, burned.getEffects().stream().filter(e -> e.getType() == StatusEffectType.BURN)
            .findFirst().orElseThrow().getRemainingTurns());
        assertEquals(burned.getMaxHp(), burned.getHp(), "Damage ticks on own turns, not each movement command");
        int damage = StatusEffectType.BURN.getDamagePerTurn();
        for (int round = 1; round <= 3; round++) {
            CombatCommandResult next = null;
            for (int turn = 0; turn < 4; turn++) next = endCurrentTurn(match);
            PlayerCombatState state = player(next, 0);
            assertEquals(burned.getMaxHp() - Math.min(round, 2) * damage, state.getHp());
            assertEquals(round == 1, state.getEffects().stream().anyMatch(e -> e.getType() == StatusEffectType.BURN));
        }
    }

    @Test void teleportOnlyBurnsOnLandingAndCannotLandInLava() {
        AuthoritativeMatch safe = match(Environment.LAVA, lavaPlayers(), resolver());
        assertFalse(safe.useAbility(0, AbilityType.TELEPORT, -1, p2(610,300)).isAccepted());
        CombatCommandResult safeLanding = safe.useAbility(0, AbilityType.TELEPORT, -1, p2(500,180));
        assertTrue(safeLanding.isAccepted());
        assertTrue(player(safeLanding, 0).getEffects().isEmpty(), "Blink does not touch cracks along its line");
        AuthoritativeMatch hot = match(Environment.LAVA, lavaPlayers(), resolver());
        CombatCommandResult hotLanding = hot.useAbility(0, AbilityType.TELEPORT, -1, p2(460,166));
        assertTrue(hotLanding.isAccepted());
        assertEquals(2, player(hotLanding, 0).getEffects().get(0).getRemainingTurns());
    }

    @Test void eldritchBlastKillsInLavaButParapetsStopThePush() {
        List<LobbyPlayer> players = lavaPlayers();
        players.set(0, player(0, CharacterClass.MAGE, Race.ELF, 1, 0, 0));
        players.get(0).setPosition(p2(430,180));
        players.get(2).setPosition(p2(500,180));
        AuthoritativeMatch open = match(Environment.LAVA, players, resolver());
        CombatCommandResult lethal = open.useAbility(0, AbilityType.ELDRITCH_BLAST, 2, null);
        assertTrue(lethal.isAccepted());
        assertEquals(0, player(lethal,2).getHp());
        assertTrue(lethal.getState().getMessage().contains("pushed into lava"));

        players.get(0).setPosition(p2(530,447));
        players.get(2).setPosition(p2(530,412));
        AuthoritativeMatch rail = match(Environment.LAVA, players, resolver());
        CombatCommandResult stopped = rail.useAbility(0, AbilityType.ELDRITCH_BLAST, 2, null);
        assertTrue(stopped.isAccepted());
        assertTrue(player(stopped,2).getHp() > 0);
        assertTrue(stopped.getState().getMessage().contains("stopped by terrain"));
    }

    private List<LobbyPlayer> lavaPlayers() {
        List<LobbyPlayer> players = new java.util.ArrayList<>(defaultPlayers());
        players.get(0).setPosition(p2(440,130));
        players.get(1).setPosition(p2(420,735));
        players.get(2).setPosition(p2(1430,560));
        players.get(3).setPosition(p2(1190,815));
        return players;
    }

    private Vector2 p2(float x, float y) { return BattlefieldArtwork.MAP2.worldPoint(x,y); }

    @Test
    void serverRejectsMovementAcrossACanyonGap() {
        AuthoritativeMatch match = match(Environment.CANYON, defaultPlayers(), resolver());
        CombatCommandResult result = match.move(0, BattlefieldArtwork.MAP1.worldPoint(410, 500));
        assertFalse(result.isAccepted());
        assertTrue(result.getError().contains("unsafe"));
    }

    @Test
    void eldritchBlastCanPushATargetIntoTheCanyon() {
        List<LobbyPlayer> players = List.of(
            player(0, CharacterClass.MAGE, Race.ELF, 1, 3.3f, 9f),
            player(1, CharacterClass.PALADIN, Race.HUMAN, 1, 4f, 12f),
            player(2, CharacterClass.PALADIN, Race.DRAGONBORNE, 2, 2.1f, 9f),
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
