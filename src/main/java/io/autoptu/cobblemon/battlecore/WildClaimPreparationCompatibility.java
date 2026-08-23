package io.autoptu.cobblemon.battlecore;

/** Compatibility mapping for claim-time WILD preparation from server-owned correlation. */
public final class WildClaimPreparationCompatibility {
    private WildClaimPreparationCompatibility() {}

    public static IntegrationFeatureCompatibility.Requirement requirement() {
        return IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.SERVER_OWNED_WILD_ENCOUNTER_PROVISIONING
        );
    }

    public static boolean hasBlockingDependency() {
        return requirement().hasBlockingDependency();
    }
}
