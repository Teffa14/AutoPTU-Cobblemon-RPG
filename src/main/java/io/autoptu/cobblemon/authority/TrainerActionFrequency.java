package io.autoptu.cobblemon.authority;

/** PTU frequency windows relevant to Trainer actions. */
public enum TrainerActionFrequency {
    AT_WILL,
    DAILY,
    SCENE,
    ENCOUNTER,
    ROUND,
    TURN;

    public boolean requiresCanonicalContext() {
        return this == SCENE || this == ENCOUNTER;
    }

    public boolean battleCoreOwned() {
        return this == ROUND || this == TURN;
    }
}
