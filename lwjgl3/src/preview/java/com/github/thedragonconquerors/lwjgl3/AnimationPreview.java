package com.github.thedragonconquerors.lwjgl3;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.github.thedragonconquerors.assets.AssetService;
import com.github.thedragonconquerors.assets.SpriteAssets;
import com.github.thedragonconquerors.entities.Player;
import com.github.thedragonconquerors.rendering.PlayerRenderer;
import com.github.thedragonconquerors.rendering.AbilityEffectsRenderer;
import com.shared.shared.model.ability.AbilityType;
import java.util.ArrayList;
import java.util.List;

/** Offline developer visual check, kept in its own source set outside tests and game distributions. */
public final class AnimationPreview extends ApplicationAdapter {
    private SpriteBatch batch;
    private BitmapFont font;
    private AssetService assets;
    private PlayerRenderer renderer;
    private AbilityEffectsRenderer effects;
    private final List<Player> players = new ArrayList<>();
    private final OrthographicCamera camera = new OrthographicCamera();
    private final FitViewport viewport = new FitViewport(30,17,camera);
    private boolean paused = true;
    private int viewer;
    public static void main(String[] args) {
        var config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("TDC - Animation Preview"); config.setWindowedMode(1280,720); config.setForegroundFPS(60);
        new Lwjgl3Application(new AnimationPreview(), config);
    }
    @Override public void create() {
        batch = new SpriteBatch(); font = new BitmapFont(); font.setUseIntegerPositions(false); font.getData().setScale(.035f);
        assets = new AssetService(new InternalFileHandleResolver());
        for (SpriteAssets profile : SpriteAssets.values()) {
            for (var clip : profile.clips()) assets.load(clip);
            int id = players.size();
            players.add(new Player(id,profile.character,new Vector2(4+id%3*10,11-id/3*7),
                com.shared.shared.model.CharacterBuild.of(com.shared.shared.model.Race.HUMAN,profile.getCharacterClass()),id<3?1:2));
        }
        renderer = new PlayerRenderer(assets,batch);
        effects = new AbilityEffectsRenderer(assets,batch);
    }
    @Override public void resize(int width,int height) { viewport.update(width,height,true); }
    @Override public void render() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) paused = !paused;
        if (Gdx.input.isKeyJustPressed(Input.Keys.V)) viewer=viewer==0?3:0;
        for (int key=Input.Keys.NUM_1;key<=Input.Keys.NUM_9;key++) if(Gdx.input.isKeyJustPressed(key)) choose(key-Input.Keys.NUM_1+1);
        float dt = paused ? (Gdx.input.isKeyJustPressed(Input.Keys.F) ? .085f : 0) : Gdx.graphics.getDeltaTime();
        ScreenUtils.clear(new Color(.14f,.17f,.19f,1)); viewport.apply();
        Player local=players.get(viewer);
        Player hovered=PlayerRenderer.hoveredPlayer(local,players,
            viewport.unproject(new Vector2(Gdx.input.getX(),Gdx.input.getY())));
        for (Player player : players) {
            if(player==local)renderer.renderLocal(player,camera.combined,null,dt,false,hovered==player);
            else renderer.renderRemote(player,camera.combined,dt,local.getTeamIndex(),hovered==player);
        }
        effects.render(camera.combined,dt);
        batch.setProjectionMatrix(camera.combined); batch.begin();
        font.draw(batch,"1 Idle  2 Walk  3 Basic  4 Secondary  5 Ultimate  6 Hurt  7 Death  8 Revive | P play/pause | F step",.4f,16.5f);
        font.draw(batch,"V switch team | hover: green ally / red foe | arrow = you | 6 red damage flash / 9 green heal flash",.4f,15.8f);
        for (Player player:players) {
            var animation=player.getAnimationController();
            font.draw(batch,player.getCharacterClass()+" / "+player.getUsername(),player.getPosition().x-2,player.getPosition().y+2.7f);
            font.draw(batch,animation.getState()+" frame "+(animation.getCurrentFrame()+1)+"/"+animation.getClip().frames(),
                player.getPosition().x-2,player.getPosition().y-1.2f);
        }
        batch.end();
    }
    private void choose(int mode) {
        for (Player player : players) {
            var animation=player.getAnimationController();
            player.getMovementController().stopMoving();
            if(mode!=8) animation.revive();
            Vector2 from=player.getPosition(), target=new Vector2(from).add(mode==4 ? -2 : 2,0);
            switch(mode) {
                case 2 -> player.getMovementController().setPath(List.of(target));
                case 3,4,5 -> {
                    List<AbilityType> abilities=AbilityType.forClass(player.getCharacterClass());
                    AbilityType ability=abilities.get(mode==5 ? abilities.size()-1 : mode-3);
                    animation.playAbility(from,target,ability);
                    effects.play(ability,from,target,animation.getClip().duration());
                }
                case 6 -> { animation.playHurt(target,from); renderer.hitFlash(player.getId(),0); }
                case 7 -> animation.playDeath();
                case 8 -> animation.revive();
                case 9 -> renderer.healFlash(player.getId(),0);
                default -> { }
            }
        }
    }
    @Override public void dispose() { renderer.dispose(); assets.dispose(); font.dispose(); batch.dispose(); }
}
