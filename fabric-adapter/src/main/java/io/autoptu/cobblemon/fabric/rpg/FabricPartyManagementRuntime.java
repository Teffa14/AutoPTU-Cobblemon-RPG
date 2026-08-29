package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalPartyLeadService;
import io.autoptu.cobblemon.authority.CanonicalPartyQueryService;
import io.autoptu.cobblemon.authority.CanonicalPartySummary;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;

/**
 * Server-authored party management surface. Minecraft renders canonical party state and submits only
 * slot selections. The server re-resolves party identity/revision before delegating lead mutation to
 * the existing durable authority service.
 */
public final class FabricPartyManagementRuntime {
    private static final int TOP_SLOT_COUNT = 27;
    private static final int[] PARTY_SLOTS = {10, 11, 12, 14, 15, 16};

    private FabricPartyManagementRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("party")
                                .then(CommandManager.literal("manage")
                                        .executes(context -> open(context.getSource()))))));
    }

    private static int open(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Party management must be requested by an authenticated player."));
            return 0;
        }
        if (queryParty(player) == null) {
            source.sendError(Text.literal("No persistent AutoPTU party exists yet."));
            return 0;
        }
        openScreen(player);
        return 1;
    }

    public static void openScreen(ServerPlayerEntity player) {
        if (player == null || player.getServer() == null) return;
        CanonicalPartySummary party = queryParty(player);
        if (party == null || party.members().isEmpty()) return;
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> new PartyManagementScreenHandler(syncId, inventory, player),
                Text.literal("Ouros Party")
        ));
    }

    static CanonicalPartySummary queryParty(ServerPlayerEntity player) {
        if (player == null || player.getServer() == null) return null;
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        CanonicalPartyQueryService service = new CanonicalPartyQueryService(
                FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(player.getServer()),
                FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(player.getServer())
        );
        try {
            return service.findParty(playerId).orElse(null);
        } catch (IllegalStateException inconsistentState) {
            return null;
        }
    }

    private static final class PartyManagementScreenHandler extends GenericContainerScreenHandler {
        private final SimpleInventory displayInventory;
        private final ServerPlayerEntity player;
        private CanonicalPartySummary displayedParty;

        private PartyManagementScreenHandler(int syncId, PlayerInventory inventory, ServerPlayerEntity player) {
            this(syncId, inventory, player, new SimpleInventory(TOP_SLOT_COUNT));
        }

        private PartyManagementScreenHandler(
                int syncId,
                PlayerInventory inventory,
                ServerPlayerEntity player,
                SimpleInventory displayInventory
        ) {
            super(ScreenHandlerType.GENERIC_9X3, syncId, inventory, displayInventory, 3);
            this.displayInventory = displayInventory;
            this.player = player;
            refresh();
        }

        @Override
        public boolean canUse(PlayerEntity clickingPlayer) {
            return clickingPlayer == player && !player.isRemoved() && player.getServer() != null;
        }

        @Override
        public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity clickingPlayer) {
            if (clickingPlayer != player || actionType != SlotActionType.PICKUP || button != 0) return;
            if (!canUse(clickingPlayer)) {
                player.closeHandledScreen();
                return;
            }

            int partySlot = partySlotForDisplaySlot(slotIndex);
            if (partySlot < 1 || displayedParty == null) return;
            CanonicalPartySummary.Member displayedMember = memberAt(displayedParty, partySlot);
            if (displayedMember == null) return;

            CanonicalPartySummary current = queryParty(player);
            CanonicalPartySummary.Member currentMember = current == null ? null : memberAt(current, partySlot);
            if (current == null
                    || current.partyRevision() != displayedParty.partyRevision()
                    || currentMember == null
                    || !currentMember.pokemonId().equals(displayedMember.pokemonId())) {
                refresh();
                player.sendMessage(Text.literal("Party changed on the server. The screen was refreshed."), true);
                return;
            }

            String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
            CanonicalPartyLeadService service = new CanonicalPartyLeadService(
                    FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(player.getServer())
            );
            CanonicalPartyLeadService.Decision decision = service.setLead(playerId, partySlot);
            switch (decision.outcome()) {
                case APPLIED -> player.sendMessage(Text.literal(
                        displayName(displayedMember.speciesId()) + " is now your party lead."), true);
                case ALREADY_LEAD -> player.sendMessage(Text.literal("That Pokemon is already your party lead."), true);
                case INVALID_SLOT, NO_PARTY, CONCURRENT_WRITE -> player.sendMessage(Text.literal(
                        "Party lead change was rejected: " + decision.reason()), false);
            }
            refresh();
        }

        @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }

        private void refresh() {
            displayedParty = queryParty(player);
            for (int i = 0; i < TOP_SLOT_COUNT; i++) displayInventory.setStack(i, ItemStack.EMPTY);
            if (displayedParty == null || displayedParty.members().isEmpty()) {
                displayInventory.setStack(13, named(Items.BARRIER.getDefaultStack(), "No canonical party available"));
            } else {
                List<CanonicalPartySummary.Member> members = displayedParty.members();
                for (int i = 0; i < members.size() && i < PARTY_SLOTS.length; i++) {
                    CanonicalPartySummary.Member member = members.get(i);
                    ItemStack icon = (member.slot() == 1 ? Items.LIME_CONCRETE : Items.LIGHT_BLUE_CONCRETE).getDefaultStack();
                    displayInventory.setStack(PARTY_SLOTS[i], named(icon, memberLabel(member)));
                }
                displayInventory.setStack(22, named(
                        Items.COMPASS.getDefaultStack(),
                        "Click a party member to make it lead"
                ));
            }
            displayInventory.markDirty();
            sendContentUpdates();
        }

        private static int partySlotForDisplaySlot(int displaySlot) {
            for (int i = 0; i < PARTY_SLOTS.length; i++) {
                if (PARTY_SLOTS[i] == displaySlot) return i + 1;
            }
            return -1;
        }

        private static CanonicalPartySummary.Member memberAt(CanonicalPartySummary party, int partySlot) {
            return party.members().stream().filter(member -> member.slot() == partySlot).findFirst().orElse(null);
        }
    }

    static String memberLabel(CanonicalPartySummary.Member member) {
        String hp = member.hasHealth() ? member.currentHp() + "/" + member.maxHp() + " HP" : "HP unavailable";
        String lead = member.slot() == 1 ? "LEAD | " : "";
        return lead + displayName(member.speciesId()) + " Lv." + member.level() + " | " + hp;
    }

    private static String displayName(String speciesId) {
        if (speciesId == null || speciesId.isBlank()) return "Unknown";
        String path = speciesId.contains(":") ? speciesId.substring(speciesId.indexOf(':') + 1) : speciesId;
        if (path.isEmpty()) return speciesId;
        return path.substring(0, 1).toUpperCase(Locale.ROOT) + path.substring(1);
    }

    private static ItemStack named(ItemStack stack, String name) {
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
        return stack;
    }
}
