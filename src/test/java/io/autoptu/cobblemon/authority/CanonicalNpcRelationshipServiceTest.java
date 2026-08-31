package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CanonicalNpcRelationshipServiceTest {
    @TempDir Path tempDir;

    @Test
    void firstAuthoredContactPersistsAndReplayIsIdempotentAcrossRepositoryReopen() {
        var firstRepository = new FileCanonicalNpcRelationshipRepository(tempDir);
        var firstService = new CanonicalNpcRelationshipService(CanonicalNpcDialogueCatalogue.DEFAULT, firstRepository);

        var first = firstService.observeContact("player-1", "cedar-ranger");
        assertTrue(first.newlyMet());
        assertTrue(first.relationship().met());
        assertEquals(0, first.relationship().reputation());
        assertEquals(1L, first.relationship().revision());

        var reopened = new FileCanonicalNpcRelationshipRepository(tempDir);
        var replay = new CanonicalNpcRelationshipService(CanonicalNpcDialogueCatalogue.DEFAULT, reopened)
                .observeContact("player-1", "cedar-ranger");
        assertFalse(replay.newlyMet());
        assertEquals(first.relationship(), replay.relationship());
    }

    @Test
    void relationshipStateIsOwnerScoped() {
        var service = new CanonicalNpcRelationshipService(
                CanonicalNpcDialogueCatalogue.DEFAULT,
                new FileCanonicalNpcRelationshipRepository(tempDir));

        service.observeContact("player-a", "cedar-ranger");
        var other = service.inspect("player-b", "cedar-ranger");

        assertFalse(other.met());
        assertEquals(0, other.reputation());
        assertEquals(0L, other.revision());
    }

    @Test
    void unknownClientSuppliedNpcIdentityCannotCreateRelationshipState() {
        var repository = new FileCanonicalNpcRelationshipRepository(tempDir);
        var service = new CanonicalNpcRelationshipService(CanonicalNpcDialogueCatalogue.DEFAULT, repository);

        assertThrows(IllegalArgumentException.class, () -> service.observeContact("player-1", "client:fake-npc"));
        assertTrue(repository.find("player-1", "client:fake-npc").isEmpty());
    }
}
