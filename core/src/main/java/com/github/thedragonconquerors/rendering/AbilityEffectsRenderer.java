package com.github.thedragonconquerors.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.github.thedragonconquerors.assets.AssetService;
import com.github.thedragonconquerors.assets.EffectAssets;
import com.shared.shared.model.ability.AbilityType;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/** Cosmetic playback only. The server remains responsible for every combat outcome. */
public final class AbilityEffectsRenderer {
    private final Batch batch;
    private final EnumMap<EffectAssets, TextureRegion[]> frames = new EnumMap<>(EffectAssets.class);
    private final List<Effect> active = new ArrayList<>();
    private static final class Effect {
        EffectAssets art;
        Vector2 from, to;
        Color tint;
        float delay, travel, age, size, rotation;
        boolean projectile;
    }
    public AbilityEffectsRenderer(AssetService assets, Batch batch) {
        this.batch = batch;
        for (EffectAssets art : EffectAssets.values()) {
            Texture texture = assets.load(art.clip);
            if (texture.getHeight() != 100 || texture.getWidth() != art.clip.frames() * 100)
                throw new IllegalStateException("Invalid effect strip: " + art.clip.path());
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            frames.put(art, TextureRegion.split(texture, 100, 100)[0]);
        }
    }
    public void play(AbilityType ability, Vector2 origin, Vector2 target, float castDuration) {
        float impact = castDuration * .65f;
        float launch = castDuration * .30f;
        switch (ability) {
            case TELEPORT -> {
                add(EffectAssets.PORTAL, origin, origin, 0, 0, 4f, Color.WHITE, false);
                add(EffectAssets.PORTAL, target, target, .12f, 0, 4f, Color.WHITE, false);
            }
            case ARROW_SHOT -> add(EffectAssets.ARROW, origin, target, launch, impact-launch, 3f, Color.WHITE, true);
            case RAIN_OF_ARROWS -> {
                for (int i = -2; i <= 2; i++) {
                    Vector2 landing = new Vector2(target).add(i * .45f, (i % 2) * .25f);
                    add(EffectAssets.ARROW, new Vector2(landing).add(-1.2f, 3f), landing,
                        launch + Math.abs(i) * .04f, impact-launch, 3f, Color.WHITE, true);
                }
            }
            case MAGE_BASIC, ICE_ATTACK -> projectile(EffectAssets.ARCANE, origin, target, launch, impact, new Color(.6f,.9f,1,1));
            case FIREBALL -> projectile(EffectAssets.FLAME, origin, target, launch, impact, Color.WHITE);
            case ELDRITCH_BLAST -> projectile(EffectAssets.SHADOW, origin, target, launch, impact, new Color(.7f,1f,.85f,1));
            case PALADIN_RANGED -> projectile(EffectAssets.RADIANT, origin, target, launch, impact, Color.WHITE);
            case DIVINE_SMITE, CLERIC_BASIC -> add(EffectAssets.RADIANT, target, target, impact, 0, 4f, Color.WHITE, false);
            case CURSE, POISON_JAB -> add(EffectAssets.SHADOW, target, target, impact, 0, 3f, Color.WHITE, false);
            case HEAL, REVIVE -> add(EffectAssets.HEAL, target, target, impact, 0, 4f, Color.WHITE, false);
            case STAT_BOOST, ENCORE, ACCURACY_BOOST -> add(EffectAssets.HEAL, target, target, impact, 0, 3f, new Color(1,.8f,.4f,1), false);
            case BARD_BASIC -> add(EffectAssets.ARCANE, target, target, impact, 0, 2.2f, new Color(1,.6f,.8f,1), false);
            default -> { /* Weapon trail is already painted in the melee strip. */ }
        }
    }
    private void projectile(EffectAssets art, Vector2 origin, Vector2 target, float launch, float impact, Color color) {
        add(art, origin, target, launch, impact-launch, 2.6f, color, true);
        add(art, target, target, impact, 0, 3.2f, color, false);
    }
    private void add(EffectAssets art, Vector2 from, Vector2 to, float delay, float travel, float size, Color tint, boolean projectile) {
        Effect e = new Effect();
        e.art = art; e.from = new Vector2(from); e.to = new Vector2(to);
        e.delay = delay; e.travel = travel; e.size = size; e.tint = tint; e.projectile = projectile;
        e.rotation = projectile && art == EffectAssets.ARROW ? new Vector2(to).sub(from).angleDeg() : 0;
        active.add(e);
    }
    public boolean isBusy() { return !active.isEmpty(); }
    public void render(Matrix4 projection, float delta) {
        if (active.isEmpty()) return;
        batch.setProjectionMatrix(projection);
        batch.begin();
        for (var iterator = active.iterator(); iterator.hasNext();) {
            Effect e = iterator.next();
            e.age += Math.max(0, delta);
            float time = e.age - e.delay;
            if (time < 0) continue;
            float duration = e.projectile ? e.travel : e.art.clip.duration();
            if (time >= duration) { iterator.remove(); continue; }
            float t = e.projectile ? Math.min(1, time / Math.max(.001f, e.travel)) : 1;
            float x = e.from.x + (e.to.x - e.from.x) * t;
            float y = e.from.y + (e.to.y - e.from.y) * t;
            TextureRegion frame = frames.get(e.art)[e.art.clip.frameAt(time)];
            batch.setColor(e.tint);
            float offset = e.art == EffectAssets.PORTAL || e.art == EffectAssets.HEAL ? e.size * .41f : e.size / 2f - .5f;
            batch.draw(frame, x - e.size/2, y - offset,
                e.size/2, e.size/2, e.size, e.size, 1, 1, e.rotation);
        }
        batch.setColor(Color.WHITE);
        batch.end();
    }
}
