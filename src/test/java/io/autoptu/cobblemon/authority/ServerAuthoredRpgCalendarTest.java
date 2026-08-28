package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ServerAuthoredRpgCalendarTest {
    private final ServerAuthoredRpgCalendar calendar = new ServerAuthoredRpgCalendar();

    @Test
    void projectsMonotonicDayIntoStableCalendarCoordinates() {
        var first = calendar.snapshot(0);
        assertEquals(1, first.year());
        assertEquals("Verdant", first.seasonName());
        assertEquals(1, first.weekOfSeason());
        assertEquals(1, first.dayOfSeason());
        assertEquals("Moonday", first.weekdayName());

        var nextSeason = calendar.snapshot(ServerAuthoredRpgCalendar.DAYS_PER_SEASON);
        assertEquals(1, nextSeason.year());
        assertEquals("Highsun", nextSeason.seasonName());
        assertEquals(1, nextSeason.dayOfSeason());

        var nextYear = calendar.snapshot(ServerAuthoredRpgCalendar.DAYS_PER_YEAR);
        assertEquals(2, nextYear.year());
        assertEquals("Verdant", nextYear.seasonName());
        assertEquals(1, nextYear.dayOfSeason());
    }

    @Test
    void exposesOnlyDeterministicServerAuthoredWorldEventKeys() {
        assertEquals(List.of("ouros:season_open"), keys(calendar.snapshot(0)));
        assertEquals(List.of("ouros:starday_market"), keys(calendar.snapshot(5)));
        assertEquals(List.of("ouros:field_research_day"), keys(calendar.snapshot(13)));

        var overlapping = calendar.snapshot(117);
        assertEquals(List.of("ouros:season_open"), keys(overlapping));
    }

    @Test
    void rejectsNegativeDayIdentity() {
        assertThrows(IllegalArgumentException.class, () -> calendar.snapshot(-1));
    }

    private static List<String> keys(ServerAuthoredRpgCalendar.Snapshot snapshot) {
        return snapshot.activeEvents().stream().map(ServerAuthoredRpgCalendar.Event::key).toList();
    }
}
