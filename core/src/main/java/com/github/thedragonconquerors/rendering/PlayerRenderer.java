package com.github.thedragonconquerors.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.github.thedragonconquerors.core.GridManager;
import com.github.thedragonconquerors.entities.Player;
import com.badlogic.gdx.utils.Disposable;

public class PlayerRenderer implements Disposable {
    private final ShapeRenderer shapeRenderer;
    private final float tileSize = GridManager.TILE_WORLD_SIZE;

    private static final Color COLOR_PLAYER = new Color(0.2f, 0.8f, 0.3f, 1f);
    private static final Color COLOR_PLAYER_RING = new Color(1f, 1f, 1f, 0.8f);
    private static final Color COLOR_STAMINA_BG = new Color(0.2f, 0.2f, 0.2f, 0.8f);
    private static final Color COLOR_STAMINA_FILL = new Color(0.1f, 0.9f, 0.3f, 1f);

    public PlayerRenderer(){
        this.shapeRenderer = new ShapeRenderer();
    }

    public void render(Player player, com.badlogic.gdx.math.Matrix4 projMatrix){
        shapeRenderer.setProjectionMatrix(projMatrix);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        float cx = player.getWorldPos().x;
        float cy = player.getWorldPos().y;
        float radius = tileSize*0.35f;

        //player body
        shapeRenderer.setColor(COLOR_PLAYER);
        shapeRenderer.circle(cx, cy, radius, 16);

        //stamina below player
        float barW = tileSize*0.8f;
        float barH = tileSize*0.1f;
        float barX = cx-barW/2f;
        float barY = cy-radius-barH-tileSize*0.05f;
        float staminaRatio = (float)player.getRemainingStamina()/player.getMaxStamina();

        shapeRenderer.setColor(COLOR_STAMINA_BG);
        shapeRenderer.rect(barX, barY, barW, barH);

        shapeRenderer.setColor(COLOR_STAMINA_FILL);
        shapeRenderer.rect(barX, barY, barW*staminaRatio, barH);

        shapeRenderer.end();

        //outer ring
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(COLOR_PLAYER_RING);
        shapeRenderer.circle(cx, cy, radius, 16);
        shapeRenderer.end();
    }

    @Override
    public void dispose(){
        shapeRenderer.dispose();
    }
}
