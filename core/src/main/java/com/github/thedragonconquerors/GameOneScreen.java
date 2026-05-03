package com.github.thedragonconquerors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.client.client.NetworkClient;
import com.github.thedragonconquerors.assets.AssetService;
import com.github.thedragonconquerors.entities.Player;
import com.github.thedragonconquerors.input.MouseInputHandler;
import com.github.thedragonconquerors.movement.MovementSystem;
import com.github.thedragonconquerors.movement.NavGrid;
import com.github.thedragonconquerors.rendering.HudRenderer;
import com.github.thedragonconquerors.rendering.PlayerRenderer;
import com.shared.shared.model.Action;
import com.shared.shared.model.Packet;
import com.shared.shared.model.Pair;

import java.util.*;

import static com.github.thedragonconquerors.assets.MapAssets.MAIN;

public class GameOneScreen extends ScreenAdapter
{
    private final Main game;
    private final Batch batch;
    private final AssetService assetService;
    private final Viewport viewport;
    private final OrthographicCamera camera;
    private OrthogonalTiledMapRenderer mapRenderer;
    private MovementSystem movementSystem;
    private NavGrid navGrid;

    private Player localPlayer;
    private ArrayList<Player> enemyPlayer = new ArrayList<>();
    private final Map<Integer, Player> playersById = new HashMap<>();
    private int localPlayerId = -1;
    private boolean receivingInitialPlayerList = false;

    private PlayerRenderer playerRenderer;
    private MouseInputHandler mouseInputHandler;
    private HudRenderer hudRenderer;

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
        movementSystem = new MovementSystem();

        Scanner scanner = new Scanner(System.in);
        while(true)
        {
            System.out.println("1. Blue \n2. Red");
            System.out.print("Select team: ");
            int teamIdx = scanner.nextInt();
            switch (teamIdx)
            {
                case 1:
                    spawnLocalPlayer(localPlayerId, "Name", 0,9);
                    break;
                case 2:
                    spawnLocalPlayer(localPlayerId, "Name", 29,9);
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

        //wire input = click anywhere to set target
        mouseInputHandler = new MouseInputHandler(camera, viewport, localPlayer, movementSystem, this::sendLocalMove);
        Gdx.input.setInputProcessor(mouseInputHandler);

        playerRenderer = new PlayerRenderer();
        hudRenderer = new HudRenderer(viewport);

        TiledMap map = assetService.load(MAIN);
        mapRenderer = new OrthogonalTiledMapRenderer(map, Main.UNIT_SCALE, batch);
        navGrid = new NavGrid(map, Main.UNIT_SCALE, Main.WORLD_WIDTH, Main.WORLD_HEIGHT);
        movementSystem.setNavGrid(navGrid);
    }

    /**
     * Manually called at the start of the game to add the local player at the map
     */
    private void spawnLocalPlayer(int localPlayerId, String username, float worldX, float worldY) {
        receivingInitialPlayerList = true;

        //convert world position to tile coordinates
        int tileX = (int)worldX;
        int tileY = (int)worldY;
        networkClient.join("local-player", tileX, tileY);

        localPlayer = new Player(-1, username, worldX, worldY);
    }

    /**
     * LibGDX automatically calls this function repeatedly to render the current state of the screen.
     * @param delta The time in seconds since the last render.
     */
    @Override
    public void render(float delta){
        //update player animation
        movementSystem.update(localPlayer, delta);

        for(Player enemy : enemyPlayer)
        {
            movementSystem.update(enemy, delta);
        }

        //handle end turn key
        if(Gdx.input.isKeyJustPressed(Input.Keys.E))    endTurn();

        //clear screen
        ScreenUtils.clear(Color.BLACK);

        viewport.apply();
        batch.setColor(Color.WHITE);
        mapRenderer.setView(this.camera);
        mapRenderer.render();

        //render player
        playerRenderer.render(localPlayer, camera.combined, navGrid);
        for(Player player : enemyPlayer)
        {
            playerRenderer.render(player, camera.combined);
        }

        hudRenderer.render(localPlayer, delta);
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
                        position.second
                    );
                    enemyPlayer.add(player);
                    playersById.put(packet.getID(), player);
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
                removeEnemyPlayer(packet.getID());
                break;
            default:
                break;
        }
    }

    private void moveEnemyPlayer(Packet packet)
    {
        Player enemyPlayer = playersById.get(packet.getID());
        Pair<Integer, Integer> finalPosition = packet.getFinalPosition();
        if(enemyPlayer == null || finalPosition == null)
        {
            return;
        }

        Vector2 destination = new Vector2(finalPosition.first, finalPosition.second);
        enemyPlayer.getMovementController().setTarget(enemyPlayer.getPosition(), destination);
    }

    private void sendLocalMove(Vector2 targetPos)
    {
        if(localPlayerId < 0)
        {
            System.out.println("Cannot send MOVE before server assigns local player ID.");
            return;
        }

        Packet packet = Packet.builder()
                .ID(localPlayerId)
                .username("local-player")
                .finalPosition(new Pair<>((int)targetPos.x, (int)targetPos.y))
                .action(Action.MOVE)
                .build();

        networkClient.send(packet);
    }

    private void receiveExistingPlayer(Packet packet)
    {
        Pair<Integer, Integer> position = packet.getFinalPosition();
        if(position == null)    return;

        if(packet.getID() == localPlayerId || playersById.containsKey(packet.getID()))  return;

        Player existing = new Player(packet.getID(), packet.getUsername(), position.first, position.second);
        enemyPlayer.add(existing);
        playersById.put(packet.getID(), existing);

        if(receivingInitialPlayerList)  System.out.println("Received existing player " + packet.getID() + " at " + position.first + ", " + position.second);
    }

    private void removeEnemyPlayer(int id){
        Player enemy = playersById.remove(id);
        if(enemy != null)   enemyPlayer.remove(enemy);
    }

    //ends current turn and resets player stamina
    private void endTurn()
    {
        localPlayer.onTurnStart();
        System.out.println("Turn ended — movement distance reset.");
    }

    @Override
    public void resize(int width, int height)
    {
        viewport.update(width, height, true);
    }

    @Override
    public void hide(){
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        playerRenderer.dispose();
        mapRenderer.dispose();
        hudRenderer.dispose();
    }
}
