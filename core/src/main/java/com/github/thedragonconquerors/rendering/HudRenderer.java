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
import com.github.thedragonconquerors.ui.*;

/** Mouse-driven overlay; no space is reserved outside the battlefield. */
public final class HudRenderer implements Disposable {
    // Keep the original overlay layout, uniformly reduced by 40%.
    private final com.badlogic.gdx.Preferences preferences = Gdx.app.getPreferences("tdc-ui");
    private float hudScale = Math.max(.6f, Math.min(.9f, preferences.getFloat("scale", .6f)));
    private boolean logCollapsed = preferences.getBoolean("logCollapsed", false);
    private final Label previewText, statusText;
    private AbilityType hoveredAbility;
    private String lastLogMessage = "";
    private final FantasyUiTheme theme = new FantasyUiTheme();
    private final Skin skin = theme.skin();
    private final ScreenViewport viewport = new ScreenViewport();
    private final Stage stage = new Stage(viewport);
    private final BattleInteraction interaction;
    private final Label hpText, manaText, staminaText, apText, roundText, hint;
    private final ProgressBar hp, mana, stamina;
    private final TextButton move, action, end, cancel;
    private final Table abilityHost = new Table(), abilityList = new Table(), logRows = new Table();
    private final ScrollPane logScroll;
    private final List<AbilityType> abilities;
    private final List<TextButton> abilityButtons = new ArrayList<>();
    private final java.util.ArrayDeque<Label> history = new java.util.ArrayDeque<>();
    private final com.badlogic.gdx.math.Vector2 pointer = new com.badlogic.gdx.math.Vector2();
    private static final Color MOVE_SELECTED = new Color(.6f, .82f, 1f, 1f);
    private int lastMovementTenths = -1, lastMaximumTenths = -1;
    private record PortraitState(int id, String name, com.shared.shared.model.CharacterClass type,
        int team, boolean active, boolean down, boolean connected, boolean stunned, boolean next) {}
    private List<PortraitState> displayedPortraits = List.of();
    private boolean listVisible;
    private String targetingPrompt = "", feedback = "", activeName = "Waiting for players";
    private float feedbackTimer;
    private int round;
    private final PixelIcons icons = new PixelIcons();
    private final com.github.thedragonconquerors.assets.AssetService assets;
    private final Table turnStrip = new Table(), statusIcons = new Table(), guidePanel = new Table();
    private final Label guideText;
    private final TextButton guideNext;
    private final Table commands = new Table();
    private final List<Cell<Stack>> resourceBars = new ArrayList<>();
    private final List<Label> orderLabels = new ArrayList<>();
    private final List<TurnPortraitCard> portraitCards = new ArrayList<>();
    private final Drawable portraitPixel = theme.solid(Color.WHITE);
    private record PortraitOwner(int id) {}
    private int guideStep;
    private boolean guideOpen;
    private String statusSignature = "";

    public HudRenderer(List<AbilityType> abilities, BattleInteraction interaction,
                       Runnable onMove, Runnable onAction, Consumer<AbilityType> onAbility,
                       Runnable onEnd, Runnable onCancel,
                       com.github.thedragonconquerors.assets.AssetService assets, Runnable onMenu) {
        this.assets = assets;
        this.abilities = abilities;
        this.interaction = interaction;
        Table resources = panel();
        hp = bar(new Color(0.18f, 0.86f, 0.35f, 1));
        mana = bar(new Color(0.20f, 0.46f, 0.92f, 1));
        stamina = bar(new Color(0.89f, 0.67f, 0.27f, 1));
        hpText = addBar(resources, hp,PixelIcons.Kind.HEART,"Health");
        manaText = addBar(resources, mana,PixelIcons.Kind.STAR,"Mana");
        staminaText = addBar(resources, stamina,PixelIcons.Kind.BOLT,"Movement");
        apText = new Label("", skin, "section");
        resources.add(apText).left().padTop(4).row();
        statusText = new Label("", skin, "caption"); statusText.setWrap(true);
        resources.add(statusText).width(238).left().row();
        resources.add(statusIcons).left().row();
        TextButton scale = button("UI: " + Math.round(hudScale * 100) + "%", "quiet", () -> {});
        scale.addListener(new ClickListener() { @Override public void clicked(InputEvent event, float x, float y) {
            hudScale = hudScale < .7f ? .75f : hudScale < .85f ? .9f : .6f;
            preferences.putFloat("scale", hudScale).flush();
            scale.setText("UI: " + Math.round(hudScale * 100) + "%");
            resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }});
        Table utility = new Table();utility.add(scale).height(28).padRight(8);
        utility.add(button("MENU", "quiet", onMenu)).height(28);
        resources.add(utility).left().padTop(3);
        anchor(Align.topLeft).add(resources).width(266);

        Table turn = panel();
        roundText = new Label("", skin, "caption");
        roundText.setEllipsis(true);
        turn.add(roundText).width(330).padTop(3).row();
        turn.add(turnStrip).padTop(14);
        anchor(Align.topRight).add(turn).width(362);

        Table controls = panel();
        hint = new Label("", skin, "status");
        hint.setWrap(true);
        controls.add(hint).width(400).left().padBottom(8).row();
        previewText = new Label("", skin, "caption"); previewText.setWrap(true);
        controls.add(previewText).width(400).left().row();
        for (AbilityType ability : abilities) {
            TextButton button = button(ability.getDisplayName(), "quiet", () -> onAbility.accept(ability));
            button.getLabel().setAlignment(Align.left);
            button.clearChildren();
            button.add(new Image(icons.drawable(PixelIcons.kind(ability)))).size(24).padRight(8);
            button.getLabel().setEllipsis(true);
            button.add(button.getLabel()).minWidth(0).growX();
            button.padLeft(12);
            button.addListener(new InputListener() {
                @Override public void enter(InputEvent e, float x, float y, int pointer, Actor from) { hoveredAbility = ability; }
                @Override public void exit(InputEvent e, float x, float y, int pointer, Actor to) {
                    if (to == null || !to.isDescendantOf(button)) hoveredAbility = null;
                }
            });
            abilityButtons.add(button);
            abilityList.add(button).width(400).height(38).padBottom(4).row();
        }
        controls.add(abilityHost).growX().row();
        move = button("MOVE", "secondary", onMove);
        action = button("ACTION", "secondary", onAction);
        end = button("END TURN", "primary", onEnd);
        cancel = button("CANCEL", "quiet", onCancel);
        controls.add(commands);
        anchor(Align.bottomLeft).add(controls).width(428);

        Table log = panel();
        TextButton logToggle = button(logCollapsed ? "LOG [+]" : "LOG [-]", "quiet", () -> {});
        log.add(logToggle).left().height(28).padBottom(5).row();
        logRows.top().left();
        ScrollPane.ScrollPaneStyle scrollStyle = new ScrollPane.ScrollPaneStyle();
        scrollStyle.vScroll = ((TextureRegionDrawable) theme.divider()).tint(FantasyUiTheme.GOLD_DIM);
        scrollStyle.vScrollKnob = theme.divider();
        logScroll = new ScrollPane(logRows, scrollStyle);
        logScroll.setScrollingDisabled(true, false);
        logScroll.setOverscroll(false, false);
        logScroll.setFadeScrollBars(false);
        Cell<ScrollPane> logCell = log.add(logScroll).width(324).height(logCollapsed ? 0 : 145);
        logScroll.setVisible(!logCollapsed);
        logToggle.addListener(new ClickListener() { @Override public void clicked(InputEvent e, float x, float y) {
            logCollapsed = !logCollapsed;
            preferences.putBoolean("logCollapsed", logCollapsed).flush();
            logToggle.setText(logCollapsed ? "LOG [+]" : "LOG [-]");
            logScroll.setVisible(!logCollapsed); logCell.height(logCollapsed ? 0 : 145); log.invalidateHierarchy();
        }});
        anchor(Align.bottomRight).add(log).width(352);
        guidePanel.setBackground(theme.panel());guidePanel.pad(14);
        guidePanel.add(new Label("QUICK GUIDE",skin,"section")).left().row();
        guideText=new Label("",skin,"status");guideText.setWrap(true);
        guidePanel.add(guideText).width(300).padTop(5).row();
        Table guideButtons=new Table();
        guideNext=button("NEXT", "secondary", ()->{if(++guideStep>=3)PresentationSettings.dismissGuide(true);});
        guideButtons.add(guideNext).width(110).minHeight(34).padRight(8);
        guideButtons.add(button("CLOSE", "quiet", ()->PresentationSettings.dismissGuide(true))).width(165).minHeight(34);
        guidePanel.add(guideButtons).padTop(8);
        anchor(Align.bottom).add(guidePanel).width(330);
        Label intro=new Label("BATTLE BEGINS",skin,"heading");intro.setColor(FantasyUiTheme.GOLD);
        Table introHost=anchor(Align.center);introHost.setTouchable(Touchable.disabled);introHost.add(intro);
        introHost.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
            com.badlogic.gdx.scenes.scene2d.actions.Actions.delay(PresentationSettings.reducedMotion()?.5f:.9f),
            com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut(PresentationSettings.reducedMotion()?0:.25f),
            com.badlogic.gdx.scenes.scene2d.actions.Actions.removeActor()));
        applyTextLayout();
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }
    public void restartGuide() { guideStep=0;guideOpen=true; }

    public Stage stage() { return stage; }
    public AbilityType hoveredAbility() { return listVisible ? hoveredAbility : null; }
    public void setPreview(String text) { previewText.setText(text == null ? "" : text); }
    public boolean pointerOverUi() {
        var point = stage.screenToStageCoordinates(new com.badlogic.gdx.math.Vector2(Gdx.input.getX(),Gdx.input.getY()));
        return stage.hit(point.x, point.y, true) != null;
    }
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
        // No large HUD backplates: outlined text and individual controls carry contrast.
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
        Drawable background = theme.resourceTrack();
        Drawable fill = theme.resourceFill(color);
        style.background = background;
        style.knobBefore = fill;
        ProgressBar result = new ProgressBar(0, 1, 0.001f, false, style);
        result.setAnimateDuration(PresentationSettings.reducedMotion()?0:.18f);
        result.setTouchable(Touchable.disabled);
        return result;
    }
    private Label addBar(Table table, ProgressBar bar,PixelIcons.Kind icon,String name) {
        Label label = new Label("", skin, "status");
        label.setAlignment(Align.center);
        Table row=new Table();
        row.add(new Image(icons.drawable(icon))).size(24).padRight(8);
        Cell<Stack> cell=row.add(new Stack(bar, label)).width(206).height(25);
        resourceBars.add(cell);
        row.addListener(new InputListener(){@Override public void enter(InputEvent e,float x,float y,int pointer,Actor from){showFeedback(name);}});
        table.add(row).width(238).padBottom(6).row();
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
        var ordered = new ArrayList<>(state.getPlayers());
        if (state.getTurnOrder()!=null) ordered.sort(java.util.Comparator.comparingInt(p -> state.getTurnOrder().indexOf(p.getId())));
        var signature = ordered.stream().map(p -> new PortraitState(p.getId(), p.getUsername(),
            p.getCharacterClass(), p.getTeamIndex(), p.isActiveTurn(), p.getHp() <= 0, p.isConnected(),
            p.getEffects().stream().anyMatch(e -> e.getType() == com.shared.shared.model.effect.StatusEffectType.STUN),
            p.getId() == state.getNextPlayerId())).toList();
        if (!signature.equals(displayedPortraits)) {
        displayedPortraits = signature;
        turnStrip.clearChildren();
        orderLabels.clear();
        portraitCards.clear();
        for(var p:ordered) {
            Table slot=new Table();slot.pad(3);
            slot.setUserObject(new PortraitOwner(p.getId()));
            TurnPortraitCard portrait=new TurnPortraitCard(assets,p.getCharacterClass(),p.getTeamIndex(),p.isActiveTurn(),p.getHp()<=0,portraitPixel);
            portraitCards.add(portrait);
            slot.add(portrait).size(52,56).padTop(10).row();
            Label who=new Label(portraitName(p.getUsername(),p.getId()),skin,"caption");
            who.setEllipsis(true);who.setAlignment(Align.center);
            who.setFontScale(.8f*PresentationSettings.textScale());orderLabels.add(who);
            slot.add(who).width(74).padTop(4).row();
            String marker=!p.isConnected()?"OFFLINE":p.getHp()<=0?"DOWN":p.getEffects().stream().anyMatch(e->e.getType()==com.shared.shared.model.effect.StatusEffectType.STUN)?"STUN":p.isActiveTurn()?"NOW":p.getId()==state.getNextPlayerId()?"NEXT":"#"+(ordered.indexOf(p)+1);
            Label orderLabel=new Label(marker,skin,"caption");orderLabel.setFontScale(.8f*PresentationSettings.textScale());orderLabels.add(orderLabel);orderLabel.setColor(p.isActiveTurn()?FantasyUiTheme.GOLD:FantasyUiTheme.TEXT_PRIMARY);
            slot.add(orderLabel).row();
            slot.addListener(new InputListener(){@Override public void enter(InputEvent e,float x,float y,int pointer,Actor from){showFeedback(clean(p.getUsername())+" | Team "+(p.getTeamIndex()==1?"A":"B")+" | "+marker);}});
            turnStrip.add(slot).width(76).padRight(3);
        }
        }
        // Only accepted server results enter the history, never targeting attempts/errors.
        String message = clean(state.getMessage());
        if (!message.isBlank() && !lastLogMessage.equals("R" + round + "  " + message)) {
            lastLogMessage = "R" + round + "  " + message;
            Label entry = new Label(lastLogMessage, skin, "status");
            entry.setWrap(true);
            history.addLast(entry);
            if (history.size() > 100) {
                history.removeFirst();
                // Table retains empty cells after actor removal; compact only at the cap.
                logRows.clearChildren();
                for (Label line : history) logRows.add(line).width(312).left().padBottom(8).row();
            } else {
                logRows.add(entry).width(312).left().padBottom(8).row();
            }
            for (Actor actor : stage.getActors()) if (actor instanceof Table table) table.validate();
            logScroll.validate();
            logScroll.setScrollPercentY(1);
            logScroll.updateVisualScroll();
        }
    }
    public static String portraitName(String name,int id) {
        return name==null || name.isBlank()?"Player "+(id+1):clean(name);
    }
    public int hoveredPortraitId() {
        var point=stage.screenToStageCoordinates(pointer.set(Gdx.input.getX(),Gdx.input.getY()));
        for(Actor actor=stage.hit(point.x,point.y,true);actor!=null;actor=actor.getParent())
            if(actor.getUserObject() instanceof PortraitOwner owner)return owner.id();
        return -1;
    }
    public void render(Player player, float delta, boolean moving) {
        if(theme.refreshTextScale()) applyTextLayout();
        float barDuration=PresentationSettings.reducedMotion()?0:.18f;
        hp.setAnimateDuration(barDuration);mana.setAnimateDuration(barDuration);stamina.setAnimateDuration(barDuration);
        guidePanel.setVisible(guideOpen && !PresentationSettings.guideDismissed());
        guideNext.setText(guideStep>=2?"DONE":"NEXT");
        guideText.setText(switch(Math.min(2,guideStep)) {
            case 0 -> "1 / 3  MOVE\nClick Move, then a blue area. You can move in several short steps.";
            case 1 -> "2 / 3  ACTION\nClick Action, choose an ability, then a highlighted target or valid ground. Cancel costs nothing.";
            default -> "3 / 3  YOUR TURN\nYou get 1 AP plus movement. Spend both to end automatically, or click End Turn. Lava is lethal when pushed into it.";
        });
        String statuses=com.github.thedragonconquerors.input.TacticalPreview.statuses(player);
        statusText.setText(player.getActiveEffects().stream().anyMatch(e->e.getType()==com.shared.shared.model.effect.StatusEffectType.CURSE)?"Curse: hit its caster to escape.":"");
        if(!statuses.equals(statusSignature)) {
            statusSignature=statuses;statusIcons.clearChildren();
            for(var effect:player.getActiveEffects()) {
                Image badge=new Image(icons.drawable(PixelIcons.kind(effect.getType())));
                badge.addListener(new InputListener(){@Override public void enter(InputEvent e,float x,float y,int pointer,Actor from){
                    showFeedback(effect.getType().getDisplayName()+" ("+effect.getRemainingTurns()+" turns): "+effect.getType().getDescription());
                }});
                statusIcons.add(badge).size(20);
                statusIcons.add(new Label(" "+effect.getRemainingTurns()+"t ",skin,"caption"));
            }
        }
        feedbackTimer = Math.max(0, feedbackTimer - delta);
        float maximum = player.getMovementController().getMaxMovementDistance();
        float remaining = player.getMovementController().getRemainingMovementDistance();
        hp.setValue(ratio(player.getStats().getHp(), player.getStats().getMaxHp()));
        mana.setValue(ratio(player.getStats().getMana(), player.getStats().getMaxMana()));
        stamina.setValue(ratio(remaining, maximum));
        hpText.setText(player.getStats().getHp() + " / " + player.getStats().getMaxHp());
        manaText.setText(player.getStats().getMana() + " / " + player.getStats().getMaxMana());
        int remainingTenths = Math.round(remaining * 10), maximumTenths = Math.round(maximum * 10);
        if (remainingTenths != lastMovementTenths || maximumTenths != lastMaximumTenths) {
            lastMovementTenths = remainingTenths; lastMaximumTenths = maximumTenths;
            staminaText.setText(String.format(java.util.Locale.ROOT, "%.1f / %.1f", remaining, maximum));
        }
        apText.setText(player.getActionPoints() + " AP");
        for(TurnPortraitCard card:portraitCards)card.setViewerTeam(player.getTeamIndex());
        roundText.setText("ROUND " + round);
        move.setDisabled(!interaction.canMove(player, moving));
        action.setDisabled(!interaction.canAct(player, moving));
        end.setDisabled(!interaction.canControl(player, moving));
        cancel.setDisabled(interaction.mode() == BattleInteraction.Mode.NONE && targetingPrompt.isEmpty());
        move.setColor(interaction.mode() == BattleInteraction.Mode.MOVE ? MOVE_SELECTED : Color.WHITE);
        action.setColor(interaction.mode() == BattleInteraction.Mode.ACTION ? FantasyUiTheme.GOLD : Color.WHITE);
        boolean visible = interaction.mode() == BattleInteraction.Mode.ACTION && targetingPrompt.isEmpty();
        if (visible != listVisible) {
            abilityHost.clearChildren();
            if (visible) abilityHost.add(abilityList).growX().padBottom(5);
            if (visible) UiMotion.reveal(abilityList);
            listVisible = visible;
        }
        for (int i = 0; i < abilities.size(); i++) {
            AbilityType ability = abilities.get(i);
            TextButton button = abilityButtons.get(i);
            button.setDisabled(!interaction.canUse(player, moving, ability));
            String reason = unavailableReason(player,ability,moving);
            button.setText(ability.getDisplayName() + (PresentationSettings.textScale()>1f?"\n":" | ") + (!reason.isEmpty()?reason
                : "1 AP" + (ability.getManaCost() > 0 ? " + " + ability.getManaCost() + " mana" : "")));
        }
        hint.setText(feedbackTimer > 0 ? feedback : !targetingPrompt.isEmpty() ? targetingPrompt
            : interaction.awaitingServer() ? "Waiting for the server..."
            : !player.isActiveTurn() ? "Waiting for " + clean(activeName) + "."
            : moving ? ""
            : interaction.mode() == BattleInteraction.Mode.MOVE ? "Move"
            : visible ? ""
            : "");
        viewport.apply();
        stage.act(Math.min(delta, 1f / 15f));
        stage.draw();
    }
    public String unavailableReason(Player p, AbilityType ability, boolean moving) {
        if(!p.isAlive())return "Defeated";
        if(!p.isActiveTurn())return "Not your turn";
        if(interaction.awaitingServer())return "Waiting for server";
        if(p.isActionUsed()||p.getActionPoints()==0)return "No AP remaining";
        if(p.cooldownTurns(ability)>0)return "Cooldown: "+p.cooldownTurns(ability)+"t";
        if(p.getStats().getMana()<ability.getManaCost())return "Not enough mana";
        return moving?"Wait for animation":"";
    }
    private void applyTextLayout() {
        float textScale=PresentationSettings.textScale();
        for(Cell<Stack> cell:resourceBars) cell.height(25*textScale);
        for(ProgressBar bar:new ProgressBar[]{hp,mana,stamina}) {
            bar.getStyle().background.setMinHeight(22*textScale);
            bar.getStyle().knobBefore.setMinHeight(22*textScale-4);
            bar.invalidateHierarchy();
        }
        for(TextButton button:abilityButtons) abilityList.getCell(button).height((textScale>1f?54:38)*textScale);
        for(Label label:orderLabels) label.setFontScale(.8f*textScale);
        commands.clearChildren();
        if(textScale>1f) {
            // Larger type gets two comfortable rows, without widening the map overlay.
            commands.add(move).width(197).height(40*textScale).padRight(6).padBottom(5);
            commands.add(action).width(197).height(40*textScale).padBottom(5).row();
            commands.add(end).width(197).height(40*textScale).padRight(6);
            commands.add(cancel).width(197).height(40*textScale);
        } else {
            commands.add(move).width(95).height(40).padRight(5);
            commands.add(action).width(95).height(40).padRight(5);
            commands.add(end).width(115).height(40).padRight(5);
            commands.add(cancel).width(80).height(40);
        }
        for(Actor root:stage.getActors())UiMotion.relayout(root);
    }
    private static float ratio(float value, float max) { return max <= 0 ? 0 : Math.max(0, Math.min(1, value / max)); }
    private static String clean(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ')
            .replace("\u2192", "->").replace('\u2014', '-').replace('\u2013', '-');
    }
    public void resize(int width, int height) {
        viewport.setUnitsPerPixel(Math.max(1280f / Math.max(1, width), 720f / Math.max(1, height)) / hudScale);
        viewport.update(width, height, true);
    }
    @Override public void dispose() { icons.dispose();stage.dispose(); theme.dispose(); }
}
