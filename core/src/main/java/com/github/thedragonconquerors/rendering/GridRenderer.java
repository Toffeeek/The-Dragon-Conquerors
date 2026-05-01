package com.github.thedragonconquerors.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;
import com.github.thedragonconquerors.core.GridManager;
import com.github.thedragonconquerors.core.Tile;

public class GridRenderer implements Disposable {
    private final GridManager gridManager;
    private final ShapeRenderer shapeRenderer;

    // Tile colors
    private static final Color COLOR_BASE = new Color(0.15f, 0.15f, 0.15f, 1f);
    private static final Color COLOR_REACHABLE = new Color(0.2f,  0.5f,  0.9f,  0.45f);
    private static final Color COLOR_PATH = new Color(0.1f,  0.9f,  0.9f,  0.6f);
    private static final Color COLOR_SELECTED = new Color(1f,    0.85f, 0.1f,  0.7f);
    private static final Color COLOR_GRID_LINE = new Color(0.3f,  0.3f,  0.3f,  0.5f);

    private final float tileSize = GridManager.TILE_WORLD_SIZE;

    public GridRenderer(GridManager gridManager){
        this.gridManager = gridManager;
        this.shapeRenderer = new ShapeRenderer();
    }

    public void render(com.badlogic.gdx.math.Matrix4 projMatrix){
        shapeRenderer.setProjectionMatrix(projMatrix);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for(int x=0; x<gridManager.getCols(); x++){
            for(int y=0; y<gridManager.getRows(); y++){
                Tile tile = gridManager.getTile(x, y);
                Color tileColor = getTileColor(tile);
                if(tileColor != null){
                    shapeRenderer.setColor(tileColor);
                    shapeRenderer.rect(tile.getWorldX(), tile.getWorldY(), tileSize, tileSize);
                }
            }
        }
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(COLOR_GRID_LINE);
        for(int x=0; x< gridManager.getCols(); x++){
            for(int y=0; y< gridManager.getRows(); y++){
                Tile tile = gridManager.getTile(x, y);
                shapeRenderer.rect(tile.getWorldX(), tile.getWorldY(), tileSize, tileSize);
            }
        }
        shapeRenderer.end();
    }

    private Color getTileColor(Tile tile){
        return switch (tile.getHighlightState()){
            case REACHABLE -> COLOR_REACHABLE;
            case PATH -> COLOR_PATH;
            case SELECTED -> COLOR_SELECTED;
            default -> null;
        };
    }

    @Override
    public void dispose(){
        shapeRenderer.dispose();
    }
}
