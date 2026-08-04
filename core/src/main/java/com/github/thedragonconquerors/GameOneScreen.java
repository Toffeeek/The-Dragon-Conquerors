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
import com.github.thedragonconquerors.combat.ActionAnimation;
import com.github.thedragonconquerors.combat.ActionResult;
import com.github.thedragonconquerors.combat.ActionSystem;
import com.github.thedragonconquerors.combat.ActionType;
import com.github.thedragonconquerors.entities.Player;
import com.github.thedragonconquerors.input.MouseInputHandler;
import com.github.thedragonconquerors.movement.MovementSystem;
import com.github.thedragonconquerors.movement.NavGrid;
import com.github.thedragonconquerors.rendering.HudRenderer;
import com.github.thedragonconquerors.rendering.PlayerRenderer;
import com.shared.shared.model.Action;
import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.Packet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.github.thedragonconquerors.assets.MapAssets.MAIN;

public class GameOneScreen extends ScreenAdapter {
    private final Main game;
    private final Batch batch;
    private final AssetService assetService;
    private final Viewport viewport;
    private final OrthographicCamera camera;
    private final NetworkClient networkClient;
    private final int teamIndex;
    private final CharacterClass chosenClass;

    private OrthogonalTiledMapRenderer mapRenderer;
    private MovementSystem movementSystem;
    private NavGrid navGrid;
    private Player localPlayer;
    private int activePlayerId = -1;
    private final ArrayList<Player> enemyPlayers = new ArrayList<>();
    private final Map<Integer, Player> playersById = new HashMap<>();
    private int localPlayerId = -1;
    private boolean receivingInitialPlayerList = false;

    private PlayerRenderer playerRenderer;
    private MouseInputHandler mouseInputHandler;
    private HudRenderer hudRenderer;
    private ActionSystem actionSystem;
    private ActionType[] availableActions;
    private int selectedActionIndex = 0;
    private ActionType pendingTargetAction;

    public GameOneScreen(Main game, int teamIndex, CharacterClass chosenClass) {
        this.networkClient = game.getNetworkClient();
        this.teamIndex = teamIndex;
        this.chosenClass = chosenClass;
        this.game = game;
        this.assetService = game.getAssetService();
        this.viewport = game.getViewport();
        this.camera = game.getCamera();
        this.batch = game.getBatch();
        this.networkClient.setPacketHandler(
            packet -> Gdx.app.postRunnable(() -> handlePacket(packet)));
    }

    @Override
    public void show() {
        movementSystem = new MovementSystem();
        actionSystem = new ActionSystem();

        float spawnX = teamIndex == 1 ? 1f : 28f;
        spawnLocalPlayer("Name", spawnX, 9f, chosenClass);
        availableActions = ActionType.availableFor(chosenClass);

        TiledMap map = assetService.load(MAIN);
        mapRenderer = new OrthogonalTiledMapRenderer(map, Main.UNIT_SCALE, batch);
        navGrid = new NavGrid(map, Main.UNIT_SCALE, Main.WORLD_WIDTH, Main.WORLD_HEIGHT);
        movementSystem.setNavGrid(navGrid);

        for (SpriteAssets sprite : SpriteAssets.values()) {
            try {
                assetService.load(sprite);
            } catch (Exception exception) {
                System.out.println("Sprite sheet could not be loaded: " + sprite.name()
                    + " (" + exception.getMessage() + ")");
            }
        }

        playerRenderer = new PlayerRenderer(assetService, batch, this::localPlayerIsActive);
        hudRenderer = new HudRenderer(viewport);
        hudRenderer.setJoinUrl(game.getHostedJoinUrl());
        mouseInputHandler = new MouseInputHandler(
            camera, viewport, localPlayer, movementSystem,
            this::handleWorldClick, this::sendLocalMove, this::localPlayerIsActive);
        Gdx.input.setInputProcessor(mouseInputHandler);
    }

    private void spawnLocalPlayer(String username, float worldX, float worldY,
                                  CharacterClass characterClass) {
        receivingInitialPlayerList = true;
        Vector2 startingPosition = new Vector2(worldX, worldY);
        networkClient.join("local-player", startingPosition, characterClass);
        localPlayer = new Player(-1, username, startingPosition, characterClass);
    }

    private boolean localPlayerIsActive()
    {
        return activePlayerId == localPlayerId;
    }

    @Override
    public void render(float delta) {
        movementSystem.update(localPlayer, delta);
        for (Player enemy : enemyPlayers) movementSystem.update(enemy, delta);

        if (Gdx.input.isKeyJustPressed(Input.Keys.E) && localPlayerIsActive())
        {
            endTurn();
        }
        handleActionKeys();

        ScreenUtils.clear(Color.BLACK);
        viewport.apply();
        batch.setColor(Color.WHITE);
        mapRenderer.setView(camera);
        mapRenderer.render();

        playerRenderer.renderLocal(localPlayer, camera.combined, navGrid, delta);
        for (Player enemy : enemyPlayers) {
            boolean selecting = pendingTargetAction != null && enemy.getStats().getHp() > 0;
            boolean inRange = selecting && localPlayer.getPosition().dst(enemy.getPosition())
                <= pendingTargetAction.range;
            playerRenderer.renderEnemy(enemy, camera.combined, delta, selecting, inRange);
        }

        hudRenderer.render(localPlayer, delta);
    }

    private void handlePacket(Packet packet) {
        switch (packet.getAction()) {
            case PRIVATE_JOIN_CONFIRMATION:
                localPlayerId = packet.getID();
                this.activePlayerId = packet.getActivePlayerID();
                System.out.println("My player ID is " + localPlayerId + " Current Active Player: " + activePlayerId);
                break;
            case PLAYER_COORDINATE:
                receiveExistingPlayer(packet);
                break;
            case EOF:
                receivingInitialPlayerList = false;
                System.out.println("Finished receiving existing players. Count: "
                    + enemyPlayers.size());
                break;
            case JOIN:
                receiveJoin(packet);
                break;
            case MOVE:
                if (packet.getID() != localPlayerId) moveEnemyPlayer(packet);
                break;
            case END_TURN:
                System.out.println(activePlayerId + " has ended their turn");
                this.activePlayerId = packet.getActivePlayerID();
                System.out.println(activePlayerId + " is the new active player");
                break;
            case LEAVE:
                removeEnemyPlayer(packet.getID());
                break;
            default:
                break;
        }
    }

    private void receiveJoin(Packet packet) {
        if (isPendingLocalJoin(packet)) {
            System.out.println("Received my own join packet before confirmation; ignoring.");
            return;
        }
        if (packet.getID() == localPlayerId || playersById.containsKey(packet.getID())) return;

        Vector2 position = packet.getFinalPosition();
        if (position == null) return;
        CharacterClass characterClass = packet.getCharacterClass() == null
            ? CharacterClass.WARRIOR : packet.getCharacterClass();
        Player player = new Player(packet.getID(), packet.getUsername(),
            new Vector2(position), characterClass);
        enemyPlayers.add(player);
        playersById.put(packet.getID(), player);
    }

    private void moveEnemyPlayer(Packet packet) {
        Player enemyPlayer = playersById.get(packet.getID());
        Vector2 destination = packet.getFinalPosition();
        if (enemyPlayer == null || destination == null) return;
        movementSystem.setNetworkDestination(enemyPlayer, new Vector2(destination));
    }

    private void sendLocalMove(Vector2 targetPosition) {
        if (localPlayerId < 0) {
            System.out.println("Cannot send MOVE before server assigns local player ID.");
            return;
        }

        Packet packet = Packet.builder()
            .ID(localPlayerId)
            .username("local-player")
            .finalPosition(targetPosition)
            .action(Action.MOVE)
            .build();
        networkClient.send(packet);
    }

    private void receiveExistingPlayer(Packet packet) {
        Vector2 position = packet.getFinalPosition();
        if (position == null || packet.getID() == localPlayerId
            || playersById.containsKey(packet.getID())) return;

        CharacterClass characterClass = packet.getCharacterClass() == null
            ? CharacterClass.WARRIOR : packet.getCharacterClass();
        Player existingPlayer = new Player(packet.getID(), packet.getUsername(),
            new Vector2(position), characterClass);
        enemyPlayers.add(existingPlayer);
        playersById.put(packet.getID(), existingPlayer);

        if (receivingInitialPlayerList) {
            System.out.println("Received existing player " + packet.getID()
                + " at " + position.x + ", " + position.y);
        }
    }

    private boolean isPendingLocalJoin(Packet packet) {
        if (localPlayerId >= 0 || localPlayer == null
            || packet.getFinalPosition() == null) return false;

        return "local-player".equals(packet.getUsername())
            && localPlayer.getPosition().epsilonEquals(packet.getFinalPosition(), 0.001f)
            && packet.getCharacterClass() == chosenClass;
    }

    private void removeEnemyPlayer(int id) {
        Player enemy = playersById.remove(id);
        if (enemy != null) enemyPlayers.remove(enemy);
        if (enemyPlayers.isEmpty()) cancelTargetSelection(false);
    }

    private void handleActionKeys() {

        if(!localPlayerIsActive())
        {
//            System.out.println("WAIT FO YO TURN FOO");
            return;
        }

        if (availableActions == null) return;

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) && pendingTargetAction != null) {
            cancelTargetSelection(true);
            return;
        }

        int[] keys = {
            Input.Keys.NUM_1,
            Input.Keys.NUM_2,
            Input.Keys.NUM_3,
            Input.Keys.NUM_4
        };

        for (int i = 0; i < keys.length && i < availableActions.length; i++) {
            if (!Gdx.input.isKeyJustPressed(keys[i])) continue;
            selectedActionIndex = i;
            hudRenderer.setSelectedActionIndex(i);
            selectAction(availableActions[i]);
            return;
        }
    }

    private void selectAction(ActionType action) {
        if (localPlayer.getStats().getHp() <= 0) {
            cancelTargetSelection(false);
            hudRenderer.showFeedback("A defeated player cannot act.");
            return;
        }

        if (action.manaCost > 0 && localPlayer.getStats().getMana() < action.manaCost) {
            cancelTargetSelection(false);
            hudRenderer.showFeedback(ActionResult.noMana(action));
            return;
        }

        if (action.targetsSelf) {
            cancelTargetSelection(false);
            executeSelfAction(action);
            return;
        }

        boolean hasLivingTarget = false;
        for (Player enemy : enemyPlayers) {
            if (enemy.getStats().getHp() > 0) {
                hasLivingTarget = true;
                break;
            }
        }
        if (!hasLivingTarget) {
            cancelTargetSelection(false);
            hudRenderer.showFeedback("No player is available to target.");
            return;
        }

        pendingTargetAction = action;
        hudRenderer.showTargetingPrompt(action);
        hudRenderer.showFeedback("Click the player you want to attack.");
    }

    /** Returns true while targeting so the click is not also interpreted as movement. */
    private boolean handleWorldClick(Vector2 clickedWorldPosition) {
        if (pendingTargetAction == null) return false;

        Player selectedTarget = findClickedEnemy(clickedWorldPosition);
        if (selectedTarget == null) {
            hudRenderer.showFeedback("Click directly on a highlighted player.");
            return true;
        }

        ActionType action = pendingTargetAction;
        float targetDistance = localPlayer.getPosition().dst(selectedTarget.getPosition());
        if (targetDistance > action.range) {
            hudRenderer.showFeedback(ActionResult.outOfRange(
                action, targetDistance, action.range));
            return true;
        }

        cancelTargetSelection(false);
        ActionResult result = actionSystem.execute(localPlayer, selectedTarget, action);

        if (result.outcome == ActionResult.Outcome.HIT
            || result.outcome == ActionResult.Outcome.MISS) {
            localPlayer.getAnimationController().playAttack(
                localPlayer.getPosition(), selectedTarget.getPosition(),
                action.animation == ActionAnimation.CAST);
        }

        if (result.outcome == ActionResult.Outcome.HIT) {
            if (selectedTarget.getStats().getHp() <= 0) {
                selectedTarget.getAnimationController().playDeath();
            } else {
                selectedTarget.getAnimationController().playHurt(
                    localPlayer.getPosition(), selectedTarget.getPosition());
            }
        }

        hudRenderer.showFeedback("Target: " + selectedTarget.getUsername()
            + " — " + result.message);
        System.out.println("[Action] " + result.message);
        return true;
    }

    private Player findClickedEnemy(Vector2 clickedWorldPosition) {
        Player best = null;
        float bestDistance = PlayerRenderer.TARGET_CLICK_RADIUS;
        for (Player enemy : enemyPlayers) {
            if (enemy.getStats().getHp() <= 0) continue;
            float distance = enemy.getPosition().dst(clickedWorldPosition);
            if (distance <= bestDistance) {
                best = enemy;
                bestDistance = distance;
            }
        }
        return best;
    }

    private void executeSelfAction(ActionType action) {
        ActionResult result = actionSystem.execute(localPlayer, localPlayer, action);
        if (result.outcome == ActionResult.Outcome.SELF_EFFECT) {
            localPlayer.getAnimationController().playAttack(
                localPlayer.getPosition(),
                new Vector2(localPlayer.getPosition()).add(0f, 1f),
                action.animation == ActionAnimation.CAST);
        }
        hudRenderer.showFeedback(result);
        System.out.println("[Action] " + result.message);
    }

    private void cancelTargetSelection(boolean showMessage) {
        pendingTargetAction = null;
        if (hudRenderer != null) {
            hudRenderer.clearTargetingPrompt();
            if (showMessage) hudRenderer.showFeedback("Target selection cancelled.");
        }
    }

    private void endTurn() {
        cancelTargetSelection(false);
        localPlayer.onTurnStart();

        Packet packet = Packet.builder()
            .ID(localPlayerId)
            .action(Action.END_TURN)
            .build();
        networkClient.send(packet);

        System.out.println("Turn ended — movement distance reset.");

    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        if (playerRenderer != null) playerRenderer.dispose();
        if (mapRenderer != null) mapRenderer.dispose();
        if (hudRenderer != null) hudRenderer.dispose();
    }
}
