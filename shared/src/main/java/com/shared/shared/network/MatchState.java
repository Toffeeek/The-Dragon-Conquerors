// File Location: shared/src/main/java/com/shared/shared/network/MatchState.java
package com.shared.shared.network;

import com.shared.shared.model.ability.AbilityType;
import com.shared.shared.model.world.Environment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Full combat snapshot broadcast after every accepted command and turn transition. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchState {
    @Builder.Default
    private List<PlayerCombatState> players = new ArrayList<>();
    @Builder.Default
    private int activePlayerId = -1;
    private int roundNumber;
    private Environment environment;
    private boolean matchOver;
    private boolean testingMode;
    private int winningTeam;
    private String message;
    @Builder.Default
    private int lastActorId = -1;
    private AbilityType lastAbility;
}
