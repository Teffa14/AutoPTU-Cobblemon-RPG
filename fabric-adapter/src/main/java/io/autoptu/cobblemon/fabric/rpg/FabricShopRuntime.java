package io.autoptu.cobblemon.fabric.rpg;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.authority.CanonicalShopCatalogue;
import io.autoptu.cobblemon.authority.CanonicalShopQueryService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Read-only fallback surface for server-authored offers plus durable server-owned remaining stock. */
public final class FabricShopRuntime {
    private static final String DEFAULT_SHOP_ID = "cedar-mart";

    private FabricShopRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("shop")
                                .executes(context -> show(context.getSource(), DEFAULT_SHOP_ID))
                                .then(CommandManager.literal("list")
                                        .executes(context -> show(context.getSource(), DEFAULT_SHOP_ID))
                                        .then(CommandManager.argument("shop", StringArgumentType.word())
                                                .executes(context -> show(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "shop"))))))));
    }

    private static int show(ServerCommandSource source, String shopId) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("AutoPTU shop must be requested by an authenticated player."));
            return 0;
        }
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        if (FabricCanonicalPlayerStoreRuntime.requireRepository(player.getServer()).findPlayer(playerId).isEmpty()) {
            source.sendError(Text.literal("Canonical Trainer state is not loaded."));
            return 0;
        }

        CanonicalShopQueryService.ShopSnapshot shop;
        try {
            shop = new CanonicalShopQueryService(
                    CanonicalShopCatalogue.DEFAULT,
                    FabricCanonicalPlayerStoreRuntime.requireShopStockRepository(player.getServer())
            ).inspectShop(playerId, shopId);
        } catch (RuntimeException invalidId) {
            source.sendError(Text.literal("Invalid or unavailable shop state."));
            return 0;
        }
        if (shop.offers().isEmpty()) {
            source.sendError(Text.literal("Unknown or empty AutoPTU shop: " + shop.shopId()));
            return 0;
        }

        player.sendMessage(Text.literal("AutoPTU shop: " + shop.shopId()), false);
        for (CanonicalShopQueryService.OfferSnapshot offer : shop.offers()) {
            player.sendMessage(Text.literal(formatOffer(offer)), false);
        }
        player.sendMessage(Text.literal("Server stock is persistent. Purchases remain a separate authoritative transaction."), false);
        return 1;
    }

    static String formatOffer(CanonicalShopQueryService.OfferSnapshot snapshot) {
        var offer = snapshot.offer();
        return offer.offerId() + " -> " + offer.itemTemplateId()
                + " | " + offer.unitPrice() + " " + offer.currencyId()
                + " | stock " + snapshot.remainingStock() + "/" + offer.stockLimit();
    }
}
