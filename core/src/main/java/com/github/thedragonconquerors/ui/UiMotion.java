package com.github.thedragonconquerors.ui;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
public final class UiMotion {
    private UiMotion() {}
    public static void relayout(Actor actor) {
        if(actor instanceof com.badlogic.gdx.scenes.scene2d.Group group)
            for(Actor child:group.getChildren())relayout(child);
        if(actor instanceof com.badlogic.gdx.scenes.scene2d.utils.Layout layout)layout.invalidateHierarchy();
    }
    public static void reveal(Actor actor) {
        actor.clearActions(); actor.getColor().a=1;
        if(!PresentationSettings.reducedMotion()) { actor.getColor().a=0;actor.addAction(Actions.fadeIn(.18f)); }
    }
}
