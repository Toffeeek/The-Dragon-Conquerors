package com.github.thedragonconquerors.rendering;

import com.shared.shared.model.world.Environment;

/** Artwork classification only; never a source of gameplay collision or hazard rules. */
public final class MapSurface {
    private MapSurface() {}
    public static boolean liquid(Environment environment,int rgba,boolean lethal) {
        if(!lethal)return false;
        int r=rgba>>>24,g=rgba>>>16&255,b=rgba>>>8&255;
        return switch(environment) {
            case CANYON -> b>135 && r<65 && g>80;
            case LAVA -> r>190 && g>35 && g<170 && b<65;
            case BOG -> b>140 && r>65 && r<200 && g<95;
        };
    }
    public static boolean waterfall(Environment environment,int x,int y,int rgba) {
        int r=rgba>>>24,g=rgba>>>16&255,b=rgba>>>8&255;
        return environment==Environment.BOG && x>=788 && x<=837 && y>=156 && y<=273
            && b>160 && b>g*1.25f && r>90;
    }
}
