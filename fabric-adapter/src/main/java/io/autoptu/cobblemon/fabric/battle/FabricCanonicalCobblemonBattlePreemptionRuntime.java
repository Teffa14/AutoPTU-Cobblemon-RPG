package io.autoptu.cobblemon.fabric.battle;

import com.cobblemon.mod.common.api.reactive.ObservableSubscription;
import io.autoptu.cobblemon.fabric.world.VisibleWildPokemonEncounterRuntime;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

/**
 * Production fail-closed bridge for AutoPTU-bound WILD actors.
 *
 * When Cobblemon tries to start a battle involving a visible WILD entity already owned by the
 * AutoPTU world runtime, this handler cancels Cobblemon at BATTLE_STARTED_PRE. Full PTU battle
 * bootstrap remains blocked until the canonical reservation carries every required AutoPTU-Java
 * input; the integration must not fall back to Cobblemon mechanics while that work is incomplete.
 */
public final class FabricCanonicalCobblemonBattlePreemptionRuntime {
    private static volatile MinecraftServer server;
    private static ObservableSubscription<?> subscription;
    private static boolean registered;

    private FabricCanonicalCobblemonBattlePreemptionRuntime() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        ServerLifecycleEvents.SERVER_STARTED.register(started -> server = started);
        ServerLifecycleEvents.SERVER_STOPPED.register(stopped -> {
            if (server == stopped) server = null;
        });
        subscription = CobblemonBattleStartInterceptor.subscribe(
                FabricCanonicalCobblemonBattlePreemptionRuntime::tryClaim);
    }

    static boolean tryClaim(CobblemonBattleStartInterceptor.BattleStartSignal signal) {
        MinecraftServer activeServer = server;
        if (activeServer == null || signal == null) return false;

        for (CobblemonBattleStartInterceptor.ParticipantIdentity playerIdentity : signal.participants()) {
            if (playerIdentity.kind() != CobblemonBattleStartInterceptor.ParticipantKind.PLAYER) continue;
            ServerPlayerEntity player = onlinePlayer(activeServer, playerIdentity.actorId());
            if (player == null) continue;

            for (CobblemonBattleStartInterceptor.ParticipantIdentity wildIdentity : signal.participants()) {
                if (wildIdentity.kind() != CobblemonBattleStartInterceptor.ParticipantKind.WILD) continue;
                if (wildIdentity.side() == playerIdentity.side() || wildIdentity.presentationEntityId() == null) continue;

                UUID presentationEntityId = parseUuid(wildIdentity.presentationEntityId());
                if (presentationEntityId == null || !VisibleWildPokemonEncounterRuntime.isBound(presentationEntityId)) {
                    continue;
                }

                player.sendMessage(Text.literal(
                        "AutoPTU claimed this wild encounter. Cobblemon battle startup was blocked while "
                                + "the canonical PTU battle handoff is completed."), true);
                return true;
            }
        }
        return false;
    }

    static boolean matchesBoundWild(
            CobblemonBattleStartInterceptor.BattleStartSignal signal,
            String playerActorId,
            String presentationEntityId
    ) {
        if (signal == null || playerActorId == null || presentationEntityId == null) return false;
        for (var player : signal.participants()) {
            if (player.kind() != CobblemonBattleStartInterceptor.ParticipantKind.PLAYER
                    || !playerActorId.equals(player.actorId())) continue;
            for (var wild : signal.participants()) {
                if (wild.kind() == CobblemonBattleStartInterceptor.ParticipantKind.WILD
                        && wild.side() != player.side()
                        && presentationEntityId.equals(wild.presentationEntityId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static ServerPlayerEntity onlinePlayer(MinecraftServer server, String actorId) {
        UUID uuid = parseUuid(actorId);
        return uuid == null ? null : server.getPlayerManager().getPlayer(uuid);
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return UUID.fromString(value.strip());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
