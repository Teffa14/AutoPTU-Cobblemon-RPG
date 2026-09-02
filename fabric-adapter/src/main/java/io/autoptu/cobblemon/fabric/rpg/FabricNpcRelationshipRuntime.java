package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalNpcDialogueCatalogue;
import io.autoptu.cobblemon.authority.CanonicalNpcRelationshipService;
import io.autoptu.cobblemon.authority.CanonicalQuestObjectiveCatalogue;
import io.autoptu.cobblemon.authority.CanonicalQuestObjectiveService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

/** Records durable RPG relationship contact and authored quest progress from the normal physical NPC interaction path. */
public final class FabricNpcRelationshipRuntime {
    private FabricNpcRelationshipRuntime() {}

    public static void register() {
        FabricFactionReputationRuntime.register();
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() || hand != Hand.MAIN_HAND || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }
            var npcId = FabricNpcDialogueRuntime.npcId(entity).orElse(null);
            if (npcId == null) return ActionResult.PASS;

            String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(serverPlayer.getUuid());
            if (FabricCanonicalPlayerStoreRuntime.requireRepository(serverPlayer.getServer()).findPlayer(playerId).isEmpty()) {
                return ActionResult.PASS;
            }

            var result = new CanonicalNpcRelationshipService(
                    CanonicalNpcDialogueCatalogue.DEFAULT,
                    FabricCanonicalPlayerStoreRuntime.requireNpcRelationshipRepository(serverPlayer.getServer())
            ).observeContact(playerId, npcId);

            if (result.newlyMet()) {
                String name = CanonicalNpcDialogueCatalogue.DEFAULT.dialogue(npcId)
                        .map(CanonicalNpcDialogueCatalogue.Dialogue::displayName)
                        .orElse(npcId);
                serverPlayer.sendMessage(Text.literal(
                        "Relationship established: " + name + " — reputation " + result.relationship().reputation()), false);
            }

            var objectiveEvent = new CanonicalQuestObjectiveService(
                    CanonicalQuestObjectiveCatalogue.DEFAULT,
                    FabricCanonicalPlayerStoreRuntime.requireQuestJournalRepository(serverPlayer.getServer()),
                    FabricCanonicalPlayerStoreRuntime.requireQuestObjectiveRepository(serverPlayer.getServer())
            ).observe(playerId, CanonicalQuestObjectiveCatalogue.npcTalkedEvent(npcId));
            if (objectiveEvent.changed()) {
                String name = CanonicalNpcDialogueCatalogue.DEFAULT.dialogue(npcId)
                        .map(CanonicalNpcDialogueCatalogue.Dialogue::displayName)
                        .orElse(npcId);
                serverPlayer.sendMessage(Text.literal("Quest updated: you spoke with " + name + "."), false);
            }
            return ActionResult.PASS;
        });
    }
}
