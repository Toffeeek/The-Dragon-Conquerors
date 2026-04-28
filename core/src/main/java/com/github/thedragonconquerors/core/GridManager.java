package com.github.thedragonconquerors.core;

import com.github.thedragonconquerors.assets.MapAssets;

import java.util.ArrayList;
import java.util.List;

public class GridManager {  //manages the 2d grid of the environment
    public static final int COLS = 20;
    public static final int ROWS = 12;
    public static final float TILE_WORLD_SIZE = 1f;

    private final Tile[][] tiles;

    public GridManager(){
        tiles = new Tile[COLS][ROWS];
        initGrid();
    }

    private void initGrid(){    //builds the grid
        for(int i=0; i<COLS; i++){
            for(int j=0; j<ROWS; j++)   tiles[i][j] = new Tile(i, j, TILE_WORLD_SIZE);
        }

        setWalkable(0, 0, false);
        setWalkable(1, 0, false);
        setWalkable(2, 0, false);
        setWalkable(3, 0, false);
        setWalkable(4, 0, false);
        setWalkable(0, 1, false);
        setWalkable(1, 1, false);
        setWalkable(2, 1, false);
        setWalkable(3, 1, false);
        setWalkable(0, 2, false);
        setWalkable(1, 2, false);
        setWalkable(2, 2, false);
        setWalkable(0, 3, false);
        setWalkable(0, 4, false);
        setWalkable(0, 5, false);
        setWalkable(1, 5, false);
        setWalkable(2, 5, false);
        setWalkable(3, 5, false);
        setWalkable(3, 6, false);
        setWalkable(2, 6, false);
        setWalkable(2, 7, false);
        setWalkable(1, 7, false);
        setWalkable(1, 8, false);
        setWalkable(1, 9, false);
        setWalkable(1, 10, false);
        setWalkable(3, 10, false);
        setWalkable(4, 10, false);
        setWalkable(4, 11, false);
        setWalkable(4, 8, false);
        setWalkable(5, 8, false);
        setWalkable(6, 8, false);
        setWalkable(7, 8, false);
        setWalkable(7, 7, false);
        setWalkable(7, 6, false);
        setWalkable(8, 6, false);
        setWalkable(8, 5, false);
        setWalkable(8, 4, false);
        setWalkable(8, 3, false);
        setWalkable(8, 2, false);
        setWalkable(7, 2, false);
        setWalkable(6, 2, false);
        setWalkable(5, 2, false);
        setWalkable(5, 3, false);
        setWalkable(5, 4, false);
        setWalkable(5, 5, false);
        setWalkable(6, 5, false);
        setWalkable(6, 6, false);
        setWalkable(11, 1, false);
        setWalkable(12, 1, false);
        setWalkable(12, 2, false);
        setWalkable(11, 2, false);
        setWalkable(2, 10, false);
    }

    public void setWalkable(int x, int y, boolean walkable){
        Tile t = getTile(x, y);
        if(t!=null) t.setWalkable(walkable);
    }

    public Tile getTile(int x, int y) {
        if(x<0 || x>=COLS || y<0 || y>=ROWS)    return null;    //if out of bounds
        return tiles[x][y];
    }

    public Tile getTileAtWorld (float worldX, float worldY){    // converst world prosition to the tile that contains it
        int gx = (int)Math.floor(worldX/TILE_WORLD_SIZE);
        int gy = (int)Math.floor(worldY/TILE_WORLD_SIZE);

        return getTile(gx, gy);
    }

    public void clearAllHighlights(){
        for(int x=0; x<COLS; x++){
            for(int y=0; y<ROWS; y++)   tiles[x][y].setHighlightState((Tile.HighlightState.NONE));
        }
    }

    public List<Tile> getNeighbors(Tile tile){      //returns 4 directional neighbors of a tile
        List<Tile> neighbors = new ArrayList<>();
        int x = tile.getGridX();
        int y = tile.getGridY();
        addIfExists(neighbors, x+1, y);
        addIfExists(neighbors, x-1, y);
        addIfExists(neighbors, x, y+1);
        addIfExists(neighbors, x, y-1);

        return neighbors;
    }

    private void addIfExists(List<Tile> list, int x, int y){
        Tile t = getTile(x, y);
        if(t!=null) list.add(t);
    }

    public int getCols(){
        return COLS;
    }

    public int getRows(){
        return ROWS;
    }
}
