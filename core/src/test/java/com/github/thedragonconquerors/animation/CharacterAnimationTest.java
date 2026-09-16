package com.github.thedragonconquerors.animation;

import com.badlogic.gdx.math.Vector2;
import com.github.thedragonconquerors.assets.SpriteAssets;
import com.github.thedragonconquerors.assets.EffectAssets;
import com.github.thedragonconquerors.movement.MovementController;
import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.ability.AbilityType;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.io.File;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CharacterAnimationTest {
    private final Vector2 origin = new Vector2(5,5);
    private final MovementController movement = new MovementController(6);

    @Test void everyProfileUsesTheRequestedCharacterAndOriginalShadowVariant() throws Exception {
        String[] expected = {"Knight", "Necromancer", "Archer", "Priest", "Wizard", "Orc"};
        int index = 0;
        File assets = new File(System.getProperty("game.assets"));
        Map<String,String> sourceHashes = Files.readAllLines(new File(assets, "characters/animated/SOURCES.sha256").toPath())
            .stream().map(line -> line.split(" ",2)).collect(Collectors.toMap(parts -> parts[1], parts -> parts[0]));
        for (SpriteAssets profile : SpriteAssets.values()) {
            assertEquals(expected[index++], profile.character);
            assertEquals(profile != SpriteAssets.WRAITH, profile.hasShadow);
            for (SpriteAssets.Clip clip : profile.clips()) {
                File packed = new File(assets, clip.path());
                assertEquals(sourceHashes.get(clip.path()), HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(packed.toPath()))), clip.path());
                var image = ImageIO.read(packed);
                assertEquals(clip.frames() * 100, image.getWidth(), clip.path());
                assertEquals(100, image.getHeight());
            }
        }
        for (EffectAssets effect : EffectAssets.values()) {
            var image = ImageIO.read(new File(assets, effect.clip.path()));
            assertEquals(effect.clip.frames() * 100, image.getWidth());
            assertEquals(100, image.getHeight());
        }
    }

    @Test void everyAbilityPlaysItsWholeStripThenReturnsToIdle() {
        for (CharacterClass type : CharacterClass.values()) {
            for (AbilityType ability : AbilityType.forClass(type)) {
                PlayerAnimationController animation = new PlayerAnimationController(type);
                animation.playAbility(origin, new Vector2(7,5), ability);
                SpriteAssets.Clip clip = animation.getClip();
                for (int frame = 0; frame < clip.frames(); frame++) {
                    if (frame > 0) animation.update(clip.frameDuration(), origin, movement);
                    // Sample midway through each frame to avoid floating-point boundary ambiguity.
                    if (frame == 0) animation.update(clip.frameDuration() / 2, origin, movement);
                    assertEquals(frame, animation.getCurrentFrame(), type + " " + ability);
                    assertTrue(animation.isBusy());
                }
                animation.update(clip.frameDuration(), origin, movement);
                assertEquals(AnimationState.IDLE, animation.getState());
                assertFalse(animation.isBusy());
            }
        }
    }

    @Test void walkLoopsAndVerticalMotionKeepsHorizontalFacing() {
        for (CharacterClass type : CharacterClass.values()) {
            PlayerAnimationController animation = new PlayerAnimationController(type);
            movement.setPath(List.of(new Vector2(2,5)));
            animation.update(.1f, origin, movement);
            assertEquals(AnimationState.WALK, animation.getState());
            assertTrue(animation.isFacingLeft());
            for (int tick = 0; tick < 200; tick++) {
                animation.update(.1f, origin, movement);
                assertTrue(animation.getCurrentFrame() < animation.getClip().frames());
            }
            movement.setPath(List.of(new Vector2(5,8)));
            animation.update(.1f, origin, movement);
            assertTrue(animation.isFacingLeft());
            movement.setPath(List.of(new Vector2(8,5)));
            animation.update(.1f, origin, movement);
            assertFalse(animation.isFacingLeft());
            movement.stopMoving();
            animation.update(.1f, origin, movement);
            assertEquals(AnimationState.IDLE, animation.getState());
        }
    }

    @Test void deathHoldsFinalFrameAndReviveRestoresAllAnimations() {
        for (CharacterClass type : CharacterClass.values()) {
            PlayerAnimationController animation = new PlayerAnimationController(type);
            animation.playDeath();
            int last = animation.getClip().frames() - 1;
            animation.update(10, origin, movement);
            assertEquals(last, animation.getCurrentFrame());
            animation.playDeath();
            assertEquals(last, animation.getCurrentFrame(), "Do not restart death on another snapshot");
            animation.playAttack(origin, origin, false);
            assertEquals(AnimationState.DEATH, animation.getState());
            animation.revive();
            assertEquals(AnimationState.IDLE, animation.getState());
            animation.playAbility(origin, origin, AbilityType.forClass(type).get(0));
            assertTrue(animation.isBusy());
        }
    }

    @Test void reactionWaitsForImpactAndDoesNotTruncateTheDeathStrip() {
        PlayerAnimationController animation = new PlayerAnimationController(CharacterClass.WRAITH);
        animation.queueReaction(true, .5f);
        animation.update(.3f, origin, movement);
        assertEquals(AnimationState.IDLE, animation.getState());
        assertTrue(animation.isBusy());
        animation.update(.21f, origin, movement);
        assertEquals(AnimationState.DEATH, animation.getState());
        assertEquals(0, animation.getCurrentFrame());
        animation.update(1.1f, origin, movement);
        assertEquals(8, animation.getCurrentFrame());
        assertFalse(animation.isBusy());
    }

    @Test void teleportAndEldritchBlastRetainTheirRulesAndHaveDistinctCasts() {
        assertTrue(AbilityType.forClass(CharacterClass.WRAITH).contains(AbilityType.TELEPORT));
        assertTrue(AbilityType.forClass(CharacterClass.MAGE).contains(AbilityType.ELDRITCH_BLAST));
        assertEquals(AnimationState.SPECIAL, SpriteAssets.stateFor(AbilityType.TELEPORT));
        assertEquals(AnimationState.SPECIAL, SpriteAssets.stateFor(AbilityType.ELDRITCH_BLAST));
        assertEquals(20, AbilityType.TELEPORT.getManaCost());
        assertEquals(30, AbilityType.ELDRITCH_BLAST.getManaCost());
    }
}
