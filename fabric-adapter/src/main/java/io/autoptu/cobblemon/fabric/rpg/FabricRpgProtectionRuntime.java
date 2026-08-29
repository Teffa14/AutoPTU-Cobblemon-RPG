package io.autoptu.cobblemon.fabric.rpg;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

/**
 * Hard authority boundary between Minecraft simulation and Ouros RPG state.
 *
 * Pokemon entities and canonical NPCs are presentation actors. Vanilla/mod damage is never
 * allowed to decide their HP or death. Direct player world mutation is also denied for loaded
 * canonical Trainers; authored interactions and server-side world services remain the mutation
 * path. This intentionally behaves like an RPG/adventure world rather than survival mining.
 */
public final class FabricRpgProtectionRuntime {
    private static final Text WORLD_PROTECTED = Text.literal(
            "Ouros world state is protected. Use an authored interaction instead.");

    private FabricRpgProtectionRuntime() {}

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !isProtectedRpgActor(entity));

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayerEntity serverPlayer) || !hasCanonicalTrainer(serverPlayer)) {
                return true;
            }
            serverPlayer.sendMessage(WORLD_PROTECTED, true);
            return false;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient()
                    || hand != Hand.MAIN_HAND
                    || !(player instanceof ServerPlayerEntity serverPlayer)
                    || !hasCanonicalTrainer(serverPlayer)) {
                return ActionResult.PASS;
            }
            if (!requestsDirectWorldMutation(serverPlayer.getStackInHand(hand))) {
                return ActionResult.PASS;
            }
            serverPlayer.sendMessage(WORLD_PROTECTED, true);
            return ActionResult.FAIL;
        });
    }

    static boolean isProtectedRpgActor(LivingEntity entity) {
        if (entity instanceof PokemonEntity) {
            return true;
        }
        return FabricNpcDialogueRuntime.npcId(entity).isPresent();
    }

    static boolean requestsDirectWorldMutation(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof BlockItem || stack.getItem() instanceof BucketItem) return true;
        return stack.isOf(Items.FLINT_AND_STEEL) || stack.isOf(Items.FIRE_CHARGE);
    }

    static boolean hasCanonicalTrainer(ServerPlayerEntity player) {
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        return FabricCanonicalPlayerStoreRuntime.requireRepository(player.getServer()).findPlayer(playerId).isPresent();
    }
}
