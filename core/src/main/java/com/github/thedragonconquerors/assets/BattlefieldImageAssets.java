// File Location: core/src/main/java/com/github/thedragonconquerors/assets/BattlefieldImageAssets.java
package com.github.thedragonconquerors.assets;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.graphics.Texture;

/** Original full-map artwork, rendered to the same 30x17 world as its collision mask. */
public enum BattlefieldImageAssets implements Asset<Texture> {
    LAVA;
    private final AssetDescriptor<Texture> descriptor = new AssetDescriptor<>("maps-new/lava-map.png", Texture.class);
    @Override public AssetDescriptor<Texture> getDescriptor() { return descriptor; }
}
