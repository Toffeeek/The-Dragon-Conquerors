package com.github.thedragonconquerors.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.utils.Disposable;
import com.github.thedragonconquerors.entities.CharacterClass;

import java.util.EnumMap;

public class AssetService implements Disposable {

    private static final int   WALK_FRAMES    = 4;
    private static final float FRAME_DURATION = 0.15f;
    private static final int   FRAME_W        = 16;
    private static final int   FRAME_H        = 16;

    private final AssetManager assetManager;

    // cached walk animations, built once the walk sheet is loaded
    private final EnumMap<CharacterClass, Animation<TextureRegion>> walkAnimations =
        new EnumMap<>(CharacterClass.class);

    public AssetService(FileHandleResolver fileHandleResolver) {
        this.assetManager = new AssetManager(fileHandleResolver);
        this.assetManager.setLoader(TiledMap.class, new TmxMapLoader(fileHandleResolver));
    }

    // ── load / queue ──────────────────────────────────────────────

    public <T> T load(Asset<T> asset) {
        this.assetManager.load(asset.getDescriptor());
        this.assetManager.finishLoading();
        return this.assetManager.get(asset.getDescriptor());
    }

    public <T> void queue(Asset<T> asset) {
        this.assetManager.load(asset.getDescriptor());
    }

    public <T> void queueAll(Asset<T>[] assets) {
        for (Asset<T> a : assets) queue(a);
    }

    public boolean update() { return this.assetManager.update(); }

    public <T> T get(Asset<T> asset) {
        return this.assetManager.get(asset.getDescriptor());
    }

    public <T> T tryGet(Asset<T> asset) {
        if (asset == null) return null;
        try {
            if (assetManager.isLoaded(asset.getDescriptor().fileName, asset.getDescriptor().type))
                return assetManager.get(asset.getDescriptor());
        } catch (Exception ignored) {}
        return null;
    }

    public <T> T tryGet(AssetDescriptor<T> desc) {
        if (desc == null) return null;
        try {
            if (assetManager.isLoaded(desc.fileName, desc.type))
                return assetManager.get(desc);
        } catch (Exception ignored) {}
        return null;
    }

    // ── walk animation ────────────────────────────────────────────

    /**
     * Loads the walk spritesheet for all classes and builds Animation objects.
     * Call this after the map has loaded (same place you loaded individual sprites).
     */
    public void loadWalkAnimations() {
        for (SpriteAssets sa : SpriteAssets.values()) {
            try {
                assetManager.load(sa.getWalkDescriptor());
                assetManager.finishLoading();
                Texture sheet = assetManager.get(sa.getWalkDescriptor());
                TextureRegion[] frames = new TextureRegion[WALK_FRAMES];
                for (int i = 0; i < WALK_FRAMES; i++) {
                    frames[i] = new TextureRegion(sheet, i * FRAME_W, 0, FRAME_W, FRAME_H);
                }
                walkAnimations.put(sa.getCharacterClass(),
                    new Animation<>(FRAME_DURATION, frames));
            } catch (Exception e) {
                Gdx.app.log("AssetService", "Walk sheet not found for "
                    + sa.name() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Returns the walk animation for the given class, or null if not loaded.
     */
    public Animation<TextureRegion> getWalkAnimation(CharacterClass cls) {
        return walkAnimations.get(cls);
    }

    public void debugDiagnostic() {
        Gdx.app.debug("Asset Service", this.assetManager.getDiagnostics());
    }

    @Override
    public void dispose() {
        this.assetManager.dispose();
    }
}
