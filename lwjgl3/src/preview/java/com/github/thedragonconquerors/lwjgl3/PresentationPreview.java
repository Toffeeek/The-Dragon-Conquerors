package com.github.thedragonconquerors.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.client.client.NetworkClient;
import com.github.thedragonconquerors.Main;
import com.github.thedragonconquerors.entities.Player;
import com.github.thedragonconquerors.input.BattleInteraction;
import com.github.thedragonconquerors.rendering.HudRenderer;
import com.github.thedragonconquerors.ui.PresentationSettings;
import com.shared.shared.model.*;
import com.shared.shared.model.world.Environment;
import com.shared.shared.network.*;
import java.util.List;

/** Offline UI fixture. Never joins a server or ships in the game package.
 *  H = four-player HUD, R = results, T = text size; close to restore preferences. */
public final class PresentationPreview extends Main {
    private final NetworkClient offline=new NetworkClient("ws://localhost:8080/ws");
    private float savedText;
    private boolean savedGuide;
    private boolean savedMotion;
    private HudRenderer hud;
    private com.github.thedragonconquerors.rendering.SurfaceAnimationRenderer surface;
    private com.github.thedragonconquerors.rendering.AmbientRenderer ambience;
    private com.github.thedragonconquerors.rendering.PlayerRenderer actors;
    private int viewer;
    private final CharacterBuild build=CharacterBuild.of(Race.HUMAN,CharacterClass.MAGE);
    private MatchState fixture;
    private Environment previewEnvironment=Environment.CANYON;
    private char page='R';
    public static void main(String[] args) {
        var config=new Lwjgl3ApplicationConfiguration();
        config.setWindowedMode(1024,576);config.setForegroundFPS(60);
        new Lwjgl3Application(new PresentationPreview(),config);
    }
    @Override public NetworkClient getNetworkClient() { return offline; }
    @Override public void create() {
        super.create();
        Gdx.graphics.setTitle("TDC - Presentation Preview (H / R / T)");
        savedText=PresentationSettings.textScale();savedGuide=PresentationSettings.guideDismissed();
        savedMotion=PresentationSettings.reducedMotion();
        fixture=MatchState.builder().activePlayerId(0).nextPlayerId(1).roundNumber(4)
            .winningTeam(1).matchOver(true).environment(Environment.CANYON)
            .message("Azure wins! Pushes, healing and teamwork secured the battlefield.")
            .turnOrder(List.of(0,1,2,3))
            .players(List.of(player(0,CharacterClass.MAGE,1,true),player(1,CharacterClass.CLERIC,1,true),
                player(2,CharacterClass.WRAITH,2,false),player(3,CharacterClass.ARCHER,2,true)))
            .statistics(List.of(new BattleStatistics(0,"Ember",CharacterClass.MAGE,1,143,0,1,1,4),
                new BattleStatistics(1,"Aurora",CharacterClass.CLERIC,1,39,72,1,0,4),
                new BattleStatistics(2,"Shade",CharacterClass.WRAITH,2,93,0,0,0,3),
                new BattleStatistics(3,"Fletch",CharacterClass.ARCHER,2,72,0,0,0,4))).build();
        showResults();
    }
    private PlayerCombatState player(int id,CharacterClass type,int team,boolean connected) {
        return PlayerCombatState.builder().id(id).username(List.of("Ember","Aurora","Shade","Fletch").get(id))
            .characterClass(type).teamIndex(team).race(Race.HUMAN).position(new Vector2(15,8))
            .hp(id==3?0:100).maxHp(120).mana(48).maxMana(100).activeTurn(id==0)
            .remainingMovement(4).maxMovement(6).connected(connected).build();
    }
    private void showResults() { showPostMatch(1,build,Environment.CANYON,0,fixture); }
    private void showHud() {
        var field=com.shared.shared.model.world.BattlefieldDefinition.forEnvironment(previewEnvironment);
        var roster=new java.util.ArrayList<Player>();
        for(var state:fixture.getPlayers()) {
            var hero=new Player(state.getId(),state.getUsername(),new Vector2(),CharacterBuild.of(Race.HUMAN,state.getCharacterClass()),state.getTeamIndex());
            hero.applyCombatState(state);hero.getPosition().set(field.spawnFor(state.getTeamIndex(),state.getId()%2));roster.add(hero);
        }
        var p=roster.get(viewer);
        var interaction=new BattleInteraction();
        if(hud!=null)hud.dispose();
        if(surface!=null)surface.dispose();if(ambience!=null)ambience.dispose();if(actors!=null)actors.dispose();
        surface=new com.github.thedragonconquerors.rendering.SurfaceAnimationRenderer(field);
        ambience=new com.github.thedragonconquerors.rendering.AmbientRenderer(field);
        for(var profile:com.github.thedragonconquerors.assets.SpriteAssets.values())for(var clip:profile.clips())getAssetService().load(clip);
        actors=new com.github.thedragonconquerors.rendering.PlayerRenderer(getAssetService(),getBatch());
        hud=new HudRenderer(com.shared.shared.model.ability.AbilityType.forClass(p.getCharacterClass()),interaction,()->interaction.chooseMove(p,false),
            ()->interaction.chooseAction(p,false),ability->hud.showTargetingPrompt(ability),
            ()->{},()->{interaction.cancel();hud.clearTargetingPrompt();},getAssetService(),()->{});
        hud.recordState(fixture);
        PresentationSettings.dismissGuide(false);
        setScreen(new ScreenAdapter(){
            @Override public void show(){Gdx.input.setInputProcessor(hud.stage());}
            @Override public void render(float delta){
                ScreenUtils.clear(.01f,.04f,.07f,1);
                getViewport().apply();
                var art=com.shared.shared.model.world.BattlefieldArtwork.forEnvironment(previewEnvironment);
                var texture=getAssetService().load(com.github.thedragonconquerors.assets.BattlefieldImageAssets.forEnvironment(previewEnvironment));
                surface.render(getBatch(),texture,getCamera().combined,delta);ambience.render(getCamera().combined,delta);
                int hovered=hud.hoveredPortraitId();
                for(var hero:roster) {
                    if(hero==p)actors.renderLocal(hero,getCamera().combined,null,delta,false,hero.getId()==hovered);
                    else actors.renderRemote(hero,getCamera().combined,delta,p.getTeamIndex(),hero.getId()==hovered);
                }
                hud.render(p,delta,false);
            }
            @Override public void resize(int w,int h){getViewport().update(w,h,true);hud.resize(w,h);}
        });
    }
    private void showPage() {
        switch(page) {
            case 'H' -> showHud();
            case 'M' -> {addScreen(new com.github.thedragonconquerors.MenuScreen(this));setScreen(com.github.thedragonconquerors.MenuScreen.class);}
            case 'L' -> {addScreen(new com.github.thedragonconquerors.LobbyScreen(this,"ws://localhost:8080/ws"));setScreen(com.github.thedragonconquerors.LobbyScreen.class);}
            default -> showResults();
        }
    }
    @Override public void render() {
        if(Gdx.input.isKeyJustPressed(Input.Keys.H)){page='H';showPage();}
        if(Gdx.input.isKeyJustPressed(Input.Keys.R)){page='R';showPage();}
        if(Gdx.input.isKeyJustPressed(Input.Keys.M)){page='M';showPage();}
        if(Gdx.input.isKeyJustPressed(Input.Keys.L)){page='L';showPage();}
        if(Gdx.input.isKeyJustPressed(Input.Keys.V)){viewer=viewer==0?2:0;page='H';showPage();}
        if(Gdx.input.isKeyJustPressed(Input.Keys.O))PresentationSettings.toggleMotion();
        for(int key=Input.Keys.NUM_1;key<=Input.Keys.NUM_3;key++)if(Gdx.input.isKeyJustPressed(key)) {
            previewEnvironment=Environment.selectionOrder().get(key-Input.Keys.NUM_1);page='H';showPage();
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            PresentationSettings.cycleText();
            showPage();
        }
        super.render();
    }
    @Override public void dispose() {
        Gdx.app.getPreferences("tdc-presentation").putFloat("textScale",savedText).putBoolean("guideDismissed",savedGuide).putBoolean("reducedMotion",savedMotion).flush();
        if(hud!=null)hud.dispose();
        if(surface!=null)surface.dispose();if(ambience!=null)ambience.dispose();if(actors!=null)actors.dispose();
        super.dispose();
    }
}
