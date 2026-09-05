package com.server.server.combat;

import com.badlogic.gdx.math.Vector2;
import com.server.server.selection.LobbyPlayer;
import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.Race;
import com.shared.shared.model.ability.AbilityType;
import com.shared.shared.model.world.Environment;
import com.shared.shared.network.PlayerCombatState;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TurnResourceTest {
    @Test void actionThenMovementAutomaticallyEndsTurn() {
        AuthoritativeMatch match = match(false);
        assertEquals(1, player(match, 0).getActionPoints());
        assertTrue(match.useAbility(0, AbilityType.ACCURACY_BOOST, 0, null).isAccepted());
        assertEquals(0, player(match, 0).getActionPoints());
        assertEquals(0, match.getActivePlayerId(), "Movement is still available");
        assertFalse(match.useAbility(0, AbilityType.ACCURACY_BOOST, 0, null).isAccepted());
        assertTrue(match.move(0, new Vector2(8.5f, 5f)).isAccepted());
        assertEquals(0f, player(match, 0).getRemainingMovement(), 0.001f);
        assertEquals(1, match.getActivePlayerId());
        assertTrue(match.snapshot().getMessage().contains("automatically"));
        match.endTurn(1);
        assertEquals(1, player(match, 0).getActionPoints());
        assertEquals(player(match, 0).getMaxMovement(), player(match, 0).getRemainingMovement());
    }

    @Test void movementThenActionAutomaticallyEndsTurn() {
        AuthoritativeMatch match = match(false);
        assertTrue(match.move(0, new Vector2(8.5f, 5f)).isAccepted());
        assertEquals(0, match.getActivePlayerId(), "The action point is still available");
        assertEquals(1, player(match, 0).getActionPoints());
        assertFalse(match.move(0, new Vector2(2f, 5f)).isAccepted());
        assertTrue(match.useAbility(0, AbilityType.ACCURACY_BOOST, 0, null).isAccepted());
        assertEquals(1, match.getActivePlayerId());
    }

    @Test void soloAutoEndRefreshesResourcesWithoutEndingPractice() {
        AuthoritativeMatch match = match(true);
        match.useAbility(0, AbilityType.ACCURACY_BOOST, 0, null);
        match.move(0, new Vector2(8.5f, 5f));
        assertEquals(0, match.getActivePlayerId());
        assertFalse(match.snapshot().isMatchOver());
        assertTrue(match.snapshot().getRoundNumber() > 1);
        assertEquals(1, player(match, 0).getActionPoints());
        assertEquals(player(match, 0).getMaxMovement(), player(match, 0).getRemainingMovement());
        assertFalse(player(match, 0).getMovementPath().isEmpty());
    }

    @Test void invalidAbilityAndBlockedMovementDoNotSpendPoints() {
        AuthoritativeMatch match = match(false);
        float movement = player(match, 0).getRemainingMovement();
        assertFalse(match.useAbility(0, AbilityType.FIREBALL, 1, null).isAccepted());
        assertFalse(match.move(0, new Vector2(22.5f, 9.5f)).isAccepted());
        assertEquals(1, player(match, 0).getActionPoints());
        assertEquals(movement, player(match, 0).getRemainingMovement());
        assertEquals(0, match.getActivePlayerId());
    }

    private AuthoritativeMatch match(boolean solo) {
        LobbyPlayer first = new LobbyPlayer(0, "a", "Archer", CharacterClass.ARCHER, Race.HUMAN, 1, new Vector2());
        LobbyPlayer second = new LobbyPlayer(1, "b", "Opponent", CharacterClass.ARCHER, Race.HUMAN, 2, new Vector2());
        return new AuthoritativeMatch(solo ? List.of(first) : List.of(first, second), Environment.CANYON, solo);
    }
    private PlayerCombatState player(AuthoritativeMatch match, int id) {
        return match.snapshot().getPlayers().stream().filter(p -> p.getId() == id).findFirst().orElseThrow();
    }
}
