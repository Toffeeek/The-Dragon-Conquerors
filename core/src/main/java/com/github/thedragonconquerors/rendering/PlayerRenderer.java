package com.github.thedragonconquerors.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.github.thedragonconquerors.assets.AssetService;
import com.github.thedragonconquerors.assets.SpriteAssets;
import com.github.thedragonconquerors.entities.Player;
import com.badlogic.gdx.utils.Disposable;
import com.github.thedragonconquerors.movement.NavGrid;

import java.util.List;

/**
 * Draws players on the game world.
 *
 * Sprite support
 * ──────────────
 * Call setAssetService(assetService) after construction (or pass it in the
 * constructor) so the renderer can look up character sprites.
 *
 * If a sprite is found for the player's CharacterClass, it is drawn instead
 * of the placeholder circle.  If no sprite is loaded (artwork still missing,
 * asset not yet queued, etc.) the renderer falls back silently to the
 * shape-based circle so the game always displays something.
 *
 * Sprite sizing: sprites are drawn at SPRITE_SIZE × SPRITE_SIZE world units,
 * centred on the player's position.  Adjust SPRITE_SIZE to taste.
 */
public class PlayerRenderer implements Disposable {

    // ── constants ─────────────────────────────────────────────────
    public static final float TILE_SIZE   = 1f;
    public static final float SPRITE_SIZE = 1.0f;   // world-unit size of the drawn sprite

    private static final Color COLOR_PLAYER      = new Color(0.2f, 0.8f, 0.3f,  1f);
    private static final Color COLOR_PLAYER_RING = new Color(1f,   1f,   1f,   0.8f);
    private static final Color COLOR_PATH        = new Color(1f,   0.85f,0.1f, 0.8f);
    private static final Color COLOR_STAMINA_BG  = new Color(0.2f, 0.2f, 0.2f, 0.8f);
    private static final Color COLOR_STAMINA_FILL= new Color(0.1f, 0.9f, 0.3f, 1f);
    private static final Color COLOR_REACHABLE   = new Color(0.2f, 0.5f, 0.9f, 0.35f);

    // ── rendering objects ─────────────────────────────────────────
    private final ShapeRenderer shapeRenderer;
    private final SpriteBatch   spriteBatch;      // dedicated batch for sprite drawing

    // ── optional asset service (set after construction) ───────────
    private AssetService assetService;

    // ── reachable-tile cache ──────────────────────────────────────
    private List<Vector2> cachedReachable    = null;
    private float         lastRemainingDistance = -1f;

    // ──────────────────────────────────────────────────────────────
    //  Construction
    // ──────────────────────────────────────────────────────────────

    public PlayerRenderer() {
        this.shapeRenderer = new ShapeRenderer();
        this.spriteBatch   = new SpriteBatch();
    }

    /** Convenience constructor that wires the AssetService immediately. */
    public PlayerRenderer(AssetService assetService) {
        this();
        this.assetService = assetService;
    }

    /** Wire in the AssetService after construction (called from GameOneScreen). */
    public void setAssetService(AssetService assetService) {
        this.assetService = assetService;
    }

    // ──────────────────────────────────────────────────────────────
    //  Public render API
    // ──────────────────────────────────────────────────────────────

    /**
     * Renders the local player with reachable-area overlay and path preview.
     *
     * @param player    the player to draw
     * @param projMatrix combined camera matrix
     * @param navGrid   used to compute reachable tiles (may be null)
     */
    public void render(Player player, com.badlogic.gdx.math.Matrix4 projMatrix, NavGrid navGrid) {
        float cx      = player.getPosition().x;
        float cy      = player.getPosition().y;
        float radius  = TILE_SIZE * 0.35f;
        float remaining = player.getMovementController().getRemainingMovementDistance();
        float maxDist   = player.getMovementController().getMaxMovementDistance();

        // ── rebuild reachable cache only when stamina changes ─────
        if (navGrid != null && remaining != lastRemainingDistance) {
            cachedReachable       = navGrid.getReachablePositions(player.getPosition(), remaining);
            lastRemainingDistance = remaining;
        }

        // ── reachable overlay + stamina bar (shapes) ──────────────
        shapeRenderer.setProjectionMatrix(projMatrix);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        if (cachedReachable != null) {
            shapeRenderer.setColor(COLOR_REACHABLE);
            for (Vector2 pos : cachedReachable)
                shapeRenderer.circle(pos.x, pos.y, NavGrid.NODE_SIZE * 0.35f, 6);
        }

        // Draw placeholder circle only when there is no sprite
        Texture sprite = lookupSprite(player);
        if (sprite == null) {
            shapeRenderer.setColor(COLOR_PLAYER);
            shapeRenderer.circle(cx, cy, radius, 16);
        }

        // stamina bar
        float distRatio = remaining / maxDist;
        float barW = TILE_SIZE * 0.8f;
        float barH = TILE_SIZE * 0.1f;
        float barX = cx - barW / 2f;
        float barY = cy - radius - barH - TILE_SIZE * 0.05f;
        shapeRenderer.setColor(COLOR_STAMINA_BG);
        shapeRenderer.rect(barX, barY, barW, barH);
        shapeRenderer.setColor(COLOR_STAMINA_FILL);
        shapeRenderer.rect(barX, barY, barW * distRatio, barH);

        shapeRenderer.end();

        // ── sprite (drawn on top of overlay, underneath path line) ─
        if (sprite != null) {
            spriteBatch.setProjectionMatrix(projMatrix);
            spriteBatch.begin();
            float hw = SPRITE_SIZE / 2f;
            float hh = SPRITE_SIZE / 2f;
            spriteBatch.draw(sprite, cx - hw, cy - hh, SPRITE_SIZE, SPRITE_SIZE);
            spriteBatch.end();
        }

        // ── path line ─────────────────────────────────────────────
        List<Vector2> path = player.getMovementController().getPath();
        if (path != null && path.size() > 1) {
            shapeRenderer.setProjectionMatrix(projMatrix);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(COLOR_PATH);
            Vector2 prev = player.getPosition();
            for (Vector2 waypoint : path) {
                shapeRenderer.line(prev.x, prev.y, waypoint.x, waypoint.y);
                prev = waypoint;
            }
            shapeRenderer.end();
        }

        // ── ring outline (always drawn) ───────────────────────────
        shapeRenderer.setProjectionMatrix(projMatrix);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(COLOR_PLAYER_RING);
        shapeRenderer.circle(cx, cy, radius, 16);
        shapeRenderer.end();
    }

    /** Overload for enemy players (no reachable-area overlay). */
    public void render(Player player, com.badlogic.gdx.math.Matrix4 projMatrix) {
        render(player, projMatrix, null);
    }

    // ──────────────────────────────────────────────────────────────
    //  Sprite lookup (graceful — returns null if unavailable)
    // ──────────────────────────────────────────────────────────────

    /**
     * Looks up the sprite texture for the player's CharacterClass.
     * Returns null if the AssetService is not wired, the class has no
     * registered sprite, or the texture has not been loaded yet.
     * The renderer treats null as "fall back to shape drawing".
     */
    private Texture lookupSprite(Player player) {
        if (assetService == null) return null;
        SpriteAssets entry = SpriteAssets.forClass(player.getCharacterClass());
        return assetService.tryGet(entry);   // null-safe
    }

    // ──────────────────────────────────────────────────────────────
    //  Disposal
    // ──────────────────────────────────────────────────────────────

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        spriteBatch.dispose();
    }
}
