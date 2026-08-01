package com.github.thedragonconquerors.entities;

import com.github.thedragonconquerors.stats.StatComponent;

public enum CharacterClass {
    WARRIOR("Warrior", new StatComponent(120, 35, 10, 14, 9, 8, 8)),
    MAGE("Mage", new StatComponent(80, 100, 12, 6, 9, 11, 16)),
    ARCHER("Archer", new StatComponent(90, 60, 15, 10, 13, 10, 10)),
    PALADIN("Paladin", new StatComponent(110, 70, 10, 12, 8, 13, 12)),
    ROGUE("Rogue", new StatComponent(85, 55, 14, 11, 16, 10, 9));

    public final String displayName;
    private final StatComponent baseStats;

    CharacterClass(String displayName, StatComponent baseStats) {
        this.displayName = displayName;
        this.baseStats = baseStats;
    }

    public StatComponent createStats() {
        return new StatComponent(
            baseStats.getMaxHp(),
            baseStats.getMaxMana(),
            baseStats.getAccuracy(),
            baseStats.getStrength(),
            baseStats.getSpeed(),
            baseStats.getInspiration(),
            baseStats.getWisdom()
        );
    }
}
