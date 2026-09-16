package com.github.thedragonconquerors.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.github.thedragonconquerors.assets.*;
import com.github.thedragonconquerors.animation.AnimationState;
import com.shared.shared.model.CharacterClass;

/** Cropped original sprite animation. The shared asset service owns its texture. */
public final class CharacterPortrait extends Actor {
    private final Texture texture;
    private final SpriteAssets.Clip clip;
    private final TextureRegion region=new TextureRegion();
    private float time;
    private final boolean celebrate;
    private final boolean wraith;
    public CharacterPortrait(AssetService assets, CharacterClass type, boolean celebrate) {
        this.celebrate=celebrate;
        this.wraith=type==CharacterClass.WRAITH;
        clip=SpriteAssets.forClass(type).clip(AnimationState.IDLE);
        texture=assets.load(clip); texture.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
        region.setTexture(texture);
    }
    @Override public void act(float delta) { super.act(delta); if(!PresentationSettings.reducedMotion()) time+=Math.min(delta,.1f); }
    @Override public void draw(Batch batch,float parentAlpha) {
        // Preserve the Wraith's tall scythe; trim excess transparent space on the others.
        region.setRegion(clip.frameAt(time)*100+(wraith?26:34),wraith?18:30,wraith?48:34,wraith?52:38);
        Color c=getColor(); batch.setColor(c.r,c.g,c.b,c.a*parentAlpha);
        float bob=celebrate&&time<3&&!PresentationSettings.reducedMotion()?(float)Math.sin(time*4)*3*(1-time/3):0;
        batch.draw(region,getX(),getY()+bob,getWidth(),getHeight()); batch.setColor(Color.WHITE);
    }
}
