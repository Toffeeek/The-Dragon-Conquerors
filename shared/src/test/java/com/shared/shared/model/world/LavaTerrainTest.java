// File Location: shared/src/test/java/com/shared/shared/model/world/LavaTerrainTest.java
package com.shared.shared.model.world;

import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LavaTerrainTest {
    private final BattlefieldDefinition field = BattlefieldDefinition.forEnvironment(Environment.LAVA);
    private Vector2 at(float x,float y) { return new Vector2(x*30f/1672f,17f-y*17f/941f); }

    @Test void moltenSeaIsBlockedButBridgeDecksAndSolidCracksAreWalkable() {
        assertTrue(field.isLava(at(1000,100)));
        assertFalse(field.isWalkable(at(1000,100)));
        assertTrue(field.isWalkable(at(500,427)), "West bridge deck");
        assertTrue(field.isWalkable(at(1045,520)), "East bridge deck");
        assertTrue(field.isWalkable(at(1195,685)), "South-east bridge deck");
        assertTrue(field.isWalkable(at(452,279)), "Burning crack remains walkable");
        assertFalse(field.isLava(at(452,279)));
        assertFalse(field.isLava(at(294,138)), "Furnace is a solid prop, not a lava pool");
        assertFalse(field.isWalkable(at(294,138)));
    }

    @Test void glowingCracksAreContactHazardsNotLavaOrSolidObstacles() {
        for (Vector2 point : List.of(at(130,165), at(458,167), at(460,282), at(887,359),
            at(830,540), at(1544,176), at(1228,489), at(785,750), at(889,849))) {
            assertTrue(field.isBurningCrack(point), "Burning crack at " + point);
            assertTrue(field.isHazard(point));
            assertTrue(field.isWalkable(point));
            assertFalse(field.isLava(point));
        }
        for (Vector2 point : List.of(at(405,282), at(495,282), at(500,427), at(294,138), at(930,376))) {
            assertFalse(field.isBurningCrack(point), "Safe ground or solid prop at " + point);
        }
        assertTrue(field.pathTouchesBurningCrack(at(405,282), List.of(at(495,282))));
        assertFalse(field.pathTouchesBurningCrack(at(405,282), List.of(at(420,282))));
    }

    @Test void solidPropsAndRailsBlockMovementWithoutBecomingLava() {
        for (Vector2 point: List.of(at(891,306),at(778,229),at(934,376),at(427,103),at(579,388))) {
            assertFalse(field.isWalkable(point));
            assertFalse(field.isLava(point));
        }
        assertFalse(field.pathIsWalkable(at(520,427),at(520,340)));
        assertFalse(field.pushEntersLava(at(520,427),at(520,300)), "Railing stops a push before lava");
    }

    @Test void openEdgesAndCrossingTheLavaRingAreLethal() {
        assertTrue(field.isWalkable(at(824,182)));
        assertTrue(field.pushEntersLava(at(824,182),at(824,90)));
        assertTrue(field.isLava(at(125,780)), "Enclosed lava ring is also molten");
        assertFalse(field.isWalkable(at(125,780)));
        assertTrue(field.pushEntersLava(at(170,720),at(170,820)), "Crossing lava kills even when ending on rock");
    }

    @Test void everySpawnAndMainIslandIsConnectedBySafeRoutes() {
        BattlefieldNavigation nav = new BattlefieldNavigation(field);
        Vector2 start=field.spawnFor(1,0);
        for (Vector2 goal: List.of(field.spawnFor(1,1),field.spawnFor(2,0),field.spawnFor(2,1),at(350,786),at(1170,836))) {
            assertTrue(field.isWalkable(goal));
            List<Vector2> path=nav.findPath(start,goal,100f,List.of());
            assertFalse(path.isEmpty(), "Bridge route to "+goal);
            assertEquals(goal,path.get(path.size()-1));
            Vector2 previous=start;
            for(Vector2 point:path){ assertTrue(field.pathIsWalkable(previous,point)); previous=point; }
        }
    }
}
