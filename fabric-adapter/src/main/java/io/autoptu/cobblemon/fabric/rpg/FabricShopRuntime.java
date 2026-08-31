package io.autoptu.cobblemon.fabric.rpg;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.authority.CanonicalShopCatalogue;
import io.autoptu.cobblemon.authority.CanonicalShopPurchaseService;
import io.autoptu.cobblemon.authority.CanonicalShopQueryService;
import io.autoptu.cobblemon.authority.CanonicalShopRestockService;
import io.autoptu.cobblemon.authority.CanonicalShopSaleService;
import io.autoptu.cobblemon.authority.CanonicalShopSellCatalogue;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

/** Authenticated fallback surface for server-authored shop reads and recoverable canonical trades. */
public final class FabricShopRuntime {
    private static final String DEFAULT_SHOP_ID = "cedar-mart";

    private FabricShopRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var shop = CommandManager.literal("shop")
                    .executes(context -> show(context.getSource(), DEFAULT_SHOP_ID))
                    .then(CommandManager.literal("list")
                            .executes(context -> show(context.getSource(), DEFAULT_SHOP_ID))
                            .then(CommandManager.argument("shop", StringArgumentType.word())
                                    .executes(context -> show(
                                            context.getSource(),
                                            StringArgumentType.getString(context, "shop")))))
                    .then(CommandManager.literal("buy")
                            .then(CommandManager.argument("offer", StringArgumentType.word())
                                    .executes(context -> buy(
                                            context.getSource(),
                                            StringArgumentType.getString(context, "offer"),
                                            1))
                                    .then(CommandManager.argument("qty", IntegerArgumentType.integer(1))
                                            .executes(context -> buy(
                                                    context.getSource(),
                                                    StringArgumentType.getString(context, "offer"),
                                                    IntegerArgumentType.getInteger(context, "qty"))))))
                    .then(CommandManager.literal("sell")
                            .then(CommandManager.argument("item", StringArgumentType.word())
                                    .executes(context -> sell(
                                            context.getSource(),
                                            StringArgumentType.getString(context, "item"),
                                            1))
                                    .then(CommandManager.argument("qty", IntegerArgumentType.integer(1))
                                            .executes(context -> sell(
                                                    context.getSource(),
                                                    StringArgumentType.getString(context, "item"),
                                                    IntegerArgumentType.getInteger(context, "qty"))))));
            dispatcher.register(CommandManager.literal("autoptu").then(shop));
        });
    }

    private static int show(ServerCommandSource source, String shopId) {
        ServerPlayerEntity player = requireCanonicalPlayer(source);
        if (player == null) return 0;
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());

        CanonicalShopQueryService.ShopSnapshot shop;
        try {
            reconcileDueRestock(player, shopId);
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
        player.sendMessage(Text.literal("Use /autoptu shop buy <offer> [qty] or /autoptu shop sell <item> [qty]. Server re-resolves all canonical trade truth."), false);
        return 1;
    }

    private static int buy(ServerCommandSource source, String offerId, int quantity) {
        ServerPlayerEntity player = requireCanonicalPlayer(source);
        if (player == null) return 0;
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        try {
            reconcileDueRestock(player, DEFAULT_SHOP_ID);
        } catch (RuntimeException failed) {
            source.sendError(Text.literal("Shop stock could not be reconciled safely."));
            return 0;
        }
        CanonicalShopPurchaseService service = new CanonicalShopPurchaseService(
                CanonicalShopCatalogue.DEFAULT,
                FabricCanonicalPlayerStoreRuntime.requireWalletRepository(player.getServer()),
                FabricCanonicalPlayerStoreRuntime.requireShopStockRepository(player.getServer()),
                FabricCanonicalPlayerStoreRuntime.requireAssetRepository(player.getServer()),
                FabricCanonicalPlayerStoreRuntime.requireShopPurchaseRepository(player.getServer())
        );
        CanonicalShopPurchaseService.PurchaseResult result;
        try {
            result = service.purchase(UUID.randomUUID().toString(), playerId, DEFAULT_SHOP_ID, offerId, quantity);
        } catch (IllegalArgumentException invalidRequest) {
            source.sendError(Text.literal("Unknown shop offer or invalid quantity."));
            return 0;
        } catch (RuntimeException failed) {
            source.sendError(Text.literal("Shop purchase could not be committed safely. No local fallback result was invented."));
            return 0;
        }

        var attempt = result.attempt();
        return switch (result.status()) {
            case COMMITTED -> {
                player.sendMessage(Text.literal(
                        "Purchased " + attempt.quantity() + " " + attempt.itemTemplateId()
                                + " for " + attempt.totalPrice() + " " + attempt.currencyId()
                                + " | wallet " + result.walletBalance()
                                + " | stock " + result.remainingStock()), false);
                yield 1;
            }
            case INSUFFICIENT_FUNDS -> {
                source.sendError(Text.literal("Insufficient " + attempt.currencyId() + ". Balance: " + result.walletBalance()));
                yield 0;
            }
            case OUT_OF_STOCK -> {
                source.sendError(Text.literal("Offer is out of stock. Any provisional debit was refunded exactly once."));
                yield 0;
            }
        };
    }

    private static int sell(ServerCommandSource source, String itemTemplateId, int quantity) {
        ServerPlayerEntity player = requireCanonicalPlayer(source);
        if (player == null) return 0;
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        CanonicalShopSaleService service = new CanonicalShopSaleService(
                CanonicalShopSellCatalogue.DEFAULT,
                FabricCanonicalPlayerStoreRuntime.requireWalletRepository(player.getServer()),
                FabricCanonicalPlayerStoreRuntime.requireAssetRepository(player.getServer()),
                FabricCanonicalPlayerStoreRuntime.requireShopSaleRepository(player.getServer())
        );
        CanonicalShopSaleService.SaleResult result;
        try {
            result = service.sell(UUID.randomUUID().toString(), playerId, DEFAULT_SHOP_ID, itemTemplateId, quantity);
        } catch (IllegalArgumentException invalidRequest) {
            source.sendError(Text.literal("This shop does not buy that canonical item or the quantity is invalid."));
            return 0;
        } catch (RuntimeException failed) {
            source.sendError(Text.literal("Shop sale could not be committed safely. No currency or inventory fallback was invented."));
            return 0;
        }
        if (!result.committed()) {
            source.sendError(Text.literal("Required unreserved canonical item quantity is unavailable."));
            return 0;
        }
        var attempt = result.attempt();
        player.sendMessage(Text.literal(
                "Sold " + attempt.quantity() + " " + attempt.itemTemplateId()
                        + " for " + attempt.totalPrice() + " " + attempt.currencyId()
                        + " | wallet " + result.walletBalance()), false);
        return 1;
    }

    private static void reconcileDueRestock(ServerPlayerEntity player, String shopId) {
        new CanonicalShopRestockService(
                CanonicalShopCatalogue.DEFAULT,
                FabricCanonicalPlayerStoreRuntime.requireShopStockRepository(player.getServer()))
                .reconcileShop(shopId, FabricTrainerPtuActionRuntime.currentRpgDay(player.getServer()));
    }

    private static ServerPlayerEntity requireCanonicalPlayer(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("AutoPTU shop must be requested by an authenticated player."));
            return null;
        }
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        if (FabricCanonicalPlayerStoreRuntime.requireRepository(player.getServer()).findPlayer(playerId).isEmpty()) {
            source.sendError(Text.literal("Canonical Trainer state is not loaded."));
            return null;
        }
        return player;
    }

    static String formatOffer(CanonicalShopQueryService.OfferSnapshot snapshot) {
        var offer = snapshot.offer();
        return offer.offerId() + " -> " + offer.itemTemplateId()
                + " | " + offer.unitPrice() + " " + offer.currencyId()
                + " | stock " + snapshot.remainingStock() + "/" + offer.stockLimit();
    }
}
