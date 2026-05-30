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
import com.github.thedragonconquerors.assets.SpriteAssets;
import com.github.thedragonconquerors.entities.CharacterClass;
import com.github.thedragonconquerors.entities.Player;
import com.github.thedragonconquerors.input.MouseInputHandler;
import com.github.thedragonconquerors.movement.MovementSystem;
import com.github.thedragonconquerors.movement.NavGrid;
import com.github.thedragonconquerors.rendering.HudRenderer;
import com.github.thedragonconquerors.rendering.PlayerRenderer;
import com.shared.shared.model.Action;
import com.shared.shared.model.Packet;

import com.github.thedragonconquerors.combat.ActionResult;
import com.github.thedragonconquerors.combat.ActionSystem;
import com.github.thedragonconquerors.combat.ActionType;
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
    private ArrayList<Player> enemyPlayers = new ArrayList<>();
    private final Map<Integer, Player> playersById = new HashMap<>();
    private int localPlayerId = -1;
    private boolean receivingInitialPlayerList = false;

    private PlayerRenderer playerRenderer;
    private MouseInputHandler mouseInputHandler;
    private HudRenderer hudRenderer;

    private final NetworkClient networkClient;

    private ActionSystem actionSystem;
    private ActionType[] availableActions;

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
        this.enemyPlayers = new ArrayList<>();
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
        actionSystem = new ActionSystem();

        Scanner scanner = new Scanner(System.in);

        // ── step 1: pick team ─────────────────────────────────────
        CharacterClass chosenClass = CharacterClass.WARRIOR;
        while(true)
        {
            System.out.println("1. Blue \n2. Red");
            System.out.print("Select team: ");
            int teamIdx = scanner.nextInt();
            if(teamIdx == 1 || teamIdx == 2)
            {
                // ── step 2: pick class ────────────────────────────
                System.out.println("Select your class:");
                CharacterClass[] classes = CharacterClass.values();
                for (int i = 0; i < classes.length; i++) {
                    System.out.println((i + 1) + ". " + classes[i].displayName);
                }
                System.out.print("Enter number: ");
                int classIdx = scanner.nextInt() - 1;
                if (classIdx >= 0 && classIdx < classes.length) {
                    chosenClass = classes[classIdx];
                }
                System.out.println("Class selected: " + chosenClass.displayName);

                float spawnX = (teamIdx == 1) ? 0 : 29;
                spawnLocalPlayer(localPlayerId, "Name", spawnX, 9, chosenClass);
                availableActions = ActionType.availableFor(chosenClass);
                break;
            }
            System.out.println("Invalid team, please enter 1 or 2.");
        }

        //wire input = click anywhere to set target
        mouseInputHandler = new MouseInputHandler(camera, viewport, localPlayer, movementSystem, this::sendLocalMove);
        Gdx.input.setInputProcessor(mouseInputHandler);

        // Load map first (blocking), then sprites individually with graceful fallback
        TiledMap map = assetService.load(MAIN);
        mapRenderer = new OrthogonalTiledMapRenderer(map, Main.UNIT_SCALE, batch);
        navGrid = new NavGrid(map, Main.UNIT_SCALE, Main.WORLD_WIDTH, Main.WORLD_HEIGHT);
        movementSystem.setNavGrid(navGrid);

        for (SpriteAssets sprite : SpriteAssets.values()) {
            try {
                assetService.load(sprite);
            } catch (Exception e) {
                System.out.println("Sprite not found, skipping: " + sprite.name() + " (" + e.getMessage() + ")");
            }
        }

        playerRenderer = new PlayerRenderer(assetService);
        hudRenderer = new HudRenderer(viewport);

        // wire action callback: index → execute action
        hudRenderer.setOnActionSelected(this::executeAction);

        // give MouseInputHandler access to HUD so it can forward clicks
        mouseInputHandler.setHudContext(hudRenderer, availableActions, localPlayer.getStats());
    }

    /**
     * Manually called at the start of the game to add the local player at the map.
     * @param characterClass the class the player selected before joining
     */
    private void spawnLocalPlayer(int localPlayerId, String username,
                                  float worldX, float worldY,
                                  CharacterClass characterClass)
    {
        receivingInitialPlayerList = true;

        Vector2 startingPosition = new Vector2(worldX, worldY);
        networkClient.join("local-player", new Vector2(worldX, worldY));
        localPlayer = new Player(-1, username, worldX, worldY, characterClass);
    }

    /**
     * LibGDX automatically calls this function repeatedly to render the current state of the screen.
     * @param delta The time in seconds since the last render.
     */
    @Override
    public void render(float delta){
        //update player animation
        movementSystem.update(localPlayer, delta);

        for(Player enemy : enemyPlayers)
        {
            movementSystem.update(enemy, delta);
        }

        //handle end turn key
        if(Gdx.input.isKeyJustPressed(Input.Keys.E))    endTurn();

        // update action panel hover state
        hudRenderer.updateHover(com.badlogic.gdx.Gdx.input.getX(), com.badlogic.gdx.Gdx.input.getY(), availableActions);

        //clear screen
        ScreenUtils.clear(Color.BLACK);

        viewport.apply();
        batch.setColor(Color.WHITE);
        mapRenderer.setView(this.camera);
        mapRenderer.render();

        //render player
        playerRenderer.render(localPlayer, camera.combined, navGrid);
        for(Player player : enemyPlayers)
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
                System.out.println("Finished receiving existing players. Count: " + enemyPlayers.size());
                break;
            case JOIN:
                if(packet.getID() != localPlayerId && !playersById.containsKey(packet.getID()))
                {
                    Vector2 position = packet.getFinalPosition();
                    if(position == null)    return;
                    Player player = new Player
                    (
                        packet.getID(),
                        packet.getUsername(),
                        new Vector2(position)
                    );
                    enemyPlayers.add(player);
                    playersById.put(packet.getID(), player);
                    System.out.println("Enemy player " + packet.getID() + " joined at " + packet.getFinalPosition());
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
        Vector2 destination = packet.getFinalPosition();
        if(enemyPlayer == null || destination == null)
        {
            return;
        }

        movementSystem.setNetworkDestination(enemyPlayer, new Vector2(destination));
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
                .finalPosition(targetPos)
                .action(Action.MOVE)
                .build();

        networkClient.send(packet);
    }

    private void receiveExistingPlayer(Packet packet)
    {
        Vector2 position = packet.getFinalPosition();
        if(position == null)    return;

        if(packet.getID() == localPlayerId || playersById.containsKey(packet.getID()))  return;

        Player existingPlayer = new Player(packet.getID(), packet.getUsername(), new Vector2(position));
        enemyPlayers.add(existingPlayer);
        playersById.put(packet.getID(), existingPlayer);

        if(receivingInitialPlayerList)  System.out.println("Received existingPlayer player " + packet.getID() + " at " + position.x + ", " + position.y);
    }

    private void removeEnemyPlayer(int id)
    {
        Player enemy = playersById.remove(id);
        if(enemy != null)   enemyPlayers.remove(enemy);
    }

    private void executeAction(int index) {
        if (availableActions == null || localPlayer == null) return;
        if (index < 0 || index >= availableActions.length) return;
        ActionType action = availableActions[index];
        ActionResult result = actionSystem.execute(localPlayer, enemyPlayers, action);
        hudRenderer.showFeedback(result);
        System.out.println("[Action] " + result.message);
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
