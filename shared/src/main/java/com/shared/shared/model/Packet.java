// File Location: shared/src/main/java/com/shared/shared/model/Packet.java
package com.shared.shared.model;

import com.badlogic.gdx.math.Vector2;
import com.shared.shared.model.world.Environment;
import com.shared.shared.model.ability.AbilityType;
import com.shared.shared.network.MatchState;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Packet
{
    @Builder.Default
    private int ID = -1;
    @Builder.Default
    private int activePlayerID = -1;
    private String username;
    private String roomId;
    private Vector2 finalPosition;
    private Action action;
    private CharacterClass characterClass;
    private Race race;
    private Environment environment;
    private int teamIndex;
    private int connectedPlayers;
    private int bogVotes;
    private int lavaVotes;
    private int canyonVotes;
    private String message;
    private AbilityType ability;
    @Builder.Default
    private int targetPlayerID = -1;
    private Vector2 targetPosition;
    private MatchState matchState;

    Packet(int ID, Vector2 finalPosition, Action action, int activePlayerID)
    {
        this.ID = ID;
        this.finalPosition = finalPosition;
        this.action = action;
        this.activePlayerID = activePlayerID;
    }
    Packet(int ID, CharacterClass characterClass, Vector2 finalPosition, Action action, int activePlayerID)
    {
        this.ID = ID;
        this.characterClass = characterClass;
        this.finalPosition = finalPosition;
        this.action = action;
        this.activePlayerID = activePlayerID;
    }
    Packet(int ID, Vector2 finalPosition, Action action)
    {
        this.ID = ID;
        this.finalPosition = finalPosition;
        this.action = action;
    }
}
