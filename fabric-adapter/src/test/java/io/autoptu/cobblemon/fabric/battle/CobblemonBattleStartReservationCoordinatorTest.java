package io.autoptu.cobblemon.fabric.battle;

import io.autoptu.cobblemon.authority.BattleCombatantAuthoritySnapshot;
import io.autoptu.cobblemon.authority.BattleEncounterParticipantSnapshot;
import io.autoptu.cobblemon.authority.BattleEncounterRosterRepository;
import io.autoptu.cobblemon.authority.BattleEncounterRosterReservation;
import io.autoptu.cobblemon.authority.BattleEncounterRosterReservationService;
import io.autoptu.cobblemon.authority.BattleParticipantKind;
import io.autoptu.cobblemon.authority.CanonicalBattleEncounterRepository;
import io.autoptu.cobblemon.authority.CanonicalBattlePokemonView;
import io.autoptu.cobblemon.authority.CanonicalEncounterPokemonState;
import io.autoptu.cobblemon.authority.CanonicalHealth;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CobblemonBattleStartReservationCoordinatorTest {
    @Test
    void mapsOpaqueExternalIdsIntoCanonicalMultiSideReservation() {
        TestRepository repository = new TestRepository();
        repository.put(BattleParticipantKind.WILD, "wild-left", canonical("canonical-left", 7));
        repository.put(BattleParticipantKind.WILD, "wild-right", canonical("canonical-right", 9));

        CobblemonCanonicalEncounterIdentityRegistry registry = new CobblemonCanonicalEncounterIdentityRegistry();
        registry.register(
                CobblemonBattleStartInterceptor.ParticipantKind.WILD,
                "actor-left",
                "wild-left",
                Map.of("external-pokemon-left", "canonical-left")
        );
        registry.register(
                CobblemonBattleStartInterceptor.ParticipantKind.WILD,
                "actor-right",
                "wild-right",
                Map.of("external-pokemon-right", "canonical-right")
        );

        CobblemonBattleStartReservationCoordinator coordinator = coordinator(registry, repository);
        var attempt = coordinator.tryReserve(signal(
                participant(1, "actor-left", "external-pokemon-left"),
                participant(2, "actor-right", "external-pokemon-right")
        ));

        assertTrue(attempt.claimed());
        assertEquals("reservation-live-1", attempt.reservation().reservationId());
        assertEquals(5150L, attempt.reservation().rngSeed());
        assertEquals(2, attempt.reservation().participants().size());

        BattleEncounterParticipantSnapshot left = attempt.reservation().participants().get(0);
        BattleEncounterParticipantSnapshot right = attempt.reservation().participants().get(1);
        assertEquals("wild-left", left.participantId());
        assertEquals("battle-side-1", left.teamId());
        assertEquals("canonical-left", left.combatants().getFirst().combatantId());
        assertEquals("wild-right", right.participantId());
        assertEquals("battle-side-2", right.teamId());
        assertEquals("canonical-right", right.combatants().getFirst().combatantId());
    }

    @Test
    void unresolvedExternalIdentityDoesNotCreateReservation() {
        TestRepository repository = new TestRepository();
        repository.put(BattleParticipantKind.WILD, "wild-left", canonical("canonical-left", 1));
        CobblemonCanonicalEncounterIdentityRegistry registry = new CobblemonCanonicalEncounterIdentityRegistry();
        registry.register(
                CobblemonBattleStartInterceptor.ParticipantKind.WILD,
                "actor-left",
                "wild-left",
                Map.of("external-pokemon-left", "canonical-left")
        );

        var attempt = coordinator(registry, repository).tryReserve(signal(
                participant(1, "actor-left", "external-pokemon-left"),
                participant(2, "unknown-actor", "unknown-pokemon")
        ));

        assertFalse(attempt.claimed());
        assertEquals("unresolved_participant:unknown-actor", attempt.rejectionCode());
        assertTrue(repository.reservations.isEmpty());
    }

    @Test
    void registeredParticipantMustMatchExactExternalPokemonRoster() {
        CobblemonCanonicalEncounterIdentityRegistry registry = new CobblemonCanonicalEncounterIdentityRegistry();
        LinkedHashMap<String, String> mappings = new LinkedHashMap<>();
        mappings.put("external-a", "canonical-a");
        mappings.put("external-b", "canonical-b");
        registry.register(
                CobblemonBattleStartInterceptor.ParticipantKind.WILD,
                "actor",
                "wild-pack",
                mappings
        );

        var incomplete = new CobblemonBattleStartInterceptor.ParticipantIdentity(
                1,
                CobblemonBattleStartInterceptor.ParticipantKind.WILD,
                "actor",
                List.of("external-a")
        );
        var reversed = new CobblemonBattleStartInterceptor.ParticipantIdentity(
                1,
                CobblemonBattleStartInterceptor.ParticipantKind.WILD,
                "actor",
                List.of("external-b", "external-a")
        );

        assertTrue(registry.resolve(incomplete).isEmpty());
        assertTrue(registry.resolve(reversed).isEmpty());
    }

    @Test
    void registryRejectsIdentityAliasingAcrossParticipants() {
        CobblemonCanonicalEncounterIdentityRegistry registry = new CobblemonCanonicalEncounterIdentityRegistry();
        registry.register(
                CobblemonBattleStartInterceptor.ParticipantKind.WILD,
                "actor-a",
                "wild-a",
                Map.of("external-a", "canonical-a")
        );

        assertThrows(IllegalStateException.class, () -> registry.register(
                CobblemonBattleStartInterceptor.ParticipantKind.WILD,
                "actor-b",
                "wild-b",
                Map.of("external-a", "canonical-b")
        ));
        assertThrows(IllegalStateException.class, () -> registry.register(
                CobblemonBattleStartInterceptor.ParticipantKind.WILD,
                "actor-c",
                "wild-c",
                Map.of("external-c", "canonical-a")
        ));
    }

    private static CobblemonBattleStartReservationCoordinator coordinator(
            CobblemonCanonicalEncounterIdentityRegistry registry,
            TestRepository repository
    ) {
        return new CobblemonBattleStartReservationCoordinator(
                registry,
                new BattleEncounterRosterReservationService(
                        repository,
                        repository,
                        () -> "reservation-live-1",
                        () -> 5150L
                )
        );
    }

    private static CobblemonBattleStartInterceptor.BattleStartSignal signal(
            CobblemonBattleStartInterceptor.ParticipantIdentity... participants
    ) {
        return new CobblemonBattleStartInterceptor.BattleStartSignal("external-battle", List.of(participants));
    }

    private static CobblemonBattleStartInterceptor.ParticipantIdentity participant(
            int side,
            String actorId,
            String pokemonId
    ) {
        return new CobblemonBattleStartInterceptor.ParticipantIdentity(
                side,
                CobblemonBattleStartInterceptor.ParticipantKind.WILD,
                actorId,
                List.of(pokemonId)
        );
    }

    private static CanonicalEncounterPokemonState canonical(String id, long revision) {
        return new CanonicalEncounterPokemonState(
                id,
                "cobblemon:pikachu",
                12,
                Set.of("Overland"),
                Set.of(),
                null,
                null,
                new CanonicalHealth(20, 20),
                null,
                null,
                null,
                null,
                null,
                null,
                revision
        );
    }

    private static final class TestRepository
            implements CanonicalBattleEncounterRepository, BattleEncounterRosterRepository {
        private final Map<String, CanonicalBattlePokemonView> canonical = new HashMap<>();
        private final Map<String, BattleEncounterRosterReservation> reservations = new HashMap<>();
        private final Set<String> locked = new HashSet<>();

        void put(BattleParticipantKind kind, String participantId, CanonicalBattlePokemonView state) {
            canonical.put(key(kind, participantId, state.pokemonId()), state);
        }

        @Override
        public Optional<CanonicalBattlePokemonView> findCombatant(
                BattleParticipantKind participantKind,
                String participantId,
                String combatantId
        ) {
            return Optional.ofNullable(canonical.get(key(participantKind, participantId, combatantId)));
        }

        @Override
        public Optional<BattleEncounterRosterReservation> findReservation(String reservationId) {
            return Optional.ofNullable(reservations.get(reservationId));
        }

        @Override
        public synchronized boolean tryReserve(BattleEncounterRosterReservation reservation) {
            if (reservations.containsKey(reservation.reservationId())) return false;
            for (BattleEncounterParticipantSnapshot participant : reservation.participants()) {
                for (BattleCombatantAuthoritySnapshot combatant : participant.combatants()) {
                    CanonicalBattlePokemonView live = canonical.get(key(
                            combatant.participantKind(),
                            combatant.participantId(),
                            combatant.combatantId()
                    ));
                    if (live == null || live.revision() != combatant.revision() || locked.contains(combatant.combatantId())) {
                        return false;
                    }
                }
            }
            reservation.participants().stream()
                    .flatMap(participant -> participant.combatants().stream())
                    .forEach(combatant -> locked.add(combatant.combatantId()));
            reservations.put(reservation.reservationId(), reservation);
            return true;
        }

        @Override
        public synchronized boolean release(String reservationId) {
            BattleEncounterRosterReservation reservation = reservations.remove(reservationId);
            if (reservation == null) return false;
            reservation.participants().stream()
                    .flatMap(participant -> participant.combatants().stream())
                    .forEach(combatant -> locked.remove(combatant.combatantId()));
            return true;
        }

        private static String key(BattleParticipantKind kind, String participantId, String combatantId) {
            return kind + "|" + participantId + "|" + combatantId;
        }
    }
}
