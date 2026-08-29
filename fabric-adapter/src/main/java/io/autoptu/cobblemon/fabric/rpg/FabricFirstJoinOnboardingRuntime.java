package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.FileCanonicalPlayerEncounterProfileRepository;
import io.autoptu.cobblemon.authority.FileVersionedCanonicalStateRepository;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
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

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Normal first-entry surface for an authenticated Minecraft player who has a canonical Trainer
 * but has not selected a persistent starter/party yet.
 *
 * <p>The client contributes no Trainer fields or starter state. The server derives the canonical
 * player id from the authenticated UUID and checks the world-save encounter profile repository.
 * Existing players with a canonical party are never interrupted by this onboarding screen.</p>
 */
public final class FabricFirstJoinOnboardingRuntime {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static final int TOP_SLOT_COUNT = 27;
    private static final int TITLE_SLOT = 4;
    private static final int TRAINER_SLOT = 11;
    private static final int NEXT_SLOT = 15;

    private FabricFirstJoinOnboardingRuntime() {}

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) return;
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            server.execute(() -> openIfNeeded(player));
        });
    }

    static boolean needsOnboarding(ServerPlayerEntity player) {
        if (player == null || player.getServer() == null) return false;
        return needsOnboarding(
                FabricCanonicalPlayerStoreRuntime.requireRepository(player.getServer()),
                FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(player.getServer()),
                player.getUuid()
        );
    }

    static boolean needsOnboarding(
            FileVersionedCanonicalStateRepository playerRepository,
            FileCanonicalPlayerEncounterProfileRepository encounterProfileRepository,
            UUID authenticatedUuid
    ) {
        if (playerRepository == null || encounterProfileRepository == null || authenticatedUuid == null) return false;
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(authenticatedUuid);
        return playerRepository.findPlayer(playerId).isPresent()
                && encounterProfileRepository.findProfile(playerId).isEmpty();
    }

    static void openIfNeeded(ServerPlayerEntity player) {
        if (!needsOnboarding(player)) return;
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> new OnboardingScreenHandler(syncId, inventory, player, playerId),
                Text.literal("Welcome to Ouros")
        ));
    }

    private static final class OnboardingScreenHandler extends GenericContainerScreenHandler {
        private final SimpleInventory displayInventory;
        private final ServerPlayerEntity player;
        private final String playerId;

        OnboardingScreenHandler(int syncId, PlayerInventory inventory, ServerPlayerEntity player, String playerId) {
            this(syncId, inventory, player, playerId, new SimpleInventory(TOP_SLOT_COUNT));
        }

        private OnboardingScreenHandler(
                int syncId,
                PlayerInventory inventory,
                ServerPlayerEntity player,
                String playerId,
                SimpleInventory displayInventory
        ) {
            super(ScreenHandlerType.GENERIC_9X3, syncId, inventory, displayInventory, 3);
            this.displayInventory = displayInventory;
            this.player = player;
            this.playerId = playerId;
            refresh();
        }

        @Override
        public boolean canUse(PlayerEntity clickingPlayer) {
            if (clickingPlayer != player || player.isRemoved() || player.getServer() == null) return false;
            String authenticated = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
            if (!authenticated.equals(playerId)) return false;
            return needsOnboarding(
                    FabricCanonicalPlayerStoreRuntime.requireRepository(player.getServer()),
                    FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(player.getServer()),
                    player.getUuid()
            );
        }

        @Override
        public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity clickingPlayer) {
            if (clickingPlayer != player || actionType != SlotActionType.PICKUP || button != 0) return;
            if (!canUse(clickingPlayer)) {
                player.closeHandledScreen();
                return;
            }
            if (slotIndex != NEXT_SLOT) return;
            player.closeHandledScreen();
            player.sendMessage(Text.literal("Trainer loaded. Choose your starter to begin your Ouros journey."), false);
            player.sendMessage(Text.literal("Starter fallback: /autoptu starter list"), false);
        }

        @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }

        private void refresh() {
            for (int i = 0; i < TOP_SLOT_COUNT; i++) displayInventory.setStack(i, ItemStack.EMPTY);
            displayInventory.setStack(TITLE_SLOT, named(
                    Items.MAP.getDefaultStack(),
                    "Ouros Pokemon RPG"
            ));
            displayInventory.setStack(TRAINER_SLOT, named(
                    Items.NAME_TAG.getDefaultStack(),
                    "Canonical Trainer ready"
            ));
            displayInventory.setStack(NEXT_SLOT, named(
                    Items.COMPASS.getDefaultStack(),
                    "Continue to starter selection"
            ));
            displayInventory.markDirty();
            sendContentUpdates();
        }

        private static ItemStack named(ItemStack stack, String name) {
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
            return stack;
        }
    }
}
