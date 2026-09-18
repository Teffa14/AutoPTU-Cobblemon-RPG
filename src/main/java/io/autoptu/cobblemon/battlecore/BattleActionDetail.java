package io.autoptu.cobblemon.battlecore;

import java.util.Objects;

/** Human-readable values supplied alongside a move by its owning server profile. */
public record BattleActionDetail(String name, String description, String range, String damage, String accuracy) {
    public BattleActionDetail {
        name = bounded(name, 128);
        description = bounded(description, 512);
        range = bounded(range, 64);
        damage = bounded(damage, 64);
        accuracy = bounded(accuracy, 64);
    }

    private static String bounded(String value, int maximum) {
        Objects.requireNonNull(value);
        if (value.length() > maximum) throw new IllegalArgumentException("action detail is too long");
        return value;
    }
}
