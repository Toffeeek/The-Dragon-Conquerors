package com.shared.shared.model.world;

import com.badlogic.gdx.math.Vector2;
import java.util.*;

/** Deterministic collision-aware routes shared by the server and client. */
public final class BattlefieldNavigation {
    public static final float NODE_SIZE = 0.125f;
    public static final float PLAYER_SEPARATION = 0.65f;
    public static final float EPSILON = 0.001f;
    private final BattlefieldDefinition battlefield;
    private final int cols, rows;
    private final boolean[] open;
    private record Node(int id, float distance, float priority) {}
    private record Search(float[] distance, int[] previous, int target) {}

    public BattlefieldNavigation(BattlefieldDefinition battlefield) {
        this.battlefield = battlefield;
        cols = (int)(battlefield.getWidth() / NODE_SIZE);
        rows = (int)(battlefield.getHeight() / NODE_SIZE);
        open = new boolean[cols * rows];
        for (int id = 0; id < open.length; id++) open[id] = battlefield.isWalkable(point(id));
    }

    public List<Vector2> findPath(Vector2 start, Vector2 goal, float budget, Collection<Vector2> occupied) {
        if (!Float.isFinite(budget) || budget <= EPSILON || !battlefield.isWalkable(start)
            || !battlefield.isWalkable(goal) || !unoccupied(goal, occupied)) return List.of();
        if (segmentClear(start, goal, occupied)) return clamp(start, List.of(new Vector2(goal)), budget);
        Search search = search(start, goal, Float.MAX_VALUE, occupied);
        int end = search.target;
        if (end < 0) return List.of();
        List<Vector2> path = new ArrayList<>();
        for (int id = end; id >= 0; id = search.previous[id]) path.add(point(id));
        Collections.reverse(path);
        path.add(new Vector2(goal));
        List<Vector2> smooth = new ArrayList<>();
        Vector2 from = start;
        for (int index = 0; index < path.size();) {
            int furthest = index;
            for (int next = index + 1; next < path.size(); next++) {
                if (segmentClear(from, path.get(next), occupied)) furthest = next;
            }
            from = path.get(furthest);
            smooth.add(from);
            index = furthest + 1;
        }
        return clamp(start, smooth, budget);
    }

    public List<Vector2> reachable(Vector2 start, float budget, Collection<Vector2> occupied) {
        if (!battlefield.isWalkable(start) || budget <= EPSILON || !Float.isFinite(budget)) return List.of();
        Search search = search(start, null, budget, occupied);
        List<Vector2> result = new ArrayList<>();
        for (int id = 0; id < open.length; id++) if (search.distance[id] <= budget) result.add(point(id));
        return result;
    }

    private Search search(Vector2 start, Vector2 goal, float budget, Collection<Vector2> occupied) {
        float[] distance = new float[open.length];
        int[] previous = new int[open.length];
        Arrays.fill(distance, Float.POSITIVE_INFINITY);
        Arrays.fill(previous, -2);
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingDouble(Node::priority).thenComparingInt(Node::id));
        int sx = (int)(start.x / NODE_SIZE), sy = (int)(start.y / NODE_SIZE);
        for (int x = sx - 1; x <= sx + 1; x++) for (int y = sy - 1; y <= sy + 1; y++) {
            if (x < 0 || y < 0 || x >= cols || y >= rows) continue;
            int id = y * cols + x;
            Vector2 point = point(id);
            float cost = start.dst(point);
            if (!open[id] || cost > budget || !segmentClear(start, point, occupied)) continue;
            distance[id] = cost;
            previous[id] = -1;
            queue.add(new Node(id, cost, cost + (goal == null ? 0f : point.dst(goal))));
        }
        int target = -1;
        while (!queue.isEmpty()) {
            Node current = queue.poll();
            if (current.distance > distance[current.id] + EPSILON) continue;
            int cx = current.id % cols, cy = current.id / cols;
            Vector2 from = point(current.id);
            // A legal shoreline click need not share a cell with a legal node centre.
            if (goal != null && from.dst(goal) <= NODE_SIZE * 2f && segmentClear(from, goal, occupied)) {
                target = current.id;
                break;
            }
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int x = cx + dx, y = cy + dy;
                if (x < 0 || y < 0 || x >= cols || y >= rows) continue;
                int id = y * cols + x;
                if (!open[id]) continue;
                Vector2 to = point(id);
                float cost = current.distance + from.dst(to);
                if (cost > budget || cost + EPSILON >= distance[id] || !segmentClear(from, to, occupied)) continue;
                distance[id] = cost;
                previous[id] = current.id;
                queue.add(new Node(id, cost, cost + (goal == null ? 0f : to.dst(goal))));
            }
        }
        return new Search(distance, previous, target);
    }

    public boolean segmentClear(Vector2 start, Vector2 end, Collection<Vector2> occupied) {
        if (!battlefield.pathIsWalkable(start, end)) return false;
        for (Vector2 player : occupied) {
            float dx = end.x - start.x, dy = end.y - start.y;
            float length2 = dx * dx + dy * dy;
            float t = length2 == 0f ? 0f : Math.max(0f, Math.min(1f,
                ((player.x - start.x) * dx + (player.y - start.y) * dy) / length2));
            if (player.dst2(start.x + t * dx, start.y + t * dy)
                < PLAYER_SEPARATION * PLAYER_SEPARATION) return false;
        }
        return true;
    }

    private boolean unoccupied(Vector2 goal, Collection<Vector2> occupied) {
        for (Vector2 player : occupied) if (player.dst(goal) < PLAYER_SEPARATION) return false;
        return true;
    }

    public static List<Vector2> clamp(Vector2 start, List<Vector2> path, float budget) {
        List<Vector2> result = new ArrayList<>();
        Vector2 previous = start;
        for (Vector2 waypoint : path) {
            float distance = previous.dst(waypoint);
            if (distance <= EPSILON) continue;
            float spend = Math.min(distance, budget);
            if (spend <= EPSILON) break;
            result.add(new Vector2(previous).lerp(waypoint, spend / distance));
            budget -= spend;
            if (budget <= EPSILON) break;
            previous = waypoint;
        }
        return result;
    }

    public static float length(Vector2 start, List<Vector2> path) {
        float distance = 0f;
        for (Vector2 waypoint : path) { distance += start.dst(waypoint); start = waypoint; }
        return distance;
    }
    private int node(Vector2 p) { return Math.min(rows - 1, (int)(p.y / NODE_SIZE)) * cols + Math.min(cols - 1, (int)(p.x / NODE_SIZE)); }
    private Vector2 point(int id) { return new Vector2((id % cols + 0.5f) * NODE_SIZE, (id / cols + 0.5f) * NODE_SIZE); }
}
