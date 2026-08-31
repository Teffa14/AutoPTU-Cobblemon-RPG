package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CanonicalMailServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void readAndRewardClaimPersistAcrossReopenAndDoNotDoubleCredit() {
        var service = service();
        var inbox = service.inspect("trainer:alpha");
        assertEquals(1, inbox.messages().size());
        assertFalse(inbox.messages().getFirst().read());
        assertFalse(inbox.messages().getFirst().rewardClaimed());

        var read = service.read("trainer:alpha", "ouros-welcome");
        assertTrue(read.newlyRead());
        assertTrue(read.message().read());

        var firstClaim = service.claimReward("trainer:alpha", "ouros-welcome");
        assertEquals(CanonicalMailService.ClaimStatus.APPLIED, firstClaim.status());
        assertTrue(firstClaim.committed());
        assertEquals(100L, firstClaim.transaction().balance());

        var reopened = service();
        var persisted = reopened.inspect("trainer:alpha").messages().getFirst();
        assertTrue(persisted.read());
        assertTrue(persisted.rewardClaimed());

        var repeated = reopened.claimReward("trainer:alpha", "ouros-welcome");
        assertEquals(CanonicalMailService.ClaimStatus.ALREADY_CLAIMED, repeated.status());
        assertEquals(100L, new FileCanonicalWalletRepository(tempDir).findOrCreate("trainer:alpha").balance());
    }

    @Test
    void clientCannotInventMailOrRewardPayload() {
        var service = service();
        assertThrows(IllegalArgumentException.class, () -> service.read("trainer:alpha", "client-mail"));
        assertThrows(IllegalArgumentException.class, () -> service.claimReward("trainer:alpha", "client-mail"));
        assertEquals(0L, new FileCanonicalWalletRepository(tempDir).findOrCreate("trainer:alpha").balance());
    }

    @Test
    void mailStateIsOwnerScopedAndRepositoryRejectsStaleCas() {
        var repository = new FileCanonicalMailRepository(tempDir);
        var service = new CanonicalMailService(
                CanonicalMailCatalogue.DEFAULT,
                repository,
                new FileCanonicalWalletRepository(tempDir));
        service.read("trainer:alpha", "ouros-welcome");

        assertFalse(service.inspect("trainer:beta").messages().getFirst().read());
        var alpha = repository.find("trainer:alpha").orElseThrow();
        var replacement = new FileCanonicalMailRepository.MailState(
                alpha.playerId(), alpha.readMailIds(), Set.of("ouros-welcome"), alpha.revision() + 1);
        assertTrue(repository.replaceIfRevision(replacement, alpha.revision()));
        assertFalse(repository.replaceIfRevision(
                new FileCanonicalMailRepository.MailState(
                        alpha.playerId(), alpha.readMailIds(), alpha.claimedRewardMailIds(), alpha.revision() + 1),
                alpha.revision()));
    }

    private CanonicalMailService service() {
        return new CanonicalMailService(
                CanonicalMailCatalogue.DEFAULT,
                new FileCanonicalMailRepository(tempDir),
                new FileCanonicalWalletRepository(tempDir));
    }
}
