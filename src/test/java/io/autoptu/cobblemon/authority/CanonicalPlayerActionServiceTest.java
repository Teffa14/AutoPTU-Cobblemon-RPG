package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class CanonicalPlayerActionServiceTest {
    private final CanonicalPlayerActionService service = new CanonicalPlayerActionService();

    @Test
    void allowsServerObservedMinecraftActionForProvisionedTrainer() {
        CanonicalPlayerActionService.Decision decision = service.canPerform(new CanonicalPlayerActionService.Request(
                "player:test",
                true,
                ActionKind.INTERACT,
                "party_quick_access",
                true
        ));

        assertTrue(decision.allowed());
        assertEquals(ActionKind.INTERACT, decision.action());
        assertEquals("party_quick_access", decision.contextId());
    }

    @Test
    void rejectsMissingCanonicalTrainer() {
        CanonicalPlayerActionService.Decision decision = service.canPerform(new CanonicalPlayerActionService.Request(
                "player:test",
                false,
                ActionKind.INTERACT,
                "party_quick_access",
                true
        ));

        assertFalse(decision.allowed());
        assertEquals("canonical Trainer is not provisioned", decision.reason());
    }

    @Test
    void rejectsContextNotObservedByServer() {
        CanonicalPlayerActionService.Decision decision = service.canPerform(new CanonicalPlayerActionService.Request(
                "player:test",
                true,
                ActionKind.INTERACT,
                "party_quick_access",
                false
        ));

        assertFalse(decision.allowed());
        assertEquals("action context was not observed by the server", decision.reason());
    }

    @Test
    void rejectsMissingActionOrContextIdentity() {
        assertFalse(service.canPerform(new CanonicalPlayerActionService.Request(
                "player:test", true, null, "party_quick_access", true)).allowed());
        assertFalse(service.canPerform(new CanonicalPlayerActionService.Request(
                "player:test", true, ActionKind.INTERACT, "", true)).allowed());
    }
}
