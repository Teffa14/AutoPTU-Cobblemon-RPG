package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildCalmAlphaCohesionNavigationRuntimeTest {
    private static final UUID SELF = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID ALPHA_NEAR = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID ALPHA_FAR = UUID.fromString("00000000-0000-0000-0000-000000000030");
    private static final UUID MEMBER = UUID.fromString("00000000-0000-0000-0000-000000000040");

    @Test
    void nearestActiveSamePopulationAlphaWins() {
        var candidates = List.of(
                candidate(ALPHA_FAR, "marea", 9.0D, 0.0D, WildSocialRole.ALPHA, true),
                candidate(MEMBER, "marea", 1.0D, 0.0D, WildSocialRole.MEMBER, true),
                candidate(ALPHA_NEAR, "marea", 4.0D, 0.0D, WildSocialRole.ALPHA, true));

        assertEquals(ALPHA_NEAR, WildCalmAlphaCohesionNavigationRuntime.deterministicAlphaAnchorIdentity(
                SELF, 0.0D, 0.0D, "marea", candidates).orElseThrow());
    }

    @Test
    void ignoresOtherPopulationInactiveMemberAndSelf() {
        var candidates = List.of(
                candidate(SELF, "marea", 1.0D, 0.0D, WildSocialRole.ALPHA, true),
                candidate(ALPHA_NEAR, "other", 2.0D, 0.0D, WildSocialRole.ALPHA, true),
                candidate(ALPHA_FAR, "marea", 3.0D, 0.0D, WildSocialRole.ALPHA, false),
                candidate(MEMBER, "marea", 4.0D, 0.0D, WildSocialRole.MEMBER, true));

        assertTrue(WildCalmAlphaCohesionNavigationRuntime.deterministicAlphaAnchorIdentity(
                SELF, 0.0D, 0.0D, "marea", candidates).isEmpty());
    }

    @Test
    void stableUuidBreaksEqualDistanceAlphaTie() {
        var low = candidate(ALPHA_NEAR, "marea", -4.0D, 0.0D, WildSocialRole.ALPHA, true);
        var high = candidate(ALPHA_FAR, "marea", 4.0D, 0.0D, WildSocialRole.ALPHA, true);

        assertEquals(ALPHA_NEAR, WildCalmAlphaCohesionNavigationRuntime.deterministicAlphaAnchorIdentity(
                SELF, 0.0D, 0.0D, "marea", List.of(high, low)).orElseThrow());
        assertEquals(ALPHA_NEAR, WildCalmAlphaCohesionNavigationRuntime.deterministicAlphaAnchorIdentity(
                SELF, 0.0D, 0.0D, "marea", List.of(low, high)).orElseThrow());
    }

    @Test
    void invalidInputsFailClosed() {
        var candidates = List.of(candidate(
                ALPHA_NEAR, "marea", 2.0D, 0.0D, WildSocialRole.ALPHA, true));

        assertTrue(WildCalmAlphaCohesionNavigationRuntime.deterministicAlphaAnchorIdentity(
                null, 0.0D, 0.0D, "marea", candidates).isEmpty());
        assertTrue(WildCalmAlphaCohesionNavigationRuntime.deterministicAlphaAnchorIdentity(
                SELF, Double.NaN, 0.0D, "marea", candidates).isEmpty());
        assertTrue(WildCalmAlphaCohesionNavigationRuntime.deterministicAlphaAnchorIdentity(
                SELF, 0.0D, 0.0D, "", candidates).isEmpty());
        assertTrue(WildCalmAlphaCohesionNavigationRuntime.deterministicAlphaAnchorIdentity(
                SELF, 0.0D, 0.0D, "marea", null).isEmpty());
    }

    private static WildCalmAlphaCohesionNavigationRuntime.AlphaCandidate candidate(
            UUID actorId,
            String populationKey,
            double x,
            double z,
            WildSocialRole role,
            boolean active
    ) {
        return new WildCalmAlphaCohesionNavigationRuntime.AlphaCandidate(
                actorId, populationKey, x, z, role, active);
    }
}
