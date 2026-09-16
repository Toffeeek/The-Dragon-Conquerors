package com.github.thedragonconquerors.ui;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.shared.shared.model.ability.AbilityType;
import com.shared.shared.model.effect.StatusEffectType;
import java.util.*;

/** Tiny code-drawn pixel symbols share the game's palette; no external art dependency. */
public final class PixelIcons implements Disposable {
    public enum Kind { BLADE, FLAME, ICE, SKULL, DROP, STAR, ARROW, PORTAL, HEART, MUSIC, SHIELD, BOLT }
    private final Map<Kind,Texture> textures=new EnumMap<>(Kind.class);
    private final Map<Kind,TextureRegionDrawable> icons=new EnumMap<>(Kind.class);
    public PixelIcons() {
        for(var kind:Kind.values()) {
            Pixmap p=new Pixmap(20,20,Pixmap.Format.RGBA8888);
            p.setColor(.025f,.06f,.09f,.55f); p.fillCircle(10,10,9);
            p.setColor(.65f,.55f,.34f,.8f); p.drawCircle(10,10,9);
            p.setColor(color(kind));
            switch(kind) {
                case BLADE -> { p.fillRectangle(9,3,3,10); p.fillRectangle(5,12,11,2); p.fillRectangle(9,14,3,3); }
                case FLAME -> { p.fillTriangle(5,14,10,3,15,14); p.fillCircle(10,13,5); p.setColor(Color.GOLD); p.fillTriangle(8,15,10,9,13,15); }
                case ICE -> { p.drawLine(10,3,10,17); p.drawLine(3,6,17,14); p.drawLine(3,14,17,6); p.drawRectangle(7,7,6,6); }
                case SKULL -> { p.fillCircle(10,8,6); p.fillRectangle(7,12,6,5); p.setColor(Color.BLACK); p.fillRectangle(6,7,3,3); p.fillRectangle(11,7,3,3); p.drawLine(10,14,10,16); }
                case DROP -> { p.fillTriangle(5,12,10,3,15,12); p.fillCircle(10,12,5); }
                case STAR -> { p.fillTriangle(10,2,7,11,14,11); p.fillTriangle(10,18,7,9,14,9); p.fillTriangle(2,10,11,7,11,14); p.fillTriangle(18,10,9,7,9,14); }
                case ARROW -> { p.drawLine(4,16,15,5); p.drawLine(5,16,16,5); p.drawLine(9,4,16,4); p.drawLine(16,4,16,11); }
                case PORTAL -> { p.drawCircle(10,10,7); p.drawCircle(10,10,4); p.fillRectangle(9,8,3,4); }
                case HEART -> { p.fillCircle(6,7,4); p.fillCircle(13,7,4); p.fillTriangle(2,8,17,8,10,17); }
                case MUSIC -> { p.fillRectangle(9,4,3,10); p.fillRectangle(11,4,5,3); p.fillCircle(7,14,4); }
                case SHIELD -> { p.fillRectangle(4,3,12,8); p.fillTriangle(4,10,16,10,10,17); p.setColor(Color.WHITE); p.drawLine(10,5,10,13); }
                case BOLT -> { p.fillTriangle(11,2,5,11,11,11);p.fillTriangle(9,9,15,9,9,18); }
            }
            Texture texture=new Texture(p); p.dispose(); texture.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
            textures.put(kind,texture); icons.put(kind,new TextureRegionDrawable(new TextureRegion(texture)));
        }
    }
    public static Color color(Kind kind) { return switch(kind) {
        case FLAME -> Color.ORANGE; case ICE -> Color.CYAN; case SKULL,PORTAL -> Color.VIOLET;
        case DROP -> Color.LIME; case HEART -> Color.SALMON; case MUSIC -> Color.PINK; default -> Color.GOLD;
    }; }
    public static Kind kind(StatusEffectType effect) { return switch(effect) { case BURN->Kind.FLAME;case POISON->Kind.DROP;case CURSE->Kind.SKULL;case SUB_ZERO->Kind.ICE;case STUN->Kind.STAR; }; }
    public static Kind kind(AbilityType ability) { return switch(ability) {
        case FIREBALL->Kind.FLAME; case ICE_ATTACK->Kind.ICE;case CURSE->Kind.SKULL;case POISON_JAB->Kind.DROP;
        case TELEPORT->Kind.PORTAL;case HEAL,REVIVE->Kind.HEART;case ARROW_SHOT,RAIN_OF_ARROWS,ACCURACY_BOOST->Kind.ARROW;
        case BARD_BASIC,STAT_BOOST,ENCORE->Kind.MUSIC;case PALADIN_BASIC,DIVINE_SMITE->Kind.SHIELD;
        case ELDRITCH_BLAST,MAGE_BASIC,PALADIN_RANGED->Kind.STAR;default->Kind.BLADE;
    }; }
    public TextureRegionDrawable drawable(Kind kind) { return icons.get(kind); }
    public Texture texture(Kind kind) { return textures.get(kind); }
    @Override public void dispose() { textures.values().forEach(Texture::dispose); }
}
