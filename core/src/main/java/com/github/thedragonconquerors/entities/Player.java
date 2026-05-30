package com.github.thedragonconquerors.entities;

import com.badlogic.gdx.math.Vector2;
import com.github.thedragonconquerors.movement.MovementController;
import com.github.thedragonconquerors.stats.StatCalculator;
import com.github.thedragonconquerors.stats.StatComponent;

/**
 * Represents a single player (local or remote) on the game board.
 *
 * CharacterClass is now a first-class field:
 *   - stats are derived from the class when no explicit StatComponent is supplied
 *   - the renderer uses the class to look up the correct sprite via SpriteAssets
 *
 * Constructors (in order of preference):
 *   Player(id, username, x, y, CharacterClass)            ← preferred for local player
 *   Player(id, username, Vector2, CharacterClass)          ← preferred for remote player
 *   Player(id, username, x, y, StatComponent)             ← legacy — class defaults to WARRIOR
 *   Player(id, username, x, y)                            ← legacy — class + stats default
 *   Player(id, username, Vector2)                         ← legacy — class + stats default
 */
public class Player {

    // ── identity ──────────────────────────────────────────────────
    private final int ID;
    private final String username;

    // ── class & stats ─────────────────────────────────────────────
    /** The character class this player has chosen. Never null. */
    private final CharacterClass characterClass;
    private final StatComponent stats;

    // ── movement ──────────────────────────────────────────────────
    private final Vector2 position;
    private final float speed;          // visual movement speed (world-units / second)
    private final MovementController movementController;

    // ──────────────────────────────────────────────────────────────
    //  Preferred constructors
    // ──────────────────────────────────────────────────────────────

    /**
     * Full constructor — everything explicit.
     * Stats are taken directly from the supplied StatComponent (use this if you
     * want to override the class defaults, e.g. for a levelled-up character).
     */
    public Player(int ID, String username, float startX, float startY,
                  CharacterClass characterClass, StatComponent stats) {
        this.ID              = ID;
        this.username        = username;
        this.characterClass  = characterClass;
        this.stats           = stats;
        this.position        = new Vector2(startX, startY);
        this.speed           = 5f;
        this.movementController = new MovementController(
            StatCalculator.deriveMaxMovementDistance(stats));
    }

    /** Spawns a player using the class's own base stats. */
    public Player(int ID, String username, float startX, float startY,
                  CharacterClass characterClass) {
        this(ID, username, startX, startY, characterClass,
             characterClass.createBaseStats());
    }

    /** Remote-player variant (position as Vector2). */
    public Player(int ID, String username, Vector2 startingPosition,
                  CharacterClass characterClass) {
        this(ID, username, startingPosition.x, startingPosition.y, characterClass);
    }

    // ──────────────────────────────────────────────────────────────
    //  Legacy constructors (kept for backward compatibility)
    // ──────────────────────────────────────────────────────────────

    /** Legacy — explicit StatComponent, class defaults to WARRIOR. */
    public Player(int ID, String username, float startX, float startY, StatComponent stats) {
        this(ID, username, startX, startY, CharacterClass.WARRIOR, stats);
    }

    /** Legacy — defaults to WARRIOR class with default stats. */
    public Player(int ID, String username, float startX, float startY) {
        this(ID, username, startX, startY, CharacterClass.WARRIOR,
             StatComponent.defaultStats());
    }

    /** Legacy — remote player, defaults to WARRIOR class with default stats. */
    public Player(int ID, String username, Vector2 startingPosition) {
        this(ID, username, startingPosition.x, startingPosition.y,
             CharacterClass.WARRIOR, StatComponent.defaultStats());
    }

    // ──────────────────────────────────────────────────────────────
    //  Turn lifecycle
    // ──────────────────────────────────────────────────────────────

    public void onTurnStart() {
        movementController.resetForNewTurn();
        stats.regenerateMana(StatCalculator.manaRegenPerTurn(stats));
    }

    // ──────────────────────────────────────────────────────────────
    //  Accessors
    // ──────────────────────────────────────────────────────────────

    public int getID()                               { return ID; }
    public String getUsername()                      { return username; }
    public CharacterClass getCharacterClass()        { return characterClass; }
    public StatComponent getStats()                  { return stats; }
    public Vector2 getPosition()                     { return position; }
    public float getSpeed()                          { return speed; }
    public MovementController getMovementController(){ return movementController; }

    public void setPosition(float x, float y)        { position.set(x, y); }
}
