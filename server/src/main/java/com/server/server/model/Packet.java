package com.server.server.model;

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
    private Pair<Integer, Integer> finalPosition;
    private Action action;

    Packet(int ID, Pair<Integer,Integer> finalPosition, Action action)
    {
        this.ID = ID;
        this.finalPosition = finalPosition;
        this.action = action;
    }
}
