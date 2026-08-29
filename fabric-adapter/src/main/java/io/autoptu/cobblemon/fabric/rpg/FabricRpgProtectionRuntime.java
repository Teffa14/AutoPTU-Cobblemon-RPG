package io.autoptu.cobblemon.fabric.rpg;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
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
import net.minecraft.util.math.BlockPos;

/**
 * Authority boundary between Minecraft simulation and Ouros RPG state.
 *
 * Pokemon entities and canonical NPCs are presentation actors, so Minecraft damage never decides
 * their HP or death. Normal Minecraft mining, building, logging, digging and world interaction stay
 * available. World mutation is denied only inside an explicitly registered battle/quest/script
 * protection scope whose terrain must remain stable for that interaction.
 */
public final class FabricRpgProtectionRuntime {
    private FabricRpgProtectionRuntime() {}

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !isProtectedRpgActor(entity));

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            var protection = FabricRpgWorldProtectionRegistry.protectionAt(world, pos);
            if (protection.isEmpty()) return true;
            if (player instanceof ServerPlayerEntity serverPlayer) {
                sendProtectedRegionFeedback(serverPlayer, protection.get().reason());
            }
            return false;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient()
                    || hand != Hand.MAIN_HAND
                    || !(player instanceof ServerPlayerEntity serverPlayer)
                    || !requestsDirectWorldMutation(serverPlayer.getStackInHand(hand))) {
                return ActionResult.PASS;
            }

            BlockPos clicked = hitResult.getBlockPos();
            BlockPos adjacent = clicked.offset(hitResult.getSide());
            var protection = FabricRpgWorldProtectionRegistry.protectionAt(world, clicked)
                    .or(() -> FabricRpgWorldProtectionRegistry.protectionAt(world, adjacent));
            if (protection.isEmpty()) return ActionResult.PASS;

            sendProtectedRegionFeedback(serverPlayer, protection.get().reason());
            return ActionResult.FAIL;
        });
    }

    static boolean isProtectedRpgActor(LivingEntity entity) {
        if (entity instanceof PokemonEntity) return true;
        return FabricNpcDialogueRuntime.npcId(entity).isPresent();
    }

    static boolean requestsDirectWorldMutation(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof BlockItem || stack.getItem() instanceof BucketItem) return true;
        return stack.isOf(Items.FLINT_AND_STEEL) || stack.isOf(Items.FIRE_CHARGE);
    }

    private static void sendProtectedRegionFeedback(ServerPlayerEntity player, String reason) {
        player.sendMessage(Text.literal("This area is temporarily protected: " + reason), true);
    }
}
