package io.autoptu.cobblemon.fabric.persistence;

import io.autoptu.cobblemon.authority.CanonicalShopCatalogue;
import io.autoptu.cobblemon.authority.CanonicalShopPurchaseService;
import io.autoptu.cobblemon.authority.FileCanonicalItemReservationRepository;
import io.autoptu.cobblemon.authority.FileCanonicalPlayerEncounterProfileRepository;
import io.autoptu.cobblemon.authority.FileCanonicalPokemonRepository;
import io.autoptu.cobblemon.authority.FileCanonicalShopPurchaseRepository;
import io.autoptu.cobblemon.authority.FileCanonicalShopStockRepository;
import io.autoptu.cobblemon.authority.FileCanonicalWalletRepository;
import io.autoptu.cobblemon.authority.FileCraftIngredientDepositHandoffRepository;
import io.autoptu.cobblemon.authority.FileFieldCampSetupAttemptRepository;
import io.autoptu.cobblemon.authority.FileVersionedCanonicalStateRepository;
import io.autoptu.cobblemon.authority.FileWorldTaskCraftAttemptRepository;
import io.autoptu.cobblemon.fabric.battle.WorldScopedCanonicalWildEncounterBlueprintRegistry;
import io.autoptu.cobblemon.fabric.battle.WorldScopedWildEncounterCorrelationRegistry;
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
            FileCanonicalItemReservationRepository assets,
            FileCanonicalWalletRepository wallets,
            FileCanonicalShopStockRepository shopStock,
            FileCanonicalShopPurchaseRepository shopPurchases,
            FileWorldTaskCraftAttemptRepository craftAttempts,
            FileCraftIngredientDepositHandoffRepository craftDepositHandoffs,
            FileFieldCampSetupAttemptRepository fieldCampAttempts,
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

    public static FileVersionedCanonicalStateRepository requireRepository(MinecraftServer server) { return requireStores(server).players(); }
    public static FileCanonicalPlayerEncounterProfileRepository requireEncounterProfileRepository(MinecraftServer server) { return requireStores(server).encounterProfiles(); }
    public static FileCanonicalPokemonRepository requirePokemonRepository(MinecraftServer server) { return requireStores(server).pokemon(); }
    public static FileCanonicalItemReservationRepository requireAssetRepository(MinecraftServer server) { return requireStores(server).assets(); }
    public static FileCanonicalWalletRepository requireWalletRepository(MinecraftServer server) { return requireStores(server).wallets(); }
    public static FileCanonicalShopStockRepository requireShopStockRepository(MinecraftServer server) { return requireStores(server).shopStock(); }
    public static FileCanonicalShopPurchaseRepository requireShopPurchaseRepository(MinecraftServer server) { return requireStores(server).shopPurchases(); }
    public static FileWorldTaskCraftAttemptRepository requireCraftAttemptRepository(MinecraftServer server) { return requireStores(server).craftAttempts(); }
    public static FileCraftIngredientDepositHandoffRepository requireCraftDepositHandoffRepository(MinecraftServer server) { return requireStores(server).craftDepositHandoffs(); }
    public static FileFieldCampSetupAttemptRepository requireFieldCampSetupAttemptRepository(MinecraftServer server) { return requireStores(server).fieldCampAttempts(); }
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
        FileCanonicalPokemonRepository pokemon = new FileCanonicalPokemonRepository(root);
        FileCanonicalItemReservationRepository assets = new FileCanonicalItemReservationRepository(root, pokemon::findPokemon);
        FileCanonicalWalletRepository wallets = new FileCanonicalWalletRepository(root);
        FileCanonicalShopStockRepository shopStock = new FileCanonicalShopStockRepository(root);
        FileCanonicalShopPurchaseRepository shopPurchases = new FileCanonicalShopPurchaseRepository(root);
        Stores stores = new Stores(
                new FileVersionedCanonicalStateRepository(root),
                new FileCanonicalPlayerEncounterProfileRepository(root),
                pokemon,
                assets,
                wallets,
                shopStock,
                shopPurchases,
                new FileWorldTaskCraftAttemptRepository(root),
                new FileCraftIngredientDepositHandoffRepository(root),
                new FileFieldCampSetupAttemptRepository(root),
                new WorldScopedCanonicalWildEncounterBlueprintRegistry(),
                new WorldScopedWildEncounterCorrelationRegistry()
        );
        synchronized (STORES) {
            if (STORES.putIfAbsent(server, stores) != null) throw new IllegalStateException("canonical stores already initialized for server");
        }
        new CanonicalShopPurchaseService(
                CanonicalShopCatalogue.DEFAULT,
                wallets,
                shopStock,
                assets,
                shopPurchases
        ).recoverPending();
    }

    private static void stop(MinecraftServer server) {
        synchronized (STORES) { STORES.remove(server); }
    }
}
