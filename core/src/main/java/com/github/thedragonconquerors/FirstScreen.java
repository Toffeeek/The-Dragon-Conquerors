package com.github.thedragonconquerors;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.github.thedragonconquerors.assets.AssetService;
import com.github.thedragonconquerors.assets.MapAssets;

public class FirstScreen extends ScreenAdapter {
    private final Main game;
    private final AssetService assetService;
    private final Viewport viewport;
    private final OrthographicCamera camera;

    public FirstScreen(Main game){
        this.game = game;
        this.assetService = game.getAssetService();
        this.viewport = game.getViewport();
        this.camera = game.getCamera();
    }

    @Override
    public void show(){
        this.assetService.load(MapAssets.MAIN);
    }

    @Override
    public void render(float delta){
        super.render(delta);
    }
}
