package io.autoptu.cobblemon.fabric.world;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Runtime geometry assertions that reject regressions back to the old rectangular Palace mass. */
final class OurosGrandPalaceV4QualityAudit {
    private OurosGrandPalaceV4QualityAudit() {}

    static Report assertValid(ServerWorld world, BlockPos o) {
        List<String> failures = new ArrayList<>();

        double westGroundOpen = openRatio(world, o, -24, -15, -52, 52, 10);
        double eastGroundOpen = openRatio(world, o, 15, 24, -52, 52, 10);
        double westUpperOpen = openRatio(world, o, -24, -15, -52, 52, 24);
        double eastUpperOpen = openRatio(world, o, 15, 24, -52, 52, 24);
        double westSkyOpen = openRatio(world, o, -24, -15, -52, 52, 31);
        double eastSkyOpen = openRatio(world, o, 15, 24, -52, 52, 31);

        requireAtLeast(failures, "west court ground openness", westGroundOpen, 0.82);
        requireAtLeast(failures, "east court ground openness", eastGroundOpen, 0.82);
        requireAtLeast(failures, "west court upper openness", westUpperOpen, 0.82);
        requireAtLeast(failures, "east court upper openness", eastUpperOpen, 0.82);
        requireAtLeast(failures, "west court roof openness", westSkyOpen, 0.88);
        requireAtLeast(failures, "east court roof openness", eastSkyOpen, 0.88);

        // At each pavilion row the three architectural masses must exist while the two court cores stay void.
        for (int z : new int[]{-42, -14, 14, 42}) {
            requireSolid(failures, world, o, -50, 10, z, "west pavilion wall z=" + z);
            requireSolid(failures, world, o, -11, 10, z, "central spine wall z=" + z);
            requireSolid(failures, world, o, 50, 10, z, "east pavilion wall z=" + z);
            requireAir(failures, world, o, -20, 10, z, "west court core z=" + z);
            requireAir(failures, world, o, 20, 10, z, "east court core z=" + z);
        }

        // The deep central arrival projects past two recessed side forecourts.
        requireSolid(failures, world, o, 0, 10, -60, "deep central portico");
        requireAir(failures, world, o, -40, 10, -60, "west recessed forecourt");
        requireAir(failures, world, o, 40, 10, -60, "east recessed forecourt");

        if (!failures.isEmpty()) {
            throw new IllegalStateException("Grand Palace V4 anti-box audit failed: " + String.join("; ", failures));
        }
        return new Report(westGroundOpen, eastGroundOpen, westUpperOpen, eastUpperOpen, westSkyOpen, eastSkyOpen);
    }

    private static double openRatio(ServerWorld world, BlockPos o, int x1, int x2, int z1, int z2, int y) {
        int total = 0;
        int air = 0;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                // Bridge bands are intentional connectors, not court infill.
                if (inBridgeBand(z)) continue;
                total++;
                if (world.getBlockState(o.add(x, y, z)).isAir()) air++;
            }
        }
        return total == 0 ? 0.0 : air / (double) total;
    }

    private static boolean inBridgeBand(int z) {
        return (z >= -30 && z <= -27) || (z >= -2 && z <= 1) || (z >= 26 && z <= 29);
    }

    private static void requireAtLeast(List<String> failures, String label, double actual, double expected) {
        if (actual + 1.0e-9 < expected) failures.add(label + "=" + String.format("%.1f%%", actual * 100.0));
    }

    private static void requireSolid(List<String> failures, ServerWorld world, BlockPos o,
                                     int x, int y, int z, String label) {
        if (world.getBlockState(o.add(x, y, z)).isAir()) failures.add(label + " unexpectedly air");
    }

    private static void requireAir(List<String> failures, ServerWorld world, BlockPos o,
                                   int x, int y, int z, String label) {
        if (!world.getBlockState(o.add(x, y, z)).isAir()) failures.add(label + " unexpectedly filled");
    }

    record Report(double westGroundOpen, double eastGroundOpen,
                  double westUpperOpen, double eastUpperOpen,
                  double westSkyOpen, double eastSkyOpen) {}
}