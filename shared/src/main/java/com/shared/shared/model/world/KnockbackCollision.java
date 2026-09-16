package com.shared.shared.model.world;

import com.badlogic.gdx.math.Vector2;
import java.util.Collection;

/** Swept player collision: no tunnelling and no chain pushes. Terrain resolves afterward. */
public final class KnockbackCollision {
    private KnockbackCollision() {}

    public static Vector2 stopBeforePlayers(Vector2 start, Vector2 end, Collection<Vector2> occupied) {
        float dx = end.x - start.x, dy = end.y - start.y;
        float length2 = dx * dx + dy * dy;
        if (length2 < .000001f) return new Vector2(start);
        float stop = 1f;
        float radius = BattlefieldNavigation.PLAYER_SEPARATION;
        for (Vector2 other : occupied) {
            float ox = start.x - other.x, oy = start.y - other.y;
            float projection = ox * dx + oy * dy;
            float c = ox * ox + oy * oy - radius * radius;
            if (c <= 0) {
                if (projection < 0) stop = 0; // Allow an already-touching pair to separate.
                continue;
            }
            float discriminant = projection * projection - length2 * c;
            if (discriminant < 0) continue;
            float hit = (-projection - (float)Math.sqrt(discriminant)) / length2;
            if (hit >= 0 && hit <= stop) stop = Math.max(0, hit - .001f / (float)Math.sqrt(length2));
        }
        return new Vector2(start.x + dx * stop, start.y + dy * stop);
    }
}
