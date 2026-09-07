package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.ecology.MigrationPhase;
import net.minecraft.particle.ParticleTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildMigrationPhasePresentationRuntimeTest {
    @Test
    void transitionPhasesReceiveAmbientPresentation() {
        assertTrue(WildMigrationPhasePresentationRuntime.shouldProject(MigrationPhase.PREPARING));
        assertTrue(WildMigrationPhasePresentationRuntime.shouldProject(MigrationPhase.DEPARTING));
        assertTrue(WildMigrationPhasePresentationRuntime.shouldProject(MigrationPhase.ARRIVING));
        assertTrue(WildMigrationPhasePresentationRuntime.shouldProject(MigrationPhase.RETURNING));
    }

    @Test
    void hiddenTransitAndStableResidenceDoNotInventAmbientTransitions() {
        assertFalse(WildMigrationPhasePresentationRuntime.shouldProject(MigrationPhase.IN_TRANSIT));
        assertFalse(WildMigrationPhasePresentationRuntime.shouldProject(MigrationPhase.STOPOVER));
        assertFalse(WildMigrationPhasePresentationRuntime.shouldProject(MigrationPhase.SEASONAL_RESIDENCE));
        assertFalse(WildMigrationPhasePresentationRuntime.shouldProject(MigrationPhase.COMPLETE));
        assertFalse(WildMigrationPhasePresentationRuntime.shouldProject(null));
    }

    @Test
    void authoredTransitionPhasesHaveDistinctMinecraftPresentation() {
        assertSame(ParticleTypes.CLOUD,
                WildMigrationPhasePresentationRuntime.particleEffect(MigrationPhase.PREPARING));
        assertSame(ParticleTypes.POOF,
                WildMigrationPhasePresentationRuntime.particleEffect(MigrationPhase.DEPARTING));
        assertSame(ParticleTypes.HAPPY_VILLAGER,
                WildMigrationPhasePresentationRuntime.particleEffect(MigrationPhase.ARRIVING));
        assertSame(ParticleTypes.END_ROD,
                WildMigrationPhasePresentationRuntime.particleEffect(MigrationPhase.RETURNING));
        assertThrows(IllegalArgumentException.class,
                () -> WildMigrationPhasePresentationRuntime.particleEffect(MigrationPhase.IN_TRANSIT));
        assertThrows(IllegalArgumentException.class,
                () -> WildMigrationPhasePresentationRuntime.particleEffect(null));
    }

    @Test
    void preparingIsSubtleAndMovementBoundaryIsStronger() {
        assertEquals(1, WildMigrationPhasePresentationRuntime.particleCount(MigrationPhase.PREPARING));
        assertEquals(3, WildMigrationPhasePresentationRuntime.particleCount(MigrationPhase.DEPARTING));
        assertEquals(3, WildMigrationPhasePresentationRuntime.particleCount(MigrationPhase.ARRIVING));
        assertEquals(3, WildMigrationPhasePresentationRuntime.particleCount(MigrationPhase.RETURNING));
        assertEquals(0, WildMigrationPhasePresentationRuntime.particleCount(MigrationPhase.IN_TRANSIT));
        assertEquals(0, WildMigrationPhasePresentationRuntime.particleCount(null));
    }
}
