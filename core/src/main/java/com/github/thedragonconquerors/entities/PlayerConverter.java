package com.github.thedragonconquerors.entities;

import com.badlogic.gdx.math.Vector2;
import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.CharacterBuild;
import com.shared.shared.model.PlayerState;
import com.shared.shared.model.TEAM;

public final class PlayerConverter {
    private PlayerConverter() {}

    public static Player toPlayer(PlayerState playerState) {
        if (playerState == null) return null;

        CharacterClass characterClass = playerState.getCharacterClass() == null
            ? CharacterClass.PALADIN
            : playerState.getCharacterClass();
        Vector2 position = playerState.getPosition() == null
            ? new Vector2()
            : new Vector2(playerState.getPosition());

        Player player = new Player(
            playerState.getID(),
            playerState.getUsername(),
            position,
            CharacterBuild.of(CharacterBuild.DEFAULT_RACE, characterClass),
            teamIndex(playerState.getTeam()));

        if (playerState.isDead()) {
            player.getStats().setHp(0);
            player.getAnimationController().playDeath();
        }

        return player;
    }

    public static PlayerState toPlayerState(Player player) {
        if (player == null) return null;

        return PlayerState.builder()
            .ID(player.getID())
            .username(player.getUsername())
            .team(team(player.getTeamIndex()))
            .characterClass(player.getCharacterClass())
            .position(new Vector2(player.getPosition()))
            .dead(player.getStats().getHp() <= 0)
            .build();
    }

    private static int teamIndex(TEAM team) {
        if (team == TEAM.BLUE) return 1;
        if (team == TEAM.RED) return 2;
        return 0;
    }

    private static TEAM team(int teamIndex) {
        if (teamIndex == 1) return TEAM.BLUE;
        if (teamIndex == 2) return TEAM.RED;
        return TEAM.UNASSIGNED;
    }
}
