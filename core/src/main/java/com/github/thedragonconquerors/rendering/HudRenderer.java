// File Location: core/src/main/java/com/github/thedragonconquerors/rendering/HudRenderer.java
package com.github.thedragonconquerors.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.github.thedragonconquerors.combat.ActionResult;
import com.github.thedragonconquerors.entities.Player;
import com.shared.shared.model.ability.AbilityType;
import com.shared.shared.model.stats.StatComponent;
import com.shared.shared.model.world.Environment;

import java.util.List;

public class HudRenderer implements Disposable {
    public static final float VIRTUAL_WIDTH = 1280f;
    public static final float BOTTOM_HEIGHT = 112f;
    public static final float TOP_HEIGHT = 58f;

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
    private static final float SLOT_H  = 78f;
    private static final float SLOT_GAP = 6f;
    private static final float SLOT_Y  = 10f;   // distance from bottom of screen

    // ── feedback message ──────────────────────────────────────────
    private String  feedbackMessage   = "";
    private float   feedbackTimer     = 0f;
    private static final float FEEDBACK_DURATION = 2.5f;

    // Persistent while a targeted action is waiting for a mouse click.
    private String targetingPrompt = "";
    private String joinUrl = "";
    private String environmentName = "";
    private String environmentRule = "";

    // ── action selection ──────────────────────────────────────────
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
        showFeedback(result.message);
    }

    public void showFeedback(String message) {
        // The built-in bitmap font has no Unicode arrows/dashes; avoid missing-glyph squares.
        this.feedbackMessage = message == null ? "" : message.replace("\u2192", "->")
            .replace('\u2014', '-').replace('\u2013', '-');
        this.feedbackTimer   = FEEDBACK_DURATION;
    }

    public void setSelectedActionIndex(int selectedActionIndex) {
        this.selectedActionIndex = selectedActionIndex;
    }

    public void showTargetingPrompt(AbilityType action) {
        this.targetingPrompt = "Select a target for " + action.getDisplayName()
            + " - green: in range, red: out of range  [ESC to cancel]";
    }

    public void clearTargetingPrompt() {
        this.targetingPrompt = "";
    }

    public void setJoinUrl(String joinUrl) {
        this.joinUrl = joinUrl == null ? "" : joinUrl;
    }

    public void setEnvironment(Environment environment) {
        this.environmentName = environment == null ? "" : environment.getDisplayName();
        this.environmentRule = environment == null ? "" : environment.hazardSummary().replace('\u2014', '-');
    }

    // ──────────────────────────────────────────────────────────────
    //  Render
    // ──────────────────────────────────────────────────────────────

    public void render(Player player, float delta) {
        feedbackTimer = Math.max(0f, feedbackTimer - delta);

        int sw = (int)VIRTUAL_WIDTH;
        int sh = Math.round(VIRTUAL_WIDTH * com.badlogic.gdx.Gdx.graphics.getHeight() / com.badlogic.gdx.Gdx.graphics.getWidth());
        com.badlogic.gdx.Gdx.gl.glViewport(0, 0, com.badlogic.gdx.Gdx.graphics.getWidth(), com.badlogic.gdx.Gdx.graphics.getHeight());
        Matrix4 screenMatrix = new Matrix4().setToOrtho2D(0, 0, sw, sh);

        StatComponent stats         = player.getStats();
        float maxMovement           = player.getMovementController().getMaxMovementDistance();
        float remainingMovement     = player.getMovementController().getRemainingMovementDistance();
        List<AbilityType> actions   = AbilityType.forClass(player.getCharacterClass());

        float hpRatio               = (float) stats.getHp()   / stats.getMaxHp();
        float manaRatio             = (float) stats.getMana()  / stats.getMaxMana();
        float staminaRatio          = maxMovement <= 0f ? 0f : remainingMovement / maxMovement;

        // total action bar width, centred on screen
        float totalBarW = actions.size() * SLOT_W + (actions.size() - 1) * SLOT_GAP;
        float barStartX = (sw - totalBarW) / 2f;

        shapeRenderer.setProjectionMatrix(screenMatrix);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.045f, 0.06f, 0.08f, 1f);
        shapeRenderer.rect(0, 0, sw, BOTTOM_HEIGHT);
        shapeRenderer.rect(0, sh - TOP_HEIGHT, sw, TOP_HEIGHT);

        // ── stat bars ─────────────────────────────────────────────
        drawBar(HP_Y,      hpRatio,      hpRatio < 0.3f ? HP_LOW : HP_FILL);
        drawBar(MANA_Y,    manaRatio,    MANA_FILL);
        drawBar(STAMINA_Y, staminaRatio, STAMINA_FILL);

        // ── action slots background ───────────────────────────────
        for (int i = 0; i < actions.size(); i++) {
            float sx = barStartX + i * (SLOT_W + SLOT_GAP);
            AbilityType ability = actions.get(i);
            boolean canAfford = stats.getMana() >= ability.getManaCost()
                && player.cooldownTurns(ability) == 0
                && player.isActiveTurn() && !player.isActionUsed();

            shapeRenderer.setColor(canAfford ? ACTION_BG : ACTION_NOMANA);
            shapeRenderer.rect(sx, SLOT_Y, SLOT_W, SLOT_H);
        }

        shapeRenderer.end();

        // ── action slot borders (Line mode) ───────────────────────
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < actions.size(); i++) {
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
            BAR_X, HP_Y + BAR_H - 1f);
        font.draw(hudBatch,
            "Mana:  "  + stats.getMana() + " / " + stats.getMaxMana(),
            BAR_X, MANA_Y + BAR_H - 1f);
        font.draw(hudBatch,
            "Movement: " + String.format("%.2f", remainingMovement) + " / " + String.format("%.2f", maxMovement),
            BAR_X, STAMINA_Y + BAR_H - 1f);
        font.setColor(ACTION_SELECTED);
        font.draw(hudBatch, "Action points: " + player.getActionPoints() + " / 1", BAR_X, 22f);
        font.setColor(TEXT_COLOR);

        // end turn hint (top-right)
        font.draw(hudBatch, "[E] End Turn", sw - 180f, 42f);
        font.draw(hudBatch, "1-4: Ability | Click: Move", sw - 200f, 22f);

        if (!joinUrl.isEmpty()) {
            font.draw(hudBatch, "Join: " + joinUrl, 16f, sh - 14f);
        }
        if (!environmentName.isEmpty()) {
            font.draw(hudBatch, "Battlefield: " + environmentName, sw - 180f, sh - 14f);
            font.draw(hudBatch, environmentRule, sw - 340f, sh - 32f);
        }

        // action slot labels
        for (int i = 0; i < actions.size(); i++) {
            float sx = barStartX + i * (SLOT_W + SLOT_GAP);
            AbilityType action = actions.get(i);

            // hotkey
            font.setColor(ACTION_SELECTED);
            font.draw(hudBatch, "[" + (i + 1) + "]", sx + 4f, SLOT_Y + SLOT_H - 4f);

            // action name (wrap at ~14 chars)
            font.setColor(TEXT_COLOR);
            String name = action.getDisplayName();
            if (name.length() > 13) {
                // split at space closest to middle
                int mid = name.lastIndexOf(' ', 13);
                if (mid < 0) mid = 13;
                font.draw(hudBatch, name.substring(0, mid),  sx + 4f, SLOT_Y + SLOT_H - 18f);
                font.draw(hudBatch, name.substring(mid + 1), sx + 4f, SLOT_Y + SLOT_H - 34f);
            } else {
                font.draw(hudBatch, name, sx + 4f, SLOT_Y + SLOT_H - 18f);
            }

            // mana cost
            font.setColor(MANA_FILL);
            int cooldown = player.cooldownTurns(action);
            String cost = cooldown > 0 ? "CD " + cooldown
                : "1 AP" + (action.getManaCost() > 0 ? " + " + action.getManaCost() + " Mana" : "");
            font.draw(hudBatch, cost, sx + 4f, SLOT_Y + 14f);
        }

        font.setColor(player.isActiveTurn() ? ACTION_SELECTED : TEXT_COLOR);
        font.draw(hudBatch, player.isActiveTurn() ? "YOUR TURN" : "WAITING FOR TURN",
            sw - 180f, 70f);

        // target-selection prompt remains visible until a player is clicked or ESC is pressed
        if (!targetingPrompt.isEmpty()) {
            font.setColor(ACTION_SELECTED);
            font.draw(hudBatch, targetingPrompt,
                Math.max(20f, (sw - targetingPrompt.length() * 7f) / 2f), BOTTOM_HEIGHT + 20f);
        }

        // feedback message (fades out)
        if (feedbackTimer > 0f) {
            font.setColor(FEEDBACK_COLOR.r, FEEDBACK_COLOR.g, FEEDBACK_COLOR.b,
                Math.min(1f, feedbackTimer));
            float feedbackY = BOTTOM_HEIGHT - 5f;
            font.draw(hudBatch, feedbackMessage,
                Math.max(20f, (sw - feedbackMessage.length() * 7f) / 2f), feedbackY);
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
