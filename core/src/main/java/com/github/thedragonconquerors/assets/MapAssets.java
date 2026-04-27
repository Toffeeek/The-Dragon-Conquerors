package com.github.thedragonconquerors.assets;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.maps.tiled.BaseTiledMapLoader;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;

public enum MapAssets implements Asset<TiledMap> {
    MAIN("canyon.tmx");

    private final AssetDescriptor<TiledMap> descriptor;

    MapAssets(String mapName){
        TmxMapLoader.Parameters parameters = new TmxMapLoader.Parameters();
        parameters.projectFilePath = "maps/tiled.tiled-project";
        this.descriptor = new AssetDescriptor<>("maps/" + mapName, TiledMap.class);
    }

    @Override
    public AssetDescriptor<TiledMap> getDescriptor(){
        return this.descriptor;
    }
}
