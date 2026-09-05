// File Location: core/src/main/java/com/github/thedragonconquerors/PostMatchScreen.java
package com.github.thedragonconquerors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
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
import com.shared.shared.model.Action;
import com.shared.shared.model.CharacterBuild;
import com.shared.shared.model.Packet;
import com.shared.shared.model.world.Environment;
import com.shared.shared.network.MatchState;
import com.shared.shared.network.PlayerCombatState;

import java.util.ArrayList;
import java.util.List;

/** Dedicated server-backed victory, defeat, draw, and rematch screen. */
public final class PostMatchScreen extends ScreenAdapter {
    private final Main game;
    private final int teamIndex;
    private final CharacterBuild chosenBuild;
    private final Environment environment;
    private final int localPlayerId;
    private final MatchState finalState;
    private final boolean testingMode;
    private int connectedPlayers;

    private Stage stage;
    private FantasyUiTheme theme;
    private Skin skin;
    private Label voteLabel;
    private Label statusLabel;
    private TextButton rematchButton;
    private boolean rematchRequested;
    private boolean restarting;

    public PostMatchScreen(Main game, int teamIndex, CharacterBuild chosenBuild,
                           Environment environment, int localPlayerId,
                           MatchState finalState) {
        this.game = game;
        this.teamIndex = teamIndex;
        this.chosenBuild = chosenBuild;
        this.environment = environment;
        this.localPlayerId = localPlayerId;
        this.finalState = finalState;
        this.testingMode = finalState != null && finalState.isTestingMode();
        this.connectedPlayers = finalState == null ? 4 : finalState.getPlayers().size();
    }

    @Override
    public void show() {
        if (stage == null) buildUi();
        game.getNetworkClient().setPacketHandler(
            packet -> Gdx.app.postRunnable(() -> handlePacket(packet)));
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
        root.center();
        stage.addActor(root);

        Table panel = new Table();
        panel.setBackground(theme.panel());
        panel.pad(42f, 58f, 42f, 58f);

        Label eyebrow = new Label("BATTLE CONCLUDED", skin, "section");
        Label result = new Label(resultTitle(), skin, "title");
        result.setColor(resultColor());
        Label summary = new Label(resultSummary(), skin, "subtitle");
        summary.setWrap(true);
        summary.setAlignment(Align.center);

        voteLabel = new Label("Rematch votes: 0/" + requiredVotes(), skin, "class-role");
        statusLabel = new Label(
            testingMode ? "All connected players must agree to restart this test."
                : "All four players must agree to restart this battlefield.", skin, "caption");
        statusLabel.setWrap(true);
        statusLabel.setAlignment(Align.center);

        rematchButton = new TextButton("VOTE REMATCH", skin, "primary");
        rematchButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                requestRematch();
            }
        });
        TextButton menuButton = new TextButton("RETURN TO MENU", skin, "secondary");
        menuButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                game.returnToMenu();
            }
        });

        Table buttons = new Table();
        buttons.add(rematchButton).width(220f).height(52f).padRight(14f);
        buttons.add(menuButton).width(220f).height(52f);

        panel.add(eyebrow).padBottom(14f).row();
        panel.add(result).padBottom(18f).row();
        panel.add(summary).width(610f).padBottom(28f).row();
        panel.add(voteLabel).padBottom(10f).row();
        panel.add(statusLabel).width(600f).padBottom(28f).row();
        panel.add(buttons).row();
        root.add(panel).width(760f);
    }

    private void requestRematch() {
        if (rematchRequested || restarting) return;
        rematchRequested = true;
        rematchButton.setDisabled(true);
        rematchButton.setText("VOTE SUBMITTED");
        statusLabel.setText("Waiting for the rest of the room...");
        statusLabel.setColor(FantasyUiTheme.SUCCESS);
        game.getNetworkClient().requestRematch(localPlayerId);
    }

    private void handlePacket(Packet packet) {
        if (packet == null || packet.getAction() == null || restarting) return;
        switch (packet.getAction()) {
            case REMATCH_UPDATE:
                connectedPlayers = packet.getConnectedPlayers();
                if (testingMode && packet.getID() >= 0) {
                    rematchRequested = false;
                    rematchButton.setDisabled(false);
                    rematchButton.setText("VOTE REMATCH");
                }
                voteLabel.setText("Rematch votes: " + packet.getRematchVotes() + "/" + requiredVotes());
                if (packet.getMessage() != null) statusLabel.setText(packet.getMessage());
                if (!testingMode && packet.getConnectedPlayers() < 4) {
                    statusLabel.setColor(FantasyUiTheme.ERROR);
                    rematchButton.setDisabled(true);
                }
                break;
            case REMATCH_START:
                beginRematch(packet);
                break;
            case LEAVE:
                if (testingMode) {
                    statusLabel.setText("A player left. The remaining players can restart the test.");
                    break;
                }
                statusLabel.setText("A player left. A 2v2 rematch is no longer available.");
                statusLabel.setColor(FantasyUiTheme.ERROR);
                rematchButton.setDisabled(true);
                break;
            case ERROR:
                if (testingMode) {
                    rematchRequested = false;
                    rematchButton.setDisabled(false);
                    rematchButton.setText("VOTE REMATCH");
                }
                statusLabel.setText(packet.getMessage() == null
                    ? "The rematch vote was rejected." : packet.getMessage());
                statusLabel.setColor(FantasyUiTheme.ERROR);
                break;
            default:
                break;
        }
    }

    private void beginRematch(Packet packet) {
        MatchState state = packet.getMatchState();
        if (state == null || state.isMatchOver()) {
            statusLabel.setText("The server returned an invalid rematch state.");
            statusLabel.setColor(FantasyUiTheme.ERROR);
            return;
        }
        restarting = true;
        Environment nextEnvironment = packet.getEnvironment() == null
            ? environment : packet.getEnvironment();
        game.startGame(teamIndex, chosenBuild, nextEnvironment, localPlayerId,
            rosterFrom(state), state);
    }

    private List<Packet> rosterFrom(MatchState state) {
        List<Packet> roster = new ArrayList<>();
        for (PlayerCombatState player : state.getPlayers()) {
            roster.add(Packet.builder()
                .ID(player.getId())
                .username(player.getUsername())
                .teamIndex(player.getTeamIndex())
                .characterClass(player.getCharacterClass())
                .race(player.getRace())
                .finalPosition(player.getPosition() == null
                    ? null : new Vector2(player.getPosition()))
                .action(Action.JOIN)
                .build());
        }
        return roster;
    }

    private String resultTitle() {
        if (finalState == null || finalState.getWinningTeam() == 0) return "DRAW";
        return finalState.getWinningTeam() == teamIndex ? "VICTORY" : "DEFEAT";
    }

    private int requiredVotes() { return testingMode ? connectedPlayers : 4; }

    private String resultSummary() {
        if (finalState != null && finalState.getMessage() != null
            && !finalState.getMessage().isBlank()) return finalState.getMessage();
        return "The battle has ended.";
    }

    private com.badlogic.gdx.graphics.Color resultColor() {
        if (finalState == null || finalState.getWinningTeam() == 0) return FantasyUiTheme.GOLD;
        return finalState.getWinningTeam() == teamIndex
            ? FantasyUiTheme.SUCCESS : FantasyUiTheme.ERROR;
    }

    @Override public void render(float delta) {
        ScreenUtils.clear(0.025f, 0.022f, 0.028f, 1f);
        stage.act(Math.min(delta, 1f / 30f));
        stage.draw();
    }

    @Override public void resize(int width, int height) {
        if (stage != null) stage.getViewport().update(width, height, true);
    }

    @Override public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override public void dispose() {
        if (stage != null) stage.dispose();
        if (theme != null) theme.dispose();
        stage = null;
        theme = null;
        skin = null;
    }
}
