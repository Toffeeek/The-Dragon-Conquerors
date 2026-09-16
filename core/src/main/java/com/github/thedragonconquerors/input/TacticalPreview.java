package com.github.thedragonconquerors.input;

import com.badlogic.gdx.math.Vector2;
import com.github.thedragonconquerors.entities.Player;
import com.shared.shared.model.ability.AbilityType;
import com.shared.shared.model.combat.AbilityResolver;
import com.shared.shared.model.effect.StatusEffectType;

/** Read-only presentation helpers: no RNG rolls, resource spending, or collider changes. */
public final class TacticalPreview {
    private static final AbilityResolver PREVIEW = new AbilityResolver(0);
    private TacticalPreview() {}
    public static boolean hitsBody(Vector2 feet, Vector2 click) {
        return feet.dst2(click) <= .72f * .72f
            || Math.abs(click.x - feet.x) <= .45f && click.y >= feet.y - .15f && click.y <= feet.y + 1.2f;
    }
    public static String describe(Player actor, AbilityType ability, Player target) {
        if (actor == null || ability == null) return "";
        StringBuilder text = new StringBuilder(ability.getDisplayName()).append(" | ");
        text.append(ability.getRange() > 100 ? "Map-wide" : "Range " + ability.getRange());
        int damage = PREVIEW.previewDamage(actor, ability), healing = PREVIEW.previewHealing(actor, ability);
        if (damage > 0) text.append(" | ").append(damage).append(" damage on hit");
        if (healing > 0) text.append(" | Up to ").append(healing).append(" healing");
        if (ability.getAreaRadius() > 0) text.append(" | Radius ").append(ability.getAreaRadius());
        if (ability.hasStatusEffect()) {
            var effect = ability.getAppliedEffect();
            text.append("\n").append(effect.getDisplayName()).append(": ").append(ability.getEffectChancePercent())
                .append("% on success, ").append(effect.getDefaultDuration()).append(" own turns");
            if (effect == StatusEffectType.CURSE) text.append("; strike the caster to escape");
        }
        if (target != null) {
            text.append("\n").append(target.getUsername());
            if (damage > 0) text.append(" | ").append(PREVIEW.previewHitChance(actor,target)).append("% hit chance");
            if (actor.getPosition().dst(target.getPosition()) > ability.getRange()) text.append(" | OUT OF RANGE");
            if (!target.getActiveEffects().isEmpty()) text.append("\n").append(statuses(target));
        }
        return text.toString();
    }
    public static String statuses(Player player) {
        StringBuilder text = new StringBuilder();
        for (var effect : player.getActiveEffects()) {
            if (!text.isEmpty()) text.append(" | ");
            text.append(effect.getType().getDisplayName()).append(' ').append(effect.getRemainingTurns()).append("t");
            if (effect.getType() == StatusEffectType.CURSE) text.append(effect.isCounterHitLanded() ? " (escaped)" : " (hit caster)");
        }
        return text.toString();
    }
}
