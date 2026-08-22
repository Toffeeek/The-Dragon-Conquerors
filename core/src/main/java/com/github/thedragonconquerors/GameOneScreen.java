// File Location: core/src/main/java/com/github/thedragonconquerors/GameOneScreen.java
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
import com.github.thedragonconquerors.entities.Player;
import com.github.thedragonconquerors.input.MouseInputHandler;
import com.github.thedragonconquerors.movement.MovementSystem;
import com.github.thedragonconquerors.movement.NavGrid;
import com.github.thedragonconquerors.rendering.HudRenderer;
import com.github.thedragonconquerors.rendering.PlayerRenderer;
import com.github.thedragonconquerors.rendering.BattlefieldOverlayRenderer;
import com.shared.shared.model.Action;
import com.shared.shared.model.CharacterBuild;
import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.Packet;
import com.shared.shared.model.Race;
import com.shared.shared.model.ability.AbilityType;
import com.shared.shared.model.ability.TargetType;
import com.shared.shared.model.world.Environment;
import com.shared.shared.model.world.BattlefieldDefinition;
import com.shared.shared.network.MatchState;
import com.shared.shared.network.PlayerCombatState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameOneScreen extends ScreenAdapter {

    /**
     * Class assumed for a remote player whose join packet carried no class.
     *
     * <p>Should not happen — the lobby always sends one — but a null class would
     * NPE inside {@link Player}'s stat setup, so a mid-match join is rendered as
     * a Paladin instead of dropping the player. If this ever fires it means the
     * join packet lost its class in transit, which is worth investigating.</p>
     */
    private static final CharacterClass DEFAULT_REMOTE_CLASS = CharacterClass.PALADIN;

    private final Main game;
    private final Batch batch;
    private final AssetService assetService;
    private final Viewport viewport;
    private final OrthographicCamera camera;
    private final NetworkClient networkClient;
    private final int teamIndex;
    private final CharacterBuild chosenBuild;
    private final Environment environment;
    private final List<Packet> initialRoster;
    private final MatchState initialMatchState;
    private final BattlefieldDefinition battlefield;

    private OrthogonalTiledMapRenderer mapRenderer;
    private BattlefieldOverlayRenderer battlefieldOverlay;
    private MovementSystem movementSystem;
    private NavGrid navGrid;
    private Player localPlayer;
    private final ArrayList<Player> enemyPlayers = new ArrayList<>();
    private final Map<Integer, Player> playersById = new HashMap<>();
    private int localPlayerId;
    private boolean receivingInitialPlayerList = false;

    private PlayerRenderer playerRenderer;
    private MouseInputHandler mouseInputHandler;
    private HudRenderer hudRenderer;
    private List<AbilityType> availableActions;
    private int selectedActionIndex = 0;
    private AbilityType pendingTargetAction;

    public GameOneScreen(Main game, int teamIndex, CharacterBuild chosenBuild,
                         Environment environment, int localPlayerId,
                         List<Packet> initialRoster, MatchState initialMatchState) {
        this.networkClient = game.getNetworkClient();
        this.teamIndex = teamIndex;
        this.chosenBuild = chosenBuild;
        this.environment = environment;
        this.localPlayerId = localPlayerId;
        this.initialRoster = initialRoster == null
            ? new ArrayList<>() : new ArrayList<>(initialRoster);
        this.initialMatchState = initialMatchState;
        this.battlefield = BattlefieldDefinition.forEnvironment(environment);
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

        Packet localPacket = findInitialPlayer(localPlayerId);
        float spawnX = teamIndex == 1 ? 1f : 28f;
        Vector2 spawn = localPacket != null && localPacket.getFinalPosition() != null
            ? new Vector2(localPacket.getFinalPosition()) : new Vector2(spawnX, 9f);
        String username = localPacket != null && localPacket.getUsername() != null
            ? localPacket.getUsername() : "Player";
        spawnLocalPlayer(username, spawn, chosenBuild);
        availableActions = AbilityType.forClass(chosenBuild.getCharacterClass());

        TiledMap map = assetService.load(
            com.github.thedragonconquerors.assets.MapAssets.forEnvironment(environment));
        mapRenderer = new OrthogonalTiledMapRenderer(map, Main.UNIT_SCALE, batch);
        navGrid = new NavGrid(map, Main.UNIT_SCALE, Main.WORLD_WIDTH, Main.WORLD_HEIGHT,
            battlefield);
        movementSystem.setNavGrid(navGrid);
        battlefieldOverlay = new BattlefieldOverlayRenderer(battlefield);

        for (SpriteAssets sprite : SpriteAssets.values()) {
            try {
                assetService.load(sprite);
            } catch (Exception exception) {
                System.out.println("Sprite sheet could not be loaded: " + sprite.name()
                    + " (" + exception.getMessage() + ")");
            }
        }

        playerRenderer = new PlayerRenderer(assetService, batch);
        hudRenderer = new HudRenderer(viewport);
        hudRenderer.setJoinUrl(game.getHostedJoinUrl());
        hudRenderer.setEnvironment(environment);
        mouseInputHandler = new MouseInputHandler(
            camera, viewport, localPlayer, movementSystem,
            this::handleWorldClick, this::sendLocalMove);
        Gdx.input.setInputProcessor(mouseInputHandler);

        for (Packet packet : initialRoster) {
            if (packet.getID() != localPlayerId) receiveExistingPlayer(packet);
        }
        receivingInitialPlayerList = false;
        applyMatchState(initialMatchState);
    }

    private void spawnLocalPlayer(String username, Vector2 startingPosition,
                                  CharacterBuild build) {
        localPlayer = new Player(localPlayerId, username, startingPosition, build, teamIndex);
    }

    private Packet findInitialPlayer(int playerId) {
        for (Packet packet : initialRoster) {
            if (packet.getID() == playerId) return packet;
        }
        return null;
    }

    @Override
    public void render(float delta) {
        movementSystem.update(localPlayer, delta);
        for (Player enemy : enemyPlayers) movementSystem.update(enemy, delta);

        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) endTurn();
        handleActionKeys();

        ScreenUtils.clear(Color.BLACK);
        viewport.apply();
        batch.setColor(Color.WHITE);
        mapRenderer.setView(camera);
        mapRenderer.render();
        battlefieldOverlay.render(camera.combined);

        playerRenderer.renderLocal(localPlayer, camera.combined, navGrid, delta);
        for (Player enemy : enemyPlayers) {
            boolean selecting = pendingTargetAction != null && isLegalTarget(enemy, pendingTargetAction);
            boolean inRange = selecting && localPlayer.getPosition().dst(enemy.getPosition())
                <= pendingTargetAction.getRange();
            playerRenderer.renderEnemy(enemy, camera.combined, delta, selecting, inRange);
        }

        hudRenderer.render(localPlayer, delta);
    }

    private void handlePacket(Packet packet) {
        switch (packet.getAction()) {
            case PRIVATE_JOIN_CONFIRMATION:
                localPlayerId = packet.getID();
                System.out.println("My player ID is " + localPlayerId);
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
            case LEAVE:
                removeEnemyPlayer(packet.getID());
                break;
            case MATCH_STATE:
                applyMatchState(packet.getMatchState());
                break;
            case ERROR:
                if (hudRenderer != null) {
                    hudRenderer.showFeedback(packet.getMessage() == null
                        ? "The server rejected that command." : packet.getMessage());
                }
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
        Player player = createRemotePlayer(packet, position);
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
        if (localPlayerId < 0 || !localPlayer.isActiveTurn()) {
            hudRenderer.showFeedback("Wait for your turn before moving.");
            return;
        }
        if (localPlayer.getMovementController().isMoving()) {
            hudRenderer.showFeedback("Finish the current move first.");
            return;
        }
        networkClient.move(localPlayerId, targetPosition);
    }

    private void receiveExistingPlayer(Packet packet) {
        Vector2 position = packet.getFinalPosition();
        if (position == null || packet.getID() == localPlayerId
            || playersById.containsKey(packet.getID())) return;

        Player existingPlayer = createRemotePlayer(packet, position);
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
            && packet.getCharacterClass() == chosenBuild.getCharacterClass();
    }

    private Player createRemotePlayer(Packet packet, Vector2 position) {
        CharacterClass characterClass = packet.getCharacterClass() == null
            ? DEFAULT_REMOTE_CLASS : packet.getCharacterClass();
        Race race = packet.getRace() == null ? CharacterBuild.DEFAULT_RACE : packet.getRace();
        return new Player(packet.getID(), packet.getUsername(), new Vector2(position),
            CharacterBuild.of(race, characterClass), packet.getTeamIndex());
    }

    private void removeEnemyPlayer(int id) {
        Player enemy = playersById.remove(id);
        if (enemy != null) enemyPlayers.remove(enemy);
        if (enemyPlayers.isEmpty()) cancelTargetSelection(false);
    }

    private void handleActionKeys() {
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

        for (int i = 0; i < keys.length && i < availableActions.size(); i++) {
            if (!Gdx.input.isKeyJustPressed(keys[i])) continue;
            selectedActionIndex = i;
            hudRenderer.setSelectedActionIndex(i);
            selectAction(availableActions.get(i));
            return;
        }
    }

    private void selectAction(AbilityType ability) {
        if (!localPlayer.isActiveTurn()) {
            cancelTargetSelection(false);
            hudRenderer.showFeedback("It is not your turn.");
            return;
        }
        if (!localPlayer.isAlive()) {
            cancelTargetSelection(false);
            hudRenderer.showFeedback("A defeated player cannot act.");
            return;
        }
        if (localPlayer.getMovementController().isMoving()) {
            hudRenderer.showFeedback("Finish moving before using an ability.");
            return;
        }
        if (localPlayer.isActionUsed()) {
            cancelTargetSelection(false);
            hudRenderer.showFeedback("Your action is spent. You may still move or end the turn.");
            return;
        }
        if (localPlayer.cooldownTurns(ability) > 0) {
            cancelTargetSelection(false);
            hudRenderer.showFeedback(ability.getDisplayName() + " is still recharging.");
            return;
        }
        if (localPlayer.getStats().getMana() < ability.getManaCost()) {
            cancelTargetSelection(false);
            hudRenderer.showFeedback("Not enough mana for " + ability.getDisplayName() + ".");
            return;
        }
        if (ability.getTargetType() == TargetType.SELF) {
            cancelTargetSelection(false);
            sendAbility(ability, localPlayer, null);
            return;
        }
        if (!ability.getTargetType().targetsGround() && !hasAnyLegalTarget(ability)) {
            cancelTargetSelection(false);
            hudRenderer.showFeedback("No legal target is available for "
                + ability.getDisplayName() + ".");
            return;
        }
        pendingTargetAction = ability;
        hudRenderer.showTargetingPrompt(ability);
        hudRenderer.showFeedback(ability.getTargetType().targetsGround()
            ? "Click a tile on the battlefield." : "Click a highlighted player.");
    }

    /** Returns true while targeting so the click is not also interpreted as movement. */
    private boolean handleWorldClick(Vector2 clickedWorldPosition) {
        if (pendingTargetAction == null) return false;

        AbilityType ability = pendingTargetAction;
        if (ability.getTargetType().targetsGround()) {
            float distance = localPlayer.getPosition().dst(clickedWorldPosition);
            if (distance > ability.getRange()) {
                hudRenderer.showFeedback("Target is out of range ("
                    + String.format("%.1f", distance) + "/"
                    + String.format("%.1f", ability.getRange()) + ").");
                return true;
            }
            cancelTargetSelection(false);
            sendAbility(ability, null, clickedWorldPosition);
            return true;
        }

        Player selectedTarget = findClickedTarget(clickedWorldPosition, ability);
        if (selectedTarget == null) {
            hudRenderer.showFeedback("Click directly on a highlighted player.");
            return true;
        }
        float targetDistance = localPlayer.getPosition().dst(selectedTarget.getPosition());
        if (targetDistance > ability.getRange()) {
            hudRenderer.showFeedback("Target is out of range ("
                + String.format("%.1f", targetDistance) + "/"
                + String.format("%.1f", ability.getRange()) + ").");
            return true;
        }
        cancelTargetSelection(false);
        sendAbility(ability, selectedTarget, null);
        return true;
    }

    private Player findClickedTarget(Vector2 clickedWorldPosition, AbilityType ability) {
        Player best = null;
        float bestDistance = PlayerRenderer.TARGET_CLICK_RADIUS;
        if (isLegalTarget(localPlayer, ability)) {
            float localDistance = localPlayer.getPosition().dst(clickedWorldPosition);
            if (localDistance <= bestDistance) {
                best = localPlayer;
                bestDistance = localDistance;
            }
        }
        for (Player enemy : enemyPlayers) {
            if (!isLegalTarget(enemy, ability)) continue;
            float distance = enemy.getPosition().dst(clickedWorldPosition);
            if (distance <= bestDistance) {
                best = enemy;
                bestDistance = distance;
            }
        }
        return best;
    }

    private boolean hasAnyLegalTarget(AbilityType ability) {
        if (isLegalTarget(localPlayer, ability)) return true;
        for (Player player : enemyPlayers) {
            if (isLegalTarget(player, ability)) return true;
        }
        return false;
    }

    private boolean isLegalTarget(Player target, AbilityType ability) {
        if (target == null || ability == null) return false;
        switch (ability.getTargetType()) {
            case ALLY:
                return target.getTeamIndex() == localPlayer.getTeamIndex() && target.isAlive();
            case DOWNED_ALLY:
                return target.getTeamIndex() == localPlayer.getTeamIndex() && target.isDowned();
            case ENEMY:
                return target.getTeamIndex() != localPlayer.getTeamIndex() && target.isAlive();
            default:
                return false;
        }
    }

    private void sendAbility(AbilityType ability, Player target, Vector2 point) {
        int targetId = target == null ? -1 : target.getId();
        networkClient.useAbility(localPlayerId, ability, targetId, point);
        hudRenderer.showFeedback("Waiting for server: " + ability.getDisplayName());
    }

    private void applyMatchState(MatchState state) {
        if (state == null || localPlayer == null) return;

        Map<Integer, PlayerCombatState> incoming = new HashMap<>();
        Map<Integer, Integer> previousHp = new HashMap<>();
        for (PlayerCombatState playerState : state.getPlayers()) {
            incoming.put(playerState.getId(), playerState);
            Player player = playerState.getId() == localPlayerId
                ? localPlayer : playersById.get(playerState.getId());
            if (player == null) {
                player = createRemotePlayer(playerState);
                enemyPlayers.add(player);
                playersById.put(player.getId(), player);
            }

            previousHp.put(player.getId(), player.getStats().getHp());
            Vector2 authoritativePosition = playerState.getPosition();
            player.applyCombatState(playerState);
            if (authoritativePosition != null
                && !player.getPosition().epsilonEquals(authoritativePosition, 0.02f)) {
                movementSystem.setAuthoritativeDestination(player,
                    new Vector2(authoritativePosition), playerState.getRemainingMovement());
            }
        }

        for (Player player : new ArrayList<>(enemyPlayers)) {
            if (!incoming.containsKey(player.getId())) removeEnemyPlayer(player.getId());
        }

        Player actor = state.getLastActorId() == localPlayerId
            ? localPlayer : playersById.get(state.getLastActorId());
        Player firstDamaged = null;
        for (PlayerCombatState playerState : state.getPlayers()) {
            Player player = playerState.getId() == localPlayerId
                ? localPlayer : playersById.get(playerState.getId());
            int before = previousHp.getOrDefault(playerState.getId(), playerState.getHp());
            if (player != null && playerState.getHp() < before) {
                if (firstDamaged == null) firstDamaged = player;
                if (playerState.getHp() <= 0) player.getAnimationController().playDeath();
                else if (actor != null) player.getAnimationController().playHurt(
                    actor.getPosition(), player.getPosition());
            }
        }

        if (actor != null && state.getLastAbility() != null) {
            Vector2 target = firstDamaged == null
                ? new Vector2(actor.getPosition()).add(0f, 1f) : firstDamaged.getPosition();
            actor.getAnimationController().playAttack(actor.getPosition(), target,
                state.getLastAbility().getManaCost() > 0);
        }

        boolean localTurn = !state.isMatchOver() && state.getActivePlayerId() == localPlayerId;
        if (mouseInputHandler != null) mouseInputHandler.setLocalPlayerTurn(localTurn);
        if (!localTurn) cancelTargetSelection(false);
        if (hudRenderer != null && state.getMessage() != null && !state.getMessage().isBlank()) {
            hudRenderer.showFeedback(state.getMessage());
        }
        if (state.isMatchOver() && hudRenderer != null) {
            String result = state.getWinningTeam() == 0 ? "Match ended in a draw."
                : state.getWinningTeam() == teamIndex ? "Your team wins!" : "Your team was defeated.";
            hudRenderer.showFeedback(result);
        }
    }

    private Player createRemotePlayer(PlayerCombatState state) {
        CharacterClass characterClass = state.getCharacterClass() == null
            ? DEFAULT_REMOTE_CLASS : state.getCharacterClass();
        Race race = state.getRace() == null ? CharacterBuild.DEFAULT_RACE : state.getRace();
        Vector2 position = state.getPosition() == null ? new Vector2() : state.getPosition();
        return new Player(state.getId(), state.getUsername(), new Vector2(position),
            CharacterBuild.of(race, characterClass), state.getTeamIndex());
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
        if (!localPlayer.isActiveTurn()) {
            hudRenderer.showFeedback("It is not your turn.");
            return;
        }
        if (localPlayer.getMovementController().isMoving()) {
            hudRenderer.showFeedback("Finish moving before ending the turn.");
            return;
        }
        networkClient.endTurn(localPlayerId);
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
        if (battlefieldOverlay != null) battlefieldOverlay.dispose();
    }
}
