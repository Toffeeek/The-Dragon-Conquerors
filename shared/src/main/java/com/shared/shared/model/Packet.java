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
    private String username;
    private Vector2 finalPosition;
    private Action action;

    Packet(int ID, Vector2 finalPosition, Action action)
    {
        this.ID = ID;
        this.finalPosition = finalPosition;
        this.action = action;
    }
}
