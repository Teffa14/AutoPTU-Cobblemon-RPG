package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.ServerAuthoredRpgCalendar;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Minecraft-visible projection of the durable RPG calendar.
 *
 * <p>The runtime observes only the server-owned monotonic RPG day. Calendar event keys are authored
 * Ouros world hooks; this class deliberately grants no rewards and consumes no PTU frequencies.</p>
 */
public final class FabricRpgCalendarRuntime {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static final ServerAuthoredRpgCalendar CALENDAR = new ServerAuthoredRpgCalendar();
    private static final Map<MinecraftServer, Long> LAST_ANNOUNCED_DAY = new IdentityHashMap<>();
    private static int tickCounter;

    private FabricRpgCalendarRuntime() {}

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) return;

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("calendar")
                                .executes(context -> showCalendar(context.getSource())))));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter % 20 != 0) return;
            announceTransitionIfNeeded(server);
        });
    }

    public static ServerAuthoredRpgCalendar.Snapshot currentSnapshot(MinecraftServer server) {
        long dayId = FabricTrainerPtuActionRuntime.currentRpgDay(server);
        return CALENDAR.snapshot(dayId);
    }

    private static void announceTransitionIfNeeded(MinecraftServer server) {
        ServerAuthoredRpgCalendar.Snapshot snapshot = currentSnapshot(server);
        Long prior;
        synchronized (LAST_ANNOUNCED_DAY) {
            prior = LAST_ANNOUNCED_DAY.put(server, snapshot.rpgDayId());
        }
        if (prior == null || prior == snapshot.rpgDayId()) return;

        Text headline = Text.literal("Ouros calendar: " + snapshot.displayLabel());
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.sendMessage(headline, false);
            for (ServerAuthoredRpgCalendar.Event event : snapshot.activeEvents()) {
                player.sendMessage(Text.literal("Event · " + event.title() + " — " + event.description()), false);
            }
        }
    }

    private static int showCalendar(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("The RPG calendar must be requested by a player."));
            return 0;
        }

        ServerAuthoredRpgCalendar.Snapshot snapshot = currentSnapshot(source.getServer());
        player.sendMessage(Text.literal("Ouros calendar: " + snapshot.displayLabel()), false);
        if (snapshot.activeEvents().isEmpty()) {
            player.sendMessage(Text.literal("No authored world events are active today."), false);
        } else {
            for (ServerAuthoredRpgCalendar.Event event : snapshot.activeEvents()) {
                player.sendMessage(Text.literal("Event · " + event.title() + " — " + event.description()), false);
            }
        }
        return 1;
    }
}
