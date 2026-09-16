// File Location: core/src/main/java/com/github/thedragonconquerors/rendering/PlayerRenderer.java
package com.github.thedragonconquerors.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.github.thedragonconquerors.assets.AssetService;
import com.github.thedragonconquerors.assets.SpriteAssets;
import com.github.thedragonconquerors.entities.Player;
import com.github.thedragonconquerors.movement.NavGrid;
import com.shared.shared.model.CharacterClass;

import java.util.List;
import java.util.Map;

/** Renders class-specific animated sprite sheets plus movement and target overlays. */
public class PlayerRenderer implements Disposable {
    public static final float TILE_SIZE = 1f;
    public static final float TARGET_CLICK_RADIUS = 0.72f;

    // The 100px cells contain ~20-30px bodies; feet are anchored at source (50,59).
    private static final float SPRITE_WIDTH = 4f;
    private static final float SPRITE_HEIGHT = 4f;
    private static final float SPRITE_Y_OFFSET = -1.64f;

    private static final Color COLOR_STAMINA_BG = new Color(0.12f, 0.12f, 0.15f, 0.9f);
    private static final Color COLOR_STAMINA_FILL = new Color(0.1f, 0.9f, 0.3f, 1f);
    private static final Color COLOR_HP_BG = new Color(0.12f, 0.12f, 0.15f, 0.9f);
    private static final Color COLOR_ALLY = new Color(0.18f, 0.86f, 0.35f, 1f);
    private static final Color COLOR_OPPONENT = new Color(0.94f, 0.22f, 0.22f, 1f);
    private static final Color COLOR_REACHABLE = new Color(0.08f, 0.42f, 1f, 0.38f);
    private static final Color COLOR_ACTIVE_TURN = new Color(1f, 0.82f, 0.25f, 1f);

    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private final Batch spriteBatch;
    private final AssetService assetService;
    private final Map<SpriteAssets.Clip, TextureRegion[]> sheetCache = new java.util.HashMap<>();

    private List<Vector2> cachedReachable;
    private float lastRemainingDistance = -1f;
    private final Vector2 lastReachablePosition = new Vector2(Float.NaN, Float.NaN);
    private int lastGridRevision = -1;
    private final ShaderProgram tintShader;
    private final Map<Integer,HealthFlash> flashes=new java.util.HashMap<>();
    public void hitFlash(int id,float delay) { flashes.put(id,new HealthFlash(HealthFlash.Kind.DAMAGE,delay)); }
    public void healFlash(int id,float delay) { flashes.put(id,new HealthFlash(HealthFlash.Kind.HEAL,delay)); }
    private void advanceFlash(Player p,float delta) {
        HealthFlash flash=flashes.get(p.getId());if(flash==null)return;
        flash.advance(delta);
        if(flash.finished())flashes.remove(p.getId());
    }

    public PlayerRenderer(AssetService assetService, Batch spriteBatch) {
        this.assetService = assetService;
        this.spriteBatch = spriteBatch;
        tintShader = new ShaderProgram("""
            attribute vec4 a_position;
            attribute vec4 a_color;
            attribute vec2 a_texCoord0;
            uniform mat4 u_projTrans;
            varying vec4 v_color;
            varying vec2 v_texCoords;
            void main() {
                v_color=a_color; v_color.a *= 255.0/254.0;
                v_texCoords=a_texCoord0; gl_Position=u_projTrans*a_position;
            }
            """, """
            #ifdef GL_ES
            precision mediump float;
            #endif
            varying vec4 v_color;
            varying vec2 v_texCoords;
            uniform sampler2D u_texture;
            uniform vec3 u_teamTint;
            uniform float u_tintStrength;
            void main() {
                vec4 original=texture2D(u_texture,v_texCoords)*v_color;
                // A light colour wash, not a replacement silhouette. Preserve texture alpha.
                gl_FragColor=vec4(mix(original.rgb,u_teamTint,u_tintStrength),original.a);
            }
            """);
        if (!tintShader.isCompiled()) throw new com.badlogic.gdx.utils.GdxRuntimeException(tintShader.getLog());
    }

    public void renderLocal(Player player, Matrix4 projection, NavGrid navGrid, float delta, boolean showMovement, boolean hovered) {
        advanceFlash(player,delta);
        player.getAnimationController().update(
            delta, player.getPosition(), player.getMovementController());

        float remaining = player.getMovementController().getRemainingMovementDistance();
        if (showMovement && navGrid != null && !player.getMovementController().isMoving()
            && (Math.abs(remaining - lastRemainingDistance) > 0.0001f
                || !player.getPosition().epsilonEquals(lastReachablePosition, 0.001f)
                || lastGridRevision != navGrid.getRevision())) {
            cachedReachable = navGrid.getReachablePositions(player.getPosition(), remaining);
            lastRemainingDistance = remaining;
            lastReachablePosition.set(player.getPosition());
            lastGridRevision = navGrid.getRevision();
        } else if (!player.isActiveTurn()) {
            cachedReachable = null;
            lastRemainingDistance = -1f;
        }

        if (showMovement && !player.getMovementController().isMoving()) drawReachable(projection);
        drawCharacter(player, projection, true, hovered);
        drawBars(player, projection, true, true);
        drawIdentity(player, projection, true);
    }

    public void renderRemote(Player player, Matrix4 projection, float delta, int localTeam, boolean hovered) {
        advanceFlash(player,delta);
        player.getAnimationController().update(
            delta, player.getPosition(), player.getMovementController());

        boolean ally = isAlly(localTeam, player.getTeamIndex());
        drawCharacter(player, projection, ally, hovered);
        drawBars(player, projection, false, ally);
        drawIdentity(player, projection, false);
    }

    private void drawReachable(Matrix4 projection) {
        if (cachedReachable == null) return;
        com.badlogic.gdx.Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        com.badlogic.gdx.Gdx.gl.glBlendFunc(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(projection);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(COLOR_REACHABLE);
        for (Vector2 pos : cachedReachable) {
            shapeRenderer.rect(pos.x - NavGrid.NODE_SIZE / 2f, pos.y - NavGrid.NODE_SIZE / 2f,
                NavGrid.NODE_SIZE, NavGrid.NODE_SIZE);
        }
        shapeRenderer.end();
        com.badlogic.gdx.Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
    }

    private void drawCharacter(Player player, Matrix4 projection, boolean ally, boolean hovered) {
        TextureRegion frame = currentFrame(player);
        float x = player.getPosition().x - SPRITE_WIDTH / 2f;
        float y = player.getPosition().y + SPRITE_Y_OFFSET;

        if (frame != null) {
            spriteBatch.setProjectionMatrix(projection);
            spriteBatch.setColor(Color.WHITE);
            HealthFlash flash=flashes.get(player.getId());
            boolean flashing=flash!=null && flash.active() && !com.github.thedragonconquerors.ui.PresentationSettings.reducedMotion();
            ShaderProgram previousShader=spriteBatch.getShader();
            boolean tint=hovered || flashing;
            if(tint)spriteBatch.setShader(tintShader);
            spriteBatch.begin();
            if(tint) {
                Color color=flashing ? (flash.kind==HealthFlash.Kind.HEAL?COLOR_ALLY:COLOR_OPPONENT)
                    : (ally?COLOR_ALLY:COLOR_OPPONENT);
                tintShader.setUniformf("u_teamTint",color.r,color.g,color.b);
                tintShader.setUniformf("u_tintStrength",flashing?.48f:.28f);
            }
            boolean flip = player.getAnimationController().isFacingLeft();
            spriteBatch.draw(frame, flip ? x + SPRITE_WIDTH : x, y,
                flip ? -SPRITE_WIDTH : SPRITE_WIDTH, SPRITE_HEIGHT);
            spriteBatch.end();
            if(tint)spriteBatch.setShader(previousShader);
            spriteBatch.setColor(Color.WHITE);
            return;
        }

        shapeRenderer.setProjectionMatrix(projection);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(hovered ? (ally?COLOR_ALLY:COLOR_OPPONENT) : fallbackColor(player.getCharacterClass()));
        shapeRenderer.circle(player.getPosition().x, player.getPosition().y, 0.35f, 16);
        shapeRenderer.end();
    }

    private TextureRegion currentFrame(Player player) {
        TextureRegion[] sheet = sheetFor(player.getAnimationController().getClip());
        if (sheet == null) return null;

        int frame = player.getAnimationController().getCurrentFrame();
        return sheet[frame];
    }

    private TextureRegion[] sheetFor(SpriteAssets.Clip clip) {
        TextureRegion[] cached = sheetCache.get(clip);
        if (cached != null) return cached;

        Texture texture = assetService.tryGet(clip);
        if (texture == null) return null;

        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        if (texture.getHeight() != 100 || texture.getWidth() != clip.frames() * 100)
            throw new IllegalStateException("Incorrect animation strip dimensions: " + clip.path());
        TextureRegion[] frames = TextureRegion.split(texture, 100, 100)[0];
        sheetCache.put(clip, frames);
        return frames;
    }

    private void drawBars(Player player, Matrix4 projection, boolean showStamina, boolean ally) {
        float x = player.getPosition().x;
        float y = player.getPosition().y;
        float barWidth = 0.86f;
        float barHeight = 0.075f;
        float hpRatio = player.getStats().getMaxHp()<=0?0:(float) player.getStats().getHp() / player.getStats().getMaxHp();

        shapeRenderer.setProjectionMatrix(projection);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(COLOR_HP_BG);
        shapeRenderer.rect(x - barWidth / 2f, y + 1.25f, barWidth, barHeight);
        shapeRenderer.setColor(ally?COLOR_ALLY:COLOR_OPPONENT);
        shapeRenderer.rect(x - barWidth / 2f, y + 1.25f,
            barWidth * Math.max(0f, hpRatio), barHeight);

        if (showStamina) {
            float remaining = player.getMovementController().getRemainingMovementDistance();
            float maximum = player.getMovementController().getMaxMovementDistance();
            float staminaRatio = maximum <= 0f ? 0f : remaining / maximum;
            shapeRenderer.setColor(COLOR_STAMINA_BG);
            shapeRenderer.rect(x - barWidth / 2f, y - 0.55f, barWidth, barHeight);
            shapeRenderer.setColor(COLOR_STAMINA_FILL);
            shapeRenderer.rect(x - barWidth / 2f, y - 0.55f,
                barWidth * Math.max(0f, staminaRatio), barHeight);
        }
        shapeRenderer.end();
    }

    private void drawIdentity(Player player, Matrix4 projection, boolean local) {
        if(!local && !(player.isActiveTurn() && player.isAlive()))return;
        shapeRenderer.setProjectionMatrix(projection);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        float x=player.getPosition().x,y=player.getPosition().y;
        if(local) {
            shapeRenderer.setColor(COLOR_HP_BG);
            shapeRenderer.triangle(x-.17f,y+1.72f,x+.17f,y+1.72f,x,y+1.44f);
            shapeRenderer.setColor(player.isActiveTurn()?COLOR_ACTIVE_TURN:Color.WHITE);
            shapeRenderer.triangle(x-.12f,y+1.68f,x+.12f,y+1.68f,x,y+1.49f);
        } else {
            shapeRenderer.setColor(COLOR_ACTIVE_TURN);
            shapeRenderer.rect(x-.43f,y+1.37f,.86f,.045f);
        }
        shapeRenderer.end();
    }

    public static boolean isAlly(int localTeam, int otherTeam) { return localTeam==otherTeam; }

    /** Body-only hover, independent of action mode, turn ownership and attack range. */
    public static Player hoveredPlayer(Player local, Iterable<Player> others, Vector2 pointer) {
        Player best=overBody(local,pointer)?local:null;
        float distance=best==null?Float.MAX_VALUE:hoverDistance(best,pointer);
        for(Player p:others) if(overBody(p,pointer)) {
            float next=hoverDistance(p,pointer);
            if(next<=distance){best=p;distance=next;}
        }
        return best;
    }
    private static boolean overBody(Player p,Vector2 point) {
        return p!=null && point!=null && Math.abs(point.x-p.getPosition().x)<=.45f
            && point.y>=p.getPosition().y-.12f && point.y<=p.getPosition().y+1.2f;
    }
    private static float hoverDistance(Player p,Vector2 point) {
        return point.dst2(p.getPosition().x,p.getPosition().y+.5f);
    }

    /**
     * Flat colour drawn when a class has no usable sprite sheet.
     *
     * <p>One distinct hue per class so the six classes stay tellable apart in
     * placeholder mode. {@code null} and any future class fall through to white
     * rather than throwing.</p>
     */
    private Color fallbackColor(CharacterClass characterClass) {
        if (characterClass == null) return Color.WHITE;
        switch (characterClass) {
            case PALADIN: return new Color(0.85f, 0.78f, 0.35f, 1f); // gold
            case MAGE:    return new Color(0.34f, 0.22f, 0.72f, 1f); // violet
            case WRAITH:  return new Color(0.28f, 0.20f, 0.35f, 1f); // shadow purple
            case CLERIC:  return new Color(0.80f, 0.86f, 0.92f, 1f); // pale silver
            case BARD:    return new Color(0.80f, 0.42f, 0.55f, 1f); // rose
            case ARCHER:  return new Color(0.18f, 0.55f, 0.30f, 1f); // forest green
            default:      return Color.WHITE;
        }
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        tintShader.dispose();
    }
}
