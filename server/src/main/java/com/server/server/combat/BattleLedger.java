package com.server.server.combat;

import com.server.server.selection.LobbyPlayer;
import com.shared.shared.network.BattleStatistics;
import java.util.*;

/** Counts actual HP changes, not attempted/overkill damage. Snapshot reads are side-effect free. */
final class BattleLedger {
    private final Map<Integer, BattleStatistics> rows = new LinkedHashMap<>();
    BattleLedger(List<LobbyPlayer> players) {
        for (var p : players) rows.put(p.getId(), new BattleStatistics(p.getId(),p.getUsername(),
            p.getCharacterClass(),p.getTeamIndex(),0,0,0,0,0));
    }
    void turn(int id) { var row=rows.get(id); if(row!=null) row.setTurnsPlayed(row.getTurnsPlayed()+1); }
    void health(int actor, int target, int before, int after) {
        var row=rows.get(actor);
        if(row==null) return;
        if(after>before) row.setHealingDone(row.getHealingDone()+after-before);
        else if(actor!=target) damage(actor,Math.max(0,before-after),before>0 && after<=0);
    }
    void damage(int actor, int actual, boolean eliminated) {
        var row=rows.get(actor); if(row==null) return; // Environmental status damage has no player owner.
        row.setDamageDealt(row.getDamageDealt()+Math.max(0,actual));
        if(eliminated) row.setEliminations(row.getEliminations()+1);
    }
    void environmentalKill(int actor) {
        var row=rows.get(actor); if(row==null) return;
        row.setEnvironmentalKills(row.getEnvironmentalKills()+1);
        row.setEliminations(row.getEliminations()+1);
    }
    List<BattleStatistics> snapshot() { return rows.values().stream().map(BattleStatistics::copy).toList(); }
}
