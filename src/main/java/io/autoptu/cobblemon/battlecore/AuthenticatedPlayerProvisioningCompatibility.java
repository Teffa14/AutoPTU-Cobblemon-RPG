package io.autoptu.cobblemon.battlecore;

/** Compatibility boundary for Fabric login-time canonical player provisioning. */
public final class AuthenticatedPlayerProvisioningCompatibility {
    private AuthenticatedPlayerProvisioningCompatibility() {}

    public static IntegrationFeatureCompatibility.Feature integrationFeature() {
        return IntegrationFeatureCompatibility.Feature.AUTHENTICATED_PLAYER_CONTEXT_RESOLUTION;
    }

    public static IntegrationFeatureCompatibility.Requirement requirement() {
        return IntegrationFeatureCompatibility.requirement(integrationFeature());
    }
}
