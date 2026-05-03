package com.github.thedragonconquerors.core;

import java.util.ArrayList;
import java.util.List;

public class GridManager {  //manages the 2d grid of the environment
    public static final int COLS = 30;
    public static final int ROWS = 17;
    public static final float TILE_WORLD_SIZE = 1f;

    GameMap map;

    private final Tile[][] tiles;

    public GridManager(GameMap map)
    {
        this.map = map;
        tiles = new Tile[COLS][ROWS];
        initGrid();
    }

    // builds the grid
    private void initGrid()
    {
        for(int i=0; i<COLS; i++){
            for(int j=0; j<ROWS; j++)   tiles[i][j] = new Tile(i, j, TILE_WORLD_SIZE);
        }

        if(map == GameMap.MAP1)
        {

            setWalkable(0, 0, false);
            setWalkable(1, 0, false);
            setWalkable(2, 0, false);
            setWalkable(3, 0, false);
            setWalkable(4, 0, false);
            setWalkable(5, 0, false);
            setWalkable(6, 0, false);
            setWalkable(7, 0, false);
            setWalkable(8, 0, false);
            setWalkable(9, 0, false);
            setWalkable(10, 0, false);
            setWalkable(11, 0, false);
            setWalkable(12, 0, false);
            setWalkable(13, 0, false);
            setWalkable(14, 0, false);
            setWalkable(15, 0, false);
            setWalkable(16, 0, false);
            setWalkable(17, 0, false);
            setWalkable(18, 0, false);
            setWalkable(19, 0, false);
            setWalkable(20, 0, false);
            setWalkable(21, 0, false);
            setWalkable(22, 0, false);
            setWalkable(23, 0, false);
            setWalkable(24, 0, false);
            setWalkable(25, 0, false);
            setWalkable(26, 0, false);
            setWalkable(27, 0, false);
            setWalkable(28, 0, false);
            setWalkable(29, 0, false);

            setWalkable(0, 1, false);
            setWalkable(1, 1, false);
            setWalkable(2, 1, false);
            setWalkable(3, 1, false);
            setWalkable(11, 1, false);
            setWalkable(12, 1, false);
            setWalkable(13, 1, false);
            setWalkable(14, 1, false);
            setWalkable(15, 1, false);
            setWalkable(16, 1, false);
            setWalkable(17, 1, false);
            setWalkable(18, 1, false);
            setWalkable(19, 1, false);
            setWalkable(20, 1, false);
            setWalkable(27, 1, false);
            setWalkable(28, 1, false);
            setWalkable(29, 1, false);

            setWalkable(0, 2, false);
            setWalkable(1, 2, false);
            setWalkable(2, 2, false);
            setWalkable(12, 2, false);
            setWalkable(13, 2, false);
            setWalkable(14, 2, false);
            setWalkable(15, 2, false);
            setWalkable(16, 2, false);
            setWalkable(17, 2, false);
            setWalkable(18, 2, false);
            setWalkable(19, 2, false);
            setWalkable(28, 2, false);
            setWalkable(29, 2, false);

            setWalkable(0, 3, false);
            setWalkable(1, 3, false);
            setWalkable(28, 3, false);
            setWalkable(29, 3, false);

            setWalkable(0, 4, false);
            setWalkable(7, 4, false);
            setWalkable(8, 4, false);
            setWalkable(9, 4, false);
            setWalkable(10, 4, false);
            setWalkable(11, 4, false);
            setWalkable(12, 4, false);
            setWalkable(13, 4, false);
            setWalkable(17, 4, false);
            setWalkable(18, 4, false);
            setWalkable(19, 4, false);
            setWalkable(21, 4, false);
            setWalkable(24, 4, false);
            setWalkable(25, 4, false);
            setWalkable(26, 4, false);
            setWalkable(27, 4, false);
            setWalkable(28, 4, false);

            setWalkable(7, 5, false);
            setWalkable(8, 5, false);
            setWalkable(9, 5, false);
            setWalkable(10, 5, false);
            setWalkable(11, 5, false);
            setWalkable(12, 5, false);
            setWalkable(13, 5, false);
            setWalkable(17, 5, false);
            setWalkable(18, 5, false);
            setWalkable(19, 5, false);
            setWalkable(21, 5, false);
            setWalkable(22, 5, false);

            setWalkable(9, 6, false);
            setWalkable(10, 6, false);
            setWalkable(11, 6, false);
            setWalkable(12, 6, false);
            setWalkable(13, 6, false);

            setWalkable(22, 6, false);

            setWalkable(11, 7, false);
            setWalkable(12, 7, false);
            setWalkable(13, 7, false);
            setWalkable(14, 7, false);
            setWalkable(18, 7, false);
            setWalkable(19, 7, false);
            setWalkable(20, 7, false);
            setWalkable(21, 7, false);
            setWalkable(22, 7, false);
            setWalkable(26, 7, false);
            setWalkable(27, 7, false);


            setWalkable(4, 8, false);
            setWalkable(5, 8, false);
            setWalkable(6, 8, false);
            setWalkable(11, 8, false);
            setWalkable(12, 8, false);
            setWalkable(13, 8, false);
            setWalkable(14, 8, false);
            setWalkable(17, 8, false);
            setWalkable(18, 8, false);
            setWalkable(26, 8, false);
            setWalkable(27, 8, false);

            setWalkable(4, 9, false);
            setWalkable(5, 9, false);
            setWalkable(6, 9, false);
            setWalkable(20, 9, false);
            setWalkable(21, 9, false);
            setWalkable(22, 9, false);
            setWalkable(26, 9, false);
            setWalkable(27, 9, false);

            setWalkable(6, 10, false);
            setWalkable(7, 10, false);
            setWalkable(20, 10, false);
            setWalkable(22, 10, false);
            setWalkable(23, 10, false);
            setWalkable(26, 10, false);
            setWalkable(27, 10, false);

            setWalkable(7, 11, false);
            setWalkable(8, 11, false);
            setWalkable(9, 11, false);
            setWalkable(10, 11, false);
            setWalkable(11, 11, false);
            setWalkable(12, 11, false);
            setWalkable(17, 11, false);
            setWalkable(18, 11, false);
            setWalkable(19, 11, false);
            setWalkable(20, 11, false);

            setWalkable(12, 12, false);
            setWalkable(13, 12, false);
            setWalkable(15, 12, false);
            setWalkable(16, 12, false);
            setWalkable(17, 12, false);

            setWalkable(7, 13, false);
            setWalkable(8, 13, false);
            setWalkable(9, 13, false);
            setWalkable(10, 13, false);
            setWalkable(15, 13, false);
            setWalkable(19, 13, false);
            setWalkable(20, 13, false);
            setWalkable(21, 13, false);
            setWalkable(23, 13, false);
            setWalkable(24, 13, false);
            setWalkable(25, 13, false);
            setWalkable(28, 13, false);
            setWalkable(29, 13, false);

            setWalkable(0, 14, false);
            setWalkable(1, 14, false);
            setWalkable(4, 14, false);
            setWalkable(5, 14, false);
            setWalkable(6, 14, false);
            setWalkable(7, 14, false);
            setWalkable(8, 14, false);
            setWalkable(9, 14, false);
            setWalkable(10, 14, false);
            setWalkable(11, 14, false);
            setWalkable(15, 14, false);
            setWalkable(19, 14, false);
            setWalkable(20, 14, false);
            setWalkable(23, 14, false);
            setWalkable(24, 14, false);
            setWalkable(25, 14, false);
            setWalkable(27, 14, false);
            setWalkable(28, 14, false);
            setWalkable(29, 14, false);

            setWalkable(0, 15, false);
            setWalkable(1, 15, false);
            setWalkable(2, 15, false);
            setWalkable(3, 15, false);
            setWalkable(4, 15, false);
            setWalkable(5, 15, false);
            setWalkable(6, 15, false);
            setWalkable(7, 15, false);
            setWalkable(8, 15, false);
            setWalkable(9, 15, false);
            setWalkable(10, 15, false);
            setWalkable(11, 15, false);
            setWalkable(12, 15, false);
            setWalkable(26, 15, false);
            setWalkable(27, 15, false);
            setWalkable(28, 15, false);
            setWalkable(29, 15, false);

            setWalkable(0, 16, false);
            setWalkable(1, 16, false);
            setWalkable(2, 16, false);
            setWalkable(3, 16, false);
            setWalkable(4, 16, false);
            setWalkable(5, 16, false);
            setWalkable(6, 16, false);
            setWalkable(7, 16, false);
            setWalkable(8, 16, false);
            setWalkable(9, 16, false);
            setWalkable(10, 16, false);
            setWalkable(11, 16, false);
            setWalkable(12, 16, false);
            setWalkable(13, 16, false);
            setWalkable(14, 16, false);
            setWalkable(15, 16, false);
            setWalkable(16, 16, false);
            setWalkable(17, 16, false);
            setWalkable(18, 16, false);
            setWalkable(19, 16, false);
            setWalkable(20, 16, false);
            setWalkable(21, 16, false);
            setWalkable(22, 16, false);
            setWalkable(23, 16, false);
            setWalkable(24, 16, false);
            setWalkable(25, 16, false);
            setWalkable(26, 16, false);
            setWalkable(27, 16, false);
            setWalkable(28, 16, false);
            setWalkable(29, 16, false);

        }

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
