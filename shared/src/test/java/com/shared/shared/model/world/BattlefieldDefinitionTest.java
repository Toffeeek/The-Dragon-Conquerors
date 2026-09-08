// File Location: shared/src/test/java/com/shared/shared/model/world/BattlefieldDefinitionTest.java
package com.shared.shared.model.world;

import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattlefieldDefinitionTest {
    @Test
    void everyTeamSpawnIsWalkable() {
        for (Environment environment : Environment.values()) {
            BattlefieldDefinition battlefield = BattlefieldDefinition.forEnvironment(environment);
            for (int team = 1; team <= 2; team++) {
                assertTrue(battlefield.isWalkable(battlefield.spawnFor(team, 0)));
                assertTrue(battlefield.isWalkable(battlefield.spawnFor(team, 1)));
            }
        }
    }

    @Test
    void bogPoisonZonesRemainWalkable() {
        BattlefieldDefinition bog = BattlefieldDefinition.forEnvironment(Environment.BOG);
        Vector2 pool = new Vector2(8f, 3f);
        assertTrue(bog.isHazard(pool));
        assertTrue(bog.isWalkable(pool));
    }

    @Test
    void canyonChasmIsLethalAndBlocksPaths() {
        BattlefieldDefinition canyon = BattlefieldDefinition.forEnvironment(Environment.CANYON);
        Vector2 chasm = new Vector2(13f, 4f);
        assertTrue(canyon.isLethalFall(chasm));
        assertFalse(canyon.isWalkable(chasm));
        assertTrue(canyon.pathCrossesLethalFall(
            new Vector2(8f, 4f), new Vector2(17f, 4f)));
        assertFalse(canyon.pathIsWalkable(
            new Vector2(8f, 4f), new Vector2(17f, 4f)));
    }
}
