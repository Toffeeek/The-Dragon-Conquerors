package com.github.thedragonconquerors.input;
import com.github.thedragonconquerors.ui.PixelIcons;
import com.shared.shared.model.ability.AbilityType;
import com.shared.shared.model.effect.StatusEffectType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PresentationMappingTest {
    @Test void everyAbilityAndStatusHasAnIconAndStatusesHaveDistinctShapes() {
        for(var ability:AbilityType.values())assertNotNull(PixelIcons.kind(ability));
        var kinds=new java.util.HashSet<PixelIcons.Kind>();
        for(var effect:StatusEffectType.values())assertTrue(kinds.add(PixelIcons.kind(effect)));
        assertEquals(PixelIcons.Kind.PORTAL,PixelIcons.kind(AbilityType.TELEPORT));
        assertEquals(PixelIcons.Kind.STAR,PixelIcons.kind(AbilityType.ELDRITCH_BLAST));
    }
}
