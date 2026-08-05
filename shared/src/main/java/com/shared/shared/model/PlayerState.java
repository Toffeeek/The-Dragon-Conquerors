package com.shared.shared.model;

import com.badlogic.gdx.math.Vector2;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PlayerState {
    private int ID;
    private String username;
    private TEAM team = TEAM.UNASSIGNED;
    private CharacterClass characterClass;
    private Vector2 position;
    private boolean dead = false;

    @Builder
    public PlayerState(int ID, String username, TEAM team, CharacterClass characterClass, Vector2 position, boolean dead) {
        this.ID = ID;
        this.username = username;
        this.team = team == null ? TEAM.UNASSIGNED : team;
        this.characterClass = characterClass;
        this.position = position;
        this.dead = dead;
    }
}
