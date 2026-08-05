package com.github.thedragonconquerors.movement;

import com.badlogic.gdx.math.Vector2;
import com.github.thedragonconquerors.entities.Player;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 *updates player position every frame
 * each frame, we compute a direction vector from player's current position to tagret
 *
 * if remaining distance to target is less than one frame's step, we snap the player directly to the target instead of overshooting
 */

@Setter
@Getter
public class MovementSystem {
    private static final float ARRIVAL_THRESHOLD = 0.05f;   //minimum distance to target before snapping
    private NavGrid navGrid;

    public boolean setDestination(Player player, Vector2 clickedWorldPos)
    {
        if (navGrid == null) return false;

        float remaining = player.getMovementController().getRemainingMovementDistance();
        List<Vector2> path = navGrid.findPath(
            player.getPosition(), clickedWorldPos, remaining);

        if (path.isEmpty()) return false;

        // Clamp path to remaining distance budget
        List<Vector2> clampedPath = clampPathToDistance(player.getPosition(), path, remaining);
        if (clampedPath.isEmpty()) return false;

        player.getMovementController().setPath(clampedPath);
        return true;
    }

    public void setNetworkDestination(Player player, Vector2 destination)
    {
        if(navGrid == null)
        {
            player.getMovementController().setNetworkTarget(player.getPosition(), destination);
            return;
        }

        List<Vector2> path = navGrid.findPath(player.getPosition(), destination, Float.MAX_VALUE);

        if(path.isEmpty())  return;

        player.getMovementController().setNetworkPath(player.getPosition(), path);
    }

    private List<Vector2> clampPathToDistance(Vector2 start, List<Vector2> path, float maxDistance)
    {
        List<Vector2> clamped = new ArrayList<>();
        float distSoFar = 0f;
        Vector2 prev = start;

        for (Vector2 waypoint : path)
        {
            float segDist = prev.dst(waypoint);
            if (distSoFar + segDist > maxDistance)
            {
                // Interpolate to exact limit
                float remaining = maxDistance - distSoFar;
                float t = remaining / segDist;
                clamped.add(new Vector2(
                    prev.x + (waypoint.x - prev.x) * t,
                    prev.y + (waypoint.y - prev.y) * t
                ));
                break;
            }
            clamped.add(new Vector2(waypoint));
            distSoFar += segDist;
            prev = waypoint;
        }
        return clamped;
    }

    public void update(Player player, float delta){
        MovementController controller = player.getMovementController();

        if(!controller.isMoving())  return;
        if(controller.getRemainingMovementDistance() <= 0){
            controller.stopMoving();
            return;
        }

        Vector2 currentPos  = player.getPosition();
        Vector2 nextWaypoint = controller.getCurrentWaypoint();
        if (nextWaypoint == null) {
            controller.stopMoving();
            return;
        }

        float distToWaypoint = currentPos.dst(nextWaypoint);

        // Arrived at this waypoint — advance to next
        if (distToWaypoint <= ARRIVAL_THRESHOLD) {
            player.setPosition(nextWaypoint.x, nextWaypoint.y);
            controller.deductDistance(distToWaypoint);
            controller.advanceWaypoint();
            return;
        }

        // Move toward current waypoint
        float stepDistance = player.getSpeed() * delta;
        stepDistance = Math.min(stepDistance, distToWaypoint);
        stepDistance = Math.min(stepDistance, controller.getRemainingMovementDistance());

        Vector2 dir = new Vector2(nextWaypoint).sub(currentPos).nor();
        player.setPosition(
            currentPos.x + dir.x * stepDistance,
            currentPos.y + dir.y * stepDistance
        );
        controller.deductDistance(stepDistance);
    }

}
