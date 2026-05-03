package com.github.thedragonconquerors.movement;

import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.*;

public class NavGrid {
    public static final float NODE_SIZE = 0.25f;
    private final int cols;
    private final int rows;
    private final boolean[][] walkable;
    private final float worldWidth;
    private final float worldHeight;

    public NavGrid(TiledMap map, float unitScale, float worldWidth, float worldHeight) {
        this.worldWidth  = worldWidth;
        this.worldHeight = worldHeight;
        this.cols = (int) Math.ceil(worldWidth  / NODE_SIZE);
        this.rows = (int) Math.ceil(worldHeight / NODE_SIZE);
        this.walkable = new boolean[cols][rows];

        // Start with everything walkable
        for (boolean[] col : walkable) Arrays.fill(col, true);

        // Build collision polygons and mark blocked nodes
        List<Polygon> collisionPolygons = buildCollisionPolygons(map, unitScale);
        markBlockedNodes(collisionPolygons);

        System.out.println("NavGrid: " + cols + "x" + rows + " nodes, unitScale=" + unitScale);
    }

    //collision polygon extraction
    private List<Polygon> buildCollisionPolygons(TiledMap map, float unitScale) {
        List<Polygon> polygons = new ArrayList<>();
        for (var layer : map.getLayers()) {
            if (!(layer instanceof TiledMapTileLayer)) continue;
            TiledMapTileLayer tileLayer = (TiledMapTileLayer) layer;
            int mapCols   = tileLayer.getWidth();
            int mapRows   = tileLayer.getHeight();
            int tilePixW  = (int) tileLayer.getTileWidth();
            int tilePixH  = (int) tileLayer.getTileHeight();

            for (int c = 0; c < mapCols; c++) {
                for (int r = 0; r < mapRows; r++) {
                    TiledMapTileLayer.Cell cell = tileLayer.getCell(c, r);
                    if (cell == null) continue;
                    TiledMapTile tile = cell.getTile();
                    if (tile == null) continue;
                    MapObjects objects = tile.getObjects();
                    if (objects.getCount() == 0) continue;

                    float tileWorldX = c * tilePixW * unitScale;
                    float tileWorldY = r * tilePixH * unitScale;

                    for (MapObject obj : objects) {
                        Polygon p = extractPolygon(
                            obj, tileWorldX, tileWorldY, tilePixH, unitScale);
                        if (p != null) polygons.add(p);
                    }
                }
            }
        }
        return polygons;
    }

    private Polygon extractPolygon(MapObject obj,
                                   float tileWorldX, float tileWorldY,
                                   int tilePixelH, float unitScale) {
        if (obj instanceof PolygonMapObject) {
            PolygonMapObject polyObj = (PolygonMapObject) obj;
            float[] lv = polyObj.getPolygon().getVertices();
            float ox = polyObj.getPolygon().getX();
            float oy = polyObj.getPolygon().getY();
            float[] wv = new float[lv.length];
            for (int i = 0; i < lv.length; i += 2) {
                wv[i]     = tileWorldX + (ox + lv[i])     * unitScale;
                wv[i + 1] = tileWorldY + (tilePixelH - (oy + lv[i + 1])) * unitScale;
            }
            return new Polygon(wv);
        } else if (obj instanceof RectangleMapObject) {
            Rectangle r = ((RectangleMapObject) obj).getRectangle();
            float x = tileWorldX + r.x * unitScale;
            float y = tileWorldY + (tilePixelH - r.y - r.height) * unitScale;
            float w = r.width  * unitScale;
            float h = r.height * unitScale;
            return new Polygon(new float[]{x,y, x+w,y, x+w,y+h, x,y+h});
        }
        return null;
    }

    private void markBlockedNodes(List<Polygon> polygons) {
        float half = NODE_SIZE / 2f;
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                float cx = c * NODE_SIZE + half;
                float cy = r * NODE_SIZE + half;
                Rectangle nodeBounds = new Rectangle(cx - half, cy - half, NODE_SIZE, NODE_SIZE);
                Polygon nodeSquare = new Polygon(new float[]{
                    nodeBounds.x, nodeBounds.y,
                    nodeBounds.x + nodeBounds.width, nodeBounds.y,
                    nodeBounds.x + nodeBounds.width, nodeBounds.y + nodeBounds.height,
                    nodeBounds.x, nodeBounds.y + nodeBounds.height
                });
                for (Polygon poly : polygons) {
                    if (Intersector.overlapConvexPolygons(nodeSquare, poly)) {
                        walkable[c][r] = false;
                        break;
                    }
                }
            }
        }
    }

    public List<Vector2> findPath(Vector2 startWorld, Vector2 goalWorld, float maxDistance) {
        int[] startNode = worldToNode(startWorld);
        int[] goalNode  = worldToNode(goalWorld);

        // Clamp goal to nearest walkable node if it's inside a wall
        goalNode = nearestWalkable(goalNode);
        if (goalNode == null) return Collections.emptyList();

        // A* open set ordered by f = g + h
        PriorityQueue<int[]> open = new PriorityQueue<>(
            Comparator.comparingDouble(n -> (double) n[2]));
        Map<Integer, Float> gScore  = new HashMap<>();
        Map<Integer, int[]>   cameFrom = new HashMap<>();

        int startKey = key(startNode[0], startNode[1]);
        gScore.put(startKey, 0f);
        open.add(new int[]{startNode[0], startNode[1], 0});

        while (!open.isEmpty()) {
            int[] current = open.poll();
            int cx = current[0], cy = current[1];

            if (cx == goalNode[0] && cy == goalNode[1]) {
                return reconstructPath(cameFrom, goalNode, startNode);
            }

            for (int[] neighbor : getNeighbors(cx, cy)) {
                int nx = neighbor[0], ny = neighbor[1];
                if (!walkable[nx][ny]) continue;

                float stepCost = (neighbor[2] == 1) ? NODE_SIZE : NODE_SIZE * 1.414f;
                float tentativeG = gScore.getOrDefault(key(cx, cy), Float.MAX_VALUE) + stepCost;

                // Prune paths that exceed stamina budget
                if (tentativeG > maxDistance) continue;

                int nKey = key(nx, ny);
                if (tentativeG < gScore.getOrDefault(nKey, Float.MAX_VALUE)) {
                    gScore.put(nKey, tentativeG);
                    cameFrom.put(nKey, new int[]{cx, cy});
                    float h = heuristic(nx, ny, goalNode[0], goalNode[1]);
                    open.add(new int[]{nx, ny, (int)((tentativeG + h) * 1000)});
                }
            }
        }
        return Collections.emptyList(); // no path found within budget
    }

    public List<Vector2> getReachablePositions(Vector2 startWorld, float maxDistance) {
        int[] startNode = worldToNode(startWorld);
        List<Vector2> reachable = new ArrayList<>();

        Map<Integer, Float> dist = new HashMap<>();
        PriorityQueue<float[]> open = new PriorityQueue<>(
            Comparator.comparingDouble(n -> n[2]));

        int startKey = key(startNode[0], startNode[1]);
        dist.put(startKey, 0f);
        open.add(new float[]{startNode[0], startNode[1], 0f});

        while (!open.isEmpty()) {
            float[] current = open.poll();
            int cx = (int) current[0], cy = (int) current[1];
            float cd = current[2];

            reachable.add(nodeToWorld(cx, cy));

            for (int[] neighbor : getNeighbors(cx, cy)) {
                int nx = neighbor[0], ny = neighbor[1];
                if (!walkable[nx][ny]) continue;

                float stepCost = (neighbor[2] == 1) ? NODE_SIZE : NODE_SIZE * 1.414f;
                float newDist = cd + stepCost;
                if (newDist > maxDistance) continue;

                int nKey = key(nx, ny);
                if (newDist < dist.getOrDefault(nKey, Float.MAX_VALUE)) {
                    dist.put(nKey, newDist);
                    open.add(new float[]{nx, ny, newDist});
                }
            }
        }
        return reachable;
    }

    private List<Vector2> reconstructPath(Map<Integer, int[]> cameFrom,
                                          int[] goal, int[] start) {
        List<Vector2> path = new ArrayList<>();
        int[] current = goal;
        while (!(current[0] == start[0] && current[1] == start[1])) {
            path.add(0, nodeToWorld(current[0], current[1]));
            int[] prev = cameFrom.get(key(current[0], current[1]));
            if (prev == null) break;
            current = prev;
        }
        return path;
    }

    private List<int[]> getNeighbors(int c, int r) {
        List<int[]> neighbors = new ArrayList<>();
        int[][] dirs = {
            {1,0,1},{-1,0,1},{0,1,1},{0,-1,1},   // cardinal
            {1,1,0},{-1,1,0},{1,-1,0},{-1,-1,0}   // diagonal
        };
        for (int[] d : dirs) {
            int nc = c + d[0], nr = r + d[1];
            if (nc >= 0 && nc < cols && nr >= 0 && nr < rows)
                neighbors.add(new int[]{nc, nr, d[2]});
        }
        return neighbors;
    }

    private float heuristic(int c1, int r1, int c2, int r2) {
        // Octile distance — correct for 8-directional grid
        float dx = Math.abs(c1 - c2);
        float dy = Math.abs(r1 - r2);
        return NODE_SIZE * (Math.max(dx, dy) + (1.414f - 1) * Math.min(dx, dy));
    }

    public int[] worldToNode(Vector2 world) {
        int c = (int)(world.x / NODE_SIZE);
        int r = (int)(world.y / NODE_SIZE);
        c = Math.max(0, Math.min(cols - 1, c));
        r = Math.max(0, Math.min(rows - 1, r));
        return new int[]{c, r};
    }

    public Vector2 nodeToWorld(int c, int r) {
        return new Vector2(c * NODE_SIZE + NODE_SIZE / 2f,
            r * NODE_SIZE + NODE_SIZE / 2f);
    }

    private int[] nearestWalkable(int[] node) {
        if (walkable[node[0]][node[1]]) return node;
        for (int radius = 1; radius < 5; radius++) {
            for (int dc = -radius; dc <= radius; dc++) {
                for (int dr = -radius; dr <= radius; dr++) {
                    int nc = node[0] + dc, nr = node[1] + dr;
                    if (nc >= 0 && nc < cols && nr >= 0 && nr < rows
                        && walkable[nc][nr])
                        return new int[]{nc, nr};
                }
            }
        }
        return null;
    }

    private int key(int c, int r) { return c * 10000 + r; }

    public boolean isWalkable(int c, int r) { return walkable[c][r]; }
    public int getCols() { return cols; }
    public int getRows() { return rows; }
}
