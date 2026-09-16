package com.server.server.combat;

import com.badlogic.gdx.math.Vector2;
import com.server.server.selection.LobbyPlayer;
import com.shared.shared.model.*;
import com.shared.shared.model.ability.AbilityType;
import com.shared.shared.model.combat.AbilityResolver;
import com.shared.shared.model.world.*;
import com.shared.shared.network.PlayerCombatState;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Deterministic four-combatant battle, not an in-game bot or a win-rate simulation. */
class FullMatchLifecycleTest {
    @ParameterizedTest @EnumSource(Environment.class)
    void damageHealDeathReviveAndVictoryCompleteOnEachMap(Environment environment) {
        BattlefieldDefinition field = BattlefieldDefinition.forEnvironment(environment);
        Vector2 base = arena(field);
        List<LobbyPlayer> lobby = List.of(
            player(0, CharacterClass.PALADIN, 1, base),
            player(1, CharacterClass.CLERIC, 1, new Vector2(base).add(0, .8f)),
            player(2, CharacterClass.ARCHER, 2, new Vector2(base).add(.8f, 0)),
            player(3, CharacterClass.MAGE, 2, new Vector2(base).add(.8f, .8f)));
        AuthoritativeMatch match = new AuthoritativeMatch(lobby, environment,
            new AbilityResolver(new Random(1) { @Override public int nextInt(int bound) { return 0; } }));
        boolean healed = false, revived = false;
        for (int turn = 0; turn < 200 && !match.snapshot().isMatchOver(); turn++) {
            int id = match.getActivePlayerId();
            PlayerCombatState actor = get(match, id);
            AbilityType ability;
            int target;
            if (id == 1 && get(match, 0).getHp() == 0 && !revived) {
                ability = AbilityType.REVIVE; target = 0;
            } else if (id == 1 && !healed && get(match, 0).getHp() < get(match, 0).getMaxHp()) {
                ability = AbilityType.HEAL; target = 0;
            } else if (id == 1 && !revived) {
                assertTrue(match.endTurn(id).isAccepted()); continue;
            } else {
                ability = switch(id) {
                    case 0 -> AbilityType.PALADIN_BASIC;
                    case 1 -> AbilityType.CLERIC_BASIC;
                    case 2 -> AbilityType.ARROW_SHOT;
                    default -> AbilityType.MAGE_BASIC;
                };
                target = match.snapshot().getPlayers().stream()
                    .filter(p -> p.getTeamIndex() != actor.getTeamIndex() && p.getHp() > 0)
                    .findFirst().orElseThrow().getId();
            }
            CombatCommandResult result = match.useAbility(id, ability, target, null);
            assertTrue(result.isAccepted(), ability + ": " + result.getError());
            if (ability == AbilityType.HEAL) healed = true;
            if (ability == AbilityType.REVIVE) { revived = true; assertTrue(get(match, 0).getHp() > 0); }
            if (!match.snapshot().isMatchOver()) assertTrue(match.endTurn(id).isAccepted());
        }
        assertTrue(healed); assertTrue(revived);
        assertTrue(match.snapshot().isMatchOver(), "Battle must reach victory");
        assertTrue(match.snapshot().getWinningTeam() > 0);
        assertFalse(match.endTurn(0).isAccepted());
    }
    private PlayerCombatState get(AuthoritativeMatch match, int id) {
        return match.snapshot().getPlayers().stream().filter(p -> p.getId() == id).findFirst().orElseThrow();
    }
    private LobbyPlayer player(int id, CharacterClass type, int team, Vector2 position) {
        return new LobbyPlayer(id, "fixture-" + id, "Player " + id, type, Race.HUMAN, team, position);
    }
    private Vector2 arena(BattlefieldDefinition field) {
        for (float x = 2; x < 28; x += .5f) for (float y = 2; y < 15; y += .5f) {
            Vector2 p = new Vector2(x, y);
            if (field.isWalkable(p) && field.isWalkable(new Vector2(p).add(.8f, 0))
                && field.isWalkable(new Vector2(p).add(0, .8f)) && field.isWalkable(new Vector2(p).add(.8f, .8f))) return p;
        }
        throw new AssertionError("No safe fixture arena");
    }
}
