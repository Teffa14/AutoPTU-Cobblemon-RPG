package io.autoptu.cobblemon.fabric.persistence;

import io.autoptu.cobblemon.authority.CanonicalItemStorageTransferService;
import io.autoptu.cobblemon.authority.CanonicalPokemonStorageTransferService;
import io.autoptu.cobblemon.authority.CanonicalShopCatalogue;
import io.autoptu.cobblemon.authority.CanonicalShopPurchaseService;
import io.autoptu.cobblemon.authority.CanonicalShopSaleService;
import io.autoptu.cobblemon.authority.CanonicalShopSellCatalogue;
import io.autoptu.cobblemon.authority.FileCanonicalItemReservationRepository;
import io.autoptu.cobblemon.authority.FileCanonicalItemStorageRepository;
import io.autoptu.cobblemon.authority.FileCanonicalItemStorageTransferRepository;
import io.autoptu.cobblemon.authority.FileCanonicalLocationDiscoveryRepository;
import io.autoptu.cobblemon.authority.FileCanonicalNpcRelationshipRepository;
import io.autoptu.cobblemon.authority.FileCanonicalPlayerEncounterProfileRepository;
import io.autoptu.cobblemon.authority.FileCanonicalPokemonRepository;
import io.autoptu.cobblemon.authority.FileCanonicalPokemonStorageRepository;
import io.autoptu.cobblemon.authority.FileCanonicalPokemonTransferRepository;
import io.autoptu.cobblemon.authority.FileCanonicalQuestJournalRepository;
import io.autoptu.cobblemon.authority.FileCanonicalQuestObjectiveRepository;
import io.autoptu.cobblemon.authority.FileCanonicalShopPurchaseRepository;
import io.autoptu.cobblemon.authority.FileCanonicalShopSaleRepository;
import io.autoptu.cobblemon.authority.FileCanonicalShopStockRepository;
import io.autoptu.cobblemon.authority.FileCanonicalWalletRepository;
import io.autoptu.cobblemon.authority.FileCanonicalWorldEventObjectRepository;
import io.autoptu.cobblemon.authority.FileCanonicalWorldStoryRepository;
import io.autoptu.cobblemon.authority.FileCraftIngredientDepositHandoffRepository;
import io.autoptu.cobblemon.authority.FileFieldCampSetupAttemptRepository;
import io.autoptu.cobblemon.authority.FileVersionedCanonicalStateRepository;
import io.autoptu.cobblemon.authority.FileWorldTaskCraftAttemptRepository;
import io.autoptu.cobblemon.fabric.battle.FileWorldEncounterTriggerRequestRepository;
import io.autoptu.cobblemon.fabric.battle.WorldScopedCanonicalWildEncounterBlueprintRegistry;
import io.autoptu.cobblemon.fabric.battle.WorldScopedWildEncounterCorrelationRegistry;
import io.autoptu.cobblemon.fabric.world.VisibleWildPokemonEncounterRuntime;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Owns world-scoped canonical RPG state for each live Minecraft server instance. */
public final class FabricCanonicalPlayerStoreRuntime {
    private record Stores(
            FileVersionedCanonicalStateRepository players,
            FileCanonicalPlayerEncounterProfileRepository encounterProfiles,
            FileCanonicalPokemonRepository pokemon,
            FileCanonicalPokemonStorageRepository pokemonStorage,
            FileCanonicalPokemonTransferRepository pokemonTransfers,
            FileCanonicalItemReservationRepository assets,
            FileCanonicalItemStorageRepository itemStorage,
            FileCanonicalItemStorageTransferRepository itemStorageTransfers,
            FileCanonicalWalletRepository wallets,
            FileCanonicalQuestJournalRepository questJournals,
            FileCanonicalQuestObjectiveRepository questObjectives,
            FileCanonicalLocationDiscoveryRepository locationDiscoveries,
            FileCanonicalWorldEventObjectRepository worldEventObjects,
            FileCanonicalNpcRelationshipRepository npcRelationships,
            FileCanonicalWorldStoryRepository worldStory,
            FileCanonicalShopStockRepository shopStock,
            FileCanonicalShopPurchaseRepository shopPurchases,
            FileCanonicalShopSaleRepository shopSales,
            FileWorldTaskCraftAttemptRepository craftAttempts,
            FileCraftIngredientDepositHandoffRepository craftDepositHandoffs,
            FileFieldCampSetupAttemptRepository fieldCampAttempts,
            FileWorldEncounterTriggerRequestRepository activeEncounterSessions,
            WorldScopedCanonicalWildEncounterBlueprintRegistry wildEncounterBlueprints,
            WorldScopedWildEncounterCorrelationRegistry wildEncounterCorrelations
    ) {}

    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static final Map<MinecraftServer, Stores> STORES = new IdentityHashMap<>();

    private FabricCanonicalPlayerStoreRuntime() {}

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) return;
        ServerLifecycleEvents.SERVER_STARTED.register(FabricCanonicalPlayerStoreRuntime::start);
        ServerLifecycleEvents.SERVER_STOPPED.register(FabricCanonicalPlayerStoreRuntime::stop);
    }

    public static boolean storesAvailable(MinecraftServer server) {
        if (server == null) return false;
        synchronized (STORES) { return STORES.containsKey(server); }
    }

    public static FileVersionedCanonicalStateRepository requireRepository(MinecraftServer server) { return requireStores(server).players(); }
    public static FileCanonicalPlayerEncounterProfileRepository requireEncounterProfileRepository(MinecraftServer server) { return requireStores(server).encounterProfiles(); }
    public static FileCanonicalPokemonRepository requirePokemonRepository(MinecraftServer server) { return requireStores(server).pokemon(); }
    public static FileCanonicalPokemonStorageRepository requirePokemonStorageRepository(MinecraftServer server) { return requireStores(server).pokemonStorage(); }
    public static FileCanonicalPokemonTransferRepository requirePokemonTransferRepository(MinecraftServer server) { return requireStores(server).pokemonTransfers(); }
    public static FileCanonicalItemReservationRepository requireAssetRepository(MinecraftServer server) { return requireStores(server).assets(); }
    public static FileCanonicalItemStorageRepository requireItemStorageRepository(MinecraftServer server) { return requireStores(server).itemStorage(); }
    public static FileCanonicalItemStorageTransferRepository requireItemStorageTransferRepository(MinecraftServer server) { return requireStores(server).itemStorageTransfers(); }
    public static FileCanonicalWalletRepository requireWalletRepository(MinecraftServer server) { return requireStores(server).wallets(); }
    public static FileCanonicalQuestJournalRepository requireQuestJournalRepository(MinecraftServer server) { return requireStores(server).questJournals(); }
    public static FileCanonicalQuestObjectiveRepository requireQuestObjectiveRepository(MinecraftServer server) { return requireStores(server).questObjectives(); }
    public static FileCanonicalLocationDiscoveryRepository requireLocationDiscoveryRepository(MinecraftServer server) { return requireStores(server).locationDiscoveries(); }
    public static FileCanonicalWorldEventObjectRepository requireWorldEventObjectRepository(MinecraftServer server) { return requireStores(server).worldEventObjects(); }
    public static FileCanonicalNpcRelationshipRepository requireNpcRelationshipRepository(MinecraftServer server) { return requireStores(server).npcRelationships(); }
    public static FileCanonicalWorldStoryRepository requireWorldStoryRepository(MinecraftServer server) { return requireStores(server).worldStory(); }
    public static FileCanonicalShopStockRepository requireShopStockRepository(MinecraftServer server) { return requireStores(server).shopStock(); }
    public static FileCanonicalShopPurchaseRepository requireShopPurchaseRepository(MinecraftServer server) { return requireStores(server).shopPurchases(); }
    public static FileCanonicalShopSaleRepository requireShopSaleRepository(MinecraftServer server) { return requireStores(server).shopSales(); }
    public static FileWorldTaskCraftAttemptRepository requireCraftAttemptRepository(MinecraftServer server) { return requireStores(server).craftAttempts(); }
    public static FileCraftIngredientDepositHandoffRepository requireCraftDepositHandoffRepository(MinecraftServer server) { return requireStores(server).craftDepositHandoffs(); }
    public static FileFieldCampSetupAttemptRepository requireFieldCampSetupAttemptRepository(MinecraftServer server) { return requireStores(server).fieldCampAttempts(); }
    public static FileWorldEncounterTriggerRequestRepository requireActiveEncounterSessionRepository(MinecraftServer server) { return requireStores(server).activeEncounterSessions(); }
    public static WorldScopedCanonicalWildEncounterBlueprintRegistry requireWildEncounterBlueprintRegistry(MinecraftServer server) { return requireStores(server).wildEncounterBlueprints(); }
    public static WorldScopedWildEncounterCorrelationRegistry requireWildEncounterCorrelationRegistry(MinecraftServer server) { return requireStores(server).wildEncounterCorrelations(); }

    static Path storageRoot(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        return storageRoot(server.getSavePath(WorldSavePath.ROOT));
    }

    static Path storageRoot(Path worldSaveRoot) {
        Objects.requireNonNull(worldSaveRoot, "worldSaveRoot");
        return worldSaveRoot.resolve("autoptu").resolve("canonical-state").normalize();
    }

    private static Stores requireStores(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        synchronized (STORES) {
            Stores stores = STORES.get(server);
            if (stores == null) throw new IllegalStateException("canonical stores are unavailable for this server lifecycle");
            return stores;
        }
    }

    private static void start(MinecraftServer server) {
        Path root = storageRoot(server);
        FileCanonicalPlayerEncounterProfileRepository encounterProfiles = new FileCanonicalPlayerEncounterProfileRepository(root);
        FileCanonicalPokemonRepository pokemon = new FileCanonicalPokemonRepository(root);
        FileCanonicalPokemonStorageRepository pokemonStorage = new FileCanonicalPokemonStorageRepository(root);
        FileCanonicalPokemonTransferRepository pokemonTransfers = new FileCanonicalPokemonTransferRepository(root);
        FileCanonicalItemReservationRepository assets = new FileCanonicalItemReservationRepository(root, pokemon::findPokemon);
        FileCanonicalItemStorageRepository itemStorage = new FileCanonicalItemStorageRepository(root);
        FileCanonicalItemStorageTransferRepository itemStorageTransfers = new FileCanonicalItemStorageTransferRepository(root);
        FileCanonicalWalletRepository wallets = new FileCanonicalWalletRepository(root);
        FileCanonicalQuestJournalRepository questJournals = new FileCanonicalQuestJournalRepository(root);
        FileCanonicalQuestObjectiveRepository questObjectives = new FileCanonicalQuestObjectiveRepository(root);
        FileCanonicalLocationDiscoveryRepository locationDiscoveries = new FileCanonicalLocationDiscoveryRepository(root);
        FileCanonicalWorldEventObjectRepository worldEventObjects = new FileCanonicalWorldEventObjectRepository(root);
        FileCanonicalNpcRelationshipRepository npcRelationships = new FileCanonicalNpcRelationshipRepository(root);
        FileCanonicalWorldStoryRepository worldStory = new FileCanonicalWorldStoryRepository(root);
        FileCanonicalShopStockRepository shopStock = new FileCanonicalShopStockRepository(root);
        FileCanonicalShopPurchaseRepository shopPurchases = new FileCanonicalShopPurchaseRepository(root);
        FileCanonicalShopSaleRepository shopSales = new FileCanonicalShopSaleRepository(root);
        FileWorldEncounterTriggerRequestRepository activeEncounterSessions = new FileWorldEncounterTriggerRequestRepository(root);
        Stores stores = new Stores(
                new FileVersionedCanonicalStateRepository(root), encounterProfiles, pokemon, pokemonStorage, pokemonTransfers,
                assets, itemStorage, itemStorageTransfers, wallets, questJournals, questObjectives, locationDiscoveries, worldEventObjects, npcRelationships,
                worldStory, shopStock, shopPurchases, shopSales, new FileWorldTaskCraftAttemptRepository(root), new FileCraftIngredientDepositHandoffRepository(root),
                new FileFieldCampSetupAttemptRepository(root), activeEncounterSessions, new WorldScopedCanonicalWildEncounterBlueprintRegistry(),
                new WorldScopedWildEncounterCorrelationRegistry());
        synchronized (STORES) {
            if (STORES.putIfAbsent(server, stores) != null) throw new IllegalStateException("canonical stores already initialized for server");
        }
        VisibleWildPokemonEncounterRuntime.bindRequestRepository(activeEncounterSessions);
        new CanonicalShopPurchaseService(CanonicalShopCatalogue.DEFAULT, wallets, shopStock, assets, shopPurchases).recoverPending();
        new CanonicalShopSaleService(CanonicalShopSellCatalogue.DEFAULT, wallets, assets, shopSales).recoverPending();
        new CanonicalPokemonStorageTransferService(encounterProfiles, pokemonStorage, pokemon, pokemonTransfers).recoverPending();
        new CanonicalItemStorageTransferService(assets, itemStorage, itemStorageTransfers).recoverPending();
    }

    private static void stop(MinecraftServer server) {
        VisibleWildPokemonEncounterRuntime.clearServerHandoffs(server);
        VisibleWildPokemonEncounterRuntime.resetRequestRepository();
        synchronized (STORES) { STORES.remove(server); }
    }
}
