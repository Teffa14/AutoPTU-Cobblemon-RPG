package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleCoreMoveCatalogProjectionTest {
    private static AuthoritativeMoveMetadata tackle() {
        return new AuthoritativeMoveMetadata(
                "tackle",
                new AuthoritativeMoveMetadata.Targeting(
                        "single", "melee", 1, 1, null, null, "Melee, 1 Target",
                        List.of(" Contact ", "Push")),
                "standard",
                true,
                new AuthoritativeMoveMetadata.Combat(2, 5, 20, "physical", "Normal"),
                "At-Will"
        );
    }

    private static BattleCoreBootstrapProjection bootstrap(List<String> moveIds) {
        return new BattleCoreBootstrapProjection(
                "battle-1", 42L, Set.of("pokemon-1"), Map.of(), Map.of(), Map.of(), Map.of(),
                Map.of("pokemon-1", new BattleCombatantMoveLoadoutProjection("pokemon-1", moveIds))
        );
    }

    @Test
    void resolvesFrozenMoveIdsThroughTrustedCatalogInLoadoutOrder() {
        AuthoritativeMoveMetadata tackle = tackle();
        AuthoritativeMoveMetadata growl = new AuthoritativeMoveMetadata(
                "growl",
                new AuthoritativeMoveMetadata.Targeting("all-foes", "burst", null, null, "burst", 1, "Burst 1"),
                "standard", false, null, "At-Will"
        );
        Map<String, AuthoritativeMoveMetadata> catalogData = Map.of("tackle", tackle, "growl", growl);
        AuthoritativeMoveCatalog catalog = id -> Optional.ofNullable(catalogData.get(id));

        BattleCoreMoveCatalogProjection projection = BattleCoreMoveCatalogProjection.resolve(
                bootstrap(List.of("tackle", "growl")), catalog);

        assertEquals("battle-1", projection.reservationId());
        assertEquals(List.of(tackle, growl), projection.movesByCombatant().get("pokemon-1"));
        assertEquals(List.of("Contact", "Push"),
                projection.movesByCombatant().get("pokemon-1").getFirst().targeting().keywords());
        assertThrows(UnsupportedOperationException.class,
                () -> projection.movesByCombatant().get("pokemon-1").add(tackle));
        assertThrows(UnsupportedOperationException.class,
                () -> projection.movesByCombatant().get("pokemon-1").getFirst().targeting().keywords().add("Pull"));
    }

    @Test
    void legacyTargetingConstructorCarriesNoInventedKeywords() {
        AuthoritativeMoveMetadata.Targeting targeting = new AuthoritativeMoveMetadata.Targeting(
                "single", "melee", 1, 1, null, null, "Melee, 1 Target");
        assertEquals(List.of(), targeting.keywords());
    }

    @Test
    void canonicalKeywordNormalizationMatchesPublicMoveSpecTransportBoundary() {
        AuthoritativeMoveMetadata.Targeting targeting = new AuthoritativeMoveMetadata.Targeting(
                "single", "melee", 1, 1, null, null, "Melee, 1 Target",
                List.of(" Contact ", "", "  ", "Push", "contact"));

        assertEquals(List.of("Contact", "Push", "contact"), targeting.keywords());
    }

    @Test
    void missingCatalogMetadataFailsClosed() {
        assertThrows(IllegalArgumentException.class,
                () -> BattleCoreMoveCatalogProjection.resolve(bootstrap(List.of("tackle")), id -> Optional.empty()));
    }

    @Test
    void mismatchedCatalogIdentityFailsClosed() {
        AuthoritativeMoveMetadata forged = new AuthoritativeMoveMetadata(
                "hyper-beam", tackle().targeting(), "standard", true, tackle().combat(), "Scene x2");
        assertThrows(IllegalArgumentException.class,
                () -> BattleCoreMoveCatalogProjection.resolve(bootstrap(List.of("tackle")), id -> Optional.of(forged)));
    }

    @Test
    void metadataRejectsClientLikeInvalidCombatFields() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuthoritativeMoveMetadata.Combat(2, -1, 20, "physical", "Normal"));
        assertThrows(IllegalArgumentException.class,
                () -> new AuthoritativeMoveMetadata("tackle", tackle().targeting(), "client-custom", true, tackle().combat(), null));
        assertTrue(tackle().requiresLineOfSight());
    }
}
