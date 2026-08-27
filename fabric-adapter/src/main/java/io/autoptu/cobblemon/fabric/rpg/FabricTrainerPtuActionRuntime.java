package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.FileMonotonicRpgDayClock;
import io.autoptu.cobblemon.authority.FileTrainerPtuDailyActionLedger;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Server-owned Minecraft bridge for PTU daily action windows.
 *
 * <p>Vanilla Minecraft activity never consumes this ledger. Only a server-side RPG mechanic that
 * has already resolved its canonical PTU action/frequency policy may call {@link #tryReserveDaily}.
 * Battle turn/round action economy and all PTU effects remain AutoPTU-Java responsibilities.</p>
 */
public final class FabricTrainerPtuActionRuntime {
    private record State(FileMonotonicRpgDayClock clock, FileTrainerPtuDailyActionLedger ledger) {}

    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static final Map<MinecraftServer, State> STATES = new IdentityHashMap<>();

    private FabricTrainerPtuActionRuntime() {}

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) return;
        ServerLifecycleEvents.SERVER_STARTED.register(FabricTrainerPtuActionRuntime::start);
        ServerLifecycleEvents.SERVER_STOPPED.register(FabricTrainerPtuActionRuntime::stop);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("trainer")
                                .then(CommandManager.literal("actions")
                                        .executes(context -> showActions(context.getSource()))))));
    }

    /**
     * Reserves one daily use for a canonical RPG action.
     *
     * <p>The caller is a trusted server service and must obtain {@code canonicalActionId} and
     * {@code maxUsesPerDay} from authoritative PTU content. This method intentionally has no client
     * payload overload.</p>
     */
    public static Optional<FileTrainerPtuDailyActionLedger.ConsumeResult> tryReserveDaily(
            MinecraftServer server,
            String canonicalPlayerId,
            String canonicalActionId,
            int maxUsesPerDay
    ) {
        Objects.requireNonNull(server, "server");
        if (canonicalPlayerId == null || canonicalPlayerId.isBlank()) return Optional.empty();
        if (FabricCanonicalPlayerStoreRuntime.requireRepository(server).findPlayer(canonicalPlayerId).isEmpty()) {
            return Optional.empty();
        }
        State state = requireState(server);
        long dayId = observeDay(server, state);
        return Optional.of(state.ledger().tryConsume(
                canonicalPlayerId,
                canonicalActionId,
                maxUsesPerDay,
                dayId));
    }

    public static long currentRpgDay(MinecraftServer server) {
        State state = requireState(server);
        return observeDay(server, state);
    }

    private static int showActions(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Trainer PTU actions must be requested by an authenticated player."));
            return 0;
        }

        String playerId = player.getUuid().toString();
        var canonicalPlayer = FabricCanonicalPlayerStoreRuntime.requireRepository(source.getServer())
                .findPlayer(playerId);
        if (canonicalPlayer.isEmpty()) {
            source.sendError(Text.literal("Canonical Trainer state is not loaded."));
            return 0;
        }

        long dayId = currentRpgDay(source.getServer());
        player.sendMessage(Text.literal("AutoPTU RPG day: " + dayId), false);
        player.sendMessage(Text.literal(
                "Trainer Features owned: " + canonicalPlayer.get().trainerFeatures().size()
                        + ". Daily uses are consumed only by server-registered PTU world actions."), false);
        player.sendMessage(Text.literal(
                "Walking, mining, building and other normal Minecraft actions do not consume PTU actions."), false);
        return 1;
    }

    private static void start(MinecraftServer server) {
        Path root = server.getSavePath(WorldSavePath.ROOT)
                .resolve("autoptu")
                .resolve("trainer-ptu-actions")
                .normalize();
        State state = new State(
                new FileMonotonicRpgDayClock(root.resolve("rpg-day.properties")),
                new FileTrainerPtuDailyActionLedger(root.resolve("usage")));
        observeDay(server, state);
        synchronized (STATES) {
            if (STATES.putIfAbsent(server, state) != null) {
                throw new IllegalStateException("trainer PTU action runtime already initialized for server");
            }
        }
    }

    private static void stop(MinecraftServer server) {
        synchronized (STATES) {
            STATES.remove(server);
        }
    }

    private static State requireState(MinecraftServer server) {
        synchronized (STATES) {
            State state = STATES.get(server);
            if (state == null) {
                throw new IllegalStateException("trainer PTU action runtime unavailable for server lifecycle");
            }
            return state;
        }
    }

    private static long observeDay(MinecraftServer server, State state) {
        return state.clock().observeWorldTime(server.getOverworld().getTimeOfDay());
    }
}
