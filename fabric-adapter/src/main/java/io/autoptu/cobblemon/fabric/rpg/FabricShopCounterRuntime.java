package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalBagQueryService;
import io.autoptu.cobblemon.authority.CanonicalShopCatalogue;
import io.autoptu.cobblemon.authority.CanonicalShopPurchaseService;
import io.autoptu.cobblemon.authority.CanonicalShopQueryService;
import io.autoptu.cobblemon.authority.CanonicalShopRestockService;
import io.autoptu.cobblemon.authority.CanonicalShopSaleService;
import io.autoptu.cobblemon.authority.CanonicalShopSellCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWalletQueryService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Physical server-authoritative Cedar Mart surface backed by canonical shop transactions. */
public final class FabricShopCounterRuntime {
    static final String SHOP_ID = "cedar-mart";
    private static final double MAX_INTERACTION_DISTANCE_SQUARED = 25.0D;

    private FabricShopCounterRuntime() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient() || hand != Hand.MAIN_HAND || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }
            BlockPos counterPos = hitResult.getBlockPos();
            if (!isShopCounter(world, counterPos)) return ActionResult.PASS;
            if (!withinInteractionDistance(serverPlayer, counterPos)) {
                serverPlayer.sendMessage(Text.literal("You are too far away to use this AutoPTU shop counter."), false);
                return ActionResult.FAIL;
            }
            String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(serverPlayer.getUuid());
            if (FabricCanonicalPlayerStoreRuntime.requireRepository(serverPlayer.getServer()).findPlayer(playerId).isEmpty()) {
                serverPlayer.sendMessage(Text.literal("Canonical Trainer state is not loaded."), false);
                return ActionResult.FAIL;
            }
            serverPlayer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inventory, ignored) -> new CanonicalShopScreenHandler(syncId, inventory, serverPlayer, counterPos),
                    Text.literal("Cedar Mart")
            ));
            return ActionResult.SUCCESS;
        });
    }

    static boolean isShopCounter(World world, BlockPos pos) {
        return world.getBlockState(pos).isOf(FabricRpgContent.CEDAR_MART_COUNTER);
    }

    static boolean withinInteractionDistance(ServerPlayerEntity player, BlockPos pos) {
        return player.squaredDistanceTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D)
                <= MAX_INTERACTION_DISTANCE_SQUARED;
    }

    static final class CanonicalShopScreenHandler extends GenericContainerScreenHandler {
        private static final int TOP_SLOT_COUNT = 54;
        private static final int INFO_SLOT = 4;
        private static final int BUY_START_SLOT = 9;
        private static final int BUY_END_SLOT = 26;
        private static final int SELL_START_SLOT = 27;
        private final SimpleInventory displayInventory;
        private final ServerPlayerEntity player;
        private final BlockPos counterPos;
        private final String playerId;
        private final CanonicalShopQueryService shopQuery;
        private final CanonicalShopRestockService restockService;
        private final CanonicalBagQueryService bagQuery;
        private final CanonicalWalletQueryService walletQuery;
        private final CanonicalShopPurchaseService purchaseService;
        private final CanonicalShopSaleService saleService;
        private final Map<Integer, BuySelection> buySelections = new HashMap<>();
        private final Map<Integer, SellSelection> sellSelections = new HashMap<>();

        CanonicalShopScreenHandler(int syncId, PlayerInventory inventory, ServerPlayerEntity player, BlockPos counterPos) {
            this(syncId, inventory, player, counterPos, new SimpleInventory(TOP_SLOT_COUNT));
        }

        private CanonicalShopScreenHandler(
                int syncId,
                PlayerInventory inventory,
                ServerPlayerEntity player,
                BlockPos counterPos,
                SimpleInventory displayInventory
        ) {
            super(ScreenHandlerType.GENERIC_9X6, syncId, inventory, displayInventory, 6);
            this.displayInventory = displayInventory;
            this.player = player;
            this.counterPos = counterPos.toImmutable();
            this.playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
            var stockRepository = FabricCanonicalPlayerStoreRuntime.requireShopStockRepository(player.getServer());
            this.shopQuery = new CanonicalShopQueryService(CanonicalShopCatalogue.DEFAULT, stockRepository);
            this.restockService = new CanonicalShopRestockService(CanonicalShopCatalogue.DEFAULT, stockRepository);
            this.bagQuery = new CanonicalBagQueryService(
                    FabricCanonicalPlayerStoreRuntime.requireAssetRepository(player.getServer()));
            this.walletQuery = new CanonicalWalletQueryService(
                    FabricCanonicalPlayerStoreRuntime.requireWalletRepository(player.getServer()));
            this.purchaseService = new CanonicalShopPurchaseService(
                    CanonicalShopCatalogue.DEFAULT,
                    FabricCanonicalPlayerStoreRuntime.requireWalletRepository(player.getServer()),
                    stockRepository,
                    FabricCanonicalPlayerStoreRuntime.requireAssetRepository(player.getServer()),
                    FabricCanonicalPlayerStoreRuntime.requireShopPurchaseRepository(player.getServer()));
            this.saleService = new CanonicalShopSaleService(
                    CanonicalShopSellCatalogue.DEFAULT,
                    FabricCanonicalPlayerStoreRuntime.requireWalletRepository(player.getServer()),
                    FabricCanonicalPlayerStoreRuntime.requireAssetRepository(player.getServer()),
                    FabricCanonicalPlayerStoreRuntime.requireShopSaleRepository(player.getServer()));
            refresh();
        }

        @Override
        public boolean canUse(PlayerEntity player) {
            return player == this.player
                    && !player.isRemoved()
                    && isShopCounter(player.getWorld(), counterPos)
                    && player.squaredDistanceTo(counterPos.getX() + 0.5D, counterPos.getY() + 0.5D, counterPos.getZ() + 0.5D)
                    <= MAX_INTERACTION_DISTANCE_SQUARED;
        }

        @Override
        public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity clickingPlayer) {
            if (clickingPlayer != player || actionType != SlotActionType.PICKUP || button != 0) return;
            if (!canUse(clickingPlayer)) {
                player.closeHandledScreen();
                return;
            }
            if (slotIndex < 0 || slotIndex >= TOP_SLOT_COUNT) return;
            BuySelection buy = buySelections.get(slotIndex);
            if (buy != null) {
                buyOne(buy);
                return;
            }
            SellSelection sell = sellSelections.get(slotIndex);
            if (sell != null) sellOne(sell);
        }

        @Override
        public ItemStack quickMove(PlayerEntity player, int slot) {
            return ItemStack.EMPTY;
        }

        private void buyOne(BuySelection selection) {
            reconcileDueRestock();
            CanonicalShopQueryService.OfferSnapshot current = shopQuery.inspectShop(playerId, SHOP_ID).offers().stream()
                    .filter(candidate -> candidate.offer().offerId().equals(selection.offerId()))
                    .findFirst().orElse(null);
            if (current == null || current.stockRevision() != selection.stockRevision()
                    || current.remainingStock() != selection.remainingStock()) {
                player.sendMessage(Text.literal("Shop stock changed. The menu was refreshed; click again."), false);
                refresh();
                return;
            }
            try {
                CanonicalShopPurchaseService.PurchaseResult result = purchaseService.purchase(
                        "shop-ui-buy:" + playerId + ":" + UUID.randomUUID(), playerId, SHOP_ID, selection.offerId(), 1);
                switch (result.status()) {
                    case COMMITTED -> player.sendMessage(Text.literal("Purchased 1 " + result.attempt().itemTemplateId() + "."), false);
                    case INSUFFICIENT_FUNDS -> player.sendMessage(Text.literal("Insufficient canonical funds."), false);
                    case OUT_OF_STOCK -> player.sendMessage(Text.literal("That offer is out of stock."), false);
                }
            } catch (RuntimeException failed) {
                player.sendMessage(Text.literal("Purchase could not be committed safely. Canonical transaction state was preserved."), false);
            }
            refresh();
        }

        private void sellOne(SellSelection selection) {
            CanonicalBagQueryService.BagSnapshot bag = bagQuery.inspect(playerId);
            long currentAvailable = bag.entries().stream()
                    .filter(entry -> entry.templateId().equals(selection.itemTemplateId()))
                    .mapToLong(CanonicalBagQueryService.BagEntry::availableQuantity)
                    .sum();
            if (currentAvailable != selection.availableQuantity()) {
                player.sendMessage(Text.literal("Bag availability changed. The menu was refreshed; click again."), false);
                refresh();
                return;
            }
            try {
                CanonicalShopSaleService.SaleResult result = saleService.sell(
                        "shop-ui-sell:" + playerId + ":" + UUID.randomUUID(), playerId, SHOP_ID, selection.itemTemplateId(), 1);
                if (result.committed()) {
                    player.sendMessage(Text.literal("Sold 1 " + result.attempt().itemTemplateId() + "."), false);
                } else {
                    player.sendMessage(Text.literal("That canonical item is no longer available to sell."), false);
                }
            } catch (RuntimeException failed) {
                player.sendMessage(Text.literal("Sale could not be committed safely. Canonical transaction state was preserved."), false);
            }
            refresh();
        }

        private void refresh() {
            reconcileDueRestock();
            buySelections.clear();
            sellSelections.clear();
            for (int i = 0; i < TOP_SLOT_COUNT; i++) displayInventory.setStack(i, ItemStack.EMPTY);

            CanonicalWalletQueryService.WalletSnapshot wallet = walletQuery.inspect(playerId);
            displayInventory.setStack(INFO_SLOT, menuItem(Items.BOOK.getDefaultStack(),
                    "Cedar Mart · " + wallet.balance() + " " + wallet.currencyId() + " · green=buy · gold=sell"));

            int buySlot = BUY_START_SLOT;
            for (CanonicalShopQueryService.OfferSnapshot snapshot : shopQuery.inspectShop(playerId, SHOP_ID).offers()) {
                if (buySlot > BUY_END_SLOT) break;
                var offer = snapshot.offer();
                displayInventory.setStack(buySlot, menuItem(Items.EMERALD.getDefaultStack(),
                        "BUY " + offer.itemTemplateId() + " · " + offer.unitPrice() + " " + offer.currencyId()
                                + " · stock " + snapshot.remainingStock() + "/" + offer.stockLimit()));
                buySelections.put(buySlot, new BuySelection(offer.offerId(), snapshot.remainingStock(), snapshot.stockRevision()));
                buySlot++;
            }

            Map<String, Long> availableByTemplate = new java.util.LinkedHashMap<>();
            for (CanonicalBagQueryService.BagEntry entry : bagQuery.inspect(playerId).entries()) {
                if (entry.availableQuantity() <= 0) continue;
                if (CanonicalShopSellCatalogue.DEFAULT.offer(SHOP_ID, entry.templateId()).isEmpty()) continue;
                availableByTemplate.merge(entry.templateId(), (long) entry.availableQuantity(), Long::sum);
            }
            int sellSlot = SELL_START_SLOT;
            for (var entry : availableByTemplate.entrySet()) {
                if (sellSlot >= TOP_SLOT_COUNT) break;
                var sellOffer = CanonicalShopSellCatalogue.DEFAULT.offer(SHOP_ID, entry.getKey()).orElseThrow();
                displayInventory.setStack(sellSlot, menuItem(Items.GOLD_INGOT.getDefaultStack(),
                        "SELL " + entry.getKey() + " · " + sellOffer.unitPrice() + " " + sellOffer.currencyId()
                                + " · available " + entry.getValue()));
                sellSelections.put(sellSlot, new SellSelection(entry.getKey(), entry.getValue()));
                sellSlot++;
            }
            displayInventory.markDirty();
            sendContentUpdates();
        }

        private void reconcileDueRestock() {
            restockService.reconcileShop(SHOP_ID, FabricTrainerPtuActionRuntime.currentRpgDay(player.getServer()));
        }

        private static ItemStack menuItem(ItemStack stack, String name) {
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
            return stack;
        }
    }

    private record BuySelection(String offerId, int remainingStock, long stockRevision) {}
    private record SellSelection(String itemTemplateId, long availableQuantity) {}
}
