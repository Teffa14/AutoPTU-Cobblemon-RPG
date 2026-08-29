package io.autoptu.cobblemon.fabric.persistence;

import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import io.autoptu.cobblemon.authority.CanonicalItemInstance;
import io.autoptu.cobblemon.authority.CanonicalPlayerEncounterProfile;
import io.autoptu.cobblemon.authority.CanonicalPlayerState;
import io.autoptu.cobblemon.authority.CanonicalPokemonState;
import io.autoptu.cobblemon.authority.CanonicalQuestCatalogue;
import io.autoptu.cobblemon.authority.CanonicalQuestJournalService;
import io.autoptu.cobblemon.authority.CanonicalQuestTrackingService;
import io.autoptu.cobblemon.authority.FileCanonicalItemStorageRepository;
import io.autoptu.cobblemon.authority.FileCanonicalQuestJournalRepository;
import io.autoptu.cobblemon.authority.ItemReservation;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Two-boot production-runtime proof for world-scoped canonical persistence.
 *
 * CI runs one dedicated server in seed mode, stops it, then boots the same world in verify mode.
 * The fixtures are canonical server-owned test data; no Minecraft or Cobblemon state is imported.
 */
public final class FabricCanonicalPlayerStoreRestartSmoke {
    public static final String MODE_PROPERTY = "autoptu.liveCanonicalStoreRestartSmoke";
    public static final String SEED_SUCCESS_LOG = "AutoPTU live canonical player store seed smoke passed";
    public static final String RESTART_SUCCESS_LOG = "AutoPTU live canonical player store restart smoke passed";

    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");
    private static final String PLAYER_ID = "integration-restart-player";
    private static final String POKEMON_ID = "restart-pokemon-1";
    private static final String ITEM_ID = "restart-item-1";
    private static final String RESERVATION_ID = "restart-item-reservation";
    private static final String QUEST_ID = "cedar-field-notes";
    private static final String STORED_ITEM_TEMPLATE = "restart_storage_supply";
    private static final String STORAGE_SEED_RECEIPT = "restart-storage-seed";

    private static final CanonicalPlayerState FIXTURE = new CanonicalPlayerState(
            PLAYER_ID,
            Set.of("Ace Trainer", "Commander"),
            Map.of("athletics", 6, "command", 5),
            Set.of("ride", "swim"),
            Set.of("Orders", "Focused Training"),
            4,
            2,
            37,
            "team-restart-smoke",
            7
    );
    private static final CanonicalPokemonState POKEMON_FIXTURE = new CanonicalPokemonState(
            POKEMON_ID,
            PLAYER_ID,
            "pikachu",
            23,
            Set.of("tracker"),
            Set.of("poisoned"),
            ITEM_ID,
            5
    );
    private static final CanonicalItemInstance ITEM_FIXTURE = new CanonicalItemInstance(
            ITEM_ID,
            PLAYER_ID,
            "potion",
            3,
            11
    );
    private static final ItemReservation ITEM_RESERVATION = new ItemReservation(
            RESERVATION_ID,
            PLAYER_ID,
            ITEM_ID,
            "potion",
            2,
            11
    );
    private static final CanonicalPlayerEncounterProfile ENCOUNTER_FIXTURE =
            new CanonicalPlayerEncounterProfile(
                    PLAYER_ID,
                    List.of(POKEMON_ID),
                    Map.of(ITEM_ID, 2),
                    new BattleArenaSnapshot("minecraft:overworld", 12, 64, -8, 1, 0, 0, 1),
                    3
            );

    private FabricCanonicalPlayerStoreRestartSmoke() {}

    public static void registerIfEnabled() {
        String mode = System.getProperty(MODE_PROPERTY, "").strip().toLowerCase(Locale.ROOT);
        if (mode.isEmpty()) return;
        if (!mode.equals("seed") && !mode.equals("verify")) {
            throw new IllegalArgumentException(MODE_PROPERTY + " must be seed or verify");
        }
        ServerLifecycleEvents.SERVER_STARTED.register(server -> run(server, mode));
    }

    private static void run(MinecraftServer server, String mode) {
        var repository = FabricCanonicalPlayerStoreRuntime.requireRepository(server);
        var encounterProfiles = FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(server);
        var pokemon = FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(server);
        var assets = FabricCanonicalPlayerStoreRuntime.requireAssetRepository(server);
        var itemStorage = FabricCanonicalPlayerStoreRuntime.requireItemStorageRepository(server);
        var questJournals = FabricCanonicalPlayerStoreRuntime.requireQuestJournalRepository(server);

        if (mode.equals("seed")) {
            if (!repository.createPlayerIfAbsent(FIXTURE)) {
                throw new IllegalStateException("canonical restart smoke fixture already exists before seed boot");
            }
            if (!encounterProfiles.createProfileIfAbsent(ENCOUNTER_FIXTURE)) {
                throw new IllegalStateException("canonical encounter profile already exists before seed boot");
            }
            if (!pokemon.createPokemonIfAbsent(POKEMON_FIXTURE)) {
                throw new IllegalStateException("canonical Pokemon fixture already exists before seed boot");
            }
            if (!assets.createItemIfAbsent(ITEM_FIXTURE)) {
                throw new IllegalStateException("canonical item fixture already exists before seed boot");
            }
            if (!assets.tryReserveItem(ITEM_RESERVATION)) {
                throw new IllegalStateException("canonical item reservation was not persisted during seed boot");
            }
            var stored = itemStorage.applyDeltaOnce(PLAYER_ID, STORAGE_SEED_RECEIPT, STORED_ITEM_TEMPLATE, 4);
            if (stored.quantity(STORED_ITEM_TEMPLATE) != 4 || stored.revision() != 1L) {
                throw new IllegalStateException("canonical item storage was not persisted during seed boot");
            }
            var questAccept = new CanonicalQuestJournalService(CanonicalQuestCatalogue.DEFAULT, questJournals)
                    .accept(PLAYER_ID, "cedar-ranger", QUEST_ID);
            if (!questAccept.newlyAccepted()) {
                throw new IllegalStateException("canonical quest fixture already existed before seed boot");
            }
            var tracked = new CanonicalQuestTrackingService(CanonicalQuestCatalogue.DEFAULT, questJournals)
                    .track(PLAYER_ID, QUEST_ID);
            if (!tracked.changed() || tracked.journalRevision() != 2L) {
                throw new IllegalStateException("canonical tracked quest was not persisted during seed boot");
            }

            requireExactSeedState(repository, encounterProfiles, pokemon, assets, itemStorage, questJournals);
            LOGGER.info(SEED_SUCCESS_LOG);
            return;
        }

        CanonicalPlayerState persisted = repository.findPlayer(PLAYER_ID).orElseThrow(
                () -> new IllegalStateException("canonical restart smoke fixture missing after server restart"));
        CanonicalPlayerEncounterProfile persistedEncounter = encounterProfiles.findProfile(PLAYER_ID).orElseThrow(
                () -> new IllegalStateException("canonical encounter profile missing after server restart"));
        CanonicalPokemonState persistedPokemon = pokemon.findPokemon(POKEMON_ID).orElseThrow(
                () -> new IllegalStateException("canonical Pokemon missing after server restart"));
        CanonicalItemInstance persistedItem = assets.findItem(ITEM_ID).orElseThrow(
                () -> new IllegalStateException("canonical item missing after server restart"));
        ItemReservation persistedReservation = assets.findReservation(RESERVATION_ID).orElseThrow(
                () -> new IllegalStateException("canonical item reservation missing after server restart"));
        var persistedStorage = itemStorage.findOrCreate(PLAYER_ID);
        FileCanonicalQuestJournalRepository.JournalState persistedQuestJournal = questJournals.find(PLAYER_ID).orElseThrow(
                () -> new IllegalStateException("canonical quest journal missing after server restart"));

        if (!FIXTURE.equals(persisted)) throw new IllegalStateException("canonical restart smoke state changed across server restart");
        if (!ENCOUNTER_FIXTURE.equals(persistedEncounter)) throw new IllegalStateException("canonical encounter profile changed across server restart");
        if (!POKEMON_FIXTURE.equals(persistedPokemon)) throw new IllegalStateException("canonical Pokemon changed across server restart");
        if (!ITEM_FIXTURE.equals(persistedItem)) throw new IllegalStateException("canonical item changed across server restart");
        if (!ITEM_RESERVATION.equals(persistedReservation)) throw new IllegalStateException("canonical item reservation changed across server restart");
        if (persistedStorage.quantity(STORED_ITEM_TEMPLATE) != 4 || persistedStorage.revision() != 1L
                || !persistedStorage.appliedTransferIds().contains(STORAGE_SEED_RECEIPT)) {
            throw new IllegalStateException("canonical item storage changed across server restart");
        }
        requireQuestState(persistedQuestJournal);
        LOGGER.info(RESTART_SUCCESS_LOG);
    }

    private static void requireExactSeedState(
            io.autoptu.cobblemon.authority.FileVersionedCanonicalStateRepository repository,
            io.autoptu.cobblemon.authority.FileCanonicalPlayerEncounterProfileRepository encounterProfiles,
            io.autoptu.cobblemon.authority.FileCanonicalPokemonRepository pokemon,
            io.autoptu.cobblemon.authority.FileCanonicalItemReservationRepository assets,
            FileCanonicalItemStorageRepository itemStorage,
            FileCanonicalQuestJournalRepository questJournals
    ) {
        if (!FIXTURE.equals(repository.findPlayer(PLAYER_ID).orElseThrow())) throw new IllegalStateException("canonical restart smoke seed did not round-trip exact Trainer state");
        if (!ENCOUNTER_FIXTURE.equals(encounterProfiles.findProfile(PLAYER_ID).orElseThrow())) throw new IllegalStateException("canonical encounter profile did not round-trip exact state");
        if (!POKEMON_FIXTURE.equals(pokemon.findPokemon(POKEMON_ID).orElseThrow())) throw new IllegalStateException("canonical Pokemon did not round-trip exact state");
        if (!ITEM_FIXTURE.equals(assets.findItem(ITEM_ID).orElseThrow())) throw new IllegalStateException("canonical item did not round-trip exact state");
        if (!ITEM_RESERVATION.equals(assets.findReservation(RESERVATION_ID).orElseThrow())) throw new IllegalStateException("canonical item reservation did not round-trip exact state");
        var stored = itemStorage.findOrCreate(PLAYER_ID);
        if (stored.quantity(STORED_ITEM_TEMPLATE) != 4 || stored.revision() != 1L
                || !stored.appliedTransferIds().contains(STORAGE_SEED_RECEIPT)) {
            throw new IllegalStateException("canonical item storage did not round-trip exact state");
        }
        requireQuestState(questJournals.find(PLAYER_ID).orElseThrow());
    }

    private static void requireQuestState(FileCanonicalQuestJournalRepository.JournalState journal) {
        var entry = journal.entries().get(QUEST_ID);
        if (journal.revision() != 2L || entry == null
                || entry.state() != FileCanonicalQuestJournalRepository.QuestState.ACCEPTED
                || entry.acceptedRevision() != 1L
                || !QUEST_ID.equals(journal.trackedQuestId())) {
            throw new IllegalStateException("canonical quest journal changed across persistence boundary");
        }
    }
}
