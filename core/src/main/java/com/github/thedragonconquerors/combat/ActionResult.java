package com.github.thedragonconquerors.combat;

/**
 * Describes what happened when an action was executed.
 * Returned by ActionSystem.execute() so callers (GameOneScreen, HUD) can
 * display feedback without coupling to combat internals.
 */
public class ActionResult {

    public enum Outcome { HIT, MISS, NO_MANA, OUT_OF_RANGE, INVALID_TARGET, SELF_EFFECT }

    public final Outcome    outcome;
    public final ActionType action;
    public final int        damageDealt;
    public final int        healingDone;
    public final String     message;

    private ActionResult(Outcome outcome, ActionType action,
                         int damageDealt, int healingDone, String message) {
        this.outcome     = outcome;
        this.action      = action;
        this.damageDealt = damageDealt;
        this.healingDone = healingDone;
        this.message     = message;
    }

    // ── factories ──────────────────────────────────────────────────

    public static ActionResult hit(ActionType action, int damage) {
        return new ActionResult(Outcome.HIT, action, damage, 0,
            action.displayName + " hit for " + damage + " damage!");
    }

    public static ActionResult miss(ActionType action) {
        return new ActionResult(Outcome.MISS, action, 0, 0,
            action.displayName + " missed!");
    }

    public static ActionResult noMana(ActionType action) {
        return new ActionResult(Outcome.NO_MANA, action, 0, 0,
            "Not enough mana for " + action.displayName + "!");
    }

    public static ActionResult outOfRange(ActionType action, float dist, float range) {
        return new ActionResult(Outcome.OUT_OF_RANGE, action, 0, 0,
            action.displayName + " out of range (need " + String.format("%.1f", range)
                + ", dist " + String.format("%.1f", dist) + ")");
    }

    public static ActionResult invalidTarget(ActionType action) {
        return new ActionResult(Outcome.INVALID_TARGET, action, 0, 0,
            action.displayName + ": no valid target.");
    }

    public static ActionResult selfEffect(ActionType action, int healing, String detail) {
        return new ActionResult(Outcome.SELF_EFFECT, action, 0, healing,
            action.displayName + ": " + detail);
    }

    @Override
    public String toString() { return message; }
}
