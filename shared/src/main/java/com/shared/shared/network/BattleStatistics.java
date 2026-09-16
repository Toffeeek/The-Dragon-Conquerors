package com.shared.shared.network;

import com.shared.shared.model.CharacterClass;
import lombok.*;

/** Server-owned lifetime totals. Rows remain available after a player disconnects. */
@Data @NoArgsConstructor @AllArgsConstructor
public class BattleStatistics {
    private int playerId;
    private String username;
    private CharacterClass characterClass;
    private int teamIndex;
    private int damageDealt;
    private int healingDone;
    private int eliminations;
    private int environmentalKills;
    private int turnsPlayed;
    public BattleStatistics copy() {
        return new BattleStatistics(playerId, username, characterClass, teamIndex,
            damageDealt, healingDone, eliminations, environmentalKills, turnsPlayed);
    }
}
