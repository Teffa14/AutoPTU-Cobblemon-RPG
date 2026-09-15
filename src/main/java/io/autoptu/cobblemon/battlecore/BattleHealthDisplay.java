package io.autoptu.cobblemon.battlecore;

/** Client presentation only: actual HP is never interpolated or written back to combat state. */
public final class BattleHealthDisplay {
    private float displayed;
    private float trail;
    private float target;
    private int hp;
    private int maximum;
    private int change;
    private int impactAge = 40;
    private boolean initialized;

    public void accept(int hp, int maximum) {
        if (maximum < 1 || hp < 0 || hp > maximum) throw new IllegalArgumentException("invalid health");
        float ratio = hp / (float) maximum;
        if (!initialized || this.maximum != maximum) {
            displayed = trail = ratio;
            impactAge = 40;
            change = 0;
        } else if (hp != this.hp) {
            change = hp - this.hp;
            trail = Math.max(trail, displayed);
            impactAge = 0;
        }
        initialized = true;
        this.hp = hp;
        this.maximum = maximum;
        target = ratio;
    }

    public void tick() {
        if (!initialized) return;
        impactAge = Math.min(40, impactAge + 1);
        displayed = approach(displayed, target, 0.24F);
        if (impactAge > 8) trail = approach(trail, target, 0.15F);
        trail = Math.max(trail, displayed);
    }

    private static float approach(float value, float target, float fraction) {
        return Math.abs(target - value) < 0.002F ? target : value + (target - value) * fraction;
    }

    public float displayed() { return displayed; }
    public float trail() { return trail; }
    public int change() { return change; }
    public int impactAge() { return impactAge; }
    public boolean recentImpact() { return impactAge < 30 && change != 0; }
}
