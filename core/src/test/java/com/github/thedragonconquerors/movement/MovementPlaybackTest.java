package com.github.thedragonconquerors.movement;

import com.badlogic.gdx.math.Vector2;
import com.github.thedragonconquerors.entities.Player;
import com.github.thedragonconquerors.assets.SpriteAssets;
import com.github.thedragonconquerors.animation.AnimationState;
import com.shared.shared.model.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MovementPlaybackTest {
    private Player player() { return new Player(0,"Hero",new Vector2(),CharacterBuild.of(Race.HUMAN,CharacterClass.MAGE),1); }
    @Test void movementPlaysThirtyPercentSlowerWithoutChangingServerResources() {
        var p=player();float maximum=p.getMovementController().getMaxMovementDistance();
        p.getMovementController().setAuthoritativePath(p.getPosition(),List.of(new Vector2(3.5f,0)),2.25f);
        var system=new MovementSystem();system.update(p,.5f);
        assertEquals(1.75f,p.getPosition().x,.001f);
        assertEquals(2.25f,p.getMovementController().getRemainingMovementDistance(),.001f);
        system.update(p,.5f);system.update(p,.02f);
        assertEquals(3.5f,p.getPosition().x,.001f);assertFalse(p.getMovementController().isMoving());
        assertEquals(maximum,p.getMovementController().getMaxMovementDistance(),.001f);
    }
    @Test void localPreviewStillChargesDistanceRatherThanElapsedTime() {
        var p=player();float remaining=p.getMovementController().getRemainingMovementDistance();
        p.getMovementController().setPath(List.of(new Vector2(2,0)));
        new MovementSystem().update(p,.2f);
        assertEquals(.7f,p.getPosition().x,.001f);
        assertEquals(remaining-.7f,p.getMovementController().getRemainingMovementDistance(),.001f);
    }
    @Test void walkingCadenceIsSlowerWithoutChangingCombatClipTiming() {
        for(var profile:SpriteAssets.values()) {
            assertEquals(.13f,profile.clip(AnimationState.WALK).frameDuration(),.0001f);
            assertEquals(.085f,profile.clip(AnimationState.ATTACK).frameDuration(),.0001f);
        }
    }
}
