// File Location: core/src/main/java/com/github/thedragonconquerors/GameOneScreen.java
package com.github.thedragonconquerors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.shared.shared.model.world.BattlefieldArtwork;
import com.github.thedragonconquerors.assets.BattlefieldImageAssets;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.github.thedragonconquerors.input.BattleInteraction;
import com.client.client.NetworkClient;
import com.github.thedragonconquerors.assets.AssetService;
import com.github.thedragonconquerors.assets.SpriteAssets;
import com.github.thedragonconquerors.entities.Player;
import com.github.thedragonconquerors.input.MouseInputHandler;
import com.github.thedragonconquerors.movement.MovementSystem;
import com.github.thedragonconquerors.movement.NavGrid;
import com.github.thedragonconquerors.rendering.HudRenderer;
import com.github.thedragonconquerors.rendering.PlayerRenderer;
import com.github.thedragonconquerors.rendering.AbilityEffectsRenderer;
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
    private Texture battlefieldImage;
    private BattlefieldOverlayRenderer battlefieldOverlay;
    private MovementSystem movementSystem;
    private NavGrid navGrid;
    private Player localPlayer;
    private final ArrayList<Player> enemyPlayers = new ArrayList<>();
    private final Map<Integer, Player> playersById = new HashMap<>();
    private int localPlayerId;
    private boolean receivingInitialPlayerList = false;
    private boolean postMatchShown;
    private MatchState pendingPostMatch;
    private float postMatchDelay;
    private boolean receivedMatchState;
    private long lastSeenActionSequence;
    private AbilityEffectsRenderer abilityEffects;
    private com.github.thedragonconquerors.rendering.TacticalRenderer tactical;
    private com.github.thedragonconquerors.rendering.AmbientRenderer ambience;
    private com.github.thedragonconquerors.rendering.SurfaceAnimationRenderer surfaceAnimation;
    private com.github.thedragonconquerors.rendering.ImpactRenderer impacts;
    private com.github.thedragonconquerors.ui.BattleMenu battleMenu;

    private PlayerRenderer playerRenderer;
    private MouseInputHandler mouseInputHandler;
    private HudRenderer hudRenderer;
    private List<AbilityType> availableActions;
    private final BattleInteraction interaction = new BattleInteraction();
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
        this.camera = new OrthographicCamera();
        BattlefieldArtwork artwork = BattlefieldArtwork.forEnvironment(environment);
        this.viewport = new StretchViewport(artwork == null ? Main.WORLD_WIDTH : artwork.width,
            Main.WORLD_HEIGHT, camera);
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

        TiledMap map = null;
        BattlefieldImageAssets imageAsset = BattlefieldImageAssets.forEnvironment(environment);
        if (imageAsset != null) {
            battlefieldImage = assetService.load(imageAsset);
            battlefieldImage.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            surfaceAnimation=new com.github.thedragonconquerors.rendering.SurfaceAnimationRenderer(battlefield);
        } else {
            map = assetService.load(com.github.thedragonconquerors.assets.MapAssets.forEnvironment(environment));
            mapRenderer = new OrthogonalTiledMapRenderer(map, Main.UNIT_SCALE, batch);
        }
        navGrid = new NavGrid(map, Main.UNIT_SCALE, Main.WORLD_WIDTH, Main.WORLD_HEIGHT,
            battlefield);
        movementSystem.setNavGrid(navGrid);
        battlefieldOverlay = new BattlefieldOverlayRenderer(battlefield);

        for (SpriteAssets sprite : SpriteAssets.values())
            for (SpriteAssets.Clip clip : sprite.clips()) assetService.load(clip);

        playerRenderer = new PlayerRenderer(assetService, batch);
        abilityEffects = new AbilityEffectsRenderer(assetService, batch);
        tactical = new com.github.thedragonconquerors.rendering.TacticalRenderer();
        ambience = new com.github.thedragonconquerors.rendering.AmbientRenderer(battlefield);
        impacts = new com.github.thedragonconquerors.rendering.ImpactRenderer();
        hudRenderer = new HudRenderer(availableActions, interaction, this::chooseMove,
            this::chooseAction, this::selectAction, this::endTurn, this::cancelInteraction, assetService, this::openBattleMenu);
        battleMenu = new com.github.thedragonconquerors.ui.BattleMenu(game::returnToMenu,hudRenderer::restartGuide);
        mouseInputHandler = new MouseInputHandler(
            camera, viewport, localPlayer, movementSystem,
            this::handleWorldClick, this::sendLocalMove);
        Gdx.input.setInputProcessor(new InputMultiplexer(hudRenderer.stage(), mouseInputHandler));

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
        if (networkClient.isReady() && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if(battleMenu.isOpen())battleMenu.close();else openBattleMenu();
        }
        movementSystem.update(localPlayer, delta);
        for (Player enemy : enemyPlayers) movementSystem.update(enemy, delta);

        ScreenUtils.clear(Color.BLACK);
        BattlefieldArtwork activeArtwork=BattlefieldArtwork.forEnvironment(environment);
        camera.position.x=(activeArtwork==null?Main.WORLD_WIDTH/2:activeArtwork.x+activeArtwork.width/2)+impacts.shakeOffset();
        camera.update();
        viewport.apply();
        batch.setColor(Color.WHITE);
        if (battlefieldImage != null) {
            surfaceAnimation.render(batch,battlefieldImage,camera.combined,delta);
        } else {
            mapRenderer.setView(camera);
            mapRenderer.render();
        }
        battlefieldOverlay.render(camera.combined);
        ambience.render(camera.combined,delta);
        updateTacticalPreview();

        Player hovered = battleMenu.isOpen() || !networkClient.isReady() || hudRenderer.pointerOverUi() ? null
            : PlayerRenderer.hoveredPlayer(localPlayer,enemyPlayers,viewport.unproject(new Vector2(Gdx.input.getX(),Gdx.input.getY())));
        if(!battleMenu.isOpen() && networkClient.isReady()) {
            int portraitId=hudRenderer.hoveredPortraitId();
            if(portraitId>=0)hovered=portraitId==localPlayerId?localPlayer:playersById.get(portraitId);
        }
        playerRenderer.renderLocal(localPlayer, camera.combined, navGrid, delta,
            interaction.mode() == BattleInteraction.Mode.MOVE && interaction.canMove(localPlayer, anyPlayerMoving()),hovered==localPlayer);
        for (Player enemy : enemyPlayers) {
            playerRenderer.renderRemote(enemy, camera.combined, delta, localPlayer.getTeamIndex(),hovered==enemy);
        }

        abilityEffects.render(camera.combined, delta);
        impacts.render(camera.combined,delta);
        List<Player> visiblePlayers = new ArrayList<>(enemyPlayers); visiblePlayers.add(localPlayer);
        tactical.feedback(batch, camera.combined, visiblePlayers, delta);
        hudRenderer.render(localPlayer, delta, anyPlayerMoving());
        battleMenu.render(delta);
        if (pendingPostMatch != null) {
            battleMenu.close();
            postMatchDelay -= delta;
            if (postMatchDelay <= 0 && !anyPlayerMoving()) {
                MatchState finished = pendingPostMatch;
                pendingPostMatch = null;
                game.showPostMatch(teamIndex, chosenBuild, environment, localPlayerId, finished);
            }
        }
    }
    private void openBattleMenu() {
        if(!networkClient.isReady() || battleMenu==null)return;
        cancelInteraction();battleMenu.open();
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
            case MATCH_START:
                applyMatchState(packet.getMatchState());
                break;
            case ERROR:
                interaction.receivedResponse();
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
        if (interaction.mode() != BattleInteraction.Mode.MOVE || !interaction.canMove(localPlayer, anyPlayerMoving())) return;
        if (localPlayerId < 0 || !localPlayer.isActiveTurn()) {
            hudRenderer.showFeedback("Wait for your turn before moving.");
            return;
        }
        if (anyPlayerMoving()) {
            hudRenderer.showFeedback("Wait for the current movement animation to finish.");
            return;
        }
        interaction.sentCommand();
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

    private void chooseMove() {
        cancelTargetSelection(false);
        interaction.chooseMove(localPlayer, anyPlayerMoving());
    }

    private void chooseAction() {
        cancelTargetSelection(false);
        interaction.chooseAction(localPlayer, anyPlayerMoving());
    }

    private void cancelInteraction() {
        cancelTargetSelection(false);
        interaction.cancel();
    }

    private void selectAction(AbilityType ability) {
        if (interaction.mode() != BattleInteraction.Mode.ACTION
            || !interaction.canUse(localPlayer, anyPlayerMoving(), ability)) return;
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
        if (anyPlayerMoving()) {
            hudRenderer.showFeedback("Wait for movement to finish before using an ability.");
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
        if (!interaction.canControl(localPlayer, anyPlayerMoving())) return true;
        if (pendingTargetAction == null) {
            if (interaction.mode() != BattleInteraction.Mode.MOVE) return true;
            // Stay in Move mode when a destination has no safe route.
            if (movementSystem.previewDestination(localPlayer, clickedWorldPosition) == null) {
                hudRenderer.showFeedback("Choose a reachable blue area with a safe route.");
                return true;
            }
            return false;
        }

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
        float bestDistance = Float.MAX_VALUE;
        if (isLegalTarget(localPlayer, ability)) {
            float localDistance = localPlayer.getPosition().dst(clickedWorldPosition);
            if (com.github.thedragonconquerors.input.TacticalPreview.hitsBody(localPlayer.getPosition(), clickedWorldPosition) && localDistance <= bestDistance) {
                best = localPlayer;
                bestDistance = localDistance;
            }
        }
        for (Player enemy : enemyPlayers) {
            if (!isLegalTarget(enemy, ability)) continue;
            float distance = enemy.getPosition().dst(clickedWorldPosition);
            if (com.github.thedragonconquerors.input.TacticalPreview.hitsBody(enemy.getPosition(), clickedWorldPosition) && distance <= bestDistance) {
                best = enemy;
                bestDistance = distance;
            }
        }
        return best;
    }

    private void updateTacticalPreview() {
        AbilityType ability = pendingTargetAction != null ? pendingTargetAction : hudRenderer.hoveredAbility();
        if (ability == null || !networkClient.isReady() || battleMenu.isOpen()) { hudRenderer.setPreview(""); return; }
        Vector2 point = viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        boolean overUi = hudRenderer.pointerOverUi();
        Player target = overUi ? null : findClickedTarget(point, ability);
        boolean valid = ability.getTargetType().targetsGround()
            ? battlefield.isWalkable(point) && localPlayer.getPosition().dst(point) <= ability.getRange()
            : target != null && localPlayer.getPosition().dst(target.getPosition()) <= ability.getRange();
        if (valid && ability == AbilityType.TELEPORT) valid = enemyPlayers.stream().noneMatch(p -> p.isAlive()
            && p.getPosition().dst(point) < com.shared.shared.model.world.BattlefieldNavigation.PLAYER_SEPARATION);
        tactical.preview(camera.combined, localPlayer, ability, overUi ? null : point, target, valid);
        String text = com.github.thedragonconquerors.input.TacticalPreview.describe(localPlayer, ability, target);
        String unavailable = hudRenderer.unavailableReason(localPlayer,ability,anyPlayerMoving());
        if(!unavailable.isEmpty())text += "\n" + unavailable;
        if (!overUi && ability.getTargetType().targetsGround() && !valid) text += "\nBlocked, occupied, or out of range.";
        hudRenderer.setPreview(text);
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
        if (!interaction.canUse(localPlayer, anyPlayerMoving(), ability)) return;
        int targetId = target == null ? -1 : target.getId();
        interaction.sentCommand();
        networkClient.useAbility(localPlayerId, ability, targetId, point);
        hudRenderer.showFeedback("Waiting for server: " + ability.getDisplayName());
    }

    private void applyMatchState(MatchState state) {
        if (state == null || localPlayer == null) return;
        interaction.reset();
        cancelTargetSelection(false);

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
            long previousMovement = player.getMovementSequence();
            Vector2 authoritativePosition = playerState.getPosition();
            player.applyCombatState(playerState);
            if (authoritativePosition != null && previousMovement != playerState.getMovementSequence()) {
                if (previousMovement < 0 || playerState.getMovementPath() == null || playerState.getMovementPath().isEmpty()) {
                    player.getMovementController().stopMoving();
                    player.setPosition(authoritativePosition.x, authoritativePosition.y);
                } else {
                    player.getMovementController().setAuthoritativePath(player.getPosition(),
                        playerState.getMovementPath(), playerState.getRemainingMovement());
                }
            }
        }

        for (Player player : new ArrayList<>(enemyPlayers)) {
            if (!incoming.containsKey(player.getId())) removeEnemyPlayer(player.getId());
        }
        if (navGrid != null) {
            List<Vector2> occupied = state.getPlayers().stream()
                .filter(other -> other.getId() != localPlayerId && other.getHp() > 0)
                .map(PlayerCombatState::getPosition).filter(java.util.Objects::nonNull).toList();
            navGrid.setOccupied(occupied);
        }

        Player actor = state.getLastActorId() == localPlayerId
            ? localPlayer : playersById.get(state.getLastActorId());
        boolean newAction = receivedMatchState && state.getLastAbility() != null
            && state.getActionSequence() > lastSeenActionSequence;
        float impactDelay = 0;
        if (newAction && actor != null) {
            Vector2 origin = state.getLastActionOrigin() == null ? actor.getPosition() : state.getLastActionOrigin();
            Vector2 target = state.getLastTargetPoint() == null ? actor.getPosition() : state.getLastTargetPoint();
            actor.getAnimationController().playAbility(origin, target, state.getLastAbility());
            float duration = actor.getAnimationController().getClip().duration();
            impactDelay = duration * .65f;
            abilityEffects.play(state.getLastAbility(), origin, target, duration);
        }
        for (PlayerCombatState playerState : state.getPlayers()) {
            Player player = playerState.getId() == localPlayerId
                ? localPlayer : playersById.get(playerState.getId());
            int before = previousHp.getOrDefault(playerState.getId(), playerState.getHp());
            if (player == null) continue;
            if (before <= 0 && playerState.getHp() > 0) player.getAnimationController().revive();
            if (!receivedMatchState && playerState.getHp() <= 0) player.getAnimationController().playDeath();
            else if (receivedMatchState && playerState.getHp() < before) {
                player.getAnimationController().queueReaction(playerState.getHp() <= 0, impactDelay);
                tactical.add("-" + (before - playerState.getHp()), player.getPosition(), Color.SCARLET, impactDelay);
                playerRenderer.hitFlash(player.getId(),impactDelay);
                impacts.add(state.getLastActionOrigin(),player.getPosition(),newAction?state.getLastAbility():null,false,playerState.getHp()<=0,impactDelay);
            } else if (receivedMatchState && playerState.getHp() > before) {
                playerRenderer.healFlash(player.getId(),impactDelay);
                tactical.add("+" + (playerState.getHp() - before), player.getPosition(), Color.GREEN, impactDelay);
                impacts.add(null,player.getPosition(),newAction?state.getLastAbility():null,true,false,impactDelay);
            }
            if (newAction && state.getMissedTargetIds() != null && state.getMissedTargetIds().contains(playerState.getId()))
                tactical.add("MISS",player.getPosition(),Color.LIGHT_GRAY,impactDelay);
        }
        lastSeenActionSequence = Math.max(lastSeenActionSequence, state.getActionSequence());
        receivedMatchState = true;

        boolean localTurn = !state.isMatchOver() && state.getActivePlayerId() == localPlayerId;
        if (mouseInputHandler != null) mouseInputHandler.setLocalPlayerTurn(localTurn);
        if (!localTurn) cancelTargetSelection(false);
        if (hudRenderer != null) {
            hudRenderer.recordState(state);
        }
        if (state.isMatchOver() && hudRenderer != null) {
            String result = state.getWinningTeam() == 0 ? "Match ended in a draw."
                : state.getWinningTeam() == teamIndex ? "Your team wins!" : "Your team was defeated.";
            hudRenderer.showFeedback(result);
            if (!postMatchShown) {
                postMatchShown = true;
                pendingPostMatch = state;
                postMatchDelay = 1.5f;
            }
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
        if (!interaction.canControl(localPlayer, anyPlayerMoving())) return;
        cancelTargetSelection(false);
        if (!localPlayer.isActiveTurn()) {
            hudRenderer.showFeedback("It is not your turn.");
            return;
        }
        if (anyPlayerMoving()) {
            hudRenderer.showFeedback("Wait for movement to finish before ending the turn.");
            return;
        }
        interaction.sentCommand();
        networkClient.endTurn(localPlayerId);
    }

    private boolean anyPlayerMoving() {
        return !networkClient.isReady() || battleMenu!=null&&battleMenu.isOpen() || localPlayer.getMovementController().isMoving() || localPlayer.getAnimationController().isBusy()
            || (abilityEffects != null && abilityEffects.isBusy())
            || enemyPlayers.stream().anyMatch(player -> player.getMovementController().isMoving()
                || player.getAnimationController().isBusy());
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        BattlefieldArtwork artwork = BattlefieldArtwork.forEnvironment(environment);
        if (artwork != null) camera.position.x = artwork.x + artwork.width / 2f;
        camera.update();
        if (hudRenderer != null) hudRenderer.resize(width, height);
    }

    @Override
    public void hide() {
        if(battleMenu!=null)battleMenu.close();
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        if(battleMenu!=null)battleMenu.dispose();
        if(ambience!=null)ambience.dispose();
        if(impacts!=null)impacts.dispose();
        if (tactical != null) tactical.dispose();
        if (playerRenderer != null) playerRenderer.dispose();
        if (surfaceAnimation != null) surfaceAnimation.dispose();
        if (mapRenderer != null) mapRenderer.dispose();
        if (hudRenderer != null) hudRenderer.dispose();
        if (battlefieldOverlay != null) battlefieldOverlay.dispose();
    }
}
