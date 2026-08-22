// File Location: server/src/main/java/com/server/server/selection/LobbyStateService.java
package com.server.server.selection;

import com.badlogic.gdx.math.Vector2;
import com.shared.shared.model.Packet;
import com.shared.shared.model.world.Environment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Thread-safe state for the current four-player selection lobby. */
public class LobbyStateService {
    public static final int MAX_PLAYERS = 4;
    public static final int TEAM_SIZE = 2;

    private final EnvironmentVoteResolver voteResolver;
    private final Map<Integer, LobbyPlayer> players = new LinkedHashMap<>();
    private final Map<Integer, Environment> votes = new HashMap<>();
    private int nextPlayerId;
    private boolean matchStarted;

    public LobbyStateService(EnvironmentVoteResolver voteResolver) {
        this.voteResolver = voteResolver;
    }

    public synchronized String validateJoin(Packet packet) {
        String selectionError = validateSelection(packet);
        if (selectionError != null) return selectionError;
        if (matchStarted) return "A match is already in progress.";
        if (players.size() >= MAX_PLAYERS) return "The lobby is full.";
        if (teamCount(packet.getTeamIndex()) >= TEAM_SIZE) {
            return packet.getTeamIndex() == 1 ? "Azure team is full." : "Crimson team is full.";
        }
        return null;
    }

    public static String validateSelection(Packet packet) {
        if (packet == null || packet.getCharacterClass() == null || packet.getRace() == null) {
            return "Choose both a class and race before joining.";
        }
        if (packet.getTeamIndex() != 1 && packet.getTeamIndex() != 2) {
            return "Choose Azure or Crimson team before joining.";
        }
        return null;
    }

    public synchronized LobbyPlayer addPlayer(Packet packet, String sessionId) {
        String validationError = validateJoin(packet);
        if (validationError != null) throw new IllegalArgumentException(validationError);

        int teamIndex = packet.getTeamIndex();
        int slot = teamCount(teamIndex);
        Vector2 spawn = new Vector2(teamIndex == 1 ? 1f : 28f, slot == 0 ? 6f : 11f);
        String username = packet.getUsername() == null || packet.getUsername().isBlank()
            ? "Player " + (nextPlayerId + 1) : packet.getUsername().trim();

        LobbyPlayer player = new LobbyPlayer(nextPlayerId++, sessionId, username,
            packet.getCharacterClass(), packet.getRace(), teamIndex, spawn);
        players.put(player.getId(), player);
        return player;
    }

    public synchronized List<LobbyPlayer> players() {
        return Collections.unmodifiableList(new ArrayList<>(players.values()));
    }

    public synchronized boolean contains(int playerId) {
        return players.containsKey(playerId);
    }

    public synchronized int size() {
        return players.size();
    }

    public synchronized void updatePosition(int playerId, Vector2 position) {
        LobbyPlayer player = players.get(playerId);
        if (player != null) player.setPosition(position);
    }

    public synchronized void recordVote(int playerId, Environment environment) {
        if (!players.containsKey(playerId)) throw new IllegalArgumentException("Unknown player.");
        if (environment == null) throw new IllegalArgumentException("Choose an environment.");
        votes.put(playerId, environment);
    }

    public synchronized Map<Environment, Integer> voteCounts() {
        return voteResolver.count(votes.values());
    }

    public synchronized Optional<Environment> startIfReady() {
        if (matchStarted || players.size() != MAX_PLAYERS || votes.size() != players.size()) {
            return Optional.empty();
        }
        matchStarted = true;
        return Optional.of(voteResolver.resolve(votes.values()));
    }

    public synchronized LobbyPlayer remove(int playerId) {
        votes.remove(playerId);
        return players.remove(playerId);
    }

    private int teamCount(int teamIndex) {
        int count = 0;
        for (LobbyPlayer player : players.values()) {
            if (player.getTeamIndex() == teamIndex) count++;
        }
        return count;
    }
}
