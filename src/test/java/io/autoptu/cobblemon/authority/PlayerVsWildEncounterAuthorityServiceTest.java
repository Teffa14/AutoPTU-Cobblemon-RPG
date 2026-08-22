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

class PlayerVsWildEncounterAuthorityServiceTest {
    @Test
    void composesTrainerItemsArenaAndOpposingRosterUnderOneServerIdentity() {
        Repository repository = repository();
        PlayerVsWildEncounterAuthorityService service = service(repository);
        BattleArenaSnapshot arena = arena();

        PlayerVsWildBattleReservationDecision decision = service.reserve(
                "player-1",
                List.of("pokemon-1"),
                Map.of("potion-1", 1),
                arena,
                participants("pokemon-1")
        );

        assertTrue(decision.allowed());
        PlayerVsWildBattleReservation reservation = decision.reservation();
        assertEquals("player-wild-1", reservation.reservationId());
        assertEquals(5001L, reservation.rngSeed());
        assertEquals(reservation.reservationId(), reservation.playerAuthority().reservationId());
        assertEquals(reservation.reservationId(), reservation.encounterAuthority().reservationId());
        assertEquals(reservation.rngSeed(), reservation.playerAuthority().rngSeed());
        assertEquals(reservation.rngSeed(), reservation.encounterAuthority().rngSeed());
        assertEquals(arena, reservation.playerAuthority().arena());
        assertEquals("player-1", reservation.playerAuthority().trainer().playerId());
        assertEquals("potion-1", reservation.playerAuthority().items().getFirst().itemInstanceId());
        assertEquals(List.of("pokemon-1"), reservation.playerAuthority().roster().stream()
                .map(BattlePokemonSnapshot::pokemonId).toList());
        assertEquals(BattleParticipantKind.WILD,
                reservation.encounterAuthority().participants().get(1).participantKind());
        assertEquals("wild-1",
                reservation.encounterAuthority().participants().get(1).combatants().getFirst().combatantId());
    }

    @Test
    void rejectsPlayerRosterMismatchBeforeCreatingAnyReservation() {
        Repository repository = repository();
        PlayerVsWildEncounterAuthorityService service = service(repository);

        PlayerVsWildBattleReservationDecision decision = service.reserve(
                "player-1",
                List.of("pokemon-1"),
                Map.of(),
                arena(),
                participants("other-player-pokemon")
        );

        assertFalse(decision.allowed());
        assertEquals("player_roster_mismatch", decision.reason());
        assertTrue(repository.playerReservations.isEmpty());
        assertTrue(repository.encounterReservations.isEmpty());
    }

    @Test
    void encounterLockFailureCompensatesPlayerAssetReservation() {
        Repository repository = repository();
        repository.failNextEncounterReserve = true;
        PlayerVsWildEncounterAuthorityService service = service(repository);

        PlayerVsWildBattleReservationDecision failed = service.reserve(
                "player-1", List.of("pokemon-1"), Map.of("potion-1", 1), arena(), participants("pokemon-1"));

        assertFalse(failed.allowed());
        assertEquals("encounter_authority:state_changed_or_combatants_reserved", failed.reason());
        assertTrue(repository.playerReservations.isEmpty());
        assertTrue(repository.lockedPokemon.isEmpty());
        assertTrue(repository.lockedItems.isEmpty());

        PlayerVsWildBattleReservationDecision retry = service.reserve(
                "player-1", List.of("pokemon-1"), Map.of("potion-1", 1), arena(), participants("pokemon-1"));
        assertTrue(retry.allowed());
    }

    @Test
    void rejectsNonWildTopologyAndWrongPlayerParticipant() {
        Repository repository = repository();
        PlayerVsWildEncounterAuthorityService service = service(repository);
        List<BattleEncounterParticipantRequest> npc = List.of(
                new BattleEncounterParticipantRequest(1, "player-1", BattleParticipantKind.PLAYER, List.of("pokemon-1")),
                new BattleEncounterParticipantRequest(2, "npc-1", BattleParticipantKind.NPC, List.of("wild-1"))
        );
        List<BattleEncounterParticipantRequest> wrongPlayer = List.of(
                new BattleEncounterParticipantRequest(1, "player-2", BattleParticipantKind.PLAYER, List.of("pokemon-1")),
                new BattleEncounterParticipantRequest(2, "wild-pack", BattleParticipantKind.WILD, List.of("wild-1"))
        );

        assertEquals("unsupported_player_vs_wild_topology", service.reserve(
                "player-1", List.of("pokemon-1"), Map.of(), arena(), npc).reason());
        assertEquals("player_participant_mismatch", service.reserve(
                "player-1", List.of("pokemon-1"), Map.of(), arena(), wrongPlayer).reason());
        assertTrue(repository.playerReservations.isEmpty());
        assertTrue(repository.encounterReservations.isEmpty());
    }

    private static PlayerVsWildEncounterAuthorityService service(Repository repository) {
        BattleAuthorityService player = new BattleAuthorityService(
                repository, repository, repository, () -> "unused-player", () -> -1L);
        BattleEncounterRosterReservationService encounter = new BattleEncounterRosterReservationService(
                repository, repository, () -> "unused-encounter", () -> -2L);
        AtomicInteger ids = new AtomicInteger();
        AtomicLong seeds = new AtomicLong(5000L);
        return new PlayerVsWildEncounterAuthorityService(
                player, encounter, () -> "player-wild-" + ids.incrementAndGet(), seeds::incrementAndGet);
    }

    private static List<BattleEncounterParticipantRequest> participants(String playerPokemonId) {
        return List.of(
                new BattleEncounterParticipantRequest(
                        1, "player-1", BattleParticipantKind.PLAYER, List.of(playerPokemonId)),
                new BattleEncounterParticipantRequest(
                        2, "wild-pack", BattleParticipantKind.WILD, List.of("wild-1"))
        );
    }

    private static BattleArenaSnapshot arena() {
        return new BattleArenaSnapshot("minecraft:overworld", 100, 64, 200, 1, 0, 0, 1);
    }

    private static Repository repository() {
        Repository repository = new Repository();
        repository.player = new CanonicalPlayerState(
                "player-1", Set.of("Ace Trainer"), Map.of("Command", 4), Set.of("Overland"), 11);
        repository.playerPokemon = new CanonicalPokemonState(
                "pokemon-1", "player-1", "cobblemon:charizard", 30, Set.of("Sky"), null, 4);
        repository.potion = new CanonicalItemInstance(
                "potion-1", "player-1", "autoptu:potion", 2, 5);
        repository.wild = new CanonicalEncounterPokemonState(
                "wild-1", "cobblemon:pikachu", 12, Set.of("Overland"), Set.of(), null,
                null, new CanonicalHealth(20, 20), null, null, null, null, null, null, 7);
        return repository;
    }

    private static final class Repository implements
            CanonicalStateRepository,
            CanonicalAssetRepository,
            BattleSnapshotRepository,
            CanonicalBattleEncounterRepository,
            BattleEncounterRosterRepository {
        private CanonicalPlayerState player;
        private CanonicalPokemonState playerPokemon;
        private CanonicalItemInstance potion;
        private CanonicalEncounterPokemonState wild;
        private final Map<String, BattleAuthoritySnapshot> playerReservations = new HashMap<>();
        private final Map<String, BattleEncounterRosterReservation> encounterReservations = new HashMap<>();
        private final Set<String> lockedPokemon = new HashSet<>();
        private final Set<String> lockedItems = new HashSet<>();
        private final Set<String> lockedEncounterCombatants = new HashSet<>();
        private boolean failNextEncounterReserve;

        @Override
        public Optional<CanonicalPlayerState> findPlayer(String playerId) {
            return player.playerId().equals(playerId) ? Optional.of(player) : Optional.empty();
        }

        @Override
        public Optional<CanonicalPokemonState> findPokemon(String pokemonId) {
            return playerPokemon.pokemonId().equals(pokemonId) ? Optional.of(playerPokemon) : Optional.empty();
        }

        @Override
        public Optional<CanonicalItemInstance> findItem(String itemInstanceId) {
            return potion.itemInstanceId().equals(itemInstanceId) ? Optional.of(potion) : Optional.empty();
        }

        @Override
        public Optional<ItemReservation> findReservation(String reservationId) {
            return Optional.empty();
        }

        @Override
        public boolean tryReserveItem(ItemReservation reservation) { return false; }
        @Override
        public boolean commitItemReservation(String reservationId, String playerId) { return false; }
        @Override
        public boolean releaseItemReservation(String reservationId, String playerId) { return false; }

        @Override
        public Optional<BattleAuthoritySnapshot> findSnapshot(String reservationId) {
            return Optional.ofNullable(playerReservations.get(reservationId));
        }

        @Override
        public synchronized boolean tryReserveSnapshot(BattleAuthoritySnapshot snapshot) {
            if (playerReservations.containsKey(snapshot.reservationId())) return false;
            if (!snapshot.playerId().equals(player.playerId())
                    || snapshot.trainer().revision() != player.revision()) return false;
            for (BattlePokemonSnapshot pokemon : snapshot.roster()) {
                if (!pokemon.pokemonId().equals(playerPokemon.pokemonId())
                        || pokemon.revision() != playerPokemon.revision()
                        || lockedPokemon.contains(pokemon.pokemonId())) return false;
            }
            for (BattleItemSnapshot item : snapshot.items()) {
                if (!item.itemInstanceId().equals(potion.itemInstanceId())
                        || item.revision() != potion.revision()
                        || potion.quantity() < item.reservedQuantity()
                        || lockedItems.contains(item.itemInstanceId())) return false;
            }
            snapshot.roster().forEach(p -> lockedPokemon.add(p.pokemonId()));
            snapshot.items().forEach(i -> lockedItems.add(i.itemInstanceId()));
            playerReservations.put(snapshot.reservationId(), snapshot);
            return true;
        }

        @Override
        public synchronized boolean releaseSnapshot(String reservationId, String playerId) {
            BattleAuthoritySnapshot snapshot = playerReservations.get(reservationId);
            if (snapshot == null || !snapshot.playerId().equals(playerId)) return false;
            snapshot.roster().forEach(p -> lockedPokemon.remove(p.pokemonId()));
            snapshot.items().forEach(i -> lockedItems.remove(i.itemInstanceId()));
            playerReservations.remove(reservationId);
            return true;
        }

        @Override
        public Optional<CanonicalBattlePokemonView> findCombatant(
                BattleParticipantKind participantKind, String participantId, String combatantId) {
            if (participantKind == BattleParticipantKind.PLAYER
                    && participantId.equals(player.playerId())
                    && combatantId.equals(playerPokemon.pokemonId())) {
                return Optional.of(new PlayerCanonicalBattlePokemonView(playerPokemon));
            }
            if (participantKind == BattleParticipantKind.WILD
                    && participantId.equals("wild-pack")
                    && combatantId.equals(wild.pokemonId())) {
                return Optional.of(wild);
            }
            return Optional.empty();
        }

        @Override
        public Optional<BattleEncounterRosterReservation> findReservation(String reservationId) {
            return Optional.ofNullable(encounterReservations.get(reservationId));
        }

        @Override
        public synchronized boolean tryReserve(BattleEncounterRosterReservation reservation) {
            if (failNextEncounterReserve) {
                failNextEncounterReserve = false;
                return false;
            }
            if (encounterReservations.containsKey(reservation.reservationId())) return false;
            for (BattleEncounterParticipantSnapshot participant : reservation.participants()) {
                for (BattleCombatantAuthoritySnapshot combatant : participant.combatants()) {
                    if (lockedEncounterCombatants.contains(combatant.combatantId())) return false;
                    long liveRevision = combatant.combatantId().equals(playerPokemon.pokemonId())
                            ? playerPokemon.revision() : wild.revision();
                    if (combatant.revision() != liveRevision) return false;
                }
            }
            reservation.participants().stream().flatMap(p -> p.combatants().stream())
                    .forEach(c -> lockedEncounterCombatants.add(c.combatantId()));
            encounterReservations.put(reservation.reservationId(), reservation);
            return true;
        }

        @Override
        public synchronized boolean release(String reservationId) {
            BattleEncounterRosterReservation reservation = encounterReservations.remove(reservationId);
            if (reservation == null) return false;
            reservation.participants().stream().flatMap(p -> p.combatants().stream())
                    .forEach(c -> lockedEncounterCombatants.remove(c.combatantId()));
            return true;
        }
    }
}
