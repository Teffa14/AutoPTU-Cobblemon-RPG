package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalLeagueChallengeCatalogue;
import io.autoptu.cobblemon.authority.CanonicalLeagueRegistrationService;
import io.autoptu.cobblemon.authority.CanonicalTrainerChallengeCatalogue;
import io.autoptu.cobblemon.authority.CanonicalTrainerChallengeRequestService;
import io.autoptu.cobblemon.authority.FileCanonicalLeagueRegistrationRepository;
import io.autoptu.cobblemon.authority.FileCanonicalTrainerRecordRepository;
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
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.nio.file.Path;

/** Physical Gym/League registration surface backed by durable RPG registration state. */
public final class FabricGymLeagueRegistrationRuntime {
    static final String DESK_PROVIDER_ID = "cedar-league-desk";
    static final String CEDAR_GYM_TRIAL_ID = "cedar-gym-trial-registration";
    private static final double MAX_INTERACTION_DISTANCE_SQUARED = 25.0D;

    private FabricGymLeagueRegistrationRuntime() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient() || hand != Hand.MAIN_HAND || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }
            BlockPos deskPos = hitResult.getBlockPos();
            if (!isRegistrationDesk(world, deskPos)) return ActionResult.PASS;
            if (!withinInteractionDistance(serverPlayer, deskPos)) {
                serverPlayer.sendMessage(Text.literal("You are too far away to use this Ouros registration desk."), false);
                return ActionResult.FAIL;
            }
            String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(serverPlayer.getUuid());
            if (FabricCanonicalPlayerStoreRuntime.requireRepository(serverPlayer.getServer()).findPlayer(playerId).isEmpty()) {
                serverPlayer.sendMessage(Text.literal("Canonical Trainer state is not loaded."), false);
                return ActionResult.FAIL;
            }
            serverPlayer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inventory, ignored) -> new RegistrationScreenHandler(syncId, inventory, serverPlayer, deskPos),
                    Text.literal("Cedar Gym & League Registration")
            ));
            return ActionResult.SUCCESS;
        });
    }

    static boolean isRegistrationDesk(World world, BlockPos pos) {
        return world.getBlockState(pos).isOf(FabricRpgContent.GYM_LEAGUE_REGISTRATION_DESK);
    }

    static boolean withinInteractionDistance(ServerPlayerEntity player, BlockPos pos) {
        return player.squaredDistanceTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D)
                <= MAX_INTERACTION_DISTANCE_SQUARED;
    }

    private static final class RegistrationScreenHandler extends GenericContainerScreenHandler {
        private static final int TOP_SLOT_COUNT = 27;
        private static final int INFO_SLOT = 4;
        private static final int REGISTER_SLOT = 13;
        private final SimpleInventory displayInventory;
        private final ServerPlayerEntity player;
        private final BlockPos deskPos;
        private final String playerId;
        private String statusText;

        RegistrationScreenHandler(int syncId, PlayerInventory inventory, ServerPlayerEntity player, BlockPos deskPos) {
            this(syncId, inventory, player, deskPos, new SimpleInventory(TOP_SLOT_COUNT));
        }

        private RegistrationScreenHandler(int syncId, PlayerInventory inventory, ServerPlayerEntity player, BlockPos deskPos, SimpleInventory displayInventory) {
            super(ScreenHandlerType.GENERIC_9X3, syncId, inventory, displayInventory, 3);
            this.displayInventory = displayInventory;
            this.player = player;
            this.deskPos = deskPos.toImmutable();
            this.playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
            this.statusText = registrationService().inspect(playerId).registrations().stream()
                    .anyMatch(registration -> registration.challengeId().equals(CEDAR_GYM_TRIAL_ID))
                    ? "Cedar Gym Trial registration is already persisted for this Trainer."
                    : "Select the authored Cedar Gym trial registration.";
            refresh();
        }

        @Override
        public boolean canUse(PlayerEntity candidate) {
            return candidate == player
                    && !candidate.isRemoved()
                    && isRegistrationDesk(candidate.getWorld(), deskPos)
                    && candidate.squaredDistanceTo(deskPos.getX() + 0.5D, deskPos.getY() + 0.5D, deskPos.getZ() + 0.5D)
                    <= MAX_INTERACTION_DISTANCE_SQUARED;
        }

        @Override
        public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity clickingPlayer) {
            if (clickingPlayer != player || actionType != SlotActionType.PICKUP || button != 0 || slotIndex != REGISTER_SLOT) return;
            if (!canUse(clickingPlayer)) {
                player.closeHandledScreen();
                return;
            }

            var readiness = new CanonicalTrainerChallengeRequestService(
                    CanonicalTrainerChallengeCatalogue.DEFAULT,
                    FabricCanonicalPlayerStoreRuntime.requireRepository(player.getServer()),
                    FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(player.getServer())
            ).request(playerId, DESK_PROVIDER_ID, CEDAR_GYM_TRIAL_ID);
            if (!readiness.accepted()) {
                statusText = "Registration unavailable: " + readiness.detail();
                refresh();
                return;
            }

            var registration = registrationService().register(playerId, CEDAR_GYM_TRIAL_ID);
            statusText = registration.newlyRegistered()
                    ? "Registered: " + registration.challenge().displayName() + ". This persists after reconnect/restart. AutoPTU must authorize the battle handoff."
                    : "Already registered: " + registration.challenge().displayName() + ". AutoPTU must authorize the battle handoff.";
            refresh();
        }

        @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }

        private CanonicalLeagueRegistrationService registrationService() {
            Path root = player.getServer().getSavePath(WorldSavePath.ROOT)
                    .resolve("autoptu")
                    .resolve("canonical-state")
                    .normalize();
            return new CanonicalLeagueRegistrationService(
                    CanonicalLeagueChallengeCatalogue.DEFAULT,
                    CanonicalTrainerChallengeCatalogue.DEFAULT,
                    FabricCanonicalPlayerStoreRuntime.requireRepository(player.getServer()),
                    new FileCanonicalLeagueRegistrationRepository(root),
                    new FileCanonicalTrainerRecordRepository(root));
        }

        private void refresh() {
            for (int i = 0; i < TOP_SLOT_COUNT; i++) displayInventory.setStack(i, ItemStack.EMPTY);
            displayInventory.setStack(INFO_SLOT, menuItem(Items.BOOK.getDefaultStack(), statusText));
            var challenge = CanonicalTrainerChallengeCatalogue.DEFAULT.challenge(CEDAR_GYM_TRIAL_ID).orElseThrow();
            displayInventory.setStack(REGISTER_SLOT, menuItem(Items.IRON_SWORD.getDefaultStack(),
                    "Register: " + challenge.displayName()));
            displayInventory.markDirty();
            sendContentUpdates();
        }

        private static ItemStack menuItem(ItemStack stack, String text) {
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(text));
            return stack;
        }
    }
}
