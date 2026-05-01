package com.github.thedragonconquerors.entities;

import com.badlogic.gdx.math.Vector2;
import com.github.thedragonconquerors.core.GridManager;
import com.github.thedragonconquerors.core.Tile;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Player {

    private final int ID;
    private final String username;

    private int gridX;
    private int gridY;

    private final Vector2 worldPos = new Vector2();
    private final Vector2 targetWorldPos = new Vector2();

    private final int maxStamina;
    private int remainingStamina;

    //animation state
    private boolean isMoving = false;
    private List<Tile> movementPath = new ArrayList<>();
    private int pathIndex = 0;
    private static final float MOVE_SPEED = 5f;

    public Player(int ID, String username, int startGridX, int startGridY, int maxStamina)
    {
        this.ID = ID;
        this.username = username;
        this.gridX = startGridX;
        this.gridY = startGridY;
        this.maxStamina = maxStamina;
        this.remainingStamina = maxStamina;

        float worldSize = GridManager.TILE_WORLD_SIZE;



        worldPos.set(startGridX*worldSize + worldSize/2f,   //center the player on the starting tile
                     startGridY*worldSize + worldSize/2f);
        targetWorldPos.set(worldPos);
    }

    //animation: step through path tile by tile
    public void update(float delta){    //called each frame. moves world position to the next tile in the path. when it arrives, advances to the next tile
        if(!isMoving)   return;

        float step = MOVE_SPEED*delta;
        float dist = worldPos.dst(targetWorldPos);

        if(dist<=step)
        {
            worldPos.set(targetWorldPos);
            pathIndex++;

            if(pathIndex>=movementPath.size()){
                isMoving = false;   //reached the destination
                movementPath.clear();
            }
            else    setTargetToTile(movementPath.get(pathIndex));   //if not reached then advance to next tile
        }
        else
        {
            float ratio = step/dist;
            worldPos.lerp(targetWorldPos, ratio);
        }
    }

    //begins movement animation along given tile path
    public void startMovementAnimation(List<Tile> path){
        if(path.isEmpty())  return;

        movementPath = new ArrayList<>(path);
        pathIndex = 0;
        isMoving = true;
        setTargetToTile(path.get(0));
    }

    private void setTargetToTile(Tile tile){
        float ws = GridManager.TILE_WORLD_SIZE;
        targetWorldPos.set(tile.getWorldX() + ws/2f, tile.getWorldY() + ws/2f);
    }

    //turn manager
    public void resetStamina(){  //resets stamina at the start of each turn
        remainingStamina = maxStamina;
    }

    public void deductStamina(int cost){
        remainingStamina = Math.max(0, remainingStamina-cost);
    }


    public void setGridPosition(int x, int y){
        this.gridX = x;
        this.gridY = y;
    }
}
