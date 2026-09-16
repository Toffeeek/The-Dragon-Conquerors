package com.github.thedragonconquerors.lwjgl3;

import com.github.thedragonconquerors.assets.EffectAssets;
import com.github.thedragonconquerors.assets.SpriteAssets;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

/** Checks the distributable itself: source-directory fallbacks cannot hide missing assets. */
class GamePackageTest {
    @Test void installedMapThreeMatchesTheSuppliedSvgImage() throws Exception {
        var factory=javax.xml.parsers.DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        var root=factory.newDocumentBuilder().parse(Path.of(System.getProperty("game.assets"),"maps-new/map3.svg").toFile());
        var image=(org.w3c.dom.Element)root.getElementsByTagNameNS("http://www.w3.org/2000/svg","image").item(0);
        String data=image.getAttributeNS("http://www.w3.org/1999/xlink","href");
        assertTrue(data.startsWith("data:image/png;base64,"));
        assertArrayEquals(java.util.Base64.getDecoder().decode(data.substring(data.indexOf(',')+1)),
            Files.readAllBytes(Path.of(System.getProperty("game.assets"),"maps-new/map3.png")));
    }
    private ZipFile gameJar() throws Exception {
        return new ZipFile(System.getProperty("game.jar"));
    }

    private void assertOriginalAsset(ZipFile jar, String path) throws Exception {
        var entry = jar.getEntry(path);
        assertNotNull(entry, "Missing packaged asset: " + path);
        try (var input = jar.getInputStream(entry)) {
            assertArrayEquals(Files.readAllBytes(Path.of(System.getProperty("game.assets"), path)),
                input.readAllBytes(), path);
        }
    }

    @Test void allSixCharacterProfilesAndEffectsArePackagedUnchanged() throws Exception {
        try (var jar = gameJar()) {
            for (var profile : SpriteAssets.values()) {
                for (var clip : profile.clips()) assertOriginalAsset(jar, clip.path());
            }
            for (var effect : EffectAssets.values()) assertOriginalAsset(jar, effect.clip.path());
        }
    }

    @Test void allThreeMapsAndCollisionResourcesRemainAvailable() throws Exception {
        try (var jar = gameJar()) {
            assertNotNull(jar.getEntry("server/tdc-server.jar"), "Standalone hosting needs the bundled server");
            assertOriginalAsset(jar, "fonts/Inter-Regular.otf");
            assertOriginalAsset(jar, "fonts/Cinzel.ttf");
            assertNotNull(jar.getEntry("fonts/Cinzel-OFL.txt"));
            assertNotNull(jar.getEntry("fonts/OFL.txt"));
            for (String map : List.of("map1", "map2", "map3")) {
                assertOriginalAsset(jar, "maps-new/" + map + ".png");
                var mask = jar.getEntry("battlefields/" + map + "-mask.bin");
                assertNotNull(mask, map);
                assertEquals(8 + 480 * 272, mask.getSize(), map);
            }
            for (String resource : List.of("canyon.tmx", "lava.tmx", "bog.tmx", "tileset.tsx", "terrain-mask.bin")) {
                assertNotNull(jar.getEntry("battlefields/" + resource), resource);
            }
            for (String resource : List.of("canyon.tmx", "lava.tmx", "bog.tmx", "tileset.tsx", "tileset.png")) {
                assertOriginalAsset(jar, "maps-new/" + resource);
            }
        }
    }

    @Test void unusedAuthoringArtAndLegacyCodeAreNotShipped() throws Exception {
        try (var jar = gameJar()) {
            for (String removed : List.of("assets.txt", "maps-new/map1.svg", "maps-new/map2.svg",
                "maps-new/map3.svg", "maps-new/map1-collision.xml", "characters/Paladin_HD.png",
                "characters/animated/Knight/Knight_Block.png",
                "characters/animated/Wizard/Wizard_Attack01_Effect.png",
                "characters/animated/Wizard/Wizard_Attack01(With magic effects).png",
                "com/github/thedragonconquerors/combat/ActionSystem.class",
                "com/github/thedragonconquerors/core/TurnManager.class",
                "com/github/thedragonconquerors/entities/PlayerConverter.class",
                "com/shared/shared/model/PlayerState.class",
                "com/badlogic/gdx/physics/box2d/World.class",
                "com/badlogic/ashley/core/Engine.class", "com/badlogic/gdx/ai/GdxAI.class")) {
                assertNull(jar.getEntry(removed), "Unneeded resource still packaged: " + removed);
            }
        }
    }
}
