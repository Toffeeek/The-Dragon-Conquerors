package com.shared.shared.model;


import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GameInstance
{
    private final int mapSize = 10;
    private Pair<Integer, Integer>[] playerCoordinates = new Pair[2];
}
