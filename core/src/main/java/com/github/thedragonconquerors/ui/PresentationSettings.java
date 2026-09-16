package com.github.thedragonconquerors.ui;

import com.badlogic.gdx.Gdx;

/** Presentation preferences only: never alter simulation or network timing. */
public final class PresentationSettings {
    private PresentationSettings() {}
    public static boolean reducedMotion() { return Gdx.app.getPreferences("tdc-presentation").getBoolean("reducedMotion",false); }
    public static boolean shake() { return !reducedMotion() && Gdx.app.getPreferences("tdc-presentation").getBoolean("shake",false); }
    public static float textScale() { return Math.max(1,Math.min(1.5f,Gdx.app.getPreferences("tdc-presentation").getFloat("textScale",1))); }
    public static void toggleMotion() { Gdx.app.getPreferences("tdc-presentation").putBoolean("reducedMotion",!reducedMotion()).flush(); }
    public static void toggleShake() { var p=Gdx.app.getPreferences("tdc-presentation"); p.putBoolean("shake",!p.getBoolean("shake",false)).flush(); }
    public static void cycleText() { float value=textScale(); Gdx.app.getPreferences("tdc-presentation").putFloat("textScale",value<1.2f?1.25f:value<1.4f?1.5f:1).flush(); }
    public static boolean guideDismissed() { return Gdx.app.getPreferences("tdc-presentation").getBoolean("guideDismissed",false); }
    public static void dismissGuide(boolean value) { Gdx.app.getPreferences("tdc-presentation").putBoolean("guideDismissed",value).flush(); }
}
