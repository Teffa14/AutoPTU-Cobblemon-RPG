package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class NativeDuelInvitationsTest {
    final UUID first = UUID.randomUUID(), second = UUID.randomUUID(), other = UUID.randomUUID();
    final NativeDuelInvitations invites = new NativeDuelInvitations();

    @Test void onlyRecipientCanAcceptExactTokenOnce() {
        var invite = invites.invite(first, second, 0);
        assertTrue(invites.accept(first, invite.token(), 1).isEmpty());
        assertTrue(invites.accept(other, invite.token(), 1).isEmpty());
        assertTrue(invites.accept(second, UUID.randomUUID(), 1).isEmpty());
        assertEquals(invite, invites.accept(second, invite.token(), 2).orElseThrow());
        assertTrue(invites.accept(second, invite.token(), 3).isEmpty());
        assertTrue(invites.find(first, 3).isEmpty());
    }

    @Test void invitationExpiresExactlyAtDeadlineAndReleasesBoth() {
        var invite = invites.invite(first, second, 10);
        assertTrue(invites.find(first, 60_009).isPresent());
        assertTrue(invites.accept(second, invite.token(), 60_010).isEmpty());
        assertTrue(invites.find(first, 60_010).isEmpty());
        assertDoesNotThrow(() -> invites.invite(other, second, 60_010));
    }

    @Test void cannotOverwriteOrStackInvitationsOrChallengeYourself() {
        assertThrows(IllegalArgumentException.class, () -> invites.invite(first, first, 0));
        invites.invite(first, second, 0);
        assertThrows(IllegalStateException.class, () -> invites.invite(other, second, 1));
        assertThrows(IllegalStateException.class, () -> invites.invite(second, other, 1));
        assertThrows(IllegalStateException.class, () -> invites.invite(first, other, 1));
    }

    @Test void cancellationInvalidatesTokenAndCooldownSurvivesCancellation() {
        var old = invites.invite(first, second, 0);
        assertTrue(invites.cancel(other).isEmpty());
        assertEquals(old, invites.cancel(second).orElseThrow());
        assertTrue(invites.find(first, 1).isEmpty());
        assertThrows(IllegalStateException.class, () -> invites.invite(first, second, 1));
        var next = invites.invite(first, second, 10_000);
        assertNotEquals(old.token(), next.token());
        assertTrue(invites.accept(second, old.token(), 10_001).isEmpty());
        assertEquals(next, invites.accept(second, next.token(), 10_001).orElseThrow());
    }

    @Test void serverStopRemovesEveryInvitationAndCooldown() {
        invites.invite(first, second, 0);
        invites.clear();
        assertTrue(invites.find(first, 1).isEmpty());
        assertTrue(invites.find(second, 1).isEmpty());
        assertDoesNotThrow(() -> invites.invite(first, second, 1));
    }
}
