package com.github.thedragonconquerors.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.github.thedragonconquerors.entities.Player;
import com.badlogic.gdx.utils.Disposable;
import com.github.thedragonconquerors.movement.NavGrid;

import java.util.List;

public class PlayerRenderer implements Disposable {
    private final ShapeRenderer shapeRenderer;
    public static final float TILE_SIZE = 1f;

    private static final Color COLOR_PLAYER = new Color(0.2f, 0.8f, 0.3f, 1f);
    private static final Color COLOR_PLAYER_RING = new Color(1f, 1f, 1f, 0.8f);
    private static final Color COLOR_PATH         = new Color(1f,   0.85f,0.1f, 0.8f);
    private static final Color COLOR_STAMINA_BG = new Color(0.2f, 0.2f, 0.2f, 0.8f);
    private static final Color COLOR_STAMINA_FILL = new Color(0.1f, 0.9f, 0.3f, 1f);
    private static final Color COLOR_REACHABLE    = new Color(0.2f, 0.5f, 0.9f, 0.35f);

    // Cached reachable positions — rebuilt when stamina changes
    private List<Vector2> cachedReachable = null;
    private float lastRemainingDistance   = -1f;

    public PlayerRenderer(){
        this.shapeRenderer = new ShapeRenderer();
    }

    public void render(Player player, com.badlogic.gdx.math.Matrix4 projMatrix, NavGrid navGrid){
        shapeRenderer.setProjectionMatrix(projMatrix);

        float cx = player.getPosition().x;
        float cy = player.getPosition().y;
        float radius = TILE_SIZE*0.35f;
        float maxDist = player.getMovementController().getMaxMovementDistance();

        //movement range circle
        float remaining = player.getMovementController().getRemainingMovementDistance();

        //rebuild reachable cache only when stamina changes
        if(navGrid != null && remaining != lastRemainingDistance){
            cachedReachable = navGrid.getReachablePositions(player.getPosition(), remaining);
            lastRemainingDistance = remaining;
        }

        //filled pass
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        //reachable area
        if(cachedReachable != null){
            shapeRenderer.setColor(COLOR_REACHABLE);
            for(Vector2 pos : cachedReachable)  shapeRenderer.circle(pos.x, pos.y, NavGrid.NODE_SIZE*0.35f, 6);
        }

        //player body
        shapeRenderer.setColor(COLOR_PLAYER);
        shapeRenderer.circle(cx, cy, radius, 16);

        //stamina bar
        float distRatio = remaining / maxDist;
        float barW = TILE_SIZE * 0.8f;
        float barH = TILE_SIZE * 0.1f;
        float barX = cx - barW / 2f;
        float barY = cy - radius - barH - TILE_SIZE * 0.05f;
        shapeRenderer.setColor(COLOR_STAMINA_BG);
        shapeRenderer.rect(barX, barY, barW, barH);
        shapeRenderer.setColor(COLOR_STAMINA_FILL);
        shapeRenderer.rect(barX, barY, barW * distRatio, barH);

        shapeRenderer.end();

        //planned path
        List<Vector2> path = player.getMovementController().getPath();
        if (path != null && path.size() > 1) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(COLOR_PATH);
            Vector2 prev = player.getPosition();
            for (Vector2 waypoint : path) {
                shapeRenderer.line(prev.x, prev.y, waypoint.x, waypoint.y);
                prev = waypoint;
            }
            shapeRenderer.end();
        }

        //player ring
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(COLOR_PLAYER_RING);
        shapeRenderer.circle(cx, cy, radius, 16);
        shapeRenderer.end();
    }

    // Overload for enemy players
    public void render(Player player, com.badlogic.gdx.math.Matrix4 projMatrix) {
        render(player, projMatrix, null);
    }

    @Override
    public void dispose(){
        shapeRenderer.dispose();
    }
}
