package com.github.thedragonconquerors.assets;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.graphics.Texture;

public enum SpriteAssets implements Asset<Texture> {
    PLAYER("Player.png"),
    PLAYER_ACTIONS("Player_Actions.png"),
    SKELETON("Skeleton.png"),
    GREEN_SLIME("Slime_Green.png");

    private final AssetDescriptor<Texture> descriptor;

    SpriteAssets(String fileName) {
        this.descriptor = new AssetDescriptor<>("characters/" + fileName, Texture.class);
    }

    @Override
    public AssetDescriptor<Texture> getDescriptor() {
        return descriptor;
    }
}
