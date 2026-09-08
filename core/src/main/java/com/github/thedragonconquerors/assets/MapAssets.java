// File Location: core/src/main/java/com/github/thedragonconquerors/assets/MapAssets.java
package com.github.thedragonconquerors.assets;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.shared.shared.model.world.Environment;

public enum MapAssets implements Asset<TiledMap> {
    BOG("bog.tmx"),
    LAVA("lava.tmx"),
    CANYON("canyon.tmx"),
    MAIN("canyon.tmx");

    private final AssetDescriptor<TiledMap> descriptor;

    MapAssets(String mapName){
        this.descriptor = new AssetDescriptor<>("maps-new/" + mapName, TiledMap.class);
    }

    @Override
    public AssetDescriptor<TiledMap> getDescriptor(){
        return this.descriptor;
    }

    public static MapAssets forEnvironment(Environment environment) {
        if (environment == null) return CANYON;
        switch (environment) {
            case BOG: return BOG;
            case LAVA: return LAVA;
            case CANYON:
            default: return CANYON;
        }
    }
}
