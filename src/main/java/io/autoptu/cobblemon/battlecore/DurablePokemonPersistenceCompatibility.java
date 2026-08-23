package io.autoptu.cobblemon.battlecore;

import java.util.Set;

/** Compatibility mapping for lossless durable canonical Pokemon persistence. */
public final class DurablePokemonPersistenceCompatibility {
    private DurablePokemonPersistenceCompatibility() {}

    public static final Set<UpstreamCompatibilityMatrix.Capability> CAPABILITIES = Set.of(
            UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY,
            UpstreamCompatibilityMatrix.Capability.CORE_CALCULATIONS_AND_COMBAT_STATS,
            UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE,
            UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE,
            UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR,
            UpstreamCompatibilityMatrix.Capability.ABILITIES,
            UpstreamCompatibilityMatrix.Capability.ITEMS,
            UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK
    );

    public static final String CONTRACT =
            "Persist the complete server-owned CanonicalPokemonState losslessly, including ordered stacked status entries and scalar metadata, under revision CAS and atomic replacement. "
            + "Stored base movement, stats, HP, injuries, move IDs, type/ability identities and held-item identity are canonical inputs only. "
            + "Movement modifiers, damage, status lifecycle, move specials, ability behavior and item effects remain AutoPTU-Java-owned.";

    public static boolean hasBlockingDependency() {
        return CAPABILITIES.stream()
                .map(UpstreamCompatibilityMatrix::entry)
                .anyMatch(entry -> entry.support() == UpstreamCompatibilityMatrix.Support.BLOCKING);
    }
}
