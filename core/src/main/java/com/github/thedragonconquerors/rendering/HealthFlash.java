package com.github.thedragonconquerors.rendering;

/** Short presentation-only pulse, delayed to coincide with the attack/heal impact. */
final class HealthFlash {
    enum Kind { DAMAGE, HEAL }
    final Kind kind;
    private float delay;
    private float remaining = .18f;

    HealthFlash(Kind kind, float delay) {
        this.kind = kind;
        this.delay = Math.max(0, delay);
    }

    void advance(float delta) {
        float elapsed = Math.max(0, delta);
        float activeElapsed = Math.max(0, elapsed - delay);
        delay = Math.max(0, delay - elapsed);
        remaining = Math.max(0, remaining - activeElapsed);
    }

    boolean active() { return delay <= 0 && remaining > 0; }
    boolean finished() { return remaining <= 0; }
}
