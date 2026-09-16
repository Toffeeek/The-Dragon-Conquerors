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
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
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

/** Class -> race -> test start (or environment voting) for the multiplayer lobby. */
public class LobbyScreen extends ScreenAdapter {
    private enum Step { CLASS, RACE, ENVIRONMENT }

    private final Main game;
    private final String joinUrl;
    private final Map<Integer, Packet> roster = new LinkedHashMap<>();
    private final Map<Environment, Integer> voteCounts = new EnumMap<>(Environment.class);

    private Stage stage;
    private FantasyUiTheme theme;
    private Skin skin;
    private com.github.thedragonconquerors.ui.PixelIcons icons;
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
    private boolean testingMode = true;
    private boolean roomReady;
    private boolean startRequested;

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
        icons = new com.github.thedragonconquerors.ui.PixelIcons();
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
        com.github.thedragonconquerors.ui.UiMotion.reveal(root);

        root.add(createTopBar()).growX().height(72f).row();
        Table content = new Table();
        content.add(createSessionPanel()).width(340f).growY();
        selectionHost = new Table();
        content.add(selectionHost).expand().fill().padLeft(24f);
        // Keep navigation visible even when larger fonts or wrapped status text
        // make the selection content taller than the available window space.
        ScrollPane.ScrollPaneStyle scrollStyle = new ScrollPane.ScrollPaneStyle();
        scrollStyle.vScroll = ((TextureRegionDrawable) theme.divider()).tint(FantasyUiTheme.GOLD_DIM);
        scrollStyle.vScrollKnob = theme.divider();
        ScrollPane contentScroll = new ScrollPane(content, scrollStyle);
        contentScroll.setScrollingDisabled(true, false);
        contentScroll.setOverscroll(false, false);
        contentScroll.setFadeScrollBars(false);
        root.add(contentScroll).grow().minHeight(0f).padTop(22f).row();
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

        Label title = new Label("Gather the party", skin, "heading");
        title.setAlignment(Align.center);
        TextButton copyButton = new TextButton("COPY ADDRESS", skin, "secondary");
        copyButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                Gdx.app.getClipboard().setContents(joinUrl);
                copyStatusLabel.setText("Address copied");
                copyStatusLabel.setColor(FantasyUiTheme.SUCCESS);
            }
        });

        bar.add(leaveButton).width(210f).height(42f).left();
        bar.add(title).expandX().center();
        bar.add(copyButton).width(240f).height(42f).right();
        return bar;
    }

    private Table createSessionPanel() {
        Table panel = new Table();
        panel.setBackground(theme.panel());
        panel.pad(18f);
        panel.top().left();

        Label heading = new Label("Your hero", skin, "heading");
        Label connected = new Label("CONNECTED", skin, "status");
        connected.setColor(FantasyUiTheme.SUCCESS);
        Label address = new Label(joinUrl, skin, "caption");
        address.setWrap(true);
        copyStatusLabel = new Label("", skin, "caption");
        copyStatusLabel.setWrap(true);

        Label nameHeading = new Label("PLAYER NAME", skin, "section");
        usernameField = new TextField(defaultUsername(), skin);
        usernameField.setMaxLength(24);

        Label teamHeading = new Label("CHOOSE TEAM", skin, "section");
        azureTeamButton = new TextButton("AZURE", skin, "team-blue");
        crimsonTeamButton = new TextButton("CRIMSON", skin, "team-red");
        azureTeamButton.setChecked(true);
        ButtonGroup<TextButton> teamGroup =
            new ButtonGroup<>(azureTeamButton, crimsonTeamButton);
        teamGroup.setMinCheckCount(1);
        teamGroup.setMaxCheckCount(1);
        azureTeamButton.addListener(teamListener(1));
        crimsonTeamButton.addListener(teamListener(2));

        Table teams = new Table();
        teams.defaults().width(270f).height(40f).padBottom(6f);
        teams.add(azureTeamButton).row();
        teams.add(crimsonTeamButton).row();

        Table playerCard = new Table();
        playerCard.setBackground(theme.inset());
        playerCard.pad(12f);
        selectionSummaryLabel = new Label("", skin, "default");
        selectionSummaryLabel.setWrap(true);
        playerCard.add(new Label("YOUR BUILD", skin, "section")).left().row();
        playerCard.add(selectionSummaryLabel).width(240f).left().padTop(7f).row();

        lobbyStatusLabel = new Label("", skin, "caption");
        lobbyStatusLabel.setWrap(true);

        panel.add(heading).left().row();
        panel.add(connected).left().padTop(5f).row();
        panel.add(address).width(270f).left().padTop(10f).row();
        panel.add(copyStatusLabel).width(270f).left().padTop(5f).row();
        panel.add(nameHeading).left().padTop(12f).row();
        panel.add(usernameField).width(270f).height(42f).padTop(5f).row();
        panel.add(teamHeading).left().padTop(12f).padBottom(8f).row();
        panel.add(teams).left().row();
        panel.add(playerCard).width(270f).left().padTop(8f).row();
        panel.add(lobbyStatusLabel).width(270f).left().padTop(8f).row();
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
                footerHelpLabel.setText("01  CLASS");
                break;
            case RACE:
                selectionHost.add(createRacePanel()).grow();
                backButton.setDisabled(joined);
                nextButton.setDisabled(joined);
                nextButton.setText("JOIN GAME");
                footerHelpLabel.setText("02  RACE");
                break;
            case ENVIRONMENT:
                if (testingMode || localPlayerId < 0) {
                    selectionHost.add(createTestingPanel()).grow();
                    backButton.setDisabled(true);
                    nextButton.setDisabled(!roomReady || startRequested);
                    nextButton.setText(!roomReady ? "JOINING..."
                        : startRequested ? "STARTING..." : "START BATTLE");
                    footerHelpLabel.setText("03  BATTLEFIELD");
                    break;
                }
                selectionHost.add(createEnvironmentPanel()).grow();
                backButton.setDisabled(true);
                nextButton.setDisabled(true);
                nextButton.setText(voteSent ? "VOTE SUBMITTED" : "JOINING...");
                footerHelpLabel.setText("03  BATTLEFIELD");
                break;
            default:
                throw new IllegalStateException("Unknown selection step: " + step);
        }
        updateSelectionSummary();
        com.github.thedragonconquerors.ui.UiMotion.reveal(selectionHost);
    }

    private Table panel(String title) {
        Table panel = new Table();
        panel.setBackground(theme.panel());
        panel.pad(22f, 26f, 22f, 26f);
        panel.top().left();
        panel.add(new Label(title, skin, "heading")).left().padBottom(18).row();
        return panel;
    }

    private Table createClassPanel() {
        Table panel = panel("Choose your class");
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
            button.clearChildren();
            button.add(new com.github.thedragonconquerors.ui.CharacterPortrait(game.getAssetService(),value,false)).size(60,70).row();
            button.add(button.getLabel()).width(200).growY();
            button.setChecked(value == selectedClass);
            button.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    selectedClass = value;
                    Gdx.app.postRunnable(() -> showStep());
                }
            });
            group.add(button);
            grid.add(button).width(224f).minHeight(144f).fillY().pad(5f);
            if ((index + 1) % 3 == 0) grid.row();
        }
        panel.add(grid).left().row();

        Table details = detailBox();
        details.add(createClassTierTable(selectedClass)).left().padTop(10f).row();
        Table abilities=new Table();int abilityIndex=0;
        for(var ability:AbilityType.forClass(selectedClass)) {
            Table row=new Table();row.add(new Image(icons.drawable(com.github.thedragonconquerors.ui.PixelIcons.kind(ability)))).size(22).padRight(8);
            row.add(new Label(ability.getDisplayName(),skin,"caption")).left().growX();
            abilities.add(row).width(325).padBottom(6).left();if(++abilityIndex%2==0)abilities.row();
        }
        details.add(abilities).width(660).left().padTop(12).row();
        panel.add(details).width(700f).left().padTop(12f).row();
        return panel;
    }

    private Table createRacePanel() {
        Table panel = panel("Choose your lineage");
        Table grid = new Table();
        ButtonGroup<TextButton> group = new ButtonGroup<>();
        group.setMinCheckCount(1);
        group.setMaxCheckCount(1);
        Race[] races = Race.values();
        for (int index = 0; index < races.length; index++) {
            Race value = races[index];
            CharacterBuild preview = CharacterBuild.of(value, selectedClass);
            String marker = preview.isNamedSynergy() ? "SYNERGY" : "";
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
            grid.add(button).width(330f).minHeight(76f).pad(5f);
            if ((index + 1) % 2 == 0) grid.row();
        }
        panel.add(grid).left().row();

        CharacterBuild build = CharacterBuild.of(selectedRace, selectedClass);
        Table details = detailBox();
        Label boosts = new Label(build.describeBoosts(), skin, "status");
        boosts.setWrap(true);
        boosts.setColor(FantasyUiTheme.SUCCESS);
        details.add(boosts).width(650f).left().padTop(8f).row();
        details.add(createBoostedStatsTable(build.createBaseStats(), build.createStats()))
            .left().padTop(10f).row();
        panel.add(details).width(700f).left().padTop(12f).row();
        return panel;
    }

    private Table createEnvironmentPanel() {
        Table panel = panel("Choose the battlefield");
        Table cards = new Table();
        ButtonGroup<TextButton> group = new ButtonGroup<>();
        group.setMinCheckCount(0);
        group.setMaxCheckCount(1);
        for (Environment environment : Environment.selectionOrder()) {
            int votes = voteCounts.getOrDefault(environment, 0);
            String cardText = environment.name() + "\n" + hazardLabel(environment) + "\n" + votes + " votes";
            TextButton button = new TextButton(cardText, skin, "class-card");
            addMapThumbnail(button,environment);
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
            cards.add(button).width(222f).minHeight(205f).fillY().pad(5f);
        }
        panel.add(cards).left().row();
        panel.add(createPartySlots()).left().padTop(10).row();

        Table details = detailBox();
        String choice = selectedEnvironment == null
            ? "Select a battlefield"
            : "Your vote: " + selectedEnvironment.name();
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

    private Table createTestingPanel() {
        Table panel = panel("Choose the battlefield");
        Table cards = new Table();
        ButtonGroup<TextButton> group = new ButtonGroup<>();
        group.setMinCheckCount(0);
        group.setMaxCheckCount(1);
        Environment choice = selectedEnvironment == null ? Environment.CANYON : selectedEnvironment;
        for (Environment environment : Environment.selectionOrder()) {
            TextButton button = new TextButton(environment.name()
                + "\n" + hazardLabel(environment), skin, "class-card");
            addMapThumbnail(button,environment);
            button.getLabel().setWrap(true);
            button.getLabel().setAlignment(Align.center);
            group.add(button);
            button.setChecked(environment == choice);
            button.setDisabled(!roomReady || startRequested);
            button.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    if (!roomReady || startRequested) return;
                    selectedEnvironment = environment;
                    Gdx.app.postRunnable(() -> showStep());
                }
            });
            cards.add(button).width(222f).minHeight(195f).pad(5f);
        }
        panel.add(cards).left().row();
        panel.add(createPartySlots()).left().padTop(10).row();
        panel.add(new Label(connectedPlayers+" / 4 players  |  Solo ready",skin,"caption")).left().padTop(14).row();
        return panel;
    }

    private static String hazardLabel(Environment environment) {
        return switch(environment) {case CANYON->"Fatal falls";case LAVA->"Burn / lethal lava";case BOG->"Poison";};
    }

    private void addMapThumbnail(TextButton button,Environment environment) {
        Image image=new Image(game.getAssetService().load(com.github.thedragonconquerors.assets.BattlefieldImageAssets.forEnvironment(environment)));
        image.setScaling(Scaling.fit);button.clearChildren();
        button.add(image).size(202,116).padBottom(10).row();
        button.add(button.getLabel()).width(200).growY();
    }
    private Table createPartySlots() {
        Table parties=new Table();
        for(int team=1;team<=2;team++) {
            Table party=new Table();party.setBackground(theme.inset());party.pad(9);
            party.add(new Label((team==1?"A / AZURE":"B / CRIMSON")+(team==selectedTeam?"  - YOU":""),skin,"section")).left().row();
            final int teamId=team;
            var members=roster.values().stream().filter(p->p.getTeamIndex()==teamId).toList();
            for(int slot=0;slot<2;slot++) {
                Table row=new Table();
                if(slot<members.size()) {
                    Packet player=members.get(slot);
                    row.add(new com.github.thedragonconquerors.ui.CharacterPortrait(game.getAssetService(),player.getCharacterClass(),false)).size(30,38);
                    Label label=new Label((player.getID()==localPlayerId?"YOU - ":"")+player.getUsername(),skin,"caption");label.setEllipsis(true);
                    row.add(label).width(260).left();
                } else row.add(new Label("Open slot",skin,"caption")).width(290).height(38).left();
                party.add(row).left().row();
            }
            parties.add(party).width(340).padRight(10);
        }
        return parties;
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
        int index = 0;
        for (StatType stat : StatType.values()) {
            table.add(new Label(stat.getDisplayName(), skin, "caption")).width(92f).left();
            Label value = new Label(Integer.toString(characterClass.getTier(stat)), skin, "default");
            value.setColor(FantasyUiTheme.SUCCESS);
            table.add(value).width(44f).left();
            index++;
            if (index % 3 == 0) table.row();
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
        } else if (step == Step.ENVIRONMENT && testingMode && roomReady && !startRequested) {
            startRequested = true;
            game.getNetworkClient().startTestMatch(localPlayerId, selectedEnvironment);
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
        if (testingMode || localPlayerId < 0 || selectedEnvironment == null) return;
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
                roster.clear();
                localPlayerId = packet.getID();
                testingMode = packet.isTestingMode();
                connectedPlayers = Math.max(connectedPlayers, packet.getConnectedPlayers());
                String roomName = packet.getRoomId() == null ? "match room" : packet.getRoomId();
                lobbyStatusLabel.setText("Assigned to " + roomName + " as player "
                    + (localPlayerId + 1) + (testingMode ? ". Ready to test." : ". Cast your vote."));
                lobbyStatusLabel.setColor(FantasyUiTheme.SUCCESS);
                submitVoteIfReady();
                showStep();
                break;
            case ROOM_READY:
                roomReady = true;
                if (packet.getMatchState() == null) startRequested = false;
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
                connectedPlayers=roster.size();
                if(step==Step.ENVIRONMENT)showStep();
                break;
            case ERROR:
                if (localPlayerId >= 0) {
                    startRequested = false;
                    lobbyStatusLabel.setText(packet.getMessage());
                    lobbyStatusLabel.setColor(FantasyUiTheme.ERROR);
                    showStep();
                    break;
                }
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

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.02f, 0.018f, 0.022f, 1f);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) && !joined) {
            if (step == Step.CLASS) game.returnToMenu(); else previousStep();
            return;
        }
        stage.act(Math.min(delta, 1f / 15f));
        if(theme.refreshTextScale())for(var actor:stage.getActors())com.github.thedragonconquerors.ui.UiMotion.relayout(actor);
        stage.draw();
    }

    @Override public void resize(int width, int height) {
        if (stage != null) stage.getViewport().update(width, height, true);
    }

    @Override public void hide() { Gdx.input.setInputProcessor(null); }

    @Override public void dispose() {
        if (stage != null) stage.dispose();
        if (theme != null) theme.dispose();
        if (icons != null) icons.dispose();
        stage = null;
        theme = null;
        skin = null;
        icons = null;
    }

}
