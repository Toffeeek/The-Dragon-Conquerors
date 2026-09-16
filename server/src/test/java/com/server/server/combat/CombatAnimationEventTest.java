package com.server.server.combat;

import com.badlogic.gdx.math.Vector2;
import com.server.server.selection.LobbyPlayer;
import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.Race;
import com.shared.shared.model.ability.AbilityType;
import com.shared.shared.model.combat.AbilityResolver;
import com.shared.shared.model.world.BattlefieldArtwork;
import com.shared.shared.model.world.Environment;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class CombatAnimationEventTest {
    @Test void missedAttacksStillSendTheirActualTargetAndDoNotReplayOnTurnEnd() {
        Vector2 start = BattlefieldArtwork.MAP1.worldPoint(230,410);
        Vector2 target = new Vector2(start).add(.8f,0);
        var players = List.of(player(0, CharacterClass.MAGE, 1, start), player(1, CharacterClass.PALADIN, 2, target));
        var resolver = new AbilityResolver(new Random() { @Override public int nextInt(int bound) { return bound-1; } });
        AuthoritativeMatch match = new AuthoritativeMatch(players, Environment.CANYON, resolver, false);
        int active = match.getActivePlayerId();
        if (active != 0) match.endTurn(active);
        var result = match.useAbility(0, AbilityType.MAGE_BASIC, 1, null);
        assertTrue(result.isAccepted());
        var state = result.getState();
        assertEquals(1, state.getActionSequence());
        assertEquals(1, state.getLastTargetId());
        assertEquals(target, state.getLastTargetPoint());
        assertEquals(start, state.getLastActionOrigin());
        state.getLastTargetPoint().set(-100,-100);
        assertEquals(target, match.snapshot().getLastTargetPoint(), "Snapshots cannot mutate stored event positions");
        assertEquals(1, match.endTurn(0).getState().getActionSequence());
        assertNull(match.snapshot().getLastAbility());
    }

    @Test void teleportIncludesBothEndpointsAndRejectedCommandsDoNotCreateEvents() {
        Vector2 start = BattlefieldArtwork.MAP1.worldPoint(230,410);
        Vector2 destination = BattlefieldArtwork.MAP1.worldPoint(1490,550);
        var players = List.of(player(0, CharacterClass.WRAITH, 1, start),
            player(1, CharacterClass.PALADIN, 2, BattlefieldArtwork.MAP1.worldPoint(1490,410)));
        AuthoritativeMatch match = new AuthoritativeMatch(players, Environment.CANYON, new AbilityResolver(), false);
        assertFalse(match.useAbility(0, AbilityType.TELEPORT, -1, new Vector2(-1,-1)).isAccepted());
        assertEquals(0, match.snapshot().getActionSequence());
        var result = match.useAbility(0, AbilityType.TELEPORT, -1, destination);
        assertTrue(result.isAccepted());
        assertEquals(start, result.getState().getLastActionOrigin());
        assertEquals(destination, result.getState().getLastTargetPoint());
        assertEquals(1, result.getState().getActionSequence());
    }
    private LobbyPlayer player(int id, CharacterClass type, int team, Vector2 position) {
        return new LobbyPlayer(id, "test-"+id, "Test "+id, type, Race.HUMAN, team, position);
    }
}
