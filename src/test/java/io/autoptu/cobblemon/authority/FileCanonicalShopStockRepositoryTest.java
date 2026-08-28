package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FileCanonicalShopStockRepositoryTest {
    @TempDir Path temp;

    @Test
    void authoredStockIsProvisionedOnceAndSurvivesRepositoryReopen() {
        FileCanonicalShopStockRepository first = new FileCanonicalShopStockRepository(temp);
        var initial = first.getOrCreate("cedar-mart", "field-ration", 12);
        assertEquals(12, initial.remainingStock());
        assertEquals(0, initial.revision());

        assertTrue(first.replaceIfRevision("cedar-mart", "field-ration", 0, 9));

        FileCanonicalShopStockRepository reopened = new FileCanonicalShopStockRepository(temp);
        var persisted = reopened.getOrCreate("cedar-mart", "field-ration", 12);
        assertEquals(9, persisted.remainingStock());
        assertEquals(1, persisted.revision());
    }

    @Test
    void staleRevisionAndReplenishmentFailClosed() {
        FileCanonicalShopStockRepository repository = new FileCanonicalShopStockRepository(temp);
        repository.getOrCreate("cedar-mart", "basic-bandage", 8);
        assertTrue(repository.replaceIfRevision("cedar-mart", "basic-bandage", 0, 6));
        assertFalse(repository.replaceIfRevision("cedar-mart", "basic-bandage", 0, 5));
        assertThrows(IllegalArgumentException.class,
                () -> repository.replaceIfRevision("cedar-mart", "basic-bandage", 1, 7));
    }

    @Test
    void persistedStockCannotExceedCurrentAuthoredLimit() {
        FileCanonicalShopStockRepository repository = new FileCanonicalShopStockRepository(temp);
        repository.getOrCreate("cedar-mart", "revive-kit", 4);
        assertThrows(IllegalStateException.class,
                () -> repository.getOrCreate("cedar-mart", "revive-kit", 3));
    }

    @Test
    void shopQueryProjectsDurableRemainingStock() {
        FileCanonicalShopStockRepository repository = new FileCanonicalShopStockRepository(temp);
        CanonicalShopQueryService service = new CanonicalShopQueryService(CanonicalShopCatalogue.DEFAULT, repository);

        var first = service.inspectShop("player-1", "cedar-mart");
        assertEquals(12, first.offers().get(0).remainingStock());
        assertTrue(repository.replaceIfRevision("cedar-mart", "field-ration", 0, 10));

        var second = service.inspectShop("player-1", "cedar-mart");
        assertEquals(10, second.offers().get(0).remainingStock());
        assertEquals(1, second.offers().get(0).stockRevision());
    }
}
