package io.autoptu.cobblemon.fabric.rpg;

import com.cobblemon.mod.common.CobblemonBlocks;
import com.cobblemon.mod.common.block.entity.HealingMachineBlockEntity;
import io.autoptu.cobblemon.authority.CanonicalPartyHealingDecision;
import io.autoptu.cobblemon.authority.CanonicalPartyHealingService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * AutoPTU canonical healing attached to Cobblemon's real Healing Machine lifecycle.
 *
 * Cobblemon owns the block, generated Pokecenter placement, charge, animation, battle checks and
 * whether the machine actually activates. AutoPTU observes a native machine use and commits durable
 * canonical HP only after Cobblemon completes that accepted healing cycle.
 */
public final class FabricHealingStationRuntime {
    private static final double MAX_INTERACTION_DISTANCE_SQUARED = 25.0D;
    private static final int ACTIVATION_CONFIRM_TICKS = 3;
    private static final int COMPLETION_TIMEOUT_TICKS = 200;
    private static final Map<UUID, PendingHealing> PENDING = new HashMap<>();

    private FabricHealingStationRuntime() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient() || hand != Hand.MAIN_HAND || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }

            BlockPos machinePos = hitResult.getBlockPos();
            if (!isCobblemonHealingMachine(world, machinePos)) {
                return ActionResult.PASS;
            }
            if (!withinInteractionDistance(serverPlayer, machinePos)) {
                return ActionResult.PASS;
            }

            // Record only the request. Cobblemon's own block handler runs normally after this callback.
            // A server tick later we verify that its HealingMachineBlockEntity actually accepted the player.
            PENDING.put(serverPlayer.getUuid(), new PendingHealing(
                    serverPlayer.getServerWorld(), machinePos.toImmutable(), 0, false
            ));
            return ActionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(FabricHealingStationRuntime::processPending);
    }

    static boolean isCobblemonHealingMachine(World world, BlockPos pos) {
        return world.getBlockState(pos).isOf(CobblemonBlocks.HEALING_MACHINE);
    }

    static boolean withinInteractionDistance(ServerPlayerEntity player, BlockPos machinePos) {
        double x = machinePos.getX() + 0.5D;
        double y = machinePos.getY() + 0.5D;
        double z = machinePos.getZ() + 0.5D;
        return player.squaredDistanceTo(x, y, z) <= MAX_INTERACTION_DISTANCE_SQUARED;
    }

    private static void processPending(MinecraftServer server) {
        Iterator<Map.Entry<UUID, PendingHealing>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingHealing> entry = iterator.next();
            UUID playerUuid = entry.getKey();
            PendingHealing pending = entry.getValue().nextTick();
            entry.setValue(pending);

            if (!isCobblemonHealingMachine(pending.world(), pending.machinePos())) {
                iterator.remove();
                continue;
            }
            if (!(pending.world().getBlockEntity(pending.machinePos()) instanceof HealingMachineBlockEntity machine)) {
                iterator.remove();
                continue;
            }

            UUID currentUser = machine.getCurrentUser();
            if (!pending.activationSeen()) {
                if (playerUuid.equals(currentUser)) {
                    entry.setValue(pending.withActivationSeen());
                    continue;
                }
                if (pending.ticks() >= ACTIVATION_CONFIRM_TICKS) {
                    // Cobblemon rejected or did not activate the machine. Do not mutate canonical state.
                    iterator.remove();
                }
                continue;
            }

            if (playerUuid.equals(currentUser)) {
                if (pending.ticks() >= COMPLETION_TIMEOUT_TICKS) iterator.remove();
                continue;
            }

            // The machine accepted this player and later released its current user. In Cobblemon's
            // lifecycle that is the completion boundary, after party.heal() has run.
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
            iterator.remove();
            if (player != null) healCanonicalParty(player);
        }
    }

    private static void healCanonicalParty(ServerPlayerEntity player) {
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        CanonicalPartyHealingService service = new CanonicalPartyHealingService(
                FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(player.getServer()),
                FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(player.getServer())
        );
        CanonicalPartyHealingDecision decision = service.healParty(playerId);
        sendFeedback(player, decision);
    }

    private static void sendFeedback(ServerPlayerEntity player, CanonicalPartyHealingDecision decision) {
        switch (decision.outcome()) {
            case APPLIED -> player.sendMessage(Text.literal(
                    "AutoPTU canonical healing complete: " + decision.healedPokemon() + " healed, "
                            + decision.alreadyFullPokemon() + " already at full HP. "
                            + "Statuses and injuries were left unchanged."), false);
            case PARTIAL -> {
                player.sendMessage(Text.literal(
                        "AutoPTU canonical healing partially completed: " + decision.healedPokemon() + " healed."), false);
                player.sendMessage(Text.literal(
                        "Canonical party members not healed safely: "
                                + String.join(", ", decision.failedPokemonIds())), false);
            }
            case NO_PARTY -> player.sendMessage(Text.literal(
                    "This Trainer has no persistent AutoPTU party to heal."), false);
            case INVALID_REQUEST -> player.sendMessage(Text.literal(
                    "AutoPTU rejected this canonical healing commit: " + decision.reason()), false);
        }
    }

    private record PendingHealing(ServerWorld world, BlockPos machinePos, int ticks, boolean activationSeen) {
        PendingHealing nextTick() {
            return new PendingHealing(world, machinePos, ticks + 1, activationSeen);
        }

        PendingHealing withActivationSeen() {
            return new PendingHealing(world, machinePos, ticks, true);
        }
    }
}
