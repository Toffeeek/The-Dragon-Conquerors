// File Location: core/src/main/java/com/github/thedragonconquerors/rendering/BattlefieldOverlayRenderer.java
package com.github.thedragonconquerors.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import com.shared.shared.model.world.BattlefieldDefinition;
import com.shared.shared.model.world.BattlefieldZone;
import com.shared.shared.model.world.BattlefieldZoneType;
import com.shared.shared.model.world.Environment;

/** Colored placeholder terrain overlay driven by the shared battlefield metadata. */
public final class BattlefieldOverlayRenderer implements Disposable {
    private static final Color BOG_TINT = new Color(0.12f, 0.32f, 0.18f, 0.20f);
    private static final Color LAVA_TINT = new Color(0.50f, 0.12f, 0.03f, 0.23f);
    private static final Color CANYON_TINT = new Color(0.42f, 0.30f, 0.16f, 0.18f);
    private static final Color POISON = new Color(0.20f, 0.80f, 0.22f, 0.45f);
    private static final Color BLOCKED = new Color(0.13f, 0.12f, 0.14f, 0.72f);
    private static final Color FALL = new Color(0.02f, 0.01f, 0.02f, 0.90f);
    private static final Color FALL_EDGE = new Color(0.95f, 0.25f, 0.12f, 0.85f);

    private final BattlefieldDefinition battlefield;
    private final ShapeRenderer shapes = new ShapeRenderer();

    public BattlefieldOverlayRenderer(BattlefieldDefinition battlefield) {
        this.battlefield = battlefield;
    }

    public void render(Matrix4 projection) {
        if (battlefield == null) return;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(tint());
        shapes.rect(0f, 0f, battlefield.getWidth(), battlefield.getHeight());
        for (BattlefieldZone zone : battlefield.getZones()) {
            shapes.setColor(zone.getType() == BattlefieldZoneType.HAZARD ? POISON
                : zone.getType() == BattlefieldZoneType.LETHAL_FALL ? FALL : BLOCKED);
            shapes.rect(zone.getX(), zone.getY(), zone.getWidth(), zone.getHeight());
        }
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(FALL_EDGE);
        for (BattlefieldZone zone : battlefield.getZones()) {
            if (zone.getType() == BattlefieldZoneType.LETHAL_FALL) {
                shapes.rect(zone.getX(), zone.getY(), zone.getWidth(), zone.getHeight());
            }
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private Color tint() {
        Environment environment = battlefield.getEnvironment();
        if (environment == Environment.BOG) return BOG_TINT;
        if (environment == Environment.LAVA) return LAVA_TINT;
        return CANYON_TINT;
    }

    @Override public void dispose() { shapes.dispose(); }
}
