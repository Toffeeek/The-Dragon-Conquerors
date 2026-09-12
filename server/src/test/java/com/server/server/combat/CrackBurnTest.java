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
import static org.junit.jupiter.api.Assertions.*;

class CrackBurnTest {
    private Vector2 at(float x, float y) { return new Vector2(x * 30f / 1672f, 17f - y * 17f / 941f); }

    private AuthoritativeMatch match() {
        return new AuthoritativeMatch(List.of(
            new LobbyPlayer(0, "a", "Walker", CharacterClass.WRAITH, Race.HUMAN, 1, at(405, 282)),
            new LobbyPlayer(1, "b", "Opponent", CharacterClass.PALADIN, Race.HUMAN, 2, at(1450, 530))),
            Environment.LAVA, new AbilityResolver(), false);
    }

    private PlayerCombatState walker(AuthoritativeMatch match) { return match.snapshot().getPlayers().get(0); }
    private int burnTurns(AuthoritativeMatch match) {
        return walker(match).getEffects().stream().filter(e -> e.getType() == StatusEffectType.BURN)
            .mapToInt(e -> e.getRemainingTurns()).findFirst().orElse(0);
    }
    private void nextWalkerTurn(AuthoritativeMatch match) {
        assertTrue(match.endTurn(0).isAccepted());
        assertTrue(match.endTurn(1).isAccepted());
        assertEquals(0, match.getActivePlayerId());
    }

    @Test void crossingCrackAppliesBurnEvenWhenDestinationIsSafeAndTicksExactlyTwice() {
        AuthoritativeMatch match = match();
        int hp = walker(match).getHp();
        assertTrue(match.move(0, at(495, 282)).isAccepted());
        assertEquals(2, burnTurns(match));
        assertEquals(hp, walker(match).getHp(), "Contact applies burn; damage waits until owner's next turn");
        assertTrue(match.snapshot().getMessage().contains("glowing crack"));
        nextWalkerTurn(match);
        assertEquals(hp - 8, walker(match).getHp());
        assertEquals(1, burnTurns(match));
        nextWalkerTurn(match);
        assertEquals(hp - 16, walker(match).getHp());
        assertEquals(0, burnTurns(match));
        nextWalkerTurn(match);
        assertEquals(hp - 16, walker(match).getHp());
    }

    @Test void standingStillOnCrackDoesNotExtendTheTwoTurnBurn() {
        AuthoritativeMatch match = match();
        assertTrue(match.move(0, at(460, 282)).isAccepted());
        assertEquals(2, burnTurns(match));
        int hp = walker(match).getHp();
        nextWalkerTurn(match);
        nextWalkerTurn(match);
        nextWalkerTurn(match);
        assertEquals(0, burnTurns(match));
        assertEquals(hp - 16, walker(match).getHp());
    }

    @Test void anotherCrossingRefreshesInsteadOfStackingBurn() {
        AuthoritativeMatch match = match();
        assertTrue(match.move(0, at(495, 282)).isAccepted());
        nextWalkerTurn(match);
        assertEquals(1, burnTurns(match));
        int hp = walker(match).getHp();
        assertTrue(match.move(0, at(405, 282)).isAccepted());
        assertEquals(2, burnTurns(match));
        assertEquals(1, walker(match).getEffects().stream().filter(e -> e.getType() == StatusEffectType.BURN).count());
        nextWalkerTurn(match);
        assertEquals(hp - 8, walker(match).getHp());
    }

    @Test void teleportOverCrackDoesNotBurnButLandingOnItDoes() {
        AuthoritativeMatch crossing = match();
        assertTrue(crossing.useAbility(0, AbilityType.TELEPORT, -1, at(495, 282)).isAccepted());
        assertEquals(0, burnTurns(crossing));
        AuthoritativeMatch landing = match();
        assertTrue(landing.useAbility(0, AbilityType.TELEPORT, -1, at(460, 282)).isAccepted());
        assertEquals(2, burnTurns(landing));
    }

    @Test void rejectedMoveDoesNotApplyBurn() {
        AuthoritativeMatch match = match();
        assertFalse(match.move(0, at(600, 280)).isAccepted());
        assertEquals(0, burnTurns(match));
        assertEquals(walker(match).getMaxHp(), walker(match).getHp());
    }
}
