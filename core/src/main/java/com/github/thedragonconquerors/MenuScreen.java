package com.github.thedragonconquerors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
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
import com.client.client.NetworkClient;

import java.time.Duration;

/** Main menu with host/join controls connected to Main's existing networking API. */
public class MenuScreen extends ScreenAdapter {
    private final Main game;

    private Stage stage;
    private FantasyUiTheme theme;
    private Skin skin;

    private TextField addressField;
    private TextButton hostButton;
    private TextButton joinButton;
    private TextButton displayButton;
    private TextButton quitButton;
    private Label statusLabel;

    private volatile boolean busy;
    private boolean disposed;

    public MenuScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        if (stage == null) {
            buildUi();
        }

        disposed = false;
        setBusy(false);
        setStatus("Ready. Host a campaign or enter a server address.",
            FantasyUiTheme.TEXT_MUTED);
        updateDisplayButton();
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
        root.pad(48f, 58f, 42f, 58f);
        stage.addActor(root);

        Table hero = createHeroPanel();
        Table menu = createMenuPanel();

        root.add(hero).width(650f).expandY().fillY().left();
        root.add(menu).width(452f).expandY().fillY().right().padLeft(62f);
    }

    private Table createHeroPanel() {
        Table hero = new Table();
        hero.left().top();
        hero.pad(52f, 38f, 46f, 38f);

        Label eyebrow = new Label("TURN-BASED MULTIPLAYER RPG", skin, "eyebrow");
        Label title = new Label("THE DRAGON\nCONQUERORS", skin, "title");
        title.setAlignment(Align.left);

        Image divider = new Image(theme.divider());

        Label subtitle = new Label(
            "Gather your party. Choose a class.\nConquer the dungeon one turn at a time.",
            skin, "subtitle");
        subtitle.setAlignment(Align.left);

        Table features = new Table();
        features.setBackground(theme.inset());
        features.pad(18f, 20f, 18f, 20f);
        features.left();

        addFeature(features, "TACTICAL COMBAT", "Movement, range and positioning decide every encounter.");
        addFeature(features, "CLASS-BASED PARTY", "Warrior, Mage, Archer, Paladin and Rogue play differently.");
        addFeature(features, "MULTIPLAYER", "Host locally or join another player's campaign over WebSocket.");

        Label buildLabel = new Label(
            "DEVELOPMENT BUILD  |  THE DRAGON CONQUERORS",
            skin, "caption");

        hero.add(eyebrow).left().padTop(28f).row();
        hero.add(title).left().padTop(10f).row();
        hero.add(divider).left().width(230f).height(3f).padTop(22f).row();
        hero.add(subtitle).left().padTop(22f).row();
        hero.add(features).width(540f).left().padTop(42f).row();
        hero.add().expandY().row();
        hero.add(buildLabel).left();
        return hero;
    }

    private void addFeature(Table table, String heading, String description) {
        Label headingLabel = new Label(heading, skin, "section");
        Label descriptionLabel = new Label(description, skin, "caption");
        descriptionLabel.setWrap(true);

        table.add(headingLabel).left().padBottom(4f).row();
        table.add(descriptionLabel).width(490f).left().padBottom(16f).row();
    }

    private Table createMenuPanel() {
        Table panel = new Table();
        panel.setBackground(theme.panel());
        panel.pad(34f, 34f, 30f, 34f);
        panel.top();

        Label heading = new Label("MAIN MENU", skin, "heading");
        heading.setAlignment(Align.center);
        Label hint = new Label("Create a lobby or join an existing campaign.", skin, "caption");
        hint.setAlignment(Align.center);

        hostButton = new TextButton("HOST CAMPAIGN", skin, "primary");
        hostButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hostGame();
            }
        });

        Label addressLabel = new Label("SERVER ADDRESS", skin, "section");
        addressField = new TextField("", skin);
        addressField.setMessageText("Paste host address, e.g. ws://192.168.1.10:8080/ws");

        joinButton = new TextButton("JOIN CAMPAIGN", skin, "secondary");
        joinButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                joinGame(addressField.getText());
            }
        });

        Table statusBox = new Table();
        statusBox.setBackground(theme.inset());
        statusBox.pad(12f, 14f, 12f, 14f);
        statusLabel = new Label("", skin, "status");
        statusLabel.setWrap(true);
        statusLabel.setAlignment(Align.center);
        statusBox.add(statusLabel).width(350f).minHeight(38f);

        displayButton = new TextButton("", skin, "quiet");
        displayButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                toggleDisplayMode();
            }
        });

        quitButton = new TextButton("QUIT", skin, "danger");
        quitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!busy) Gdx.app.exit();
            }
        });

        Table utilityRow = new Table();
        utilityRow.defaults().height(42f);
        utilityRow.add(displayButton).width(220f);
        utilityRow.add(quitButton).width(120f).padLeft(10f);

        panel.add(heading).growX().padTop(4f).row();
        panel.add(hint).growX().padTop(8f).padBottom(26f).row();
        panel.add(hostButton).width(350f).height(58f).row();
        panel.add(new Image(theme.divider())).width(300f).height(2f).padTop(24f).padBottom(22f).row();
        panel.add(addressLabel).width(350f).left().padBottom(7f).row();
        panel.add(addressField).width(350f).height(50f).row();
        panel.add(joinButton).width(350f).height(54f).padTop(12f).row();
        panel.add(statusBox).width(378f).padTop(22f).row();
        panel.add().expandY().row();
        panel.add(utilityRow).padBottom(2f);
        return panel;
    }

    private void hostGame() {
        if (busy) return;
        setBusy(true);
        setStatus("Opening the local campaign server...", FantasyUiTheme.GOLD);

        Thread hostThread = new Thread(() -> {
            try {
                game.startLocalServer();
                if (!game.waitForLocalServer(Duration.ofSeconds(30))) {
                    throw new IllegalStateException("The local server did not become ready.");
                }

                NetworkClient client = connectWithRetry(game.getLocalServerUrl());
                String joinUrl = game.getLocalJoinUrl();
                postToRenderThread(() -> game.startLobby(client, joinUrl));
            } catch (Exception exception) {
                game.stopLocalServer();
                postFailure("Could not host the campaign", exception);
            }
        }, "tdc-menu-host");
        hostThread.setDaemon(true);
        hostThread.start();
    }

    private void joinGame(String rawAddress) {
        if (busy) return;

        String address;
        try {
            address = normalizeServerUrl(rawAddress);
        } catch (IllegalArgumentException exception) {
            setStatus(exception.getMessage(), FantasyUiTheme.ERROR);
            return;
        }

        addressField.setText(address);
        setBusy(true);
        setStatus("Connecting to " + address + "...", FantasyUiTheme.GOLD);

        Thread joinThread = new Thread(() -> {
            try {
                NetworkClient client = game.connectToServer(address);
                postToRenderThread(() -> game.startLobby(client, address));
            } catch (Exception exception) {
                postFailure("Could not join the campaign", exception);
            }
        }, "tdc-menu-join");
        joinThread.setDaemon(true);
        joinThread.start();
    }

    private NetworkClient connectWithRetry(String url) throws Exception {
        Exception lastError = null;
        for (int attempt = 0; attempt < 30; attempt++) {
            try {
                return game.connectToServer(url);
            } catch (Exception exception) {
                lastError = exception;
                Thread.sleep(400L);
            }
        }

        if (lastError != null) throw lastError;
        throw new IllegalStateException("The local client could not connect.");
    }

    private String normalizeServerUrl(String rawAddress) {
        String value = rawAddress == null ? "" : rawAddress.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Enter the host address before joining.");
        }

        if (!value.startsWith("ws://") && !value.startsWith("wss://")) {
            value = "ws://" + value;
        }

        int authorityStart = value.indexOf("://") + 3;
        int pathStart = value.indexOf('/', authorityStart);
        String authority = pathStart < 0
            ? value.substring(authorityStart)
            : value.substring(authorityStart, pathStart);

        if (authority.isEmpty()) {
            throw new IllegalArgumentException("The server address is not valid.");
        }

        // Add the project's default port for convenient inputs such as localhost
        // or 192.168.1.10. Bracketed IPv6 and explicit ports are left unchanged.
        if (!authority.startsWith("[") && authority.indexOf(':') < 0) {
            String suffix = pathStart < 0 ? "" : value.substring(pathStart);
            value = value.substring(0, authorityStart)
                + authority + ":" + Main.SERVER_PORT + suffix;
            pathStart = value.indexOf('/', authorityStart);
        }

        if (pathStart < 0) {
            value += "/ws";
        } else if (value.substring(pathStart).equals("/")) {
            value += "ws";
        }

        return value;
    }

    private void postFailure(String prefix, Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = exception.getClass().getSimpleName();
        }
        final String finalMessage = prefix + ": " + message;
        postToRenderThread(() -> {
            setBusy(false);
            setStatus(finalMessage, FantasyUiTheme.ERROR);
        });
    }

    private void postToRenderThread(Runnable runnable) {
        Gdx.app.postRunnable(() -> {
            if (!disposed) runnable.run();
        });
    }

    private void setBusy(boolean value) {
        busy = value;
        if (hostButton != null) hostButton.setDisabled(value);
        if (joinButton != null) joinButton.setDisabled(value);
        if (displayButton != null) displayButton.setDisabled(value);
        if (quitButton != null) quitButton.setDisabled(value);
        if (addressField != null) addressField.setDisabled(value);
    }

    private void setStatus(String text, Color color) {
        if (statusLabel == null) return;
        statusLabel.setText(text);
        statusLabel.setColor(color);
    }

    private void toggleDisplayMode() {
        if (busy) return;
        if (Gdx.graphics.isFullscreen()) {
            Gdx.graphics.setWindowedMode(1280, 720);
        } else {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        }
        updateDisplayButton();
    }

    private void updateDisplayButton() {
        if (displayButton == null) return;
        displayButton.setText(Gdx.graphics.isFullscreen()
            ? "DISPLAY: FULLSCREEN"
            : "DISPLAY: WINDOWED");
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.02f, 0.018f, 0.022f, 1f);

        if (!busy && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }
        if (!busy
            && Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            && stage.getKeyboardFocus() == addressField) {
            joinGame(addressField.getText());
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
        disposed = true;
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
