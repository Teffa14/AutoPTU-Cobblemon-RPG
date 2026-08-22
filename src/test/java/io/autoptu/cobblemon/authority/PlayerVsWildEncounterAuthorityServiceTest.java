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
        Fixture fixture = fixture();
        PlayerVsWildEncounterAuthorityService service = service(fixture);
        BattleArenaSnapshot arena = arena();

        PlayerVsWildBattleReservationDecision decision = service.reserve(
                "player-1", List.of("pokemon-1"), Map.of("potion-1", 1), arena, participants("pokemon-1"));

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
        Fixture fixture = fixture();
        PlayerVsWildEncounterAuthorityService service = service(fixture);

        PlayerVsWildBattleReservationDecision decision = service.reserve(
                "player-1", List.of("pokemon-1"), Map.of(), arena(), participants("other-player-pokemon"));

        assertFalse(decision.allowed());
        assertEquals("player_roster_mismatch", decision.reason());
        assertTrue(fixture.playerRepository.snapshots.isEmpty());
        assertTrue(fixture.encounterRepository.reservations.isEmpty());
    }

    @Test
    void encounterLockFailureCompensatesPlayerAssetReservation() {
        Fixture fixture = fixture();
        fixture.encounterRepository.failNextReserve = true;
        PlayerVsWildEncounterAuthorityService service = service(fixture);

        PlayerVsWildBattleReservationDecision failed = service.reserve(
                "player-1", List.of("pokemon-1"), Map.of("potion-1", 1), arena(), participants("pokemon-1"));

        assertFalse(failed.allowed());
        assertEquals("encounter_authority:state_changed_or_combatants_reserved", failed.reason());
        assertTrue(fixture.playerRepository.snapshots.isEmpty());
        assertTrue(fixture.playerRepository.lockedPokemon.isEmpty());
        assertTrue(fixture.playerRepository.lockedItems.isEmpty());

        PlayerVsWildBattleReservationDecision retry = service.reserve(
                "player-1", List.of("pokemon-1"), Map.of("potion-1", 1), arena(), participants("pokemon-1"));
        assertTrue(retry.allowed());
    }

    @Test
    void rejectsNonWildTopologyAndWrongPlayerParticipant() {
        Fixture fixture = fixture();
        PlayerVsWildEncounterAuthorityService service = service(fixture);
        List<BattleEncounterParticipantRequest> npc = List.of(
                new BattleEncounterParticipantRequest(1, "player-1", BattleParticipantKind.PLAYER, List.of("pokemon-1")),
                new BattleEncounterParticipantRequest(2, "npc-1", BattleParticipantKind.NPC, List.of("wild-1")));
        List<BattleEncounterParticipantRequest> wrongPlayer = List.of(
                new BattleEncounterParticipantRequest(1, "player-2", BattleParticipantKind.PLAYER, List.of("pokemon-1")),
                new BattleEncounterParticipantRequest(2, "wild-pack", BattleParticipantKind.WILD, List.of("wild-1")));

        assertEquals("unsupported_player_vs_wild_topology", service.reserve(
                "player-1", List.of("pokemon-1"), Map.of(), arena(), npc).reason());
        assertEquals("player_participant_mismatch", service.reserve(
                "player-1", List.of("pokemon-1"), Map.of(), arena(), wrongPlayer).reason());
        assertTrue(fixture.playerRepository.snapshots.isEmpty());
        assertTrue(fixture.encounterRepository.reservations.isEmpty());
    }

    private static PlayerVsWildEncounterAuthorityService service(Fixture fixture) {
        BattleAuthorityService player = new BattleAuthorityService(
                fixture.playerRepository, fixture.playerRepository, fixture.playerRepository,
                () -> "unused-player", () -> -1L);
        BattleEncounterRosterReservationService encounter = new BattleEncounterRosterReservationService(
                fixture.encounterRepository, fixture.encounterRepository,
                () -> "unused-encounter", () -> -2L);
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
                        2, "wild-pack", BattleParticipantKind.WILD, List.of("wild-1")));
    }

    private static BattleArenaSnapshot arena() {
        return new BattleArenaSnapshot("minecraft:overworld", 100, 64, 200, 1, 0, 0, 1);
    }

    private static Fixture fixture() {
        CanonicalPlayerState player = new CanonicalPlayerState(
                "player-1", Set.of("Ace Trainer"), Map.of("Command", 4), Set.of("Overland"), 11);
        CanonicalPokemonState playerPokemon = new CanonicalPokemonState(
                "pokemon-1", "player-1", "cobblemon:charizard", 30, Set.of("Sky"), null, 4);
        CanonicalItemInstance potion = new CanonicalItemInstance(
                "potion-1", "player-1", "autoptu:potion", 2, 5);
        CanonicalEncounterPokemonState wild = new CanonicalEncounterPokemonState(
                "wild-1", "cobblemon:pikachu", 12, Set.of("Overland"), Set.of(), null,
                null, new CanonicalHealth(20, 20), null, null, null, null, null, null, 7);
        return new Fixture(
                new PlayerRepository(player, playerPokemon, potion),
                new EncounterRepository(playerPokemon, wild));
    }

    private record Fixture(PlayerRepository playerRepository, EncounterRepository encounterRepository) {}

    private static final class PlayerRepository
            implements CanonicalStateRepository, CanonicalAssetRepository, BattleSnapshotRepository {
        private final CanonicalPlayerState player;
        private final CanonicalPokemonState pokemon;
        private final CanonicalItemInstance item;
        private final Map<String, BattleAuthoritySnapshot> snapshots = new HashMap<>();
        private final Set<String> lockedPokemon = new HashSet<>();
        private final Set<String> lockedItems = new HashSet<>();

        private PlayerRepository(CanonicalPlayerState player, CanonicalPokemonState pokemon, CanonicalItemInstance item) {
            this.player = player;
            this.pokemon = pokemon;
            this.item = item;
        }

        @Override public Optional<CanonicalPlayerState> findPlayer(String playerId) {
            return player.playerId().equals(playerId) ? Optional.of(player) : Optional.empty();
        }
        @Override public Optional<CanonicalPokemonState> findPokemon(String pokemonId) {
            return pokemon.pokemonId().equals(pokemonId) ? Optional.of(pokemon) : Optional.empty();
        }
        @Override public Optional<CanonicalItemInstance> findItem(String itemInstanceId) {
            return item.itemInstanceId().equals(itemInstanceId) ? Optional.of(item) : Optional.empty();
        }
        @Override public Optional<ItemReservation> findReservation(String reservationId) { return Optional.empty(); }
        @Override public boolean tryReserveItem(ItemReservation reservation) { return false; }
        @Override public boolean commitItemReservation(String reservationId, String playerId) { return false; }
        @Override public boolean releaseItemReservation(String reservationId, String playerId) { return false; }
        @Override public Optional<BattleAuthoritySnapshot> findSnapshot(String reservationId) {
            return Optional.ofNullable(snapshots.get(reservationId));
        }

        @Override
        public synchronized boolean tryReserveSnapshot(BattleAuthoritySnapshot snapshot) {
            if (snapshots.containsKey(snapshot.reservationId())) return false;
            if (!snapshot.playerId().equals(player.playerId()) || snapshot.trainer().revision() != player.revision()) return false;
            for (BattlePokemonSnapshot requested : snapshot.roster()) {
                if (!requested.pokemonId().equals(pokemon.pokemonId())
                        || requested.revision() != pokemon.revision()
                        || lockedPokemon.contains(requested.pokemonId())) return false;
            }
            for (BattleItemSnapshot requested : snapshot.items()) {
                if (!requested.itemInstanceId().equals(item.itemInstanceId())
                        || requested.revision() != item.revision()
                        || item.quantity() < requested.reservedQuantity()
                        || lockedItems.contains(requested.itemInstanceId())) return false;
            }
            snapshot.roster().forEach(p -> lockedPokemon.add(p.pokemonId()));
            snapshot.items().forEach(i -> lockedItems.add(i.itemInstanceId()));
            snapshots.put(snapshot.reservationId(), snapshot);
            return true;
        }

        @Override
        public synchronized boolean releaseSnapshot(String reservationId, String playerId) {
            BattleAuthoritySnapshot snapshot = snapshots.get(reservationId);
            if (snapshot == null || !snapshot.playerId().equals(playerId)) return false;
            snapshot.roster().forEach(p -> lockedPokemon.remove(p.pokemonId()));
            snapshot.items().forEach(i -> lockedItems.remove(i.itemInstanceId()));
            snapshots.remove(reservationId);
            return true;
        }
    }

    private static final class EncounterRepository
            implements CanonicalBattleEncounterRepository, BattleEncounterRosterRepository {
        private final CanonicalPokemonState playerPokemon;
        private final CanonicalEncounterPokemonState wild;
        private final Map<String, BattleEncounterRosterReservation> reservations = new HashMap<>();
        private final Set<String> lockedCombatants = new HashSet<>();
        private boolean failNextReserve;

        private EncounterRepository(CanonicalPokemonState playerPokemon, CanonicalEncounterPokemonState wild) {
            this.playerPokemon = playerPokemon;
            this.wild = wild;
        }

        @Override
        public Optional<CanonicalBattlePokemonView> findCombatant(
                BattleParticipantKind participantKind, String participantId, String combatantId) {
            if (participantKind == BattleParticipantKind.PLAYER
                    && participantId.equals(playerPokemon.ownerPlayerId())
                    && combatantId.equals(playerPokemon.pokemonId())) {
                return Optional.of(new PlayerCanonicalBattlePokemonView(playerPokemon));
            }
            if (participantKind == BattleParticipantKind.WILD
                    && participantId.equals("wild-pack")
                    && combatantId.equals(wild.pokemonId())) return Optional.of(wild);
            return Optional.empty();
        }

        @Override public Optional<BattleEncounterRosterReservation> findReservation(String reservationId) {
            return Optional.ofNullable(reservations.get(reservationId));
        }

        @Override
        public synchronized boolean tryReserve(BattleEncounterRosterReservation reservation) {
            if (failNextReserve) {
                failNextReserve = false;
                return false;
            }
            if (reservations.containsKey(reservation.reservationId())) return false;
            for (BattleEncounterParticipantSnapshot participant : reservation.participants()) {
                for (BattleCombatantAuthoritySnapshot combatant : participant.combatants()) {
                    if (lockedCombatants.contains(combatant.combatantId())) return false;
                    long liveRevision = combatant.combatantId().equals(playerPokemon.pokemonId())
                            ? playerPokemon.revision() : wild.revision();
                    if (combatant.revision() != liveRevision) return false;
                }
            }
            reservation.participants().stream().flatMap(p -> p.combatants().stream())
                    .forEach(c -> lockedCombatants.add(c.combatantId()));
            reservations.put(reservation.reservationId(), reservation);
            return true;
        }

        @Override
        public synchronized boolean release(String reservationId) {
            BattleEncounterRosterReservation reservation = reservations.remove(reservationId);
            if (reservation == null) return false;
            reservation.participants().stream().flatMap(p -> p.combatants().stream())
                    .forEach(c -> lockedCombatants.remove(c.combatantId()));
            return true;
        }
    }
}
