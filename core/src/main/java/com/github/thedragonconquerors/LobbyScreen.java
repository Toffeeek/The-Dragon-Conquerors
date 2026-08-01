package com.github.thedragonconquerors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.shared.shared.model.CharacterClass;

public class LobbyScreen extends ScreenAdapter {
    private final Main game;
    private final String joinUrl;
    private Stage stage;
    private Skin skin;
    private int selectedTeam = 1;
    private CharacterClass selectedClass = CharacterClass.WARRIOR;

    public LobbyScreen(Main game, String joinUrl) {
        this.game = game;
        this.joinUrl = joinUrl;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        skin = createSkin();

        Table root = new Table();
        root.setFillParent(true);
        root.pad(36f);
        stage.addActor(root);

        Label title = new Label("Lobby", skin, "title");
        Label joinUrlLabel = new Label("Join URL: " + joinUrl, skin);
        joinUrlLabel.setWrap(true);
        Label teamLabel = new Label("Team", skin);
        Label classLabel = new Label("Class", skin);

        Table teamTable = new Table();
        TextButton blueTeam = new TextButton("Blue", skin, "toggle");
        TextButton redTeam = new TextButton("Red", skin, "toggle");
        blueTeam.setChecked(true);

        ButtonGroup<TextButton> teamGroup = new ButtonGroup<>(blueTeam, redTeam);
        teamGroup.setMinCheckCount(1);
        teamGroup.setMaxCheckCount(1);

        blueTeam.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectedTeam = 1;
            }
        });
        redTeam.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectedTeam = 2;
            }
        });

        teamTable.defaults().width(160f).height(44f).pad(4f);
        teamTable.add(blueTeam);
        teamTable.add(redTeam);

        Table classTable = new Table();
        ButtonGroup<TextButton> classGroup = new ButtonGroup<>();
        classGroup.setMinCheckCount(1);
        classGroup.setMaxCheckCount(1);

        for(CharacterClass characterClass : CharacterClass.values()) {
            TextButton classButton = new TextButton(characterClass.displayName, skin, "toggle");
            classButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    selectedClass = characterClass;
                }
            });
            if(characterClass == selectedClass) classButton.setChecked(true);
            classGroup.add(classButton);
            classTable.add(classButton).width(160f).height(44f).pad(4f);
            if(classTable.getCells().size % 3 == 0) classTable.row();
        }

        TextButton spawnButton = new TextButton("Spawn", skin);
        spawnButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.startGame(selectedTeam, selectedClass);
            }
        });

        root.defaults().width(560f).padBottom(14f);
        root.add(title).row();
        root.add(joinUrlLabel).minHeight(40f).row();
        root.add(teamLabel).row();
        root.add(teamTable).row();
        root.add(classLabel).row();
        root.add(classTable).row();
        root.add(spawnButton).height(48f).row();

        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.06f, 0.07f, 0.09f, 1f);
        stage.act(delta);
        stage.draw();
    }

    private Skin createSkin() {
        Skin skin = new Skin();
        BitmapFont font = new BitmapFont();
        skin.add("default-font", font);

        Texture white = createTexture(Color.WHITE);
        Texture accent = createTexture(new Color(0.73f, 0.22f, 0.18f, 1f));
        Texture accentDark = createTexture(new Color(0.45f, 0.12f, 0.11f, 1f));
        Texture field = createTexture(new Color(0.17f, 0.19f, 0.23f, 1f));

        skin.add("white", white);

        Label.LabelStyle labelStyle = new Label.LabelStyle(font, new Color(0.88f, 0.9f, 0.92f, 1f));
        skin.add("default", labelStyle);

        BitmapFont titleFont = new BitmapFont();
        titleFont.getData().setScale(1.8f);
        skin.add("title", new Label.LabelStyle(titleFont, Color.WHITE));

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.up = new TextureRegionDrawable(accent);
        buttonStyle.down = new TextureRegionDrawable(accentDark);
        skin.add("default", buttonStyle);

        TextButton.TextButtonStyle toggleStyle = new TextButton.TextButtonStyle();
        toggleStyle.font = font;
        toggleStyle.fontColor = Color.WHITE;
        toggleStyle.up = new TextureRegionDrawable(field);
        toggleStyle.down = new TextureRegionDrawable(accentDark);
        toggleStyle.checked = new TextureRegionDrawable(accent);
        skin.add("toggle", toggleStyle);

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
        if(stage != null) stage.dispose();
        if(skin != null) skin.dispose();
    }
}
