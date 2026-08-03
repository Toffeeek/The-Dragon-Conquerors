package com.github.thedragonconquerors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.shared.shared.model.CharacterClass;

/**
 * Current multiplayer preparation screen.
 *
 * It keeps the existing team/class selection and startGame(team, class) flow,
 * while presenting it as a proper tactical-RPG lobby.
 */
public class LobbyScreen extends ScreenAdapter {
    private final Main game;
    private final String joinUrl;

    private Stage stage;
    private FantasyUiTheme theme;
    private Skin skin;

    private int selectedTeam = 1;
    private CharacterClass selectedClass = CharacterClass.WARRIOR;

    private Label selectedClassLabel;
    private Label selectedRoleLabel;
    private Label selectedDescriptionLabel;
    private Label selectionSummaryLabel;
    private Label copyStatusLabel;

    public LobbyScreen(Main game, String joinUrl) {
        this.game = game;
        this.joinUrl = joinUrl == null ? Main.DEFAULT_SERVER_URL : joinUrl;
    }

    @Override
    public void show() {
        if (stage == null) {
            buildUi();
        }
        updateClassDetails();
        Gdx.input.setInputProcessor(stage);
    }

    private void buildUi() {
        theme = new FantasyUiTheme();
        skin = theme.skin();
        stage = new Stage(new FitViewport(
            FantasyUiTheme.VIRTUAL_WIDTH,
            FantasyUiTheme.VIRTUAL_HEIGHT));

        Image background = new Image(theme.background());
        background.setFillParent(true);
        background.setScaling(Scaling.stretch);
        stage.addActor(background);

        Table root = new Table();
        root.setFillParent(true);
        root.pad(30f, 42f, 30f, 42f);
        stage.addActor(root);

        root.add(createTopBar()).growX().height(72f).row();
        root.add(createContent()).grow().padTop(22f).row();
        root.add(createBottomBar()).growX().height(72f).padTop(18f);
    }

    private Table createTopBar() {
        Table bar = new Table();
        bar.setBackground(theme.panelAlt());
        bar.pad(10f, 14f, 10f, 14f);

        TextButton leaveButton = new TextButton("LEAVE LOBBY", skin, "quiet");
        leaveButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.returnToMenu();
            }
        });

        Label title = new Label("CAMPAIGN LOBBY", skin, "heading");
        title.setAlignment(Align.center);

        TextButton copyButton = new TextButton("COPY ADDRESS", skin, "secondary");
        copyButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.getClipboard().setContents(joinUrl);
                copyStatusLabel.setText("Address copied");
                copyStatusLabel.setColor(FantasyUiTheme.SUCCESS);
            }
        });

        bar.add(leaveButton).width(150f).height(42f).left();
        bar.add(title).expandX().center();
        bar.add(copyButton).width(160f).height(42f).right();
        return bar;
    }

    private Table createContent() {
        Table content = new Table();
        content.add(createSessionPanel()).width(330f).growY();
        content.add(createClassPanel()).expand().fill().padLeft(24f);
        return content;
    }

    private Table createSessionPanel() {
        Table panel = new Table();
        panel.setBackground(theme.panel());
        panel.pad(24f);
        panel.top().left();

        Label heading = new Label("SESSION", skin, "heading");
        Label connected = new Label("CONNECTED", skin, "section");
        connected.setColor(FantasyUiTheme.SUCCESS);

        Label addressHeading = new Label("JOIN ADDRESS", skin, "section");
        Label address = new Label(joinUrl, skin, "caption");
        address.setWrap(true);

        copyStatusLabel = new Label("Share this address with other players.", skin, "caption");
        copyStatusLabel.setWrap(true);

        Label teamHeading = new Label("CHOOSE TEAM", skin, "section");
        TextButton blueTeam = new TextButton("AZURE TEAM", skin, "team-blue");
        TextButton redTeam = new TextButton("CRIMSON TEAM", skin, "team-red");
        blueTeam.setChecked(true);

        ButtonGroup<TextButton> teamGroup = new ButtonGroup<>(blueTeam, redTeam);
        teamGroup.setMinCheckCount(1);
        teamGroup.setMaxCheckCount(1);

        blueTeam.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectedTeam = 1;
                updateSelectionSummary();
            }
        });
        redTeam.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectedTeam = 2;
                updateSelectionSummary();
            }
        });

        Table teamButtons = new Table();
        teamButtons.defaults().width(270f).height(48f).padBottom(9f);
        teamButtons.add(blueTeam).row();
        teamButtons.add(redTeam).row();

        Table localPlayerCard = new Table();
        localPlayerCard.setBackground(theme.inset());
        localPlayerCard.pad(16f);
        Label playerTitle = new Label("LOCAL PLAYER", skin, "section");
        selectionSummaryLabel = new Label("", skin, "default");
        selectionSummaryLabel.setWrap(true);
        localPlayerCard.add(playerTitle).left().row();
        localPlayerCard.add(selectionSummaryLabel).width(238f).left().padTop(8f).row();

        panel.add(heading).left().row();
        panel.add(connected).left().padTop(8f).row();
        panel.add(new Image(theme.divider())).width(270f).height(2f)
            .left().padTop(16f).padBottom(18f).row();
        panel.add(addressHeading).left().row();
        panel.add(address).width(270f).left().padTop(7f).row();
        panel.add(copyStatusLabel).width(270f).left().padTop(10f).row();
        panel.add(teamHeading).left().padTop(28f).padBottom(10f).row();
        panel.add(teamButtons).left().row();
        panel.add(localPlayerCard).width(270f).left().padTop(18f).row();
        return panel;
    }

    private Table createClassPanel() {
        Table panel = new Table();
        panel.setBackground(theme.panel());
        panel.pad(24f, 26f, 24f, 26f);
        panel.top();

        Label heading = new Label("CHOOSE YOUR CLASS", skin, "heading");
        Label hint = new Label(
            "Your class determines the actions available during combat.",
            skin, "caption");

        Table classGrid = new Table();
        ButtonGroup<TextButton> classGroup = new ButtonGroup<>();
        classGroup.setMinCheckCount(1);
        classGroup.setMaxCheckCount(1);

        CharacterClass[] classes = CharacterClass.values();
        for (int index = 0; index < classes.length; index++) {
            CharacterClass characterClass = classes[index];
            String cardText = characterClass.displayName.toUpperCase()
                + "\n" + roleFor(characterClass);
            TextButton classButton = new TextButton(cardText, skin, "class-card");
            classButton.getLabel().setAlignment(Align.center);
            classButton.getLabel().setWrap(true);
            classButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    selectedClass = characterClass;
                    updateClassDetails();
                }
            });

            if (characterClass == selectedClass) classButton.setChecked(true);
            classGroup.add(classButton);
            classGrid.add(classButton).width(218f).height(92f).pad(6f);
            if ((index + 1) % 3 == 0) classGrid.row();
        }

        Table details = new Table();
        details.setBackground(theme.inset());
        details.pad(18f, 20f, 18f, 20f);
        details.left();

        selectedClassLabel = new Label("", skin, "class-title");
        selectedRoleLabel = new Label("", skin, "class-role");
        selectedDescriptionLabel = new Label("", skin, "default");
        selectedDescriptionLabel.setWrap(true);

        details.add(selectedClassLabel).left().row();
        details.add(selectedRoleLabel).left().padTop(4f).row();
        details.add(selectedDescriptionLabel).width(640f).left().padTop(12f).row();

        panel.add(heading).left().row();
        panel.add(hint).left().padTop(6f).padBottom(14f).row();
        panel.add(classGrid).left().row();
        panel.add(details).width(690f).left().padTop(18f).row();
        return panel;
    }

    private Table createBottomBar() {
        Table bar = new Table();
        bar.setBackground(theme.panelAlt());
        bar.pad(10f, 14f, 10f, 14f);

        Label help = new Label(
            "Select a team and class, then enter the battlefield.",
            skin, "caption");

        TextButton startButton = new TextButton("ENTER BATTLE", skin, "primary");
        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.startGame(selectedTeam, selectedClass);
            }
        });

        bar.add(help).expandX().left().padLeft(8f);
        bar.add(startButton).width(240f).height(50f).right();
        return bar;
    }

    private void updateClassDetails() {
        if (selectedClassLabel == null) return;
        selectedClassLabel.setText(selectedClass.displayName.toUpperCase());
        selectedRoleLabel.setText(roleFor(selectedClass).toUpperCase());
        selectedDescriptionLabel.setText(descriptionFor(selectedClass));
        updateSelectionSummary();
    }

    private void updateSelectionSummary() {
        if (selectionSummaryLabel == null) return;
        String teamName = selectedTeam == 1 ? "Azure Team" : "Crimson Team";
        selectionSummaryLabel.setText(
            teamName + "\n" + selectedClass.displayName + " - " + roleFor(selectedClass));
    }

    private String roleFor(CharacterClass characterClass) {
        switch (characterClass) {
            case WARRIOR:
                return "Front-line fighter";
            case MAGE:
                return "Arcane damage";
            case ARCHER:
                return "Ranged striker";
            case PALADIN:
                return "Defender and support";
            case ROGUE:
                return "Mobile assassin";
            default:
                return "Adventurer";
        }
    }

    private String descriptionFor(CharacterClass characterClass) {
        switch (characterClass) {
            case WARRIOR:
                return "A durable melee combatant built to hold ground, pressure nearby enemies and survive direct confrontation.";
            case MAGE:
                return "A powerful spellcaster who spends mana to control space and deal heavy magical damage from a safer position.";
            case ARCHER:
                return "A precise ranged attacker who performs best when distance, movement and clear lines of attack are maintained.";
            case PALADIN:
                return "A resilient protector who combines close-range combat with defensive and supportive abilities for the party.";
            case ROGUE:
                return "A fast opportunist who relies on positioning, target selection and sudden attacks rather than prolonged combat.";
            default:
                return "A versatile adventurer ready to enter the dungeon.";
        }
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.02f, 0.018f, 0.022f, 1f);

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.returnToMenu();
            return;
        }

        stage.act(Math.min(delta, 1f / 15f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        if (stage != null) stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        if (stage != null) {
            stage.dispose();
            stage = null;
        }
        if (theme != null) {
            theme.dispose();
            theme = null;
        }
        skin = null;
    }
}
