package com.github.thedragonconquerors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.client.client.NetworkClient;

public class MenuScreen extends ScreenAdapter {
    private final Main game;
    private Stage stage;
    private Skin skin;
    private Label statusLabel;
    private TextField urlField;
    private TextButton hostButton;
    private TextButton joinButton;

    public MenuScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        skin = createSkin();

        Table root = new Table();
        root.setFillParent(true);
        root.pad(36f);
        stage.addActor(root);

        Label title = new Label("The Dragon Conquerors", skin, "title");
        Label subtitle = new Label("Host a local match or join an existing server.", skin);
        statusLabel = new Label("Enter a WebSocket URL, or host locally.", skin);
        statusLabel.setWrap(true);

        urlField = new TextField(Main.DEFAULT_SERVER_URL, skin);
        urlField.setMessageText("ws://host:port/ws");

        hostButton = new TextButton("Host Game", skin);
        joinButton = new TextButton("Join Game", skin);

        hostButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hostGame();
            }
        });

        joinButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                joinGame(urlField.getText());
            }
        });

        root.defaults().width(520f).padBottom(14f);
        root.add(title).row();
        root.add(subtitle).row();
        root.add(urlField).height(48f).row();
        root.add(joinButton).height(48f).row();
        root.add(hostButton).height(48f).row();
        root.add(statusLabel).width(520f).minHeight(56f).row();

        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.06f, 0.07f, 0.09f, 1f);
        stage.act(delta);
        stage.draw();
    }

    private void hostGame() {
        hostButton.setDisabled(true);
        joinButton.setDisabled(true);
        statusLabel.setText("Starting local server...");

        Thread hostThread = new Thread(() -> {
            try {
                String joinUrl = game.getLocalJoinUrl();
                game.startLocalServer();
                NetworkClient client = connectWithRetry(Main.DEFAULT_SERVER_URL);
                Gdx.app.postRunnable(() -> game.startLobby(client, joinUrl));
            } catch (Exception e) {
                Gdx.app.postRunnable(() -> {
                    hostButton.setDisabled(false);
                    joinButton.setDisabled(false);
                    statusLabel.setText("Could not start server: " + e.getMessage());
                });
            }
        }, "tdc-server-start");
        hostThread.setDaemon(true);
        hostThread.start();
    }

    private void joinGame(String url) {
        String trimmedUrl = url == null ? "" : url.trim();
        if (trimmedUrl.isEmpty()) {
            statusLabel.setText("Enter a WebSocket URL before joining.");
            return;
        }

        joinButton.setDisabled(true);
        hostButton.setDisabled(true);
        statusLabel.setText("Connecting to " + trimmedUrl + "...");

        Thread joinThread = new Thread(() -> {
            try {
                NetworkClient client = game.connectToServer(trimmedUrl);
                Gdx.app.postRunnable(() -> game.startLobby(client, trimmedUrl));
            } catch (Exception e) {
                Gdx.app.postRunnable(() -> {
                    joinButton.setDisabled(false);
                    hostButton.setDisabled(false);
                    statusLabel.setText("Could not connect: " + e.getMessage());
                });
            }
        }, "tdc-server-join");
        joinThread.setDaemon(true);
        joinThread.start();
    }

    private NetworkClient connectWithRetry(String url) throws Exception {
        Exception lastError = null;
        for(int attempt = 0; attempt < 30; attempt++) {
            try {
                return game.connectToServer(url);
            } catch (Exception e) {
                lastError = e;
                Thread.sleep(500L);
            }
        }

        throw lastError == null ? new IllegalStateException("Could not connect to local server") : lastError;
    }

    private Skin createSkin() {
        Skin skin = new Skin();
        BitmapFont font = new BitmapFont();
        skin.add("default-font", font);

        Texture white = createTexture(Color.WHITE);
        Texture panel = createTexture(new Color(0.11f, 0.13f, 0.16f, 1f));
        Texture accent = createTexture(new Color(0.73f, 0.22f, 0.18f, 1f));
        Texture accentDisabled = createTexture(new Color(0.36f, 0.24f, 0.24f, 1f));
        Texture field = createTexture(new Color(0.17f, 0.19f, 0.23f, 1f));

        skin.add("white", white);
        skin.add("panel", panel);
        skin.add("accent", accent);
        skin.add("accent-disabled", accentDisabled);
        skin.add("field", field);

        Label.LabelStyle labelStyle = new Label.LabelStyle(font, new Color(0.88f, 0.9f, 0.92f, 1f));
        skin.add("default", labelStyle);

        BitmapFont titleFont = new BitmapFont();
        titleFont.getData().setScale(1.8f);
        skin.add("title", new Label.LabelStyle(titleFont, Color.WHITE));

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.up = new TextureRegionDrawable(accent);
        buttonStyle.down = new TextureRegionDrawable(createTexture(new Color(0.55f, 0.14f, 0.12f, 1f)));
        buttonStyle.disabled = new TextureRegionDrawable(accentDisabled);
        buttonStyle.disabledFontColor = new Color(0.75f, 0.75f, 0.75f, 1f);
        skin.add("default", buttonStyle);

        TextField.TextFieldStyle fieldStyle = new TextField.TextFieldStyle();
        fieldStyle.font = font;
        fieldStyle.fontColor = Color.WHITE;
        fieldStyle.messageFontColor = new Color(0.6f, 0.64f, 0.7f, 1f);
        fieldStyle.background = new TextureRegionDrawable(field);
        fieldStyle.cursor = new TextureRegionDrawable(white);
        fieldStyle.selection = new TextureRegionDrawable(accent);
        skin.add("default", fieldStyle);

        return skin;
    }

    private Texture createTexture(Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
    }
}
