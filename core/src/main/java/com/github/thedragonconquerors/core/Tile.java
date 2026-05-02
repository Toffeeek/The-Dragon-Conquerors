package com.github.thedragonconquerors.core;

import lombok.Setter;
import lombok.Getter;

@Getter
@Setter
public class Tile {
    public enum HighlightState{
        NONE,       //normal tile
        REACHABLE,  //within player range
        PATH,       //tiles that are available to go on the turn
        SELECTED    //destination the player wants to go
    }

    private final int gridX;
    private final int gridY;
    private final float worldX;
    private final float worldY;


    private boolean walkable;
    private int movementCost;
    private boolean occupied;   //true if a player stands here
    private HighlightState highlightState;

    public Tile(int gridX, int gridY, float tileWorldSize){
        this.gridX = gridX;
        this.gridY = gridY;
        this.worldX = gridX*tileWorldSize;
        this.worldY = gridY*tileWorldSize;
        this.walkable = true;
        this.movementCost =1;
        this.occupied = false;
        this.highlightState = HighlightState.NONE;
    }

    public boolean isPassable(){
        return walkable & !occupied;    //is this tile a valid destination (walkable and not occupied by another player)
    }
}
