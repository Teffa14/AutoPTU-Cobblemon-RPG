package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildCalmHerdAttentionRuntimeTest {
    private static final UUID SELF = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID LOW = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID MID = UUID.fromString("00000000-0000-0000-0000-000000000080");
    private static final UUID HIGH = UUID.fromString("00000000-0000-0000-0000-0000000000ff");

    @Test
    void authoredAlphaWinsInsideCohesionBeforeNearerMember() {
        List<WildCalmHerdAttentionRuntime.AnchorCandidate> candidates = List.of(
                new WildCalmHerdAttentionRuntime.AnchorCandidate(HIGH, 2.0D, 0.0D, WildSocialRole.MEMBER),
                new WildCalmHerdAttentionRuntime.AnchorCandidate(LOW, 8.0D, 0.0D, WildSocialRole.ALPHA));

        assertEquals(LOW, WildCalmHerdAttentionRuntime.deterministicPreferredAnchorIdentity(
                SELF, 0.0D, 0.0D, 10.0D, candidates).orElseThrow());
    }

    @Test
    void alphaWithoutHerdLeaderCapabilityHasNoLeaderPreference() {
        List<WildCalmHerdAttentionRuntime.AnchorCandidate> candidates = List.of(
                new WildCalmHerdAttentionRuntime.AnchorCandidate(
                        LOW, 8.0D, 0.0D, WildSocialRole.ALPHA, false),
                new WildCalmHerdAttentionRuntime.AnchorCandidate(
                        HIGH, 2.0D, 0.0D, WildSocialRole.MEMBER, false));

        assertEquals(HIGH, WildCalmHerdAttentionRuntime.deterministicPreferredAnchorIdentity(
                SELF, 0.0D, 0.0D, 10.0D, candidates).orElseThrow());
    }

    @Test
    void nearestEligibleMemberWinsWhenNoAlphaIsEligible() {
        List<WildCalmHerdAttentionRuntime.AnchorCandidate> candidates = List.of(
                new WildCalmHerdAttentionRuntime.AnchorCandidate(LOW, 8.0D, 0.0D),
                new WildCalmHerdAttentionRuntime.AnchorCandidate(HIGH, 2.0D, 0.0D));

        assertEquals(HIGH, WildCalmHerdAttentionRuntime.deterministicPreferredAnchorIdentity(
                SELF, 0.0D, 0.0D, 10.0D, candidates).orElseThrow());
    }

    @Test
    void outOfCohesionAlphaDoesNotOverrideEligibleMember() {
        List<WildCalmHerdAttentionRuntime.AnchorCandidate> candidates = List.of(
                new WildCalmHerdAttentionRuntime.AnchorCandidate(HIGH, 3.0D, 0.0D, WildSocialRole.MEMBER),
                new WildCalmHerdAttentionRuntime.AnchorCandidate(LOW, 7.0D, 0.0D, WildSocialRole.ALPHA));

        assertEquals(HIGH, WildCalmHerdAttentionRuntime.deterministicPreferredAnchorIdentity(
                SELF, 0.0D, 0.0D, 5.0D, candidates).orElseThrow());
    }

    @Test
    void equalDistanceUsesStableActorIdentityIndependentOfInputOrder() {
        WildCalmHerdAttentionRuntime.AnchorCandidate low =
                new WildCalmHerdAttentionRuntime.AnchorCandidate(LOW, -2.0D, 0.0D);
        WildCalmHerdAttentionRuntime.AnchorCandidate high =
                new WildCalmHerdAttentionRuntime.AnchorCandidate(HIGH, 2.0D, 0.0D);

        assertEquals(LOW, WildCalmHerdAttentionRuntime.deterministicPreferredAnchorIdentity(
                SELF, 0.0D, 0.0D, 10.0D, List.of(high, low)).orElseThrow());
        assertEquals(LOW, WildCalmHerdAttentionRuntime.deterministicPreferredAnchorIdentity(
                SELF, 0.0D, 0.0D, 10.0D, List.of(low, high)).orElseThrow());
    }

    @Test
    void ignoresSelfCoincidentAndOutOfCohesionCandidates() {
        List<WildCalmHerdAttentionRuntime.AnchorCandidate> candidates = List.of(
                new WildCalmHerdAttentionRuntime.AnchorCandidate(SELF, 1.0D, 0.0D),
                new WildCalmHerdAttentionRuntime.AnchorCandidate(MID, 0.0D, 0.0D),
                new WildCalmHerdAttentionRuntime.AnchorCandidate(HIGH, 6.0D, 0.0D),
                new WildCalmHerdAttentionRuntime.AnchorCandidate(LOW, 4.0D, 0.0D));

        assertEquals(LOW, WildCalmHerdAttentionRuntime.deterministicPreferredAnchorIdentity(
                SELF, 0.0D, 0.0D, 5.0D, candidates).orElseThrow());
    }

    @Test
    void invalidOrEmptyInputsHaveNoPresentationAnchor() {
        assertTrue(WildCalmHerdAttentionRuntime.deterministicPreferredAnchorIdentity(
                SELF, 0.0D, 0.0D, 5.0D, List.of()).isEmpty());
        assertTrue(WildCalmHerdAttentionRuntime.deterministicPreferredAnchorIdentity(
                SELF, 0.0D, 0.0D, 5.0D, null).isEmpty());
        assertTrue(WildCalmHerdAttentionRuntime.deterministicPreferredAnchorIdentity(
                SELF, 0.0D, 0.0D, 0.0D, List.of(
                        new WildCalmHerdAttentionRuntime.AnchorCandidate(LOW, 1.0D, 0.0D))).isEmpty());
        assertTrue(WildCalmHerdAttentionRuntime.deterministicPreferredAnchorIdentity(
                SELF, Double.NaN, 0.0D, 5.0D, List.of(
                        new WildCalmHerdAttentionRuntime.AnchorCandidate(LOW, 1.0D, 0.0D))).isEmpty());
    }
}
