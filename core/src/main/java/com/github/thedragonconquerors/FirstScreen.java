package com.github.thedragonconquerors;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.github.thedragonconquerors.assets.AssetService;
import com.github.thedragonconquerors.assets.MapAssets;

public class FirstScreen extends ScreenAdapter {
    private final Main game;
    private final Batch batch;
    private final AssetService assetService;
    private final Viewport viewport;
    private final OrthographicCamera camera;
    private final OrthogonalTiledMapRenderer mapRenderer;

    public FirstScreen(Main game){
        this.game = game;
        this.assetService = game.getAssetService();
        this.viewport = game.getViewport();
        this.camera = game.getCamera();
        this.batch = game.getBatch();
        this.mapRenderer = new OrthogonalTiledMapRenderer(null, Main.UNIT_SCALE, this.batch);
    }

    @Override
    public void show(){
        this.assetService.load(MapAssets.MAIN);
        this.mapRenderer.setMap(this.assetService.get(MapAssets.MAIN));
    }

    @Override
    public void render(float delta){
        this.viewport.apply();
        this.batch.setColor(Color.WHITE);
        this.mapRenderer.setView(this.camera);
        this.mapRenderer.render();
    }

    @Override
    public void dispose() {
        this.mapRenderer.dispose();
    }
}
