package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalBagQueryService;
import io.autoptu.cobblemon.authority.WorldTaskCatalogue;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Read-only Minecraft fallback for the authenticated player's spendable canonical RPG bag. */
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
            source.sendError(Text.literal("Bag inspection must be requested by an authenticated player."));
            return 0;
        }

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        if (FabricCanonicalPlayerStoreRuntime.requireRepository(player.getServer()).findPlayer(playerId).isEmpty()) {
            source.sendError(Text.literal("Canonical Trainer state is unavailable for this player."));
            return 0;
        }

        CanonicalBagQueryService service = new CanonicalBagQueryService(
                FabricCanonicalPlayerStoreRuntime.requireAssetRepository(player.getServer()),
                new WorldTaskCatalogue());
        CanonicalBagQueryService.BagSnapshot bag;
        try {
            bag = service.readAvailable(playerId);
        } catch (IllegalStateException invalidCanonicalState) {
            source.sendError(Text.literal("AutoPTU bag state is inconsistent and cannot be displayed safely."));
            return 0;
        }

        if (bag.empty()) {
            player.sendMessage(Text.literal("AutoPTU bag — no currently available canonical items."), false);
            player.sendMessage(Text.literal("Items reserved by an in-flight server transaction are intentionally not shown as available."), false);
            return 1;
        }

        player.sendMessage(Text.literal("AutoPTU bag — available canonical inventory"), false);
        for (CanonicalBagQueryService.Entry entry : bag.entries()) {
            player.sendMessage(Text.literal(
                    entry.itemTemplateId() + " x" + entry.availableQuantity()
                            + (entry.stackCount() > 1 ? " (" + entry.stackCount() + " stacks)" : "")), false);
        }
        player.sendMessage(Text.literal("Reserved transaction inventory is excluded from available quantities."), false);
        return 1;
    }
}
