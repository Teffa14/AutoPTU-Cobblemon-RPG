package io.autoptu.cobblemon.fabric.rpg;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import io.autoptu.cobblemon.authority.CanonicalStarterCatalogue;
import io.autoptu.cobblemon.authority.CanonicalStarterSelectionDecision;
import io.autoptu.cobblemon.authority.CanonicalStarterSelectionService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.IdentifierArgumentType;
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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/** Minecraft-facing one-time starter claim backed only by server-owned canonical state. */
public final class FabricStarterSelectionRuntime {
    private static final CanonicalStarterCatalogue CATALOGUE = new CanonicalStarterCatalogue();
    private static final int TOP_SLOT_COUNT = 27;
    private static final int[] OPTION_SLOTS = {10, 13, 16};
    private static final int CONFIRM_SLOT = 22;

    private FabricStarterSelectionRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("starter")
                                .then(CommandManager.literal("choose")
                                        .then(CommandManager.argument("species", IdentifierArgumentType.identifier())
                                                .executes(context -> choose(
                                                        context.getSource(),
                                                        IdentifierArgumentType.getIdentifier(context, "species")
                                                )))))));
    }

    /**
     * Opens the normal gameplay selector. Starter options are rebuilt from the server catalogue;
     * the client only clicks server-owned slots. Selecting an option creates a temporary Cobblemon
     * presentation actor from that server-authored species so the player can preview the model.
     * No gameplay data is ever read back from the preview actor.
     */
    public static void openSelectionScreen(ServerPlayerEntity player) {
        if (player == null || player.getServer() == null) return;
        if (!FabricFirstJoinOnboardingRuntime.needsOnboarding(player)) return;
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> new StarterSelectionScreenHandler(syncId, inventory, player),
                Text.literal("Choose your Ouros starter")
        ));
    }

    private static int choose(ServerCommandSource source, Identifier requestedSpecies) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Starter selection must be requested by an authenticated player."));
            return 0;
        }

        String requestedId = requestedSpecies.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)
                ? requestedSpecies.getPath()
                : requestedSpecies.toString();
        CanonicalStarterSelectionDecision decision = chooseForPlayer(player, requestedId);

        return switch (decision.outcome()) {
            case CHOSEN -> {
                player.sendMessage(Text.literal(
                        "AutoPTU starter chosen: " + displayName(decision.speciesId())
                                + ". Your persistent canonical party now contains this Pokemon."), false);
                yield 1;
            }
            case ALREADY_CHOSEN -> {
                source.sendError(Text.literal(
                        decision.speciesId().isBlank()
                                ? "A persistent AutoPTU party already exists for this player."
                                : "Starter already chosen: " + displayName(decision.speciesId()) + "."));
                yield 0;
            }
            case INVALID_STARTER -> {
                source.sendError(Text.literal(
                        "That species is not an available starter. Use /autoptu starter list."));
                yield 0;
            }
            case INVALID_REQUEST, CONFLICT -> {
                source.sendError(Text.literal("AutoPTU could not persist the starter claim: " + decision.detail()));
                yield 0;
            }
        };
    }

    static CanonicalStarterSelectionDecision chooseForPlayer(ServerPlayerEntity player, String requestedId) {
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        BlockPos pos = player.getBlockPos();
        BattleArenaSnapshot arena = new BattleArenaSnapshot(
                player.getServerWorld().getRegistryKey().getValue().toString(),
                pos.getX(), pos.getY(), pos.getZ(),
                1, 0,
                0, 1
        );
        CanonicalStarterSelectionService service = new CanonicalStarterSelectionService(
                CATALOGUE,
                FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(player.getServer()),
                FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(player.getServer())
        );
        return service.choose(playerId, requestedId, arena);
    }

    private static String displayName(String speciesId) {
        return CATALOGUE.findConfigured(speciesId)
                .map(CanonicalStarterCatalogue.StarterOption::displayName)
                .orElse(speciesId);
    }

    private static final class StarterSelectionScreenHandler extends GenericContainerScreenHandler {
        private final SimpleInventory displayInventory;
        private final ServerPlayerEntity player;
        private String selectedSpeciesId;
        private PokemonEntity previewEntity;

        private StarterSelectionScreenHandler(int syncId, PlayerInventory inventory, ServerPlayerEntity player) {
            this(syncId, inventory, player, new SimpleInventory(TOP_SLOT_COUNT));
        }

        private StarterSelectionScreenHandler(
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
            return clickingPlayer == player
                    && !player.isRemoved()
                    && player.getServer() != null
                    && FabricFirstJoinOnboardingRuntime.needsOnboarding(player);
        }

        @Override
        public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity clickingPlayer) {
            if (clickingPlayer != player || actionType != SlotActionType.PICKUP || button != 0) return;
            if (!canUse(clickingPlayer)) {
                player.closeHandledScreen();
                return;
            }

            CanonicalStarterCatalogue.StarterOption option = optionForSlot(slotIndex);
            if (option != null) {
                selectedSpeciesId = option.speciesId();
                replacePreview(option);
                refresh();
                player.sendMessage(Text.literal("Previewing " + option.displayName() + ". Confirm when ready."), true);
                return;
            }

            if (slotIndex != CONFIRM_SLOT || selectedSpeciesId == null) return;
            CanonicalStarterSelectionDecision decision = chooseForPlayer(player, selectedSpeciesId);
            switch (decision.outcome()) {
                case CHOSEN -> {
                    cleanupPreview();
                    player.closeHandledScreen();
                    player.sendMessage(Text.literal(
                            "Starter chosen: " + displayName(decision.speciesId())
                                    + ". Your canonical party is ready."), false);
                }
                case ALREADY_CHOSEN -> {
                    cleanupPreview();
                    player.closeHandledScreen();
                    player.sendMessage(Text.literal("Your canonical starter/party already exists."), false);
                }
                case INVALID_STARTER -> {
                    selectedSpeciesId = null;
                    cleanupPreview();
                    refresh();
                    player.sendMessage(Text.literal("That starter is no longer available. Choose again."), false);
                }
                case INVALID_REQUEST, CONFLICT -> {
                    refresh();
                    player.sendMessage(Text.literal("Starter claim was not committed: " + decision.detail()), false);
                }
            }
        }

        @Override
        public void onClosed(PlayerEntity player) {
            cleanupPreview();
            super.onClosed(player);
        }

        @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }

        private CanonicalStarterCatalogue.StarterOption optionForSlot(int slotIndex) {
            List<CanonicalStarterCatalogue.StarterOption> options = CATALOGUE.configuredStarters();
            int count = Math.min(options.size(), OPTION_SLOTS.length);
            for (int i = 0; i < count; i++) {
                if (slotIndex == OPTION_SLOTS[i]) return options.get(i);
            }
            return null;
        }

        private void refresh() {
            for (int i = 0; i < TOP_SLOT_COUNT; i++) displayInventory.setStack(i, ItemStack.EMPTY);
            List<CanonicalStarterCatalogue.StarterOption> options = CATALOGUE.configuredStarters();
            int count = Math.min(options.size(), OPTION_SLOTS.length);
            for (int i = 0; i < count; i++) {
                CanonicalStarterCatalogue.StarterOption option = options.get(i);
                displayInventory.setStack(OPTION_SLOTS[i], named(
                        presentationIcon(option.speciesId()),
                        (option.speciesId().equals(selectedSpeciesId) ? "Selected: " : "Preview: ") + option.displayName()
                ));
            }
            if (selectedSpeciesId != null) {
                displayInventory.setStack(CONFIRM_SLOT, named(
                        Items.LIME_DYE.getDefaultStack(),
                        "Confirm " + displayName(selectedSpeciesId)
                ));
            }
            displayInventory.markDirty();
            sendContentUpdates();
        }

        private void replacePreview(CanonicalStarterCatalogue.StarterOption option) {
            cleanupPreview();
            Species species = PokemonSpecies.INSTANCE.getByName(option.speciesId());
            if (species == null) return;

            ServerWorld world = player.getServerWorld();
            Pokemon pokemon = new Pokemon();
            pokemon.setSpecies(species);
            PokemonEntity entity = new PokemonEntity(world, pokemon, CobblemonEntities.POKEMON);
            double yawRadians = Math.toRadians(player.getYaw());
            double x = player.getX() - Math.sin(yawRadians) * 2.5D;
            double z = player.getZ() + Math.cos(yawRadians) * 2.5D;
            entity.refreshPositionAndAngles(x, player.getY(), z, player.getYaw() + 180.0F, 0.0F);
            entity.setCustomName(Text.literal(option.displayName() + " preview"));
            entity.setCustomNameVisible(true);
            entity.setInvulnerable(true);
            if (world.spawnEntity(entity)) {
                previewEntity = entity;
            }
        }

        private void cleanupPreview() {
            if (previewEntity != null && !previewEntity.isRemoved()) previewEntity.discard();
            previewEntity = null;
        }

        private static ItemStack presentationIcon(String speciesId) {
            return switch (speciesId) {
                case "bulbasaur" -> Items.MOSS_BLOCK.getDefaultStack();
                case "charmander" -> Items.BLAZE_POWDER.getDefaultStack();
                case "squirtle" -> Items.WATER_BUCKET.getDefaultStack();
                default -> Items.EGG.getDefaultStack();
            };
        }

        private static ItemStack named(ItemStack stack, String name) {
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
            return stack;
        }
    }
}
