package com.github.thedragonconquerors.entities;

import com.badlogic.gdx.math.Vector2;
import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.PlayerState;

public final class PlayerConverter {
    private PlayerConverter() {}

    public static Player toPlayer(PlayerState playerState) {
        if (playerState == null) return null;

        CharacterClass characterClass = playerState.getCharacterClass() == null
            ? CharacterClass.WARRIOR
            : playerState.getCharacterClass();
        Vector2 position = playerState.getPosition() == null
            ? new Vector2()
            : new Vector2(playerState.getPosition());

        Player player = new Player(
            playerState.getID(),
            playerState.getUsername(),
            playerState.getTeam(),
            position,
            characterClass);

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
            .team(player.getTeam())
            .characterClass(player.getCharacterClass())
            .position(new Vector2(player.getPosition()))
            .dead(player.getStats().getHp() <= 0)
            .build();
    }
}
