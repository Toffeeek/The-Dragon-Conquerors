package com.github.thedragonconquerors.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.github.thedragonconquerors.combat.ActionResult;
import com.github.thedragonconquerors.combat.ActionType;
import com.github.thedragonconquerors.entities.Player;
import com.github.thedragonconquerors.stats.StatComponent;

public class HudRenderer implements Disposable {

    private final SpriteBatch   hudBatch;
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont    font;
    private final Viewport      viewport;

    // ── colours ───────────────────────────────────────────────────
    private static final Color BAR_BG         = new Color(0.2f,  0.2f,  0.2f,  0.85f);
    private static final Color HP_FILL        = new Color(0.85f, 0.15f, 0.15f, 1f);
    private static final Color HP_LOW         = new Color(1.0f,  0.35f, 0.05f, 1f);
    private static final Color MANA_FILL      = new Color(0.15f, 0.40f, 0.95f, 1f);
    private static final Color STAMINA_FILL   = new Color(0.2f,  0.8f,  0.3f,  1f);
    private static final Color TEXT_COLOR     = new Color(0.9f,  0.9f,  0.9f,  1f);

    // action panel
    private static final Color PANEL_BG       = new Color(0.08f, 0.08f, 0.12f, 0.95f);
    private static final Color PANEL_BORDER   = new Color(0.6f,  0.6f,  0.7f,  1f);
    private static final Color BTN_BG         = new Color(0.15f, 0.15f, 0.22f, 1f);
    private static final Color BTN_HOVER      = new Color(0.25f, 0.25f, 0.38f, 1f);
    private static final Color BTN_NOMANA     = new Color(0.3f,  0.3f,  0.35f, 0.85f);
    private static final Color BTN_BORDER     = new Color(0.5f,  0.5f,  0.6f,  1f);
    private static final Color TOGGLE_BG      = new Color(0.18f, 0.22f, 0.32f, 1f);
    private static final Color TOGGLE_HOVER   = new Color(0.28f, 0.34f, 0.50f, 1f);
    private static final Color TOGGLE_BORDER  = new Color(0.7f,  0.75f, 0.9f,  1f);
    private static final Color FEEDBACK_COLOR = new Color(1f,    0.9f,  0.3f,  1f);
    private static final Color MANA_TEXT      = new Color(0.5f,  0.7f,  1.0f,  1f);

    // ── stat bar layout (bottom-left) ─────────────────────────────
    private static final float BAR_W        = 200f;
    private static final float BAR_H        = 14f;
    private static final float BAR_X        = 16f;
    private static final float GAP          = 22f;
    private static final float STAMINA_Y    = 36f;
    private static final float MANA_Y       = STAMINA_Y + GAP;
    private static final float HP_Y         = MANA_Y + GAP;
    private static final float LABEL_OFFSET = BAR_H + 3f;

    // ── toggle button (bottom-right) ──────────────────────────────
    private static final float TOGGLE_W = 100f;
    private static final float TOGGLE_H = 32f;
    private static final float TOGGLE_PAD = 12f;   // from screen edge

    // ── action panel ──────────────────────────────────────────────
    private static final float BTN_W    = 180f;
    private static final float BTN_H    = 38f;
    private static final float BTN_GAP  = 6f;
    private static final float PANEL_PAD = 8f;

    // ── state ─────────────────────────────────────────────────────
    private boolean panelOpen           = false;
    private int     hoveredActionIndex  = -1;

    private String feedbackMessage = "";
    private float  feedbackTimer   = 0f;
    private static final float FEEDBACK_DURATION = 2.5f;

    // ── callback ──────────────────────────────────────────────────
    /** Set by GameOneScreen so HudRenderer can fire actions on click. */
    private java.util.function.Consumer<Integer> onActionSelected;

    public HudRenderer(Viewport viewport) {
        this.viewport      = viewport;
        this.hudBatch      = new SpriteBatch();
        this.shapeRenderer = new ShapeRenderer();
        this.font          = new BitmapFont();
        this.font.setColor(TEXT_COLOR);
    }

    public void setOnActionSelected(java.util.function.Consumer<Integer> cb) {
        this.onActionSelected = cb;
    }

    public void showFeedback(ActionResult result) {
        this.feedbackMessage = result.message;
        this.feedbackTimer   = FEEDBACK_DURATION;
    }

    public void showFeedback(String message) {
        this.feedbackMessage = message;
        this.feedbackTimer   = FEEDBACK_DURATION;
    }

    // ──────────────────────────────────────────────────────────────
    //  Hit-testing (called from GameOneScreen.touchDown)
    // ──────────────────────────────────────────────────────────────

    /**
     * Call this from GameOneScreen whenever a left-click occurs.
     * screenX/screenY are raw LibGDX screen coords (y=0 at top).
     * Returns true if the HUD consumed the click (so movement should be suppressed).
     */
    public boolean handleClick(int screenX, int screenY, ActionType[] actions, StatComponent stats) {
        int sw = viewport.getScreenWidth();
        int sh = viewport.getScreenHeight();
        // screenX/Y are full-window coords; subtract letterbox offset, then flip Y
        float fx = screenX - viewport.getScreenX();
        float fy = sh - (screenY - viewport.getScreenY());

        // ── toggle button ─────────────────────────────────────────
        float toggleX = sw - TOGGLE_W - TOGGLE_PAD;
        float toggleY = TOGGLE_PAD;
        if (fx >= toggleX && fx <= toggleX + TOGGLE_W &&
            fy >= toggleY && fy <= toggleY + TOGGLE_H) {
            panelOpen = !panelOpen;
            return true;
        }

        // ── action buttons (only when open) ───────────────────────
        if (panelOpen && actions != null) {
            float panelW = BTN_W + PANEL_PAD * 2;
            float panelH = PANEL_PAD + actions.length * (BTN_H + BTN_GAP);
            float panelX = sw - panelW - TOGGLE_PAD;
            float panelY = TOGGLE_PAD + TOGGLE_H + 4f;

            for (int i = 0; i < actions.length; i++) {
                float bx = panelX + PANEL_PAD;
                float by = panelY + PANEL_PAD + (actions.length - 1 - i) * (BTN_H + BTN_GAP);
                if (fx >= bx && fx <= bx + BTN_W && fy >= by && fy <= by + BTN_H) {
                    boolean canAfford = stats.getMana() >= actions[i].manaCost;
                    if (canAfford && onActionSelected != null) {
                        onActionSelected.accept(i);
                        panelOpen = false;   // close panel after selecting
                    } else if (!canAfford) {
                        showFeedback("Not enough mana for " + actions[i].displayName + "!");
                    }
                    return true;
                }
            }

            // click inside panel background but not a button — still consume
            if (fx >= panelX && fx <= panelX + panelW &&
                fy >= panelY && fy <= panelY + panelH) {
                return true;
            }
        }

        return false;
    }

    /**
     * Update hovered action based on mouse position (call each frame with Gdx.input.getX/Y).
     */
    public void updateHover(int screenX, int screenY, ActionType[] actions) {
        if (!panelOpen || actions == null) { hoveredActionIndex = -1; return; }
        int sw = viewport.getScreenWidth();
        int sh = viewport.getScreenHeight();
        // screenX/Y are full-window coords; subtract letterbox offset, then flip Y
        float fx = screenX - viewport.getScreenX();
        float fy = sh - (screenY - viewport.getScreenY());

        float panelW = BTN_W + PANEL_PAD * 2;
        float panelX = sw - panelW - TOGGLE_PAD;
        float panelY = TOGGLE_PAD + TOGGLE_H + 4f;

        hoveredActionIndex = -1;
        for (int i = 0; i < actions.length; i++) {
            float bx = panelX + PANEL_PAD;
            float by = panelY + PANEL_PAD + (actions.length - 1 - i) * (BTN_H + BTN_GAP);
            if (fx >= bx && fx <= bx + BTN_W && fy >= by && fy <= by + BTN_H) {
                hoveredActionIndex = i;
                break;
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Render
    // ──────────────────────────────────────────────────────────────

    public void render(Player player, float delta) {
        feedbackTimer = Math.max(0f, feedbackTimer - delta);

        int sw = viewport.getScreenWidth();
        int sh = viewport.getScreenHeight();
        Matrix4 screen = new Matrix4().setToOrtho2D(0, 0, sw, sh);

        StatComponent stats     = player.getStats();
        float maxMovement       = player.getMovementController().getMaxMovementDistance();
        float remainingMovement = player.getMovementController().getRemainingMovementDistance();
        ActionType[] actions    = ActionType.availableFor(player.getCharacterClass());

        float hpRatio      = (float) stats.getHp()   / stats.getMaxHp();
        float manaRatio    = (float) stats.getMana()  / stats.getMaxMana();
        float staminaRatio = remainingMovement / maxMovement;

        float toggleX = sw - TOGGLE_W - TOGGLE_PAD;
        float toggleY = TOGGLE_PAD;

        float panelW = BTN_W + PANEL_PAD * 2;
        float panelH = PANEL_PAD + actions.length * (BTN_H + BTN_GAP);
        float panelX = sw - panelW - TOGGLE_PAD;
        float panelY = toggleY + TOGGLE_H + 4f;

        shapeRenderer.setProjectionMatrix(screen);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // stat bars
        drawBar(HP_Y,      hpRatio,      hpRatio < 0.3f ? HP_LOW : HP_FILL);
        drawBar(MANA_Y,    manaRatio,    MANA_FILL);
        drawBar(STAMINA_Y, staminaRatio, STAMINA_FILL);

        // action panel background
        if (panelOpen) {
            shapeRenderer.setColor(PANEL_BG);
            shapeRenderer.rect(panelX, panelY, panelW, panelH);

            // action buttons
            for (int i = 0; i < actions.length; i++) {
                float bx = panelX + PANEL_PAD;
                float by = panelY + PANEL_PAD + (actions.length - 1 - i) * (BTN_H + BTN_GAP);
                boolean canAfford = stats.getMana() >= actions[i].manaCost;
                boolean hovered   = (i == hoveredActionIndex);

                if (!canAfford)     shapeRenderer.setColor(BTN_NOMANA);
                else if (hovered)   shapeRenderer.setColor(BTN_HOVER);
                else                shapeRenderer.setColor(BTN_BG);

                shapeRenderer.rect(bx, by, BTN_W, BTN_H);
            }
        }

        // toggle button
        boolean toggleHovered = isToggleHovered(sw, sh);
        shapeRenderer.setColor(toggleHovered ? TOGGLE_HOVER : TOGGLE_BG);
        shapeRenderer.rect(toggleX, toggleY, TOGGLE_W, TOGGLE_H);

        shapeRenderer.end();

        // borders
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        if (panelOpen) {
            shapeRenderer.setColor(PANEL_BORDER);
            shapeRenderer.rect(panelX, panelY, panelW, panelH);

            for (int i = 0; i < actions.length; i++) {
                float bx = panelX + PANEL_PAD;
                float by = panelY + PANEL_PAD + (actions.length - 1 - i) * (BTN_H + BTN_GAP);
                shapeRenderer.setColor(BTN_BORDER);
                shapeRenderer.rect(bx, by, BTN_W, BTN_H);
            }
        }

        shapeRenderer.setColor(TOGGLE_BORDER);
        shapeRenderer.rect(toggleX, toggleY, TOGGLE_W, TOGGLE_H);

        shapeRenderer.end();

        // text
        hudBatch.setProjectionMatrix(screen);
        hudBatch.begin();

        // stat labels
        font.setColor(TEXT_COLOR);
        font.draw(hudBatch, "HP:      " + stats.getHp() + " / " + stats.getMaxHp(),
            BAR_X, HP_Y + LABEL_OFFSET);
        font.draw(hudBatch, "Mana:  " + stats.getMana() + " / " + stats.getMaxMana(),
            BAR_X, MANA_Y + LABEL_OFFSET);
        font.draw(hudBatch,
            "Stamina: " + String.format("%.2f", remainingMovement) + "/" + String.format("%.2f", maxMovement),
            BAR_X, 60);

        font.draw(hudBatch, "[E] End Turn", BAR_X, 18f);

        // toggle button label
        font.setColor(TEXT_COLOR);
        String toggleLabel = panelOpen ? "X  Close" : "\u2694 Actions";
        font.draw(hudBatch, toggleLabel, toggleX + 8f, toggleY + TOGGLE_H - 10f);

        // action button labels
        if (panelOpen) {
            for (int i = 0; i < actions.length; i++) {
                float bx = panelX + PANEL_PAD;
                float by = panelY + PANEL_PAD + (actions.length - 1 - i) * (BTN_H + BTN_GAP);
                ActionType action = actions[i];
                boolean canAfford = stats.getMana() >= action.manaCost;

                font.setColor(canAfford ? TEXT_COLOR : new Color(0.55f, 0.55f, 0.6f, 1f));
                font.draw(hudBatch, action.displayName, bx + 8f, by + BTN_H - 10f);

                font.setColor(canAfford ? MANA_TEXT : new Color(0.4f, 0.4f, 0.5f, 1f));
                String cost = action.manaCost > 0 ? action.manaCost + " MP" : "Free";
                font.draw(hudBatch, cost, bx + 8f, by + 14f);
            }
        }

        // feedback message
        if (feedbackTimer > 0f) {
            font.setColor(FEEDBACK_COLOR.r, FEEDBACK_COLOR.g, FEEDBACK_COLOR.b,
                Math.min(1f, feedbackTimer));
            font.draw(hudBatch, feedbackMessage,
                (sw - feedbackMessage.length() * 7f) / 2f, sh - 30f);
        }

        font.setColor(TEXT_COLOR);
        hudBatch.end();
    }

    // ──────────────────────────────────────────────────────────────
    //  Helpers
    // ──────────────────────────────────────────────────────────────

    private void drawBar(float y, float ratio, Color fill) {
        shapeRenderer.setColor(BAR_BG);
        shapeRenderer.rect(BAR_X, y, BAR_W, BAR_H);
        shapeRenderer.setColor(fill);
        shapeRenderer.rect(BAR_X, y, BAR_W * Math.max(0f, ratio), BAR_H);
    }

    private boolean isToggleHovered(int sw, int sh) {
        float mx = com.badlogic.gdx.Gdx.input.getX() - viewport.getScreenX();
        float my = sh - (com.badlogic.gdx.Gdx.input.getY() - viewport.getScreenY());
        float tx = sw - TOGGLE_W - TOGGLE_PAD;
        float ty = TOGGLE_PAD;
        return mx >= tx && mx <= tx + TOGGLE_W && my >= ty && my <= ty + TOGGLE_H;
    }

    @Override
    public void dispose() {
        hudBatch.dispose();
        shapeRenderer.dispose();
        font.dispose();
    }
}
