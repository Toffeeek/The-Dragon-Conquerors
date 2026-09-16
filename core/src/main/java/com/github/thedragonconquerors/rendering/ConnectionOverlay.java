package com.github.thedragonconquerors.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.github.thedragonconquerors.FantasyUiTheme;

/** Modal recovery UI shared by lobby, battle, and results screens. */
public final class ConnectionOverlay implements Disposable {
    private final FantasyUiTheme theme = new FantasyUiTheme();
    private final Stage stage = new Stage(new FitViewport(1280,720));
    private final Label message;
    private final TextButton retry;
    private InputProcessor previousInput;
    private boolean visible;
    public ConnectionOverlay(Runnable retryAction, Runnable menuAction) {
        Table root = new Table(); root.setFillParent(true); root.setBackground(theme.solid(new com.badlogic.gdx.graphics.Color(0,0,0,.8f)));
        Table panel = new Table(); panel.setBackground(theme.panel()); panel.pad(30);
        panel.add(new Label("CONNECTION INTERRUPTED", theme.skin(), "heading")).colspan(2).padBottom(16).row();
        message = new Label("", theme.skin(), "default"); message.setWrap(true);
        panel.add(message).colspan(2).width(520).padBottom(22).row();
        retry = new TextButton("RETRY / SYNC", theme.skin(), "primary");
        TextButton menu = new TextButton("RETURN TO MENU", theme.skin(), "secondary");
        retry.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) { retryAction.run(); }});
        menu.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) { menuAction.run(); }});
        panel.add(retry).width(245).height(52).padRight(15); panel.add(menu).width(245).height(52);
        root.add(panel); stage.addActor(root);
    }
    public void render(String text, boolean recovering, float delta) {
        if (!visible) { previousInput = Gdx.input.getInputProcessor(); visible = true; }
        Gdx.input.setInputProcessor(stage);
        message.setText(recovering ? "Reconnecting and reading the current match state..." : text);
        retry.setDisabled(recovering);
        stage.getViewport().update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        stage.act(Math.min(delta,.1f)); stage.draw();
    }
    public void hide() {
        if (visible && Gdx.input.getInputProcessor() == stage) Gdx.input.setInputProcessor(previousInput);
        visible = false; previousInput = null;
    }
    @Override public void dispose() { stage.dispose(); theme.dispose(); }
}
