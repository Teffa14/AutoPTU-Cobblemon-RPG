package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalWorldInteractionService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;

import java.util.Optional;

/**
 * Server-authoritative fallback for the registered Ouros world object currently targeted by a player.
 *
 * <p>The command accepts no object id, position, kind or outcome from the client. The server raycasts
 * from the authenticated player, resolves the authored binding from the observed block, revalidates
 * the canonical Trainer and delegates permission to the same {@link CanonicalWorldInteractionService}
 * used by normal block interaction. Minecraft-native chest/switch/door behavior remains native; this
 * fallback reports authorization instead of fabricating a synthetic vanilla use action.
 */
public final class FabricTargetedInteractionCommandRuntime {
    private static final double MAX_TARGET_DISTANCE = 5.0D;
    private static final double MAX_DISTANCE_SQUARED = 25.0D;
    private static final CanonicalWorldInteractionService SERVICE =
            new CanonicalWorldInteractionService(MAX_DISTANCE_SQUARED);

    private FabricTargetedInteractionCommandRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("interact")
                                .executes(context -> interact(context.getSource())))));
    }

    private static int interact(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("World interaction must be requested by an authenticated player."));
            return 0;
        }

        var hit = player.raycast(MAX_TARGET_DISTANCE, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)) {
            player.sendMessage(Text.literal("Ouros interaction denied: no registered world object is targeted."), true);
            return 0;
        }

        Optional<FabricCanonicalWorldInteractionRuntime.AuthoredObject> authored =
                FabricCanonicalWorldInteractionRuntime.authoredObject(player.getServerWorld(), blockHit.getBlockPos());
        if (authored.isEmpty()) {
            player.sendMessage(Text.literal("Ouros interaction denied: targeted block is not a registered Ouros object."), true);
            return 0;
        }

        FabricCanonicalWorldInteractionRuntime.AuthoredObject object = authored.orElseThrow();
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        boolean trainerExists = FabricCanonicalPlayerStoreRuntime.requireRepository(player.getServer())
                .findPlayer(playerId)
                .isPresent();
        CanonicalWorldInteractionService.Decision decision = SERVICE.canInteract(
                new CanonicalWorldInteractionService.Request(
                        playerId,
                        trainerExists,
                        object.objectId(),
                        object.kind(),
                        object.kind(),
                        player.squaredDistanceTo(
                                object.anchor().getX() + 0.5D,
                                object.anchor().getY() + 0.5D,
                                object.anchor().getZ() + 0.5D
                        )
                )
        );
        if (!decision.allowed()) {
            player.sendMessage(Text.literal("Ouros interaction denied: " + decision.reason()), true);
            return 0;
        }

        if (object.kind() == CanonicalWorldInteractionService.Kind.TERMINAL) {
            player.sendMessage(Text.literal("Ouros terminal authenticated: " + object.objectId()), false);
            return 1;
        }
        if (object.kind() == CanonicalWorldInteractionService.Kind.SHRINE) {
            player.sendMessage(Text.literal("The Ouros shrine recognizes your Trainer profile."), false);
            return 1;
        }

        player.sendMessage(Text.literal(
                "Ouros interaction authorized: " + object.kind().name().toLowerCase() + " " + object.objectId()
                        + ". Use the targeted block normally for its Minecraft-native action."), false);
        return 1;
    }
}
