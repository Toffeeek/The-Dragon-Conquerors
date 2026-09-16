package com.github.thedragonconquerors.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.Disposable;
import com.github.thedragonconquerors.ui.PresentationSettings;
import com.shared.shared.model.world.*;
import java.util.*;

/** Cosmetic surface samples only. Does not touch navigation, hazard masks or the camera. */
public final class AmbientRenderer implements Disposable {
    private record Mote(Vector2 point,float phase,boolean hazard) {}
    private final List<Mote> motes=new ArrayList<>();
    private final ShapeRenderer shapes=new ShapeRenderer();
    private final Environment environment;
    private float time;
    private final Vector2 waterfallBase=BattlefieldArtwork.MAP3.worldPoint(813,283);
    private static final Vector2[] FIRES={new Vector2(5.23f,14.45f),new Vector2(25.32f,10.45f),new Vector2(27.05f,10.45f)};
    public AmbientRenderer(BattlefieldDefinition field) {
        environment=field.getEnvironment();
        BattlefieldArtwork art=BattlefieldArtwork.forEnvironment(environment);
        if(art==null)return;
        Pixmap pixels=new Pixmap(Gdx.files.internal("maps-new/"+art.name+".png"));
        Random random=new Random(51+environment.ordinal());
        try {
            for(int attempts=0;attempts<3000 && motes.size()<65;attempts++) {
                int x=random.nextInt(pixels.getWidth()), y=random.nextInt(pixels.getHeight());
                Vector2 point=art.worldPoint(x*art.pixelWidth/(float)pixels.getWidth(),y*art.pixelHeight/(float)pixels.getHeight());
                int rgba=pixels.getPixel(x,y);
                boolean hazard=field.isHazard(point);
                boolean surface=MapSurface.liquid(environment,rgba,field.isLethalFall(point));
                // Keep each ripple clear of the shore; the shader handles shoreline foam.
                boolean clear=surface;
                for(int dx:new int[]{-15,15})for(int dy:new int[]{-8,8}) {
                    int sx=Math.max(0,Math.min(pixels.getWidth()-1,x+dx)),sy=Math.max(0,Math.min(pixels.getHeight()-1,y+dy));
                    clear &= MapSurface.liquid(environment,pixels.getPixel(sx,sy),true);
                }
                if(clear || environment==Environment.BOG&&hazard) motes.add(new Mote(point,random.nextFloat()*6.28f,hazard));
            }
        } finally { pixels.dispose(); }
    }
    public void render(Matrix4 projection,float delta) {
        if(PresentationSettings.reducedMotion())return;
        time+=Math.min(delta,.1f);
        Gdx.gl.glEnable(GL20.GL_BLEND);Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);shapes.begin(ShapeRenderer.ShapeType.Line);
        for(Mote mote:motes) {
            float cycle=(time*.24f+mote.phase)%1, alpha=(float)Math.sin(cycle*Math.PI)*.23f;
            Vector2 p=mote.point;
            if(environment==Environment.CANYON) {
                shapes.setColor(.65f,.91f,1,alpha);shapes.ellipse(p.x-.18f,p.y-.04f,.36f+cycle*.18f,.08f+cycle*.045f,18);
            } else if(environment==Environment.LAVA) {
                shapes.setColor(1,.8f,.22f,alpha*.7f);shapes.ellipse(p.x-.16f,p.y-.05f,.32f+cycle*.1f,.1f,18);
            } else {
                shapes.setColor(.67f,.4f,1,alpha);shapes.circle(p.x,p.y,.07f+cycle*.15f,18);
            }
        }
        shapes.end();shapes.begin(ShapeRenderer.ShapeType.Filled);
        if(environment!=Environment.CANYON) for(Mote mote:motes) {
            float cycle=(time*.3f+mote.phase)%1,alpha=(float)Math.sin(cycle*Math.PI)*.55f;
            shapes.setColor(environment==Environment.LAVA?1:.7f,environment==Environment.LAVA?.7f:.5f,environment==Environment.LAVA?.25f:1,alpha);
            float drift=(float)Math.sin(time+mote.phase)*.06f;
            shapes.rect(mote.point.x+drift,mote.point.y+cycle*.3f,.025f,.045f);
        }
        // Subtle, artwork-aligned glow at the three lava-map fire sources.
        if(environment==Environment.LAVA) for(Vector2 p:FIRES) {
            shapes.setColor(1,.52f,.13f,.06f+.025f*(float)Math.sin(time*5+p.x));shapes.circle(p.x,p.y,.38f,24);
        }
        if(environment==Environment.BOG) {
            Vector2 base=waterfallBase;
            for(int i=0;i<12;i++) {
                float phase=(time*.45f+i*.0833f)%1;
                float spread=(i%5-2)*.035f*(1+phase);
                shapes.setColor(.88f,.72f,1,(1-phase)*.14f);
                shapes.circle(base.x+spread,base.y+phase*.22f,.025f+phase*.045f,8);
            }
        }
        shapes.end();Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    @Override public void dispose(){shapes.dispose();}
}
