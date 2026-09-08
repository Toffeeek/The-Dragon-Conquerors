// File Location: shared/src/test/java/com/shared/shared/model/GameInstanceTest.java
package com.shared.shared.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GameInstanceTest {
    @Test
    void builderKeepsTheDefaultCoordinateSlots() {
        GameInstance instance = GameInstance.builder().build();

        assertNotNull(instance.getPlayerCoordinates());
        assertEquals(2, instance.getPlayerCoordinates().length);
    }
}
