package io.autoptu.cobblemon.authority;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

/** Server-owned initial catalogue for capability-sensitive Ouros world tasks. */
public final class WorldTaskCatalogue {
    private final Map<String, WorldTaskDefinition> tasks;

    public WorldTaskCatalogue() {
        this.tasks = Map.of(
                "field_ration", new WorldTaskDefinition(
                        "field_ration",
                        "Field Ration",
                        "Survival",
                        0,
                        curve(
                                0, quality(70, 25, 5),
                                2, quality(45, 40, 15),
                                4, quality(20, 50, 30),
                                6, quality(5, 50, 45)
                        )
                ),
                "precision_poketech_parts", new WorldTaskDefinition(
                        "precision_poketech_parts",
                        "Precision Poketech Parts",
                        "Technology Education",
                        1,
                        curve(
                                0, quality(100, 0, 0),
                                1, quality(65, 30, 5),
                                3, quality(35, 45, 20),
                                5, quality(15, 50, 35),
                                7, quality(5, 45, 50)
                        )
                ),
                "occult_lure", new WorldTaskDefinition(
                        "occult_lure",
                        "Occult Lure",
                        "Occult Education",
                        1,
                        curve(
                                0, quality(100, 0, 0),
                                1, quality(70, 25, 5),
                                3, quality(40, 45, 15),
                                5, quality(20, 50, 30),
                                7, quality(5, 50, 45)
                        )
                )
        );
    }

    public Optional<WorldTaskDefinition> find(String taskId) {
        if (taskId == null || taskId.isBlank()) return Optional.empty();
        return Optional.ofNullable(tasks.get(taskId.trim().toLowerCase()));
    }

    public List<WorldTaskDefinition> all() {
        return tasks.values().stream()
                .sorted(java.util.Comparator.comparing(WorldTaskDefinition::taskId))
                .toList();
    }

    private static WorldTaskDefinition.QualityDistribution quality(
            int improvised,
            int standard,
            int excellent
    ) {
        return new WorldTaskDefinition.QualityDistribution(improvised, standard, excellent);
    }

    private static NavigableMap<Integer, WorldTaskDefinition.QualityDistribution> curve(Object... values) {
        if (values.length == 0 || values.length % 2 != 0) {
            throw new IllegalArgumentException("curve requires rank/distribution pairs");
        }
        TreeMap<Integer, WorldTaskDefinition.QualityDistribution> curve = new TreeMap<>();
        for (int index = 0; index < values.length; index += 2) {
            curve.put(
                    (Integer) values[index],
                    (WorldTaskDefinition.QualityDistribution) values[index + 1]
            );
        }
        return curve;
    }
}
