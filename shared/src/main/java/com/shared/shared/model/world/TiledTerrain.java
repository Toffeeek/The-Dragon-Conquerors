package com.shared.shared.model.world;

import com.badlogic.gdx.utils.XmlReader;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Intersector;
import java.util.zip.InflaterInputStream;

/** Rendering-free reader for the packaged Tiled terrain. Unknown tiles fail closed. */
final class TiledTerrain {
    static final int TILE_PIXELS = 16;
    private static final float PIXEL_SIZE = 1f / TILE_PIXELS;
    private final String[][] terrain;
    private final boolean[][] ground;
    private final List<List<Rectangle>> blockedRows;

    TiledTerrain(String mapName) {
        try {
            XmlReader.Element map = read(mapName + ".tmx");
            int width = map.getIntAttribute("width"), height = map.getIntAttribute("height");
            if (width != 30 || height != 17 || map.getIntAttribute("tilewidth") != 16
                || map.getIntAttribute("tileheight") != 16) {
                throw new IllegalStateException("Unexpected battlefield dimensions: " + mapName);
            }
            terrain = new String[width][height];
            ground = new boolean[width * TILE_PIXELS][height * TILE_PIXELS];
            blockedRows = new ArrayList<>(height * TILE_PIXELS);
            boolean[][] mask;
            try (DataInputStream input = new DataInputStream(TiledTerrain.class.getResourceAsStream("/battlefields/terrain-mask.bin"))) {
                int maskWidth = input.readInt(), maskHeight = input.readInt();
                if (maskWidth != 192 || maskHeight != 256) throw new IllegalStateException("Unexpected tileset mask size");
                mask = new boolean[maskWidth][maskHeight];
                for (int y = 0; y < maskHeight; y++) for (int x = 0; x < maskWidth; x++) mask[x][y] = input.readBoolean();
            }
            XmlReader.Element tileset = map.getChildByName("tileset");
            int firstGid = tileset.getIntAttribute("firstgid");
            Map<Integer, String> types = new HashMap<>();
            for (XmlReader.Element tile : read(tileset.getAttribute("source")).getChildrenByName("tile")) {
                XmlReader.Element properties = tile.getChildByName("properties");
                if (properties == null) continue;
                for (XmlReader.Element property : properties.getChildrenByName("property")) {
                    if ("terrain".equals(property.getAttribute("name"))) {
                        types.put(tile.getIntAttribute("id"), property.getAttribute("value"));
                    }
                }
            }
            XmlReader.Element data = map.getChildByName("layer").getChildByName("data");
            if (!"base64".equals(data.getAttribute("encoding"))
                || !"zlib".equals(data.getAttribute("compression"))) {
                throw new IllegalStateException("Expected base64/zlib tile data");
            }
            byte[] packed = Base64.getMimeDecoder().decode(data.getText().trim());
            try (InputStream inflater = new InflaterInputStream(new ByteArrayInputStream(packed))) {
                ByteBuffer bytes = ByteBuffer.wrap(inflater.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN);
                if (bytes.remaining() != width * height * 4) throw new IllegalStateException("Invalid tile count");
                for (int row = 0; row < height; row++) {
                    for (int x = 0; x < width; x++) {
                        int rawGid = bytes.getInt();
                        int gid = rawGid & 0x0fffffff;
                        terrain[x][height - 1 - row] = types.getOrDefault(gid - firstGid, "blocked");
                        int tile = gid - firstGid;
                        if (tile < 0 || tile >= 192) continue;
                        for (int py = 0; py < TILE_PIXELS; py++) for (int px = 0; px < TILE_PIXELS; px++) {
                            int source = sourcePixel(px, py, rawGid);
                            ground[x * TILE_PIXELS + px][(height - row) * TILE_PIXELS - 1 - py] =
                                mask[(tile % 12) * TILE_PIXELS + source % TILE_PIXELS]
                                    [(tile / 12) * TILE_PIXELS + source / TILE_PIXELS];
                        }
                    }
                }
            }
            // Merge neighboring solid pixels into short horizontal rectangles for fast circle/sweep queries.
            for (int y = 0; y < ground[0].length; y++) {
                blockedRows.add(new ArrayList<>());
                for (int x = 0; x < ground.length;) {
                    if (ground[x][y]) { x++; continue; }
                    int from = x;
                    while (x < ground.length && !ground[x][y]) x++;
                    blockedRows.get(y).add(new Rectangle(from * PIXEL_SIZE, y * PIXEL_SIZE,
                        (x - from) * PIXEL_SIZE, PIXEL_SIZE));
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot load authoritative terrain: " + mapName, exception);
        }
    }

    /** Inverse Tiled transform in image (top-left) coordinates: undo H/V, then the axis swap. */
    static int sourcePixel(int x, int y, int flags) {
        if ((flags & 0x80000000) != 0) x = TILE_PIXELS - 1 - x;
        if ((flags & 0x40000000) != 0) y = TILE_PIXELS - 1 - y;
        if ((flags & 0x20000000) != 0) { int swap = x; x = y; y = swap; }
        return y * TILE_PIXELS + x;
    }

    private XmlReader.Element read(String name) throws Exception {
        if (name.contains("/") || name.contains("\\") || name.contains("..")) {
            throw new IllegalArgumentException("Invalid terrain resource name");
        }
        try (InputStream input = TiledTerrain.class.getResourceAsStream("/battlefields/" + name)) {
            if (input == null) throw new IllegalStateException("Missing terrain resource: " + name);
            return new XmlReader().parse(input);
        }
    }

    String at(float x, float y) {
        int px = (int)Math.floor(x * TILE_PIXELS), py = (int)Math.floor(y * TILE_PIXELS);
        if (px < 0 || py < 0 || px >= ground.length || py >= ground[0].length) return "fall";
        if (ground[px][py]) return "ground";
        return terrain[px / TILE_PIXELS][py / TILE_PIXELS];
    }

    boolean isWalkable(float x, float y, float radius) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || x - radius < 0f || y - radius < 0f
            || x + radius >= terrain.length || y + radius >= terrain[0].length) return false;
        float radius2 = radius * radius;
        for (int row = (int)((y - radius) * TILE_PIXELS); row <= (int)((y + radius) * TILE_PIXELS); row++) {
            for (Rectangle block : blockedRows.get(row)) {
                if (pointDistance2(x, y, block) <= radius2) return false;
            }
        }
        return true;
    }

    boolean segmentWalkable(Vector2 start, Vector2 end, float radius) {
        if (!isWalkable(start.x, start.y, radius) || !isWalkable(end.x, end.y, radius)) return false;
        float left = Math.min(start.x, end.x) - radius, right = Math.max(start.x, end.x) + radius;
        float radius2 = radius * radius;
        for (int row = (int)((Math.min(start.y, end.y) - radius) * TILE_PIXELS);
             row <= (int)((Math.max(start.y, end.y) + radius) * TILE_PIXELS); row++) {
            for (Rectangle block : blockedRows.get(row)) {
                if (block.x > right || block.x + block.width < left) continue;
                if (Intersector.intersectSegmentRectangle(start, end, block)
                    || pointDistance2(start.x, start.y, block) <= radius2
                    || pointDistance2(end.x, end.y, block) <= radius2
                    || segmentDistance2(start, end, block.x, block.y) <= radius2
                    || segmentDistance2(start, end, block.x + block.width, block.y) <= radius2
                    || segmentDistance2(start, end, block.x, block.y + block.height) <= radius2
                    || segmentDistance2(start, end, block.x + block.width, block.y + block.height) <= radius2) return false;
            }
        }
        return true;
    }

    private float pointDistance2(float x, float y, Rectangle block) {
        float dx = Math.max(block.x - x, Math.max(0f, x - block.x - block.width));
        float dy = Math.max(block.y - y, Math.max(0f, y - block.y - block.height));
        return dx * dx + dy * dy;
    }

    private float segmentDistance2(Vector2 start, Vector2 end, float x, float y) {
        float dx = end.x - start.x, dy = end.y - start.y;
        float length2 = dx * dx + dy * dy;
        float t = length2 == 0f ? 0f : Math.max(0f, Math.min(1f, ((x - start.x) * dx + (y - start.y) * dy) / length2));
        float ex = x - start.x - t * dx, ey = y - start.y - t * dy;
        return ex * ex + ey * ey;
    }
}
