// File Location: server/src/main/java/com/server/server/combat/AuthoritativeMatch.java
package com.server.server.combat;

import com.badlogic.gdx.math.Vector2;
import com.server.server.selection.LobbyPlayer;
import com.shared.shared.model.ability.AbilityType;
import com.shared.shared.model.combat.AbilityOutcome;
import com.shared.shared.model.combat.AbilityResolver;
import com.shared.shared.model.combat.CombatContext;
import com.shared.shared.model.combat.Combatant;
import com.shared.shared.model.combat.TurnQueue;
import com.shared.shared.model.combat.TurnStartReport;
import com.shared.shared.model.world.Environment;
import com.shared.shared.model.world.BattlefieldDefinition;
import com.shared.shared.model.world.BattlefieldNavigation;
import com.shared.shared.network.MatchState;
import com.shared.shared.network.PlayerCombatState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Single authoritative match using the shared combat engine as its rulebook. */
public final class AuthoritativeMatch {
    private static final float MOVE_EPSILON = 0.01f;
    private static final float PLAYER_COLLISION_RADIUS = 0.65f;

    private final Map<Integer, ServerCombatant> players = new LinkedHashMap<>();
    private final CombatContext context;
    private final AbilityResolver resolver;
    private final BattlefieldDefinition battlefield;
    private final BattlefieldNavigation navigation;
    private final boolean testingMode;
    private final boolean singleTeamPractice;
    private int activePlayerId = -1;
    private boolean matchOver;
    private int winningTeam;
    private String lastMessage = "";
    private int lastActorId = -1;
    private AbilityType lastAbility;

    public AuthoritativeMatch(List<LobbyPlayer> lobbyPlayers, Environment environment) {
        this(lobbyPlayers, environment, new AbilityResolver(), true);
    }

    public AuthoritativeMatch(List<LobbyPlayer> lobbyPlayers, Environment environment,
                              boolean testingMode) {
        this(lobbyPlayers, environment, new AbilityResolver(), true, testingMode);
    }

    AuthoritativeMatch(List<LobbyPlayer> lobbyPlayers, Environment environment,
                       AbilityResolver resolver) {
        this(lobbyPlayers, environment, resolver, false);
    }

    AuthoritativeMatch(List<LobbyPlayer> lobbyPlayers, Environment environment,
                       AbilityResolver resolver, boolean assignBattlefieldSpawns) {
        this(lobbyPlayers, environment, resolver, assignBattlefieldSpawns, false);
    }

    private AuthoritativeMatch(List<LobbyPlayer> lobbyPlayers, Environment environment,
                               AbilityResolver resolver, boolean assignBattlefieldSpawns,
                               boolean testingMode) {
        if (lobbyPlayers == null || lobbyPlayers.isEmpty()) {
            throw new IllegalArgumentException("A match needs players");
        }
        this.battlefield = BattlefieldDefinition.forEnvironment(environment);
        this.navigation = new BattlefieldNavigation(battlefield);
        this.testingMode = testingMode;
        this.singleTeamPractice = testingMode && lobbyPlayers.stream()
            .map(LobbyPlayer::getTeamIndex).distinct().count() == 1;
        Map<Integer, Integer> teamSlots = new LinkedHashMap<>();
        for (LobbyPlayer player : lobbyPlayers) {
            int slot = teamSlots.getOrDefault(player.getTeamIndex(), 0);
            teamSlots.put(player.getTeamIndex(), slot + 1);
            Vector2 spawn = assignBattlefieldSpawns
                ? battlefield.spawnFor(player.getTeamIndex(), slot) : player.getPosition();
            ServerCombatant combatant = new ServerCombatant(player, spawn);
            players.put(combatant.getId(), combatant);
        }
        this.context = new CombatContext(players.values(), environment);
        this.resolver = resolver;
        advanceTurn("Match begins");
    }

    public CombatCommandResult move(int playerId, Vector2 destination) {
        ServerCombatant actor = activeActor(playerId);
        if (actor == null) return wrongTurn(playerId);
        if (!battlefield.isWalkable(destination)) {
            return CombatCommandResult.rejected("Destination is blocked or unsafe.");
        }
        if (actor.getPosition().dst(destination) <= MOVE_EPSILON) return CombatCommandResult.rejected("Choose a different destination.");
        for (ServerCombatant other : players.values()) {
            if (other != actor && other.isAlive()
                && other.getPosition().dst(destination) < PLAYER_COLLISION_RADIUS) {
                return CombatCommandResult.rejected("Another player occupies that tile.");
            }
        }

        List<Vector2> occupied = players.values().stream().filter(other -> other != actor && other.isAlive())
            .map(ServerCombatant::getPosition).toList();
        List<Vector2> path = navigation.findPath(actor.getPosition(), destination, actor.getRemainingMovement(), occupied);
        if (path.isEmpty()) return CombatCommandResult.rejected("No movement points or no safe route around terrain and players.");
        float distance = BattlefieldNavigation.length(actor.getPosition(), path);
        actor.moveAlong(path);
        lastActorId = playerId;
        lastAbility = null;
        lastMessage = actor.getUsername() + " moves " + String.format("%.1f", distance) + " units.";
        autoEndTurn(actor);
        return CombatCommandResult.accepted(snapshot());
    }

    public CombatCommandResult useAbility(int playerId, AbilityType ability,
                                          int targetPlayerId, Vector2 targetPoint) {
        ServerCombatant actor = activeActor(playerId);
        if (actor == null) return wrongTurn(playerId);
        if (actor.isActionUsed()) {
            return CombatCommandResult.rejected("Your action for this turn is already spent.");
        }
        if (ability == null) return CombatCommandResult.rejected("Choose an ability.");
        if (ability.getTargetType().targetsGround() && !battlefield.isWalkable(targetPoint)) {
            return CombatCommandResult.rejected("Target tile is blocked or unsafe.");
        }
        if (ability == AbilityType.TELEPORT && players.values().stream().anyMatch(other -> other != actor
            && other.isAlive() && other.getPosition().dst(targetPoint) < PLAYER_COLLISION_RADIUS)) {
            return CombatCommandResult.rejected("Another player occupies that tile.");
        }

        ServerCombatant target = players.get(targetPlayerId);
        Map<Integer, Vector2> positionsBefore = positions();
        AbilityOutcome outcome = resolver.resolve(actor, ability, target, targetPoint, context);
        if (!outcome.isLegal()) {
            return CombatCommandResult.rejected(outcome.getRejection().getMessage());
        }

        actor.markActionUsed();
        lastActorId = playerId;
        lastAbility = ability;
        lastMessage = outcome.describe();
        resolvePushes(outcome, positionsBefore);
        for (ServerCombatant player : players.values()) {
            if (!player.getPosition().epsilonEquals(positionsBefore.get(player.getId()), 0.001f)) player.markDisplaced();
        }
        if (!finishIfOver()) autoEndTurn(actor);
        return CombatCommandResult.accepted(snapshot());
    }

    public CombatCommandResult endTurn(int playerId) {
        ServerCombatant actor = activeActor(playerId);
        if (actor == null) return wrongTurn(playerId);
        lastActorId = playerId;
        lastAbility = null;
        advanceTurn(actor.getUsername() + " ends the turn");
        return CombatCommandResult.accepted(snapshot());
    }

    public MatchState disconnect(int playerId) {
        ServerCombatant removed = players.remove(playerId);
        if (removed == null) return snapshot();
        boolean removedCurrent = playerId == activePlayerId;
        context.getTurnQueue().remove(removed);
        lastMessage = removed.getUsername() + " disconnected.";
        finishIfOver();
        if (!matchOver && removedCurrent) advanceTurn(lastMessage);
        return snapshot();
    }

    public MatchState snapshot() {
        List<PlayerCombatState> states = new ArrayList<>();
        for (ServerCombatant player : players.values()) {
            states.add(player.snapshot(context, player.getId() == activePlayerId));
        }
        return MatchState.builder()
            .players(states)
            .activePlayerId(activePlayerId)
            .roundNumber(context.getTurnQueue().getRoundNumber())
            .environment(context.getEnvironment())
            .matchOver(matchOver)
            .testingMode(testingMode)
            .winningTeam(winningTeam)
            .message(lastMessage)
            .lastActorId(lastActorId)
            .lastAbility(lastAbility)
            .build();
    }

    public boolean contains(int playerId) { return players.containsKey(playerId); }
    public int getActivePlayerId() { return activePlayerId; }

    private void advanceTurn(String prefix) {
        StringBuilder message = new StringBuilder(prefix == null ? "" : prefix);
        TurnQueue queue = context.getTurnQueue();

        for (int attempts = 0; attempts < Math.max(1, queue.size() * 2); attempts++) {
            if (finishIfOver()) return;
            Combatant next = queue.advance();
            if (next == null) {
                matchOver = true;
                activePlayerId = -1;
                winningTeam = TurnQueue.NO_WINNER;
                append(message, "No combatant can continue");
                lastMessage = message.toString();
                return;
            }

            TurnStartReport report = context.beginTurn(next,
                battlefield.isHazard(next.getPosition()));
            if (!report.isUneventful()) append(message, next.getUsername() + ": " + report.describe());
            if (isBattleFinished()) {
                lastMessage = message.toString();
                finishIfOver();
                return;
            }
            if (report.isTurnSkipped()) continue;

            activePlayerId = next.getId();
            append(message, next.getUsername() + "'s turn");
            lastMessage = message.toString();
            return;
        }

        matchOver = true;
        activePlayerId = -1;
        lastMessage = "No eligible combatant could take a turn.";
    }

    private void autoEndTurn(ServerCombatant actor) {
        if (!matchOver && actor.resourcesExhausted()) {
            advanceTurn(lastMessage + " Action and movement points spent; turn ends automatically.");
        }
    }

    private boolean finishIfOver() {
        if (matchOver) return true;
        if (!isBattleFinished()) return false;
        matchOver = true;
        activePlayerId = -1;
        winningTeam = context.getTurnQueue().winningTeam();
        String ending = winningTeam == TurnQueue.NO_WINNER
            ? "The match ends in a draw." : "Team " + winningTeam + " wins.";
        lastMessage = lastMessage == null || lastMessage.isBlank()
            ? ending : lastMessage + " " + ending;
        return true;
    }

    private boolean isBattleFinished() {
        // A practice session with no opponents must not immediately declare victory.
        // Sessions that started with both teams retain normal victory/disconnect rules.
        if (singleTeamPractice) return players.values().stream().noneMatch(Combatant::isAlive);
        return context.getTurnQueue().isMatchOver();
    }

    private ServerCombatant activeActor(int playerId) {
        if (matchOver || playerId != activePlayerId) return null;
        return players.get(playerId);
    }

    private CombatCommandResult wrongTurn(int playerId) {
        if (!players.containsKey(playerId)) return CombatCommandResult.rejected("Unknown player.");
        if (matchOver) return CombatCommandResult.rejected("The match is over.");
        return CombatCommandResult.rejected("It is not your turn.");
    }

    private Map<Integer, Vector2> positions() {
        Map<Integer, Vector2> copy = new LinkedHashMap<>();
        for (ServerCombatant player : players.values()) {
            copy.put(player.getId(), new Vector2(player.getPosition()));
        }
        return copy;
    }

    private void resolvePushes(AbilityOutcome outcome, Map<Integer, Vector2> positionsBefore) {
        for (AbilityOutcome.TargetResult targetResult : outcome.getTargets()) {
            if (targetResult.getPushedTo() == null) continue;
            ServerCombatant target = players.get(targetResult.getTargetId());
            Vector2 start = positionsBefore.get(targetResult.getTargetId());
            if (target == null || start == null) continue;

            Vector2 destination = target.getPosition();
            if (battlefield.getEnvironment() == Environment.CANYON
                && battlefield.pathCrossesLethalFall(start, destination)) {
                target.getStats().setHp(0);
                target.getPosition().set(battlefield.lastWalkablePoint(start, destination));
                lastMessage += " " + target.getUsername() + " is pushed into the canyon.";
            } else if (!battlefield.pathIsWalkable(start, destination)) {
                target.getPosition().set(battlefield.lastWalkablePoint(start, destination));
                lastMessage += " " + target.getUsername() + " is stopped by terrain.";
            }
        }
    }

    private void append(StringBuilder builder, String fragment) {
        if (fragment == null || fragment.isBlank()) return;
        if (builder.length() > 0) builder.append("; ");
        builder.append(fragment);
    }
}
