// File Location: shared/src/main/java/com/shared/shared/model/ability/AbilitySlot.java
package com.shared.shared.model.ability;

import com.shared.shared.model.Action;

/**
 * Which action-bar slot an ability occupies.
 *
 * <p>Slot order is also hotkey order on the HUD: {@code PRIMARY} is key 1,
 * {@code ULTIMATE} is the last key. The mapping to {@link Action} exists because
 * the wire protocol already speaks in {@code PRIMARY / SECONDARY / ULTIMATE}
 * terms, so this enum is the bridge between the ability catalogue and the
 * packets sent to the server.</p>
 */
public enum AbilitySlot {

    PRIMARY("Primary", Action.PRIMARY),
    SECONDARY("Secondary", Action.SECONDARY),
    TERTIARY("Tertiary", Action.SECONDARY),
    ULTIMATE("Ultimate", Action.ULTIMATE);

    private final String displayName;
    private final Action wireAction;

    AbilitySlot(String displayName, Action wireAction) {
        this.displayName = displayName;
        this.wireAction = wireAction;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * The {@link Action} value used when this ability travels over the network.
     *
     * <p>{@code TERTIARY} shares {@code SECONDARY}'s wire value; the exact
     * ability is identified separately, so the coarse action category is enough
     * for the server's turn-phase validation.</p>
     */
    public Action toWireAction() {
        return wireAction;
    }

    public boolean isUltimate() {
        return this == ULTIMATE;
    }
}
