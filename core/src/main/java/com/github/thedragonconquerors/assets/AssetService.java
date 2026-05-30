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
        this.assetManager.setLoader(TiledMap.class, new TmxMapLoader(fileHandleResolver));
    }

    // ── synchronous load (blocks until finished) ──────────────────

    public <T> T load(Asset<T> asset) {
        this.assetManager.load(asset.getDescriptor());
        this.assetManager.finishLoading();
        return this.assetManager.get(asset.getDescriptor());
    }

    // ── async queue & poll ─────────────────────────────────────────

    public <T> void queue(Asset<T> asset) {
        this.assetManager.load(asset.getDescriptor());
    }

    /**
     * Queues every entry in an Asset enum array for async loading.
     * Example:
     * <pre>
     *   assetService.queueAll(SpriteAssets.values());
     *   // then poll assetService.update() each frame until true
     * </pre>
     */
    public <T> void queueAll(Asset<T>[] assets) {
        for (Asset<T> asset : assets) {
            queue(asset);
        }
    }

    /** Advances async loading; returns true when all queued assets are ready. */
    public boolean update() {
        return this.assetManager.update();
    }

    // ── retrieval ─────────────────────────────────────────────────

    public <T> T get(Asset<T> asset) {
        return this.assetManager.get(asset.getDescriptor());
    }

    /**
     * Returns the asset if it has been loaded, or null if it hasn't been
     * queued / the file is missing.  Use this for optional assets such as
     * character sprites that may not yet have artwork.
     */
    public <T> T tryGet(Asset<T> asset) {
        if (asset == null) return null;
        try {
            if (this.assetManager.isLoaded(asset.getDescriptor().fileName,
                                           asset.getDescriptor().type)) {
                return this.assetManager.get(asset.getDescriptor());
            }
        } catch (Exception ignored) { /* file not found or not loaded yet */ }
        return null;
    }

    // ── diagnostics & disposal ────────────────────────────────────

    public void debugDiagnostic() {
        Gdx.app.debug("Asset Service", this.assetManager.getDiagnostics());
    }

    @Override
    public void dispose() {
        this.assetManager.dispose();
    }
}
