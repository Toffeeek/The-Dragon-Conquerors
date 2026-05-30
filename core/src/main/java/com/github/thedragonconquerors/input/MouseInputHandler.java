package com.github.thedragonconquerors.input;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.github.thedragonconquerors.entities.Player;
import com.github.thedragonconquerors.movement.MovementSystem;
import com.github.thedragonconquerors.rendering.HudRenderer;
import com.github.thedragonconquerors.combat.ActionType;
import com.github.thedragonconquerors.stats.StatComponent;

import java.util.function.Consumer;

public class MouseInputHandler extends InputAdapter {
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final Player player;
    private final MovementSystem movementSystem;

    private final Consumer<Vector2> onMoveCallBack;
    private HudRenderer hudRenderer;
    private ActionType[] availableActions;
    private StatComponent stats;

    private final Vector3 unprojectScratch = new Vector3(); //reusable unproject vector
    private boolean isLocalPlayerTurn = true;

    public MouseInputHandler(OrthographicCamera camera, Viewport viewport, Player player, MovementSystem movementSystem, Consumer<Vector2> onMoveCallBack) {
        this.camera = camera;
        this.viewport = viewport;
        this.player = player;
        this.movementSystem = movementSystem;
        this.onMoveCallBack = onMoveCallBack;
    }

    public void setHudContext(HudRenderer hudRenderer, ActionType[] actions, StatComponent stats) {
        this.hudRenderer      = hudRenderer;
        this.availableActions = actions;
        this.stats            = stats;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if(!isLocalPlayerTurn)  return false;
        if(button != Input.Buttons.LEFT)    return false;

        // let the HUD consume the click first (action panel, toggle button)
        if (hudRenderer != null && availableActions != null && stats != null) {
            if (hudRenderer.handleClick(screenX, screenY, availableActions, stats)) return true;
        }

        //convert screen pixels to world coordinates
        unprojectScratch.set(screenX, screenY, 0);
        camera.unproject(unprojectScratch, viewport.getScreenX(), viewport.getScreenY(), viewport.getScreenWidth(), viewport.getScreenHeight());

        Vector2 clickedWorldPos = new Vector2(unprojectScratch.x, unprojectScratch.y);

        // Run A* and store path in MovementController
        movementSystem.setDestination(player, clickedWorldPos);

        // Send final waypoint to server
        Vector2 target = player.getMovementController().getTargetPosition();
        if (target != null) onMoveCallBack.accept(target);

        return true;
    }

    public void setLocalPlayerTurn(boolean localPlayerTurn) {
        this.isLocalPlayerTurn = localPlayerTurn;
    }
}
