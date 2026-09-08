// File Location: server/src/main/java/com/server/server/selection/LobbyPlayer.java
package com.server.server.selection;

import com.badlogic.gdx.math.Vector2;
import com.shared.shared.model.Action;
import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.Packet;
import com.shared.shared.model.Race;

/** Immutable lobby identity plus the mutable authoritative position. */
public final class LobbyPlayer {
    private final int id;
    private final String sessionId;
    private final String username;
    private final CharacterClass characterClass;
    private final Race race;
    private final int teamIndex;
    private final Vector2 position;

    public LobbyPlayer(int id, String sessionId, String username,
                       CharacterClass characterClass, Race race, int teamIndex,
                       Vector2 position) {
        this.id = id;
        this.sessionId = sessionId;
        this.username = username;
        this.characterClass = characterClass;
        this.race = race;
        this.teamIndex = teamIndex;
        this.position = new Vector2(position);
    }

    public int getId() { return id; }
    public String getSessionId() { return sessionId; }
    public String getUsername() { return username; }
    public CharacterClass getCharacterClass() { return characterClass; }
    public Race getRace() { return race; }
    public int getTeamIndex() { return teamIndex; }
    public Vector2 getPosition() { return new Vector2(position); }

    public void setPosition(Vector2 nextPosition) {
        if (nextPosition != null) position.set(nextPosition);
    }

    public Packet toPacket(Action action, int connectedPlayers) {
        return Packet.builder()
            .ID(id)
            .username(username)
            .characterClass(characterClass)
            .race(race)
            .teamIndex(teamIndex)
            .finalPosition(getPosition())
            .connectedPlayers(connectedPlayers)
            .action(action)
            .build();
    }
}
