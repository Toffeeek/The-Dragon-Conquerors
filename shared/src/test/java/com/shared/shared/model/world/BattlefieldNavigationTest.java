package com.shared.shared.model.world;

import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BattlefieldNavigationTest {
    private final BattlefieldDefinition field = BattlefieldDefinition.forEnvironment(Environment.CANYON);
    private final BattlefieldNavigation nav = new BattlefieldNavigation(field);

    @Test void collisionMatchesWaterCliffsAndFlippedTilesInTheMap() {
        assertFalse(field.isWalkable(new Vector2(22.5f, 9.5f)), "Large visible lake");
        assertFalse(field.isWalkable(new Vector2(4.5f, 7.5f)), "Flipped water tile");
        assertFalse(field.isWalkable(new Vector2(15.375f, 10.5f)), "Actual brown cliff pixels");
        assertTrue(field.isWalkable(new Vector2(15.0625f, 10.5f)), "Grass beside the cliff within the same tile");
        assertFalse(field.isWalkable(new Vector2(0f, 5f)), "Body cannot leave map");
        assertTrue(field.isWalkable(new Vector2(10.5f, 8.5f)), "Old black rectangle is ordinary grass");
        assertTrue(field.isWalkable(new Vector2(18.5f, 10.5f)), "Old black rectangle is ordinary grass");
    }

    @Test void pathRoutesAroundWaterAndChargesTheWholeRoute() {
        Vector2 start = new Vector2(2f, 7f), goal = new Vector2(8f, 7f);
        assertFalse(field.pathIsWalkable(start, goal));
        List<Vector2> path = nav.findPath(start, goal, 30f, List.of());
        assertFalse(path.isEmpty());
        assertEquals(goal, path.get(path.size() - 1));
        assertTrue(BattlefieldNavigation.length(start, path) > start.dst(goal));
        checkSegments(start, path, List.of());
        List<Vector2> limited = nav.findPath(start, goal, 3.37f, List.of());
        assertEquals(3.37f, BattlefieldNavigation.length(start, limited), 0.001f);
        checkSegments(start, limited, List.of());
    }

    @Test void playersBlockTheWholePathNotJustTheDestination() {
        Vector2 start = new Vector2(2f, 5f), goal = new Vector2(8f, 5f);
        List<Vector2> occupied = List.of(new Vector2(5f, 5f));
        assertFalse(nav.segmentClear(start, goal, occupied));
        List<Vector2> path = nav.findPath(start, goal, 20f, occupied);
        assertFalse(path.isEmpty());
        checkSegments(start, path, occupied);
        assertTrue(nav.findPath(start, occupied.get(0), 20f, occupied).isEmpty());
    }

    @Test void previewsNeverIncludeWaterAndInvalidCommandsAreRejected() {
        Vector2 start = new Vector2(2f, 5f);
        for (Vector2 point : nav.reachable(start, 7f, List.of())) assertTrue(field.isWalkable(point));
        for (Vector2 invalid : List.of(new Vector2(Float.NaN, 3f), new Vector2(Float.POSITIVE_INFINITY, 5f),
            new Vector2(-1f, 5f), new Vector2(22f, 9f))) {
            assertTrue(nav.findPath(start, invalid, 7f, List.of()).isEmpty());
        }
        assertTrue(nav.findPath(start, new Vector2(3f, 5f), 0f, List.of()).isEmpty());
    }

    @Test void allSpawnsCanReachEachOther() {
        for (Environment environment : Environment.values()) {
            BattlefieldDefinition map = BattlefieldDefinition.forEnvironment(environment);
            BattlefieldNavigation navigation = new BattlefieldNavigation(map);
            for (int team = 1; team <= 2; team++) for (int slot = 0; slot < 2; slot++) {
                assertFalse(navigation.findPath(map.spawnFor(1, 0), map.spawnFor(team, slot), 100f, List.of()).isEmpty()
                    && (team != 1 || slot != 0), environment + " spawn unreachable");
            }
        }
    }

    private void checkSegments(Vector2 start, List<Vector2> path, List<Vector2> occupied) {
        for (Vector2 point : path) { assertTrue(nav.segmentClear(start, point, occupied)); start = point; }
    }
}
