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

/** Colored placeholder terrain overlay driven by the shared battlefield metadata. */
public final class BattlefieldOverlayRenderer implements Disposable {
    private static final Color POISON = new Color(0.20f, 0.80f, 0.22f, 0.45f);

    private final BattlefieldDefinition battlefield;
    private final ShapeRenderer shapes = new ShapeRenderer();

    public BattlefieldOverlayRenderer(BattlefieldDefinition battlefield) {
        this.battlefield = battlefield;
    }

    public void render(Matrix4 projection) {
        if (battlefield == null || battlefield.getZones().isEmpty()) return;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (BattlefieldZone zone : battlefield.getZones()) {
            if (zone.getType() != BattlefieldZoneType.HAZARD) continue;
            shapes.setColor(POISON);
            // Shade only actual ground in the poison zone, not water or cliff tiles.
            for (float x = zone.getX(); x < zone.getX() + zone.getWidth(); x += 0.25f) {
                for (float y = zone.getY(); y < zone.getY() + zone.getHeight(); y += 0.25f) {
                    if (battlefield.isWalkable(new com.badlogic.gdx.math.Vector2(x + 0.125f, y + 0.125f))) {
                        shapes.rect(x, y, 0.25f, 0.25f);
                    }
                }
            }
        }
        shapes.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    @Override public void dispose() { shapes.dispose(); }
}
