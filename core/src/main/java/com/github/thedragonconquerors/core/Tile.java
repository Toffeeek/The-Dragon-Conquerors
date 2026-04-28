package com.github.thedragonconquerors.core;

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

    //getters
    public int getGridX() {
        return gridX;
    }

    public int getGridY() {
        return gridY;
    }

    public float getWorldX() {
        return worldX;
    }

    public float getWorldY() {
        return worldY;
    }

    public boolean isWalkable() {
        return walkable;
    }

    public int getMovementCost() {
        return movementCost;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public HighlightState getHighlightState() {
        return highlightState;
    }

    //setters

    public void setWalkable(boolean walkable) {
        this.walkable = walkable;
    }

    public void setMovementCost(int movementCost) {
        this.movementCost = movementCost;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    public void setHighlightState(HighlightState highlightState) {
        this.highlightState = highlightState;
    }

    public boolean isPassable(){
        return walkable & !occupied;    //is this tile a valid destination (walkable and not occupied by another player)
    }
}
