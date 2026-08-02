package com.github.thedragonconquerors.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.github.thedragonconquerors.assets.AssetService;
import com.github.thedragonconquerors.assets.SpriteAssets;
import com.github.thedragonconquerors.entities.Player;
import com.github.thedragonconquerors.movement.NavGrid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerRenderer implements Disposable {

    public static final float TILE_SIZE   = 1f;
    public static final float SPRITE_SIZE = 1.0f;

    private static final Color COLOR_PLAYER       = new Color(0.2f, 0.8f, 0.3f,  1f);
    private static final Color COLOR_PLAYER_RING  = new Color(1f,   1f,   1f,   0.8f);
    private static final Color COLOR_PATH         = new Color(1f,   0.85f,0.1f, 0.8f);
    private static final Color COLOR_STAMINA_BG   = new Color(0.2f, 0.2f, 0.2f, 0.8f);
    private static final Color COLOR_STAMINA_FILL = new Color(0.1f, 0.9f, 0.3f,  1f);
    private static final Color COLOR_REACHABLE    = new Color(0.2f, 0.5f, 0.9f, 0.35f);

    private final ShapeRenderer shapeRenderer;
    private final SpriteBatch   spriteBatch;
    private AssetService        assetService;

    // per-player animation state
    private final Map<Integer, Float>   stateTimes     = new HashMap<>();
    private final Map<Integer, Vector2> lastPositions  = new HashMap<>();

    private List<Vector2> cachedReachable       = null;
    private float         lastRemainingDistance = -1f;

    public PlayerRenderer() {
        this.shapeRenderer = new ShapeRenderer();
        this.spriteBatch   = new SpriteBatch();
    }

    public PlayerRenderer(AssetService assetService) {
        this();
        this.assetService = assetService;
    }

    public void setAssetService(AssetService assetService) {
        this.assetService = assetService;
    }

    public void render(Player player, com.badlogic.gdx.math.Matrix4 projMatrix,
                       NavGrid navGrid, float delta) {
        float cx        = player.getPosition().x;
        float cy        = player.getPosition().y;
        float radius    = TILE_SIZE * 0.35f;
        float remaining = player.getMovementController().getRemainingMovementDistance();
        float maxDist   = player.getMovementController().getMaxMovementDistance();

        // detect movement by comparing position to last frame
        Vector2 currentPos = player.getPosition();
        Vector2 lastPos    = lastPositions.getOrDefault(player.getID(),
                                new Vector2(currentPos));
        boolean moving     = currentPos.dst2(lastPos) > 0.00001f;
        lastPositions.put(player.getID(), new Vector2(currentPos));

        float stateTime = stateTimes.getOrDefault(player.getID(), 0f);
        if (moving) stateTime += delta;
        stateTimes.put(player.getID(), stateTime);

        if (navGrid != null && remaining != lastRemainingDistance) {
            cachedReachable       = navGrid.getReachablePositions(player.getPosition(), remaining);
            lastRemainingDistance = remaining;
        }

        shapeRenderer.setProjectionMatrix(projMatrix);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        if (cachedReachable != null) {
            shapeRenderer.setColor(COLOR_REACHABLE);
            for (Vector2 pos : cachedReachable)
                shapeRenderer.circle(pos.x, pos.y, NavGrid.NODE_SIZE * 0.35f, 6);
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

        // fallback circle if no sprite
        Animation<TextureRegion> anim = lookupAnimation(player);
        Texture staticSprite = lookupStaticSprite(player);
        if (anim == null && staticSprite == null) {
            shapeRenderer.setColor(COLOR_PLAYER);
            shapeRenderer.circle(cx, cy, radius, 16);
        }

        shapeRenderer.end();

        // draw animated sprite
        spriteBatch.setProjectionMatrix(projMatrix);
        spriteBatch.begin();
        float hw = SPRITE_SIZE / 2f;
        float hh = SPRITE_SIZE / 2f;
        if (anim != null) {
            TextureRegion frame = anim.getKeyFrame(stateTime, true);
            spriteBatch.draw(frame, cx - hw, cy - hh, SPRITE_SIZE, SPRITE_SIZE);
        } else if (staticSprite != null) {
            spriteBatch.draw(staticSprite, cx - hw, cy - hh, SPRITE_SIZE, SPRITE_SIZE);
        }
        spriteBatch.end();

        // path line
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

        // ring outline
        shapeRenderer.setProjectionMatrix(projMatrix);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(COLOR_PLAYER_RING);
        shapeRenderer.circle(cx, cy, radius, 16);
        shapeRenderer.end();
    }

    /** Overload without delta — animation stays on frame 0 (for enemy/remote players). */
    public void render(Player player, com.badlogic.gdx.math.Matrix4 projMatrix, NavGrid navGrid) {
        render(player, projMatrix, navGrid, 0f);
    }

    public void render(Player player, com.badlogic.gdx.math.Matrix4 projMatrix) {
        render(player, projMatrix, null, 0f);
    }

    // ── sprite lookup ─────────────────────────────────────────────

    private Animation<TextureRegion> lookupAnimation(Player player) {
        if (assetService == null) return null;
        return assetService.getWalkAnimation(player.getCharacterClass());
    }

    private Texture lookupStaticSprite(Player player) {
        if (assetService == null) return null;
        SpriteAssets entry = SpriteAssets.forClass(player.getCharacterClass());
        return assetService.tryGet(entry);
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        spriteBatch.dispose();
    }
}
