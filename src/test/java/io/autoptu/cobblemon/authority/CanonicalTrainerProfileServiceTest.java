package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CanonicalTrainerProfileServiceTest {
    @TempDir Path root;

    @Test
    void defaultsPersistAndReopen() {
        var service = new CanonicalTrainerProfileService(new FileCanonicalTrainerProfileRepository(root));
        var initial = service.inspect("player-a");
        assertEquals("rookie", initial.titleId());
        assertEquals("classic", initial.cardThemeId());
        assertEquals(0L, initial.revision());

        service.chooseTitle("player-a", "field_researcher");
        service.chooseCardTheme("player-a", "cedar");

        var reopened = new CanonicalTrainerProfileService(new FileCanonicalTrainerProfileRepository(root)).inspect("player-a");
        assertEquals("field_researcher", reopened.titleId());
        assertEquals("cedar", reopened.cardThemeId());
        assertEquals(2L, reopened.revision());
    }

    @Test
    void choicesAreServerAuthoredAndOwnerScoped() {
        var service = new CanonicalTrainerProfileService(new FileCanonicalTrainerProfileRepository(root));
        service.chooseTitle("player-a", "league_challenger");
        assertEquals("rookie", service.inspect("player-b").titleId());
        assertThrows(IllegalArgumentException.class, () -> service.chooseTitle("player-a", "admin"));
        assertThrows(IllegalArgumentException.class, () -> service.chooseCardTheme("player-a", "client-supplied-theme"));
    }

    @Test
    void staleRevisionFailsClosedAndRepeatIsIdempotent() {
        var repository = new FileCanonicalTrainerProfileRepository(root);
        var service = new CanonicalTrainerProfileService(repository);
        var first = service.chooseTitle("player-a", "field_researcher");
        var repeat = service.chooseTitle("player-a", "field_researcher");
        assertEquals(first.revision(), repeat.revision());

        var staleReplacement = new FileCanonicalTrainerProfileRepository.TrainerProfile(
                "player-a", "rookie", "classic", first.revision() + 1);
        assertTrue(repository.replaceIfRevision(staleReplacement, first.revision()));
        assertEquals("rookie", service.inspect("player-a").titleId());
    }
}
