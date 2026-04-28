package com.github.thedragonconquerors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.github.thedragonconquerors.core.GridManager;
import com.github.thedragonconquerors.entities.Player;
import com.github.thedragonconquerors.rendering.GridRenderer;
import com.github.thedragonconquerors.rendering.PlayerRenderer;

public class FirstScreen extends ScreenAdapter {
    private final Main game;
    private final Batch batch;
    //private final AssetService assetService;
    private final Viewport viewport;
    private final OrthographicCamera camera;
    //private final OrthogonalTiledMapRenderer mapRenderer;
    //private MovementSystem movementSystem;
    private Player player;
    private PlayerRenderer playerRenderer;
    private GridManager gridManager;
    private GridRenderer gridRenderer;

    public FirstScreen(Main game){
        this.game = game;
        //this.assetService = game.getAssetService();
        this.viewport = game.getViewport();
        this.camera = game.getCamera();
        this.batch = game.getBatch();
        //this.mapRenderer = new OrthogonalTiledMapRenderer(null, Main.UNIT_SCALE, this.batch);
    }

    @Override
    public void show(){
        //build core system
        gridManager = new GridManager();

        // Spawn player at tile(2, 2)
        player = new Player(2, 2, 5);
        gridManager.getTile(2, 2).setOccupied(true);

        //build renderers
        gridRenderer = new GridRenderer(gridManager);
        playerRenderer = new PlayerRenderer();

        //this.assetService.load(MapAssets.MAIN);
        //this.mapRenderer.setMap(this.assetService.get(MapAssets.MAIN));
    }

    @Override
    public void render(float delta){
        //update player animation
        player.update(delta);

        //clear screen
        ScreenUtils.clear(Color.BLACK);

        //render grid
        gridRenderer.render(camera.combined);

        //render player
        playerRenderer.render(player, camera.combined);


//        this.viewport.apply();
//        this.batch.setColor(Color.WHITE);
//        this.mapRenderer.setView(this.camera);
//        this.mapRenderer.render();
    }

    //ends current turn and resets player stamina
    private void endTurn(){
        player.resetStamin();
    }

    @Override
    public void resize(int width, int height){
        viewport.update(width, height, true);
    }

    @Override
    public void hide(){
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        gridRenderer.dispose();
        playerRenderer.dispose();
        //this.mapRenderer.dispose();
    }
}
