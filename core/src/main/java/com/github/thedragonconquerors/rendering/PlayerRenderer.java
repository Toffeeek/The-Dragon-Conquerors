package com.github.thedragonconquerors.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.github.thedragonconquerors.assets.AssetService;
import com.github.thedragonconquerors.assets.SpriteAssets;
import com.github.thedragonconquerors.animation.PlayerAnimationController;
import com.github.thedragonconquerors.entities.Player;
import com.github.thedragonconquerors.movement.NavGrid;
import com.shared.shared.model.CharacterClass;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/** Renders class-specific animated sprite sheets plus movement and target overlays. */
public class PlayerRenderer implements Disposable {
    public static final float TILE_SIZE = 1f;
    public static final float TARGET_CLICK_RADIUS = 0.72f;

    private static final int FRAME_WIDTH = 96;
    private static final int FRAME_HEIGHT = 96;
    private static final float SPRITE_WIDTH = 1.48f;
    private static final float SPRITE_HEIGHT = 1.48f;
    private static final float SPRITE_Y_OFFSET = -0.48f;

    private static final Color COLOR_PLAYER_RING = new Color(1f, 1f, 1f, 0.85f);
    private static final Color COLOR_PATH = new Color(1f, 0.85f, 0.1f, 0.8f);
    private static final Color COLOR_STAMINA_BG = new Color(0.12f, 0.12f, 0.15f, 0.9f);
    private static final Color COLOR_STAMINA_FILL = new Color(0.1f, 0.9f, 0.3f, 1f);
    private static final Color COLOR_HP_BG = new Color(0.12f, 0.12f, 0.15f, 0.9f);
    private static final Color COLOR_HP_FILL = new Color(0.85f, 0.15f, 0.15f, 1f);
    private static final Color COLOR_REACHABLE = new Color(0.2f, 0.5f, 0.9f, 0.30f);
    private static final Color COLOR_TARGET_IN_RANGE = new Color(0.25f, 1f, 0.35f, 0.95f);
    private static final Color COLOR_TARGET_OUT_OF_RANGE = new Color(1f, 0.25f, 0.2f, 0.95f);

    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private final Batch spriteBatch;
    private final AssetService assetService;
    private final Map<CharacterClass, TextureRegion[][]> sheetCache = new EnumMap<>(CharacterClass.class);

    private final BooleanSupplier localPlayerActiveCheck;
    private List<Vector2> cachedReachable;
    private float lastRemainingDistance = -1f;
    private float pulseTime = 0f;

    public PlayerRenderer(AssetService assetService, Batch spriteBatch, BooleanSupplier localPlayerActiveCheck) {
        this.assetService = assetService;
        this.spriteBatch = spriteBatch;
        this.localPlayerActiveCheck = localPlayerActiveCheck;
    }

    public void renderLocal(Player player, Matrix4 projection, NavGrid navGrid, float delta) {
        player.getAnimationController().update(
            delta, player.getPosition(), player.getMovementController());
        pulseTime += delta;

        float remaining = player.getMovementController().getRemainingMovementDistance();
        if (navGrid != null && Math.abs(remaining - lastRemainingDistance) > 0.0001f) {
            cachedReachable = navGrid.getReachablePositions(player.getPosition(), remaining);
            lastRemainingDistance = remaining;
        }

        drawReachable(projection);
        drawCharacter(player, projection);
        drawPath(player, projection);
        drawBars(player, projection, true);
        drawRing(player, projection, COLOR_PLAYER_RING, 0.43f);
    }

    public void renderEnemy(Player player, Matrix4 projection, float delta,
                            boolean targetSelectionActive, boolean inRange) {
        player.getAnimationController().update(
            delta, player.getPosition(), player.getMovementController());

        if (targetSelectionActive) {
            float pulse = 0.47f + 0.04f * (float) Math.sin(pulseTime * 6f);
            drawRing(player, projection,
                inRange ? COLOR_TARGET_IN_RANGE : COLOR_TARGET_OUT_OF_RANGE, pulse);
        }

        drawCharacter(player, projection);
        drawBars(player, projection, false);
    }

    private void drawReachable(Matrix4 projection) {
        if (cachedReachable == null || !localPlayerActiveCheck.getAsBoolean()) return;
        shapeRenderer.setProjectionMatrix(projection);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(COLOR_REACHABLE);
        for (Vector2 pos : cachedReachable) {
            shapeRenderer.circle(pos.x, pos.y, NavGrid.NODE_SIZE * 0.35f, 6);
        }
        shapeRenderer.end();
    }

    private void drawCharacter(Player player, Matrix4 projection) {
        TextureRegion frame = currentFrame(player);
        float x = player.getPosition().x - SPRITE_WIDTH / 2f;
        float y = player.getPosition().y + SPRITE_Y_OFFSET;

        if (frame != null) {
            spriteBatch.setProjectionMatrix(projection);
            spriteBatch.setColor(Color.WHITE);
            spriteBatch.begin();
            spriteBatch.draw(frame, x, y, SPRITE_WIDTH, SPRITE_HEIGHT);
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
        TextureRegion[][] sheet = sheetFor(player.getCharacterClass());
        if (sheet == null) return null;

        int row = player.getAnimationController().getCurrentRow();
        int frame = player.getAnimationController().getCurrentFrame();
        if (row < 0 || row >= sheet.length || frame < 0 || frame >= sheet[row].length) return null;
        return sheet[row][frame];
    }

    private TextureRegion[][] sheetFor(CharacterClass characterClass) {
        TextureRegion[][] cached = sheetCache.get(characterClass);
        if (cached != null) return cached;

        Texture texture = assetService.tryGet(SpriteAssets.forClass(characterClass));
        if (texture == null) return null;

        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        TextureRegion[][] split = TextureRegion.split(texture, FRAME_WIDTH, FRAME_HEIGHT);
        if (split.length != 24) {
            System.out.println("Invalid sprite sheet row count for " + characterClass
                + ": expected 24, got " + split.length);
            return null;
        }
        for (TextureRegion[] row : split) {
            if (row.length != PlayerAnimationController.FRAMES_PER_ROW) {
                System.out.println("Invalid sprite sheet column count for " + characterClass
                    + ": expected " + PlayerAnimationController.FRAMES_PER_ROW
                    + ", got " + row.length);
                return null;
            }
        }
        sheetCache.put(characterClass, split);
        return split;
    }

    private void drawPath(Player player, Matrix4 projection) {
        List<Vector2> path = player.getMovementController().getPath();
        if (path == null || path.size() <= 1) return;

        shapeRenderer.setProjectionMatrix(projection);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(COLOR_PATH);
        Vector2 previous = player.getPosition();
        for (Vector2 waypoint : path) {
            shapeRenderer.line(previous.x, previous.y, waypoint.x, waypoint.y);
            previous = waypoint;
        }
        shapeRenderer.end();
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

    private Color fallbackColor(CharacterClass characterClass) {
        switch (characterClass) {
            case WARRIOR: return new Color(0.65f, 0.18f, 0.18f, 1f);
            case MAGE: return new Color(0.34f, 0.22f, 0.72f, 1f);
            case ARCHER: return new Color(0.18f, 0.55f, 0.30f, 1f);
            case PALADIN: return new Color(0.85f, 0.78f, 0.35f, 1f);
            case ROGUE: return new Color(0.28f, 0.20f, 0.35f, 1f);
            default: return Color.WHITE;
        }
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
    }
}
