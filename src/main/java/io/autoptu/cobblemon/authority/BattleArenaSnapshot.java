package io.autoptu.cobblemon.authority;

/**
 * Immutable server-owned placement of a PTU battle grid in world space.
 *
 * This record freezes only the dimension, origin, elevation, and two cardinal basis axes.
 * It does not decide terrain, collision, movement legality, targeting, forced movement,
 * hazards, or any other PTU rule.
 */
public record BattleArenaSnapshot(
        String dimensionId,
        int originX,
        int originY,
        int originZ,
        int gridXdx,
        int gridXdz,
        int gridYdx,
        int gridYdz
) {
    public BattleArenaSnapshot {
        if (dimensionId == null || dimensionId.isBlank()) {
            throw new IllegalArgumentException("dimensionId must not be blank");
        }
        dimensionId = dimensionId.strip();
        validateCardinalUnitAxis(gridXdx, gridXdz, "grid X");
        validateCardinalUnitAxis(gridYdx, gridYdz, "grid Y");
        if (gridXdx * gridYdx + gridXdz * gridYdz != 0) {
            throw new IllegalArgumentException("battle grid axes must be perpendicular");
        }
    }

    private static void validateCardinalUnitAxis(int dx, int dz, String name) {
        if (Math.abs(dx) + Math.abs(dz) != 1) {
            throw new IllegalArgumentException(name + " axis must be one horizontal cardinal unit step");
        }
    }
}
