package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalWorldEventObjectService;
import io.autoptu.cobblemon.authority.CanonicalWorldInteractionService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;

/**
 * Normal-world interaction gate for explicitly authored Ouros objects.
 *
 * A gold block directly below the interaction footprint marks the object as authored. Ordinary
 * Minecraft chests, switches, doors, lecterns and respawn anchors remain vanilla when no marker is
 * present. Authored objects require an authenticated canonical Trainer and range revalidation.
 */
public final class FabricCanonicalWorldInteractionRuntime {
    private static final double MAX_DISTANCE_SQUARED = 25.0D;
    private static final CanonicalWorldInteractionService SERVICE =
            new CanonicalWorldInteractionService(MAX_DISTANCE_SQUARED);

    private FabricCanonicalWorldInteractionRuntime() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient() || hand != Hand.MAIN_HAND || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }

            Optional<AuthoredObject> authored = authoredObject(world, hitResult.getBlockPos());
            if (authored.isEmpty()) return ActionResult.PASS;

            AuthoredObject object = authored.orElseThrow();
            String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(serverPlayer.getUuid());
            boolean trainerExists = FabricCanonicalPlayerStoreRuntime.requireRepository(serverPlayer.getServer())
                    .findPlayer(playerId)
                    .isPresent();
            CanonicalWorldInteractionService.Decision decision = SERVICE.canInteract(
                    new CanonicalWorldInteractionService.Request(
                            playerId,
                            trainerExists,
                            object.objectId(),
                            object.kind(),
                            object.kind(),
                            serverPlayer.squaredDistanceTo(
                                    object.anchor().getX() + 0.5D,
                                    object.anchor().getY() + 0.5D,
                                    object.anchor().getZ() + 0.5D
                            )
                    )
            );
            if (!decision.allowed()) {
                serverPlayer.sendMessage(Text.literal("Ouros interaction denied: " + decision.reason()), true);
                return ActionResult.FAIL;
            }

            if (object.kind() == CanonicalWorldInteractionService.Kind.TERMINAL) {
                serverPlayer.sendMessage(Text.literal("Ouros terminal authenticated: " + object.objectId()), false);
                return ActionResult.SUCCESS;
            }
            if (object.kind() == CanonicalWorldInteractionService.Kind.SHRINE) {
                var eventService = new CanonicalWorldEventObjectService(
                        FabricCanonicalPlayerStoreRuntime.requireRepository(serverPlayer.getServer()),
                        FabricCanonicalPlayerStoreRuntime.requireWorldEventObjectRepository(serverPlayer.getServer())
                );
                var event = eventService.activateShrine(playerId, object.objectId());
                if (!event.allowed()) {
                    serverPlayer.sendMessage(Text.literal("Ouros shrine denied: " + event.detail()), true);
                    return ActionResult.FAIL;
                }
                if (event.newlyActivated()) {
                    serverPlayer.sendMessage(Text.literal("The Ouros shrine awakens. Its world state is now persistent."), false);
                } else {
                    serverPlayer.sendMessage(Text.literal("The Ouros shrine is already awake."), false);
                }
                return ActionResult.SUCCESS;
            }

            // Chest, switch and door behavior remains Minecraft-native after server authorization.
            return ActionResult.PASS;
        });
    }

    static Optional<AuthoredObject> authoredObject(World world, BlockPos clicked) {
        BlockState state = world.getBlockState(clicked);
        CanonicalWorldInteractionService.Kind kind = kindOf(state.getBlock());
        if (kind == null) return Optional.empty();

        BlockPos marker = markerFor(world, clicked, kind);
        if (marker == null) return Optional.empty();
        String objectId = world.getRegistryKey().getValue() + ":" + marker.getX() + ":" + marker.getY() + ":" + marker.getZ();
        return Optional.of(new AuthoredObject(objectId, kind, marker.up()));
    }

    private static BlockPos markerFor(World world, BlockPos clicked, CanonicalWorldInteractionService.Kind kind) {
        if (world.getBlockState(clicked.down()).isOf(Blocks.GOLD_BLOCK)) return clicked.down();
        if (kind == CanonicalWorldInteractionService.Kind.DOOR
                && world.getBlockState(clicked.down(2)).isOf(Blocks.GOLD_BLOCK)) {
            return clicked.down(2);
        }
        return null;
    }

    private static CanonicalWorldInteractionService.Kind kindOf(Block block) {
        if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST) return CanonicalWorldInteractionService.Kind.CHEST;
        if (block == Blocks.LEVER || block == Blocks.STONE_BUTTON || block == Blocks.OAK_BUTTON) {
            return CanonicalWorldInteractionService.Kind.SWITCH;
        }
        if (block == Blocks.OAK_DOOR || block == Blocks.IRON_DOOR) return CanonicalWorldInteractionService.Kind.DOOR;
        if (block == Blocks.LECTERN) return CanonicalWorldInteractionService.Kind.TERMINAL;
        if (block == Blocks.RESPAWN_ANCHOR) return CanonicalWorldInteractionService.Kind.SHRINE;
        return null;
    }

    record AuthoredObject(
            String objectId,
            CanonicalWorldInteractionService.Kind kind,
            BlockPos anchor
    ) {}
}
