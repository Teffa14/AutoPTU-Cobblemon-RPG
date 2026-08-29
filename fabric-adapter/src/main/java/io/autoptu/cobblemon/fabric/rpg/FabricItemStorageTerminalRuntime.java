package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalBagQueryService;
import io.autoptu.cobblemon.authority.CanonicalItemStorageState;
import io.autoptu.cobblemon.authority.CanonicalItemStorageTransferService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Physical server-authoritative bag/storage terminal backed only by AutoPTU canonical state. */
public final class FabricItemStorageTerminalRuntime {
    private static final double MAX_INTERACTION_DISTANCE_SQUARED = 25.0D;
    private static final int TOP_SLOT_COUNT = 54;
    private static final int INFO_SLOT = 4;
    private static final int BAG_START_SLOT = 9;
    private static final int BAG_END_SLOT = 26;
    private static final int STORAGE_START_SLOT = 27;

    private FabricItemStorageTerminalRuntime() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient() || hand != Hand.MAIN_HAND || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }
            BlockPos terminalPos = hitResult.getBlockPos();
            if (!isItemStorageTerminal(world, terminalPos)) return ActionResult.PASS;
            if (!withinInteractionDistance(serverPlayer, terminalPos)) {
                serverPlayer.sendMessage(Text.literal("You are too far away to use this AutoPTU item storage terminal."), false);
                return ActionResult.FAIL;
            }
            String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(serverPlayer.getUuid());
            if (FabricCanonicalPlayerStoreRuntime.requireRepository(serverPlayer.getServer()).findPlayer(playerId).isEmpty()) {
                serverPlayer.sendMessage(Text.literal("Canonical Trainer state is not loaded."), false);
                return ActionResult.FAIL;
            }
            serverPlayer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inventory, ignored) -> new CanonicalItemStorageScreenHandler(
                            syncId, inventory, serverPlayer, terminalPos),
                    Text.literal("AutoPTU Item Storage")
            ));
            return ActionResult.SUCCESS;
        });
    }

    static boolean isItemStorageTerminal(World world, BlockPos pos) {
        return world.getBlockState(pos).isOf(Blocks.IRON_BLOCK)
                && world.getBlockState(pos.down()).isOf(Blocks.BARREL);
    }

    static boolean withinInteractionDistance(ServerPlayerEntity player, BlockPos pos) {
        return player.squaredDistanceTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D)
                <= MAX_INTERACTION_DISTANCE_SQUARED;
    }

    static final class CanonicalItemStorageScreenHandler extends GenericContainerScreenHandler {
        private final SimpleInventory displayInventory;
        private final ServerPlayerEntity player;
        private final BlockPos terminalPos;
        private final String playerId;
        private final CanonicalBagQueryService bagQuery;
        private final CanonicalItemStorageTransferService transferService;
        private final Map<Integer, BagSelection> bagSelections = new LinkedHashMap<>();
        private final Map<Integer, StorageSelection> storageSelections = new LinkedHashMap<>();

        CanonicalItemStorageScreenHandler(
                int syncId,
                PlayerInventory inventory,
                ServerPlayerEntity player,
                BlockPos terminalPos
        ) {
            this(syncId, inventory, player, terminalPos, new SimpleInventory(TOP_SLOT_COUNT));
        }

        private CanonicalItemStorageScreenHandler(
                int syncId,
                PlayerInventory inventory,
                ServerPlayerEntity player,
                BlockPos terminalPos,
                SimpleInventory displayInventory
        ) {
            super(ScreenHandlerType.GENERIC_9X6, syncId, inventory, displayInventory, 6);
            this.displayInventory = displayInventory;
            this.player = player;
            this.terminalPos = terminalPos.toImmutable();
            this.playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
            this.bagQuery = new CanonicalBagQueryService(
                    FabricCanonicalPlayerStoreRuntime.requireAssetRepository(player.getServer()));
            this.transferService = new CanonicalItemStorageTransferService(
                    FabricCanonicalPlayerStoreRuntime.requireAssetRepository(player.getServer()),
                    FabricCanonicalPlayerStoreRuntime.requireItemStorageRepository(player.getServer()),
                    FabricCanonicalPlayerStoreRuntime.requireItemStorageTransferRepository(player.getServer()));
            refresh();
        }

        @Override
        public boolean canUse(PlayerEntity player) {
            return player == this.player
                    && !player.isRemoved()
                    && isItemStorageTerminal(player.getWorld(), terminalPos)
                    && player.squaredDistanceTo(
                            terminalPos.getX() + 0.5D,
                            terminalPos.getY() + 0.5D,
                            terminalPos.getZ() + 0.5D) <= MAX_INTERACTION_DISTANCE_SQUARED;
        }

        @Override
        public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity clickingPlayer) {
            if (clickingPlayer != player || actionType != SlotActionType.PICKUP || button != 0) return;
            if (!canUse(clickingPlayer)) {
                player.closeHandledScreen();
                return;
            }
            if (slotIndex < 0 || slotIndex >= TOP_SLOT_COUNT) return;

            BagSelection bag = bagSelections.get(slotIndex);
            if (bag != null) {
                depositOne(bag);
                return;
            }
            StorageSelection stored = storageSelections.get(slotIndex);
            if (stored != null) withdrawOne(stored);
        }

        @Override
        public ItemStack quickMove(PlayerEntity player, int slot) {
            return ItemStack.EMPTY;
        }

        private void depositOne(BagSelection selection) {
            CanonicalBagQueryService.BagEntry current = bagQuery.inspect(playerId).entries().stream()
                    .filter(entry -> entry.itemInstanceId().equals(selection.itemInstanceId()))
                    .findFirst().orElse(null);
            if (current == null
                    || current.revision() != selection.revision()
                    || current.availableQuantity() != selection.availableQuantity()
                    || current.availableQuantity() < 1) {
                player.sendMessage(Text.literal("Bag state changed. The storage menu was refreshed; click again."), false);
                refresh();
                return;
            }
            try {
                transferService.deposit(
                        "item-storage-ui:" + playerId + ":" + UUID.randomUUID(),
                        playerId,
                        selection.itemInstanceId(),
                        1);
                player.sendMessage(Text.literal("Stored 1 " + selection.templateId() + "."), false);
            } catch (IllegalArgumentException | IllegalStateException failed) {
                player.sendMessage(Text.literal("Item could not be stored safely. Canonical transfer state was preserved."), false);
            }
            refresh();
        }

        private void withdrawOne(StorageSelection selection) {
            CanonicalItemStorageState current = transferService.inspect(playerId);
            if (current.revision() != selection.storageRevision()
                    || current.quantity(selection.templateId()) != selection.quantity()
                    || selection.quantity() < 1) {
                player.sendMessage(Text.literal("Stored item state changed. The storage menu was refreshed; click again."), false);
                refresh();
                return;
            }
            try {
                transferService.withdraw(
                        "item-storage-ui:" + playerId + ":" + UUID.randomUUID(),
                        playerId,
                        selection.templateId(),
                        1);
                player.sendMessage(Text.literal("Withdrew 1 " + selection.templateId() + "."), false);
            } catch (IllegalArgumentException | IllegalStateException failed) {
                player.sendMessage(Text.literal("Item could not be withdrawn safely. Canonical transfer state was preserved."), false);
            }
            refresh();
        }

        private void refresh() {
            bagSelections.clear();
            storageSelections.clear();
            for (int i = 0; i < TOP_SLOT_COUNT; i++) displayInventory.setStack(i, ItemStack.EMPTY);

            CanonicalBagQueryService.BagSnapshot bag = bagQuery.inspect(playerId);
            CanonicalItemStorageState storage = transferService.inspect(playerId);
            displayInventory.setStack(INFO_SLOT, menuItem(
                    Items.BOOK.getDefaultStack(),
                    "Canonical item storage · lime=bag deposit · cyan=storage withdraw · storage rev " + storage.revision()));

            int bagSlot = BAG_START_SLOT;
            for (CanonicalBagQueryService.BagEntry entry : bag.entries()) {
                if (bagSlot > BAG_END_SLOT) break;
                if (entry.availableQuantity() <= 0 || entry.transactionLocked()) continue;
                displayInventory.setStack(bagSlot, menuItem(
                        Items.LIME_DYE.getDefaultStack(),
                        "BAG " + entry.templateId() + " x" + entry.availableQuantity() + " · click to store 1"));
                bagSelections.put(bagSlot, new BagSelection(
                        entry.itemInstanceId(), entry.templateId(), entry.availableQuantity(), entry.revision()));
                bagSlot++;
            }

            int storageSlot = STORAGE_START_SLOT;
            for (Map.Entry<String, Integer> entry : storage.quantities().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey()).toList()) {
                if (storageSlot >= TOP_SLOT_COUNT) break;
                if (entry.getValue() <= 0) continue;
                displayInventory.setStack(storageSlot, menuItem(
                        Items.CYAN_DYE.getDefaultStack(),
                        "STORAGE " + entry.getKey() + " x" + entry.getValue() + " · click to withdraw 1"));
                storageSelections.put(storageSlot, new StorageSelection(
                        entry.getKey(), entry.getValue(), storage.revision()));
                storageSlot++;
            }

            displayInventory.markDirty();
            sendContentUpdates();
        }

        private static ItemStack menuItem(ItemStack stack, String name) {
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
            return stack;
        }
    }

    private record BagSelection(String itemInstanceId, String templateId, int availableQuantity, long revision) {}
    private record StorageSelection(String templateId, int quantity, long storageRevision) {}
}
