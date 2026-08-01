package com.github.thedragonconquerors.entities;

import com.badlogic.gdx.math.Vector2;
import com.github.thedragonconquerors.movement.MovementController;
import com.github.thedragonconquerors.stats.StatCalculator;
import com.github.thedragonconquerors.stats.StatComponent;

public class Player {
    //server side
    private final int ID;
    private final String username;
    private final CharacterClass characterClass;

    private final Vector2 position; //world-space position
    private final float speed;  //movement speed in world units per second

    private final StatComponent stats;
    private final MovementController movementController;

    public Player(int ID, String username, float startX, float startY, CharacterClass characterClass, StatComponent stats)
    {
        this.ID = ID;
        this.username = username;
        this.characterClass = characterClass;

        this.stats = stats;
        this.position = new Vector2(startX, startY);
        this.speed = 5f;

        //max movement distance per turn derived from speed stat
        float maxDistance = StatCalculator.deriveMaxMovementDistance(stats);
        this.movementController = new MovementController(maxDistance);
    }

    public Player(int ID, String username, float startX, float startY, CharacterClass characterClass)
    {
        this(ID, username, startX, startY, characterClass, characterClass.createStats());
    }

    public Player(int ID, String username, Vector2 startingPosition)
    {
        this.ID = ID;
        this.username = username;
        this.position = new Vector2(startingPosition);
        this.characterClass = CharacterClass.WARRIOR;
        this.stats = StatComponent.defaultStats();
        this.speed = 5f;

        //max movement distance per turn derived from speed stat
        float maxDistance = StatCalculator.deriveMaxMovementDistance(stats);
        this.movementController = new MovementController(maxDistance);
    }

    public Player(int ID, String username, float startX, float startY){
        this(ID, username, startX, startY, CharacterClass.WARRIOR, StatComponent.defaultStats());
    }

    public void onTurnStart(){
        movementController.resetForNewTurn();
        stats.regenerateMana(StatCalculator.manaRegenPerTurn(stats));
    }

    public void setPosition(float x, float y){
        position.set(x, y);
    }

    public Vector2 getPosition() {
        return position;
    }

    public float getSpeed() {
        return speed;
    }

    public String getUsername() {
        return username;
    }

    public CharacterClass getCharacterClass() {
        return characterClass;
    }

    public StatComponent getStats() {
        return stats;
    }

    public MovementController getMovementController() {
        return movementController;
    }
}
