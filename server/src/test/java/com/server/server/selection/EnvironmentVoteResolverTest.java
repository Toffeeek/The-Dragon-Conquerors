// File Location: server/src/test/java/com/server/server/selection/EnvironmentVoteResolverTest.java
package com.server.server.selection;

import com.shared.shared.model.world.Environment;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class EnvironmentVoteResolverTest {
    @Test
    void majorityWins() {
        EnvironmentVoteResolver resolver = new EnvironmentVoteResolver(new Random(7));
        assertEquals(Environment.BOG, resolver.resolve(List.of(
            Environment.BOG, Environment.LAVA, Environment.BOG, Environment.CANYON)));
    }

    @Test
    void tieIsResolvedOnlyAmongSelectedLeaders() {
        EnvironmentVoteResolver resolver = new EnvironmentVoteResolver(new Random(3));
        Environment result = resolver.resolve(List.of(
            Environment.BOG, Environment.BOG, Environment.LAVA, Environment.LAVA));
        assertNotEquals(Environment.CANYON, result);
    }
}
