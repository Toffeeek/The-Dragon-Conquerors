package com.github.thedragonconquerors.input;

import com.github.thedragonconquerors.entities.Player;
import com.shared.shared.model.ability.AbilityType;

/** Presentation state only; the server still validates every command. */
public final class BattleInteraction {
    public enum Mode { NONE, MOVE, ACTION }
    private Mode mode = Mode.NONE;
    private boolean awaitingServer;
    public Mode mode() { return mode; }
    public boolean awaitingServer() { return awaitingServer; }
    public boolean canControl(Player p, boolean moving) {
        return p != null && p.isAlive() && p.isActiveTurn() && !moving && !awaitingServer;
    }
    public boolean canMove(Player p, boolean moving) {
        return canControl(p, moving) && p.getMovementController().getRemainingMovementDistance() > 0.001f;
    }
    public boolean canAct(Player p, boolean moving) {
        return canControl(p, moving) && !p.isActionUsed() && p.getActionPoints() > 0;
    }
    public boolean canUse(Player p, boolean moving, AbilityType ability) {
        return canAct(p, moving) && ability != null && p.cooldownTurns(ability) == 0
            && p.getStats().getMana() >= ability.getManaCost();
    }
    public void chooseMove(Player p, boolean moving) { mode = canMove(p, moving) ? Mode.MOVE : Mode.NONE; }
    public void chooseAction(Player p, boolean moving) { mode = canAct(p, moving) ? Mode.ACTION : Mode.NONE; }
    public void cancel() { mode = Mode.NONE; }
    public void sentCommand() { awaitingServer = true; cancel(); }
    public void receivedResponse() { awaitingServer = false; }
    public void reset() { receivedResponse(); cancel(); }
}
