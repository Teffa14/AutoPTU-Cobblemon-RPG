package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalBagQueryService;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
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
 * Visible stacks and navigation controls are presentation only. Item clicks carry only a slot index;
 * the server maps that index back to the canonical item instance captured for this page and revalidates
 * current canonical state before reporting item-use readiness. Page changes re-read canonical inventory.
 */
final class FabricCanonicalBagScreenHandler extends GenericContainerScreenHandler {
    static final int SLOT_COUNT = 54;
    static final int ITEMS_PER_PAGE = 45;
    static final int PREVIOUS_PAGE_SLOT = 45;
    static final int PAGE_STATUS_SLOT = 49;
    static final int NEXT_PAGE_SLOT = 53;

    private final List<String> canonicalItemInstanceIds;
    private final int page;
    private final int pageCount;

    FabricCanonicalBagScreenHandler(
            int syncId,
            PlayerInventory playerInventory,
            List<CanonicalBagQueryService.BagEntry> entries,
            int page
    ) {
        this(syncId, playerInventory, createPage(entries, page));
    }

    private FabricCanonicalBagScreenHandler(
            int syncId,
            PlayerInventory playerInventory,
            PageProjection projection
    ) {
        super(ScreenHandlerType.GENERIC_9X6, syncId, playerInventory, projection.inventory(), 6);
        this.canonicalItemInstanceIds = projection.itemInstanceIds();
        this.page = projection.page();
        this.pageCount = projection.pageCount();
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        if (slotIndex >= 0 && slotIndex < canonicalItemInstanceIds.size()) {
            FabricBagRuntime.inspectItemUse(serverPlayer, canonicalItemInstanceIds.get(slotIndex));
            return;
        }
        if (slotIndex == PREVIOUS_PAGE_SLOT && page > 0) {
            FabricBagRuntime.openPlayerBagScreen(serverPlayer, page - 1);
            return;
        }
        if (slotIndex == NEXT_PAGE_SLOT && page + 1 < pageCount) {
            FabricBagRuntime.openPlayerBagScreen(serverPlayer, page + 1);
        }
        // Deliberately do not call super: canonical bag presentation, controls and the player's
        // Minecraft inventory are read-only while this screen is open, so placeholders cannot escape.
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    static int pageCount(int entryCount) {
        return Math.max(1, (entryCount + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
    }

    static int clampPage(int requestedPage, int entryCount) {
        return Math.max(0, Math.min(requestedPage, pageCount(entryCount) - 1));
    }

    private static PageProjection createPage(List<CanonicalBagQueryService.BagEntry> entries, int requestedPage) {
        int pageCount = pageCount(entries.size());
        int page = clampPage(requestedPage, entries.size());
        int fromIndex = page * ITEMS_PER_PAGE;
        int toIndex = Math.min(entries.size(), fromIndex + ITEMS_PER_PAGE);
        List<CanonicalBagQueryService.BagEntry> pageEntries = entries.subList(fromIndex, toIndex);

        SimpleInventory inventory = new SimpleInventory(SLOT_COUNT);
        for (int index = 0; index < pageEntries.size(); index++) {
            CanonicalBagQueryService.BagEntry entry = pageEntries.get(index);
            BagItemPresentation presentation = presentationFor(entry.templateId());
            ItemStack display = new ItemStack(presentation.icon());
            display.set(DataComponentTypes.CUSTOM_NAME, Text.literal(displayName(entry, presentation)));
            inventory.setStack(index, display);
        }

        if (page > 0) {
            ItemStack previous = new ItemStack(Items.ARROW);
            previous.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Previous page"));
            inventory.setStack(PREVIOUS_PAGE_SLOT, previous);
        }

        ItemStack status = new ItemStack(Items.MAP);
        status.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Page " + (page + 1) + " / " + pageCount));
        inventory.setStack(PAGE_STATUS_SLOT, status);

        if (page + 1 < pageCount) {
            ItemStack next = new ItemStack(Items.ARROW);
            next.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Next page"));
            inventory.setStack(NEXT_PAGE_SLOT, next);
        }

        return new PageProjection(
                inventory,
                pageEntries.stream().map(CanonicalBagQueryService.BagEntry::itemInstanceId).toList(),
                page,
                pageCount);
    }

    static String displayName(CanonicalBagQueryService.BagEntry entry) {
        return displayName(entry, presentationFor(entry.templateId()));
    }

    private static String displayName(
            CanonicalBagQueryService.BagEntry entry,
            BagItemPresentation presentation
    ) {
        StringBuilder name = new StringBuilder(presentation.displayName())
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

    /**
     * Server-authored visual catalogue only. These vanilla icons and labels communicate identity in the
     * Minecraft bag UI; they never grant item effects, target legality, healing, capture or PTU outcomes.
     * Unknown canonical templates fail closed to a neutral paper icon while retaining their exact id.
     */
    private static BagItemPresentation presentationFor(String templateId) {
        return switch (templateId) {
            case "field_ration" -> new BagItemPresentation(Items.BREAD, "Field Ration");
            case "basic_bandage" -> new BagItemPresentation(Items.WHITE_WOOL, "Basic Bandage");
            case "revive_kit" -> new BagItemPresentation(Items.TOTEM_OF_UNDYING, "Revive Kit");
            default -> new BagItemPresentation(Items.PAPER, templateId);
        };
    }

    private record BagItemPresentation(Item icon, String displayName) {}

    private record PageProjection(
            SimpleInventory inventory,
            List<String> itemInstanceIds,
            int page,
            int pageCount
    ) {}
}
