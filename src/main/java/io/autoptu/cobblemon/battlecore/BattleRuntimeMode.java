package io.autoptu.cobblemon.battlecore;

/** Mutually exclusive gameplay owners: never register both onboarding/battle stacks. */
public enum BattleRuntimeMode {
    COBBLEMON, PTU_EXPERIMENTAL;

    public static BattleRuntimeMode parse(String value) {
        if (value == null || value.isBlank() || value.equals("cobblemon")) return COBBLEMON;
        if (value.equals("ptu-experimental")) return PTU_EXPERIMENTAL;
        throw new IllegalArgumentException("autoptu.gameplay must be cobblemon or ptu-experimental");
    }

    public static BattleRuntimeMode configured() {
        return parse(System.getProperty("autoptu.gameplay"));
    }
}
