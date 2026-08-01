package com.shared.shared.model;

import com.badlogic.gdx.math.Vector2;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Packet
{
    private int ID = -1;
    private int activePlayerID = -1;
    private String username;
    private Vector2 finalPosition;
    private Action action;
    private CharacterClass characterClass;

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
