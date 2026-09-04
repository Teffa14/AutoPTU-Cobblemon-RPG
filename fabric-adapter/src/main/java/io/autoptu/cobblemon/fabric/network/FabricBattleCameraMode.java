package io.autoptu.cobblemon.fabric.network;

/**
 * Presentation-only camera modes for an already server-authored AutoPTU battle scene.
 *
 * <p>The mode changes only how Minecraft frames known world actors. It never communicates turn
 * ownership, legality, damage, target validity, battle outcome or any other PTU semantic fact.
 */
public enum FabricBattleCameraMode {
    TACTICAL_AERIAL,
    TRAINER_EXTERNAL,
    ACTION_CINEMATIC;

    static FabricBattleCameraMode fromWire(String value) {
        if (value == null || value.isBlank()) return TACTICAL_AERIAL;
        try {
            return valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return TACTICAL_AERIAL;
        }
    }
}
