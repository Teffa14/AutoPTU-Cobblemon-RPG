package io.autoptu.cobblemon.authority;

import java.util.Map;
import java.util.Set;

public record GateRule(
        ActionKind action,
        String resourceId,
        Set<String> anyTrainerClasses,
        Map<String, Integer> minimumSkillRanks,
        Set<String> anyPokemonCapabilities
) {
    public GateRule {
        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
        }
        if (resourceId == null || resourceId.isBlank()) {
            throw new IllegalArgumentException("resourceId must not be blank");
        }
        anyTrainerClasses = anyTrainerClasses == null ? Set.of() : Set.copyOf(anyTrainerClasses);
        minimumSkillRanks = minimumSkillRanks == null ? Map.of() : Map.copyOf(minimumSkillRanks);
        anyPokemonCapabilities = anyPokemonCapabilities == null ? Set.of() : Set.copyOf(anyPokemonCapabilities);
        minimumSkillRanks.forEach((skill, rank) -> {
            if (skill == null || skill.isBlank() || rank == null || rank < 0) {
                throw new IllegalArgumentException("invalid minimum skill requirement");
            }
        });
    }
}
