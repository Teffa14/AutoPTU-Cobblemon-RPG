package io.autoptu.cobblemon.battlecore;

import java.util.Set;

/** Compatibility mapping for the durable canonical item reservation slice. */
public final class DurableItemReservationCompatibility {
    private DurableItemReservationCompatibility() {}

    public static final Set<UpstreamCompatibilityMatrix.Capability> CAPABILITIES = Set.of(
            UpstreamCompatibilityMatrix.Capability.ITEMS,
            UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK
    );

    public static final String CONTRACT =
            "Persist only server-owned canonical item identity, ownership, template, quantity, revision and reservation state. "
            + "Reservation create/commit/release must remain atomic and revision-checked across restart. "
            + "Item effects, crafting semantics, capture modifiers, healing, damage hooks and equipment behavior remain AutoPTU-Java-owned.";

    public static boolean hasBlockingDependency() {
        return CAPABILITIES.stream()
                .map(UpstreamCompatibilityMatrix::entry)
                .anyMatch(entry -> entry.support() == UpstreamCompatibilityMatrix.Support.BLOCKING);
    }
}
