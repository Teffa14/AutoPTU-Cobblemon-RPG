package io.autoptu.cobblemon.authority;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PTUGateService {
    private final CanonicalStateRepository stateRepository;
    private final Map<GateKey, GateRule> rules;

    public PTUGateService(CanonicalStateRepository stateRepository, List<GateRule> rules) {
        if (stateRepository == null) {
            throw new IllegalArgumentException("stateRepository must not be null");
        }
        this.stateRepository = stateRepository;
        this.rules = new HashMap<>();
        if (rules != null) {
            for (GateRule rule : rules) {
                GateKey key = new GateKey(rule.action(), rule.resourceId());
                if (this.rules.putIfAbsent(key, rule) != null) {
                    throw new IllegalArgumentException("duplicate gate rule for " + key);
                }
            }
        }
    }

    public AuthorityDecision canPerform(String playerId, ActionKind action, String resourceId) {
        if (playerId == null || playerId.isBlank() || action == null || resourceId == null || resourceId.isBlank()) {
            return AuthorityDecision.deny(List.of("invalid_request"), -1);
        }

        GateRule rule = rules.get(new GateKey(action, resourceId));
        if (rule == null) {
            return AuthorityDecision.deny(List.of("unregistered_action"), -1);
        }

        return stateRepository.findPlayer(playerId)
                .map(state -> evaluate(state, rule))
                .orElseGet(() -> AuthorityDecision.deny(List.of("unknown_player"), -1));
    }

    private AuthorityDecision evaluate(CanonicalPlayerState state, GateRule rule) {
        List<String> reasons = new ArrayList<>();

        if (!rule.anyTrainerClasses().isEmpty()
                && rule.anyTrainerClasses().stream().noneMatch(state.trainerClasses()::contains)) {
            reasons.add("trainer_class_required");
        }

        rule.minimumSkillRanks().forEach((skill, minimum) -> {
            if (state.skillRank(skill) < minimum) {
                reasons.add("skill_rank_required:" + skill + ":" + minimum);
            }
        });

        if (!rule.anyPokemonCapabilities().isEmpty()
                && rule.anyPokemonCapabilities().stream().noneMatch(state.availablePokemonCapabilities()::contains)) {
            reasons.add("pokemon_capability_required");
        }

        return reasons.isEmpty()
                ? AuthorityDecision.allow(state.revision())
                : AuthorityDecision.deny(reasons, state.revision());
    }

    private record GateKey(ActionKind action, String resourceId) {}
}
