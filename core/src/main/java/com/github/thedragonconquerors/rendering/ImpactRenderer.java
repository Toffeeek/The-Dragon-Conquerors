package com.github.thedragonconquerors.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.Disposable;
import com.github.thedragonconquerors.ui.PresentationSettings;
import com.shared.shared.model.ability.AbilityType;
import java.util.*;

/** Server-confirmed cosmetic bursts. No hit-stop or changes to simulation timing. */
public final class ImpactRenderer implements Disposable {
    private static final class Burst { Vector2 point,direction; Color color;float age,delay;boolean healing,strong,finisher; }
    private final List<Burst> bursts=new ArrayList<>();
    private final ShapeRenderer shapes=new ShapeRenderer();
    private float shake;
    public void add(Vector2 origin,Vector2 point,AbilityType ability,boolean heal,boolean finisher,float delay) {
        Burst b=new Burst();b.point=new Vector2(point);b.direction=origin==null?new Vector2(0,1):new Vector2(point).sub(origin).nor();
        b.delay=delay;b.healing=heal;b.finisher=finisher;b.strong=ability==AbilityType.FIREBALL||ability==AbilityType.ELDRITCH_BLAST||finisher;
        b.color=heal?Color.GREEN:ability==AbilityType.FIREBALL?Color.ORANGE:ability==AbilityType.ELDRITCH_BLAST?Color.VIOLET:ability==AbilityType.ICE_ATTACK?Color.CYAN:Color.GOLD;
        if(bursts.size()>=48)bursts.removeFirst();bursts.add(b);
    }
    public float shakeOffset() { return PresentationSettings.shake()?(float)Math.sin(shake*83)*shake*.13f:0; }
    public void render(Matrix4 projection,float delta) {
        float dt=Math.min(.1f,Math.max(0,delta));shake=Math.max(0,shake-dt);
        shapes.setProjectionMatrix(projection);Gdx.gl.glEnable(GL20.GL_BLEND);Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        for(var iterator=bursts.iterator();iterator.hasNext();) {
            Burst b=iterator.next();float before=b.age-b.delay;b.age+=dt;float t=b.age-b.delay;
            if(t<0)continue;
            if(t>.55f){iterator.remove();continue;}
            if(before<=0&&b.strong&&!b.healing)shake=.25f;
            float alpha=1-t/.55f;
            if(PresentationSettings.reducedMotion())continue;
            shapes.setColor(b.color.r,b.color.g,b.color.b,alpha*.8f);
            float radius=(b.strong?.18f:.08f)+t*(b.healing?.6f:1.8f);
            shapes.circle(b.point.x,b.point.y+.35f,radius,28);
            int count=b.strong?12:7;
            for(int i=0;i<count;i++) {
                float angle=i*6.28318f/count,dx=(float)Math.cos(angle),dy=(float)Math.sin(angle);
                float push=b.healing?0:t*.45f;
                float x=b.point.x+dx*radius+b.direction.x*push,y=b.point.y+.35f+dy*radius+(b.healing?t*.7f:b.direction.y*push);
                shapes.line(x,y,x+dx*.12f,y+dy*.12f);
            }
            if(b.finisher){shapes.setColor(1,1,.8f,alpha);shapes.line(b.point.x-.55f,b.point.y+.35f,b.point.x+.55f,b.point.y+.35f);shapes.line(b.point.x,b.point.y-.2f,b.point.x,b.point.y+.9f);}
        }
        shapes.end();Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    @Override public void dispose(){shapes.dispose();}
}
