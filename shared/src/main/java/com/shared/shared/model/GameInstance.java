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
    @Builder.Default
    private Pair<Integer, Integer>[] playerCoordinates = emptyCoordinates();

    @SuppressWarnings("unchecked")
    private static Pair<Integer, Integer>[] emptyCoordinates() {
        return (Pair<Integer, Integer>[]) new Pair<?, ?>[2];
    }
}
