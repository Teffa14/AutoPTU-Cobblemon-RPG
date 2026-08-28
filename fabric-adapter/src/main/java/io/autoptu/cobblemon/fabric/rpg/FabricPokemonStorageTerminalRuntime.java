package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalPartyQueryService;
import io.autoptu.cobblemon.authority.CanonicalPartySummary;
import io.autoptu.cobblemon.authority.CanonicalPokemonStorageQueryService;
import io.autoptu.cobblemon.authority.CanonicalPokemonStorageSummary;
import io.autoptu.cobblemon.authority.CanonicalPokemonStorageTransferService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Physical Cobblemon-PC surface for AutoPTU-owned party and boxed Pokemon.
 *
 * The Cobblemon block is presentation only. This runtime never opens or reads Cobblemon's party,
 * PCStore, Pokemon payloads, HP, moves, ownership or battle state. Every displayed member and every
 * transfer is resolved from AutoPTU's canonical world-save repositories on the server.
 */
public final class FabricPokemonStorageTerminalRuntime {
    static final Identifier COBBLEMON_PC_ID = Identifier.of("cobblemon", "pc");
    private static final double MAX_INTERACTION_DISTANCE_SQUARED = 25.0D;

    private FabricPokemonStorageTerminalRuntime() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient() || hand != Hand.MAIN_HAND || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }

            BlockPos terminalPos = hitResult.getBlockPos();
            if (!isCanonicalPc(world, terminalPos)) {
                return ActionResult.PASS;
            }
            if (!withinInteractionDistance(serverPlayer, terminalPos)) {
                serverPlayer.sendMessage(Text.literal("You are too far away to use this AutoPTU PC."), false);
                return ActionResult.FAIL;
            }

            try {
                serverPlayer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                        (syncId, playerInventory, ignored) -> new CanonicalPokemonStorageScreenHandler(
                                syncId, playerInventory, serverPlayer, terminalPos),
                        Text.literal("AutoPTU Pokemon Storage")
                ));
                return ActionResult.SUCCESS;
            } catch (IllegalStateException invalidCanonicalState) {
                serverPlayer.sendMessage(Text.literal(
                        "AutoPTU storage could not be opened safely because canonical party/box state is inconsistent."), false);
                return ActionResult.FAIL;
            }
        });
    }

    static boolean isCanonicalPc(World world, BlockPos pos) {
        return COBBLEMON_PC_ID.equals(Registries.BLOCK.getId(world.getBlockState(pos).getBlock()));
    }

    static boolean withinInteractionDistance(ServerPlayerEntity player, BlockPos pos) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        return player.squaredDistanceTo(x, y, z) <= MAX_INTERACTION_DISTANCE_SQUARED;
    }

    static final class CanonicalPokemonStorageScreenHandler extends GenericContainerScreenHandler {
        private static final int TOP_SLOT_COUNT = 54;
        private static final int PARTY_SLOT_COUNT = 6;
        private static final int PREVIOUS_PAGE_SLOT = 6;
        private static final int INFO_SLOT = 7;
        private static final int NEXT_PAGE_SLOT = 8;
        private static final int BOX_START_SLOT = 9;
        private static final int BOX_SLOTS_PER_PAGE = 45;

        private final SimpleInventory displayInventory;
        private final ServerPlayerEntity player;
        private final BlockPos terminalPos;
        private final String playerId;
        private final CanonicalPartyQueryService partyQuery;
        private final CanonicalPokemonStorageQueryService storageQuery;
        private final CanonicalPokemonStorageTransferService transferService;
        private final Map<Integer, PartySelection> partySelections = new HashMap<>();
        private final Map<Integer, BoxSelection> boxSelections = new HashMap<>();
        private int page;
        private int pageCount = 1;

        CanonicalPokemonStorageScreenHandler(
                int syncId,
                PlayerInventory playerInventory,
                ServerPlayerEntity player,
                BlockPos terminalPos
        ) {
            this(syncId, playerInventory, player, terminalPos, new SimpleInventory(TOP_SLOT_COUNT));
        }

        private CanonicalPokemonStorageScreenHandler(
                int syncId,
                PlayerInventory playerInventory,
                ServerPlayerEntity player,
                BlockPos terminalPos,
                SimpleInventory displayInventory
        ) {
            super(ScreenHandlerType.GENERIC_9X6, syncId, playerInventory, displayInventory, 6);
            this.displayInventory = displayInventory;
            this.player = player;
            this.terminalPos = terminalPos.toImmutable();
            this.playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
            this.partyQuery = new CanonicalPartyQueryService(
                    FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(player.getServer()),
                    FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(player.getServer())
            );
            this.storageQuery = new CanonicalPokemonStorageQueryService(
                    FabricCanonicalPlayerStoreRuntime.requirePokemonStorageRepository(player.getServer()),
                    FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(player.getServer()),
                    FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(player.getServer())
            );
            this.transferService = new CanonicalPokemonStorageTransferService(
                    FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(player.getServer()),
                    FabricCanonicalPlayerStoreRuntime.requirePokemonStorageRepository(player.getServer()),
                    FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(player.getServer()),
                    FabricCanonicalPlayerStoreRuntime.requirePokemonTransferRepository(player.getServer())
            );
            refresh();
        }

        @Override
        public boolean canUse(PlayerEntity player) {
            return player == this.player
                    && !player.isRemoved()
                    && isCanonicalPc(player.getWorld(), terminalPos)
                    && player.squaredDistanceTo(
                            terminalPos.getX() + 0.5D,
                            terminalPos.getY() + 0.5D,
                            terminalPos.getZ() + 0.5D) <= MAX_INTERACTION_DISTANCE_SQUARED;
        }

        @Override
        public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity clickingPlayer) {
            if (clickingPlayer != player || actionType != SlotActionType.PICKUP || button != 0) {
                return;
            }
            if (!canUse(clickingPlayer)) {
                player.closeHandledScreen();
                return;
            }
            if (slotIndex < 0 || slotIndex >= TOP_SLOT_COUNT) {
                return;
            }

            if (slotIndex == PREVIOUS_PAGE_SLOT) {
                if (page > 0) {
                    page--;
                    refresh();
                }
                return;
            }
            if (slotIndex == NEXT_PAGE_SLOT) {
                if (page + 1 < pageCount) {
                    page++;
                    refresh();
                }
                return;
            }

            PartySelection partySelection = partySelections.get(slotIndex);
            if (partySelection != null) {
                deposit(partySelection);
                return;
            }
            BoxSelection boxSelection = boxSelections.get(slotIndex);
            if (boxSelection != null) {
                withdraw(boxSelection);
            }
        }

        @Override
        public ItemStack quickMove(PlayerEntity player, int slot) {
            return ItemStack.EMPTY;
        }

        private void deposit(PartySelection selection) {
            if (!partySelectionStillCurrent(selection)) {
                player.sendMessage(Text.literal("Party changed while the PC was open. Storage view refreshed; click again."), false);
                refresh();
                return;
            }
            try {
                CanonicalPokemonStorageTransferService.TransferResult result = transferService.deposit(
                        transferId(), playerId, selection.partySlot());
                player.sendMessage(Text.literal("Stored " + selection.displayName()
                        + " in the canonical box."), false);
                refresh();
            } catch (IllegalArgumentException invalidRequest) {
                player.sendMessage(Text.literal(invalidRequest.getMessage()), false);
                refresh();
            } catch (IllegalStateException invalidState) {
                player.sendMessage(Text.literal(
                        "AutoPTU could not safely store that Pokemon. The recoverable canonical transfer was preserved."), false);
                refresh();
            }
        }

        private void withdraw(BoxSelection selection) {
            if (!boxSelectionStillCurrent(selection)) {
                player.sendMessage(Text.literal("Box changed while the PC was open. Storage view refreshed; click again."), false);
                refresh();
                return;
            }
            try {
                CanonicalPokemonStorageTransferService.TransferResult result = transferService.withdraw(
                        transferId(), playerId, selection.boxSlot());
                player.sendMessage(Text.literal("Moved " + selection.displayName()
                        + " to the active canonical party."), false);
                refresh();
            } catch (IllegalArgumentException invalidRequest) {
                player.sendMessage(Text.literal(invalidRequest.getMessage()), false);
                refresh();
            } catch (IllegalStateException invalidState) {
                player.sendMessage(Text.literal(
                        "AutoPTU could not safely withdraw that Pokemon. The recoverable canonical transfer was preserved."), false);
                refresh();
            }
        }

        private boolean partySelectionStillCurrent(PartySelection selection) {
            CanonicalPartySummary current = partyQuery.findParty(playerId).orElse(null);
            if (current == null) return false;
            return current.members().stream().anyMatch(member ->
                    member.slot() == selection.partySlot() && member.pokemonId().equals(selection.pokemonId()));
        }

        private boolean boxSelectionStillCurrent(BoxSelection selection) {
            CanonicalPokemonStorageSummary current = storageQuery.inspect(playerId);
            return current.members().stream().anyMatch(member ->
                    member.boxSlot() == selection.boxSlot() && member.pokemonId().equals(selection.pokemonId()));
        }

        private void refresh() {
            CanonicalPartySummary party = partyQuery.findParty(playerId).orElse(null);
            CanonicalPokemonStorageSummary storage = storageQuery.inspect(playerId);
            List<CanonicalPokemonStorageSummary.Member> boxed = storage.members();

            pageCount = Math.max(1, (boxed.size() + BOX_SLOTS_PER_PAGE - 1) / BOX_SLOTS_PER_PAGE);
            if (page >= pageCount) page = pageCount - 1;
            if (page < 0) page = 0;

            partySelections.clear();
            boxSelections.clear();
            for (int i = 0; i < TOP_SLOT_COUNT; i++) {
                displayInventory.setStack(i, ItemStack.EMPTY);
            }

            if (party != null) {
                for (CanonicalPartySummary.Member member : party.members()) {
                    int menuSlot = member.slot() - 1;
                    if (menuSlot < 0 || menuSlot >= PARTY_SLOT_COUNT) continue;
                    String name = displayName(member.speciesId());
                    String hp = member.hasHealth() ? " · " + member.currentHp() + "/" + member.maxHp() + " HP" : "";
                    displayInventory.setStack(menuSlot, menuItem(
                            Items.LIME_DYE.getDefaultStack(),
                            "Party " + member.slot() + ": " + name + " Lv." + member.level() + hp + " · click to store"
                    ));
                    partySelections.put(menuSlot, new PartySelection(member.slot(), member.pokemonId(), name));
                }
            }

            if (page > 0) {
                displayInventory.setStack(PREVIOUS_PAGE_SLOT, menuItem(Items.ARROW.getDefaultStack(), "Previous box page"));
            }
            displayInventory.setStack(INFO_SLOT, menuItem(
                    Items.BOOK.getDefaultStack(),
                    "Canonical PC · party " + (party == null ? 0 : party.members().size())
                            + " · box " + boxed.size() + " · page " + (page + 1) + "/" + pageCount
            ));
            if (page + 1 < pageCount) {
                displayInventory.setStack(NEXT_PAGE_SLOT, menuItem(Items.ARROW.getDefaultStack(), "Next box page"));
            }

            int from = page * BOX_SLOTS_PER_PAGE;
            int to = Math.min(boxed.size(), from + BOX_SLOTS_PER_PAGE);
            for (int index = from; index < to; index++) {
                CanonicalPokemonStorageSummary.Member member = boxed.get(index);
                int menuSlot = BOX_START_SLOT + (index - from);
                String name = displayName(member.speciesId());
                displayInventory.setStack(menuSlot, menuItem(
                        Items.CYAN_DYE.getDefaultStack(),
                        "Box " + member.boxSlot() + ": " + name + " Lv." + member.level() + " · click to withdraw"
                ));
                boxSelections.put(menuSlot, new BoxSelection(member.boxSlot(), member.pokemonId(), name));
            }

            displayInventory.markDirty();
            sendContentUpdates();
        }

        private String transferId() {
            return "pokemon-transfer:" + playerId + ":pc:" + UUID.randomUUID();
        }

        private static ItemStack menuItem(ItemStack stack, String name) {
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
            return stack;
        }
    }

    private static String displayName(String speciesId) {
        if (speciesId == null || speciesId.isBlank()) return "Unknown";
        String path = speciesId.contains(":") ? speciesId.substring(speciesId.indexOf(':') + 1) : speciesId;
        if (path.isEmpty()) return speciesId;
        return path.substring(0, 1).toUpperCase(Locale.ROOT) + path.substring(1);
    }

    private record PartySelection(int partySlot, String pokemonId, String displayName) {}
    private record BoxSelection(int boxSlot, String pokemonId, String displayName) {}
}
