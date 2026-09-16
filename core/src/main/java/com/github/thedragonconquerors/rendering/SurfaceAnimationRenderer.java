package com.github.thedragonconquerors.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.github.thedragonconquerors.ui.PresentationSettings;
import com.shared.shared.model.world.*;

/** One masked map draw. Masks are built once; terrain pixels and simulation remain untouched. */
public final class SurfaceAnimationRenderer implements Disposable {
    private final Texture mask;
    private final ShaderProgram shader;
    private final BattlefieldArtwork art;
    private final boolean lava;
    private float time;
    public SurfaceAnimationRenderer(BattlefieldDefinition field) {
        art=BattlefieldArtwork.forEnvironment(field.getEnvironment());
        lava=field.getEnvironment()==Environment.LAVA;
        Pixmap source=new Pixmap(Gdx.files.internal("maps-new/"+art.name+".png"));
        Pixmap pixels=new Pixmap(source.getWidth(),source.getHeight(),Pixmap.Format.RGBA8888);
        pixels.setBlending(Pixmap.Blending.None);
        Vector2 point=new Vector2();
        for(int y=0;y<source.getHeight();y++)for(int x=0;x<source.getWidth();x++) {
            int rgba=source.getPixel(x,y);
            point.set(art.x+x*art.width/art.pixelWidth,17f-y*17f/art.pixelHeight);
            boolean fall=MapSurface.waterfall(field.getEnvironment(),x,y,rgba);
            boolean fluid=MapSurface.liquid(field.getEnvironment(),rgba,field.isLethalFall(point));
            pixels.drawPixel(x,y,fall?0xffff00ff:fluid?0xff0000ff:0x000000ff);
        }
        mask=new Texture(pixels);mask.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
        source.dispose();pixels.dispose();
        shader=new ShaderProgram("""
            attribute vec4 a_position;
            attribute vec4 a_color;
            attribute vec2 a_texCoord0;
            uniform mat4 u_projTrans;
            varying vec2 v_uv;
            void main(){v_uv=a_texCoord0;gl_Position=u_projTrans*a_position;}
            ""","""
            #ifdef GL_ES
            precision mediump float;
            #endif
            varying vec2 v_uv;
            uniform sampler2D u_texture;
            uniform sampler2D u_mask;
            uniform vec2 u_pixel;
            uniform float u_time;
            uniform float u_lava;
            void main(){
                vec4 original=texture2D(u_texture,v_uv);
                vec2 kind=texture2D(u_mask,v_uv).rg;
                if(kind.r<0.5){gl_FragColor=original;return;}
                float t=u_time;
                vec2 shift=vec2(sin(v_uv.y*180.0+t*1.3),cos(v_uv.x*125.0+t))*u_pixel*1.6;
                if(kind.g>0.5)shift=vec2(sin(v_uv.y*95.0-t*5.0)*0.5,sin(v_uv.y*120.0-t*7.0)*2.0)*u_pixel;
                vec2 destination=v_uv+shift;
                vec2 neighbor=texture2D(u_mask,destination).rg;
                float safe=step(0.5,neighbor.r)*(1.0-step(0.5,abs(neighbor.g-kind.g)));
                vec3 flow=mix(original.rgb,texture2D(u_texture,destination).rgb,safe*0.45);
                float wave=sin(v_uv.x*130.0+sin(v_uv.y*65.0+t*0.3)*2.0-t*0.7)
                    *cos(v_uv.y*170.0+sin(v_uv.x*80.0-t*0.4)*2.0-t)*0.007;
                float edge=1.0-min(min(texture2D(u_mask,v_uv+vec2(u_pixel.x*3.0,0)).r,texture2D(u_mask,v_uv-vec2(u_pixel.x*3.0,0)).r),min(texture2D(u_mask,v_uv+vec2(0,u_pixel.y*3.0)).r,texture2D(u_mask,v_uv-vec2(0,u_pixel.y*3.0)).r));
                float foam=edge*(0.035+0.035*sin(v_uv.x*260.0+v_uv.y*220.0-t*1.8))*(1.0-u_lava);
                float fall=kind.g*pow(max(0.0,sin(v_uv.y*210.0-t*5.5)),8.0)*0.11;
                gl_FragColor=vec4(flow+vec3(wave+foam+fall),original.a);
            }
            """);
        if(!shader.isCompiled())Gdx.app.error("Map animation",shader.getLog());
    }
    public void render(Batch batch,Texture image,Matrix4 projection,float delta) {
        boolean animated=!PresentationSettings.reducedMotion() && shader.isCompiled();
        if(animated)time=(time+Math.min(Math.max(delta,0),.1f))%3600f;
        ShaderProgram previous=batch.getShader();
        batch.setProjectionMatrix(projection);batch.setColor(Color.WHITE);
        if(animated)batch.setShader(shader);
        batch.begin();
        if(animated) {
            mask.bind(1);Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
            shader.setUniformi("u_mask",1);shader.setUniformf("u_pixel",1f/art.pixelWidth,1f/art.pixelHeight);
            shader.setUniformf("u_time",time);shader.setUniformf("u_lava",lava?1f:0f);
        }
        batch.draw(image,art.x,0,art.width,17);
        batch.end();
        if(animated)batch.setShader(previous);
    }
    @Override public void dispose(){mask.dispose();shader.dispose();}
}
