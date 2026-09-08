// File Location: shared/src/main/java/com/shared/shared/model/stats/RaceClassSynergy.java
package com.shared.shared.model.stats;

import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.Race;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Race + class stat boosts, and the balance rule that keeps them fair.
 *
 * <h2>The balance guarantee</h2>
 *
 * <p>Every race/class pair receives <b>exactly {@value #TOTAL_BUDGET} boost
 * steps</b>: {@value #BASELINE_BUDGET} from the race's baseline trait plus
 * {@value #PAIR_BUDGET} from the pairing itself. Race choice therefore changes
 * <em>what</em> a build is good at, never <em>how strong</em> it is. This is
 * enforced, not merely documented — {@link #validateBudgets()} runs in a static
 * initialiser and fails fast at class-load time if any entry drifts off budget,
 * so a mis-tuned table cannot reach a live match.</p>
 *
 * <h2>Synergy vs affinity</h2>
 *
 * <p>The design document names six synergies. Those are implemented verbatim in
 * {@link #SYNERGIES} and are deliberately <em>spiky</em>: they stack two steps
 * into one stat ({@code Mana++}) to create a specialist. Every unnamed pairing
 * instead receives a flat three-step {@link #AFFINITIES} spread for its race —
 * same budget, broader shape. That keeps all 24 combinations playable without
 * making the six named ones any less distinctive.</p>
 *
 * <h2>Adding or retuning a synergy</h2>
 *
 * <ol>
 *   <li>Add or edit an entry in {@link #SYNERGIES}.</li>
 *   <li>Keep its total at {@value #PAIR_BUDGET} steps, or the static
 *       validation will throw on startup and tell you which entry is wrong.</li>
 *   <li>Avoid the race's {@link Race#getBaselineStat()} — stacking a baseline
 *       and a synergy on the same stat is how overpowered outliers appear.</li>
 * </ol>
 */
public final class RaceClassSynergy {

    /** Boost steps granted by the race's baseline trait, for every class. */
    public static final int BASELINE_BUDGET = 1;

    /** Boost steps granted by the race/class pairing itself. */
    public static final int PAIR_BUDGET = 3;

    /** Total steps any build receives from its race. */
    public static final int TOTAL_BUDGET = BASELINE_BUDGET + PAIR_BUDGET;

    /**
     * The six synergies named in the design document, transcribed literally.
     *
     * <pre>
     *   Paladin + Human       -> Str+,  Hp+,  Acc+
     *   Elf     + Mage        -> Mana++,       Wisdom+
     *   Dragonborne + Paladin -> Str++,        Acc+
     *   Dragonborne + Mage    -> Wisdom++,     Mana+
     *   Undead  + Wraith      -> Speed+, Wisdom+, Hp+
     *   Elf     + Cleric/Bard -> Inspiration++, Wisdom+
     * </pre>
     */
    private static final Map<Race, Map<CharacterClass, List<StatBoost>>> SYNERGIES =
        new EnumMap<>(Race.class);

    /**
     * Generic three-step spread used for any pairing not named above.
     *
     * <p>Each race's affinity avoids that race's baseline stat, so no stat ever
     * receives both a baseline and an affinity step.</p>
     */
    private static final Map<Race, List<StatBoost>> AFFINITIES = new EnumMap<>(Race.class);

    static {
        // ── Named synergies ───────────────────────────────────────────────
        synergy(Race.HUMAN, CharacterClass.PALADIN,
            StatBoost.plus(StatType.STRENGTH),
            StatBoost.plus(StatType.HP),
            StatBoost.plus(StatType.ACCURACY));

        synergy(Race.ELF, CharacterClass.MAGE,
            StatBoost.plusPlus(StatType.MANA),
            StatBoost.plus(StatType.WISDOM));

        synergy(Race.DRAGONBORNE, CharacterClass.PALADIN,
            StatBoost.plusPlus(StatType.STRENGTH),
            StatBoost.plus(StatType.ACCURACY));

        synergy(Race.DRAGONBORNE, CharacterClass.MAGE,
            StatBoost.plusPlus(StatType.WISDOM),
            StatBoost.plus(StatType.MANA));

        synergy(Race.UNDEAD, CharacterClass.WRAITH,
            StatBoost.plus(StatType.SPEED),
            StatBoost.plus(StatType.WISDOM),
            StatBoost.plus(StatType.HP));

        // "Elf + Cleric/Bard" is one line in the document, two entries here.
        synergy(Race.ELF, CharacterClass.CLERIC,
            StatBoost.plusPlus(StatType.INSPIRATION),
            StatBoost.plus(StatType.WISDOM));

        synergy(Race.ELF, CharacterClass.BARD,
            StatBoost.plusPlus(StatType.INSPIRATION),
            StatBoost.plus(StatType.WISDOM));

        // ── Generic affinities (every unnamed pairing) ─────────────────────
        // Human baseline is INSPIRATION, so the spread avoids it.
        affinity(Race.HUMAN,
            StatBoost.plus(StatType.HP),
            StatBoost.plus(StatType.WISDOM),
            StatBoost.plus(StatType.SPEED));

        // Elf baseline is ACCURACY.
        affinity(Race.ELF,
            StatBoost.plus(StatType.MANA),
            StatBoost.plus(StatType.WISDOM),
            StatBoost.plus(StatType.SPEED));

        // Dragonborne baseline is HP.
        affinity(Race.DRAGONBORNE,
            StatBoost.plus(StatType.STRENGTH),
            StatBoost.plus(StatType.ACCURACY),
            StatBoost.plus(StatType.MANA));

        // Undead baseline is STRENGTH.
        affinity(Race.UNDEAD,
            StatBoost.plus(StatType.HP),
            StatBoost.plus(StatType.SPEED),
            StatBoost.plus(StatType.WISDOM));

        // Fail fast rather than shipping a silently unbalanced table.
        validateBudgets();
    }

    private RaceClassSynergy() {
        // Static utility holder — never instantiated.
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Public API
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Every boost a build receives from its race: baseline trait first, then
     * the pairing's synergy or affinity.
     *
     * <p>The returned list is unmodifiable and ordered for display, so the
     * race-selection screen can render it directly.</p>
     */
    public static List<StatBoost> boostsFor(Race race, CharacterClass characterClass) {
        if (race == null || characterClass == null) return Collections.emptyList();

        List<StatBoost> boosts = new ArrayList<>();
        boosts.add(StatBoost.plus(race.getBaselineStat()));
        boosts.addAll(pairBoosts(race, characterClass));
        return Collections.unmodifiableList(boosts);
    }

    /**
     * Applies {@link #boostsFor} to {@code stats} in place.
     *
     * @return the same instance, for call chaining
     */
    public static StatComponent apply(StatComponent stats, Race race,
                                      CharacterClass characterClass) {
        if (stats == null) return null;
        for (StatBoost boost : boostsFor(race, characterClass)) {
            boost.applyTo(stats);
        }
        return stats;
    }

    /** True when this pairing is one of the six synergies named in the design document. */
    public static boolean isNamedSynergy(Race race, CharacterClass characterClass) {
        Map<CharacterClass, List<StatBoost>> byClass = SYNERGIES.get(race);
        return byClass != null && byClass.containsKey(characterClass);
    }

    /**
     * Short label for the race-selection screen, e.g.
     * {@code "Synergy: Strength+, HP+, Accuracy+"}.
     */
    public static String describe(Race race, CharacterClass characterClass) {
        List<StatBoost> boosts = boostsFor(race, characterClass);
        if (boosts.isEmpty()) return "No racial bonus";

        StringBuilder builder = new StringBuilder(
            isNamedSynergy(race, characterClass) ? "Synergy: " : "Affinity: ");
        for (int i = 0; i < boosts.size(); i++) {
            if (i > 0) builder.append(", ");
            builder.append(boosts.get(i));
        }
        return builder.toString();
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Internals
    // ──────────────────────────────────────────────────────────────────────

    /** The pairing half of the budget: named synergy if present, else the race affinity. */
    private static List<StatBoost> pairBoosts(Race race, CharacterClass characterClass) {
        Map<CharacterClass, List<StatBoost>> byClass = SYNERGIES.get(race);
        if (byClass != null) {
            List<StatBoost> synergy = byClass.get(characterClass);
            if (synergy != null) return synergy;
        }
        return AFFINITIES.getOrDefault(race, Collections.emptyList());
    }

    private static void synergy(Race race, CharacterClass characterClass, StatBoost... boosts) {
        SYNERGIES.computeIfAbsent(race, key -> new EnumMap<>(CharacterClass.class))
            .put(characterClass, List.of(boosts));
    }

    private static void affinity(Race race, StatBoost... boosts) {
        AFFINITIES.put(race, List.of(boosts));
    }

    /**
     * Verifies the balance guarantee for all {@code Race.values().length *
     * CharacterClass.values().length} combinations.
     *
     * <p>Runs once at class load. Throws {@link IllegalStateException} — which
     * surfaces as an {@code ExceptionInInitializerError} — naming the offending
     * pairing and its actual budget, so a bad merge is obvious immediately
     * instead of becoming a balance bug someone notices three matches later.</p>
     */
    static void validateBudgets() {
        for (Race race : Race.values()) {
            if (!AFFINITIES.containsKey(race)) {
                throw new IllegalStateException(
                    "Race " + race + " has no generic affinity spread defined");
            }

            for (CharacterClass characterClass : CharacterClass.values()) {
                List<StatBoost> pair = pairBoosts(race, characterClass);
                int points = totalPoints(pair);
                if (points != PAIR_BUDGET) {
                    throw new IllegalStateException(
                        "Unbalanced boosts for " + race + " + " + characterClass
                            + ": expected " + PAIR_BUDGET + " steps but found "
                            + points + " " + pair);
                }

                // A baseline landing on the same stat as a pair boost would
                // double up and create an outlier value.
                for (StatBoost boost : pair) {
                    if (boost.getStat() == race.getBaselineStat()) {
                        throw new IllegalStateException(
                            "Boost overlap for " + race + " + " + characterClass
                                + ": " + boost.getStat()
                                + " is both the racial baseline and a pair boost");
                    }
                }
            }
        }
    }

    private static int totalPoints(List<StatBoost> boosts) {
        int total = 0;
        for (StatBoost boost : boosts) {
            total += boost.points();
        }
        return total;
    }
}
