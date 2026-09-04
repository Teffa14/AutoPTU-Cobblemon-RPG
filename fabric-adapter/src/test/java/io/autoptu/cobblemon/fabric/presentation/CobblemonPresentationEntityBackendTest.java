package io.autoptu.cobblemon.fabric.presentation;

import io.autoptu.cobblemon.battlecore.BattlePresentationCommand;
import io.autoptu.cobblemon.battlecore.PresentationEntityPlatformBackend;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CobblemonPresentationEntityBackendTest {
    @Test
    void implementsPresentationOnlyPlatformBoundary() {
        assertTrue(PresentationEntityPlatformBackend.class
                .isAssignableFrom(CobblemonPresentationEntityBackend.class));

        var methodNames = Arrays.stream(CobblemonPresentationEntityBackend.class.getDeclaredMethods())
                .map(Method::getName)
                .filter(name -> !name.startsWith("lambda$"))
                .sorted()
                .toList();

        assertTrue(methodNames.contains("relocate"));
        assertTrue(methodNames.contains("animateMove"));
        assertTrue(methodNames.contains("projectDisplayedHealth"));
        assertTrue(methodNames.contains("authoritativeHpNameplate"));
        assertTrue(methodNames.contains("authoritativeFlag"));
        assertTrue(methodNames.contains("showCue"));
        assertTrue(methodNames.contains("statusSkipText"));
        assertTrue(methodNames.contains("renderMelee"));
        assertTrue(methodNames.contains("renderProjectile"));
        assertTrue(methodNames.contains("renderBeam"));
        assertTrue(methodNames.contains("renderWave"));
        assertTrue(methodNames.contains("renderBurst"));
        assertTrue(methodNames.contains("renderArc"));
        assertTrue(methodNames.contains("renderAura"));
        assertTrue(methodNames.contains("renderMiss"));
        assertTrue(methodNames.contains("renderCritical"));
    }

    @Test
    void authoritativeHpNameplateMirrorsExactEngineHpWithoutInference() {
        assertEquals("PTU HP 37", CobblemonPresentationEntityBackend.authoritativeHpNameplate(37));
        assertEquals("PTU HP 0", CobblemonPresentationEntityBackend.authoritativeHpNameplate(0));
        assertThrows(
                IllegalArgumentException.class,
                () -> CobblemonPresentationEntityBackend.authoritativeHpNameplate(-1)
        );
    }

    @Test
    void moveOutcomeFlagsAreCopiedOnlyFromAuthoritativeCommand() {
        BattlePresentationCommand command = new BattlePresentationCommand(
                10,
                0,
                BattlePresentationCommand.Kind.MOVE_ANIMATION,
                "pokemon-1",
                Map.of(
                        "targetId", "pokemon-2",
                        "moveId", "Thunderbolt",
                        "hit", "true",
                        "crit", "false"
                )
        );

        assertTrue(CobblemonPresentationEntityBackend.authoritativeFlag(command, "hit"));
        assertFalse(CobblemonPresentationEntityBackend.authoritativeFlag(command, "crit"));
    }

    @Test
    void missingOrNonBooleanMoveOutcomeCannotBeInventedByPresentation() {
        BattlePresentationCommand missing = new BattlePresentationCommand(
                10,
                0,
                BattlePresentationCommand.Kind.MOVE_ANIMATION,
                "pokemon-1",
                Map.of("targetId", "pokemon-2", "moveId", "Tackle")
        );
        BattlePresentationCommand malformed = new BattlePresentationCommand(
                11,
                0,
                BattlePresentationCommand.Kind.MOVE_ANIMATION,
                "pokemon-1",
                Map.of("targetId", "pokemon-2", "moveId", "Tackle", "hit", "maybe")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> CobblemonPresentationEntityBackend.authoritativeFlag(missing, "hit")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> CobblemonPresentationEntityBackend.authoritativeFlag(malformed, "hit")
        );
    }

    @Test
    void statusSkipTextMirrorsOnlyAuthoritativeCueFields() {
        BattlePresentationCommand cue = new BattlePresentationCommand(
                12,
                0,
                BattlePresentationCommand.Kind.STATUS_SKIP_CUE,
                "wild-1",
                Map.of(
                        "status", "sleep",
                        "phase", "action",
                        "reason", "cannot_act"
                )
        );

        assertEquals(
                "sleep · action · cannot_act",
                CobblemonPresentationEntityBackend.statusSkipText(cue)
        );
    }

    @Test
    void statusSkipTextKeepsMissingOptionalTextPresentationOnly() {
        BattlePresentationCommand cue = new BattlePresentationCommand(
                13,
                0,
                BattlePresentationCommand.Kind.STATUS_SKIP_CUE,
                "wild-1",
                Map.of("phase", "action")
        );

        assertEquals(
                "status · action · authoritative skip",
                CobblemonPresentationEntityBackend.statusSkipText(cue)
        );
    }

    @Test
    void statusSkipTextRejectsNonStatusCue() {
        BattlePresentationCommand cue = new BattlePresentationCommand(
                14,
                0,
                BattlePresentationCommand.Kind.TURN_START_CUE,
                "wild-1",
                Map.of("phase", "action")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> CobblemonPresentationEntityBackend.statusSkipText(cue)
        );
    }
}
