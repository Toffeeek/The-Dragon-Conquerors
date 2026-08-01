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
import com.shared.shared.model.stats.StatComponent;
import lombok.Setter;

public class HudRenderer implements Disposable {

    private final SpriteBatch   hudBatch;
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont    font;
    private final Viewport      viewport;

    // ── colours ───────────────────────────────────────────────────
    private static final Color BAR_BG          = new Color(0.2f,  0.2f,  0.2f,  0.85f);
    private static final Color HP_FILL         = new Color(0.85f, 0.15f, 0.15f, 1f);
    private static final Color HP_LOW          = new Color(1.0f,  0.35f, 0.05f, 1f);
    private static final Color MANA_FILL       = new Color(0.15f, 0.40f, 0.95f, 1f);
    private static final Color STAMINA_FILL    = new Color(0.2f,  0.8f,  0.3f,  1f);
    private static final Color TEXT_COLOR      = new Color(0.9f,  0.9f,  0.9f,  1f);

    // action bar
    private static final Color ACTION_BG       = new Color(0.1f,  0.1f,  0.1f,  0.9f);
    private static final Color ACTION_BORDER   = new Color(0.5f,  0.5f,  0.5f,  1f);
    private static final Color ACTION_SELECTED = new Color(0.9f,  0.75f, 0.1f,  1f);
    private static final Color ACTION_NOMANA   = new Color(0.4f,  0.4f,  0.55f, 0.9f);
    private static final Color FEEDBACK_COLOR  = new Color(1f,    0.9f,  0.3f,  1f);

    // ── stat bar layout (bottom-left) ─────────────────────────────
    private static final float BAR_W        = 200f;
    private static final float BAR_H        = 14f;
    private static final float BAR_X        = 16f;
    private static final float GAP          = 22f;
    private static final float STAMINA_Y    = 36f;
    private static final float MANA_Y       = STAMINA_Y + GAP;
    private static final float HP_Y         = MANA_Y    + GAP;
    private static final float LABEL_OFFSET = BAR_H + 3f;

    // ── action bar layout (bottom-centre) ─────────────────────────
    private static final float SLOT_W  = 110f;
    private static final float SLOT_H  = 48f;
    private static final float SLOT_GAP = 6f;
    private static final float SLOT_Y  = 10f;   // distance from bottom of screen

    // ── feedback message ──────────────────────────────────────────
    private String  feedbackMessage   = "";
    private float   feedbackTimer     = 0f;
    private static final float FEEDBACK_DURATION = 2.5f;

    // ── action selection ──────────────────────────────────────────
    @Setter
    private int selectedActionIndex = 0;

    public HudRenderer(Viewport viewport) {
        this.viewport      = viewport;
        this.hudBatch      = new SpriteBatch();
        this.shapeRenderer = new ShapeRenderer();
        this.font          = new BitmapFont();
        this.font.setColor(TEXT_COLOR);
    }

    // ──────────────────────────────────────────────────────────────
    //  Public API
    // ──────────────────────────────────────────────────────────────

    /** Call this after an action executes to show a result message on screen. */
    public void showFeedback(ActionResult result) {
        this.feedbackMessage = result.message;
        this.feedbackTimer   = FEEDBACK_DURATION;
    }

    public void showFeedback(String message) {
        this.feedbackMessage = message;
        this.feedbackTimer   = FEEDBACK_DURATION;
    }

    // ──────────────────────────────────────────────────────────────
    //  Render
    // ──────────────────────────────────────────────────────────────

    public void render(Player player, float delta) {
        feedbackTimer = Math.max(0f, feedbackTimer - delta);

        Matrix4 screenMatrix = new Matrix4().setToOrtho2D(
            0, 0, viewport.getScreenWidth(), viewport.getScreenHeight());
        int sw = viewport.getScreenWidth();
        int sh = viewport.getScreenHeight();

        StatComponent stats         = player.getStats();
        float maxMovement           = player.getMovementController().getMaxMovementDistance();
        float remainingMovement     = player.getMovementController().getRemainingMovementDistance();
        ActionType[] actions        = ActionType.availableFor(player.getCharacterClass());

        float hpRatio               = (float) stats.getHp()   / stats.getMaxHp();
        float manaRatio             = (float) stats.getMana()  / stats.getMaxMana();
        float staminaRatio          = remainingMovement / maxMovement;

        // total action bar width, centred on screen
        float totalBarW = actions.length * SLOT_W + (actions.length - 1) * SLOT_GAP;
        float barStartX = (sw - totalBarW) / 2f;

        shapeRenderer.setProjectionMatrix(screenMatrix);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // ── stat bars ─────────────────────────────────────────────
        drawBar(HP_Y,      hpRatio,      hpRatio < 0.3f ? HP_LOW : HP_FILL);
        drawBar(MANA_Y,    manaRatio,    MANA_FILL);
        drawBar(STAMINA_Y, staminaRatio, STAMINA_FILL);

        // ── action slots background ───────────────────────────────
        for (int i = 0; i < actions.length; i++) {
            float sx = barStartX + i * (SLOT_W + SLOT_GAP);
            boolean canAfford = stats.getMana() >= actions[i].manaCost;

            shapeRenderer.setColor(canAfford ? ACTION_BG : ACTION_NOMANA);
            shapeRenderer.rect(sx, SLOT_Y, SLOT_W, SLOT_H);
        }

        shapeRenderer.end();

        // ── action slot borders (Line mode) ───────────────────────
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < actions.length; i++) {
            float sx = barStartX + i * (SLOT_W + SLOT_GAP);
            boolean selected = (i == selectedActionIndex);
            shapeRenderer.setColor(selected ? ACTION_SELECTED : ACTION_BORDER);
            shapeRenderer.rect(sx, SLOT_Y, SLOT_W, SLOT_H);
            if (selected) {
                // double border for selected slot
                shapeRenderer.rect(sx + 1, SLOT_Y + 1, SLOT_W - 2, SLOT_H - 2);
            }
        }
        shapeRenderer.end();

        // ── text labels ───────────────────────────────────────────
        hudBatch.setProjectionMatrix(screenMatrix);
        hudBatch.begin();

        // stat bar labels
        font.draw(hudBatch,
            "HP:      " + stats.getHp() + " / " + stats.getMaxHp(),
            BAR_X, HP_Y + LABEL_OFFSET);
        font.draw(hudBatch,
            "Mana:  "  + stats.getMana() + " / " + stats.getMaxMana(),
            BAR_X, MANA_Y + LABEL_OFFSET);
        font.draw(hudBatch,
            "Stamina: " + String.format("%.2f", remainingMovement) + "/" + String.format("%.2f", maxMovement),
            BAR_X, 60);

        // end turn hint (top-right)
        font.draw(hudBatch, "[E] End Turn", sw - 120f, 24f);

        // action slot labels
        for (int i = 0; i < actions.length; i++) {
            float sx = barStartX + i * (SLOT_W + SLOT_GAP);
            ActionType action = actions[i];

            // hotkey
            font.setColor(ACTION_SELECTED);
            font.draw(hudBatch, "[" + (i + 1) + "]", sx + 4f, SLOT_Y + SLOT_H - 4f);

            // action name (wrap at ~14 chars)
            font.setColor(TEXT_COLOR);
            String name = action.displayName;
            if (name.length() > 13) {
                // split at space closest to middle
                int mid = name.lastIndexOf(' ', 13);
                if (mid < 0) mid = 13;
                font.draw(hudBatch, name.substring(0, mid),  sx + 4f, SLOT_Y + SLOT_H - 18f);
                font.draw(hudBatch, name.substring(mid + 1), sx + 4f, SLOT_Y + SLOT_H - 30f);
            } else {
                font.draw(hudBatch, name, sx + 4f, SLOT_Y + SLOT_H - 18f);
            }

            // mana cost
            font.setColor(MANA_FILL);
            String cost = action.manaCost > 0 ? action.manaCost + " MP" : "Free";
            font.draw(hudBatch, cost, sx + 4f, SLOT_Y + 14f);
        }

        // feedback message (fades out)
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

    @Override
    public void dispose() {
        hudBatch.dispose();
        shapeRenderer.dispose();
        font.dispose();
    }
}
