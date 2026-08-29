package io.autoptu.cobblemon.fabric.rpg;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.autoptu.cobblemon.authority.CanonicalAccuracyEvasion;
import io.autoptu.cobblemon.authority.CanonicalBaseMovement;
import io.autoptu.cobblemon.authority.CanonicalBattleTraits;
import io.autoptu.cobblemon.authority.CanonicalCombatStats;
import io.autoptu.cobblemon.authority.CanonicalPokemonDetail;
import io.autoptu.cobblemon.authority.CanonicalPokemonDetailService;
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

import java.util.Locale;

/** Minecraft-facing read-only screen for one durable canonical party Pokemon. */
public final class FabricPokemonDetailRuntime {
    private static final int TOP_SLOT_COUNT = 54;

    private FabricPokemonDetailRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("pokemon")
                                .then(CommandManager.argument("slot", IntegerArgumentType.integer(1))
                                        .executes(context -> show(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "slot")
                                        ))))));
    }

    private static int show(ServerCommandSource source, int slot) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Pokemon inspection must be requested by an authenticated player."));
            return 0;
        }
        return openScreen(player, slot) ? 1 : 0;
    }

    public static boolean openScreen(ServerPlayerEntity player, int slot) {
        if (player == null || player.getServer() == null || slot < 1) return false;
        CanonicalPokemonDetail detail = queryDetail(player, slot);
        if (detail == null) {
            player.sendMessage(Text.literal("No canonical Pokemon exists in party slot " + slot + "."), false);
            return false;
        }
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> new PokemonSummaryScreenHandler(syncId, inventory, player, slot),
                Text.literal(displayName(detail.speciesId()) + " Summary")
        ));
        return true;
    }

    static CanonicalPokemonDetail queryDetail(ServerPlayerEntity player, int slot) {
        if (player == null || player.getServer() == null || slot < 1) return null;
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        CanonicalPokemonDetailService service = new CanonicalPokemonDetailService(
                FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(player.getServer()),
                FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(player.getServer())
        );
        try {
            return service.findPokemon(playerId, slot).orElse(null);
        } catch (IllegalStateException invalidCanonicalState) {
            player.sendMessage(Text.literal("AutoPTU Pokemon state is inconsistent and cannot be displayed safely."), false);
            return null;
        }
    }

    private static final class PokemonSummaryScreenHandler extends GenericContainerScreenHandler {
        private final SimpleInventory displayInventory;
        private final ServerPlayerEntity player;
        private final int partySlot;
        private String displayedPokemonId;
        private long displayedRevision;

        private PokemonSummaryScreenHandler(
                int syncId,
                PlayerInventory inventory,
                ServerPlayerEntity player,
                int partySlot
        ) {
            this(syncId, inventory, player, partySlot, new SimpleInventory(TOP_SLOT_COUNT));
        }

        private PokemonSummaryScreenHandler(
                int syncId,
                PlayerInventory inventory,
                ServerPlayerEntity player,
                int partySlot,
                SimpleInventory displayInventory
        ) {
            super(ScreenHandlerType.GENERIC_9X6, syncId, inventory, displayInventory, 6);
            this.displayInventory = displayInventory;
            this.player = player;
            this.partySlot = partySlot;
            refresh();
        }

        @Override
        public boolean canUse(PlayerEntity clickingPlayer) {
            return clickingPlayer == player && !player.isRemoved() && player.getServer() != null;
        }

        @Override
        public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity clickingPlayer) {
            if (clickingPlayer != player || slotIndex < 0 || slotIndex >= TOP_SLOT_COUNT) return;
            if (!canUse(clickingPlayer)) {
                player.closeHandledScreen();
                return;
            }
            CanonicalPokemonDetail current = queryDetail(player, partySlot);
            if (current == null
                    || displayedPokemonId == null
                    || !displayedPokemonId.equals(current.pokemonId())
                    || displayedRevision != current.revision()) {
                refresh();
                player.sendMessage(Text.literal("Pokemon state changed on the server. The summary was refreshed."), true);
            }
        }

        @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }

        private void refresh() {
            CanonicalPokemonDetail detail = queryDetail(player, partySlot);
            for (int i = 0; i < TOP_SLOT_COUNT; i++) displayInventory.setStack(i, ItemStack.EMPTY);
            if (detail == null) {
                displayedPokemonId = null;
                displayedRevision = -1;
                displayInventory.setStack(22, named(Items.BARRIER.getDefaultStack(), "Canonical Pokemon unavailable"));
                displayInventory.markDirty();
                sendContentUpdates();
                return;
            }

            displayedPokemonId = detail.pokemonId();
            displayedRevision = detail.revision();
            displayInventory.setStack(4, named(Items.NAME_TAG.getDefaultStack(),
                    displayName(detail.speciesId()) + " | Party slot " + detail.slot()));
            displayInventory.setStack(10, named(Items.EXPERIENCE_BOTTLE.getDefaultStack(),
                    "Level " + detail.level() + " | XP unavailable"));
            displayInventory.setStack(11, named(Items.REDSTONE.getDefaultStack(),
                    "HP " + health(detail)));
            displayInventory.setStack(12, named(Items.MILK_BUCKET.getDefaultStack(),
                    "Status " + listOr(detail.statuses(), "clear")));
            displayInventory.setStack(13, named(Items.IRON_BOOTS.getDefaultStack(),
                    "Injuries " + injuries(detail)));
            displayInventory.setStack(14, named(Items.PRISMARINE_CRYSTALS.getDefaultStack(),
                    traits(detail.battleTraits())));
            displayInventory.setStack(15, named(Items.BOOK.getDefaultStack(),
                    "Moves " + (detail.moveLoadout() == null ? "unavailable" : listOr(detail.moveLoadout().moveIds(), "none"))));
            displayInventory.setStack(16, named(Items.CHEST.getDefaultStack(),
                    "Held item " + (detail.heldItemEquipped() ? "equipped" : "none")));

            displayInventory.setStack(20, named(Items.IRON_SWORD.getDefaultStack(), stats(detail.combatStats())));
            displayInventory.setStack(22, named(Items.LEATHER_BOOTS.getDefaultStack(), movement(detail.baseMovement())));
            displayInventory.setStack(24, named(Items.TARGET.getDefaultStack(), accuracy(detail.accuracyEvasion())));

            displayInventory.setStack(30, named(Items.COMPASS.getDefaultStack(),
                    "Capabilities " + listOr(detail.capabilities(), "none")));
            displayInventory.setStack(32, named(Items.PAPER.getDefaultStack(),
                    "Canonical revision " + detail.revision()));
            displayInventory.setStack(49, named(Items.BARRIER.getDefaultStack(),
                    "Read-only canonical summary"));
            displayInventory.markDirty();
            sendContentUpdates();
        }
    }

    static String health(CanonicalPokemonDetail detail) {
        return detail.health() == null ? "unavailable" : detail.health().currentHp() + "/" + detail.health().maxHp();
    }

    static String stats(CanonicalCombatStats stats) {
        if (stats == null) return "Combat stats unavailable";
        return "ATK " + stats.atk() + " | DEF " + stats.def()
                + " | SPATK " + stats.spatk() + " | SPDEF " + stats.spdef() + " | SPD " + stats.spd();
    }

    static String movement(CanonicalBaseMovement movement) {
        if (movement == null) return "Base movement unavailable";
        return "Movement OVR " + movement.overland() + " | SWIM " + movement.swim()
                + " | SKY " + movement.sky() + " | LJ " + movement.longJump()
                + " | HJ " + movement.highJump();
    }

    static String accuracy(CanonicalAccuracyEvasion accuracy) {
        if (accuracy == null) return "Accuracy/evasion unavailable";
        return "Accuracy " + accuracy.accuracyStage()
                + " | PEV " + accuracy.physicalEvasionBonus()
                + " | SEV " + accuracy.specialEvasionBonus()
                + " | STEV " + accuracy.statusEvasionBonus();
    }

    static String traits(CanonicalBattleTraits traits) {
        if (traits == null) return "Types unavailable | Abilities unavailable";
        return "Types " + listOr(traits.types(), "unavailable")
                + " | Abilities " + listOr(traits.abilities(), "none");
    }

    static String injuries(CanonicalPokemonDetail detail) {
        return detail.injuryState() == null ? "unavailable" : Integer.toString(detail.injuryState().injuries());
    }

    private static String listOr(java.util.List<String> values, String fallback) {
        return values == null || values.isEmpty() ? fallback : String.join(", ", values);
    }

    static String displayName(String speciesId) {
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
