package com.github.thedragonconquerors.entities;

import com.badlogic.gdx.math.Vector2;
import com.github.thedragonconquerors.animation.PlayerAnimationController;
import com.github.thedragonconquerors.movement.MovementController;
import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.stats.StatCalculator;
import com.shared.shared.model.stats.StatComponent;
import com.shared.shared.model.TEAM;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

//@Builder
@Getter
public class Player
{
    @Setter
    private int ID;
    private final String username;
    @Setter
    private TEAM team;
    private final CharacterClass characterClass;
    private final Vector2 position;
    private final float speed;
    private final StatComponent stats;
    private final MovementController movementController;
    private final PlayerAnimationController animationController;

    public Player(int ID, String username, TEAM team, Vector2 position, CharacterClass characterClass) {
        this.ID = ID;
        this.username = username;
        this.team = team;
        this.characterClass = characterClass;
        this.stats = characterClass.createStats();
        this.position = new Vector2(position);
        this.speed = 5f;

        float maxDistance = StatCalculator.deriveMaxMovementDistance(stats);
        this.movementController = new MovementController(maxDistance);
        this.animationController = new PlayerAnimationController();
    }

    public void onTurnStart() {
        movementController.resetForNewTurn();
        stats.regenerateMana(StatCalculator.manaRegenPerTurn(stats));
    }

    public void setPosition(float x, float y) {
        position.set(x, y);
    }
}
