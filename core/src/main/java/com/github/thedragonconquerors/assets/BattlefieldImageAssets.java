package com.github.thedragonconquerors.assets;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.graphics.Texture;
import com.shared.shared.model.world.Environment;

public enum BattlefieldImageAssets implements Asset<Texture> {
    MAP1("map1.png"), MAP2("map2.png"), MAP3("map3.png");
    private final AssetDescriptor<Texture> descriptor;
    BattlefieldImageAssets(String name) { descriptor = new AssetDescriptor<>("maps-new/" + name, Texture.class); }
    @Override public AssetDescriptor<Texture> getDescriptor() { return descriptor; }
    public static BattlefieldImageAssets forEnvironment(Environment environment) {
        return environment == Environment.CANYON ? MAP1 : environment == Environment.BOG ? MAP3
            : environment == Environment.LAVA ? MAP2 : null;
    }
}
