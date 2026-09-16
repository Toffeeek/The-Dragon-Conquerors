package com.github.thedragonconquerors;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
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
 * Generated drawables with bundled Cinzel headings and Inter body text.
 * The built-in bitmap font remains a fallback if font assets cannot be loaded.
 */
public final class FantasyUiTheme implements Disposable {
    public static final float VIRTUAL_WIDTH = 1280f;
    public static final float VIRTUAL_HEIGHT = 720f;

    public static final Color TEXT_PRIMARY = new Color(0.96f, 0.95f, 0.89f, 1f);
    public static final Color TEXT_MUTED = new Color(0.74f, 0.81f, 0.84f, 1f);
    public static final Color GOLD = new Color(0.93f, 0.79f, 0.49f, 1f);
    public static final Color GOLD_DIM = new Color(0.59f, 0.51f, 0.34f, 1f);
    public static final Color SUCCESS = new Color(0.42f, 0.78f, 0.48f, 1f);
    public static final Color ERROR = new Color(0.95f, 0.42f, 0.34f, 1f);

    private static final Color PANEL = new Color(0.045f, 0.085f, 0.12f, 0.84f);
    private static final Color PANEL_ALT = new Color(0.09f, 0.15f, 0.19f, 0.72f);
    private static final Color BORDER = new Color(0.38f, 0.47f, 0.50f, 0.65f);
    private static final Color FIELD = new Color(0.025f, 0.055f, 0.08f, 0.82f);
    private static final Color IRON = new Color(0.08f, 0.15f, 0.20f, 0.78f);
    private static final Color IRON_HOVER = new Color(0.15f, 0.27f, 0.32f, 0.92f);
    private static final Color IRON_DOWN = new Color(0.04f, 0.10f, 0.14f, 0.95f);
    private static final Color BRONZE = new Color(0.19f, 0.28f, 0.29f, 0.94f);
    private static final Color BRONZE_HOVER = new Color(0.28f, 0.39f, 0.39f, 1f);
    private static final Color BRONZE_DOWN = new Color(0.10f, 0.18f, 0.21f, 1f);
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
    private final FileHandle fontFile;
    private final FileHandle displayFontFile;
    private float appliedTextScale = 1f;

    public FantasyUiTheme() {
        fontFile = findReadableFont();
        FileHandle cinzel=com.badlogic.gdx.Gdx.files.internal("fonts/Cinzel.ttf");
        displayFontFile=cinzel.exists()?cinzel:fontFile;
        background = new TextureRegionDrawable(createDungeonBackground());
        panel = new NinePatchDrawable(createPatch(PANEL, BORDER, 14, 2));
        panelAlt = new NinePatchDrawable(createPatch(PANEL_ALT, BORDER, 12, 1));
        inset = new NinePatchDrawable(createPatch(
            new Color(0.04f, 0.09f, 0.13f, 0.45f),
            new Color(0.30f, 0.43f, 0.48f, 0.40f), 6, 1));
        divider = new TextureRegionDrawable(createSolidTexture(GOLD_DIM, 4, 2));

        createFontsAndLabels();
        createButtons();
        createTextField();
        refreshTextScale();
    }

    public boolean refreshTextScale() {
        float next = com.github.thedragonconquerors.ui.PresentationSettings.textScale();
        if (next == appliedTextScale) return false;
        for (BitmapFont font : skin.getAll(BitmapFont.class).values())
            font.getData().setScale(font.getData().scaleX * next / appliedTextScale);
        appliedTextScale = next;
        return true;
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

    /** Flat UI fill with its original colour, owned and disposed by this theme. */
    public Drawable solid(Color color) {
        return new TextureRegionDrawable(createSolidTexture(color, 4, 4));
    }

    private void createFontsAndLabels() {
        BitmapFont body = font(18, 1.05f);
        BitmapFont title = font(44, 2.85f, displayFontFile);
        BitmapFont heading = font(25, 1.65f, displayFontFile);
        BitmapFont button = font(18, 1.10f, displayFontFile);
        BitmapFont small = font(16, 0.86f);
        BitmapFont tiny = font(14, 0.74f);

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
        skin.add("class-role", new Label.LabelStyle(small, TEXT_MUTED), Label.LabelStyle.class);
    }

    private BitmapFont font(int size, float fallbackScale) {
        return font(size,fallbackScale,fontFile);
    }
    private BitmapFont font(int size, float fallbackScale, FileHandle face) {
        if (face != null) {
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(face);
            try {
                FreeTypeFontParameter parameter = new FreeTypeFontParameter();
                parameter.size = size;
                parameter.color = Color.WHITE;
                parameter.borderWidth = size<20?1.1f:.8f;
                parameter.borderColor = new Color(.015f,.025f,.035f,.9f);
                parameter.shadowOffsetY = 1;
                parameter.shadowColor = new Color(0,0,0,.6f);
                parameter.minFilter = Texture.TextureFilter.Linear;
                parameter.magFilter = Texture.TextureFilter.Linear;
                parameter.hinting = FreeTypeFontGenerator.Hinting.Full;
                parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "\u2013\u2014\u2018\u2019\u201c\u201d";
                BitmapFont font = generator.generateFont(parameter);
                font.setUseIntegerPositions(true);
                return font;
            } finally {
                generator.dispose();
            }
        }

        BitmapFont font = new BitmapFont();
        font.getRegion().getTexture().setFilter(
            Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        font.getData().setScale(fallbackScale);
        font.setUseIntegerPositions(true);
        return font;
    }

    private FileHandle findReadableFont() {
        FileHandle bundled = com.badlogic.gdx.Gdx.files.internal("fonts/Inter-Regular.otf");
        if (bundled.exists()) return bundled;
        String windowsDirectory = System.getenv("WINDIR");
        String[] paths = {
            (windowsDirectory == null ? "C:/Windows" : windowsDirectory) + "/Fonts/segoeui.ttf",
            (windowsDirectory == null ? "C:/Windows" : windowsDirectory) + "/Fonts/arial.ttf",
            "/usr/share/fonts/TTF/DejaVuSans.ttf",
            "/usr/share/fonts/TTF/LiberationSans-Regular.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf",
            "/usr/share/fonts/liberation/LiberationSans-Regular.ttf",
            "/System/Library/Fonts/Supplemental/Arial.ttf"
        };
        for (String path : paths) {
            FileHandle handle = new FileHandle(path);
            if (handle.exists() && !handle.isDirectory()) return handle;
        }
        return null;
    }

    private void createButtons() {
        BitmapFont buttonFont = skin.get("font-button", BitmapFont.class);
        BitmapFont smallFont = skin.get("font-small", BitmapFont.class);

        TextButton.TextButtonStyle primary = buttonStyle(
            buttonFont, BRONZE, BRONZE_HOVER, BRONZE_DOWN, GOLD, GOLD_DIM);
        skin.add("primary", primary, TextButton.TextButtonStyle.class);
        skin.add("default", primary, TextButton.TextButtonStyle.class);

        TextButton.TextButtonStyle secondary = buttonStyle(
            buttonFont, IRON, IRON_HOVER, IRON_DOWN, TEXT_PRIMARY, BORDER);
        skin.add("secondary", secondary, TextButton.TextButtonStyle.class);

        TextButton.TextButtonStyle quiet = buttonStyle(
            smallFont,
            new Color(0.035f, 0.09f, 0.13f, .42f),
            new Color(0.10f, 0.20f, 0.25f, .78f),
            new Color(0.02f, 0.06f, 0.10f, .80f),
            TEXT_MUTED,
            new Color(0.40f, 0.53f, 0.57f, .45f));
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
        style.checkedFontColor = GOLD;
        style.up = new NinePatchDrawable(createPatch(
            new Color(0.06f, 0.12f, 0.17f, .78f),
            new Color(0.28f, 0.39f, 0.44f, .65f), 6, 1));
        style.over = new NinePatchDrawable(createPatch(
            new Color(0.12f, 0.23f, 0.28f, .90f), GOLD_DIM, 6, 1));
        style.down = new NinePatchDrawable(createPatch(
            new Color(0.08f, 0.075f, 0.08f, 1f), GOLD_DIM, 11, 1));
        style.checked = new NinePatchDrawable(createPatch(
            new Color(0.14f, 0.26f, 0.28f, .96f), GOLD, 6, 2));
        style.checkedOver = new NinePatchDrawable(createPatch(
            new Color(0.20f, 0.33f, 0.34f, 1f), GOLD, 6, 2));
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
            for (int x = 0; x < width; x++) {
                float noise = (random.nextFloat() - 0.5f) * 0.008f;
                float glow=(float)Math.exp(-((x-150f)*(x-150f)+(y-100f)*(y-100f))/22000f);
                pixmap.setColor(
                    clamp(.018f + glow*.025f + noise),
                    clamp(.04f + glow*.09f + noise),
                    clamp(.07f + glow*.12f + noise),
                    1f);
                pixmap.drawPixel(x, y);
            }
        }

        // Quiet arcane engraving and stars, generated once rather than animated every frame.
        pixmap.setColor(.5f,.65f,.68f,.10f);
        pixmap.drawCircle(155,136,105);pixmap.drawCircle(155,136,110);
        for(int i=0;i<12;i++) {
            double a=i*Math.PI/6;
            int x=155+(int)(105*Math.cos(a)),y=136+(int)(105*Math.sin(a));
            pixmap.drawLine(x-3,y,x+3,y);pixmap.drawLine(x,y-3,x,y+3);
        }
        for(int i=0;i<85;i++) {
            pixmap.setColor(.7f,.8f,.83f,.12f+random.nextFloat()*.2f);
            pixmap.drawPixel(random.nextInt(width),random.nextInt(height));
        }

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

    private NinePatch createPatch(Color fill, Color border, int radius, int borderWidth) {
        int size = 64;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        // Replacement blending keeps translucent interiors truly translucent over the border.
        pixmap.setBlending(Pixmap.Blending.None);
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

    /** Slim gilded resource track with a bevelled, coloured glass fill. */
    public Drawable resourceTrack() {
        var drawable=new NinePatchDrawable(createPatch(new Color(.02f,.06f,.09f,.62f),GOLD_DIM,5,1));
        drawable.setMinHeight(22);drawable.setMinWidth(0);
        drawable.setLeftWidth(2);drawable.setRightWidth(2);drawable.setTopHeight(2);drawable.setBottomHeight(2);
        return drawable;
    }
    public Drawable resourceFill(Color base) {
        Pixmap p=new Pixmap(8,24,Pixmap.Format.RGBA8888);
        for(int y=0;y<24;y++) {
            float light=y<5?1.2f:y<12?1f:.73f;
            p.setColor(clamp(base.r*light),clamp(base.g*light),clamp(base.b*light),.94f);
            p.fillRectangle(0,y,8,1);
        }
        p.setColor(1,1,1,.3f);p.drawLine(0,1,7,1);
        Texture t=new Texture(p);p.dispose();textures.add(t);
        var fill=new TextureRegionDrawable(t);fill.setMinWidth(0);fill.setMinHeight(18);
        return fill;
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
