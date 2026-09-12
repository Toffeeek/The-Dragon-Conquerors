// File Location: server/src/test/java/com/server/server/combat/LavaPushTest.java
package com.server.server.combat;

import com.badlogic.gdx.math.Vector2;
import com.server.server.selection.LobbyPlayer;
import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.Race;
import com.shared.shared.model.ability.AbilityType;
import com.shared.shared.model.combat.AbilityResolver;
import com.shared.shared.model.world.Environment;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class LavaPushTest {
    private Vector2 at(float x,float y) { return new Vector2(x*30f/1672f,17f-y*17f/941f); }
    private LobbyPlayer player(int id, CharacterClass type,int team,float x,float y) {
        return new LobbyPlayer(id,"session"+id,"Player"+id,type,Race.HUMAN,team,at(x,y));
    }
    private AuthoritativeMatch match(boolean rail) {
        return new AuthoritativeMatch(List.of(
            player(0,CharacterClass.MAGE,1,rail?520:824,rail?440:225),
            player(1,CharacterClass.PALADIN,2,rail?520:824,rail?405:182)), Environment.LAVA,
            new AbilityResolver(new Random(1L) { @Override public int nextInt(int bound) { return 0; } }),false);
    }
    @Test void blastIntoLavaKillsImmediatelyAndResolvesVictory() {
        AuthoritativeMatch match=match(false);
        var result=match.useAbility(0,AbilityType.ELDRITCH_BLAST,1,null);
        assertTrue(result.isAccepted());
        assertEquals(0,result.getState().getPlayers().get(1).getHp());
        assertTrue(result.getState().isMatchOver());
        assertEquals(1,result.getState().getWinningTeam());
        assertTrue(result.getState().getMessage().contains("lava and dies instantly"));
    }
    @Test void railStopsBlastAndDoesNotKillPlayerInLavaBehindIt() {
        AuthoritativeMatch match=match(true);
        var result=match.useAbility(0,AbilityType.ELDRITCH_BLAST,1,null);
        assertTrue(result.isAccepted());
        assertTrue(result.getState().getPlayers().get(1).getHp()>0);
        assertFalse(result.getState().isMatchOver());
        assertTrue(result.getState().getMessage().contains("stopped by terrain"));
    }
    @Test void walkingAndTeleportingIntoLavaAreRejectedWithoutSpendingResources() {
        AuthoritativeMatch match=new AuthoritativeMatch(List.of(player(0,CharacterClass.WRAITH,1,824,225)),Environment.LAVA,true);
        assertFalse(match.move(0,at(1000,100)).isAccepted());
        assertFalse(match.useAbility(0,AbilityType.TELEPORT,-1,at(1000,100)).isAccepted());
        assertEquals(1,match.snapshot().getPlayers().get(0).getActionPoints());
        assertEquals(match.snapshot().getPlayers().get(0).getMaxMovement(),match.snapshot().getPlayers().get(0).getRemainingMovement());
    }
}
