package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleEncounterRosterReservationServiceTest {
    @Test
    void reservesPlayerAndWildCanonicalRostersWithExplicitOpposingTeams() {
        InMemoryEncounterRepository repository = repository();
        BattleEncounterRosterReservationService service = service(repository);

        BattleEncounterRosterReservationDecision decision = service.reserve(List.of(
                request(1, "player-1", BattleParticipantKind.PLAYER, "player-pokemon-1"),
                request(2, "wild-pack-1", BattleParticipantKind.WILD, "wild-pokemon-1")
        ));

        assertTrue(decision.allowed());
        BattleEncounterRosterReservation reservation = decision.reservation();
        assertEquals("encounter-1", reservation.reservationId());
        assertEquals(9001L, reservation.rngSeed());
        assertEquals(2, reservation.participants().size());
        assertEquals("battle-side-1", reservation.participants().get(0).teamId());
        assertEquals("battle-side-2", reservation.participants().get(1).teamId());
        assertEquals(BattleParticipantKind.WILD, reservation.participants().get(1).participantKind());
        assertEquals("wild-pokemon-1", reservation.participants().get(1).combatants().getFirst().combatantId());
    }

    @Test
    void refusesParticipantClaimsThatCanonicalRepositoryDoesNotAuthorize() {
        InMemoryEncounterRepository repository = repository();
        BattleEncounterRosterReservationService service = service(repository);

        BattleEncounterRosterReservationDecision decision = service.reserve(List.of(
                request(1, "player-forged", BattleParticipantKind.PLAYER, "player-pokemon-1"),
                request(2, "wild-pack-1", BattleParticipantKind.WILD, "wild-pokemon-1")
        ));

        assertFalse(decision.allowed());
        assertEquals("unknown_or_unauthorized_combatant:player-pokemon-1", decision.reason());
        assertTrue(repository.reservations.isEmpty());
    }

    @Test
    void atomicReservationRejectsRevisionChangeAfterCanonicalRead() {
        InMemoryEncounterRepository repository = repository();
        repository.bumpBeforeNextReserve = "wild-pokemon-1";
        BattleEncounterRosterReservationService service = service(repository);

        BattleEncounterRosterReservationDecision decision = service.reserve(List.of(
                request(1, "player-1", BattleParticipantKind.PLAYER, "player-pokemon-1"),
                request(2, "wild-pack-1", BattleParticipantKind.WILD, "wild-pokemon-1")
        ));

        assertFalse(decision.allowed());
        assertEquals("state_changed_or_combatants_reserved", decision.reason());
        assertTrue(repository.reservations.isEmpty());
    }

    @Test
    void lockedCombatantsCannotEnterSecondEncounterUntilReleased() {
        InMemoryEncounterRepository repository = repository();
        BattleEncounterRosterReservationService service = service(repository);
        List<BattleEncounterParticipantRequest> requests = List.of(
                request(1, "player-1", BattleParticipantKind.PLAYER, "player-pokemon-1"),
                request(2, "wild-pack-1", BattleParticipantKind.WILD, "wild-pokemon-1")
        );

        BattleEncounterRosterReservationDecision first = service.reserve(requests);
        BattleEncounterRosterReservationDecision second = service.reserve(requests);

        assertTrue(first.allowed());
        assertFalse(second.allowed());
        assertEquals("state_changed_or_combatants_reserved", second.reason());
        assertTrue(service.release(first.reservation().reservationId()).allowed());
        assertTrue(service.reserve(requests).allowed());
    }

    @Test
    void refusesOneSidedEncounterAndDuplicateCombatants() {
        InMemoryEncounterRepository repository = repository();
        BattleEncounterRosterReservationService service = service(repository);

        BattleEncounterRosterReservationDecision oneSide = service.reserve(List.of(
                request(1, "player-1", BattleParticipantKind.PLAYER, "player-pokemon-1")
        ));
        BattleEncounterRosterReservationDecision duplicate = service.reserve(List.of(
                request(1, "player-1", BattleParticipantKind.PLAYER, "player-pokemon-1"),
                request(2, "wild-pack-1", BattleParticipantKind.WILD, "player-pokemon-1")
        ));

        assertFalse(oneSide.allowed());
        assertTrue(oneSide.reason().startsWith("invalid_encounter:"));
        assertFalse(duplicate.allowed());
        assertEquals("duplicate_combatant:player-pokemon-1", duplicate.reason());
    }

    private static BattleEncounterParticipantRequest request(
            int side,
            String participantId,
            BattleParticipantKind kind,
            String combatantId
    ) {
        return new BattleEncounterParticipantRequest(side, participantId, kind, List.of(combatantId));
    }

    private static BattleEncounterRosterReservationService service(InMemoryEncounterRepository repository) {
        AtomicInteger ids = new AtomicInteger();
        AtomicLong seeds = new AtomicLong(9000L);
        return new BattleEncounterRosterReservationService(
                repository,
                repository,
                () -> "encounter-" + ids.incrementAndGet(),
                seeds::incrementAndGet
        );
    }

    private static InMemoryEncounterRepository repository() {
        InMemoryEncounterRepository repository = new InMemoryEncounterRepository();
        CanonicalPokemonState playerState = new CanonicalPokemonState(
                "player-pokemon-1", "player-1", "cobblemon:charizard", 30, Set.of("Sky"), 4);
        repository.put(
                BattleParticipantKind.PLAYER,
                "player-1",
                new PlayerCanonicalBattlePokemonView(playerState)
        );
        repository.put(
                BattleParticipantKind.WILD,
                "wild-pack-1",
                new CanonicalEncounterPokemonState(
                        "wild-pokemon-1", "cobblemon:pikachu", 12, Set.of("Overland"), Set.of(), null,
                        null, new CanonicalHealth(20, 20), null, null, null, null, null, null, 7)
        );
        return repository;
    }

    private static final class InMemoryEncounterRepository
            implements CanonicalBattleEncounterRepository, BattleEncounterRosterRepository {
        private final Map<String, CanonicalBattlePokemonView> canonical = new HashMap<>();
        private final Map<String, Long> liveRevisions = new HashMap<>();
        private final Map<String, BattleEncounterRosterReservation> reservations = new HashMap<>();
        private final Set<String> lockedCombatants = new HashSet<>();
        private String bumpBeforeNextReserve;

        void put(BattleParticipantKind kind, String participantId, CanonicalBattlePokemonView state) {
            canonical.put(key(kind, participantId, state.pokemonId()), state);
            liveRevisions.put(state.pokemonId(), state.revision());
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
            if (bumpBeforeNextReserve != null) {
                liveRevisions.computeIfPresent(bumpBeforeNextReserve, (ignored, revision) -> revision + 1);
                bumpBeforeNextReserve = null;
            }
            if (reservations.containsKey(reservation.reservationId())) return false;

            for (BattleEncounterParticipantSnapshot participant : reservation.participants()) {
                for (BattleCombatantAuthoritySnapshot combatant : participant.combatants()) {
                    Long liveRevision = liveRevisions.get(combatant.combatantId());
                    if (liveRevision == null
                            || liveRevision != combatant.revision()
                            || lockedCombatants.contains(combatant.combatantId())) {
                        return false;
                    }
                }
            }
            reservation.participants().stream()
                    .flatMap(participant -> participant.combatants().stream())
                    .forEach(combatant -> lockedCombatants.add(combatant.combatantId()));
            reservations.put(reservation.reservationId(), reservation);
            return true;
        }

        @Override
        public synchronized boolean release(String reservationId) {
            BattleEncounterRosterReservation reservation = reservations.remove(reservationId);
            if (reservation == null) return false;
            reservation.participants().stream()
                    .flatMap(participant -> participant.combatants().stream())
                    .forEach(combatant -> lockedCombatants.remove(combatant.combatantId()));
            return true;
        }

        private static String key(BattleParticipantKind kind, String participantId, String combatantId) {
            return kind + "|" + participantId + "|" + combatantId;
        }
    }
}
