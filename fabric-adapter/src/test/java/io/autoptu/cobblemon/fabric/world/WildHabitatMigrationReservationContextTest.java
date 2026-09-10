package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildHabitatMigrationReservationContextTest {
    @Test
    void interactionActiveWildContributesMigrationHabitatContext() {
        assertTrue(WildHabitatMigrationContextRuntime.contributesToMigrationContext(true));
    }

    @Test
    void reservedWildDoesNotContributeMigrationHabitatContext() {
        assertFalse(WildHabitatMigrationContextRuntime.contributesToMigrationContext(false));
    }
}
