// File Location: shared/src/test/java/com/shared/shared/model/CharacterBuildTest.java
package com.shared.shared.model;

import com.shared.shared.model.stats.RaceClassSynergy;
import com.shared.shared.model.stats.StatComponent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterBuildTest {
    @Test
    void elfMagePreviewAppliesNamedBalancedSynergy() {
        CharacterBuild build = CharacterBuild.of(Race.ELF, CharacterClass.MAGE);
        StatComponent base = build.createBaseStats();
        StatComponent boosted = build.createStats();

        assertTrue(build.isNamedSynergy());
        assertEquals(RaceClassSynergy.TOTAL_BUDGET,
            build.appliedBoosts().stream().mapToInt(boost -> boost.points()).sum());
        assertTrue(boosted.getMaxMana() > base.getMaxMana());
        assertTrue(boosted.getWisdom() > base.getWisdom());
        assertTrue(boosted.getAccuracy() > base.getAccuracy());
        assertNotSame(base, boosted);
    }

    @Test
    void everyRaceClassPairUsesTheSameBoostBudget() {
        for (Race race : Race.values()) {
            for (CharacterClass characterClass : CharacterClass.values()) {
                CharacterBuild build = CharacterBuild.of(race, characterClass);
                assertEquals(RaceClassSynergy.TOTAL_BUDGET,
                    build.appliedBoosts().stream().mapToInt(boost -> boost.points()).sum(),
                    build.displayName());
            }
        }
    }
}
