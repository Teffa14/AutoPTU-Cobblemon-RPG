package io.autoptu.cobblemon.authority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class BattleOutcomeCommitServiceTest {
    @Test
    void commitConsumesOnlyTheUsedReservedQuantityAndReleasesBattleLocks() {
        InMemoryRepository repository = standardRepository();
        BattleAuthorityService battleService = battleService(repository);
        BattleAuthoritySnapshot snapshot = battleService.reserveBattle(
                "player-1", List.of("pokemon-1"), Map.of("item-potion", 2)).snapshot();

        BattleOutcomeDecision decision = outcomeService(repository).commitEngineOutcome(
                "player-1", snapshot.reservationId(), "sha256:transcript-a", Map.of("item-potion", 1));

        assertTrue(decision.accepted());
        assertFalse(decision.idempotent());
        CanonicalItemInstance potion = repository.findItem("item-potion").orElseThrow();
        assertEquals(3, potion.quantity());
        assertEquals(45L, potion.revision());
        assertTrue(repository.findSnapshot(snapshot.reservationId()).isEmpty());

        assertTrue(battleService.reserveBattle(
                "player-1", List.of("pokemon-1"), Map.of("item-potion", 1)).allowed());
    }

    @Test
    void exactReplayIsIdempotentAndCannotConsumeTwice() {
        InMemoryRepository repository = standardRepository();
        BattleAuthoritySnapshot snapshot = battleService(repository).reserveBattle(
                "player-1", List.of("pokemon-1"), Map.of("item-potion", 2)).snapshot();
        BattleOutcomeCommitService service = outcomeService(repository);

        BattleOutcomeDecision first = service.commitEngineOutcome(
                "player-1", snapshot.reservationId(), "sha256:transcript-a", Map.of("item-potion", 2));
        BattleOutcomeDecision replay = service.commitEngineOutcome(
                "player-1", snapshot.reservationId(), "sha256:transcript-a", Map.of("item-potion", 2));

        assertTrue(first.accepted());
        assertFalse(first.idempotent());
        assertTrue(replay.accepted());
        assertTrue(replay.idempotent());
        assertEquals(first.outcome(), replay.outcome());
        assertEquals(2, repository.findItem("item-potion").orElseThrow().quantity());
        assertEquals(45L, repository.findItem("item-potion").orElseThrow().revision());
    }

    @Test
    void replayWithDifferentPayloadFailsClosed() {
        InMemoryRepository repository = standardRepository();
        BattleAuthoritySnapshot snapshot = battleService(repository).reserveBattle(
                "player-1", List.of("pokemon-1"), Map.of("item-potion", 2)).snapshot();
        BattleOutcomeCommitService service = outcomeService(repository);
        service.commitEngineOutcome(
                "player-1", snapshot.reservationId(), "sha256:transcript-a", Map.of("item-potion", 1));

        BattleOutcomeDecision forgedReplay = service.commitEngineOutcome(
                "player-1", snapshot.reservationId(), "sha256:transcript-a", Map.of("item-potion", 2));
        BattleOutcomeDecision differentTranscript = service.commitEngineOutcome(
                "player-1", snapshot.reservationId(), "sha256:transcript-b", Map.of("item-potion", 1));

        assertFalse(forgedReplay.accepted());
        assertEquals("outcome_already_committed_with_different_payload", forgedReplay.reason());
        assertFalse(differentTranscript.accepted());
        assertEquals("outcome_already_committed_with_different_payload", differentTranscript.reason());
        assertEquals(3, repository.findItem("item-potion").orElseThrow().quantity());
    }

    @Test
    void cannotConsumeAnItemThatWasNotReservedForTheBattle() {
        InMemoryRepository repository = standardRepository();
        repository.putItem(new CanonicalItemInstance(
                "item-extra", "player-1", "autoptu:full_heal", 2, 50));
        BattleAuthoritySnapshot snapshot = battleService(repository).reserveBattle(
                "player-1", List.of("pokemon-1"), Map.of("item-potion", 1)).snapshot();

        BattleOutcomeDecision decision = outcomeService(repository).commitEngineOutcome(
                "player-1", snapshot.reservationId(), "sha256:transcript", Map.of("item-extra", 1));

        assertFalse(decision.accepted());
        assertEquals("item_not_reserved_for_battle:item-extra", decision.reason());
        assertEquals(2, repository.findItem("item-extra").orElseThrow().quantity());
        assertTrue(repository.findSnapshot(snapshot.reservationId()).isPresent());
    }

    @Test
    void cannotConsumeMoreThanTheFrozenBattleReservation() {
        InMemoryRepository repository = standardRepository();
        BattleAuthoritySnapshot snapshot = battleService(repository).reserveBattle(
                "player-1", List.of("pokemon-1"), Map.of("item-potion", 1)).snapshot();

        BattleOutcomeDecision decision = outcomeService(repository).commitEngineOutcome(
                "player-1", snapshot.reservationId(), "sha256:transcript", Map.of("item-potion", 2));

        assertFalse(decision.accepted());
        assertEquals("consumption_exceeds_reservation:item-potion", decision.reason());
        assertEquals(4, repository.findItem("item-potion").orElseThrow().quantity());
    }

    @Test
    void heldItemCannotBePresentedAsAConsumedBattleItem() {
        InMemoryRepository repository = standardRepository();
        BattleAuthoritySnapshot snapshot = battleService(repository).reserveBattle(
                "player-1", List.of("pokemon-1"), Map.of()).snapshot();

        BattleOutcomeDecision decision = outcomeService(repository).commitEngineOutcome(
                "player-1", snapshot.reservationId(), "sha256:transcript", Map.of("item-charcoal", 1));

        assertFalse(decision.accepted());
        assertEquals("held_item_cannot_be_consumed:item-charcoal", decision.reason());
        assertEquals(1, repository.findItem("item-charcoal").orElseThrow().quantity());
    }

    @Test
    void concurrentCanonicalMutationRejectsOutcomeWithoutPartialCommit() {
        InMemoryRepository repository = standardRepository();
        BattleAuthoritySnapshot snapshot = battleService(repository).reserveBattle(
                "player-1", List.of("pokemon-1"), Map.of("item-potion", 2)).snapshot();
        repository.bumpItemBeforeNextOutcomeCommit = "item-potion";

        BattleOutcomeDecision decision = outcomeService(repository).commitEngineOutcome(
                "player-1", snapshot.reservationId(), "sha256:transcript", Map.of("item-potion", 1));

        assertFalse(decision.accepted());
        assertEquals("state_changed_or_outcome_conflict", decision.reason());
        assertEquals(4, repository.findItem("item-potion").orElseThrow().quantity());
        assertEquals(45L, repository.findItem("item-potion").orElseThrow().revision());
        assertTrue(repository.findSnapshot(snapshot.reservationId()).isPresent());
        assertTrue(repository.findCommittedOutcome(snapshot.reservationId()).isEmpty());
    }

    @Test
    void anotherPlayerCannotCommitTheBattleOutcome() {
        InMemoryRepository repository = standardRepository();
        BattleAuthoritySnapshot snapshot = battleService(repository).reserveBattle(
                "player-1", List.of("pokemon-1"), Map.of("item-potion", 1)).snapshot();

        BattleOutcomeDecision decision = outcomeService(repository).commitEngineOutcome(
                "player-2", snapshot.reservationId(), "sha256:transcript", Map.of("item-potion", 1));

        assertFalse(decision.accepted());
        assertEquals("battle_reservation_not_owned", decision.reason());
        assertEquals(4, repository.findItem("item-potion").orElseThrow().quantity());
    }

    private static BattleAuthorityService battleService(InMemoryRepository repository) {
        AtomicInteger ids = new AtomicInteger();
        AtomicLong seeds = new AtomicLong(8000);
        return new BattleAuthorityService(
                repository,
                repository,
                repository,
                () -> "battle-" + ids.incrementAndGet(),
                seeds::incrementAndGet);
    }

    private static BattleOutcomeCommitService outcomeService(InMemoryRepository repository) {
        return new BattleOutcomeCommitService(repository, repository);
    }

    private static InMemoryRepository standardRepository() {
        InMemoryRepository repository = new InMemoryRepository();
        repository.putPlayer(new CanonicalPlayerState(
                "player-1", Set.of("Ace Trainer"), Map.of("Command", 4), Set.of("Sky"), 17));
        repository.putPokemon(new CanonicalPokemonState(
                "pokemon-1", "player-1", "cobblemon:charizard", 42,
                Set.of("Sky", "Power 5"), "item-charcoal", 8));
        repository.putItem(new CanonicalItemInstance(
                "item-charcoal", "player-1", "autoptu:charcoal", 1, 31));
        repository.putItem(new CanonicalItemInstance(
                "item-potion", "player-1", "autoptu:hyper_potion", 4, 44));
        return repository;
    }

    private static final class InMemoryRepository implements
            CanonicalStateRepository,
            CanonicalAssetRepository,
            BattleSnapshotRepository,
            BattleOutcomeRepository {
        private final Map<String, CanonicalPlayerState> players = new HashMap<>();
        private final Map<String, CanonicalPokemonState> pokemon = new HashMap<>();
        private final Map<String, CanonicalItemInstance> items = new HashMap<>();
        private final Map<String, BattleAuthoritySnapshot> snapshots = new HashMap<>();
        private final Map<String, BattleOutcomeCommit> committedOutcomes = new HashMap<>();
        private final Set<String> lockedPokemon = new HashSet<>();
        private final Set<String> lockedItems = new HashSet<>();
        private String bumpItemBeforeNextOutcomeCommit;

        void putPlayer(CanonicalPlayerState state) {
            players.put(state.playerId(), state);
        }

        void putPokemon(CanonicalPokemonState state) {
            pokemon.put(state.pokemonId(), state);
        }

        void putItem(CanonicalItemInstance state) {
            items.put(state.itemInstanceId(), state);
        }

        @Override
        public Optional<CanonicalPlayerState> findPlayer(String playerId) {
            return Optional.ofNullable(players.get(playerId));
        }

        @Override
        public Optional<CanonicalPokemonState> findPokemon(String pokemonId) {
            return Optional.ofNullable(pokemon.get(pokemonId));
        }

        @Override
        public Optional<CanonicalItemInstance> findItem(String itemInstanceId) {
            return Optional.ofNullable(items.get(itemInstanceId));
        }

        @Override
        public Optional<ItemReservation> findReservation(String reservationId) {
            return Optional.empty();
        }

        @Override
        public boolean tryReserveItem(ItemReservation reservation) {
            return false;
        }

        @Override
        public boolean commitItemReservation(String reservationId, String playerId) {
            return false;
        }

        @Override
        public boolean releaseItemReservation(String reservationId, String playerId) {
            return false;
        }

        @Override
        public Optional<BattleAuthoritySnapshot> findSnapshot(String reservationId) {
            return Optional.ofNullable(snapshots.get(reservationId));
        }

        @Override
        public synchronized boolean tryReserveSnapshot(BattleAuthoritySnapshot snapshot) {
            CanonicalPlayerState player = players.get(snapshot.playerId());
            if (player == null
                    || player.revision() != snapshot.trainer().revision()
                    || snapshots.containsKey(snapshot.reservationId())
                    || committedOutcomes.containsKey(snapshot.reservationId())) {
                return false;
            }

            for (BattlePokemonSnapshot requested : snapshot.roster()) {
                CanonicalPokemonState live = pokemon.get(requested.pokemonId());
                if (live == null
                        || !live.ownerPlayerId().equals(snapshot.playerId())
                        || live.revision() != requested.revision()
                        || !live.speciesId().equals(requested.speciesId())
                        || live.level() != requested.level()
                        || !Objects.equals(live.heldItemInstanceId(), requested.heldItemInstanceId())
                        || lockedPokemon.contains(requested.pokemonId())) {
                    return false;
                }
            }

            for (BattleItemSnapshot requested : snapshot.items()) {
                CanonicalItemInstance live = items.get(requested.itemInstanceId());
                if (live == null
                        || !live.ownerPlayerId().equals(snapshot.playerId())
                        || live.revision() != requested.revision()
                        || !live.templateId().equals(requested.templateId())
                        || live.quantity() < requested.reservedQuantity()
                        || lockedItems.contains(requested.itemInstanceId())) {
                    return false;
                }
            }

            snapshot.roster().forEach(p -> lockedPokemon.add(p.pokemonId()));
            snapshot.items().forEach(i -> lockedItems.add(i.itemInstanceId()));
            snapshots.put(snapshot.reservationId(), snapshot);
            return true;
        }

        @Override
        public synchronized boolean releaseSnapshot(String reservationId, String playerId) {
            BattleAuthoritySnapshot snapshot = snapshots.get(reservationId);
            if (snapshot == null || !snapshot.playerId().equals(playerId)) {
                return false;
            }
            unlockAndRemove(snapshot);
            return true;
        }

        @Override
        public Optional<BattleOutcomeCommit> findCommittedOutcome(String reservationId) {
            return Optional.ofNullable(committedOutcomes.get(reservationId));
        }

        @Override
        public synchronized boolean tryCommitOutcome(
                BattleAuthoritySnapshot snapshot,
                BattleOutcomeCommit outcome
        ) {
            if (bumpItemBeforeNextOutcomeCommit != null) {
                CanonicalItemInstance live = items.get(bumpItemBeforeNextOutcomeCommit);
                items.put(live.itemInstanceId(), new CanonicalItemInstance(
                        live.itemInstanceId(), live.ownerPlayerId(), live.templateId(),
                        live.quantity(), live.revision() + 1));
                bumpItemBeforeNextOutcomeCommit = null;
            }

            BattleAuthoritySnapshot active = snapshots.get(snapshot.reservationId());
            if (active == null
                    || !active.equals(snapshot)
                    || committedOutcomes.containsKey(snapshot.reservationId())
                    || !outcome.reservationId().equals(snapshot.reservationId())
                    || !outcome.playerId().equals(snapshot.playerId())) {
                return false;
            }

            CanonicalPlayerState player = players.get(snapshot.playerId());
            if (player == null || player.revision() != outcome.trainerRevision()) {
                return false;
            }

            for (BattlePokemonSnapshot frozen : snapshot.roster()) {
                CanonicalPokemonState live = pokemon.get(frozen.pokemonId());
                Long expectedRevision = outcome.pokemonRevisions().get(frozen.pokemonId());
                if (live == null
                        || expectedRevision == null
                        || expectedRevision != frozen.revision()
                        || live.revision() != frozen.revision()
                        || !live.ownerPlayerId().equals(snapshot.playerId())
                        || !live.speciesId().equals(frozen.speciesId())
                        || live.level() != frozen.level()
                        || !Objects.equals(live.heldItemInstanceId(), frozen.heldItemInstanceId())
                        || !lockedPokemon.contains(frozen.pokemonId())) {
                    return false;
                }
            }

            for (BattleItemSnapshot frozen : snapshot.items()) {
                CanonicalItemInstance live = items.get(frozen.itemInstanceId());
                if (live == null
                        || live.revision() != frozen.revision()
                        || !live.ownerPlayerId().equals(snapshot.playerId())
                        || !live.templateId().equals(frozen.templateId())
                        || live.quantity() < frozen.reservedQuantity()
                        || !lockedItems.contains(frozen.itemInstanceId())) {
                    return false;
                }
            }

            Map<String, CanonicalItemInstance> replacements = new HashMap<>();
            for (BattleItemConsumption consumption : outcome.consumedItems()) {
                BattleItemSnapshot frozen = snapshot.items().stream()
                        .filter(item -> item.itemInstanceId().equals(consumption.itemInstanceId()))
                        .findFirst()
                        .orElse(null);
                CanonicalItemInstance live = items.get(consumption.itemInstanceId());
                if (frozen == null
                        || frozen.heldItem()
                        || consumption.quantity() > frozen.reservedQuantity()
                        || consumption.expectedRevision() != frozen.revision()
                        || live == null
                        || !live.templateId().equals(consumption.templateId())
                        || live.quantity() < consumption.quantity()) {
                    return false;
                }
                replacements.put(live.itemInstanceId(), new CanonicalItemInstance(
                        live.itemInstanceId(), live.ownerPlayerId(), live.templateId(),
                        live.quantity() - consumption.quantity(), live.revision() + 1));
            }

            replacements.forEach(items::put);
            unlockAndRemove(snapshot);
            committedOutcomes.put(snapshot.reservationId(), outcome);
            return true;
        }

        private void unlockAndRemove(BattleAuthoritySnapshot snapshot) {
            snapshot.roster().forEach(p -> lockedPokemon.remove(p.pokemonId()));
            snapshot.items().forEach(i -> lockedItems.remove(i.itemInstanceId()));
            snapshots.remove(snapshot.reservationId());
        }
    }
}
