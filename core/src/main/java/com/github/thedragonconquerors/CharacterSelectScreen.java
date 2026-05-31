package com.github.thedragonconquerors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.github.thedragonconquerors.assets.AssetService;
import com.github.thedragonconquerors.assets.SpriteAssets;
import com.github.thedragonconquerors.entities.CharacterClass;
import com.github.thedragonconquerors.stats.StatComponent;
import java.util.function.Consumer;

public class CharacterSelectScreen extends ScreenAdapter {

    // ── card layout (horizontal row) ─────────────────────────────
    private static final float CARD_W      = 200f;
    private static final float CARD_H      = 260f;
    private static final float CARD_GAP    = 16f;
    private static final float SPRITE_SIZE = 64f;
    private static final float SCROLL_SPEED = 40f;   // pixels per scroll notch

    // ── colours ───────────────────────────────────────────────────
    private static final Color BG             = new Color(0.08f, 0.08f, 0.13f, 1f);
    private static final Color CARD_BG        = new Color(0.13f, 0.13f, 0.20f, 1f);
    private static final Color CARD_BORDER    = new Color(0.40f, 0.40f, 0.55f, 1f);
    private static final Color CARD_HOVER     = new Color(0.22f, 0.22f, 0.35f, 1f);
    private static final Color HI_BG          = new Color(0.18f, 0.22f, 0.10f, 1f);
    private static final Color HI_BORDER      = new Color(0.85f, 0.80f, 0.15f, 1f);
    private static final Color TEXT_TITLE     = new Color(1.0f,  1.0f,  1.0f,  1f);
    private static final Color TEXT_STAT      = new Color(0.75f, 0.85f, 1.0f,  1f);
    private static final Color TEXT_LABEL     = new Color(0.55f, 0.55f, 0.70f, 1f);
    private static final Color TEXT_HINT      = new Color(0.70f, 0.70f, 0.80f, 1f);
    private static final Color HP_COLOR       = new Color(0.85f, 0.20f, 0.20f, 1f);
    private static final Color MANA_COLOR     = new Color(0.20f, 0.45f, 0.95f, 1f);
    private static final Color CONFIRM_BG     = new Color(0.10f, 0.35f, 0.10f, 1f);
    private static final Color CONFIRM_BORDER = new Color(0.30f, 0.90f, 0.30f, 1f);
    private static final Color SCROLL_BG      = new Color(0.15f, 0.15f, 0.22f, 1f);
    private static final Color SCROLL_THUMB   = new Color(0.45f, 0.45f, 0.65f, 1f);

    // ── deps ──────────────────────────────────────────────────────
    private final Viewport  viewport;
    private final int       teamIdx;
    private final Consumer<CharacterClass> onClassSelected;

    private final SpriteBatch   batch;
    private final ShapeRenderer shapes;
    private final BitmapFont    fontTitle;
    private final BitmapFont    fontStat;

    private final CharacterClass[] classes = CharacterClass.values();
    private final Texture[]        sprites;
    private int   highlightedIndex = -1;

    // ── scroll state ──────────────────────────────────────────────
    private float scrollX    = 0f;   // how many pixels we've scrolled right
    private float maxScrollX = 0f;   // computed each frame

    public CharacterSelectScreen(Main game, int teamIdx,
                                 Consumer<CharacterClass> onClassSelected) {
        this.viewport        = game.getViewport();
        this.teamIdx         = teamIdx;
        this.onClassSelected = onClassSelected;

        batch     = new SpriteBatch();
        shapes    = new ShapeRenderer();
        fontTitle = new BitmapFont();
        fontStat  = new BitmapFont();

        AssetService assetService = game.getAssetService();
        sprites = new Texture[classes.length];
        for (int i = 0; i < classes.length; i++) {
            SpriteAssets sa = SpriteAssets.forClass(classes[i]);
            sprites[i] = assetService.tryGet(sa);
            if (sprites[i] == null && sa != null) {
                try { sprites[i] = assetService.load(sa); } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int sx, int sy, int pointer, int button) {
                handleClick(sx, sy);
                return true;
            }
            @Override
            public boolean scrolled(float amountX, float amountY) {
                scrollX = MathUtils.clamp(scrollX + amountY * SCROLL_SPEED, 0f, maxScrollX);
                return true;
            }
        });
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(BG);

        int sw = viewport.getScreenWidth();
        int sh = viewport.getScreenHeight();
        Matrix4 screen = new Matrix4().setToOrtho2D(0, 0, sw, sh);

        int   n       = classes.length;
        float totalW  = n * CARD_W + (n - 1) * CARD_GAP;

        // title area at top
        float titleH  = 50f;
        // cards are vertically centred in the remaining space
        float cardY   = (sh - titleH - CARD_H) / 2f;

        // scrollbar at bottom
        float sbH     = 8f;
        float sbY     = cardY - sbH - 10f;
        float sbX     = 0f;
        float sbW     = (float) sw;

        maxScrollX = Math.max(0f, totalW - sw + CARD_GAP * 2);

        // mouse in viewport space
        float mx       = Gdx.input.getX() - viewport.getScreenX();
        float myScreen = sh - (Gdx.input.getY() - viewport.getScreenY());

        shapes.setProjectionMatrix(screen);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < n; i++) {
            float cx = cardScreenX(i);
            if (cx + CARD_W < 0 || cx > sw) continue;
            boolean hovered     = isOver(mx, myScreen, cx, cardY, CARD_W, CARD_H);
            boolean highlighted = (i == highlightedIndex);
            shapes.setColor(highlighted ? HI_BG : (hovered ? CARD_HOVER : CARD_BG));
            shapes.rect(cx, cardY, CARD_W, CARD_H);
        }

        if (highlightedIndex >= 0) {
            float[] btn = confirmBtn(sw, sh);
            shapes.setColor(CONFIRM_BG);
            shapes.rect(btn[0], btn[1], btn[2], btn[3]);
        }

        // horizontal scrollbar track + thumb
        if (maxScrollX > 0f) {
            shapes.setColor(SCROLL_BG);
            shapes.rect(sbX, sbY, sbW, sbH);
            float thumbW   = Math.max(30f, sbW * ((float) sw / totalW));
            float thumbFrac = scrollX / maxScrollX;
            float thumbX   = sbX + (sbW - thumbW) * thumbFrac;
            shapes.setColor(SCROLL_THUMB);
            shapes.rect(thumbX, sbY, thumbW, sbH);
        }

        shapes.end();

        // borders
        shapes.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < n; i++) {
            float cx = cardScreenX(i);
            if (cx + CARD_W < 0 || cx > sw) continue;
            boolean highlighted = (i == highlightedIndex);
            shapes.setColor(highlighted ? HI_BORDER : CARD_BORDER);
            shapes.rect(cx, cardY, CARD_W, CARD_H);
            if (highlighted) shapes.rect(cx+2, cardY+2, CARD_W-4, CARD_H-4);
        }
        if (highlightedIndex >= 0) {
            float[] btn = confirmBtn(sw, sh);
            shapes.setColor(CONFIRM_BORDER);
            shapes.rect(btn[0], btn[1], btn[2], btn[3]);
        }
        shapes.end();

        // text & sprites
        batch.setProjectionMatrix(screen);
        batch.begin();

        // title
        fontTitle.setColor(TEXT_TITLE);
        String titleStr = "SELECT YOUR CHARACTER  —  Team: " + (teamIdx == 1 ? "Blue" : "Red");
        fontTitle.draw(batch, titleStr, 16f, sh - 10f);

        // scroll hint
        if (maxScrollX > 0f) {
            fontStat.setColor(TEXT_HINT);
            fontStat.draw(batch, "\u25C4 scroll to see more \u25BA", 16f, sh - 28f);
        }

        for (int i = 0; i < n; i++) {
            float cx = cardScreenX(i);
            if (cx + CARD_W < 0 || cx > sw) continue;

            CharacterClass cls        = classes[i];
            StatComponent  stats      = cls.createBaseStats();
            boolean        highlighted = (i == highlightedIndex);

            // sprite centred at top of card
            float sprX = cx + (CARD_W - SPRITE_SIZE) / 2f;
            float sprY = cardY + CARD_H - 12f - SPRITE_SIZE;
            if (sprites[i] != null) {
                batch.draw(sprites[i], sprX, sprY, SPRITE_SIZE, SPRITE_SIZE);
            } else {
                fontTitle.setColor(TEXT_LABEL);
                fontTitle.draw(batch, "[?]", sprX + 16f, sprY + SPRITE_SIZE - 10f);
            }

            // class name centred below sprite
            fontTitle.setColor(highlighted ? HI_BORDER : TEXT_TITLE);
            fontTitle.draw(batch, cls.displayName, cx + 8f, sprY - 4f);

            // divider
            fontStat.setColor(TEXT_LABEL);
            fontStat.draw(batch, "──────────────", cx + 4f, sprY - 18f);

            // stats
            float sy    = sprY - 34f;
            float lineH = 16f;
            drawStat(batch, "HP",  stats.getMaxHp(),        cx, sy,           HP_COLOR);
            drawStat(batch, "MP",  stats.getMaxMana(),      cx, sy - lineH,   MANA_COLOR);
            drawStat(batch, "STR", stats.getStrength(),     cx, sy - lineH*2, TEXT_STAT);
            drawStat(batch, "ACC", stats.getAccuracy(),     cx, sy - lineH*3, TEXT_STAT);
            drawStat(batch, "SPD", stats.getSpeed(),        cx, sy - lineH*4, TEXT_STAT);
            drawStat(batch, "WIS", stats.getWisdom(),       cx, sy - lineH*5, TEXT_STAT);
            drawStat(batch, "INS", stats.getInspiration(),  cx, sy - lineH*6, TEXT_STAT);

            // hint at bottom
            fontStat.setColor(highlighted ? HI_BORDER : TEXT_HINT);
            fontStat.draw(batch, highlighted ? "Click again to confirm" : "Click to select",
                cx + 4f, cardY + 14f);
        }

        // confirm button label
        if (highlightedIndex >= 0) {
            float[] btn = confirmBtn(sw, sh);
            fontTitle.setColor(CONFIRM_BORDER);
            fontTitle.draw(batch, "Confirm  \u2713", btn[0] + 28f, btn[1] + btn[3] - 10f);
        }

        batch.end();
    }

    // ── input ─────────────────────────────────────────────────────

    private void handleClick(int screenX, int screenY) {
        int sw = viewport.getScreenWidth();
        int sh = viewport.getScreenHeight();
        float fx = screenX - viewport.getScreenX();
        float fy = sh - (screenY - viewport.getScreenY());

        float titleH = 50f;
        float cardY  = (sh - titleH - CARD_H) / 2f;

        if (highlightedIndex >= 0) {
            float[] btn = confirmBtn(sw, sh);
            if (isOver(fx, fy, btn[0], btn[1], btn[2], btn[3])) {
                confirmSelection();
                return;
            }
        }

        for (int i = 0; i < classes.length; i++) {
            float cx = cardScreenX(i);
            if (isOver(fx, fy, cx, cardY, CARD_W, CARD_H)) {
                if (highlightedIndex == i) confirmSelection();
                else highlightedIndex = i;
                return;
            }
        }
    }

    private void confirmSelection() {
        if (highlightedIndex < 0 || onClassSelected == null) return;
        CharacterClass chosen = classes[highlightedIndex];
        System.out.println("[CharSelect] Selected: " + chosen.displayName);
        Gdx.input.setInputProcessor(null);
        onClassSelected.accept(chosen);
    }

    // ── helpers ───────────────────────────────────────────────────

    /** Screen X (left edge) of card i, accounting for horizontal scroll. */
    private float cardScreenX(int i) {
        float startX = CARD_GAP;
        return startX + i * (CARD_W + CARD_GAP) - scrollX;
    }

    /** {x, y, w, h} of the confirm button — fixed to bottom-centre of screen. */
    private float[] confirmBtn(int sw, int sh) {
        float btnW = 140f, btnH = 34f;
        return new float[]{ (sw - btnW) / 2f, -18f, btnW, btnH };
    }

    private void drawStat(SpriteBatch b, String label, int value,
                          float x, float y, Color col) {
        fontStat.setColor(TEXT_LABEL);
        fontStat.draw(b, label + ":", x, y);
        fontStat.setColor(col);
        fontStat.draw(b, String.valueOf(value), x + 44f, y);
    }

    private boolean isOver(float mx, float my, float rx, float ry, float rw, float rh) {
        return mx >= rx && mx <= rx + rw && my >= ry && my <= ry + rh;
    }

    @Override public void resize(int w, int h) { viewport.update(w, h, true); }
    @Override public void hide()               { Gdx.input.setInputProcessor(null); }

    @Override
    public void dispose() {
        batch.dispose();
        shapes.dispose();
        fontTitle.dispose();
        fontStat.dispose();
    }
}
