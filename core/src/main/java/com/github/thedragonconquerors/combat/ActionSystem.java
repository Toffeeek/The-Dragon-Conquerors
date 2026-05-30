package com.github.thedragonconquerors.combat;

import com.github.thedragonconquerors.entities.Player;
import com.github.thedragonconquerors.stats.StatCalculator;
import com.github.thedragonconquerors.stats.StatComponent;

import java.util.List;
import java.util.Random;

/**
 * Executes actions in combat and returns an ActionResult describing what happened.
 *
 * Damage formula:
 *   finalDamage = (action.baseDamage + strengthBonus) * hitRoll
 *   strengthBonus = (strength - 10) / 2   (D&D-style modifier)
 *   hitRoll = random check vs StatCalculator.hitChances()
 *
 * Self-targeting actions (heals, buffs) skip the range/target check.
 *
 * Usage from GameOneScreen:
 * <pre>
 *   ActionResult result = actionSystem.execute(localPlayer, enemies, selectedAction);
 *   System.out.println(result.message);
 * </pre>
 */
public class ActionSystem {

    private static final Random RNG = new Random();

    // ── execute ────────────────────────────────────────────────────────────

    /**
     * Execute an action for the acting player against the nearest enemy in range.
     *
     * @param actor    the player performing the action
     * @param enemies  list of enemy players to potentially target
     * @param action   the action to perform
     * @return ActionResult describing what happened
     */
    public ActionResult execute(Player actor, List<Player> enemies, ActionType action) {
        StatComponent stats = actor.getStats();

        // ── mana check ────────────────────────────────────────────
        if (action.manaCost > 0 && stats.getMana() < action.manaCost) {
            return ActionResult.noMana(action);
        }

        // ── self-targeting actions ─────────────────────────────────
        if (action.targetsSelf) {
            return executeSelfAction(actor, action);
        }

        // ── find nearest enemy in range ───────────────────────────
        Player target = nearestInRange(actor, enemies, action.range);
        if (target == null) {
            // report nearest enemy distance for feedback
            Player nearest = nearest(actor, enemies);
            if (nearest == null) return ActionResult.invalidTarget(action);
            float dist = actor.getPosition().dst(nearest.getPosition());
            return ActionResult.outOfRange(action, dist, action.range);
        }

        // ── spend mana ────────────────────────────────────────────
        if (action.manaCost > 0) stats.spendMana(action.manaCost);

        // ── hit roll ──────────────────────────────────────────────
        int hitChance = StatCalculator.hitChances(stats);
        boolean hit = RNG.nextInt(100) < hitChance;
        if (!hit) return ActionResult.miss(action);

        // ── damage calculation ────────────────────────────────────
        int strMod  = (stats.getStrength() - 10) / 2;
        int damage  = Math.max(1, action.baseDamage + strMod);

        // small random variance ±20%
        float variance = 0.8f + RNG.nextFloat() * 0.4f;
        damage = Math.max(1, Math.round(damage * variance));

        target.getStats().applyDamage(damage);

        System.out.println("[Combat] " + actor.getUsername() + " used " + action.displayName
            + " on target for " + damage + " damage. Target HP: "
            + target.getStats().getHp() + "/" + target.getStats().getMaxHp());

        return ActionResult.hit(action, damage);
    }

    // ── self-action handler ────────────────────────────────────────────────

    private ActionResult executeSelfAction(Player actor, ActionType action) {
        StatComponent stats = actor.getStats();

        if (action.manaCost > 0) stats.spendMana(action.manaCost);

        switch (action) {
            case DEFEND:
                // Flag could be read by a damage-reduction system — logged for now
                System.out.println("[Combat] " + actor.getUsername() + " is defending.");
                return ActionResult.selfEffect(action, 0, "Incoming damage reduced this turn.");

            case WAR_CRY:
                // Temporary strength bump — a buff system can expand this later
                int oldStr = stats.getStrength();
                stats.setStrength(Math.min(20, oldStr + 3));
                System.out.println("[Combat] War Cry! Strength " + oldStr + " → " + stats.getStrength());
                return ActionResult.selfEffect(action, 0, "Strength boosted by 3 for this turn!");

            case ARCANE_SHIELD:
                System.out.println("[Combat] Arcane Shield raised.");
                return ActionResult.selfEffect(action, 0, "Next hit absorbed by mana barrier!");

            case EVASIVE_ROLL:
                System.out.println("[Combat] Evasive Roll!");
                return ActionResult.selfEffect(action, 0, "Evasion greatly increased this turn!");

            case LAY_ON_HANDS: {
                int heal = 30;
                stats.heal(heal);
                System.out.println("[Combat] Lay on Hands — healed " + heal + " HP.");
                return ActionResult.selfEffect(action, heal, "Restored " + heal + " HP.");
            }

            case DIVINE_SHIELD:
                System.out.println("[Combat] Divine Shield!");
                return ActionResult.selfEffect(action, 0, "Immune to damage this turn!");

            case SMOKE_BOMB:
                System.out.println("[Combat] Smoke Bomb — untargetable!");
                return ActionResult.selfEffect(action, 0, "Vanished in smoke — untargetable!");

            default:
                return ActionResult.selfEffect(action, 0, action.displayName + " activated.");
        }
    }

    // ── helpers ────────────────────────────────────────────────────────────

    /** Returns the nearest enemy within range, or null if none. */
    private Player nearestInRange(Player actor, List<Player> enemies, float range) {
        Player best = null;
        float bestDist = Float.MAX_VALUE;
        for (Player e : enemies) {
            float d = actor.getPosition().dst(e.getPosition());
            if (d <= range && d < bestDist) {
                best = e;
                bestDist = d;
            }
        }
        return best;
    }

    /** Returns the nearest enemy regardless of range (for out-of-range feedback). */
    private Player nearest(Player actor, List<Player> enemies) {
        Player best = null;
        float bestDist = Float.MAX_VALUE;
        for (Player e : enemies) {
            float d = actor.getPosition().dst(e.getPosition());
            if (d < bestDist) { best = e; bestDist = d; }
        }
        return best;
    }
}
