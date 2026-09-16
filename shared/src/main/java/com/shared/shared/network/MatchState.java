// File Location: shared/src/main/java/com/shared/shared/network/MatchState.java
package com.shared.shared.network;

import com.shared.shared.model.ability.AbilityType;
import com.badlogic.gdx.math.Vector2;
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
    private List<Integer> turnOrder = new ArrayList<>();
    @Builder.Default
    private List<BattleStatistics> statistics = new ArrayList<>();
    @lombok.Builder.Default
    private java.util.List<Integer> missedTargetIds = java.util.List.of();
    @Builder.Default
    private List<PlayerCombatState> players = new ArrayList<>();
    @Builder.Default
    private int activePlayerId = -1;
    @Builder.Default
    private int nextPlayerId = -1;
    private int roundNumber;
    private Environment environment;
    private boolean matchOver;
    private boolean testingMode;
    private int winningTeam;
    private String message;
    @Builder.Default
    private int lastActorId = -1;
    private AbilityType lastAbility;
    /** Monotonic ability event identity: unrelated snapshots must not replay an attack. */
    private long actionSequence;
    @Builder.Default
    private int lastTargetId = -1;
    private Vector2 lastTargetPoint;
    private Vector2 lastActionOrigin;
}
