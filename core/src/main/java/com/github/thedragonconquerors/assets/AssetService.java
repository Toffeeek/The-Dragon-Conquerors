package com.github.thedragonconquerors.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.utils.Disposable;

public class AssetService implements Disposable {
    private final AssetManager assetManager;

    public AssetService(FileHandleResolver fileHandleResolver) {
        this.assetManager = new AssetManager(fileHandleResolver);
        this.assetManager.setLoader(TiledMap.class, new TmxMapLoader());
    }

    public <T> T load(Asset<T> asset){
        this.assetManager.load(asset.getDescriptor());
        this.assetManager.finishLoading();
        return this.assetManager.get(asset.getDescriptor());
    }

    public <T> void queue(Asset<T> asset){
        this.assetManager.load(asset.getDescriptor());
    }

    public <T> T get(Asset<T> asset){
        return this.assetManager.get(asset.getDescriptor());
    }

    public boolean update(){
        return this.assetManager.update();
    }

    public void debugDiagnostic(){
        Gdx.app.debug("Asset Service", this.assetManager.getDiagnostics());
    }

    @Override
    public void dispose(){
        this.assetManager.dispose();
    }
}
