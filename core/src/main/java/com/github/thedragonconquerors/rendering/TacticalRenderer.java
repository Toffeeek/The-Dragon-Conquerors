package com.github.thedragonconquerors.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.Disposable;
import com.github.thedragonconquerors.entities.Player;
import com.shared.shared.model.ability.AbilityType;
import java.util.*;

/** Small world-space previews, effect badges, and server-confirmed floating feedback. */
public final class TacticalRenderer implements Disposable {
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont();
    private final com.github.thedragonconquerors.ui.PixelIcons icons=new com.github.thedragonconquerors.ui.PixelIcons();
    private final List<Floating> floating = new ArrayList<>();
    private static final class Floating {
        String text; Vector2 position; Color color; float age, delay;
    }
    public TacticalRenderer() {
        // World units are sub-pixel sized before projection; rounding glyph vertices
        // to whole world units collapses these small badges and floating numbers.
        font.setUseIntegerPositions(false);
        font.getData().setScale(.026f);
    }
    public void add(String text, Vector2 position, Color color, float delay) {
        Floating item = new Floating(); item.text = text; item.position = new Vector2(position);
        item.color = color; item.delay = delay; floating.add(item);
    }
    public void preview(Matrix4 projection, Player actor, AbilityType ability, Vector2 point, Player hovered, boolean valid) {
        if (ability == null) return;
        shapes.setProjectionMatrix(projection);
        Gdx.gl.glEnable(GL20.GL_BLEND); Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(.55f,.75f,1,.55f);
        if (ability.getRange() < 100 && ability.getRange() > 0) shapes.circle(actor.getPosition().x,actor.getPosition().y,ability.getRange(),80);
        shapes.end();
        if (ability.getTargetType().targetsGround() && point != null) {
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(valid ? new Color(.25f,.65f,1,.2f) : new Color(1,.2f,.15f,.23f));
            shapes.circle(point.x,point.y,ability.getAreaRadius() > 0 ? ability.getAreaRadius() : .35f,64);
            shapes.end();
            shapes.begin(ShapeRenderer.ShapeType.Line);
            shapes.setColor(valid ? Color.SKY : Color.SCARLET);
            shapes.circle(point.x,point.y,ability.getAreaRadius() > 0 ? ability.getAreaRadius() : .35f,64);
            shapes.end();
        }
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    public void feedback(Batch batch, Matrix4 projection, Collection<Player> players, float delta) {
        batch.setProjectionMatrix(projection); batch.setColor(Color.WHITE); batch.begin();
        for (Player player : players) {
            int index = 0;
            for (var effect : player.getActiveEffects()) {
                var type = effect.getType();
                font.setColor(switch (type) {
                    case BURN -> Color.ORANGE; case POISON -> Color.LIME; case CURSE -> Color.MAGENTA;
                    case SUB_ZERO -> Color.CYAN; case STUN -> Color.YELLOW;
                });
                float x=player.getPosition().x-.5f+index++*.75f,y=player.getPosition().y+1.75f;
                batch.setColor(Color.WHITE);batch.draw(icons.texture(com.github.thedragonconquerors.ui.PixelIcons.kind(type)),x,y,.34f,.34f);
                font.draw(batch,Integer.toString(effect.getRemainingTurns()),x+.36f,y+.30f);
            }
        }
        for (var iterator = floating.iterator(); iterator.hasNext();) {
            Floating item = iterator.next(); item.age += Math.max(0,delta);
            float time = item.age - item.delay;
            if (time < 0) continue;
            if (time > 1.4f) { iterator.remove(); continue; }
            font.setColor(item.color.r,item.color.g,item.color.b,Math.min(1,(1.4f-time)*3));
            // Rise beside the persistent status row, not through its countdown.
            font.draw(batch,item.text,item.position.x+.65f,item.position.y+1.45f+(com.github.thedragonconquerors.ui.PresentationSettings.reducedMotion()?0:time*.6f));
        }
        batch.end();
    }
    @Override public void dispose() { icons.dispose();font.dispose(); shapes.dispose(); }
}
