package com.github.thedragonconquerors.entities;

import com.badlogic.gdx.math.Vector2;
import com.github.thedragonconquerors.animation.PlayerAnimationController;
import com.github.thedragonconquerors.movement.MovementController;
import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.stats.StatCalculator;
import com.shared.shared.model.stats.StatComponent;

public class Player {
    private final int ID;
    private final String username;
    private final CharacterClass characterClass;
    private final Vector2 position;
    private final float speed;
    private final StatComponent stats;
    private final MovementController movementController;
    private final PlayerAnimationController animationController;

    public Player(int ID, String username, Vector2 position, CharacterClass characterClass) {
        this.ID = ID;
        this.username = username;
        this.characterClass = characterClass;
        this.stats = characterClass.createStats();
        this.position = new Vector2(position);
        this.speed = 5f;

        float maxDistance = StatCalculator.deriveMaxMovementDistance(stats);
        this.movementController = new MovementController(maxDistance);
        this.animationController = new PlayerAnimationController();
    }

    public int getID() { return ID; }

    public String getUsername() { return username; }

    public CharacterClass getCharacterClass() { return characterClass; }

    public Vector2 getPosition() { return position; }

    public float getSpeed() { return speed; }

    public StatComponent getStats() { return stats; }

    public MovementController getMovementController() { return movementController; }

    public PlayerAnimationController getAnimationController() { return animationController; }

    public void onTurnStart() {
        movementController.resetForNewTurn();
        stats.regenerateMana(StatCalculator.manaRegenPerTurn(stats));
    }

    public void setPosition(float x, float y) {
        position.set(x, y);
    }
}
