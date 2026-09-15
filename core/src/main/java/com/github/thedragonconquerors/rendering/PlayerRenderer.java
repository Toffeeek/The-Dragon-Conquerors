// File Location: core/src/main/java/com/github/thedragonconquerors/rendering/PlayerRenderer.java
package com.github.thedragonconquerors.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.github.thedragonconquerors.animation.AnimationState;
import com.github.thedragonconquerors.animation.FacingDirection;
import com.github.thedragonconquerors.assets.AssetService;
import com.github.thedragonconquerors.assets.SpriteAssets;
import com.github.thedragonconquerors.entities.Player;
import com.github.thedragonconquerors.movement.NavGrid;
import com.shared.shared.model.CharacterClass;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Renders class-specific supplied sprite strips plus movement and target overlays. */
public class PlayerRenderer implements Disposable {
    public static final float TILE_SIZE = 1f;
    public static final float TARGET_CLICK_RADIUS = 0.72f;

    /*
     * The supplied sprite strips reserve a 100x100 canvas around much smaller
     * pixel art. These per-class canvas sizes keep the visible characters close
     * to the old in-game footprint without resampling the source textures.
     */
    private static final float PALADIN_CANVAS_SIZE = 5.0f;
    private static final float MAGE_CANVAS_SIZE = 4.7f;
    private static final float WRAITH_CANVAS_SIZE = 3.2f;
    private static final float CLERIC_CANVAS_SIZE = 4.5f;
    private static final float ARCHER_CANVAS_SIZE = 5.0f;
    private static final float SOLDIER_CANVAS_SIZE = 4.4f;
    private static final float FRAME_BASELINE = 0.43f;

    private static final Color COLOR_PLAYER_RING = new Color(1f, 1f, 1f, 0.85f);
    private static final Color COLOR_STAMINA_BG = new Color(0.12f, 0.12f, 0.15f, 0.9f);
    private static final Color COLOR_STAMINA_FILL = new Color(0.1f, 0.9f, 0.3f, 1f);
    private static final Color COLOR_HP_BG = new Color(0.12f, 0.12f, 0.15f, 0.9f);
    private static final Color COLOR_HP_FILL = new Color(0.85f, 0.15f, 0.15f, 1f);
    private static final Color COLOR_REACHABLE = new Color(0.08f, 0.42f, 1f, 0.38f);
    private static final Color COLOR_ACTIVE_TURN = new Color(1f, 0.82f, 0.25f, 1f);
    private static final Color COLOR_TARGET_IN_RANGE = new Color(0.25f, 1f, 0.35f, 0.95f);
    private static final Color COLOR_TARGET_OUT_OF_RANGE = new Color(1f, 0.25f, 0.2f, 0.95f);
    private static final Color COLOR_TEAM_AZURE = new Color(0.2f, 0.55f, 1f, 0.95f);
    private static final Color COLOR_TEAM_CRIMSON = new Color(0.95f, 0.2f, 0.2f, 0.95f);

    private static final class SpriteStrip {
        private final TextureRegion[] rightFacing;
        private final TextureRegion[] leftFacing;

        private SpriteStrip(TextureRegion[] rightFacing, TextureRegion[] leftFacing) {
            this.rightFacing = rightFacing;
            this.leftFacing = leftFacing;
        }
    }

    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private final Batch spriteBatch;
    private final AssetService assetService;
    private final Map<SpriteAssets, SpriteStrip> stripCache = new EnumMap<>(SpriteAssets.class);

    private List<Vector2> cachedReachable;
    private float lastRemainingDistance = -1f;
    private final Vector2 lastReachablePosition = new Vector2(Float.NaN, Float.NaN);
    private int lastGridRevision = -1;
    private float pulseTime = 0f;

    public PlayerRenderer(AssetService assetService, Batch spriteBatch) {
        this.assetService = assetService;
        this.spriteBatch = spriteBatch;
    }

    public void renderLocal(Player player, Matrix4 projection, NavGrid navGrid, float delta,
                            boolean showMovement) {
        player.getAnimationController().update(
            delta, player.getPosition(), player.getMovementController());
        pulseTime += delta;

        float remaining = player.getMovementController().getRemainingMovementDistance();
        if (showMovement && navGrid != null && !player.getMovementController().isMoving()
            && (Math.abs(remaining - lastRemainingDistance) > 0.0001f
                || !player.getPosition().epsilonEquals(lastReachablePosition, 0.001f)
                || lastGridRevision != navGrid.getRevision())) {
            cachedReachable = navGrid.getReachablePositions(player.getPosition(), remaining);
            lastRemainingDistance = remaining;
            lastReachablePosition.set(player.getPosition());
            lastGridRevision = navGrid.getRevision();
        } else if (!player.isActiveTurn()) {
            cachedReachable = null;
            lastRemainingDistance = -1f;
        }

        if (showMovement && !player.getMovementController().isMoving()) drawReachable(projection);
        drawCharacter(player, projection);
        drawBars(player, projection, true);
        drawRing(player, projection, teamColor(player), 0.43f);
        drawActiveTurn(player, projection);
    }

    public void renderEnemy(Player player, Matrix4 projection, float delta,
                            boolean targetSelectionActive, boolean inRange) {
        player.getAnimationController().update(
            delta, player.getPosition(), player.getMovementController());

        if (targetSelectionActive) {
            float pulse = 0.47f + 0.04f * (float) Math.sin(pulseTime * 6f);
            drawRing(player, projection,
                inRange ? COLOR_TARGET_IN_RANGE : COLOR_TARGET_OUT_OF_RANGE, pulse);
        } else {
            drawRing(player, projection, teamColor(player), 0.40f);
        }

        drawCharacter(player, projection);
        drawBars(player, projection, false);
        drawActiveTurn(player, projection);
    }

    private void drawReachable(Matrix4 projection) {
        if (cachedReachable == null) return;
        com.badlogic.gdx.Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        com.badlogic.gdx.Gdx.gl.glBlendFunc(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
            com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(projection);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(COLOR_REACHABLE);
        for (Vector2 pos : cachedReachable) {
            shapeRenderer.rect(pos.x - NavGrid.NODE_SIZE / 2f, pos.y - NavGrid.NODE_SIZE / 2f,
                NavGrid.NODE_SIZE, NavGrid.NODE_SIZE);
        }
        shapeRenderer.end();
        com.badlogic.gdx.Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
    }

    private void drawCharacter(Player player, Matrix4 projection) {
        TextureRegion frame = currentFrame(player);
        float canvasSize = canvasSize(player.getCharacterClass());
        float x = player.getPosition().x - canvasSize * 0.5f;
        float y = player.getPosition().y - canvasSize * FRAME_BASELINE;

        if (frame != null) {
            spriteBatch.setProjectionMatrix(projection);
            spriteBatch.setColor(Color.WHITE);
            spriteBatch.begin();
            spriteBatch.draw(frame, x, y, canvasSize, canvasSize);
            spriteBatch.end();
            return;
        }

        shapeRenderer.setProjectionMatrix(projection);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(fallbackColor(player.getCharacterClass()));
        shapeRenderer.circle(player.getPosition().x, player.getPosition().y, 0.35f, 16);
        shapeRenderer.end();
    }

    private TextureRegion currentFrame(Player player) {
        AnimationState state = player.getAnimationController().getState();
        SpriteAssets asset = SpriteAssets.forAnimation(player.getCharacterClass(), state);
        SpriteStrip strip = stripFor(asset);
        if (strip == null) return null;

        int frameIndex = player.getAnimationController().getCurrentFrame();
        TextureRegion[] frames = player.getAnimationController().getFacing() == FacingDirection.LEFT
            ? strip.leftFacing : strip.rightFacing;
        if (frameIndex < 0 || frameIndex >= frames.length) return null;
        return frames[frameIndex];
    }

    private SpriteStrip stripFor(SpriteAssets asset) {
        SpriteStrip cached = stripCache.get(asset);
        if (cached != null) return cached;

        Texture texture = assetService.tryGet(asset);
        if (texture == null) return null;
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        if (texture.getHeight() != SpriteAssets.FRAME_HEIGHT
            || texture.getWidth() % SpriteAssets.FRAME_WIDTH != 0) {
            System.out.println("Invalid sprite strip dimensions for " + asset.name()
                + ": " + texture.getWidth() + "x" + texture.getHeight());
            return null;
        }

        int frameCount = texture.getWidth() / SpriteAssets.FRAME_WIDTH;
        if (frameCount != asset.getFrameCount()) {
            System.out.println("Invalid sprite frame count for " + asset.name()
                + ": expected " + asset.getFrameCount() + ", got " + frameCount);
            return null;
        }

        TextureRegion[] right = new TextureRegion[frameCount];
        TextureRegion[] left = new TextureRegion[frameCount];
        for (int i = 0; i < frameCount; i++) {
            right[i] = new TextureRegion(texture,
                i * SpriteAssets.FRAME_WIDTH, 0,
                SpriteAssets.FRAME_WIDTH, SpriteAssets.FRAME_HEIGHT);
            left[i] = new TextureRegion(right[i]);
            left[i].flip(true, false);
        }

        SpriteStrip strip = new SpriteStrip(right, left);
        stripCache.put(asset, strip);
        return strip;
    }

    private float canvasSize(CharacterClass characterClass) {
        if (characterClass == null) return PALADIN_CANVAS_SIZE;
        switch (characterClass) {
            case PALADIN: return PALADIN_CANVAS_SIZE;
            case MAGE: return MAGE_CANVAS_SIZE;
            case WRAITH: return WRAITH_CANVAS_SIZE;
            case CLERIC: return CLERIC_CANVAS_SIZE;
            case ARCHER: return ARCHER_CANVAS_SIZE;
            case SOLDIER: return SOLDIER_CANVAS_SIZE;
            default: return PALADIN_CANVAS_SIZE;
        }
    }

    private void drawActiveTurn(Player player, Matrix4 projection) {
        if (player.isActiveTurn() && player.isAlive()) {
            drawRing(player, projection, COLOR_ACTIVE_TURN,
                0.51f + 0.025f * (float) Math.sin(pulseTime * 4f));
        }
    }

    private void drawBars(Player player, Matrix4 projection, boolean showStamina) {
        float x = player.getPosition().x;
        float y = player.getPosition().y;
        float barWidth = 0.86f;
        float barHeight = 0.075f;
        float hpRatio = (float) player.getStats().getHp() / player.getStats().getMaxHp();

        shapeRenderer.setProjectionMatrix(projection);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(COLOR_HP_BG);
        shapeRenderer.rect(x - barWidth / 2f, y + 0.67f, barWidth, barHeight);
        shapeRenderer.setColor(COLOR_HP_FILL);
        shapeRenderer.rect(x - barWidth / 2f, y + 0.67f,
            barWidth * Math.max(0f, hpRatio), barHeight);

        if (showStamina) {
            float remaining = player.getMovementController().getRemainingMovementDistance();
            float maximum = player.getMovementController().getMaxMovementDistance();
            float staminaRatio = maximum <= 0f ? 0f : remaining / maximum;
            shapeRenderer.setColor(COLOR_STAMINA_BG);
            shapeRenderer.rect(x - barWidth / 2f, y - 0.55f, barWidth, barHeight);
            shapeRenderer.setColor(COLOR_STAMINA_FILL);
            shapeRenderer.rect(x - barWidth / 2f, y - 0.55f,
                barWidth * Math.max(0f, staminaRatio), barHeight);
        }
        shapeRenderer.end();
    }

    private void drawRing(Player player, Matrix4 projection, Color color, float radius) {
        shapeRenderer.setProjectionMatrix(projection);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(color);
        shapeRenderer.circle(player.getPosition().x, player.getPosition().y, radius, 24);
        shapeRenderer.end();
    }

    private Color teamColor(Player player) {
        if (player == null) return COLOR_PLAYER_RING;
        return player.getTeamIndex() == 1 ? COLOR_TEAM_AZURE : COLOR_TEAM_CRIMSON;
    }

    /** Flat colour shown only when a supplied sprite strip could not be loaded. */
    private Color fallbackColor(CharacterClass characterClass) {
        if (characterClass == null) return Color.WHITE;
        switch (characterClass) {
            case PALADIN: return new Color(0.85f, 0.78f, 0.35f, 1f);
            case MAGE:    return new Color(0.34f, 0.22f, 0.72f, 1f);
            case WRAITH:  return new Color(0.28f, 0.20f, 0.35f, 1f);
            case CLERIC:  return new Color(0.80f, 0.86f, 0.92f, 1f);
            case SOLDIER: return new Color(0.50f, 0.58f, 0.66f, 1f);
            case ARCHER:  return new Color(0.18f, 0.55f, 0.30f, 1f);
            default:      return Color.WHITE;
        }
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
    }
}
