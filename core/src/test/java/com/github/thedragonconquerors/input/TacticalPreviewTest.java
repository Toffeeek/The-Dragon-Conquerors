package com.github.thedragonconquerors.input;

import com.badlogic.gdx.math.Vector2;
import com.github.thedragonconquerors.entities.Player;
import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.ability.AbilityType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TacticalPreviewTest {
    @Test void spriteBodyAndFeetAreClickableWithoutEnlargingSideBounds() {
        assertTrue(TacticalPreview.hitsBody(new Vector2(5,5), new Vector2(5,6.1f)));
        assertTrue(TacticalPreview.hitsBody(new Vector2(5,5), new Vector2(5.65f,5)));
        assertFalse(TacticalPreview.hitsBody(new Vector2(5,5), new Vector2(5.7f,6)));
        assertFalse(TacticalPreview.hitsBody(new Vector2(5,5), new Vector2(5,6.3f)));
    }
    @Test void previewDoesNotSpendManaOrHealthAndExplainsCurse() {
        Player actor = new Player(1,"Caster",new Vector2(),CharacterClass.WRAITH);
        Player target = new Player(2,"Target",new Vector2(2,0),CharacterClass.PALADIN);
        int mana = actor.getStats().getMana(), hp = target.getStats().getHp();
        assertTrue(TacticalPreview.describe(actor,AbilityType.CURSE,target).contains("strike the caster"));
        assertEquals(mana, actor.getStats().getMana()); assertEquals(hp,target.getStats().getHp());
    }
}
