package com.github.thedragonconquerors.assets;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.maps.tiled.TiledMap;

public enum MapAssets implements Asset<TiledMap> {
    MAIN("map1_alt.tmx");

    private final AssetDescriptor<TiledMap> descriptor;

    MapAssets(String mapName){
        this.descriptor = new AssetDescriptor<>("maps-new/" + mapName, TiledMap.class);
    }

    @Override
    public AssetDescriptor<TiledMap> getDescriptor(){
        return this.descriptor;
    }
}
