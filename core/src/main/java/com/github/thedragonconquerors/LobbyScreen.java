// File Location: core/src/main/java/com/github/thedragonconquerors/LobbyScreen.java
package com.github.thedragonconquerors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.shared.shared.model.Action;
import com.shared.shared.model.CharacterBuild;
import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.Packet;
import com.shared.shared.model.Race;
import com.shared.shared.model.ability.AbilityType;
import com.shared.shared.model.stats.StatComponent;
import com.shared.shared.model.stats.StatType;
import com.shared.shared.model.world.Environment;
import com.shared.shared.network.MatchState;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Class -> race -> environment-vote flow for the multiplayer lobby. */
public class LobbyScreen extends ScreenAdapter {
    private enum Step { CLASS, RACE, ENVIRONMENT }

    private final Main game;
    private final String joinUrl;
    private final Map<Integer, Packet> roster = new LinkedHashMap<>();
    private final Map<Environment, Integer> voteCounts = new EnumMap<>(Environment.class);

    private Stage stage;
    private FantasyUiTheme theme;
    private Skin skin;
    private Table selectionHost;
    private Label selectionSummaryLabel;
    private Label copyStatusLabel;
    private Label lobbyStatusLabel;
    private Label footerHelpLabel;
    private TextField usernameField;
    private TextButton azureTeamButton;
    private TextButton crimsonTeamButton;
    private TextButton backButton;
    private TextButton nextButton;

    private Step step = Step.CLASS;
    private int selectedTeam = 1;
    private CharacterClass selectedClass = CharacterClass.PALADIN;
    private Race selectedRace = Race.HUMAN;
    private Environment selectedEnvironment;
    private int localPlayerId = -1;
    private int connectedPlayers;
    private boolean joined;
    private boolean voteSent;
    private boolean matchStarting;

    public LobbyScreen(Main game, String joinUrl) {
        this.game = game;
        this.joinUrl = joinUrl == null ? Main.DEFAULT_SERVER_URL : joinUrl;
        for (Environment environment : Environment.values()) voteCounts.put(environment, 0);
    }

    @Override
    public void show() {
        if (stage == null) buildUi();
        game.getNetworkClient().setPacketHandler(
            packet -> Gdx.app.postRunnable(() -> handlePacket(packet)));
        showStep();
        Gdx.input.setInputProcessor(stage);
    }

    private void buildUi() {
        theme = new FantasyUiTheme();
        skin = theme.skin();
        stage = new Stage(new FitViewport(
            FantasyUiTheme.VIRTUAL_WIDTH, FantasyUiTheme.VIRTUAL_HEIGHT));

        Image background = new Image(theme.background());
        background.setFillParent(true);
        background.setScaling(Scaling.stretch);
        stage.addActor(background);

        Table root = new Table();
        root.setFillParent(true);
        root.pad(30f, 42f, 30f, 42f);
        stage.addActor(root);

        root.add(createTopBar()).growX().height(72f).row();
        Table content = new Table();
        content.add(createSessionPanel()).width(330f).growY();
        selectionHost = new Table();
        content.add(selectionHost).expand().fill().padLeft(24f);
        root.add(content).grow().padTop(22f).row();
        root.add(createBottomBar()).growX().height(72f).padTop(18f);
    }

    private Table createTopBar() {
        Table bar = new Table();
        bar.setBackground(theme.panelAlt());
        bar.pad(10f, 14f, 10f, 14f);

        TextButton leaveButton = new TextButton("LEAVE LOBBY", skin, "quiet");
        leaveButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                game.returnToMenu();
            }
        });

        Label title = new Label("PARTY ASSEMBLY", skin, "heading");
        title.setAlignment(Align.center);
        TextButton copyButton = new TextButton("COPY ADDRESS", skin, "secondary");
        copyButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
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

    private Table createSessionPanel() {
        Table panel = new Table();
        panel.setBackground(theme.panel());
        panel.pad(24f);
        panel.top().left();

        Label heading = new Label("SESSION", skin, "heading");
        Label connected = new Label("CONNECTED", skin, "section");
        connected.setColor(FantasyUiTheme.SUCCESS);
        Label address = new Label(joinUrl, skin, "caption");
        address.setWrap(true);
        copyStatusLabel = new Label("Share this address with the party.", skin, "caption");
        copyStatusLabel.setWrap(true);

        Label nameHeading = new Label("PLAYER NAME", skin, "section");
        usernameField = new TextField(defaultUsername(), skin);
        usernameField.setMaxLength(24);

        Label teamHeading = new Label("CHOOSE TEAM", skin, "section");
        azureTeamButton = new TextButton("AZURE TEAM", skin, "team-blue");
        crimsonTeamButton = new TextButton("CRIMSON TEAM", skin, "team-red");
        azureTeamButton.setChecked(true);
        ButtonGroup<TextButton> teamGroup =
            new ButtonGroup<>(azureTeamButton, crimsonTeamButton);
        teamGroup.setMinCheckCount(1);
        teamGroup.setMaxCheckCount(1);
        azureTeamButton.addListener(teamListener(1));
        crimsonTeamButton.addListener(teamListener(2));

        Table teams = new Table();
        teams.defaults().width(270f).height(44f).padBottom(8f);
        teams.add(azureTeamButton).row();
        teams.add(crimsonTeamButton).row();

        Table playerCard = new Table();
        playerCard.setBackground(theme.inset());
        playerCard.pad(14f);
        selectionSummaryLabel = new Label("", skin, "default");
        selectionSummaryLabel.setWrap(true);
        playerCard.add(new Label("YOUR BUILD", skin, "section")).left().row();
        playerCard.add(selectionSummaryLabel).width(240f).left().padTop(7f).row();

        lobbyStatusLabel = new Label("Not registered yet", skin, "caption");
        lobbyStatusLabel.setWrap(true);

        panel.add(heading).left().row();
        panel.add(connected).left().padTop(7f).row();
        panel.add(address).width(270f).left().padTop(10f).row();
        panel.add(copyStatusLabel).width(270f).left().padTop(7f).row();
        panel.add(nameHeading).left().padTop(18f).row();
        panel.add(usernameField).width(270f).height(42f).padTop(7f).row();
        panel.add(teamHeading).left().padTop(20f).padBottom(8f).row();
        panel.add(teams).left().row();
        panel.add(playerCard).width(270f).left().padTop(14f).row();
        panel.add(lobbyStatusLabel).width(270f).left().padTop(12f).row();
        return panel;
    }

    private ClickListener teamListener(int team) {
        return new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (!joined) {
                    selectedTeam = team;
                    updateSelectionSummary();
                }
            }
        };
    }

    private Table createBottomBar() {
        Table bar = new Table();
        bar.setBackground(theme.panelAlt());
        bar.pad(10f, 14f, 10f, 14f);
        backButton = new TextButton("BACK", skin, "quiet");
        backButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { previousStep(); }
        });
        footerHelpLabel = new Label("", skin, "caption");
        nextButton = new TextButton("", skin, "primary");
        nextButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { nextStep(); }
        });

        bar.add(backButton).width(130f).height(46f).left();
        bar.add(footerHelpLabel).expandX().left().padLeft(18f);
        bar.add(nextButton).width(230f).height(50f).right();
        return bar;
    }

    private void showStep() {
        if (selectionHost == null) return;
        selectionHost.clearChildren();
        switch (step) {
            case CLASS:
                selectionHost.add(createClassPanel()).grow();
                backButton.setDisabled(true);
                nextButton.setDisabled(false);
                nextButton.setText("CHOOSE RACE");
                footerHelpLabel.setText("Step 1 of 3 - inspect base stats and choose a class.");
                break;
            case RACE:
                selectionHost.add(createRacePanel()).grow();
                backButton.setDisabled(joined);
                nextButton.setDisabled(joined);
                nextButton.setText("JOIN & VOTE");
                footerHelpLabel.setText("Step 2 of 3 - racial boosts have an equal four-step budget.");
                break;
            case ENVIRONMENT:
                selectionHost.add(createEnvironmentPanel()).grow();
                backButton.setDisabled(true);
                nextButton.setDisabled(true);
                nextButton.setText(voteSent ? "VOTE SUBMITTED" : "JOINING...");
                footerHelpLabel.setText("Step 3 of 3 - four votes start the match; tied leaders are randomised.");
                break;
            default:
                throw new IllegalStateException("Unknown selection step: " + step);
        }
        updateSelectionSummary();
    }

    private Table panel(String title, String hint) {
        Table panel = new Table();
        panel.setBackground(theme.panel());
        panel.pad(22f, 26f, 22f, 26f);
        panel.top().left();
        panel.add(new Label(title, skin, "heading")).left().row();
        Label hintLabel = new Label(hint, skin, "caption");
        hintLabel.setWrap(true);
        panel.add(hintLabel).width(700f).left().padTop(6f).padBottom(12f).row();
        return panel;
    }

    private Table createClassPanel() {
        Table panel = panel("CHOOSE YOUR CLASS",
            "Base design tiers are shown below. Ability balance compensates for different stat totals.");
        Table grid = new Table();
        ButtonGroup<TextButton> group = new ButtonGroup<>();
        group.setMinCheckCount(1);
        group.setMaxCheckCount(1);
        CharacterClass[] classes = CharacterClass.values();
        for (int index = 0; index < classes.length; index++) {
            CharacterClass value = classes[index];
            TextButton button = new TextButton(
                value.displayName.toUpperCase() + "\n" + value.getRoleLabel(), skin, "class-card");
            button.getLabel().setWrap(true);
            button.getLabel().setAlignment(Align.center);
            button.setChecked(value == selectedClass);
            button.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    selectedClass = value;
                    Gdx.app.postRunnable(() -> showStep());
                }
            });
            group.add(button);
            grid.add(button).width(218f).height(78f).pad(5f);
            if ((index + 1) % 3 == 0) grid.row();
        }
        panel.add(grid).left().row();

        Table details = detailBox();
        details.add(new Label(selectedClass.displayName.toUpperCase(), skin, "class-title")).left().row();
        details.add(new Label(descriptionFor(selectedClass), skin, "caption"))
            .width(660f).left().padTop(6f).row();
        details.add(createClassTierTable(selectedClass)).left().padTop(10f).row();
        String abilities = AbilityType.forClass(selectedClass).stream()
            .map(AbilityType::getDisplayName).collect(Collectors.joining("  |  "));
        Label abilityLabel = new Label("Abilities: " + abilities, skin, "caption");
        abilityLabel.setWrap(true);
        details.add(abilityLabel).width(660f).left().padTop(9f).row();
        panel.add(details).width(700f).left().padTop(12f).row();
        return panel;
    }

    private Table createRacePanel() {
        Table panel = panel("CHOOSE YOUR RACE",
            "Preview the finished engine stats. Green values include this race's baseline and pairing bonuses.");
        Table grid = new Table();
        ButtonGroup<TextButton> group = new ButtonGroup<>();
        group.setMinCheckCount(1);
        group.setMaxCheckCount(1);
        Race[] races = Race.values();
        for (int index = 0; index < races.length; index++) {
            Race value = races[index];
            CharacterBuild preview = CharacterBuild.of(value, selectedClass);
            String marker = preview.isNamedSynergy() ? "NAMED SYNERGY" : "BALANCED AFFINITY";
            TextButton button = new TextButton(value.displayName.toUpperCase() + "\n" + marker,
                skin, "class-card");
            button.getLabel().setWrap(true);
            button.getLabel().setAlignment(Align.center);
            button.setChecked(value == selectedRace);
            button.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    selectedRace = value;
                    Gdx.app.postRunnable(() -> showStep());
                }
            });
            group.add(button);
            grid.add(button).width(330f).height(76f).pad(5f);
            if ((index + 1) % 2 == 0) grid.row();
        }
        panel.add(grid).left().row();

        CharacterBuild build = CharacterBuild.of(selectedRace, selectedClass);
        Table details = detailBox();
        details.add(new Label(build.displayName().toUpperCase(), skin, "class-title")).left().row();
        Label description = new Label(selectedRace.getDescription(), skin, "caption");
        description.setWrap(true);
        details.add(description).width(660f).left().padTop(5f).row();
        Label boosts = new Label(build.describeBoosts(), skin, "class-role");
        boosts.setColor(FantasyUiTheme.SUCCESS);
        details.add(boosts).left().padTop(8f).row();
        details.add(createBoostedStatsTable(build.createBaseStats(), build.createStats()))
            .left().padTop(10f).row();
        panel.add(details).width(700f).left().padTop(12f).row();
        return panel;
    }

    private Table createEnvironmentPanel() {
        Table panel = panel("VOTE FOR THE BATTLEFIELD",
            "The server counts one current vote per player. You may change your vote until all four players vote.");
        Table cards = new Table();
        ButtonGroup<TextButton> group = new ButtonGroup<>();
        group.setMinCheckCount(0);
        group.setMaxCheckCount(1);
        for (Environment environment : Environment.values()) {
            int votes = voteCounts.getOrDefault(environment, 0);
            String cardText = environment.getDisplayName().toUpperCase()
                + "\n" + environment.hazardSummary() + "\nVOTES: " + votes;
            TextButton button = new TextButton(cardText, skin, "class-card");
            button.getLabel().setWrap(true);
            button.getLabel().setAlignment(Align.center);
            button.setChecked(environment == selectedEnvironment);
            button.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    selectedEnvironment = environment;
                    submitVoteIfReady();
                    Gdx.app.postRunnable(() -> showStep());
                }
            });
            group.add(button);
            cards.add(button).width(222f).height(120f).pad(5f);
        }
        panel.add(cards).left().row();

        Table details = detailBox();
        String choice = selectedEnvironment == null
            ? "Choose Bog, Lava, or Canyon to submit your vote."
            : "Your vote: " + selectedEnvironment.getDisplayName() + " - "
                + selectedEnvironment.getDescription();
        Label choiceLabel = new Label(choice, skin, "default");
        choiceLabel.setWrap(true);
        details.add(choiceLabel).width(660f).left().row();
        int totalVotes = voteCounts.values().stream().mapToInt(Integer::intValue).sum();
        Label progress = new Label("Players: " + connectedPlayers + "/4   Votes: "
            + totalVotes + "/4", skin, "class-role");
        progress.setColor(totalVotes == 4 ? FantasyUiTheme.SUCCESS : FantasyUiTheme.TEXT_MUTED);
        details.add(progress).left().padTop(10f).row();
        panel.add(details).width(700f).left().padTop(16f).row();
        return panel;
    }

    private Table detailBox() {
        Table table = new Table();
        table.setBackground(theme.inset());
        table.pad(14f, 18f, 14f, 18f);
        table.left();
        return table;
    }

    private Table createClassTierTable(CharacterClass characterClass) {
        Table table = new Table();
        for (StatType stat : StatType.values()) {
            table.add(new Label(stat.getDisplayName(), skin, "caption")).width(68f).left();
            Label value = new Label(Integer.toString(characterClass.getTier(stat)), skin, "default");
            value.setColor(FantasyUiTheme.SUCCESS);
            table.add(value).width(32f).left();
        }
        return table;
    }

    private Table createBoostedStatsTable(StatComponent base, StatComponent boosted) {
        Table table = new Table();
        int index = 0;
        for (StatType stat : StatType.values()) {
            int before = stat.read(base);
            int after = stat.read(boosted);
            table.add(new Label(stat.getDisplayName(), skin, "caption")).width(88f).left();
            Label value = new Label(before == after ? Integer.toString(after)
                : before + " -> " + after, skin, "default");
            if (after > before) value.setColor(FantasyUiTheme.SUCCESS);
            table.add(value).width(104f).left();
            index++;
            if (index % 3 == 0) table.row();
        }
        return table;
    }

    private void nextStep() {
        if (step == Step.CLASS) {
            step = Step.RACE;
            showStep();
        } else if (step == Step.RACE && !joined) {
            step = Step.ENVIRONMENT;
            joinLobby();
            showStep();
        }
    }

    private void previousStep() {
        if (joined) return;
        if (step == Step.RACE) step = Step.CLASS;
        showStep();
    }

    private void joinLobby() {
        joined = true;
        usernameField.setDisabled(true);
        azureTeamButton.setDisabled(true);
        crimsonTeamButton.setDisabled(true);
        lobbyStatusLabel.setText("Registering build with server...");
        lobbyStatusLabel.setColor(FantasyUiTheme.TEXT_MUTED);
        float spawnX = selectedTeam == 1 ? 1f : 28f;
        game.getNetworkClient().join(username(), new Vector2(spawnX, 9f), selectedTeam,
            selectedClass, selectedRace);
    }

    private void submitVoteIfReady() {
        if (localPlayerId < 0 || selectedEnvironment == null) return;
        game.getNetworkClient().voteEnvironment(localPlayerId, selectedEnvironment);
        voteSent = true;
        nextButton.setText("VOTE SUBMITTED");
        lobbyStatusLabel.setText("Vote submitted. Waiting for the party...");
        lobbyStatusLabel.setColor(FantasyUiTheme.SUCCESS);
    }

    private void handlePacket(Packet packet) {
        if (packet == null || packet.getAction() == null) return;
        switch (packet.getAction()) {
            case PRIVATE_JOIN_CONFIRMATION:
                localPlayerId = packet.getID();
                connectedPlayers = Math.max(connectedPlayers, packet.getConnectedPlayers());
                String roomName = packet.getRoomId() == null ? "match room" : packet.getRoomId();
                lobbyStatusLabel.setText("Assigned to " + roomName + " as player "
                    + (localPlayerId + 1) + ". Cast your vote.");
                lobbyStatusLabel.setColor(FantasyUiTheme.SUCCESS);
                submitVoteIfReady();
                showStep();
                break;
            case PLAYER_COORDINATE:
            case JOIN:
                roster.put(packet.getID(), packet);
                connectedPlayers = Math.max(connectedPlayers, packet.getConnectedPlayers());
                if (step == Step.ENVIRONMENT) showStep();
                break;
            case ENVIRONMENT_VOTE_UPDATE:
                connectedPlayers = packet.getConnectedPlayers();
                voteCounts.put(Environment.BOG, packet.getBogVotes());
                voteCounts.put(Environment.LAVA, packet.getLavaVotes());
                voteCounts.put(Environment.CANYON, packet.getCanyonVotes());
                if (step == Step.ENVIRONMENT) showStep();
                break;
            case MATCH_START:
                beginMatch(packet.getEnvironment(), packet.getMatchState());
                break;
            case LEAVE:
                roster.remove(packet.getID());
                break;
            case ERROR:
                joined = false;
                voteSent = false;
                step = Step.RACE;
                usernameField.setDisabled(false);
                azureTeamButton.setDisabled(false);
                crimsonTeamButton.setDisabled(false);
                lobbyStatusLabel.setText(packet.getMessage() == null ? "Lobby rejected the selection." : packet.getMessage());
                lobbyStatusLabel.setColor(FantasyUiTheme.ERROR);
                showStep();
                break;
            default:
                break;
        }
    }

    private void beginMatch(Environment environment, MatchState initialState) {
        if (matchStarting || environment == null || localPlayerId < 0) return;
        matchStarting = true;
        Packet local = roster.get(localPlayerId);
        if (local == null) {
            local = Packet.builder().ID(localPlayerId).username(username())
                .teamIndex(selectedTeam).characterClass(selectedClass).race(selectedRace)
                .finalPosition(new Vector2(selectedTeam == 1 ? 1f : 28f, 9f))
                .action(Action.JOIN).build();
            roster.put(localPlayerId, local);
        }
        game.startGame(selectedTeam, CharacterBuild.of(selectedRace, selectedClass),
            environment, localPlayerId, new ArrayList<>(roster.values()), initialState);
    }

    private void updateSelectionSummary() {
        if (selectionSummaryLabel == null) return;
        String team = selectedTeam == 1 ? "Azure Team" : "Crimson Team";
        selectionSummaryLabel.setText(team + "\n" + selectedRace.displayName + " "
            + selectedClass.displayName);
    }

    private String username() {
        String value = usernameField == null ? "Player" : usernameField.getText().trim();
        return value.isEmpty() ? "Player" : value;
    }

    private String defaultUsername() {
        String name = System.getProperty("user.name", "Player").trim();
        return name.isEmpty() ? "Player" : name;
    }

    private String descriptionFor(CharacterClass characterClass) {
        switch (characterClass) {
            case PALADIN: return "Durable frontline fighter with Sacred Bolt and heavy single-target Divine Smite.";
            case MAGE: return "High-damage caster using fire, ice, displacement, and a one-turn stun.";
            case WRAITH: return "Fast assassin with poison, a breakable death curse, and unrestricted Teleport.";
            case CLERIC: return "Battlefield healer and the only class capable of reviving a fallen teammate.";
            case BARD: return "Support specialist that boosts allies and restores a teammate's ultimate.";
            case ARCHER: return "Long-range attacker with self-accuracy support and an area-damage ultimate.";
            default: return "A versatile combatant.";
        }
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.02f, 0.018f, 0.022f, 1f);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) && !joined) {
            if (step == Step.CLASS) game.returnToMenu(); else previousStep();
            return;
        }
        stage.act(Math.min(delta, 1f / 15f));
        stage.draw();
    }

    @Override public void resize(int width, int height) {
        if (stage != null) stage.getViewport().update(width, height, true);
    }

    @Override public void hide() { Gdx.input.setInputProcessor(null); }

    @Override public void dispose() {
        if (stage != null) stage.dispose();
        if (theme != null) theme.dispose();
        stage = null;
        theme = null;
        skin = null;
    }

}
