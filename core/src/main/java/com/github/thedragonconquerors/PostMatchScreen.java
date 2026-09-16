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
        panel.pad(24f, 32f, 24f, 32f);

        Label result = new Label(resultTitle(), skin, "title");
        Label.LabelStyle resultStyle = new Label.LabelStyle(result.getStyle());
        resultStyle.fontColor = com.badlogic.gdx.graphics.Color.WHITE;
        result.setStyle(resultStyle);
        result.setColor(resultColor());

        voteLabel = new Label("Rematch votes: 0/" + requiredVotes(), skin, "class-role");
        statusLabel = new Label("", skin, "caption");
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
        float textScale=com.github.thedragonconquerors.ui.PresentationSettings.textScale();
        buttons.add(rematchButton).width(220f*textScale).height(52f*textScale).padRight(14f);
        buttons.add(menuButton).width(220f*textScale).height(52f*textScale);
        buttons.setBackground(theme.panel());
        buttons.pad(12);

        panel.add(result).padBottom(8f).row();
        panel.add(createVictors()).padBottom(10).row();
        panel.add(createStatistics()).padBottom(16).row();
        panel.add(voteLabel).padBottom(10f).row();
        panel.add(statusLabel).width(600f).padBottom(28f).row();
        var scrollStyle=new com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle();
        scrollStyle.vScroll=theme.solid(new com.badlogic.gdx.graphics.Color(.1f,.1f,.12f,1));
        scrollStyle.vScroll.setMinWidth(6);
        scrollStyle.vScrollKnob=theme.solid(FantasyUiTheme.GOLD_DIM);
        scrollStyle.vScrollKnob.setMinWidth(6);scrollStyle.vScrollKnob.setMinHeight(22);
        com.badlogic.gdx.scenes.scene2d.ui.ScrollPane scroll=new com.badlogic.gdx.scenes.scene2d.ui.ScrollPane(panel,scrollStyle);
        scroll.setScrollingDisabled(true,false);scroll.setOverscroll(false,false);
        scroll.setFadeScrollBars(false);
        root.add(scroll).width(1040f).maxHeight(550).row();
        root.add(buttons).padTop(12);
        com.github.thedragonconquerors.ui.UiMotion.reveal(panel);
    }
    private Table createVictors() {
        Table winners=new Table();
        if(finalState==null || finalState.getWinningTeam()==0)return winners;
        // Original idle animation plus a restrained celebratory bounce, no new combat state.
        for(var row:finalState.getStatistics()) if(row.getTeamIndex()==finalState.getWinningTeam()) {
            Table card=new Table();
            card.add(new com.github.thedragonconquerors.ui.CharacterPortrait(game.getAssetService(),row.getCharacterClass(),true)).size(62,76).row();
            Label name=new Label(row.getUsername(),skin,"section");name.setEllipsis(true);name.setAlignment(Align.center);
            card.add(name).width(170);winners.add(card).padRight(18);
        }
        return winners;
    }
    private Table createStatistics() {
        Table stats=new Table();stats.setBackground(theme.inset());stats.pad(12);
        String[] titles={"PLAYER","DAMAGE","HEAL","KOs","ENV. KOs","TURNS"};
        for(int i=0;i<titles.length;i++)stats.add(new Label(titles[i],skin,"section")).width(i==0?230:132).left().padBottom(9);
        stats.row();
        if(finalState!=null)for(var row:finalState.getStatistics()) {
            Label name=new Label((row.getTeamIndex()==1?"A / ":"B / ")+row.getUsername(),skin,"default");name.setEllipsis(true);stats.add(name).width(230).left();
            for(int value:new int[]{row.getDamageDealt(),row.getHealingDone(),row.getEliminations(),row.getEnvironmentalKills(),row.getTurnsPlayed()})
                stats.add(new Label(Integer.toString(value),skin,"default")).width(132).left();
            stats.row();
        }
        return stats;
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
            case MATCH_START:
                if (packet.getMatchState() != null && !packet.getMatchState().isMatchOver()) beginRematch(packet);
                else {
                    rematchRequested = false;
                    rematchButton.setDisabled(false);
                    rematchButton.setText("VOTE REMATCH");
                    statusLabel.setText("Connection restored. The completed match is synchronized.");
                }
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

    private com.badlogic.gdx.graphics.Color resultColor() {
        if (finalState == null || finalState.getWinningTeam() == 0) return FantasyUiTheme.GOLD;
        return finalState.getWinningTeam() == teamIndex
            ? FantasyUiTheme.SUCCESS : FantasyUiTheme.ERROR;
    }

    @Override public void render(float delta) {
        ScreenUtils.clear(0.025f, 0.022f, 0.028f, 1f);
        stage.act(Math.min(delta, 1f / 30f));
        if(theme.refreshTextScale())for(var actor:stage.getActors())com.github.thedragonconquerors.ui.UiMotion.relayout(actor);
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
