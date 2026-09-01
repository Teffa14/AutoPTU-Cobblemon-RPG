package io.autoptu.cobblemon.fabric.presentation;

import io.autoptu.cobblemon.battlecore.AuthoritativeMoveMetadata;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthoritativeCobblemonMoveAnimationResolverTest {
    private static AuthoritativeMoveMetadata metadata(String moveId, String category) {
        return new AuthoritativeMoveMetadata(
                moveId,
                new AuthoritativeMoveMetadata.Targeting(
                        "combatant", "ranged", 6, 6, null, null, "Ranged 6", List.of()
                ),
                "standard",
                true,
                category == null ? null : new AuthoritativeMoveMetadata.Combat(2, 6, 20, category, "normal"),
                "At-Will"
        );
    }

    @Test
    void mapsOnlyExplicitAuthoritativePhysicalAndSpecialCategories() {
        Map<String, AuthoritativeMoveMetadata> moves = Map.of(
                "tackle", metadata("tackle", "physical"),
                "water-pulse", metadata("water-pulse", "special")
        );
        var resolver = new AuthoritativeCobblemonMoveAnimationResolver(
                moveId -> Optional.ofNullable(moves.get(moveId))
        );

        assertEquals(Optional.of("physical"), resolver.resolve("tackle"));
        assertEquals(Optional.of("special"), resolver.resolve("water-pulse"));
    }

    @Test
    void leavesNonDamageMoveUnresolvedInsteadOfGuessingStatus() {
        var resolver = new AuthoritativeCobblemonMoveAnimationResolver(
                moveId -> Optional.of(metadata(moveId, null))
        );

        assertTrue(resolver.resolve("growl").isEmpty());
    }

    @Test
    void rejectsCatalogIdentityMismatchByFallingBack() {
        var resolver = new AuthoritativeCobblemonMoveAnimationResolver(
                moveId -> Optional.of(metadata("different-move", "physical"))
        );

        assertTrue(resolver.resolve("tackle").isEmpty());
    }
}
