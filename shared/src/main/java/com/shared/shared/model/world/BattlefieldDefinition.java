// File Location: shared/src/main/java/com/shared/shared/model/world/BattlefieldDefinition.java
package com.shared.shared.model.world;

import com.badlogic.gdx.math.Vector2;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Canonical geometry and spawn metadata shared by server and libGDX client. */
public final class BattlefieldDefinition {
    public static final float DEFAULT_WIDTH = 30f;
    public static final float DEFAULT_HEIGHT = 17f;
    public static final float PLAYER_RADIUS = 0.10f;
    private static final float TRACE_STEP = 1f / 32f;
    private static final Map<Environment, BattlefieldDefinition> DEFINITIONS =
        new EnumMap<>(Environment.class);

    static {
        DEFINITIONS.put(Environment.BOG, new BattlefieldDefinition(Environment.BOG, List.of(
            zone(BattlefieldZoneType.HAZARD, 7f, 2f, 5f, 4f),
            zone(BattlefieldZoneType.HAZARD, 12.5f, 7f, 5f, 3f),
            zone(BattlefieldZoneType.HAZARD, 19f, 11f, 4f, 4f))));

        DEFINITIONS.put(Environment.LAVA, new BattlefieldDefinition(Environment.LAVA, List.of()));
        DEFINITIONS.put(Environment.CANYON, new BattlefieldDefinition(Environment.CANYON, List.of()));
    }

    private final Environment environment;
    private final float width;
    private final float height;
    private final List<BattlefieldZone> zones;
    private final TiledTerrain terrain;

    private BattlefieldDefinition(Environment environment, List<BattlefieldZone> zones) {
        this.environment = environment;
        this.width = DEFAULT_WIDTH;
        this.height = DEFAULT_HEIGHT;
        this.zones = Collections.unmodifiableList(zones);
        this.terrain = new TiledTerrain(environment.name().toLowerCase(java.util.Locale.ROOT));
        validateSpawns();
    }

    public static BattlefieldDefinition forEnvironment(Environment environment) {
        Environment key = environment == null ? Environment.CANYON : environment;
        return DEFINITIONS.get(key);
    }

    public Environment getEnvironment() { return environment; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public List<BattlefieldZone> getZones() { return zones; }

    public Vector2 spawnFor(int teamIndex, int teamSlot) {
        if (environment == Environment.LAVA) {
            return teamIndex == 1 ? new Vector2(5.2f, teamSlot <= 0 ? 12.7f : 15.6f)
                : new Vector2(25.8f, teamSlot <= 0 ? 7.4f : 12.8f);
        }
        if (teamIndex == 1) return teamSlot <= 0 ? new Vector2(2f, 5f) : new Vector2(4.5f, 12.5f);
        return new Vector2(28f, teamSlot <= 0 ? 9f : 12f);
    }

    public boolean isInside(Vector2 point) {
        return point != null && Float.isFinite(point.x) && Float.isFinite(point.y)
            && point.x >= 0f && point.x <= width && point.y >= 0f && point.y <= height;
    }

    public boolean isHazard(Vector2 point) {
        return isBurningCrack(point) || contains(BattlefieldZoneType.HAZARD, point);
    }

    public boolean isBurningCrack(Vector2 point) {
        return environment == Environment.LAVA && isInside(point) && "crack".equals(terrain.at(point.x, point.y));
    }

    /** Check the actual accepted route, not a straight line to the requested destination. */
    public boolean pathTouchesBurningCrack(Vector2 start, List<Vector2> path) {
        if (environment != Environment.LAVA) return false;
        Vector2 previous = start;
        Vector2 sample = new Vector2();
        for (Vector2 end : path) {
            int steps = Math.max(1, (int)Math.ceil(previous.dst(end) / TRACE_STEP));
            for (int step = 0; step <= steps; step++) {
                sample.set(previous).lerp(end, step / (float)steps);
                if (isBurningCrack(sample)) return true;
            }
            previous = end;
        }
        return false;
    }

    public boolean isLethalFall(Vector2 point) {
        return !isInside(point) || "fall".equals(terrain.at(point.x, point.y));
    }

    public boolean isLava(Vector2 point) {
        return environment == Environment.LAVA && isInside(point) && "lava".equals(terrain.at(point.x, point.y));
    }

    /** Stop at solid props before considering lava behind them. A push can cross an open lava edge. */
    public boolean pushEntersLava(Vector2 start, Vector2 end) {
        if (environment != Environment.LAVA || !isInside(start) || end == null
            || !Float.isFinite(end.x) || !Float.isFinite(end.y)) return false;
        int steps = Math.max(1, (int)Math.ceil(start.dst(end) / TRACE_STEP));
        Vector2 sample = new Vector2();
        for (int step=0; step<=steps; step++) {
            sample.set(start).lerp(end, step/(float)steps);
            if (isLava(sample)) return true;
            if (!isInside(sample) || "blocked".equals(terrain.at(sample.x,sample.y))) return false;
        }
        return false;
    }

    public boolean isWalkable(Vector2 point) {
        return isInside(point) && terrain.isWalkable(point.x, point.y, PLAYER_RADIUS)
            && !contains(BattlefieldZoneType.BLOCKED, point)
            && !contains(BattlefieldZoneType.LETHAL_FALL, point);
    }

    public boolean pathIsWalkable(Vector2 start, Vector2 end) {
        if (!isWalkable(start) || !isWalkable(end)) return false;
        return terrain.segmentWalkable(start, end, PLAYER_RADIUS);
    }

    public boolean pathCrossesLethalFall(Vector2 start, Vector2 end) {
        return trace(start, end, TraceMode.LETHAL);
    }

    /** Last walkable sample before a wall, cliff, or world edge. */
    public Vector2 lastWalkablePoint(Vector2 start, Vector2 end) {
        if (start == null || end == null) return start == null ? new Vector2() : new Vector2(start);
        float distance = start.dst(end);
        int steps = Math.max(1, (int) Math.ceil(distance / TRACE_STEP));
        Vector2 last = new Vector2(start);
        Vector2 sample = new Vector2();
        for (int step = 1; step <= steps; step++) {
            sample.set(start).lerp(end, step / (float) steps);
            if (!isWalkable(sample)) break;
            last.set(sample);
        }
        return last;
    }

    private boolean trace(Vector2 start, Vector2 end, TraceMode mode) {
        if (start == null || end == null) return mode == TraceMode.LETHAL;
        float distance = start.dst(end);
        int steps = Math.max(1, (int) Math.ceil(distance / TRACE_STEP));
        Vector2 sample = new Vector2();
        for (int step = 0; step <= steps; step++) {
            sample.set(start).lerp(end, step / (float) steps);
            if (mode == TraceMode.WALKABLE && !isWalkable(sample)) return false;
            if (mode == TraceMode.LETHAL && isLethalFall(sample)) return true;
        }
        return mode == TraceMode.WALKABLE;
    }

    private boolean contains(BattlefieldZoneType type, Vector2 point) {
        for (BattlefieldZone zone : zones) {
            if (zone.getType() == type && zone.contains(point)) return true;
        }
        return false;
    }

    private void validateSpawns() {
        for (int team = 1; team <= 2; team++) {
            for (int slot = 0; slot < 2; slot++) {
                Vector2 spawn = spawnFor(team, slot);
                if (!isWalkable(spawn)) {
                    throw new IllegalStateException(environment + " spawn is not walkable: " + spawn);
                }
            }
        }
    }

    private static BattlefieldZone zone(BattlefieldZoneType type, float x, float y,
                                        float width, float height) {
        return new BattlefieldZone(type, x, y, width, height);
    }

    private enum TraceMode { WALKABLE, LETHAL }
}
