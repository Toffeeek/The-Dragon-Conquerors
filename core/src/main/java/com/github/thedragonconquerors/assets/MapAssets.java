package com.github.thedragonconquerors.assets;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.maps.tiled.TiledMap;

public enum MapAssets implements Asset<TiledMap> {
    MAIN("canyon.tmx");

    private final AssetDescriptor<TiledMap> descriptor;

    MapAssets(String mapName){
        this.descriptor = new AssetDescriptor<>("maps/" + mapName, TiledMap.class);
    }

    @Override
    public AssetDescriptor<TiledMap> getDescriptor(){
        return this.descriptor;
    }
}
