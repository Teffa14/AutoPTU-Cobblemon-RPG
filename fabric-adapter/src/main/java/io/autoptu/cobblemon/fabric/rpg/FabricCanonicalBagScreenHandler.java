package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalBagQueryService;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Read-only vanilla-backed screen for the server-authoritative canonical bag.
 * The visible paper stacks are presentation only. Slot clicks carry only a slot index;
 * the server maps that index back to the canonical item instance captured when the screen opened
 * and revalidates the stack against current canonical state before reporting item-use readiness.
 */
final class FabricCanonicalBagScreenHandler extends GenericContainerScreenHandler {
    static final int SLOT_COUNT = 54;

    private final List<String> canonicalItemInstanceIds;

    FabricCanonicalBagScreenHandler(
            int syncId,
            PlayerInventory playerInventory,
            List<CanonicalBagQueryService.BagEntry> entries
    ) {
        this(syncId, playerInventory, createDisplayInventory(entries), itemInstanceIds(entries));
    }

    private FabricCanonicalBagScreenHandler(
            int syncId,
            PlayerInventory playerInventory,
            SimpleInventory displayInventory,
            List<String> canonicalItemInstanceIds
    ) {
        super(ScreenHandlerType.GENERIC_9X6, syncId, playerInventory, displayInventory, 6);
        this.canonicalItemInstanceIds = canonicalItemInstanceIds;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < canonicalItemInstanceIds.size()
                && player instanceof ServerPlayerEntity serverPlayer) {
            FabricBagRuntime.inspectItemUse(serverPlayer, canonicalItemInstanceIds.get(slotIndex));
        }
        // Deliberately do not call super: canonical bag presentation and the player's Minecraft
        // inventory are read-only while this screen is open, so placeholder stacks cannot escape.
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    private static SimpleInventory createDisplayInventory(List<CanonicalBagQueryService.BagEntry> entries) {
        SimpleInventory inventory = new SimpleInventory(SLOT_COUNT);
        int limit = Math.min(entries.size(), SLOT_COUNT);
        for (int index = 0; index < limit; index++) {
            CanonicalBagQueryService.BagEntry entry = entries.get(index);
            ItemStack display = new ItemStack(Items.PAPER);
            display.set(DataComponentTypes.CUSTOM_NAME, Text.literal(displayName(entry)));
            inventory.setStack(index, display);
        }
        return inventory;
    }

    private static List<String> itemInstanceIds(List<CanonicalBagQueryService.BagEntry> entries) {
        return entries.stream()
                .limit(SLOT_COUNT)
                .map(CanonicalBagQueryService.BagEntry::itemInstanceId)
                .toList();
    }

    static String displayName(CanonicalBagQueryService.BagEntry entry) {
        StringBuilder name = new StringBuilder(entry.templateId())
                .append(" x").append(entry.quantity())
                .append(" | available ").append(entry.availableQuantity());
        if (entry.reservedQuantity() > 0) {
            name.append(" | reserved ").append(entry.reservedQuantity());
        }
        if (entry.transactionLocked()) {
            name.append(" | locked");
        }
        return name.toString();
    }
}
