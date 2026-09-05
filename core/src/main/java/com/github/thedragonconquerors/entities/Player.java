// File Location: core/src/main/java/com/github/thedragonconquerors/entities/Player.java
package com.github.thedragonconquerors.entities;

import com.badlogic.gdx.math.Vector2;
import com.github.thedragonconquerors.animation.PlayerAnimationController;
import com.github.thedragonconquerors.movement.MovementController;
import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.CharacterBuild;
import com.shared.shared.model.Race;
import com.shared.shared.model.stats.StatCalculator;
import com.shared.shared.model.stats.StatComponent;
import com.shared.shared.model.ability.AbilityType;
import com.shared.shared.model.combat.Combatant;
import com.shared.shared.model.effect.StatusEffect;
import com.shared.shared.network.PlayerCombatState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Player implements Combatant {
    private final int ID;
    private final String username;
    private final CharacterClass characterClass;
    private final Race race;
    private final int teamIndex;
    private final Vector2 position;
    private final float speed;
    private final StatComponent stats;
    private final MovementController movementController;
    private final PlayerAnimationController animationController;
    private final List<StatusEffect> activeEffects = new ArrayList<>();
    private final Map<String, Integer> cooldowns = new LinkedHashMap<>();
    private boolean actionUsed;
    private int actionPoints = 1;
    private long movementSequence = -1;
    private boolean activeTurn;

    public Player(int ID, String username, Vector2 position, CharacterClass characterClass) {
        this(ID, username, position,
            CharacterBuild.of(CharacterBuild.DEFAULT_RACE, characterClass), 0);
    }

    public Player(int ID, String username, Vector2 position,
                  CharacterBuild build, int teamIndex) {
        this.ID = ID;
        this.username = username;
        this.characterClass = build.getCharacterClass();
        this.race = build.getRace();
        this.teamIndex = teamIndex;
        this.stats = build.createStats();
        this.position = new Vector2(position);
        this.speed = 5f;

        float maxDistance = StatCalculator.deriveMaxMovementDistance(stats);
        this.movementController = new MovementController(maxDistance);
        this.animationController = new PlayerAnimationController();
    }

    public int getID() { return ID; }

    @Override public int getId() { return ID; }

    public String getUsername() { return username; }

    public CharacterClass getCharacterClass() { return characterClass; }

    public Race getRace() { return race; }

    public int getTeamIndex() { return teamIndex; }

    public Vector2 getPosition() { return position; }

    public float getSpeed() { return speed; }

    public StatComponent getStats() { return stats; }

    public MovementController getMovementController() { return movementController; }

    public PlayerAnimationController getAnimationController() { return animationController; }

    @Override public List<StatusEffect> getActiveEffects() { return activeEffects; }

    public boolean isActionUsed() { return actionUsed; }
    public int getActionPoints() { return actionPoints; }
    public long getMovementSequence() { return movementSequence; }

    public boolean isActiveTurn() { return activeTurn; }

    public int cooldownTurns(AbilityType ability) {
        if (ability == null) return 0;
        return cooldowns.getOrDefault(ability.getDisplayName(), 0);
    }

    public void applyCombatState(PlayerCombatState state) {
        if (state == null || state.getId() != ID) return;
        stats.setMaxHp(state.getMaxHp());
        stats.setHp(state.getHp());
        stats.setMaxMana(state.getMaxMana());
        stats.setMana(state.getMana());
        stats.setAccuracy(state.getAccuracy());
        stats.setStrength(state.getStrength());
        stats.setSpeed(state.getSpeed());
        stats.setInspiration(state.getInspiration());
        stats.setWisdom(state.getWisdom());
        actionUsed = state.isActionUsed();
        actionPoints = state.getActionPoints();
        movementSequence = state.getMovementSequence();
        activeTurn = state.isActiveTurn();
        activeEffects.clear();
        if (state.getEffects() != null) activeEffects.addAll(state.getEffects());
        cooldowns.clear();
        if (state.getCooldowns() != null) cooldowns.putAll(state.getCooldowns());
        movementController.synchronizeMovement(
            state.getMaxMovement(), state.getRemainingMovement());
    }

    public void onTurnStart() {
        movementController.resetForNewTurn();
        stats.regenerateMana(StatCalculator.manaRegenPerTurn(stats));
    }

    public void setPosition(float x, float y) {
        position.set(x, y);
    }
}
