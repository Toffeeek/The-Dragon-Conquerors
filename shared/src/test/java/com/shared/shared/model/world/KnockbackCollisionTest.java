package com.shared.shared.model.world;

import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class KnockbackCollisionTest {
    @Test void stopsAtFirstPlayerEvenWhenPushWouldPassThroughThem() {
        Vector2 result = KnockbackCollision.stopBeforePlayers(new Vector2(0,0), new Vector2(10,0),
            List.of(new Vector2(6,0), new Vector2(3,0)));
        assertEquals(2.349f, result.x, .002f);
        assertEquals(0, result.y);
    }
    @Test void allowsClearAndZeroLengthPushes() {
        assertEquals(new Vector2(10,0), KnockbackCollision.stopBeforePlayers(new Vector2(), new Vector2(10,0), List.of(new Vector2(3,1))));
        assertEquals(new Vector2(), KnockbackCollision.stopBeforePlayers(new Vector2(), new Vector2(), List.of()));
    }
    @Test void touchingPlayersCanSeparateButCannotPushCloser() {
        var occupied = List.of(new Vector2(.65f,0));
        assertEquals(new Vector2(-1,0), KnockbackCollision.stopBeforePlayers(new Vector2(), new Vector2(-1,0), occupied));
        assertEquals(new Vector2(), KnockbackCollision.stopBeforePlayers(new Vector2(), new Vector2(1,0), occupied));
    }
}
