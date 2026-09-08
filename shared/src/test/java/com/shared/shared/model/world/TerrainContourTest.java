package com.shared.shared.model.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.XmlReader;
import org.junit.jupiter.api.Test;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.zip.InflaterInputStream;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class TerrainContourTest {
    @Test void allEightTiledOrientationsPreserveTheCollisionOutline() {
        int[] flags = {0, 0x80000000, 0x40000000, 0xc0000000, 0x20000000, 0xa0000000, 0x60000000, 0xe0000000};
        int[] expected = {82, 93, 162, 173, 37, 213, 42, 218};
        for (int index = 0; index < flags.length; index++) {
            assertEquals(expected[index], TiledTerrain.sourcePixel(2, 5, flags[index]));
        }
    }

    @Test void collisionFollowsArtworkPixelsIncludingTheRotatedPond() throws Exception {
        BufferedImage map = artwork();
        TiledTerrain terrain = new TiledTerrain("canyon");
        Set<Integer> grass = Set.of(0x3e8948, 0x265c42, 0x3d6c43);
        for (int y = 0; y < map.getHeight(); y++) for (int x = 0; x < map.getWidth(); x++) {
            int color = map.getRGB(x, y);
            boolean expected = (color >>> 24) == 255 && grass.contains(color & 0xffffff);
            assertEquals(expected, terrain.isWalkable((x + 0.5f) / 16f, (271.5f - y) / 16f, 0f),
                "Collision differs from artwork at pixel " + x + "," + y);
        }
        writeContourPreview(map, terrain);
    }

    @Test void grassInsideMixedShoreTilesIsReachableWithoutPermittingWater() {
        BattlefieldDefinition field = BattlefieldDefinition.forEnvironment(Environment.CANYON);
        BattlefieldNavigation nav = new BattlefieldNavigation(field);
        int restoredGround = 0;
        Vector2 nearPond = null;
        // These two tiles in the screenshot contain both ground and rotated shoreline.
        for (float x = 3.0625f; x < 5f; x += 0.125f) for (float y = 8.0625f; y < 9f; y += 0.125f) {
            Vector2 point = new Vector2(x, y);
            if (field.isWalkable(point)) {
                restoredGround++;
                assertFalse(field.isLethalFall(point), "Grass in a shore tile is not a lethal fall");
                if (nearPond == null) nearPond = point;
            }
        }
        assertTrue(restoredGround > 20, "Mixed shoreline tiles must not be blocked as whole squares");
        List<Vector2> path = nav.findPath(new Vector2(2f, 5f), nearPond, 20f, List.of());
        assertFalse(path.isEmpty());
        assertEquals(nearPond, path.get(path.size() - 1));
        Vector2 previous = new Vector2(2f, 5f);
        for (Vector2 point : path) { assertTrue(field.pathIsWalkable(previous, point)); previous = point; }
        assertFalse(field.isWalkable(new Vector2(4.5f, 7.5f)), "The pond itself remains solid");
    }

    @Test void sweptFootprintDoesNotCutThroughThinCliffPixels() {
        BattlefieldDefinition field = BattlefieldDefinition.forEnvironment(Environment.CANYON);
        assertFalse(field.pathIsWalkable(new Vector2(14.5f, 10.5f), new Vector2(16.5f, 10.5f)));
        assertFalse(field.pathIsWalkable(new Vector2(2f, 8.5f), new Vector2(6f, 8.5f)));
    }

    private BufferedImage artwork() throws Exception {
        File assets = new File(System.getProperty("terrain.assets"));
        BufferedImage sheet = ImageIO.read(new File(assets, "tileset.png"));
        XmlReader.Element xml;
        try (InputStream input = new FileInputStream(new File(assets, "canyon.tmx"))) { xml = new XmlReader().parse(input); }
        byte[] packed = Base64.getMimeDecoder().decode(xml.getChildByName("layer").getChildByName("data").getText().trim());
        ByteBuffer gids;
        try (InputStream input = new InflaterInputStream(new ByteArrayInputStream(packed))) {
            gids = ByteBuffer.wrap(input.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN);
        }
        BufferedImage map = new BufferedImage(480, 272, BufferedImage.TYPE_INT_ARGB);
        for (int row = 0; row < 17; row++) for (int col = 0; col < 30; col++) {
            int raw = gids.getInt(), id = (raw & 0x0fffffff) - 1;
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
                int source = TiledTerrain.sourcePixel(x, y, raw);
                map.setRGB(col * 16 + x, row * 16 + y, sheet.getRGB(id % 12 * 16 + source % 16, id / 12 * 16 + source / 16));
            }
        }
        return map;
    }

    private void writeContourPreview(BufferedImage map, TiledTerrain terrain) throws Exception {
        // Inspect the pond from the reported screenshot at nearest-neighbor scale.
        BufferedImage preview = new BufferedImage(976, 448, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = preview.createGraphics();
        graphics.setColor(new Color(0x151c22));
        graphics.fillRect(0, 0, preview.getWidth(), preview.getHeight());
        graphics.setColor(Color.WHITE);
        graphics.drawString("Terrain artwork", 12, 20);
        graphics.drawString("Collision: red = blocked player-foot position", 504, 20);
        for (int y = 0; y < 80; y++) for (int x = 0; x < 96; x++) {
            int sourceX = 16 + x, sourceY = 112 + y;
            Color original = new Color(map.getRGB(sourceX, sourceY));
            graphics.setColor(original);
            graphics.fillRect(8 + x * 5, 36 + y * 5, 5, 5);
            boolean walkable = terrain.isWalkable((sourceX + 0.5f) / 16f, (271.5f - sourceY) / 16f, BattlefieldDefinition.PLAYER_RADIUS);
            graphics.setColor(walkable ? original : new Color((original.getRed() + 255) / 2, original.getGreen() / 2, original.getBlue() / 2));
            graphics.fillRect(496 + x * 5, 36 + y * 5, 5, 5);
        }
        graphics.dispose();
        File output = new File("build/reports/terrain-contours.png");
        output.getParentFile().mkdirs();
        ImageIO.write(preview, "png", output);
    }
}
