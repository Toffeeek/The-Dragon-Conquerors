// File Location: shared/src/main/java/com/shared/shared/model/world/BattlefieldZone.java
package com.shared.shared.model.world;

import com.badlogic.gdx.math.Vector2;

/** Immutable axis-aligned zone used identically by server validation and client overlays. */
public final class BattlefieldZone {
    private final BattlefieldZoneType type;
    private final float x;
    private final float y;
    private final float width;
    private final float height;

    public BattlefieldZone(BattlefieldZoneType type, float x, float y,
                           float width, float height) {
        if (type == null) throw new IllegalArgumentException("zone type is required");
        if (width <= 0f || height <= 0f) {
            throw new IllegalArgumentException("zone dimensions must be positive");
        }
        this.type = type;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public BattlefieldZoneType getType() { return type; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }

    public boolean contains(Vector2 point) {
        return point != null && point.x >= x && point.x <= x + width
            && point.y >= y && point.y <= y + height;
    }
}
