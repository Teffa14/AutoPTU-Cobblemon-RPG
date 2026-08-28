package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalBagQueryService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Read-only player bag fallback backed only by durable canonical item state. */
public final class FabricBagRuntime {
    private FabricBagRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("bag")
                                .executes(context -> show(context.getSource())))));
    }

    private static int show(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("AutoPTU bag must be requested by an authenticated player."));
            return 0;
        }

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        if (FabricCanonicalPlayerStoreRuntime.requireRepository(player.getServer()).findPlayer(playerId).isEmpty()) {
            source.sendError(Text.literal("Canonical Trainer state is not loaded."));
            return 0;
        }

        CanonicalBagQueryService service = new CanonicalBagQueryService(
                FabricCanonicalPlayerStoreRuntime.requireAssetRepository(player.getServer()));
        CanonicalBagQueryService.BagSnapshot bag = service.inspect(playerId);
        for (String line : formatLines(bag)) player.sendMessage(Text.literal(line), false);
        return 1;
    }

    static List<String> formatLines(CanonicalBagQueryService.BagSnapshot bag) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("AutoPTU bag");
        if (bag.entries().isEmpty()) {
            lines.add("Canonical inventory is empty.");
            return List.copyOf(lines);
        }

        for (CanonicalBagQueryService.BagEntry entry : bag.entries()) {
            StringBuilder line = new StringBuilder()
                    .append(entry.templateId())
                    .append(" x").append(entry.quantity())
                    .append(" | available ").append(entry.availableQuantity());
            if (entry.reservedQuantity() > 0) {
                line.append(" | reserved ").append(entry.reservedQuantity());
            }
            if (entry.reservationConsumed()) {
                line.append(" | transaction lock after consumption");
            } else if (entry.transactionLocked()) {
                line.append(" | transaction lock");
            }
            line.append(" | stack ").append(entry.itemInstanceId());
            lines.add(line.toString());
        }
        lines.add("Totals: quantity " + bag.totalQuantity()
                + ", available " + bag.totalAvailable()
                + ", reserved " + bag.totalReserved()
                + ", locks " + bag.transactionLocks());
        return List.copyOf(lines);
    }
}
