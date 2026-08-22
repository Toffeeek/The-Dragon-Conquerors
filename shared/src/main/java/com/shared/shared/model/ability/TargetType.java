// File Location: shared/src/main/java/com/shared/shared/model/ability/TargetType.java
package com.shared.shared.model.ability;

/**
 * What an ability can legally be aimed at.
 *
 * <p>The server uses this to validate an incoming action before applying it, and
 * the client uses it to decide what to highlight during target selection — which
 * is why it lives in {@code shared} rather than in either module.</p>
 */
public enum TargetType {

    /** No target needed; resolves on the actor (Archer's Accuracy Boost). */
    SELF("Self"),

    /** A living teammate (Cleric's Heal, Bard's Stat Boost and Encore). */
    ALLY("Ally"),

    /** A teammate at 0 HP (Cleric's ultimate, Revive). */
    DOWNED_ALLY("Downed ally"),

    /** A single living opponent. */
    ENEMY("Enemy"),

    /** An arbitrary walkable tile (Wraith's ultimate, Teleport). */
    TILE("Tile"),

    /** Every opponent inside a radius around a chosen point (Rain of Arrows). */
    AREA_ENEMIES("Area");

    private final String displayName;

    TargetType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** True when the client must prompt for a click before sending the action. */
    public boolean requiresTargetSelection() {
        return this != SELF;
    }

    /** True when the valid target is a friendly character. */
    public boolean targetsFriendly() {
        return this == ALLY || this == DOWNED_ALLY;
    }

    /** True when the valid target is a hostile character. */
    public boolean targetsHostile() {
        return this == ENEMY || this == AREA_ENEMIES;
    }

    /** True when the target is a map position rather than a character. */
    public boolean targetsGround() {
        return this == TILE || this == AREA_ENEMIES;
    }
}
