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
import com.github.thedragonconquerors.entities.PlayerConverter;
import com.github.thedragonconquerors.input.MouseInputHandler;
import com.github.thedragonconquerors.movement.MovementSystem;
import com.github.thedragonconquerors.movement.NavGrid;
import com.github.thedragonconquerors.rendering.HudRenderer;
import com.github.thedragonconquerors.rendering.PlayerRenderer;
import com.shared.shared.model.Action;
import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.Packet;
import com.shared.shared.model.TEAM;
import com.shared.shared.model.PlayerState;

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

    private OrthogonalTiledMapRenderer mapRenderer;
    private MovementSystem movementSystem;
    private NavGrid navGrid;

    private final Vector2 GAMEONE_BLUE_SPAWN = new Vector2(1f, 9f);
    private final Vector2 GAMEONE_RED_SPAWN = new Vector2(28f, 9f);

    private Player localPlayer;
    private int activePlayerId = -1;
    private final ArrayList<Player> enemyPlayers = new ArrayList<>();
    private final Map<Integer, Player> playersById = new HashMap<>();

    private boolean receivingInitialPlayerList = false;

    private PlayerRenderer playerRenderer;
    private MouseInputHandler mouseInputHandler;
    private HudRenderer hudRenderer;
    private ActionSystem actionSystem;
    private ActionType[] availableActions;
    private int selectedActionIndex = 0;
    private ActionType pendingTargetAction;

    public GameOneScreen(Main game, TEAM team, CharacterClass chosenClass) {
        this.networkClient = game.getNetworkClient();


        this.game = game;
        this.assetService = game.getAssetService();
        this.viewport = game.getViewport();
        this.camera = game.getCamera();
        this.batch = game.getBatch();
        this.networkClient.setPacketHandler(packet -> Gdx.app.postRunnable(() -> handlePacket(packet)));

        availableActions = ActionType.availableFor(chosenClass);


        Vector2 startingPosition = team == TEAM.BLUE ? GAMEONE_BLUE_SPAWN : GAMEONE_RED_SPAWN;
        localPlayer = new Player(-1, "default-username", team, startingPosition, chosenClass);
        spawnLocalPlayer(localPlayer);
    }

    @Override
    public void show() {
        movementSystem = new MovementSystem();
        actionSystem = new ActionSystem();


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

    private void spawnLocalPlayer(Player player)
    {
        receivingInitialPlayerList = true;
        networkClient.join(PlayerConverter.toPlayerState(player));
    }

    private boolean localPlayerIsActive()
    {
        return activePlayerId == localPlayer.getID();
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
                localPlayer.setID(packet.getPlayer().getID());
                this.activePlayerId = packet.getActivePlayerID();
                System.out.println("My player ID is " + localPlayer.getID() + " Current Active Player: " + activePlayerId);
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
                if (packet.getPlayer().getID() != localPlayer.getID()) moveEnemyPlayer(packet);
                break;
            case PRIMARY:
            case SECONDARY:
            case TERTIARY:
            case ULTIMATE:
                processAttack(packet);
                break;
            case END_TURN:
                System.out.println(activePlayerId + " has ended their turn");
                this.activePlayerId = packet.getActivePlayerID();
                System.out.println(activePlayerId + " is the new active player");
                break;
            case LEAVE:
                this.activePlayerId = packet.getActivePlayerID();
                removeEnemyPlayer(packet.getPlayer().getID());
                break;
            default:
                break;
        }
    }

    private void processAttack(Packet p)
    {
        if (p.getPlayer().getID() == localPlayer.getID())
            return;

        var affectedPlayersId = p.getAffectedPlayersId();
        if (affectedPlayersId == null || affectedPlayersId.isEmpty())
            return;

        Player dealer = playerById(p.getPlayer().getID());
        Player firstAffectedPlayer = null;
        for (Integer affectedPlayerId : affectedPlayersId) {
            firstAffectedPlayer = playerById(affectedPlayerId);
            if (firstAffectedPlayer != null) break;
        }

        ActionType action = actionTypeFromPacket(p);
        if (dealer != null && firstAffectedPlayer != null) {
            dealer.getAnimationController().playAttack(
                dealer.getPosition(), firstAffectedPlayer.getPosition(),
                action.animation == ActionAnimation.CAST);
        }

        var deltaHealth = p.getDeltaHealth();
        var killedPlayersId = p.getKilledPlayersId();

        for (Integer affectedPlayerId : affectedPlayersId) {
            Player affectedPlayer = playerById(affectedPlayerId);
            if (affectedPlayer == null) continue;

            if (deltaHealth < 0) {
                affectedPlayer.getStats().applyDamage(-deltaHealth);
            } else if (deltaHealth > 0) {
                affectedPlayer.getStats().heal(deltaHealth);
            }

            boolean killed = killedPlayersId != null && killedPlayersId.contains(affectedPlayerId);
            if (killed || affectedPlayer.getStats().getHp() <= 0) {
                affectedPlayer.getStats().setHp(0);
                affectedPlayer.getAnimationController().playDeath();
            } else if (deltaHealth < 0 && dealer != null) {
                affectedPlayer.getAnimationController().playHurt(
                    dealer.getPosition(), affectedPlayer.getPosition());
            }
        }
    }

    private Player playerById(int playerId) {
        if (playerId == localPlayer.getID()) return localPlayer;
        return playersById.get(playerId);
    }

    private ActionType actionTypeFromPacket(Packet packet) {
        CharacterClass characterClass = packet.getPlayer().getCharacterClass() == null
            ? CharacterClass.WARRIOR : packet.getPlayer().getCharacterClass();
        ActionType[] actions = ActionType.availableFor(characterClass);
        int actionIndex;

        switch (packet.getAction()) {
            case SECONDARY:
                actionIndex = 1;
                break;
            case TERTIARY:
                actionIndex = 2;
                break;
            case ULTIMATE:
                actionIndex = 3;
                break;
            case PRIMARY:
            default:
                actionIndex = 0;
                break;
        }

        if (actionIndex >= actions.length) return actions[0];
        return actions[actionIndex];
    }

    private void receiveJoin(Packet packet) {
        if (packet.getPlayer() == null) return;

        if (isPendingLocalJoin(packet)) {
            System.out.println("Received my own join packet before confirmation; ignoring.");
            return;
        }
        if (packet.getPlayer().getID() == localPlayer.getID() || playersById.containsKey(packet.getPlayer().getID())) return;

        addRemotePlayer(packet.getPlayer());
    }

    private void moveEnemyPlayer(Packet packet) {
        Player enemyPlayer = playersById.get(packet.getPlayer().getID());
        Vector2 destination = packet.getPlayer().getPosition();
        if (enemyPlayer == null || destination == null) return;
        movementSystem.setNetworkDestination(enemyPlayer, new Vector2(destination));
    }

    private void sendLocalMove(Vector2 targetPosition) {
        if (localPlayer.getID() < 0) {
            System.out.println("Cannot send MOVE before server assigns local player ID.");
            return;
        }

        PlayerState playerState = PlayerConverter.toPlayerState(localPlayer);
        playerState.setPosition(new Vector2(targetPosition));

        Packet packet = Packet.builder()
            .player(playerState)
            .action(Action.MOVE)
            .build();
        networkClient.send(packet);
    }

    private void receiveExistingPlayer(Packet packet) {
        if (packet.getPlayer() == null) return;
        if (packet.getPlayer().getID() == localPlayer.getID()
            || playersById.containsKey(packet.getPlayer().getID())) return;

        Player existingPlayer = addRemotePlayer(packet.getPlayer());
        if (existingPlayer == null) return;

        Vector2 position = existingPlayer.getPosition();

        if (receivingInitialPlayerList) {
            System.out.println("Received existing player " + existingPlayer.getID()
                + " at " + position.x + ", " + position.y);
        }
    }

    private Player addRemotePlayer(PlayerState playerState) {
        if (playerState.getPosition() == null) return null;

        Player player = PlayerConverter.toPlayer(playerState);
        enemyPlayers.add(player);
        playersById.put(player.getID(), player);
        return player;
    }

    private boolean isPendingLocalJoin(Packet packet)
    {
        if (localPlayer == null || localPlayer.getID() >= 0
            || packet.getPlayer().getPosition() == null) return false;

        return localPlayer.getUsername().equals(packet.getPlayer().getUsername())
            && packet.getPlayer().getTeam() == localPlayer.getTeam()
            && localPlayer.getPosition().epsilonEquals(packet.getPlayer().getPosition(), 0.001f)
            && packet.getPlayer().getCharacterClass() == localPlayer.getCharacterClass();
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
            hudRenderer.setSelectedActionIndex(selectedActionIndex);
            selectAction(availableActions[selectedActionIndex]);
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
            || result.outcome == ActionResult.Outcome.MISS)
        {
            localPlayer.getAnimationController().playAttack(
                localPlayer.getPosition(), selectedTarget.getPosition(),
                action.animation == ActionAnimation.CAST);
        }

        Action attack;
        switch (selectedActionIndex)
        {
            case 0: attack = Action.PRIMARY;
                break;
            case 1: attack = Action.SECONDARY;
                break;
            case 2: attack = Action.TERTIARY;
                break;
            default: attack = Action.ULTIMATE;
                break;
        }


        ArrayList<Integer> affectedPlayers = new ArrayList<>();
        affectedPlayers.add(selectedTarget.getID());

        Packet packet = Packet.builder()
            .player(PlayerConverter.toPlayerState(localPlayer))
            .affectedPlayersId(affectedPlayers)
            .action(attack)
            .build();

        if (result.outcome == ActionResult.Outcome.HIT)
        {

            packet.setDeltaHealth(result.healingDone - result.damageDealt);


            if (selectedTarget.getStats().getHp() <= 0)
            {
                packet.getKilledPlayersId().add(selectedTarget.getID());
                selectedTarget.getAnimationController().playDeath();
            }
            else
            {
                selectedTarget.getAnimationController().playHurt(
                    localPlayer.getPosition(), selectedTarget.getPosition());
            }
        }
        else
        {
            packet.setDeltaHealth(result.healingDone - result.damageDealt);
        }

        networkClient.send(packet);

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
            .player(PlayerConverter.toPlayerState(localPlayer))
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
