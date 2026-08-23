package io.autoptu.cobblemon.battlecore;

/** Compatibility guard for the world-scoped canonical WILD encounter blueprint registry. */
public final class WorldScopedWildBlueprintRegistryCompatibility {
    private WorldScopedWildBlueprintRegistryCompatibility() {}

    public static IntegrationFeatureCompatibility.Requirement requirement() {
        return IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.SERVER_OWNED_WILD_ENCOUNTER_PROVISIONING
        );
    }

    public static boolean hasBlockingDependency() {
        return requirement().hasBlockingDependency();
    }
}
