package io.autoptu.cobblemon.fabric.presentation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattleMoveAnimationProfileTest {
    @Test
    void commonMovesResolveToDistinctVisibleChoreographies() {
        assertEquals(
                new BattleMoveAnimationProfile(
                        BattleMoveAnimationProfile.Motion.MELEE,
                        BattleMoveAnimationProfile.Theme.NORMAL
                ),
                BattleMoveAnimationProfile.resolve("tackle")
        );
        assertEquals(
                new BattleMoveAnimationProfile(
                        BattleMoveAnimationProfile.Motion.PROJECTILE,
                        BattleMoveAnimationProfile.Theme.FIRE
                ),
                BattleMoveAnimationProfile.resolve("ember")
        );
        assertEquals(
                new BattleMoveAnimationProfile(
                        BattleMoveAnimationProfile.Motion.BEAM,
                        BattleMoveAnimationProfile.Theme.ELECTRIC
                ),
                BattleMoveAnimationProfile.resolve("thunderbolt")
        );
        assertEquals(
                new BattleMoveAnimationProfile(
                        BattleMoveAnimationProfile.Motion.WAVE,
                        BattleMoveAnimationProfile.Theme.WATER
                ),
                BattleMoveAnimationProfile.resolve("surf")
        );
        assertEquals(
                new BattleMoveAnimationProfile(
                        BattleMoveAnimationProfile.Motion.BURST,
                        BattleMoveAnimationProfile.Theme.NORMAL
                ),
                BattleMoveAnimationProfile.resolve("explosion")
        );
    }

    @Test
    void namespacedAndFormattedMoveIdsNormalizeBeforePresentationSelection() {
        assertEquals(
                BattleMoveAnimationProfile.resolve("watergun"),
                BattleMoveAnimationProfile.resolve("cobblemon:Water_Gun")
        );
        assertEquals(
                BattleMoveAnimationProfile.resolve("icebeam"),
                BattleMoveAnimationProfile.resolve("Ice Beam")
        );
    }

    @Test
    void unknownCustomMovesAlwaysReceiveAnimatedFallback() {
        BattleMoveAnimationProfile profile = BattleMoveAnimationProfile.resolve("ouros:unknown_custom_move");
        assertNotNull(profile);
        assertEquals(BattleMoveAnimationProfile.Motion.ARC, profile.motion());
        assertEquals(BattleMoveAnimationProfile.Theme.NORMAL, profile.theme());
    }

    @Test
    void blankMoveIdFailsClosedInsteadOfCreatingPresentationMeaning() {
        assertThrows(IllegalArgumentException.class, () -> BattleMoveAnimationProfile.resolve("  "));
        assertThrows(IllegalArgumentException.class, () -> BattleMoveAnimationProfile.resolve(null));
    }
}
