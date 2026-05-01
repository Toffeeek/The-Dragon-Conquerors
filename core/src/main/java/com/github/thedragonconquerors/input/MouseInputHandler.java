package com.github.thedragonconquerors.input;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.github.thedragonconquerors.core.GridManager;
import com.github.thedragonconquerors.core.MovementSystem;
import com.github.thedragonconquerors.core.Tile;
import com.github.thedragonconquerors.entities.Player;

import java.util.List;
import java.util.function.Consumer;

public class MouseInputHandler extends InputAdapter{
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final GridManager gridManager;
    private final MovementSystem movementSystem;
    private final Player player;
    private final Consumer<Tile> moveHandler;
    private final Vector3 screenToWorld = new Vector3(); //reusable vector to avoid allocation of each frame
    private boolean isLocalPlayerTurn = true;   //is currently the local player turn or not

    public MouseInputHandler(OrthographicCamera camera, Viewport viewport, GridManager gridManager, MovementSystem movementSystem, Player player){
        this(camera, viewport, gridManager, movementSystem, player, null);
    }

    public MouseInputHandler(OrthographicCamera camera, Viewport viewport, GridManager gridManager, MovementSystem movementSystem, Player player, Consumer<Tile> moveHandler){
        this.camera = camera;
        this.viewport = viewport;
        this.gridManager = gridManager;
        this.movementSystem = movementSystem;
        this.player = player;
        this.moveHandler = moveHandler;
    }

    //mouse hover to show the reachable tile
    @Override
    public boolean mouseMoved(int screenX, int screenY){
        if(!isLocalPlayerTurn || player.isMoving()) return false;

        Tile hovered = screenToTile(screenX, screenY);
        if(hovered == null) return false;

        //preview path only if the tile is in the reachable set
        if(movementSystem.getReachableTiles().contains(hovered)){
            movementSystem.computePath(player, hovered);
        }

        return false;
    }

    //left click to move player to reachable tile
    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button){
        if(!isLocalPlayerTurn || player.isMoving()) return false;
        if(button!= Input.Buttons.LEFT) return false;

        Tile clicked = screenToTile(screenX, screenY);
        if(clicked==null)   return false;

        List<Tile> path = movementSystem.getCurrentPath();

        if(!path.isEmpty() && path.get(path.size()-1) == clicked && movementSystem.getReachableTiles().contains(clicked)){
            movementSystem.movePlayer(player, path);
            if(moveHandler != null)
            {
                moveHandler.accept(clicked);
            }
        }

        return true;
    }

    private Tile screenToTile(int screenX, int screenY){
        screenToWorld.set(screenX, screenY, 0);
        camera.unproject(screenToWorld, viewport.getScreenX(), viewport.getScreenY(), viewport.getScreenWidth(), viewport.getScreenHeight());
        return gridManager.getTileAtWorld(screenToWorld.x, screenToWorld.y);
    }

    public void setLocalPlayerTurn(boolean isLocalPlayerTurn){
        this.isLocalPlayerTurn = isLocalPlayerTurn;
    }
}
