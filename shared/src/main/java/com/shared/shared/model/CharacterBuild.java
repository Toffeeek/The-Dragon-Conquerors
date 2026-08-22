// File Location: shared/src/main/java/com/shared/shared/model/CharacterBuild.java
package com.shared.shared.model;

import com.shared.shared.model.ability.AbilityType;
import com.shared.shared.model.stats.RaceClassSynergy;
import com.shared.shared.model.stats.StatBoost;
import com.shared.shared.model.stats.StatComponent;

import java.util.List;
import java.util.Objects;

/**
 * A race paired with a class — the complete character a player takes into a match.
 *
 * <p>The game flow in the design document is <em>class selection, then race
 * selection</em>, with the race screen showing the boosted stats. This type is
 * what makes that possible: {@link #createBaseStats()} is what the class screen
 * shows, {@link #createStats()} is what the race screen shows, and
 * {@link #appliedBoosts()} is the list of "+" markers between them.</p>
 *
 * <p>Immutable and cheap to construct, so the race screen can build one per
 * hovered race to preview stats without touching the player's real character.</p>
 *
 * <h2>Why not put this on {@link CharacterClass}</h2>
 *
 * <p>A class has no race and a race has no class; the boosts only exist for a
 * <em>pair</em>. Keeping the pair as its own type means neither enum has to know
 * about the other, and {@link RaceClassSynergy} stays the single place the
 * balance table lives.</p>
 */
public final class CharacterBuild {

    /**
     * Race assumed when a player never picked one.
     *
     * <p>Human is the deliberate choice: its baseline is Inspiration and its
     * generic affinity is a flat HP/Wisdom/Speed spread, which is the least
     * distorting default for any class.</p>
     */
    public static final Race DEFAULT_RACE = Race.HUMAN;

    private final Race race;
    private final CharacterClass characterClass;

    /**
     * @param race           may be {@code null}, which falls back to
     *                       {@link #DEFAULT_RACE}
     * @param characterClass must not be {@code null} — a build with no class has
     *                       no stats and no abilities, so there is nothing
     *                       sensible to fall back to
     */
    public CharacterBuild(Race race, CharacterClass characterClass) {
        if (characterClass == null) {
            throw new IllegalArgumentException("characterClass must not be null");
        }
        this.characterClass = characterClass;
        this.race = race == null ? DEFAULT_RACE : race;
    }

    /** Reads better than the constructor at call sites: {@code CharacterBuild.of(ELF, MAGE)}. */
    public static CharacterBuild of(Race race, CharacterClass characterClass) {
        return new CharacterBuild(race, characterClass);
    }

    public Race getRace() {
        return race;
    }

    public CharacterClass getCharacterClass() {
        return characterClass;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Stats
    // ──────────────────────────────────────────────────────────────────────

    /**
     * The class's stats with no racial boosts — what the class-selection screen
     * shows, before a race has been chosen.
     */
    public StatComponent createBaseStats() {
        return characterClass.createStats();
    }

    /**
     * The finished character's stats: class base with this race's boosts applied.
     *
     * <p>Returns a fresh instance every call, so two players who picked the same
     * build never share mutable stats.</p>
     */
    public StatComponent createStats() {
        return RaceClassSynergy.apply(characterClass.createStats(), race, characterClass);
    }

    /**
     * Every boost this pairing grants, baseline trait first — exactly the list
     * the race-selection screen renders next to the stat block.
     */
    public List<StatBoost> appliedBoosts() {
        return RaceClassSynergy.boostsFor(race, characterClass);
    }

    /** True when this pairing is one of the six synergies named in the design document. */
    public boolean isNamedSynergy() {
        return RaceClassSynergy.isNamedSynergy(race, characterClass);
    }

    /** One-line boost summary, e.g. {@code "Synergy: Mana++, Wisdom+"}. */
    public String describeBoosts() {
        return RaceClassSynergy.describe(race, characterClass);
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Abilities
    // ──────────────────────────────────────────────────────────────────────

    /** This build's abilities in action-bar order; index 0 is hotkey {@code [1]}. */
    public List<AbilityType> abilities() {
        return AbilityType.forClass(characterClass);
    }

    /** This build's ultimate, or {@code null} if its class somehow has none. */
    public AbilityType ultimate() {
        return AbilityType.ultimateFor(characterClass);
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Identity
    // ──────────────────────────────────────────────────────────────────────

    /** Human-readable build name, e.g. {@code "Elf Mage"}. */
    public String displayName() {
        return race.displayName + " " + characterClass.displayName;
    }

    /**
     * Value equality, so a build can be used as a map key and two previews of the
     * same pairing compare equal.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof CharacterBuild)) return false;
        CharacterBuild that = (CharacterBuild) other;
        return race == that.race && characterClass == that.characterClass;
    }

    @Override
    public int hashCode() {
        return Objects.hash(race, characterClass);
    }

    @Override
    public String toString() {
        return displayName();
    }
}
