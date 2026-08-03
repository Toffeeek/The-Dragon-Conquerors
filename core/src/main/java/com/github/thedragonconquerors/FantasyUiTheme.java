package com.github.thedragonconquerors;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

import java.util.Random;

/**
 * Shared procedural UI theme for menu and lobby screens.
 *
 * The theme deliberately uses no external textures or fonts so it can be added to
 * the current project without changing the asset pipeline. Replace the generated
 * drawables and BitmapFonts later when final art assets are available.
 */
public final class FantasyUiTheme implements Disposable {
    public static final float VIRTUAL_WIDTH = 1280f;
    public static final float VIRTUAL_HEIGHT = 720f;

    public static final Color TEXT_PRIMARY = new Color(0.94f, 0.90f, 0.80f, 1f);
    public static final Color TEXT_MUTED = new Color(0.67f, 0.64f, 0.58f, 1f);
    public static final Color GOLD = new Color(0.88f, 0.68f, 0.30f, 1f);
    public static final Color GOLD_DIM = new Color(0.57f, 0.43f, 0.23f, 1f);
    public static final Color SUCCESS = new Color(0.42f, 0.78f, 0.48f, 1f);
    public static final Color ERROR = new Color(0.95f, 0.42f, 0.34f, 1f);

    private static final Color PANEL = new Color(0.075f, 0.070f, 0.075f, 0.96f);
    private static final Color PANEL_ALT = new Color(0.105f, 0.095f, 0.095f, 0.96f);
    private static final Color BORDER = new Color(0.42f, 0.31f, 0.16f, 1f);
    private static final Color FIELD = new Color(0.045f, 0.043f, 0.048f, 1f);
    private static final Color IRON = new Color(0.17f, 0.18f, 0.20f, 1f);
    private static final Color IRON_HOVER = new Color(0.23f, 0.24f, 0.27f, 1f);
    private static final Color IRON_DOWN = new Color(0.11f, 0.12f, 0.14f, 1f);
    private static final Color BRONZE = new Color(0.50f, 0.27f, 0.11f, 1f);
    private static final Color BRONZE_HOVER = new Color(0.66f, 0.36f, 0.14f, 1f);
    private static final Color BRONZE_DOWN = new Color(0.35f, 0.18f, 0.08f, 1f);
    private static final Color BLUE = new Color(0.13f, 0.35f, 0.52f, 1f);
    private static final Color BLUE_HOVER = new Color(0.18f, 0.47f, 0.66f, 1f);
    private static final Color RED = new Color(0.50f, 0.16f, 0.13f, 1f);
    private static final Color RED_HOVER = new Color(0.67f, 0.22f, 0.17f, 1f);
    private static final Color DISABLED = new Color(0.18f, 0.17f, 0.17f, 1f);

    private final Skin skin = new Skin();
    private final Array<Texture> textures = new Array<>();

    private final Drawable background;
    private final Drawable panel;
    private final Drawable panelAlt;
    private final Drawable inset;
    private final Drawable divider;

    public FantasyUiTheme() {
        background = new TextureRegionDrawable(createDungeonBackground());
        panel = new NinePatchDrawable(createPatch(PANEL, BORDER, 14, 2));
        panelAlt = new NinePatchDrawable(createPatch(PANEL_ALT, BORDER, 12, 1));
        inset = new NinePatchDrawable(createPatch(
            new Color(0.035f, 0.034f, 0.038f, 0.94f),
            new Color(0.25f, 0.20f, 0.13f, 1f), 10, 1));
        divider = new TextureRegionDrawable(createSolidTexture(GOLD_DIM, 4, 2));

        createFontsAndLabels();
        createButtons();
        createTextField();
    }

    public Skin skin() {
        return skin;
    }

    public Drawable background() {
        return background;
    }

    public Drawable panel() {
        return panel;
    }

    public Drawable panelAlt() {
        return panelAlt;
    }

    public Drawable inset() {
        return inset;
    }

    public Drawable divider() {
        return divider;
    }

    private void createFontsAndLabels() {
        BitmapFont body = font(1.05f);
        BitmapFont title = font(2.85f);
        BitmapFont heading = font(1.65f);
        BitmapFont button = font(1.10f);
        BitmapFont small = font(0.86f);
        BitmapFont tiny = font(0.74f);

        // Register every font so Skin.dispose() owns and disposes them.
        skin.add("font-body", body, BitmapFont.class);
        skin.add("font-title", title, BitmapFont.class);
        skin.add("font-heading", heading, BitmapFont.class);
        skin.add("font-button", button, BitmapFont.class);
        skin.add("font-small", small, BitmapFont.class);
        skin.add("font-tiny", tiny, BitmapFont.class);

        skin.add("default", new Label.LabelStyle(body, TEXT_PRIMARY), Label.LabelStyle.class);
        skin.add("title", new Label.LabelStyle(title, GOLD), Label.LabelStyle.class);
        skin.add("heading", new Label.LabelStyle(heading, TEXT_PRIMARY), Label.LabelStyle.class);
        skin.add("eyebrow", new Label.LabelStyle(small, GOLD_DIM), Label.LabelStyle.class);
        skin.add("subtitle", new Label.LabelStyle(body, TEXT_MUTED), Label.LabelStyle.class);
        skin.add("section", new Label.LabelStyle(small, GOLD), Label.LabelStyle.class);
        skin.add("caption", new Label.LabelStyle(tiny, TEXT_MUTED), Label.LabelStyle.class);
        skin.add("status", new Label.LabelStyle(small, TEXT_PRIMARY), Label.LabelStyle.class);
        skin.add("class-title", new Label.LabelStyle(heading, GOLD), Label.LabelStyle.class);
        skin.add("class-role", new Label.LabelStyle(small, GOLD_DIM), Label.LabelStyle.class);
    }

    private BitmapFont font(float scale) {
        BitmapFont font = new BitmapFont();
        font.getData().setScale(scale);
        font.setUseIntegerPositions(true);
        return font;
    }

    private void createButtons() {
        BitmapFont buttonFont = skin.get("font-button", BitmapFont.class);
        BitmapFont smallFont = skin.get("font-small", BitmapFont.class);

        TextButton.TextButtonStyle primary = buttonStyle(
            buttonFont, BRONZE, BRONZE_HOVER, BRONZE_DOWN, GOLD, BORDER);
        skin.add("primary", primary, TextButton.TextButtonStyle.class);
        skin.add("default", primary, TextButton.TextButtonStyle.class);

        TextButton.TextButtonStyle secondary = buttonStyle(
            buttonFont, IRON, IRON_HOVER, IRON_DOWN, TEXT_PRIMARY, BORDER);
        skin.add("secondary", secondary, TextButton.TextButtonStyle.class);

        TextButton.TextButtonStyle quiet = buttonStyle(
            smallFont,
            new Color(0.09f, 0.09f, 0.10f, 1f),
            new Color(0.16f, 0.16f, 0.18f, 1f),
            new Color(0.06f, 0.06f, 0.07f, 1f),
            TEXT_MUTED,
            new Color(0.23f, 0.20f, 0.15f, 1f));
        skin.add("quiet", quiet, TextButton.TextButtonStyle.class);

        TextButton.TextButtonStyle danger = buttonStyle(
            smallFont,
            new Color(0.30f, 0.10f, 0.09f, 1f),
            new Color(0.48f, 0.15f, 0.12f, 1f),
            new Color(0.20f, 0.07f, 0.06f, 1f),
            TEXT_PRIMARY,
            new Color(0.55f, 0.20f, 0.16f, 1f));
        skin.add("danger", danger, TextButton.TextButtonStyle.class);

        skin.add("team-blue", toggleStyle(smallFont, BLUE, BLUE_HOVER),
            TextButton.TextButtonStyle.class);
        skin.add("team-red", toggleStyle(smallFont, RED, RED_HOVER),
            TextButton.TextButtonStyle.class);
        skin.add("class-card", classCardStyle(smallFont),
            TextButton.TextButtonStyle.class);
    }

    private TextButton.TextButtonStyle buttonStyle(BitmapFont font,
                                                    Color up,
                                                    Color over,
                                                    Color down,
                                                    Color text,
                                                    Color border) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = font;
        style.fontColor = text;
        style.overFontColor = Color.WHITE;
        style.downFontColor = Color.WHITE;
        style.disabledFontColor = new Color(0.44f, 0.42f, 0.40f, 1f);
        style.up = new NinePatchDrawable(createPatch(up, border, 10, 1));
        style.over = new NinePatchDrawable(createPatch(over, GOLD_DIM, 10, 1));
        style.down = new NinePatchDrawable(createPatch(down, GOLD, 10, 1));
        style.disabled = new NinePatchDrawable(createPatch(DISABLED,
            new Color(0.24f, 0.22f, 0.20f, 1f), 10, 1));
        return style;
    }

    private TextButton.TextButtonStyle toggleStyle(BitmapFont font,
                                                    Color checked,
                                                    Color checkedOver) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = font;
        style.fontColor = TEXT_MUTED;
        style.overFontColor = TEXT_PRIMARY;
        style.checkedFontColor = Color.WHITE;
        style.up = new NinePatchDrawable(createPatch(IRON,
            new Color(0.25f, 0.23f, 0.20f, 1f), 9, 1));
        style.over = new NinePatchDrawable(createPatch(IRON_HOVER, BORDER, 9, 1));
        style.down = new NinePatchDrawable(createPatch(IRON_DOWN, GOLD_DIM, 9, 1));
        style.checked = new NinePatchDrawable(createPatch(checked, GOLD_DIM, 9, 2));
        style.checkedOver = new NinePatchDrawable(createPatch(checkedOver, GOLD, 9, 2));
        return style;
    }

    private TextButton.TextButtonStyle classCardStyle(BitmapFont font) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = font;
        style.fontColor = TEXT_MUTED;
        style.overFontColor = TEXT_PRIMARY;
        style.checkedFontColor = new Color(0.13f, 0.09f, 0.04f, 1f);
        style.up = new NinePatchDrawable(createPatch(
            new Color(0.105f, 0.105f, 0.115f, 1f),
            new Color(0.25f, 0.23f, 0.20f, 1f), 11, 1));
        style.over = new NinePatchDrawable(createPatch(
            new Color(0.16f, 0.15f, 0.15f, 1f), BORDER, 11, 1));
        style.down = new NinePatchDrawable(createPatch(
            new Color(0.08f, 0.075f, 0.08f, 1f), GOLD_DIM, 11, 1));
        style.checked = new NinePatchDrawable(createPatch(
            new Color(0.73f, 0.55f, 0.24f, 1f), GOLD, 11, 2));
        style.checkedOver = new NinePatchDrawable(createPatch(
            new Color(0.84f, 0.65f, 0.29f, 1f), GOLD, 11, 2));
        return style;
    }

    private void createTextField() {
        BitmapFont body = skin.get("font-body", BitmapFont.class);

        TextField.TextFieldStyle style = new TextField.TextFieldStyle();
        style.font = body;
        style.fontColor = TEXT_PRIMARY;
        style.messageFontColor = new Color(0.42f, 0.40f, 0.39f, 1f);
        style.disabledFontColor = new Color(0.45f, 0.43f, 0.41f, 1f);
        style.background = new NinePatchDrawable(createPatch(FIELD, BORDER, 9, 1));
        style.focusedBackground = new NinePatchDrawable(createPatch(
            new Color(0.055f, 0.050f, 0.050f, 1f), GOLD, 9, 2));
        style.disabledBackground = new NinePatchDrawable(createPatch(
            new Color(0.055f, 0.052f, 0.052f, 1f),
            new Color(0.20f, 0.18f, 0.16f, 1f), 9, 1));
        style.cursor = new TextureRegionDrawable(createSolidTexture(GOLD, 2, 2));
        style.selection = new TextureRegionDrawable(createSolidTexture(
            new Color(0.52f, 0.31f, 0.12f, 0.65f), 2, 2));
        skin.add("default", style, TextField.TextFieldStyle.class);
    }

    private Texture createDungeonBackground() {
        int width = 512;
        int height = 288;
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.SourceOver);

        Random random = new Random(7319842L);

        for (int y = 0; y < height; y++) {
            float t = y / (float) (height - 1);
            float shade = 0.038f + t * 0.025f;
            for (int x = 0; x < width; x++) {
                float noise = (random.nextFloat() - 0.5f) * 0.018f;
                pixmap.setColor(
                    clamp(shade + noise),
                    clamp(shade * 0.88f + noise),
                    clamp(shade * 0.92f + noise),
                    1f);
                pixmap.drawPixel(x, y);
            }
        }

        // Staggered dungeon-stone joints.
        pixmap.setColor(0.018f, 0.017f, 0.020f, 0.72f);
        int rowHeight = 36;
        int stoneWidth = 72;
        for (int y = 0; y < height; y += rowHeight) {
            pixmap.fillRectangle(0, y, width, 2);
            int row = y / rowHeight;
            int offset = (row % 2 == 0) ? 0 : stoneWidth / 2;
            for (int x = offset; x < width; x += stoneWidth) {
                pixmap.fillRectangle(x, y, 2, rowHeight);
            }
        }

        // Warm torch glows frame the menu without requiring image assets.
        addGlow(pixmap, 70, height / 2, 92);
        addGlow(pixmap, width - 70, height / 2, 92);

        // Vignette.
        for (int i = 0; i < 58; i++) {
            float alpha = 0.010f + (58 - i) * 0.0025f;
            pixmap.setColor(0f, 0f, 0f, alpha);
            pixmap.fillRectangle(i, 0, 1, height);
            pixmap.fillRectangle(width - i - 1, 0, 1, height);
        }
        for (int i = 0; i < 34; i++) {
            float alpha = 0.008f + (34 - i) * 0.0024f;
            pixmap.setColor(0f, 0f, 0f, alpha);
            pixmap.fillRectangle(0, i, width, 1);
            pixmap.fillRectangle(0, height - i - 1, width, 1);
        }

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        textures.add(texture);
        return texture;
    }

    private void addGlow(Pixmap pixmap, int centerX, int centerY, int radius) {
        for (int r = radius; r >= 4; r -= 4) {
            float strength = 1f - r / (float) radius;
            pixmap.setColor(0.70f, 0.25f, 0.045f, 0.004f + strength * 0.018f);
            pixmap.fillCircle(centerX, centerY, r);
        }
        pixmap.setColor(0.95f, 0.58f, 0.16f, 0.65f);
        pixmap.fillCircle(centerX, centerY, 4);
    }

    private NinePatch createPatch(Color fill, Color border, int radius, int borderWidth) {
        int size = 64;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();

        pixmap.setColor(border);
        drawRoundedRect(pixmap, 0, 0, size, size, radius);

        int innerRadius = Math.max(1, radius - borderWidth);
        pixmap.setColor(fill);
        drawRoundedRect(pixmap,
            borderWidth,
            borderWidth,
            size - borderWidth * 2,
            size - borderWidth * 2,
            innerRadius);

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        textures.add(texture);

        int split = 16;
        return new NinePatch(texture, split, split, split, split);
    }

    private void drawRoundedRect(Pixmap pixmap, int x, int y, int width, int height, int radius) {
        int r = Math.max(1, Math.min(radius, Math.min(width, height) / 2));
        pixmap.fillRectangle(x + r, y, width - 2 * r, height);
        pixmap.fillRectangle(x, y + r, width, height - 2 * r);
        pixmap.fillCircle(x + r, y + r, r);
        pixmap.fillCircle(x + width - r - 1, y + r, r);
        pixmap.fillCircle(x + r, y + height - r - 1, r);
        pixmap.fillCircle(x + width - r - 1, y + height - r - 1, r);
    }

    private Texture createSolidTexture(Color color, int width, int height) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        textures.add(texture);
        return texture;
    }

    private float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    @Override
    public void dispose() {
        skin.dispose();
        for (Texture texture : textures) {
            texture.dispose();
        }
        textures.clear();
    }
}
