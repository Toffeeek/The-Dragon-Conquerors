package com.github.thedragonconquerors.input;

import com.badlogic.gdx.math.Vector2;
import com.github.thedragonconquerors.entities.Player;
import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.ability.AbilityType;
import com.shared.shared.network.PlayerCombatState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattleInteractionTest {
    private Player player(boolean active, int ap, float movement) {
        Player player = new Player(0, "Tester", new Vector2(2, 5), CharacterClass.MAGE);
        player.applyCombatState(PlayerCombatState.builder().id(0).hp(100).maxHp(100)
            .mana(100).maxMana(100).activeTurn(active).actionPoints(ap).actionUsed(ap == 0)
            .maxMovement(6).remainingMovement(movement).build());
        return player;
    }
    @Test void requiresExplicitModeAndSwitchesExclusively() {
        BattleInteraction input = new BattleInteraction();
        Player p = player(true, 1, 6);
        assertEquals(BattleInteraction.Mode.NONE, input.mode());
        input.chooseMove(p, false);
        assertEquals(BattleInteraction.Mode.MOVE, input.mode());
        input.chooseAction(p, false);
        assertEquals(BattleInteraction.Mode.ACTION, input.mode());
        input.cancel();
        assertEquals(BattleInteraction.Mode.NONE, input.mode());
    }
    @Test void cannotControlAnotherPlayersTurnOrDuringAnimation() {
        BattleInteraction input = new BattleInteraction();
        input.chooseMove(player(false, 1, 6), false);
        assertEquals(BattleInteraction.Mode.NONE, input.mode());
        input.chooseAction(player(true, 1, 6), true);
        assertEquals(BattleInteraction.Mode.NONE, input.mode());
        assertFalse(input.canControl(player(false, 1, 6), false));
    }
    @Test void movementAndActionResourcesRemainIndependent() {
        BattleInteraction input = new BattleInteraction();
        assertTrue(input.canAct(player(true, 1, 0), false));
        assertFalse(input.canMove(player(true, 1, 0), false));
        assertTrue(input.canMove(player(true, 0, 6), false));
        assertFalse(input.canAct(player(true, 0, 6), false));
    }
    @Test void pendingCommandBlocksDoubleClicksUntilServerResponse() {
        BattleInteraction input = new BattleInteraction();
        Player p = player(true, 1, 6);
        input.chooseAction(p, false);
        input.sentCommand();
        assertEquals(BattleInteraction.Mode.NONE, input.mode());
        assertFalse(input.canControl(p, false));
        input.chooseMove(p, false);
        assertEquals(BattleInteraction.Mode.NONE, input.mode());
        input.receivedResponse();
        assertTrue(input.canControl(p, false));
    }
    @Test void abilityButtonsRespectManaAndDeath() {
        BattleInteraction input = new BattleInteraction();
        Player p = player(true, 1, 6);
        assertTrue(input.canUse(p, false, AbilityType.FIREBALL));
        p.getStats().setMana(0);
        assertFalse(input.canUse(p, false, AbilityType.FIREBALL));
        assertTrue(input.canUse(p, false, AbilityType.MAGE_BASIC));
        p.getStats().setHp(0);
        assertFalse(input.canAct(p, false));
    }
    @Test void abilityButtonsRespectCooldown() {
        BattleInteraction input = new BattleInteraction();
        Player p = player(true, 1, 6);
        p.applyCombatState(PlayerCombatState.builder().id(0).hp(100).maxHp(100)
            .mana(100).maxMana(100).activeTurn(true).actionPoints(1)
            .cooldowns(java.util.Map.of(AbilityType.FIREBALL.getDisplayName(), 2)).build());
        assertFalse(input.canUse(p, false, AbilityType.FIREBALL));
        assertTrue(input.canUse(p, false, AbilityType.MAGE_BASIC));
    }
    @Test void newTurnClearsPreviousModeAndPendingCommand() {
        BattleInteraction input = new BattleInteraction();
        input.sentCommand();
        input.reset();
        assertFalse(input.awaitingServer());
        assertEquals(BattleInteraction.Mode.NONE, input.mode());
    }
}
