// File Location: shared/src/main/java/com/shared/shared/model/stats/StatComponent.java
package com.shared.shared.model.stats;

import lombok.Getter;
import lombok.Setter;

@Getter
public class StatComponent {
    @Setter
    private int hp;
    @Setter
    private int maxHp;

    @Setter
    private int mana;
    @Setter
    private int maxMana;

    private int accuracy;
    private int strength;
    private int speed;
    private int inspiration;
    private int wisdom;

    public StatComponent(int maxHp, int maxMana, int accuracy, int strength, int speed, int inspiration, int wisdom){
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.mana = maxMana;
        this.maxMana = maxMana;

        this.accuracy = clamp(accuracy, 1, 20);
        this.strength = clamp(strength, 1, 20);
        this.speed = clamp(speed, 1, 20);
        this.inspiration = clamp(inspiration, 1, 20);
        this.wisdom = clamp(wisdom, 1, 20);
    }

    /** Deep copy — used to preview a build's boosted stats without mutating the original. */
    public StatComponent(StatComponent other){
        this.maxHp = other.maxHp;
        this.hp = other.hp;
        this.maxMana = other.maxMana;
        this.mana = other.mana;
        this.accuracy = other.accuracy;
        this.strength = other.strength;
        this.speed = other.speed;
        this.inspiration = other.inspiration;
        this.wisdom = other.wisdom;
    }

    public static StatComponent defaultStats(){
        return new StatComponent(100, 50, 10, 10, 10, 10, 10);
    }

    /**
     * Raises (or lowers) the HP pool and its maximum together.
     *
     * Build-time boosts must grant usable HP, not just a bigger empty bar, so
     * current HP moves with the maximum. Current HP is never pushed above the
     * new maximum, and never below 0.
     */
    public void addMaxHp(int amount){
        maxHp = Math.max(1, maxHp + amount);
        hp = Math.min(maxHp, Math.max(0, hp + amount));
    }

    /** Mana counterpart of {@link #addMaxHp(int)}. */
    public void addMaxMana(int amount){
        maxMana = Math.max(0, maxMana + amount);
        mana = Math.min(maxMana, Math.max(0, mana + amount));
    }

    public boolean applyDamage(int amount){
        hp = Math.max(0, hp-amount);
        return hp > 0;
    }

    public void heal(int amount){
        hp = Math.min(maxHp, hp+amount);
    }

    public boolean isAlive(){
        return hp > 0;
    }

    public boolean spendMana(int cost){
        if(mana < cost) return false;

        mana -= cost;
        return true;
    }

    public void regenerateMana(int amount){
        mana = Math.min(maxMana, mana+amount);
    }

    private static int clamp(int value, int min, int max){
        return Math.max(min, Math.min(max, value));
    }

    public void setAccuracy(int accuracy) {
        this.accuracy = clamp(accuracy, 1, 20);
    }

    public void setStrength(int strength) {
        this.strength = clamp(strength, 1, 20);
    }

    public void setSpeed(int speed) {
        this.speed = clamp(speed, 1, 20);
    }

    public void setInspiration(int inspiration) {
        this.inspiration = clamp(inspiration, 1, 20);
    }

    public void setWisdom(int wisdom) {
        this.wisdom = clamp(wisdom, 1, 20);
    }
}
