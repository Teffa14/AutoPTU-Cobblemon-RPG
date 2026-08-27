package io.autoptu.cobblemon.fabric.world;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import static io.autoptu.cobblemon.fabric.world.OurosGrandPalaceBuildKit.clear;

/** Clean, exterior-first builder for the courtyard-based Grand Palace V4. */
public final class OurosGrandPalaceV4Builder {
    static final int MIN_X = -54;
    static final int MAX_X = 54;
    static final int MIN_Y = -4;
    static final int MAX_Y = 72;
    static final int MIN_Z = -72;
    static final int MAX_Z = 66;

    private OurosGrandPalaceV4Builder() {}

    public static OurosGrandPalace.BuildResult build(ServerWorld world, BlockPos origin) {
        // V4 is a replacement build. Clear the whole reviewed envelope so no V1/V3 foundation,
        // envelope or roof can survive underneath the courtyard plan on rebuild.
        clear(world, origin, MIN_X, MIN_Y, MIN_Z, MAX_X, MAX_Y, MAX_Z);

        // Order is intentional: massing, front facade, authored rooms, visual cleanup, structural
        // joins, low mansards, silhouette accents, audit. Visual refinement runs after room themes so
        // it can remove diagnostic fixtures and hash-selected accent bands without erasing the room
        // program; anchoring then validates the final authored interior.
        OurosGrandPalaceV4ArchitecturePass.apply(world, origin);
        OurosGrandPalaceV4FacadeRefinementPass.apply(world, origin);
        OurosGrandPalaceV4Rooms.buildAll(world, origin);
        OurosGrandPalaceV4InteriorRefinementPass.apply(world, origin);
        OurosGrandPalaceV4AccentRefinementPass.apply(world, origin);
        OurosGrandPalaceV4RoomAnchoringPass.apply(world, origin);
        OurosGrandPalaceV4ConnectivityPass.apply(world, origin);
        OurosGrandPalaceV4AuthoredRoofPass.apply(world, origin);
        OurosGrandPalaceV4SilhouettePass.apply(world, origin);
        OurosGrandPalaceV4QualityAudit.assertValid(world, origin);

        return new OurosGrandPalace.BuildResult(
                origin,
                MAX_X - MIN_X + 1,
                MAX_Y - MIN_Y + 1,
                MAX_Z - MIN_Z + 1,
                19
        );
    }
}
