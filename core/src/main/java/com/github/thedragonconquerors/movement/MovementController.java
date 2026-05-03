package com.github.thedragonconquerors.movement;

import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

/**
 * holds the target position the player has clicked
 * tracks how much movement distance remians current turn
 *
 * when a player clicks a point futher than the remaining distance, the click doesn;t get rejected
 * the actual target is clicked along the path
 */

public class MovementController {
    private List<Vector2> path = new ArrayList<>();
    private int waypointIndex  = 0;
    private boolean moving     = false;

    private float remainingMovementDistance;
    private final float maxMovementDistance;

    public MovementController(float maxMovementDistance) {
        this.maxMovementDistance       = maxMovementDistance;
        this.remainingMovementDistance = maxMovementDistance;
    }

    //sets a new waypoint path and replaces any current path
    public void setPath(List<Vector2> newPath) {
        if (newPath == null || newPath.isEmpty()) return;
        this.path          = new ArrayList<>(newPath);
        this.waypointIndex = 0;
        this.moving        = true;
    }

    //returns new waypoint the player should walk
    public Vector2 getCurrentWaypoint() {
        if (waypointIndex >= path.size()) return null;
        return path.get(waypointIndex);
    }

    //advances to next waypoint in the path
    public void advanceWaypoint(){
        waypointIndex++;
        if(waypointIndex >= path.size())    moving = false;
    }

    public void deductDistance(float amount) {
        remainingMovementDistance -= amount;
        if (remainingMovementDistance <= 0) {
            remainingMovementDistance = 0;
            moving = false;
        }
    }

    public void stopMoving() {
        moving = false;
    }

    public void resetForNewTurn() {
        remainingMovementDistance = maxMovementDistance;
        path.clear();
        waypointIndex = 0;
        moving = false;
    }

    // Keep setTarget for enemy players receiving server positions
    public void setTarget(Vector2 currentPos, Vector2 target) {
        List<Vector2> singleStep = new ArrayList<>();
        singleStep.add(new Vector2(target));
        setPath(singleStep);
    }

    public Vector2 getTargetPosition() {
        if (path.isEmpty()) return null;
        return path.get(path.size() - 1);
    }

    public boolean isMoving()                       { return moving; }
    public float getRemainingMovementDistance()     { return remainingMovementDistance; }
    public float getMaxMovementDistance()           { return maxMovementDistance; }
    public List<Vector2> getPath()                  { return path; }
}
