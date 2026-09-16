package com.server.server.combat;

import com.badlogic.gdx.math.Vector2;
import com.server.server.selection.LobbyPlayer;
import com.shared.shared.model.*;
import com.shared.shared.model.ability.AbilityType;
import com.shared.shared.model.combat.AbilityResolver;
import com.shared.shared.model.world.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BattleStatisticsTest {
    private LobbyPlayer player(int id,CharacterClass type,int team,Vector2 point) {
        return new LobbyPlayer(id,"s"+id,"P"+id,type,Race.HUMAN,team,point);
    }
    private AbilityResolver hit() { return new AbilityResolver(new Random(){@Override public int nextInt(int bound){return 0;}}); }
    @Test void ledgerCountsActualHealingDamageAndEnvironmentalKillsWithoutMutatingSnapshots() {
        var ledger=new BattleLedger(List.of(player(0,CharacterClass.MAGE,1,new Vector2()),player(1,CharacterClass.CLERIC,2,new Vector2())));
        ledger.health(0,1,10,0);ledger.health(1,1,5,8);ledger.turn(0);ledger.environmentalKill(0);
        var result=ledger.snapshot();
        assertEquals(10,result.get(0).getDamageDealt());assertEquals(2,result.get(0).getEliminations());
        assertEquals(1,result.get(0).getEnvironmentalKills());assertEquals(1,result.get(0).getTurnsPlayed());
        assertEquals(3,result.get(1).getHealingDone());
        result.get(0).setDamageDealt(999);assertEquals(10,ledger.snapshot().get(0).getDamageDealt());
        ledger.damage(-1,50,true);assertEquals(10,ledger.snapshot().get(0).getDamageDealt());
    }
    @Test void authoritativeDamageTurnsOrderAndPresenceSurviveSnapshotsAndDisconnect() {
        Vector2 start=BattlefieldArtwork.MAP1.worldPoint(230,410);
        var match=new AuthoritativeMatch(List.of(player(0,CharacterClass.MAGE,1,start),
            player(1,CharacterClass.PALADIN,2,new Vector2(start).add(.8f,0))),Environment.CANYON,hit(),false);
        assertEquals(List.of(0,1),match.snapshot().getTurnOrder());
        int before=match.snapshot().getPlayers().get(1).getHp();
        var result=match.useAbility(0,AbilityType.MAGE_BASIC,1,null);assertTrue(result.isAccepted());
        int actual=before-result.getState().getPlayers().get(1).getHp();
        assertEquals(actual,result.getState().getStatistics().get(0).getDamageDealt());
        assertFalse(match.useAbility(0,AbilityType.MAGE_BASIC,1,null).isAccepted());
        assertEquals(actual,match.snapshot().getStatistics().get(0).getDamageDealt());
        assertFalse(match.setConnected(0,false).getPlayers().getFirst().isConnected());
        assertTrue(match.setConnected(0,true).getPlayers().getFirst().isConnected());
        assertEquals(1,match.snapshot().getStatistics().get(0).getTurnsPlayed());
        match.disconnect(0);assertEquals(2,match.snapshot().getStatistics().size());
        assertEquals(actual,match.snapshot().getStatistics().get(0).getDamageDealt());
    }
    @Test void burnDamageIsAttributedToItsCasterOnTheVictimsTurn() {
        Vector2 start=BattlefieldArtwork.MAP1.worldPoint(230,410);
        var match=new AuthoritativeMatch(List.of(player(0,CharacterClass.MAGE,1,start),
            player(1,CharacterClass.PALADIN,2,new Vector2(start).add(.8f,0))),Environment.CANYON,hit(),false);
        var cast=match.useAbility(0,AbilityType.FIREBALL,1,null);assertTrue(cast.isAccepted());
        int direct=cast.getState().getStatistics().getFirst().getDamageDealt();
        var tick=match.endTurn(0).getState();
        assertEquals(direct+8,tick.getStatistics().getFirst().getDamageDealt());
    }
}
