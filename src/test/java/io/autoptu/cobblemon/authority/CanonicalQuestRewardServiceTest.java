package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CanonicalQuestRewardServiceTest {
    @TempDir Path tempDir;

    @Test
    void requiresAcceptedQuestAndAllServerOwnedObjectives() {
        var stores = stores();
        var rewards = service(stores);

        var beforeAccept = rewards.claim("player-1", "cedar-field-notes");
        assertEquals(CanonicalQuestRewardService.Status.QUEST_NOT_ACCEPTED, beforeAccept.status());
        assertEquals(0L, stores.wallets.findOrCreate("player-1").balance());

        accept(stores, "player-1");
        var incomplete = rewards.claim("player-1", "cedar-field-notes");
        assertEquals(CanonicalQuestRewardService.Status.OBJECTIVES_INCOMPLETE, incomplete.status());
        assertEquals(0L, stores.wallets.findOrCreate("player-1").balance());

        observeAll(stores, "player-1");
        var applied = rewards.claim("player-1", "cedar-field-notes");
        assertEquals(CanonicalQuestRewardService.Status.APPLIED, applied.status());
        assertTrue(applied.committed());
        assertTrue(applied.newlyApplied());
        assertEquals(240L, applied.reward().amount());
        assertEquals(240L, applied.transaction().balance());
    }

    @Test
    void repeatedClaimAndRepositoryReopenCannotDoubleCredit() {
        var firstStores = stores();
        accept(firstStores, "player-2");
        observeAll(firstStores, "player-2");

        var first = service(firstStores).claim("player-2", "cedar-field-notes");
        var repeated = service(firstStores).claim("player-2", "cedar-field-notes");
        assertEquals(CanonicalQuestRewardService.Status.APPLIED, first.status());
        assertEquals(CanonicalQuestRewardService.Status.ALREADY_APPLIED, repeated.status());
        assertFalse(repeated.newlyApplied());
        assertEquals(240L, firstStores.wallets.findOrCreate("player-2").balance());

        var reopened = stores();
        var afterRestart = service(reopened).claim("player-2", "cedar-field-notes");
        assertEquals(CanonicalQuestRewardService.Status.ALREADY_APPLIED, afterRestart.status());
        assertEquals(240L, reopened.wallets.findOrCreate("player-2").balance());
    }

    @Test
    void unknownQuestRewardFailsClosedWithoutWalletMutation() {
        var stores = stores();
        var result = service(stores).claim("player-3", "client-invented-quest");
        assertEquals(CanonicalQuestRewardService.Status.NO_AUTHORED_REWARD, result.status());
        assertEquals(0L, stores.wallets.findOrCreate("player-3").balance());
    }

    private Stores stores() {
        return new Stores(
                new FileCanonicalQuestJournalRepository(tempDir),
                new FileCanonicalQuestObjectiveRepository(tempDir),
                new FileCanonicalWalletRepository(tempDir)
        );
    }

    private static CanonicalQuestRewardService service(Stores stores) {
        return new CanonicalQuestRewardService(
                CanonicalQuestRewardCatalogue.DEFAULT,
                stores.journals,
                new CanonicalQuestObjectiveService(CanonicalQuestObjectiveCatalogue.DEFAULT, stores.journals, stores.objectives),
                stores.wallets
        );
    }

    private static void accept(Stores stores, String playerId) {
        new CanonicalQuestJournalService(CanonicalQuestCatalogue.DEFAULT, stores.journals)
                .accept(playerId, "cedar-ranger", "cedar-field-notes");
    }

    private static void observeAll(Stores stores, String playerId) {
        var service = new CanonicalQuestObjectiveService(CanonicalQuestObjectiveCatalogue.DEFAULT, stores.journals, stores.objectives);
        service.observe(playerId, "cedar_meadow:lookout_watching");
        service.observe(playerId, "cedar_meadow:feeders_alarmed");
    }

    private record Stores(
            FileCanonicalQuestJournalRepository journals,
            FileCanonicalQuestObjectiveRepository objectives,
            FileCanonicalWalletRepository wallets
    ) {}
}
