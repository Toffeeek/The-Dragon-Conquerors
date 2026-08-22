// File Location: server/src/main/java/com/server/server/combat/ServerCombatant.java
package com.server.server.combat;

import com.badlogic.gdx.math.Vector2;
import com.server.server.selection.LobbyPlayer;
import com.shared.shared.model.CharacterBuild;
import com.shared.shared.model.CharacterClass;
import com.shared.shared.model.Race;
import com.shared.shared.model.combat.CombatContext;
import com.shared.shared.model.combat.Combatant;
import com.shared.shared.model.effect.StatusEffect;
import com.shared.shared.model.stats.StatCalculator;
import com.shared.shared.model.stats.StatComponent;
import com.shared.shared.network.PlayerCombatState;

import java.util.ArrayList;
import java.util.List;

/** Rendering-free combatant owned exclusively by the authoritative server. */
public final class ServerCombatant implements Combatant {
    private final int id;
    private final String username;
    private final CharacterClass characterClass;
    private final Race race;
    private final int teamIndex;
    private final Vector2 position;
    private final StatComponent stats;
    private final List<StatusEffect> activeEffects = new ArrayList<>();
    private float maxMovement;
    private float remainingMovement;
    private boolean actionUsed;

    public ServerCombatant(LobbyPlayer player) {
        this(player, player.getPosition());
    }

    public ServerCombatant(LobbyPlayer player, Vector2 spawnPosition) {
        this.id = player.getId();
        this.username = player.getUsername();
        this.characterClass = player.getCharacterClass();
        this.race = player.getRace();
        this.teamIndex = player.getTeamIndex();
        this.position = new Vector2(spawnPosition);
        this.stats = CharacterBuild.of(race, characterClass).createStats();
        refreshTurnResources();
    }

    @Override public int getId() { return id; }
    @Override public String getUsername() { return username; }
    @Override public CharacterClass getCharacterClass() { return characterClass; }
    @Override public Race getRace() { return race; }
    @Override public int getTeamIndex() { return teamIndex; }
    @Override public Vector2 getPosition() { return position; }
    @Override public StatComponent getStats() { return stats; }
    @Override public List<StatusEffect> getActiveEffects() { return activeEffects; }

    @Override
    public void onTurnStart() {
        stats.regenerateMana(StatCalculator.manaRegenPerTurn(stats));
        refreshTurnResources();
    }

    public float getRemainingMovement() { return remainingMovement; }
    public boolean isActionUsed() { return actionUsed; }
    public void markActionUsed() { actionUsed = true; }

    public void moveTo(Vector2 destination) {
        float distance = position.dst(destination);
        position.set(destination);
        remainingMovement = Math.max(0f, remainingMovement - distance);
    }

    private void refreshTurnResources() {
        maxMovement = StatCalculator.deriveMaxMovementDistance(stats);
        remainingMovement = maxMovement;
        actionUsed = false;
    }

    public PlayerCombatState snapshot(CombatContext context, boolean activeTurn) {
        List<StatusEffect> effectCopies = new ArrayList<>();
        for (StatusEffect effect : activeEffects) {
            StatusEffect copy = new StatusEffect();
            copy.setType(effect.getType());
            copy.setRemainingTurns(effect.getRemainingTurns());
            copy.setSourcePlayerId(effect.getSourcePlayerId());
            copy.setCounterHitLanded(effect.isCounterHitLanded());
            effectCopies.add(copy);
        }

        return PlayerCombatState.builder()
            .id(id)
            .username(username)
            .characterClass(characterClass)
            .race(race)
            .teamIndex(teamIndex)
            .position(new Vector2(position))
            .hp(stats.getHp())
            .maxHp(stats.getMaxHp())
            .mana(stats.getMana())
            .maxMana(stats.getMaxMana())
            .accuracy(stats.getAccuracy())
            .strength(stats.getStrength())
            .speed(stats.getSpeed())
            .inspiration(stats.getInspiration())
            .wisdom(stats.getWisdom())
            .remainingMovement(remainingMovement)
            .maxMovement(maxMovement)
            .actionUsed(actionUsed)
            .activeTurn(activeTurn)
            .effects(effectCopies)
            .cooldowns(context.cooldownsFor(this).snapshot())
            .build();
    }
}
