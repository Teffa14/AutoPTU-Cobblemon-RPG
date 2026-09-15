package io.autoptu.cobblemon.battlecore;

import java.util.List;
import java.util.Objects;

/** Curated practice setup, never a source of client-side action legality. */
public record BattlePracticeScenario(String id, String name, String description, int width, int height,
        BattleGridCoordinate allyStart, BattleGridCoordinate enemyStart, int hp, int speed, RivalStyle rival) {
    public enum RivalStyle { BALANCED, POWER, PRECISION }

    public static final List<BattlePracticeScenario> CATALOG = List.of(
            new BattlePracticeScenario("training", "Training field", "Learn movement, previews and turn passing.",
                    7, 4, new BattleGridCoordinate(1, 1), new BattleGridCoordinate(5, 1), 60, 3, RivalStyle.BALANCED),
            new BattlePracticeScenario("duel", "Close quarters", "A compact duel against a rival that favors powerful attacks.",
                    5, 5, new BattleGridCoordinate(1, 2), new BattleGridCoordinate(3, 2), 80, 2, RivalStyle.POWER),
            new BattlePracticeScenario("distance", "Long approach", "Cross an open field against a precision-focused rival.",
                    10, 6, new BattleGridCoordinate(1, 2), new BattleGridCoordinate(8, 3), 80, 3, RivalStyle.PRECISION),
            new BattlePracticeScenario("endurance", "Endurance arena", "A longer match with room to reposition between exchanges.",
                    9, 7, new BattleGridCoordinate(1, 1), new BattleGridCoordinate(7, 5), 120, 4, RivalStyle.BALANCED));

    public BattlePracticeScenario {
        Objects.requireNonNull(id);
        Objects.requireNonNull(name);
        Objects.requireNonNull(description);
        Objects.requireNonNull(allyStart);
        Objects.requireNonNull(enemyStart);
        Objects.requireNonNull(rival);
        if (!id.matches("[a-z0-9_-]{1,32}") || name.isBlank() || name.length() > 80 || description.length() > 256) {
            throw new IllegalArgumentException("invalid scenario identity");
        }
        if (width < 3 || height < 3 || width > 12 || height > 12 || hp < 1 || hp > 999 || speed < 1 || speed > 8) {
            throw new IllegalArgumentException("invalid practice dimensions or stats");
        }
        if (!inside(allyStart, width, height) || !inside(enemyStart, width, height) || allyStart.equals(enemyStart)) {
            throw new IllegalArgumentException("invalid starting cells");
        }
    }

    public static BattlePracticeScenario find(String id) {
        return CATALOG.stream().filter(scenario -> scenario.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown practice scenario: " + id));
    }

    private static boolean inside(BattleGridCoordinate coordinate, int width, int height) {
        return coordinate.x() >= 0 && coordinate.y() >= 0 && coordinate.x() < width && coordinate.y() < height;
    }
}
