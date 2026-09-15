package com.github.thedragonconquerors.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.github.thedragonconquerors.FantasyUiTheme;
import com.github.thedragonconquerors.entities.Player;
import com.github.thedragonconquerors.input.BattleInteraction;
import com.shared.shared.model.ability.AbilityType;
import com.shared.shared.network.MatchState;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Mouse-driven overlay; no space is reserved outside the battlefield. */
public final class HudRenderer implements Disposable {
    // Keep the original overlay layout, uniformly reduced by 40%.
    private static final float HUD_SCALE = 0.60f;
    private final FantasyUiTheme theme = new FantasyUiTheme();
    private final Skin skin = theme.skin();
    private final ScreenViewport viewport = new ScreenViewport();
    private final Stage stage = new Stage(viewport);
    private final BattleInteraction interaction;
    private final Label hpText, manaText, staminaText, apText, turnText, roundText, hint;
    private final ProgressBar hp, mana, stamina;
    private final TextButton move, action, end, cancel;
    private final Table abilityHost = new Table(), abilityList = new Table(), logRows = new Table();
    private final ScrollPane logScroll;
    private final List<AbilityType> abilities;
    private final List<TextButton> abilityButtons = new ArrayList<>();
    private final List<String> history = new ArrayList<>();
    private boolean listVisible;
    private String targetingPrompt = "", feedback = "", activeName = "Waiting for players";
    private float feedbackTimer;
    private int round;

    public HudRenderer(List<AbilityType> abilities, BattleInteraction interaction,
                       Runnable onMove, Runnable onAction, Consumer<AbilityType> onAbility,
                       Runnable onEnd, Runnable onCancel) {
        this.abilities = abilities;
        this.interaction = interaction;
        Table resources = panel();
        resources.add(new Label("YOUR RESOURCES", skin, "section")).left().padBottom(8).row();
        hp = bar(new Color(0.83f, 0.20f, 0.24f, 1));
        mana = bar(new Color(0.20f, 0.46f, 0.92f, 1));
        stamina = bar(new Color(0.22f, 0.68f, 0.42f, 1));
        hpText = addBar(resources, hp);
        manaText = addBar(resources, mana);
        staminaText = addBar(resources, stamina);
        apText = new Label("", skin, "section");
        resources.add(apText).left().padTop(4);
        anchor(Align.topLeft).add(resources).width(266);

        Table turn = panel();
        turnText = new Label("", skin, "heading");
        turnText.setAlignment(Align.center);
        turnText.setEllipsis(true);
        roundText = new Label("", skin, "caption");
        roundText.setEllipsis(true);
        turn.add(turnText).width(330).row();
        turn.add(roundText).width(330).padTop(3);
        anchor(Align.topRight).add(turn).width(362);

        Table controls = panel();
        hint = new Label("Choose Move or Action to begin.", skin, "status");
        hint.setWrap(true);
        controls.add(hint).width(400).left().padBottom(8).row();
        for (AbilityType ability : abilities) {
            TextButton button = button(ability.getDisplayName(), "quiet", () -> onAbility.accept(ability));
            button.getLabel().setAlignment(Align.left);
            button.padLeft(12);
            abilityButtons.add(button);
            abilityList.add(button).width(400).height(38).padBottom(4).row();
        }
        controls.add(abilityHost).growX().row();
        Table commands = new Table();
        move = button("MOVE", "secondary", onMove);
        action = button("ACTION", "secondary", onAction);
        end = button("END TURN", "primary", onEnd);
        cancel = button("CANCEL", "quiet", onCancel);
        commands.add(move).width(95).height(40).padRight(5);
        commands.add(action).width(95).height(40).padRight(5);
        commands.add(end).width(115).height(40).padRight(5);
        commands.add(cancel).width(80).height(40);
        controls.add(commands);
        anchor(Align.bottomLeft).add(controls).width(428);

        Table log = panel();
        log.add(new Label("BATTLE LOG", skin, "section")).left().padBottom(8).row();
        logRows.top().left();
        ScrollPane.ScrollPaneStyle scrollStyle = new ScrollPane.ScrollPaneStyle();
        scrollStyle.vScroll = ((TextureRegionDrawable) theme.divider()).tint(FantasyUiTheme.GOLD_DIM);
        scrollStyle.vScrollKnob = theme.divider();
        logScroll = new ScrollPane(logRows, scrollStyle);
        logScroll.setScrollingDisabled(true, false);
        logScroll.setOverscroll(false, false);
        logScroll.setFadeScrollBars(false);
        log.add(logScroll).width(324).height(145);
        anchor(Align.bottomRight).add(log).width(352);
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public Stage stage() { return stage; }
    private Table anchor(int alignment) {
        Table root = new Table();
        root.setFillParent(true);
        root.align(alignment).pad(16);
        root.setTouchable(Touchable.childrenOnly);
        stage.addActor(root);
        return root;
    }
    private Table panel() {
        Table table = new Table();
        table.setBackground(theme.panel());
        table.pad(14);
        // Empty panel space consumes input too: never move through the HUD.
        table.setTouchable(Touchable.enabled);
        table.addListener(new InputListener() {
            @Override public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                return true;
            }
        });
        return table;
    }
    private ProgressBar bar(Color color) {
        ProgressBar.ProgressBarStyle style = new ProgressBar.ProgressBarStyle();
        Drawable background = theme.solid(new Color(0.16f, 0.18f, 0.21f, 1));
        Drawable fill = theme.solid(color);
        background.setMinHeight(23);
        fill.setMinHeight(23);
        style.background = background;
        style.knobBefore = fill;
        ProgressBar result = new ProgressBar(0, 1, 0.001f, false, style);
        result.setTouchable(Touchable.disabled);
        return result;
    }
    private Label addBar(Table table, ProgressBar bar) {
        Label label = new Label("", skin, "status");
        label.setAlignment(Align.center);
        table.add(new Stack(bar, label)).width(238).height(23).padBottom(5).row();
        return label;
    }
    private TextButton button(String text, String style, Runnable callback) {
        TextButton button = new TextButton(text, skin, style);
        button.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (!button.isDisabled()) callback.run();
            }
        });
        return button;
    }
    public void showFeedback(String message) { feedback = clean(message); feedbackTimer = 3f; }
    public void showTargetingPrompt(AbilityType ability) {
        targetingPrompt = ability.getDisplayName() + ": "
            + (ability.getTargetType().targetsGround() ? "click a destination." : "click a highlighted player.");
        feedbackTimer = 0;
    }
    public void clearTargetingPrompt() { targetingPrompt = ""; feedbackTimer = 0; }
    public void recordState(MatchState state) {
        round = state.getRoundNumber();
        activeName = state.getPlayers().stream().filter(p -> p.getId() == state.getActivePlayerId())
            .map(p -> p.getUsername() == null || p.getUsername().isBlank()
                ? "Player " + (p.getId() + 1) : p.getUsername()).findFirst().orElse("Waiting for players");
        // Only accepted server results enter the history, never targeting attempts/errors.
        String message = clean(state.getMessage());
        if (!message.isBlank()) {
            history.add("R" + round + "  " + message);
            if (history.size() > 100) history.remove(0);
            logRows.clearChildren();
            for (String line : history) {
                Label entry = new Label(line, skin, "status");
                entry.setWrap(true);
                logRows.add(entry).width(312).left().padBottom(8).row();
            }
            for (Actor actor : stage.getActors()) if (actor instanceof Table table) table.validate();
            logScroll.validate();
            logScroll.setScrollPercentY(1);
            logScroll.updateVisualScroll();
        }
    }
    public void render(Player player, float delta, boolean moving) {
        feedbackTimer = Math.max(0, feedbackTimer - delta);
        float maximum = player.getMovementController().getMaxMovementDistance();
        float remaining = player.getMovementController().getRemainingMovementDistance();
        hp.setValue(ratio(player.getStats().getHp(), player.getStats().getMaxHp()));
        mana.setValue(ratio(player.getStats().getMana(), player.getStats().getMaxMana()));
        stamina.setValue(ratio(remaining, maximum));
        hpText.setText("HP  " + player.getStats().getHp() + " / " + player.getStats().getMaxHp());
        manaText.setText("Mana  " + player.getStats().getMana() + " / " + player.getStats().getMaxMana());
        staminaText.setText(String.format("Stamina  %.1f / %.1f", remaining, maximum));
        apText.setText("Action point: " + player.getActionPoints() + " / 1");
        turnText.setText(player.isActiveTurn() ? "YOUR TURN" : clean(activeName) + "'s turn");
        turnText.setColor(player.isActiveTurn() ? FantasyUiTheme.GOLD : FantasyUiTheme.TEXT_PRIMARY);
        roundText.setText("ROUND " + round + "  |  " + clean(activeName));
        move.setDisabled(!interaction.canMove(player, moving));
        action.setDisabled(!interaction.canAct(player, moving));
        end.setDisabled(!interaction.canControl(player, moving));
        cancel.setDisabled(interaction.mode() == BattleInteraction.Mode.NONE && targetingPrompt.isEmpty());
        move.setColor(interaction.mode() == BattleInteraction.Mode.MOVE ? new Color(0.6f, 0.82f, 1, 1) : Color.WHITE);
        action.setColor(interaction.mode() == BattleInteraction.Mode.ACTION ? FantasyUiTheme.GOLD : Color.WHITE);
        boolean visible = interaction.mode() == BattleInteraction.Mode.ACTION && targetingPrompt.isEmpty();
        if (visible != listVisible) {
            abilityHost.clearChildren();
            if (visible) abilityHost.add(abilityList).growX().padBottom(5);
            listVisible = visible;
        }
        for (int i = 0; i < abilities.size(); i++) {
            AbilityType ability = abilities.get(i);
            TextButton button = abilityButtons.get(i);
            button.setDisabled(!interaction.canUse(player, moving, ability));
            int cooldown = player.cooldownTurns(ability);
            button.setText(ability.getDisplayName() + "   |   " + (cooldown > 0 ? "Cooldown: " + cooldown
                : "1 AP" + (ability.getManaCost() > 0 ? " + " + ability.getManaCost() + " mana" : "")));
        }
        hint.setText(feedbackTimer > 0 ? feedback : !targetingPrompt.isEmpty() ? targetingPrompt
            : interaction.awaitingServer() ? "Waiting for the server..."
            : !player.isActiveTurn() ? "Waiting for " + clean(activeName) + "."
            : moving ? "Moving..."
            : interaction.mode() == BattleInteraction.Mode.MOVE ? "Click a blue area to move."
            : visible ? "Choose an ability, then select its target."
            : "Choose Move or Action to begin.");
        viewport.apply();
        stage.act(Math.min(delta, 1f / 15f));
        stage.draw();
    }
    private static float ratio(float value, float max) { return max <= 0 ? 0 : Math.max(0, Math.min(1, value / max)); }
    private static String clean(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ')
            .replace("\u2192", "->").replace('\u2014', '-').replace('\u2013', '-');
    }
    public void resize(int width, int height) {
        viewport.setUnitsPerPixel(Math.max(1280f / Math.max(1, width), 720f / Math.max(1, height)) / HUD_SCALE);
        viewport.update(width, height, true);
    }
    @Override public void dispose() { stage.dispose(); theme.dispose(); }
}
