package com.github.thedragonconquerors.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.github.thedragonconquerors.assets.AssetService;
import com.shared.shared.model.CharacterClass;

/** Small portrait-only backplate; team colours always follow the local viewer. */
public final class TurnPortraitCard extends Table {
    private static final Color ALLY=new Color(.18f,.86f,.35f,1);
    private static final Color ENEMY=new Color(.94f,.22f,.22f,1);
    private final Drawable pixel;
    private final int team;
    private final boolean active;
    private int viewerTeam;
    public TurnPortraitCard(AssetService assets,CharacterClass type,int team,boolean active,boolean down,Drawable pixel) {
        this.team=team;this.active=active;this.pixel=pixel;
        pad(7);
        CharacterPortrait portrait=new CharacterPortrait(assets,type,false);
        if(down)portrait.setColor(Color.GRAY);
        add(portrait).size(38,42);
    }
    public void setViewerTeam(int team) { viewerTeam=team; }
    public static boolean allied(int viewer,int team) { return viewer==team; }
    @Override public void draw(Batch batch,float parentAlpha) {
        validate();
        float x=getX(),y=getY(),w=getWidth(),h=getHeight(),a=parentAlpha*getColor().a;
        batch.setColor(.015f,.02f,.025f,.87f*a);pixel.draw(batch,x,y,w,h);
        Color edge=allied(viewerTeam,team)?ALLY:ENEMY;
        batch.setColor(edge.r,edge.g,edge.b,a);
        pixel.draw(batch,x,y,w,2);pixel.draw(batch,x,y+h-2,w,2);
        pixel.draw(batch,x,y,2,h);pixel.draw(batch,x+w-2,y,2,h);
        if(active) {
            batch.setColor(1,.82f,.25f,a);
            // Downward arrow above, never replacing the affiliation border.
            for(int row=0;row<7;row++)pixel.draw(batch,x+w/2-row-1,y+h+3+row,(row+1)*2,1);
        }
        batch.setColor(Color.WHITE);super.draw(batch,parentAlpha);
    }
}
