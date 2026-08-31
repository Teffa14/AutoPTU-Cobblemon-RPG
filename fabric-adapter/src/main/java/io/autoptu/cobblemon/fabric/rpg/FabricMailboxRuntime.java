package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalMailCatalogue;
import io.autoptu.cobblemon.authority.CanonicalMailService;
import io.autoptu.cobblemon.authority.FileCanonicalMailRepository;
import io.autoptu.cobblemon.authority.FileCanonicalWalletRepository;
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
import java.util.LinkedHashMap;
import java.util.Map;

/** Physical mailbox UI over the existing durable, server-authored canonical mail service. */
public final class FabricMailboxRuntime {
    private static final double MAX_INTERACTION_DISTANCE_SQUARED = 25.0D;
    private static final int TOP_SLOT_COUNT = 27;
    private static final int INFO_SLOT = 4;
    private static final int MESSAGE_START = 9;
    private static final int REWARD_START = 18;

    private FabricMailboxRuntime() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient() || hand != Hand.MAIN_HAND || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }
            BlockPos mailboxPos = hitResult.getBlockPos();
            if (!isMailbox(world, mailboxPos)) return ActionResult.PASS;
            if (!withinInteractionDistance(serverPlayer, mailboxPos)) {
                serverPlayer.sendMessage(Text.literal("You are too far away to use this Ouros mailbox."), false);
                return ActionResult.FAIL;
            }
            String playerId = playerId(serverPlayer);
            if (FabricCanonicalPlayerStoreRuntime.requireRepository(serverPlayer.getServer()).findPlayer(playerId).isEmpty()) {
                serverPlayer.sendMessage(Text.literal("Canonical Trainer state is not loaded."), false);
                return ActionResult.FAIL;
            }
            serverPlayer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inventory, ignored) -> new MailboxScreenHandler(syncId, inventory, serverPlayer, mailboxPos),
                    Text.literal("Ouros Mailbox")
            ));
            return ActionResult.SUCCESS;
        });
    }

    static boolean isMailbox(World world, BlockPos pos) {
        return world.getBlockState(pos).isOf(FabricRpgContent.OUROS_MAILBOX);
    }

    static boolean withinInteractionDistance(ServerPlayerEntity player, BlockPos pos) {
        return player.squaredDistanceTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D)
                <= MAX_INTERACTION_DISTANCE_SQUARED;
    }

    static final class MailboxScreenHandler extends GenericContainerScreenHandler {
        private final SimpleInventory displayInventory;
        private final ServerPlayerEntity player;
        private final BlockPos mailboxPos;
        private final String playerId;
        private final CanonicalMailService service;
        private final Map<Integer, String> messageSlots = new LinkedHashMap<>();
        private final Map<Integer, String> rewardSlots = new LinkedHashMap<>();

        MailboxScreenHandler(int syncId, PlayerInventory inventory, ServerPlayerEntity player, BlockPos mailboxPos) {
            this(syncId, inventory, player, mailboxPos, new SimpleInventory(TOP_SLOT_COUNT));
        }

        private MailboxScreenHandler(
                int syncId,
                PlayerInventory inventory,
                ServerPlayerEntity player,
                BlockPos mailboxPos,
                SimpleInventory displayInventory
        ) {
            super(ScreenHandlerType.GENERIC_9X3, syncId, inventory, displayInventory, 3);
            this.displayInventory = displayInventory;
            this.player = player;
            this.mailboxPos = mailboxPos.toImmutable();
            this.playerId = playerId(player);
            this.service = service(player);
            refresh();
        }

        @Override
        public boolean canUse(PlayerEntity player) {
            return player == this.player
                    && !player.isRemoved()
                    && isMailbox(player.getWorld(), mailboxPos)
                    && player.squaredDistanceTo(
                            mailboxPos.getX() + 0.5D,
                            mailboxPos.getY() + 0.5D,
                            mailboxPos.getZ() + 0.5D) <= MAX_INTERACTION_DISTANCE_SQUARED;
        }

        @Override
        public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity clickingPlayer) {
            if (clickingPlayer != player || actionType != SlotActionType.PICKUP || button != 0) return;
            if (!canUse(clickingPlayer)) {
                player.closeHandledScreen();
                return;
            }
            String messageId = messageSlots.get(slotIndex);
            if (messageId != null) {
                read(messageId);
                return;
            }
            String rewardId = rewardSlots.get(slotIndex);
            if (rewardId != null) claim(rewardId);
        }

        @Override
        public ItemStack quickMove(PlayerEntity player, int slot) {
            return ItemStack.EMPTY;
        }

        private void read(String mailId) {
            try {
                CanonicalMailService.ReadResult result = service.read(playerId, mailId);
                var message = result.message();
                player.sendMessage(Text.literal(message.subject() + " — from " + message.sender()), false);
                player.sendMessage(Text.literal(message.body()), false);
            } catch (IllegalArgumentException | IllegalStateException failed) {
                player.sendMessage(Text.literal("Mail state changed or could not be read safely. The mailbox was refreshed."), false);
            }
            refresh();
        }

        private void claim(String mailId) {
            try {
                CanonicalMailService.ClaimResult result = service.claimReward(playerId, mailId);
                switch (result.status()) {
                    case APPLIED, RECOVERED_AFTER_WALLET_COMMIT -> player.sendMessage(Text.literal(
                            "Mail reward claimed: " + result.message().rewardAmount() + " " + result.message().rewardCurrencyId()), false);
                    case ALREADY_CLAIMED -> player.sendMessage(Text.literal("Mail reward already claimed."), false);
                    case NO_REWARD -> player.sendMessage(Text.literal("This mail has no reward."), false);
                    case TRANSACTION_CONFLICT, RETRY_EXHAUSTED -> player.sendMessage(Text.literal(
                            "Mail reward could not be committed safely: " + result.status()), false);
                }
            } catch (IllegalArgumentException | IllegalStateException failed) {
                player.sendMessage(Text.literal("Mail reward could not be committed safely. Canonical state was preserved."), false);
            }
            refresh();
        }

        private void refresh() {
            messageSlots.clear();
            rewardSlots.clear();
            for (int i = 0; i < TOP_SLOT_COUNT; i++) displayInventory.setStack(i, ItemStack.EMPTY);

            CanonicalMailService.Inbox inbox = service.inspect(playerId);
            displayInventory.setStack(INFO_SLOT, named(
                    Items.BOOK.getDefaultStack(),
                    "Canonical inbox · messages " + inbox.messages().size() + " · revision " + inbox.revision()));

            int messageSlot = MESSAGE_START;
            int rewardSlot = REWARD_START;
            for (var message : inbox.messages()) {
                if (messageSlot >= REWARD_START) break;
                ItemStack letter = (message.read() ? Items.PAPER : Items.WRITABLE_BOOK).getDefaultStack();
                displayInventory.setStack(messageSlot, named(
                        letter,
                        (message.read() ? "READ " : "UNREAD ") + message.subject() + " · " + message.sender() + " · click to open"));
                messageSlots.put(messageSlot, message.mailId());

                if (message.hasReward() && !message.rewardClaimed() && rewardSlot < TOP_SLOT_COUNT) {
                    displayInventory.setStack(rewardSlot, named(
                            Items.EMERALD.getDefaultStack(),
                            "CLAIM " + message.rewardAmount() + " " + message.rewardCurrencyId() + " · " + message.subject()));
                    rewardSlots.put(rewardSlot, message.mailId());
                    rewardSlot++;
                }
                messageSlot++;
            }
            displayInventory.markDirty();
            sendContentUpdates();
        }

        private static ItemStack named(ItemStack stack, String name) {
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
            return stack;
        }
    }

    private static CanonicalMailService service(ServerPlayerEntity player) {
        Path root = player.getServer().getSavePath(WorldSavePath.ROOT)
                .resolve("autoptu")
                .resolve("canonical-state")
                .normalize();
        return new CanonicalMailService(
                CanonicalMailCatalogue.DEFAULT,
                new FileCanonicalMailRepository(root),
                new FileCanonicalWalletRepository(root));
    }

    private static String playerId(ServerPlayerEntity player) {
        return FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
    }
}
