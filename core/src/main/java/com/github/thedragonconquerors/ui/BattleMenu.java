package com.github.thedragonconquerors.ui;

import com.badlogic.gdx.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.github.thedragonconquerors.FantasyUiTheme;

/** Local menu only: never pauses the multiplayer server. */
public final class BattleMenu implements Disposable {
    private final FantasyUiTheme theme=new FantasyUiTheme();
    private final Stage stage=new Stage(new FitViewport(1280,720));
    private final com.badlogic.gdx.scenes.scene2d.utils.Drawable backdrop=theme.solid(new com.badlogic.gdx.graphics.Color(.01f,.035f,.06f,.60f));
    private final Runnable leave, guide;
    private InputProcessor previous;
    private boolean open;
    public BattleMenu(Runnable leave,Runnable guide) { this.leave=leave;this.guide=guide; }
    public boolean isOpen() { return open; }
    public void open() { if(open)return;previous=Gdx.input.getInputProcessor();open=true;build(false);Gdx.input.setInputProcessor(stage); }
    public void close() { if(open && Gdx.input.getInputProcessor()==stage)Gdx.input.setInputProcessor(previous);open=false; }
    private TextButton button(String text,Runnable action) {
        TextButton button=new TextButton(text,theme.skin(),"secondary");
        button.addListener(new ChangeListener(){@Override public void changed(ChangeEvent e,Actor a){action.run();}});
        return button;
    }
    private Table setting(String name,String value,Runnable action) {
        Table row=new Table();
        row.add(new Label(name,theme.skin(),"default")).left().growX();
        row.add(button(value,action)).width(220).minHeight(46);
        return row;
    }
    private void build(boolean confirm) {
        stage.clear(); theme.refreshTextScale();
        Table root=new Table();root.setFillParent(true);root.setBackground(backdrop);
        Table panel=new Table(); panel.setBackground(theme.panel());panel.pad(24); panel.defaults().width(530).minHeight(40).padBottom(8);
        panel.add(new Label(confirm?"Leave the battle?":"Options",theme.skin(),"heading")).row();
        Label note=new Label(confirm?"Leaving disconnects your player. A locally hosted server also stops.":"Online battle continues",theme.skin(),"caption");note.setWrap(true);panel.add(note).row();
        if(confirm) {
            panel.add(button("KEEP PLAYING",()->build(false))).row();
            panel.add(button("CONFIRM LEAVE",()->{close();leave.run();})).row();
        } else {
            panel.add(button("RESUME",this::close)).row();
            panel.add(setting("Text size",Math.round(PresentationSettings.textScale()*100)+"%",()->{PresentationSettings.cycleText();build(false);})).row();
            panel.add(setting("Reduced motion",PresentationSettings.reducedMotion()?"ON":"OFF",()->{PresentationSettings.toggleMotion();build(false);})).row();
            Table shake=setting("Screen shake",PresentationSettings.shake()?"ON":"OFF",()->{PresentationSettings.toggleShake();build(false);});
            ((TextButton)shake.getChildren().get(1)).setDisabled(PresentationSettings.reducedMotion());panel.add(shake).row();
            panel.add(setting("Display",Gdx.graphics.isFullscreen()?"FULLSCREEN":"WINDOWED",()->{
                if(Gdx.graphics.isFullscreen())Gdx.graphics.setWindowedMode(1280,720);else Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());build(false);
            })).row();
            panel.add(button("FIELD GUIDE",()->{PresentationSettings.dismissGuide(false);guide.run();close();})).row();
            panel.add(button("LEAVE MATCH...",()->build(true))).row();
        }
        ScrollPane scroll=new ScrollPane(panel);scroll.setScrollingDisabled(true,false);scroll.setOverscroll(false,false);
        root.add(scroll).width(620).maxHeight(650);stage.addActor(root);UiMotion.reveal(panel);
    }
    public void render(float delta) {
        if(!open)return;
        stage.getViewport().update(Gdx.graphics.getWidth(),Gdx.graphics.getHeight(),true);
        stage.act(Math.min(delta,.1f));stage.draw();
    }
    @Override public void dispose(){close();stage.dispose();theme.dispose();}
}
