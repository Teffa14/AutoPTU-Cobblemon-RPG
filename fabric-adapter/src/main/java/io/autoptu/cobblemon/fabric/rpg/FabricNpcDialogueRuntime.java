package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalNpcDialogueCatalogue;
import io.autoptu.cobblemon.authority.CanonicalQuestCatalogue;
import io.autoptu.cobblemon.authority.CanonicalQuestJournalService;
import io.autoptu.cobblemon.authority.CanonicalTrainerChallengeCatalogue;
import io.autoptu.cobblemon.authority.CanonicalTrainerChallengeRequestService;
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

/** Physical server-authored NPC dialogue surface. NPC entities provide identity/presentation only. */
public final class FabricNpcDialogueRuntime {
    static final String NPC_TAG_PREFIX = "autoptu:npc:";
    private static final double MAX_INTERACTION_DISTANCE_SQUARED = 25.0D;

    private FabricNpcDialogueRuntime() {}

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() || hand != Hand.MAIN_HAND || !(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            Optional<String> npcId = npcId(entity);
            if (npcId.isEmpty()) return ActionResult.PASS;
            var dialogue = CanonicalNpcDialogueCatalogue.DEFAULT.dialogue(npcId.get()).orElse(null);
            if (dialogue == null) return ActionResult.PASS;
            if (serverPlayer.squaredDistanceTo(entity) > MAX_INTERACTION_DISTANCE_SQUARED) {
                serverPlayer.sendMessage(Text.literal("You are too far away to talk to this Ouros NPC."), false);
                return ActionResult.FAIL;
            }
            String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(serverPlayer.getUuid());
            if (FabricCanonicalPlayerStoreRuntime.requireRepository(serverPlayer.getServer()).findPlayer(playerId).isEmpty()) {
                serverPlayer.sendMessage(Text.literal("Canonical Trainer state is not loaded."), false);
                return ActionResult.FAIL;
            }
            UUID entityId = entity.getUuid();
            serverPlayer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inventory, ignored) -> new DialogueScreenHandler(syncId, inventory, serverPlayer, entityId, dialogue),
                    Text.literal(dialogue.displayName())
            ));
            return ActionResult.SUCCESS;
        });
    }

    public static void bind(VillagerEntity entity, String npcId) {
        var dialogue = CanonicalNpcDialogueCatalogue.DEFAULT.dialogue(npcId)
                .orElseThrow(() -> new IllegalArgumentException("unknown canonical npcId: " + npcId));
        entity.addCommandTag(NPC_TAG_PREFIX + dialogue.npcId());
        entity.setCustomName(Text.literal(dialogue.displayName()));
        entity.setCustomNameVisible(true);
        entity.setPersistent();
    }

    static Optional<String> npcId(Entity entity) {
        if (!(entity instanceof VillagerEntity)) return Optional.empty();
        String found = null;
        for (String tag : entity.getCommandTags()) {
            if (!tag.startsWith(NPC_TAG_PREFIX)) continue;
            String candidate = tag.substring(NPC_TAG_PREFIX.length());
            if (CanonicalNpcDialogueCatalogue.DEFAULT.dialogue(candidate).isEmpty()) continue;
            if (found != null && !found.equals(candidate)) return Optional.empty();
            found = candidate;
        }
        return Optional.ofNullable(found);
    }

    private static final class DialogueScreenHandler extends GenericContainerScreenHandler {
        private static final int TOP_SLOT_COUNT = 27;
        private static final int TEXT_SLOT = 4;
        private static final int OPTION_START_SLOT = 10;
        private final SimpleInventory displayInventory;
        private final ServerPlayerEntity player;
        private final UUID npcEntityId;
        private final CanonicalNpcDialogueCatalogue.Dialogue dialogue;
        private final Map<Integer, String> optionIds = new HashMap<>();
        private String displayedText;

        DialogueScreenHandler(int syncId, PlayerInventory inventory, ServerPlayerEntity player, UUID npcEntityId, CanonicalNpcDialogueCatalogue.Dialogue dialogue) {
            this(syncId, inventory, player, npcEntityId, dialogue, new SimpleInventory(TOP_SLOT_COUNT));
        }

        private DialogueScreenHandler(int syncId, PlayerInventory inventory, ServerPlayerEntity player, UUID npcEntityId, CanonicalNpcDialogueCatalogue.Dialogue dialogue, SimpleInventory displayInventory) {
            super(ScreenHandlerType.GENERIC_9X3, syncId, inventory, displayInventory, 3);
            this.displayInventory = displayInventory;
            this.player = player;
            this.npcEntityId = npcEntityId;
            this.dialogue = dialogue;
            this.displayedText = dialogue.openingLine();
            refresh();
        }

        @Override
        public boolean canUse(PlayerEntity player) {
            if (player != this.player || player.isRemoved() || !(player.getWorld() instanceof ServerWorld serverWorld)) return false;
            Entity current = serverWorld.getEntity(npcEntityId);
            return current != null && !current.isRemoved() && npcId(current).filter(dialogue.npcId()::equals).isPresent()
                    && player.squaredDistanceTo(current) <= MAX_INTERACTION_DISTANCE_SQUARED;
        }

        @Override
        public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity clickingPlayer) {
            if (clickingPlayer != player || actionType != SlotActionType.PICKUP || button != 0) return;
            if (!canUse(clickingPlayer)) {
                player.closeHandledScreen();
                return;
            }
            String optionId = optionIds.get(slotIndex);
            if (optionId == null) return;
            CanonicalNpcDialogueCatalogue.Option option = dialogue.option(optionId).orElse(null);
            if (option == null) {
                refresh();
                return;
            }
            displayedText = option.response();
            String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
            if (option.questId() != null) {
                var service = new CanonicalQuestJournalService(
                        CanonicalQuestCatalogue.DEFAULT,
                        FabricCanonicalPlayerStoreRuntime.requireQuestJournalRepository(player.getServer())
                );
                var result = service.accept(playerId, dialogue.npcId(), option.questId());
                displayedText = result.newlyAccepted()
                        ? "Quest accepted: " + result.quest().title() + " — " + result.quest().objectiveText()
                        : "Quest already in journal: " + result.quest().title() + " — " + result.quest().objectiveText();
            } else if (option.challengeId() != null) {
                var service = new CanonicalTrainerChallengeRequestService(
                        CanonicalTrainerChallengeCatalogue.DEFAULT,
                        FabricCanonicalPlayerStoreRuntime.requireRepository(player.getServer()),
                        FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(player.getServer())
                );
                var result = service.request(playerId, dialogue.npcId(), option.challengeId());
                displayedText = result.accepted()
                        ? "Challenge ready: " + result.challenge().displayName() + ". AutoPTU must authorize any battle start and resolve every battle outcome."
                        : "Challenge unavailable: " + result.detail();
            }
            refresh();
        }

        @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }

        private void refresh() {
            optionIds.clear();
            for (int i = 0; i < TOP_SLOT_COUNT; i++) displayInventory.setStack(i, ItemStack.EMPTY);
            displayInventory.setStack(TEXT_SLOT, menuItem(Items.BOOK.getDefaultStack(), displayedText));
            int slot = OPTION_START_SLOT;
            for (CanonicalNpcDialogueCatalogue.Option option : dialogue.options()) {
                if (slot >= TOP_SLOT_COUNT) break;
                ItemStack icon = option.questId() != null
                        ? Items.WRITABLE_BOOK.getDefaultStack()
                        : option.challengeId() != null ? Items.IRON_SWORD.getDefaultStack() : Items.PAPER.getDefaultStack();
                displayInventory.setStack(slot, menuItem(icon, option.label()));
                optionIds.put(slot, option.optionId());
                slot++;
            }
            displayInventory.markDirty();
            sendContentUpdates();
        }

        private static ItemStack menuItem(ItemStack stack, String text) {
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(text));
            return stack;
        }
    }
}
