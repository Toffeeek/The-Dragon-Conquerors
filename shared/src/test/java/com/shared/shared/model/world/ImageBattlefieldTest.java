package com.shared.shared.model.world;

import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ImageBattlefieldTest {
    private final BattlefieldDefinition first = BattlefieldDefinition.forEnvironment(Environment.CANYON);
    private final BattlefieldDefinition last = BattlefieldDefinition.forEnvironment(Environment.BOG);
    private final BattlefieldDefinition lava = BattlefieldDefinition.forEnvironment(Environment.LAVA);

    @Test void lavaMapKeepsBridgesOpenAndMatchesPropsPoolsAndCracks() {
        assertEquals(BattlefieldArtwork.MAP2, BattlefieldArtwork.forEnvironment(Environment.LAVA));
        for (Vector2 p : List.of(p2(295,350), p2(530,425), p2(428,555), p2(1050,515), p2(1200,695))) {
            assertTrue(lava.isWalkable(p), "Bridge deck " + p);
            assertFalse(lava.isHazard(p));
        }
        for (Vector2 p : List.of(p2(295,150), p2(777,235), p2(896,305), p2(1440,345), p2(150,580))) {
            assertFalse(lava.isWalkable(p), "Solid prop " + p);
            assertFalse(lava.isLethalFall(p), "A solid prop is not lava");
        }
        for (Vector2 p : List.of(p2(610,300), p2(143,663), p2(10,500))) {
            assertFalse(lava.isWalkable(p), "Lava pool " + p);
            assertTrue(lava.isLethalFall(p));
        }
        assertTrue(lava.isWalkable(p2(170,730)), "Stone inside the narrow lava moat");
        assertTrue(lava.isWalkable(p2(460,166)));
        assertTrue(lava.isHazard(p2(460,166)), "Glowing crack burns but remains walkable");
        assertFalse(lava.isHazard(p2(440,210)), "Ordinary stone does not burn");
        assertTrue(lava.pathCrossesHazard(p2(460,130), p2(460,210)));
        assertFalse(lava.pathCrossesLethalFall(p2(530,425), p2(530,340)), "Parapet stops knockback");
        assertTrue(lava.pathCrossesLethalFall(p2(500,180), p2(640,180)), "Open island edge is lethal");
    }

    @Test void lavaIslandsConnectByBridgesAndDetachedIslandRequiresTeleport() {
        routes(lava, List.of(p2(220,240), p2(420,735), p2(850,440), p2(1430,560), p2(1190,815)));
        assertTrue(lava.isWalkable(p2(800,790)));
        assertTrue(new BattlefieldNavigation(lava).findPath(lava.spawnFor(1,0), p2(800,790), 100f, List.of()).isEmpty());
        for (int team = 1; team <= 2; team++) for (int slot = 0; slot < 2; slot++) {
            assertFalse(lava.isHazard(lava.spawnFor(team, slot)), "Spawn is safe from burn");
        }
    }

    @Test void firstAndFinalMapOrderAndAspectRatioAreExplicit() {
        assertEquals(List.of(Environment.CANYON, Environment.LAVA, Environment.BOG), Environment.selectionOrder());
        assertEquals(1.5f, BattlefieldArtwork.MAP3.width / 17f);
        assertEquals(new Vector2(15f, 8.5f), BattlefieldArtwork.MAP3.worldPoint(768, 512));
    }

    @Test void mapOneBlocksWaterTreesRocksAndRaisedCliffsButKeepsBridgesStairsOpen() {
        for (Vector2 p : List.of(p1(410,500), p1(630,320), p1(650,565), p1(740,230), p1(1495,280))) {
            assertFalse(first.isWalkable(p), "Blocked Map 1 point " + p);
        }
        for (Vector2 p : List.of(p1(410,405), p1(1260,405), p1(835,235), p1(835,180), p1(820,775))) {
            assertTrue(first.isWalkable(p), "Walkable Map 1 point " + p);
        }
        assertFalse(first.pathIsWalkable(p1(740,190), p1(740,280)), "Must use the staircase");
        assertFalse(first.pathCrossesLethalFall(p1(410,405), p1(410,320)), "Bridge railing stops a push before water");
        assertFalse(first.pathCrossesLethalFall(p1(200,350), p1(200,100)), "Tree stops a push before water");
    }

    @Test void finalMapKeepsBothBridgesAndAllStaircasesWalkableAndPropsSolid() {
        for (Vector2 p : List.of(p3(345,300), p3(790,823), p3(169,400), p3(520,390),
            p3(403,640), p3(1110,340), p3(1340,497), p3(1245,710))) {
            assertTrue(last.isWalkable(p), "Walkable Map 3 point " + p);
        }
        for (Vector2 p : List.of(p3(800,400), p3(440,115), p3(205,605), p3(1375,280), p3(320,820), p3(790,630))) {
            assertFalse(last.isWalkable(p), "Blocked Map 3 point " + p);
        }
        assertTrue(last.isHazard(p3(1250,310)));
        assertFalse(last.isHazard(p3(180,500)));
        assertFalse(last.isHazard(p3(800,400)), "The purple sea is not a walkable poison tile");
    }

    @Test void everyMainRegionIsReachableAlongCollisionSafePaths() {
        routes(first, List.of(p1(200,400), p1(835,180), p1(1510,530), p1(820,775)));
        routes(last, List.of(p3(200,330), p3(520,270), p3(430,850), p3(1250,310), p3(1380,805), p3(1070,858)));
    }

    @Test void routeUsesBridgeInsteadOfCuttingAcrossWaterAndHonorsBudget() {
        Vector2 start = p1(270,510), goal = p1(560,510);
        BattlefieldNavigation nav = new BattlefieldNavigation(first);
        assertFalse(first.pathIsWalkable(start, goal));
        List<Vector2> path = nav.findPath(start, goal, 40f, List.of());
        assertFalse(path.isEmpty());
        assertEquals(goal, path.get(path.size() - 1));
        assertTrue(BattlefieldNavigation.length(start, path) > start.dst(goal));
        List<Vector2> limited = nav.findPath(start, goal, 2.3f, List.of());
        assertEquals(2.3f, BattlefieldNavigation.length(start, limited), 0.001f);
        Vector2 previous = start;
        for (Vector2 point : limited) { assertTrue(first.pathIsWalkable(previous, point)); previous = point; }
    }

    private void routes(BattlefieldDefinition field, List<Vector2> goals) {
        BattlefieldNavigation nav = new BattlefieldNavigation(field);
        Vector2 start = field.spawnFor(1,0);
        for (Vector2 goal : goals) {
            assertTrue(field.isWalkable(goal), "Region ground " + goal);
            List<Vector2> path = nav.findPath(start,goal,100f,List.of());
            assertFalse(path.isEmpty(), "Unreachable region " + goal);
            assertEquals(goal,path.get(path.size()-1));
            Vector2 previous=start;
            for(Vector2 point:path) { assertTrue(field.pathIsWalkable(previous,point)); previous=point; }
        }
    }
    private Vector2 p1(float x,float y) { return BattlefieldArtwork.MAP1.worldPoint(x,y); }
    private Vector2 p2(float x,float y) { return BattlefieldArtwork.MAP2.worldPoint(x,y); }
    private Vector2 p3(float x,float y) { return BattlefieldArtwork.MAP3.worldPoint(x,y); }
}
