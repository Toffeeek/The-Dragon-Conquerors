package com.github.thedragonconquerors.combat;

import com.github.thedragonconquerors.entities.Player;
import com.shared.shared.model.stats.StatCalculator;
import com.shared.shared.model.stats.StatComponent;

import java.util.Random;

/** Executes an action against the player explicitly selected by the user. */
public class ActionSystem {
    private static final Random RNG = new Random();

    public ActionResult execute(Player actor, Player target, ActionType action) {
        StatComponent stats = actor.getStats();

        if (action.manaCost > 0 && stats.getMana() < action.manaCost) {
            return ActionResult.noMana(action);
        }

        if (action.targetsSelf) {
            return executeSelfAction(actor, action);
        }

        if (target == null || target == actor || target.getStats().getHp() <= 0) {
            return ActionResult.invalidTarget(action);
        }

        float distance = actor.getPosition().dst(target.getPosition());
        if (distance > action.range) {
            return ActionResult.outOfRange(action, distance, action.range);
        }

        if (action.manaCost > 0) stats.spendMana(action.manaCost);

        int hitChance = StatCalculator.hitChances(stats);
        boolean hit = RNG.nextInt(100) < hitChance;
        if (!hit) return ActionResult.miss(action);

        int strengthModifier = (stats.getStrength() - 10) / 2;
        int damage = Math.max(1, action.baseDamage + strengthModifier);
        float variance = 0.8f + RNG.nextFloat() * 0.4f;
        damage = Math.max(1, Math.round(damage * variance));

        target.getStats().applyDamage(damage);
        System.out.println("[Combat] " + actor.getUsername() + " used "
            + action.displayName + " on " + target.getUsername() + " for " + damage
            + " damage. Target HP: " + target.getStats().getHp() + "/"
            + target.getStats().getMaxHp());

        return ActionResult.hit(action, damage);
    }

    private ActionResult executeSelfAction(Player actor, ActionType action) {
        StatComponent stats = actor.getStats();
        if (action.manaCost > 0) stats.spendMana(action.manaCost);

        switch (action) {
            case DEFEND:
                return ActionResult.selfEffect(action, 0,
                    "Incoming damage reduced this turn.");
            case WAR_CRY:
                int oldStrength = stats.getStrength();
                stats.setStrength(Math.min(20, oldStrength + 3));
                return ActionResult.selfEffect(action, 0,
                    "Strength boosted by 3 for this turn!");
            case ARCANE_SHIELD:
                return ActionResult.selfEffect(action, 0,
                    "Next hit absorbed by mana barrier!");
            case EVASIVE_ROLL:
                return ActionResult.selfEffect(action, 0,
                    "Evasion greatly increased this turn!");
            case LAY_ON_HANDS:
                int healing = 30;
                stats.heal(healing);
                return ActionResult.selfEffect(action, healing,
                    "Restored " + healing + " HP.");
            case DIVINE_SHIELD:
                return ActionResult.selfEffect(action, 0,
                    "Immune to damage this turn!");
            case SMOKE_BOMB:
                return ActionResult.selfEffect(action, 0,
                    "Vanished in smoke — untargetable!");
            default:
                return ActionResult.selfEffect(action, 0,
                    action.displayName + " activated.");
        }
    }
}
