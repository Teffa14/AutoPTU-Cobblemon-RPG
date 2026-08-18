package io.autoptu.cobblemon.authority;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PTUGateServiceTest {
    @Test
    void allowsCanonicalMedicWithRequiredMedicineRank() {
        CanonicalPlayerState state = new CanonicalPlayerState(
                "player-1",
                Set.of("Medic"),
                Map.of("Medicine Education", 4),
                Set.of(),
                12);

        PTUGateService service = serviceFor(state, List.of(new GateRule(
                ActionKind.CRAFT,
                "autoptu:advanced_potion",
                Set.of("Medic"),
                Map.of("Medicine Education", 3),
                Set.of())));

        AuthorityDecision decision = service.canPerform(
                "player-1", ActionKind.CRAFT, "autoptu:advanced_potion");

        assertTrue(decision.allowed());
    }

    @Test
    void deniesWhenCanonicalStateDoesNotMeetRequirements() {
        CanonicalPlayerState state = new CanonicalPlayerState(
                "player-1",
                Set.of("Ace Trainer"),
                Map.of("Medicine Education", 1),
                Set.of(),
                9);

        PTUGateService service = serviceFor(state, List.of(new GateRule(
                ActionKind.CRAFT,
                "autoptu:advanced_potion",
                Set.of("Medic"),
                Map.of("Medicine Education", 3),
                Set.of())));

        AuthorityDecision decision = service.canPerform(
                "player-1", ActionKind.CRAFT, "autoptu:advanced_potion");

        assertFalse(decision.allowed());
        assertTrue(decision.reasons().contains("trainer_class_required"));
        assertTrue(decision.reasons().contains("skill_rank_required:Medicine Education:3"));
    }

    @Test
    void capabilityGateReadsServerOwnedPokemonCapabilities() {
        CanonicalPlayerState state = new CanonicalPlayerState(
                "player-1",
                Set.of("Survivalist"),
                Map.of("Survival", 3),
                Set.of("Power 6", "Tracker"),
                22);

        PTUGateService service = serviceFor(state, List.of(new GateRule(
                ActionKind.INTERACT,
                "world:collapsed_tunnel",
                Set.of(),
                Map.of(),
                Set.of("Power 6", "Telekinetic"))));

        assertTrue(service.canPerform(
                "player-1", ActionKind.INTERACT, "world:collapsed_tunnel").allowed());
    }

    @Test
    void unregisteredClientRequestedActionsFailClosed() {
        CanonicalPlayerState state = new CanonicalPlayerState(
                "player-1", Set.of("Medic"), Map.of("Medicine Education", 5), Set.of(), 3);

        PTUGateService service = serviceFor(state, List.of());

        AuthorityDecision decision = service.canPerform(
                "player-1", ActionKind.CRAFT, "client:forged_recipe");

        assertFalse(decision.allowed());
        assertTrue(decision.reasons().contains("unregistered_action"));
    }

    private static PTUGateService serviceFor(CanonicalPlayerState state, List<GateRule> rules) {
        CanonicalStateRepository repository = playerId ->
                state.playerId().equals(playerId) ? Optional.of(state) : Optional.empty();
        return new PTUGateService(repository, rules);
    }
}
