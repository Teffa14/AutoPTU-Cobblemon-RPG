package io.autoptu.cobblemon.battlecore;

/** Compatibility boundary for binding authenticated live Pokemon UUIDs to durable canonical roster IDs. */
public final class AuthenticatedPlayerPokemonIdentityBindingCompatibility {
    private AuthenticatedPlayerPokemonIdentityBindingCompatibility() {}

    public static IntegrationFeatureCompatibility.Feature integrationFeature() {
        return IntegrationFeatureCompatibility.Feature.AUTHENTICATED_PLAYER_CONTEXT_RESOLUTION;
    }

    public static IntegrationFeatureCompatibility.Requirement requirement() {
        return IntegrationFeatureCompatibility.requirement(integrationFeature());
    }
}
