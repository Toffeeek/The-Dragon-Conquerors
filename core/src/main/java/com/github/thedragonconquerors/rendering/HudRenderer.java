package com.github.thedragonconquerors.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.github.thedragonconquerors.entities.Player;

public class HudRenderer implements Disposable {
    private final SpriteBatch hudBatch;
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont font;
    private final Viewport viewport;

    private static final Color PIP_FULL = new Color(0.2f, 0.8f, 0.3f, 1f);
    private static final Color PIP_EMPTY = new Color(0.3f, 0.3f, 0.3f, 0.8f);
    private static final Color TEXT_COLOR = new Color(0.9f, 0.9f, 0.9f, 1f);

    public HudRenderer(Viewport viewport){
        this.viewport = viewport;
        this.hudBatch = new SpriteBatch();
        this.shapeRenderer = new ShapeRenderer();
        this.font = new BitmapFont();
        this.font.setColor(TEXT_COLOR);
    }

    public void render(Player player, float turnManager){
        // pixel coordinates
        Matrix4 screenMatrix = new Matrix4().setToOrtho2D(0, 0, viewport.getScreenWidth(), viewport.getScreenHeight());
        int sw = viewport.getScreenWidth();
        int sh = viewport.getScreenHeight();

        //stamina pips bottom left
        shapeRenderer.setProjectionMatrix(screenMatrix);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        float max = player.getMovementController().getMaxMovementDistance();
        float remaining = player.getMovementController().getRemainingMovementDistance();
        //float pipR = 8f;
//        float pipGap = 24f;
//        float startX = 24f+pipR;
//        float pipY = 36f;

        float barW = 200f;
        float barH = 16f;
        float barX = 16f;
        float barY = 36f;
        shapeRenderer.setColor(PIP_EMPTY);
        shapeRenderer.rect(barX, barY, barW, barH);
        shapeRenderer.setColor(PIP_FULL);
        shapeRenderer.rect(barX, barY, barW * (remaining / max), barH);

        shapeRenderer.end();

        //text labels
        hudBatch.setProjectionMatrix(screenMatrix);
        hudBatch.begin();

        //turn counter
        //font.draw(hudBatch, "Turn " + turnManager.getTurnNumber(), 16, sh -12);

        //stamina label
        font.draw(hudBatch, "Stamina: " + remaining + "/" + max, 16, 60);

        //end turn hint
        font.draw(hudBatch, "[E] End Turn: ", sw-120f, 24f);

        hudBatch.end();
    }

    @Override
    public void dispose() {
        hudBatch.dispose();
        shapeRenderer.dispose();
        font.dispose();
    }
}
