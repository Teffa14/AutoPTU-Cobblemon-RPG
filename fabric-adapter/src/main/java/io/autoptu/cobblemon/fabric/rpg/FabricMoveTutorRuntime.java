package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalMoveTutorInspectionService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Physical move tutor/relearner shell.
 *
 * Minecraft owns only the villager presentation/interaction surface. AutoPTU resolves the
 * authenticated Trainer, party membership and persisted move loadout. Teaching, forgetting and
 * relearning stay unavailable until upstream supplies authoritative PTU learnability/mutation.
 */
public final class FabricMoveTutorRuntime {
    public static final String TUTOR_NPC_ID = "cedar-move-tutor";
    static final String NPC_TAG = "autoptu:npc:" + TUTOR_NPC_ID;
    private static final double MAX_INTERACTION_DISTANCE_SQUARED = 25.0D;

    private FabricMoveTutorRuntime() {}

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() || hand != Hand.MAIN_HAND || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }
            if (!isTutor(entity)) return ActionResult.PASS;
            if (serverPlayer.squaredDistanceTo(entity) > MAX_INTERACTION_DISTANCE_SQUARED) {
                serverPlayer.sendMessage(Text.literal("You are too far away to use this Ouros move tutor."), false);
                return ActionResult.FAIL;
            }
            String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(serverPlayer.getUuid());
            if (FabricCanonicalPlayerStoreRuntime.requireRepository(serverPlayer.getServer()).findPlayer(playerId).isEmpty()) {
                serverPlayer.sendMessage(Text.literal("Canonical Trainer state is not loaded."), false);
                return ActionResult.FAIL;
            }
            serverPlayer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inventory, ignored) -> new TutorScreenHandler(syncId, inventory, serverPlayer, entity.getUuid()),
                    Text.literal("Ouros Move Tutor")
            ));
            return ActionResult.SUCCESS;
        });
    }

    public static void bind(VillagerEntity entity) {
        entity.addCommandTag(NPC_TAG);
        entity.setCustomName(Text.literal("Move Tutor"));
        entity.setCustomNameVisible(true);
        entity.setPersistent();
    }

    static boolean isTutor(Entity entity) {
        return entity instanceof VillagerEntity && entity.getCommandTags().contains(NPC_TAG);
    }

    private static final class TutorScreenHandler extends GenericContainerScreenHandler {
        private static final int TOP_SLOT_COUNT = 27;
        private final SimpleInventory displayInventory;
        private final ServerPlayerEntity player;
        private final UUID npcEntityId;
        private final String playerId;
        private final CanonicalMoveTutorInspectionService inspectionService;
        private final Map<Integer, Selection> selections = new HashMap<>();

        TutorScreenHandler(int syncId, PlayerInventory inventory, ServerPlayerEntity player, UUID npcEntityId) {
            this(syncId, inventory, player, npcEntityId, new SimpleInventory(TOP_SLOT_COUNT));
        }

        private TutorScreenHandler(
                int syncId,
                PlayerInventory inventory,
                ServerPlayerEntity player,
                UUID npcEntityId,
                SimpleInventory displayInventory
        ) {
            super(ScreenHandlerType.GENERIC_9X3, syncId, inventory, displayInventory, 3);
            this.displayInventory = displayInventory;
            this.player = player;
            this.npcEntityId = npcEntityId;
            this.playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
            this.inspectionService = new CanonicalMoveTutorInspectionService(
                    FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(player.getServer()),
                    FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(player.getServer())
            );
            refresh();
        }

        @Override
        public boolean canUse(PlayerEntity clickingPlayer) {
            if (clickingPlayer != player || player.isRemoved() || !(player.getWorld() instanceof ServerWorld serverWorld)) {
                return false;
            }
            Entity current = serverWorld.getEntity(npcEntityId);
            return current != null
                    && !current.isRemoved()
                    && isTutor(current)
                    && player.squaredDistanceTo(current) <= MAX_INTERACTION_DISTANCE_SQUARED;
        }

        @Override
        public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity clickingPlayer) {
            if (clickingPlayer != player || actionType != SlotActionType.PICKUP || button != 0) return;
            if (!canUse(clickingPlayer)) {
                player.closeHandledScreen();
                return;
            }
            Selection selection = selections.get(slotIndex);
            if (selection == null) return;
            Optional<CanonicalMoveTutorInspectionService.Inspection> current = inspectionService.inspect(
                    playerId,
                    selection.partySlot()
            );
            if (current.isEmpty()
                    || !current.get().pokemonId().equals(selection.pokemonId())
                    || current.get().pokemonRevision() != selection.revision()) {
                player.sendMessage(Text.literal("Pokemon state changed on the server. Tutor view refreshed."), true);
                refresh();
                return;
            }
            var inspection = current.get();
            String moves = inspection.moveLoadoutAvailable()
                    ? (inspection.currentMoveIds().isEmpty() ? "none" : String.join(", ", inspection.currentMoveIds()))
                    : "unavailable";
            player.sendMessage(Text.literal(displayName(inspection.speciesId()) + " current moves: " + moves), false);
            player.sendMessage(Text.literal(
                    "Teaching, forgetting and relearning remain unavailable until AutoPTU supplies authoritative PTU learnability and mutation."),
                    false
            );
        }

        @Override
        public ItemStack quickMove(PlayerEntity player, int slot) {
            return ItemStack.EMPTY;
        }

        private void refresh() {
            selections.clear();
            for (int i = 0; i < TOP_SLOT_COUNT; i++) displayInventory.setStack(i, ItemStack.EMPTY);
            displayInventory.setStack(4, named(
                    Items.ENCHANTED_BOOK.getDefaultStack(),
                    "Select a canonical party Pokemon to inspect its current moves"
            ));
            int displaySlot = 10;
            for (int partySlot = 1; partySlot <= 6 && displaySlot <= 16; partySlot++) {
                var inspection = inspectionService.inspect(playerId, partySlot).orElse(null);
                if (inspection == null) continue;
                String moves = inspection.moveLoadoutAvailable()
                        ? (inspection.currentMoveIds().isEmpty() ? "none" : String.join(", ", inspection.currentMoveIds()))
                        : "unavailable";
                displayInventory.setStack(displaySlot, named(
                        Items.BOOK.getDefaultStack(),
                        "Slot " + partySlot + " — " + displayName(inspection.speciesId()) + " — " + moves
                ));
                selections.put(displaySlot, new Selection(partySlot, inspection.pokemonId(), inspection.pokemonRevision()));
                displaySlot++;
            }
            displayInventory.setStack(22, named(
                    Items.BARRIER.getDefaultStack(),
                    "Read-only shell: no move mutation without upstream PTU authority"
            ));
            displayInventory.markDirty();
            sendContentUpdates();
        }
    }

    private record Selection(int partySlot, String pokemonId, long revision) {}

    private static String displayName(String speciesId) {
        if (speciesId == null || speciesId.isBlank()) return "Unknown";
        String path = speciesId.contains(":") ? speciesId.substring(speciesId.indexOf(':') + 1) : speciesId;
        if (path.isEmpty()) return speciesId;
        return Character.toUpperCase(path.charAt(0)) + path.substring(1);
    }

    private static ItemStack named(ItemStack stack, String name) {
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
        return stack;
    }
}
