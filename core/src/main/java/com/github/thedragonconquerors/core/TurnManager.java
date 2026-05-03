package com.github.thedragonconquerors.core;

import com.github.thedragonconquerors.entities.Player;
import com.github.thedragonconquerors.input.MouseInputHandler;
import com.github.thedragonconquerors.movement.MovementSystem;

public class TurnManager {
    private final MouseInputHandler mouseInputHandler;
    private final Player player;

    private int turnNumber = 1;
    private boolean turnActive = false;

    public TurnManager(MouseInputHandler mouseInputHandler, Player player){
        this.mouseInputHandler = mouseInputHandler;
        this.player = player;
    }

    public void beginTurn(){
        player.getMovementController().resetForNewTurn();
        mouseInputHandler.setLocalPlayerTurn(true);
        turnActive = true;
    }

    public void endTurn(){
        if(!turnActive) return;

        mouseInputHandler.setLocalPlayerTurn(false);
        turnActive = false;
        turnNumber++;

        beginTurn();
    }

    public void notifyStaminaExhausted(){
        endTurn();
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public boolean isTurnActive() {
        return turnActive;
    }
}
