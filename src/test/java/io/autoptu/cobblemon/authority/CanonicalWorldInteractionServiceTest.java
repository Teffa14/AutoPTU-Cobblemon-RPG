package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CanonicalWorldInteractionServiceTest {
    private final CanonicalWorldInteractionService service = new CanonicalWorldInteractionService(25.0D);

    @Test
    void allowsMatchingAuthoredObjectForCanonicalTrainerInRange() {
        var decision = service.canInteract(new CanonicalWorldInteractionService.Request(
                "player:test",
                true,
                "minecraft:overworld:1:64:1",
                CanonicalWorldInteractionService.Kind.TERMINAL,
                CanonicalWorldInteractionService.Kind.TERMINAL,
                9.0D
        ));
        assertTrue(decision.allowed());
    }

    @Test
    void rejectsUnprovisionedTrainerStaleObjectAndOutOfRangeRequests() {
        assertFalse(service.canInteract(new CanonicalWorldInteractionService.Request(
                "player:test", false, "object", CanonicalWorldInteractionService.Kind.CHEST,
                CanonicalWorldInteractionService.Kind.CHEST, 1.0D)).allowed());
        assertFalse(service.canInteract(new CanonicalWorldInteractionService.Request(
                "player:test", true, "object", CanonicalWorldInteractionService.Kind.CHEST,
                CanonicalWorldInteractionService.Kind.SWITCH, 1.0D)).allowed());
        assertFalse(service.canInteract(new CanonicalWorldInteractionService.Request(
                "player:test", true, "object", CanonicalWorldInteractionService.Kind.CHEST,
                CanonicalWorldInteractionService.Kind.CHEST, 26.0D)).allowed());
    }
}
