// File Location: shared/src/test/java/com/shared/shared/model/combat/TurnQueueTest.java
package com.shared.shared.model.combat;

import com.badlogic.gdx.math.Vector2;
import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.effect.StatusEffect;
import com.shared.shared.model.stats.StatComponent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnQueueTest {
    @Test
    void mutualWipeIsADraw() {
        TestCombatant azure = combatant(0, 1, false);
        TestCombatant crimson = combatant(1, 2, false);
        TurnQueue queue = new TurnQueue(List.of(azure, crimson));

        assertTrue(queue.isMatchOver());
        assertEquals(TurnQueue.NO_WINNER, queue.winningTeam());
        assertNull(queue.advance());
    }

    @Test
    void oneLivingTeamWinsWhileTwoLivingTeamsContinue() {
        TestCombatant azure = combatant(0, 1, true);
        TestCombatant crimson = combatant(1, 2, true);
        TurnQueue queue = new TurnQueue(List.of(azure, crimson));

        assertFalse(queue.isMatchOver());
        crimson.getStats().setHp(0);
        assertTrue(queue.isMatchOver());
        assertEquals(1, queue.winningTeam());
    }

    private TestCombatant combatant(int id, int team, boolean alive) {
        TestCombatant combatant = new TestCombatant(id, team);
        if (!alive) combatant.getStats().setHp(0);
        return combatant;
    }

    private static final class TestCombatant implements Combatant {
        private final int id;
        private final int team;
        private final StatComponent stats = StatComponent.defaultStats();
        private final Vector2 position = new Vector2();
        private final List<StatusEffect> effects = new ArrayList<>();

        private TestCombatant(int id, int team) {
            this.id = id;
            this.team = team;
        }

        @Override public int getId() { return id; }
        @Override public String getUsername() { return "Player " + id; }
        @Override public CharacterClass getCharacterClass() { return CharacterClass.PALADIN; }
        @Override public StatComponent getStats() { return stats; }
        @Override public Vector2 getPosition() { return position; }
        @Override public int getTeamIndex() { return team; }
        @Override public List<StatusEffect> getActiveEffects() { return effects; }
        @Override public void onTurnStart() { }
    }
}
