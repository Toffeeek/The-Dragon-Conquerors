// File Location: shared/src/main/java/com/shared/shared/model/ability/Ability.java
package com.shared.shared.model.ability;

import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.effect.StatusEffectType;
import com.shared.shared.model.stats.StatBoost;

/**
 * Contract every ability must satisfy, whether it comes from the built-in
 * {@link AbilityType} catalogue or from a class a teammate adds later.
 *
 * <p>The combat resolver is written against this interface, never against the
 * enum, so new abilities can be introduced without touching resolution logic.
 * That is the extensibility requirement in the design document: an ability is a
 * bundle of declarative numbers plus an optional status effect, and the resolver
 * reads those numbers generically.</p>
 *
 * <p><b>To add a bespoke ability</b> that the enum cannot express (a multi-stage
 * channel, say), implement this interface in its own file under
 * {@code model/ability/} and register it wherever abilities are looked up.
 * Nothing in the resolver needs to change.</p>
 */
public interface Ability {

    /** Name shown on the action bar. */
    String getDisplayName();

    /** One-line tooltip explaining what the ability does. */
    String getDescription();

    /** Class that can use this ability, or {@code null} if it is universal. */
    CharacterClass getOwnerClass();

    /** Action-bar slot, which also determines the hotkey and the wire action. */
    AbilitySlot getSlot();

    /** Mana spent on use; 0 means free. */
    int getManaCost();

    /** Raw damage before stat scaling; 0 for non-damaging abilities. */
    int getBaseDamage();

    /** Healing applied before stat scaling; 0 for non-healing abilities. */
    int getBaseHealing();

    /** Maximum distance to a legal target, in world units. */
    float getRange();

    /** Radius affected around the target point; 0 for single-target abilities. */
    float getAreaRadius();

    /** What the ability may be aimed at. */
    TargetType getTargetType();

    /** Turns before the ability can be used again; 0 means every turn. */
    int getCooldownTurns();

    /** Status effect this ability may inflict, or {@code null} if none. */
    StatusEffectType getAppliedEffect();

    /**
     * Probability in percent that {@link #getAppliedEffect()} lands.
     *
     * <p>The design document specifies 50% for Fireball's Burn, Ice Attack's
     * Sub-zero, Curse and Poison Jab. Guaranteed effects use 100.</p>
     */
    int getEffectChancePercent();

    /**
     * Stat change this ability grants to its target, or {@code null} if none.
     *
     * <p>Declared rather than hard-coded so the Bard's Stat Boost and the
     * Archer's Accuracy Boost need no special case in the resolver — both are
     * simply abilities that carry a {@link StatBoost} payload.</p>
     */
    StatBoost getGrantedBoost();

    /**
     * World-unit distance the target is pushed away from the actor; 0 for
     * abilities that do not displace ({@code Eldritch Blast} is the only
     * built-in ability that does).
     */
    float getPushDistance();

    /**
     * True when this ability clears its target's ultimate cooldown.
     *
     * <p>Declared rather than detected so the Bard's {@code Encore} needs no
     * special case in the resolver — it is simply the one ability whose payload is
     * a cooldown reset. Defaulted to false so nothing else has to implement it.</p>
     *
     * <p>An ability never resets <em>itself</em>; the resolver enforces that. Encore
     * is a Bard ultimate, so without the rule a Bard could target itself, clear
     * Encore's own 6-turn cooldown, and cast it again every turn for the mana
     * alone.</p>
     */
    default boolean restoresUltimate() {
        return false;
    }

    /** True when this ability occupies the ultimate slot. */
    default boolean isUltimate() {
        return getSlot() != null && getSlot().isUltimate();
    }

    /** True when the ability can inflict a status effect. */
    default boolean hasStatusEffect() {
        return getAppliedEffect() != null && getEffectChancePercent() > 0;
    }

    /** True when the ability deals damage to its target. */
    default boolean isOffensive() {
        return getBaseDamage() > 0;
    }

    /** True when the ability restores HP. */
    default boolean isHealing() {
        return getBaseHealing() > 0;
    }

    /** True when the ability displaces its target. */
    default boolean pushesTarget() {
        return getPushDistance() > 0f;
    }
}
