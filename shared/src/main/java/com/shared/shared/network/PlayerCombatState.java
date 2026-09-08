// File Location: shared/src/main/java/com/shared/shared/network/PlayerCombatState.java
package com.shared.shared.network;

import com.badlogic.gdx.math.Vector2;
import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.Race;
import com.shared.shared.model.effect.StatusEffect;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Serializable authoritative state for one combatant. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerCombatState {
    private int id;
    private String username;
    private CharacterClass characterClass;
    private Race race;
    private int teamIndex;
    private Vector2 position;
    private int hp;
    private int maxHp;
    private int mana;
    private int maxMana;
    private int accuracy;
    private int strength;
    private int speed;
    private int inspiration;
    private int wisdom;
    private float remainingMovement;
    private float maxMovement;
    private boolean actionUsed;
    @Builder.Default
    private int actionPoints = 1;
    private long movementSequence;
    @Builder.Default
    private List<Vector2> movementPath = new ArrayList<>();
    private boolean activeTurn;
    @Builder.Default
    private List<StatusEffect> effects = new ArrayList<>();
    @Builder.Default
    private Map<String, Integer> cooldowns = new LinkedHashMap<>();
}
