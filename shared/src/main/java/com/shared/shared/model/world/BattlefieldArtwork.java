package com.shared.shared.model.world;

import com.badlogic.gdx.math.Vector2;

/** Shared image placement keeps rendering, collision and authored spawn coordinates aligned. */
public enum BattlefieldArtwork {
    MAP1("map1", 1672, 941, 0f, 30f),
    MAP2("map2", 1672, 941, 0f, 30f),
    MAP3("map3", 1536, 1024, 2.25f, 25.5f);

    public final String name;
    public final int pixelWidth, pixelHeight;
    public final float x, width;
    BattlefieldArtwork(String name, int pixelWidth, int pixelHeight, float x, float width) {
        this.name = name;
        this.pixelWidth = pixelWidth;
        this.pixelHeight = pixelHeight;
        this.x = x;
        this.width = width;
    }
    public Vector2 worldPoint(float imageX, float imageY) {
        return new Vector2(x + imageX * width / pixelWidth, 17f - imageY * 17f / pixelHeight);
    }
    public static BattlefieldArtwork forEnvironment(Environment environment) {
        if (environment == Environment.CANYON) return MAP1;
        if (environment == Environment.BOG) return MAP3;
        if (environment == Environment.LAVA) return MAP2;
        return null;
    }
}
