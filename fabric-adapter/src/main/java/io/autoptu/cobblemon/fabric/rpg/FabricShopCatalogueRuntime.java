package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalShopCatalogue;
import io.autoptu.cobblemon.authority.CanonicalShopOffer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Minecraft-facing read-only shop catalogue backed exclusively by server-authored RPG data. */
public final class FabricShopCatalogueRuntime {
    private static final CanonicalShopCatalogue CATALOGUE = new CanonicalShopCatalogue();

    private FabricShopCatalogueRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("shop")
                                .executes(context -> show(context.getSource())))));
    }

    private static int show(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Shop catalogue must be requested by an authenticated player."));
            return 0;
        }

        player.sendMessage(Text.literal("AutoPTU shop catalogue"), false);
        for (CanonicalShopOffer offer : CATALOGUE.offers()) {
            player.sendMessage(Text.literal(
                    offer.displayName()
                            + " [" + offer.offerId() + "]"
                            + " | price " + offer.unitPrice() + " " + offer.currencyId()
                            + " | stock " + offer.availableStock()), false);
        }
        player.sendMessage(Text.literal("Purchasing is disabled until canonical wallet transactions ship."), false);
        return CATALOGUE.offers().size();
    }
}
