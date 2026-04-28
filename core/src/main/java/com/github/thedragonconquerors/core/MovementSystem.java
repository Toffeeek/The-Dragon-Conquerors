package com.github.thedragonconquerors.core;

/*
We run a Dijkstra flood-fill from the player's current position to the outward
every tile whose accumulated cost <= plkayer's remaining stamina is marked reachable
we stop explanding beyond stamina limit
 */

import com.badlogic.gdx.graphics.g2d.CpuSpriteBatch;
import com.github.thedragonconquerors.entities.Player;

import java.lang.management.PlatformLoggingMXBean;
import java.util.*;

public class MovementSystem {
    private final GridManager gridManager;

    //tiles current highlighted as reachable for the active player
    private final Set<Tile> reachableTiles = new HashSet<>();

    //the path to the selected/howvered tile
    private List<Tile> currentPath = new ArrayList<>();

    public MovementSystem(GridManager gridManager){
        this.gridManager = gridManager;
    }

    //highlight reachable tiles from player position
    //uses dijkstra so variable movement costs work correctly

    public void computeReachableTiles(Player player){
        gridManager.clearAllHighlights();
        reachableTiles.clear();

        Tile start = gridManager.getTile(player.getGridX(), player.getGridY());
        if(start==null) return;

        //cost map: how much stamina is specnt reaching the tile
        Map<Tile, Integer> costMap = new HashMap<>();
        PriorityQueue<Tile> open = new PriorityQueue<>(Comparator.comparingInt(t->costMap.getOrDefault(t, Integer.MAX_VALUE)));
        costMap.put(start, 0);
        open.add(start);

        while(!open.isEmpty()){
            Tile current = open.poll();
            int currentCost = costMap.get(current);

            for(Tile neighbor : gridManager.getNeighbors(current)){
                if(!neighbor.isPassable())  continue;   //skips obstruction or if occupied by a player

                int newCost = currentCost + neighbor.getMovementCost();
                if(newCost>player.getRemainingStamina())    continue;   //out of range

                if(newCost<costMap.getOrDefault(neighbor, Integer.MAX_VALUE)){
                    costMap.put(neighbor, newCost);
                    open.add(neighbor);
                }
            }
        }

        //mark every reachable tile
        for(Map.Entry<Tile, Integer> entry : costMap.entrySet()){
            Tile tile = entry.getKey();
            if(tile!=start){
                tile.setHighlightState(Tile.HighlightState.REACHABLE);
                reachableTiles.add(tile);
            }
        }
    }

    //highlight hovered tile
    public List<Tile> computePath(Player player, Tile target){
        //clear old path highlight
        for(Tile t : currentPath){
            if(reachableTiles.contains(t))  t.setHighlightState(Tile.HighlightState.REACHABLE);
            else                            t.setHighlightState(Tile.HighlightState.NONE);
        }
        currentPath.clear();

        Tile start = gridManager.getTile(player.getGridX(), player.getGridY());
        currentPath = aStar(start, target);

        //apply path highlight
        for(Tile t : currentPath){
            t.setHighlightState(Tile.HighlightState.PATH);
        }

        return currentPath;
    }

    private List<Tile> aStar(Tile start, Tile goal){
        Map<Tile, Tile> cameFrom = new HashMap<>();
        Map<Tile, Integer> gScore = new HashMap<>();
        PriorityQueue<Tile> open = new PriorityQueue<>(Comparator.comparingInt(t->gScore.getOrDefault(t, Integer.MAX_VALUE) + heuristic(t, goal)));

        gScore.put(start, 0);
        open.add(start);

        while(!open.isEmpty()){
            Tile current = open.poll();
            if(current == goal) return reconstructPath(cameFrom, goal, start);

            for(Tile neighbor : gridManager.getNeighbors(current)){
                if(!neighbor.isPassable())  continue;
                int tentative = gScore.getOrDefault(current, Integer.MAX_VALUE) + neighbor.getMovementCost();
                if(tentative<gScore.getOrDefault(neighbor, Integer.MAX_VALUE)){
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tentative);
                    open.add(neighbor);
                }
            }
        }

        return new ArrayList<>();   //no path found
    }

    private int heuristic(Tile a, Tile b){
        return Math.abs(a.getGridX()-b.getGridX()) + Math.abs(a.getGridY()-b.getGridY());
    }

    private List<Tile> reconstructPath(Map<Tile, Tile> cameFrom, Tile goal, Tile start){
        List<Tile> path = new ArrayList<>();
        Tile current = goal;

        while(current!=start && current!=null){
            path.add(0, current);
            current = cameFrom.get(current);
        }

        return path;
    }

    //player movement confirmation and stamina reduction
    public void movePlayer(Player player, List<Tile> path){
        if(path.isEmpty())  return;

        Tile destination = path.get(path.size()-1);

        //calculating path cost
        int pathCost =0;
        for(Tile t : path)  pathCost += t.getMovementCost();
        if(pathCost>player.getRemainingStamina())   return;

        //free old tiles
        Tile oldTile = gridManager.getTile(player.getGridX(), player.getGridY());
        if(oldTile!=null)   oldTile.setOccupied(false);

        //update player grid position and stamina
        player.setGridPosition(destination.getGridX(), destination.getGridY());
        player.deductStamina(pathCost);
        destination.setOccupied(true);
        player.startMovementAnimation(path);

        //recompute reachable tiles
        if(player.getRemainingStamina()>0)  computeReachableTiles(player);
        else{
            gridManager.clearAllHighlights();
            reachableTiles.clear();
        }
    }

    public Set<Tile> getReachableTiles(){
        return reachableTiles;
    }

    public List<Tile> getCurrentPath() {
        return currentPath;
    }
}
