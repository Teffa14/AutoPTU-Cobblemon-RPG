package io.autoptu.cobblemon.battlecore;

/** Adapter-neutral world block coordinate including a stable dimension identifier. */
public record WorldBlockCoordinate(String dimensionId, int x, int y, int z) {
    public WorldBlockCoordinate {
        if (dimensionId == null || dimensionId.isBlank()) {
            throw new IllegalArgumentException("dimensionId is required");
        }
        dimensionId = dimensionId.strip();
    }
}
