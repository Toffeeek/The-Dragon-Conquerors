package com.github.thedragonconquerors.input;

import com.badlogic.gdx.math.Vector2;
import com.github.thedragonconquerors.entities.Player;
import com.github.thedragonconquerors.rendering.PlayerRenderer;
import com.shared.shared.model.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PlayerPresentationTest {
    private Player player(int id,int team,float x) {
        return new Player(id,"P"+id,new Vector2(x,5),CharacterBuild.of(Race.HUMAN,CharacterClass.MAGE),team);
    }
    @Test void affiliationIsRelativeToBothTeamPerspectives() {
        assertTrue(PlayerRenderer.isAlly(1,1));assertFalse(PlayerRenderer.isAlly(1,2));
        assertTrue(PlayerRenderer.isAlly(2,2));assertFalse(PlayerRenderer.isAlly(2,1));
    }
    @Test void hoverWorksForSelfAllyAndOpponentWithoutTurnOrActionSelection() {
        Player local=player(0,2,2), ally=player(1,2,4), enemy=player(2,1,6);
        var others=List.of(ally,enemy);
        assertFalse(local.isActiveTurn());
        assertSame(local,PlayerRenderer.hoveredPlayer(local,others,new Vector2(2,5.7f)));
        assertSame(ally,PlayerRenderer.hoveredPlayer(local,others,new Vector2(4,5.7f)));
        assertSame(enemy,PlayerRenderer.hoveredPlayer(local,others,new Vector2(6,5.7f)));
        assertNull(PlayerRenderer.hoveredPlayer(local,others,null));
        assertNull(PlayerRenderer.hoveredPlayer(local,others,new Vector2(2,4.5f)));
        assertNull(PlayerRenderer.hoveredPlayer(local,others,new Vector2(2.6f,5.7f)));
    }
    @Test void overlappingBodiesChooseOnlyTheClosestSprite() {
        Player local=player(0,1,2), other=player(1,2,2.5f);
        assertSame(other,PlayerRenderer.hoveredPlayer(local,List.of(other),new Vector2(2.4f,5.5f)));
        assertSame(local,PlayerRenderer.hoveredPlayer(local,List.of(other),new Vector2(2.1f,5.5f)));
    }
}
