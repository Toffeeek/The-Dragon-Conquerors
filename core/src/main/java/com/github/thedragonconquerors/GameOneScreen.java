package com.github.thedragonconquerors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.client.client.NetworkClient;
import com.github.thedragonconquerors.assets.AssetService;
import com.github.thedragonconquerors.assets.MapAssets;
import com.github.thedragonconquerors.core.GridManager;
import com.github.thedragonconquerors.core.MovementSystem;
import com.github.thedragonconquerors.core.Tile;
import com.github.thedragonconquerors.entities.Player;
import com.github.thedragonconquerors.input.MouseInputHandler;
import com.github.thedragonconquerors.rendering.GridRenderer;
import com.github.thedragonconquerors.rendering.PlayerRenderer;
import com.shared.shared.model.Action;
import com.shared.shared.model.Packet;
import com.shared.shared.model.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class GameOneScreen extends ScreenAdapter
{
    private final Main game;
    private final Batch batch;
    private final AssetService assetService;
    private final Viewport viewport;
    private final OrthographicCamera camera;
    private OrthogonalTiledMapRenderer mapRenderer;
    private MovementSystem movementSystem;

    private Player localPlayer;
    private ArrayList<Player> enemyPlayer;
    private final Map<Integer, Player> playersById = new HashMap<>();
    private int localPlayerId = -1;
    private boolean receivingInitialPlayerList = false;


    private PlayerRenderer playerRenderer;
    private GridManager gridManager;
    private GridRenderer gridRenderer;
    private MouseInputHandler mouseInputHandler;


    private final NetworkClient networkClient;


    /**
     * Sets up the camera and the packet handler to communicate with the server
     */
    public GameOneScreen(Main game)
    {
        this.networkClient = game.getNetworkClient();
        this.game = game;
        this.assetService = game.getAssetService();
        this.viewport = game.getViewport();
        this.camera = game.getCamera();
        this.batch = game.getBatch();
        this.networkClient.setPacketHandler(packet -> Gdx.app.postRunnable(() -> handlePacket(packet)));
        this.enemyPlayer = new ArrayList<>();
    }

    /**
     * Automatically called after the constructor, use this function to set up the starting positions /
     * configurations of the game before launching the actual screen. Currently this function takes
     * terminal input to figure out the starting positions of players.
     */
    @Override
    public void show()
    {
        //build core system
        gridManager = new GridManager();
        movementSystem = new MovementSystem(gridManager);

        // Spawn player at tile(4, 2)
        Scanner scanner = new Scanner(System.in);

        while(true)
        {
            System.out.println("1. Blue \n2. Red");
            System.out.print("Select team: ");
            int teamIdx = scanner.nextInt();
            switch (teamIdx)
            {
                case 1:
                    spawnLocalPlayer(9,0);
                    break;
                case 2:
                    spawnLocalPlayer(9,11);
                    break;
                default:
                    System.out.println("Invalid team idx");
                    break;
            }
            if(teamIdx == 1 || teamIdx == 2)
            {
                break;
            }
        }

        //compute initial reachable tiles
        movementSystem.computeReachableTiles(localPlayer);

        //wire input
        mouseInputHandler = new MouseInputHandler(camera, viewport, gridManager, movementSystem, localPlayer, this::sendLocalMove);
        Gdx.input.setInputProcessor(mouseInputHandler);

        //build renderers
        gridRenderer = new GridRenderer(gridManager);
        playerRenderer = new PlayerRenderer();

        this.mapRenderer = new OrthogonalTiledMapRenderer(assetService.load(MapAssets.MAIN), Main.UNIT_SCALE, this.batch);
    }

    /**
     * Manually called at the start of the game to add the local player at the map
     */
    private void spawnLocalPlayer(int x, int y)
    {
        receivingInitialPlayerList = true;
        networkClient.join("local-player", x, y);

        localPlayer = new Player(localPlayerId, "local-player", x, y, 5);
        gridManager.getTile(x, y).setOccupied(true);
    }

    /**
     * LibGDX automatically calls this function repeatedly to render the current state of the screen.
     * @param delta The time in seconds since the last render.
     */
    @Override
    public void render(float delta){
        //update player animation
        localPlayer.update(delta);

        for(Player player : enemyPlayer)
        {
            player.update(delta);
        }

        //handle end turn key
        if(Gdx.input.isKeyJustPressed(Input.Keys.E))    endTurn();

        //clear screen
        ScreenUtils.clear(Color.BLACK);

        this.viewport.apply();
        this.batch.setColor(Color.WHITE);
        this.mapRenderer.setView(this.camera);
        this.mapRenderer.render();

        //render grid
        gridRenderer.render(camera.combined);

        //render player
        playerRenderer.render(localPlayer, camera.combined);
        for(Player player : enemyPlayer)
        {
            playerRenderer.render(player, camera.combined);
        }
    }

    /**
     * VERY IMPORTANT: This function is automatically called by the application whenever a packet
     * arrives from the server. The packet from the server is arrived in the form of the function parameter.
     * Handle the packet from the server based on the 'action' field of the packet.
     */
    private void handlePacket(Packet packet)
    {
        switch(packet.getAction())
        {
            case PRIVATE_JOIN_CONFIRMATION:
                localPlayerId = packet.getID();
                System.out.println("My player ID is " + localPlayerId);
                break;
            case PLAYER_COORDINATE:
                receiveExistingPlayer(packet);
                break;
            case EOF:
                receivingInitialPlayerList = false;
                System.out.println("Finished receiving existing players. Count: " + enemyPlayer.size());
                break;
            case JOIN:
                if(packet.getID() != localPlayerId)
                {
                    Pair<Integer, Integer> position = packet.getFinalPosition();
                    Player player = new Player
                    (
                        packet.getID(),
                        packet.getUsername(),
                        position.first,
                        position.second,
                        5
                    );
                    enemyPlayer.add(player);
                    playersById.put(packet.getID(), player);
                    gridManager.getTile(position.first, position.second).setOccupied(true);
                    System.out.println("Enemy player " + packet.getID() + " joined");
                }
                else
                {
                    System.out.println("Received my own join packet.. ignoring");
                }
                break;
            case MOVE:
                if(packet.getID() != localPlayerId)
                {
                    moveEnemyPlayer(packet);
                }
                break;
            case LEAVE:
                System.out.println("Game packet received: " + packet.getAction());
                break;
            default:
                break;
        }
    }

    private void moveEnemyPlayer(Packet packet)
    {
        Player player = playersById.get(packet.getID());
        Pair<Integer, Integer> position = packet.getFinalPosition();
        if(player == null || position == null)
        {
            return;
        }

        setTileOccupied(player.getGridX(), player.getGridY(), false);

        List<Tile> path = buildStraightPath(
                player.getGridX(),
                player.getGridY(),
                position.first,
                position.second
        );

        player.setGridPosition(position.first, position.second);
        setTileOccupied(position.first, position.second, true);

        if(!path.isEmpty())
        {
            player.startMovementAnimation(path);
        }
    }

    private void sendLocalMove(Tile destination)
    {
        if(localPlayerId < 0)
        {
            System.out.println("Cannot send MOVE before server assigns local player ID.");
            return;
        }

        Packet packet = Packet.builder()
                .ID(localPlayerId)
                .username("local-player")
                .finalPosition(new Pair<>(destination.getGridX(), destination.getGridY()))
                .action(Action.MOVE)
                .build();

        networkClient.send(packet);
    }

    private List<Tile> buildStraightPath(int startX, int startY, int targetX, int targetY)
    {
        List<Tile> path = new ArrayList<>();
        int x = startX;
        int y = startY;

        while(x != targetX)
        {
            x += Integer.compare(targetX, x);
            Tile tile = gridManager.getTile(x, y);
            if(tile != null)
            {
                path.add(tile);
            }
        }

        while(y != targetY)
        {
            y += Integer.compare(targetY, y);
            Tile tile = gridManager.getTile(x, y);
            if(tile != null)
            {
                path.add(tile);
            }
        }

        return path;
    }

    private void setTileOccupied(int x, int y, boolean occupied)
    {
        Tile tile = gridManager.getTile(x, y);
        if(tile != null)
        {
            tile.setOccupied(occupied);
            tile.setWalkable(!occupied);
        }
    }

    private void receiveExistingPlayer(Packet packet)
    {
        Pair<Integer, Integer> position = packet.getFinalPosition();
        if(position == null)
        {
            return;
        }

        if(packet.getID() == localPlayerId || playersById.containsKey(packet.getID()))
        {
            return;
        }

        Player player = new Player
        (
                packet.getID(),
                packet.getUsername(),
                position.first,
                position.second,
                5
        );

        enemyPlayer.add(player);
        playersById.put(packet.getID(), player);
        gridManager.getTile(position.first, position.second).setOccupied(true);

        if(receivingInitialPlayerList)
        {
            System.out.println("Received existing player " + packet.getID() + " at " + position.first + ", " + position.second);
        }
    }

    //ends current turn and resets player stamina
    private void endTurn(){
        localPlayer.resetStamina();
        movementSystem.computeReachableTiles(localPlayer);
    }

    @Override
    public void resize(int width, int height){
        viewport.update(width, height, true);
    }

    @Override
    public void hide(){
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        gridRenderer.dispose();
        playerRenderer.dispose();
        mapRenderer.dispose();
    }
}
