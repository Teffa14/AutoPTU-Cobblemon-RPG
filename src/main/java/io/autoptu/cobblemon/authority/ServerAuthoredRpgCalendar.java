package io.autoptu.cobblemon.authority;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic Ouros calendar projection over the durable monotonic RPG day identity.
 *
 * <p>This is server-authored world content. It provides stable calendar/event keys for Minecraft
 * world systems and presentation only. It does not define PTU Daily/Scene/Encounter frequencies,
 * Trainer Feature costs, battle rounds, legality, RNG, or outcomes.</p>
 */
public final class ServerAuthoredRpgCalendar {
    public static final int DAYS_PER_WEEK = 7;
    public static final int WEEKS_PER_SEASON = 4;
    public static final int DAYS_PER_SEASON = DAYS_PER_WEEK * WEEKS_PER_SEASON;
    public static final int SEASONS_PER_YEAR = 4;
    public static final int DAYS_PER_YEAR = DAYS_PER_SEASON * SEASONS_PER_YEAR;

    private static final List<String> WEEKDAY_NAMES = List.of(
            "Moonday", "Tideday", "Emberday", "Bloomday", "Skyday", "Starday", "Sunday");
    private static final List<String> SEASON_NAMES = List.of(
            "Verdant", "Highsun", "Harvest", "Frostfall");

    public Snapshot snapshot(long rpgDayId) {
        if (rpgDayId < 0) throw new IllegalArgumentException("rpgDayId must be non-negative");

        long yearIndex = Math.floorDiv(rpgDayId, DAYS_PER_YEAR);
        int dayOfYearIndex = (int) Math.floorMod(rpgDayId, DAYS_PER_YEAR);
        int seasonIndex = dayOfYearIndex / DAYS_PER_SEASON;
        int dayOfSeason = (dayOfYearIndex % DAYS_PER_SEASON) + 1;
        int weekOfSeason = ((dayOfSeason - 1) / DAYS_PER_WEEK) + 1;
        int dayOfWeekIndex = (int) Math.floorMod(rpgDayId, DAYS_PER_WEEK);

        List<Event> activeEvents = new ArrayList<>();
        if (dayOfSeason == 1) {
            activeEvents.add(new Event(
                    "ouros:season_open",
                    "Season Opening",
                    "A new Ouros season begins. World systems may schedule authored seasonal content from this stable hook."));
        }
        if (dayOfSeason == 14) {
            activeEvents.add(new Event(
                    "ouros:field_research_day",
                    "Field Research Day",
                    "Researchers and explorers mark this day for server-authored world activities."));
        }
        if (dayOfWeekIndex == 5) {
            activeEvents.add(new Event(
                    "ouros:starday_market",
                    "Starday Market",
                    "Ouros settlements may attach authored market activity to this recurring calendar hook."));
        }

        return new Snapshot(
                rpgDayId,
                yearIndex + 1,
                seasonIndex + 1,
                SEASON_NAMES.get(seasonIndex),
                weekOfSeason,
                dayOfSeason,
                dayOfWeekIndex + 1,
                WEEKDAY_NAMES.get(dayOfWeekIndex),
                List.copyOf(activeEvents));
    }

    public record Snapshot(
            long rpgDayId,
            long year,
            int season,
            String seasonName,
            int weekOfSeason,
            int dayOfSeason,
            int dayOfWeek,
            String weekdayName,
            List<Event> activeEvents
    ) {
        public Snapshot {
            if (rpgDayId < 0 || year < 1 || season < 1 || weekOfSeason < 1 || dayOfSeason < 1 || dayOfWeek < 1) {
                throw new IllegalArgumentException("invalid calendar snapshot");
            }
            seasonName = Objects.requireNonNull(seasonName, "seasonName");
            weekdayName = Objects.requireNonNull(weekdayName, "weekdayName");
            activeEvents = List.copyOf(Objects.requireNonNull(activeEvents, "activeEvents"));
        }

        public String displayLabel() {
            return "Year " + year + " · " + seasonName + " " + dayOfSeason
                    + " · " + weekdayName + " · RPG Day " + rpgDayId;
        }
    }

    public record Event(String key, String title, String description) {
        public Event {
            key = requireText(key, "key");
            title = requireText(title, "title");
            description = requireText(description, "description");
        }

        private static String requireText(String value, String field) {
            Objects.requireNonNull(value, field);
            if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
            return value;
        }
    }
}
