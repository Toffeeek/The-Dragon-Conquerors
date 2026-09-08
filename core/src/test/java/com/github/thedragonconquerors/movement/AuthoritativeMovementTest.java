package com.github.thedragonconquerors.movement;

import com.badlogic.gdx.math.Vector2;
import com.github.thedragonconquerors.entities.Player;
import com.shared.shared.model.CharacterClass;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AuthoritativeMovementTest {
    @Test void animationFinishesExactServerRouteEvenWithZeroMovementPoints() {
        Player player = new Player(0, "Tester", new Vector2(2f, 5f), CharacterClass.ARCHER);
        List<Vector2> route = List.of(new Vector2(3.5f, 5f), new Vector2(3.5f, 5.5f), new Vector2(4.137f, 5.58f));
        MovementController controller = player.getMovementController();
        controller.setAuthoritativePath(player.getPosition(), route, 0f);
        MovementSystem movement = new MovementSystem();
        for (int tick = 0; tick < 1000 && controller.isMoving(); tick++) {
            movement.update(player, 0.016f);
            assertEquals(0f, controller.getRemainingMovementDistance(), "Animation cannot manufacture movement points");
        }
        assertFalse(controller.isMoving());
        assertEquals(route.get(2), player.getPosition(), "Do not snap to a navigation node centre");
        assertTrue(controller.getRemainingPath().isEmpty(), "No stale path lines after arrival");
    }

    @Test void newTurnResourcesAreNotConsumedByThePreviousMovesAnimation() {
        Player player = new Player(0, "Tester", new Vector2(2f, 5f), CharacterClass.ARCHER);
        MovementController controller = player.getMovementController();
        controller.setAuthoritativePath(player.getPosition(), List.of(new Vector2(6f, 5f)), 0f);
        MovementSystem movement = new MovementSystem();
        movement.update(player, 0.1f);
        controller.synchronizeMovement(6f, 6f);
        for (int tick = 0; tick < 200 && controller.isMoving(); tick++) movement.update(player, 0.016f);
        assertFalse(controller.isMoving());
        assertEquals(new Vector2(6f, 5f), player.getPosition());
        assertEquals(6f, controller.getRemainingMovementDistance());
    }
}
