package com.shared.shared.model;

import com.shared.shared.model.stats.StatCalculator;
import com.shared.shared.model.stats.StatType;
import com.shared.shared.model.ability.AbilityType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClassBalanceTest {
    @Test void allClassesShareABudgetWithoutLosingTheirRoles() {
        for (CharacterClass type : CharacterClass.values()) assertEquals(48, type.totalTiers(), type.name());
        assertTrue(CharacterClass.PALADIN.getTier(StatType.HP) > CharacterClass.MAGE.getTier(StatType.HP));
        assertTrue(CharacterClass.WRAITH.getTier(StatType.SPEED) > CharacterClass.PALADIN.getTier(StatType.SPEED));
        assertTrue(CharacterClass.ARCHER.getTier(StatType.ACCURACY) > CharacterClass.CLERIC.getTier(StatType.ACCURACY));
        assertTrue(AbilityType.forClass(CharacterClass.WRAITH).contains(AbilityType.TELEPORT));
        assertTrue(AbilityType.forClass(CharacterClass.MAGE).contains(AbilityType.ELDRITCH_BLAST));
    }

    @Test void all576BuildMatchupsHaveReliableButNotGuaranteedHits() {
        for (Race race : Race.values()) for (CharacterClass type : CharacterClass.values()) {
            var attacker = CharacterBuild.of(race, type).createStats();
            assertTrue(StatCalculator.deriveMaxMovementDistance(attacker) >= 5.4f);
            assertTrue(StatCalculator.deriveMaxMovementDistance(attacker) <= 7.5f);
            for (Race otherRace : Race.values()) for (CharacterClass otherType : CharacterClass.values()) {
                var defender = CharacterBuild.of(otherRace, otherType).createStats();
                int chance = StatCalculator.hitChanceAgainst(attacker, defender);
                assertTrue(chance >= 58 && chance <= 95, type + " vs " + otherType + ": " + chance);
            }
            for (AbilityType ability : AbilityType.forClass(type)) {
                assertTrue(ability.getManaCost() <= attacker.getMaxMana(), type + ": " + ability);
            }
        }
    }
}
