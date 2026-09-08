// File Location: shared/src/main/java/com/shared/shared/model/effect/StatusEffectType.java
package com.shared.shared.model.effect;

import com.shared.shared.model.stats.StatBoost;
import com.shared.shared.model.stats.StatType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Catalogue of every status effect in the game, with the data the effect engine
 * needs to resolve it.
 *
 * <p>Design document mapping:</p>
 * <ul>
 *   <li>{@link #POISON} — damage over time. Applied by the Wraith's Poison Jab
 *       (50% chance) and by Bog terrain tiles.</li>
 *   <li>{@link #BURN} — damage over time, <b>removed by ice</b>. Applied by the
 *       Mage's Fireball (50% chance) and by Lava environments every turn.</li>
 *   <li>{@link #SUB_ZERO} — slows the target, <b>removed by fire</b>. Applied by
 *       the Mage's Ice Attack (50% chance).</li>
 *   <li>{@link #CURSE} — Wraith only. Kills the target after
 *       {@value #CURSE_DURATION} turns unless the target damages the Wraith who
 *       cast it.</li>
 *   <li>{@link #STUN} — target loses its next turn. Applied by the Mage's
 *       ultimate, Eldritch Blast.</li>
 * </ul>
 *
 * <h2>Why cleansing lives in a static map</h2>
 *
 * <p>Burn removes Sub-zero and Sub-zero removes Burn — a mutual reference. Java
 * enum constructors cannot forward-reference a constant that has not been
 * initialised yet, so the relationship is built in a static block after all
 * constants exist. {@link #cleanses()} and {@link #cleansedBy()} read that map.</p>
 *
 * <h2>Adding an effect</h2>
 *
 * <p>Add a constant with its per-turn damage, duration and any stat modifier.
 * If it cleanses something, register the pair in the static block. The effect
 * engine reads these fields generically and needs no change.</p>
 */
public enum StatusEffectType {

    /** Damage over time; no natural counter. */
    POISON("Poison", "Takes damage at the start of each turn.", 6, 3, null, false),

    /** Damage over time; cleansed by Sub-zero. */
    BURN("Burn", "Burning — takes damage each turn until doused by ice.", 8, 3, null, false),

    /** Movement/initiative debuff; cleansed by Burn. */
    SUB_ZERO("Sub-zero", "Chilled — slowed until thawed by fire.", 0, 2,
        new StatBoost(StatType.SPEED, -2), false),

    /**
     * Wraith-only death sentence.
     *
     * <p>Deals no periodic damage. When its timer runs out the target dies
     * outright, unless the target has landed a hit on the casting Wraith while
     * the curse was active — tracked per-instance by
     * {@link StatusEffect#markCounterHit()}.</p>
     */
    CURSE("Curse", "Cursed — dies in 2 turns unless the caster is struck.",
        0, Durations.CURSE_TURNS, null, true),

    /** Skips the target's next turn. */
    STUN("Stun", "Stunned — loses the next turn.", 0, 1, null, false);

    /**
     * Durations the constant list above needs to read.
     *
     * <p>The holder is required rather than stylistic. JLS 8.3.3 forbids an enum
     * constant from reading a static field of its own enum by simple name,
     * because no static field of the enum is initialised until every constant
     * exists — writing {@code CURSE_DURATION} directly in the list above is an
     * "illegal forward reference" and will not compile. A qualified reference to
     * a <em>different</em> class carries no such restriction, and because these
     * are compile-time constants the compiler inlines them, so there is no
     * class-initialisation ordering to worry about either.</p>
     */
    private static final class Durations {

        /** Turns before Curse kills its target, per the design document. */
        private static final int CURSE_TURNS = 2;

        private Durations() {
        }
    }

    /**
     * Turns before {@link #CURSE} kills its target, per the design document.
     *
     * <p>Exposed separately from {@link #getDefaultDuration()} so UI and tests
     * can reference the design number without going through an enum instance.
     * Stays a compile-time constant, which is what lets {@code {@value}} above
     * resolve it.</p>
     */
    public static final int CURSE_DURATION = Durations.CURSE_TURNS;

    /** effect -> the effect it removes when applied. Both directions registered. */
    private static final Map<StatusEffectType, StatusEffectType> CLEANSES =
        new EnumMap<>(StatusEffectType.class);

    static {
        // Fire and ice cancel each other out, in both directions.
        CLEANSES.put(BURN, SUB_ZERO);
        CLEANSES.put(SUB_ZERO, BURN);
    }

    private final String displayName;
    private final String description;
    private final int damagePerTurn;
    private final int defaultDuration;
    private final StatBoost statModifier;
    private final boolean lethalOnExpiry;

    StatusEffectType(String displayName, String description, int damagePerTurn,
                     int defaultDuration, StatBoost statModifier, boolean lethalOnExpiry) {
        this.displayName = displayName;
        this.description = description;
        this.damagePerTurn = damagePerTurn;
        this.defaultDuration = defaultDuration;
        this.statModifier = statModifier;
        this.lethalOnExpiry = lethalOnExpiry;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /** Damage dealt at the start of each of the victim's turns; 0 for non-DoT effects. */
    public int getDamagePerTurn() {
        return damagePerTurn;
    }

    /** Turns the effect lasts when applied without an explicit duration. */
    public int getDefaultDuration() {
        return defaultDuration;
    }

    /**
     * Stat change applied while this effect is active, or {@code null} if none.
     *
     * <p>The engine applies this on gain and reverses it on loss, which is why
     * it is stored as a {@link StatBoost} rather than a raw number.</p>
     */
    public StatBoost getStatModifier() {
        return statModifier;
    }

    /** True when the target dies as the effect expires (Curse). */
    public boolean isLethalOnExpiry() {
        return lethalOnExpiry;
    }

    /** True when this effect deals periodic damage. */
    public boolean isDamageOverTime() {
        return damagePerTurn > 0;
    }

    /** The effect this one removes when applied, or {@code null}. */
    public StatusEffectType cleanses() {
        return CLEANSES.get(this);
    }

    /** The effect that removes this one, or {@code null}. */
    public StatusEffectType cleansedBy() {
        for (Map.Entry<StatusEffectType, StatusEffectType> entry : CLEANSES.entrySet()) {
            if (entry.getValue() == this) return entry.getKey();
        }
        return null;
    }

    /** Creates a live instance of this effect with its default duration. */
    public StatusEffect instantiate(int sourcePlayerId) {
        return new StatusEffect(this, defaultDuration, sourcePlayerId);
    }
}
