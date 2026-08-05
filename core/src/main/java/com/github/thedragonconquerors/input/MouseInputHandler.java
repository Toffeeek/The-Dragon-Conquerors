package com.github.thedragonconquerors.input;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.github.thedragonconquerors.entities.Player;
import com.github.thedragonconquerors.movement.MovementSystem;
import lombok.Setter;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

/** Handles world clicks for target selection first, then movement. */
public class MouseInputHandler extends InputAdapter {
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final Player player;
    private final MovementSystem movementSystem;
    private final Function<Vector2, Boolean> onWorldClick;
    private final Consumer<Vector2> onMoveCallback;
    private final BooleanSupplier localPlayerActiveCheck;
    private final Vector3 unprojectScratch = new Vector3();
    @Setter
    private boolean isLocalPlayerTurn = true;

    public MouseInputHandler(OrthographicCamera camera, Viewport viewport,
                             Player player, MovementSystem movementSystem,
                             Function<Vector2, Boolean> onWorldClick,
                             Consumer<Vector2> onMoveCallback,
                             BooleanSupplier localPlayerActiveCheck) {
        this.camera = camera;
        this.viewport = viewport;
        this.player = player;
        this.movementSystem = movementSystem;
        this.onWorldClick = onWorldClick;
        this.onMoveCallback = onMoveCallback;
        this.localPlayerActiveCheck = localPlayerActiveCheck;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (!isLocalPlayerTurn || button != Input.Buttons.LEFT) return false;
        if (localPlayerActiveCheck != null && !localPlayerActiveCheck.getAsBoolean()) return false;

        unprojectScratch.set(screenX, screenY, 0f);
        camera.unproject(unprojectScratch,
            viewport.getScreenX(), viewport.getScreenY(),
            viewport.getScreenWidth(), viewport.getScreenHeight());

        Vector2 clickedWorldPosition = new Vector2(unprojectScratch.x, unprojectScratch.y);

        // Target-selection mode consumes the click so it never also becomes movement.
        if (onWorldClick != null && Boolean.TRUE.equals(onWorldClick.apply(clickedWorldPosition))) {
            return true;
        }

        if (!movementSystem.setDestination(player, clickedWorldPosition)) {
            return true;
        }

        Vector2 target = player.getMovementController().getTargetPosition();
        if (target != null && onMoveCallback != null) onMoveCallback.accept(target);
        return true;
    }
}
