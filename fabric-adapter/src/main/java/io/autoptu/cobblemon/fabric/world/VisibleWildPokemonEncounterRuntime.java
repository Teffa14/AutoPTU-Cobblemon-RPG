package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.fabric.battle.WorldEncounterTriggerRequestService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Visible-wild encounter surface.
 *
 * Cobblemon's PokemonEntity is used only as a rendered/walking Minecraft actor. AutoPTU owns the
 * binding from that actor UUID to canonical world context. This runtime never reads PokemonEntity's
 * Pokemon payload, species, HP, moves, statuses, battle state, ownership or any Cobblemon result.
 */
public final class VisibleWildPokemonEncounterRuntime {
    private static final double MAX_INTERACTION_DISTANCE_SQUARED = 36.0D;
    private static final WorldEncounterTriggerRequestService REQUESTS = new WorldEncounterTriggerRequestService();
    private static final Map<UUID, Binding> BINDINGS = new ConcurrentHashMap<>();

    private VisibleWildPokemonEncounterRuntime() {}

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() || hand != Hand.MAIN_HAND || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }
            if (!(entity instanceof PokemonEntity)) {
                return ActionResult.PASS;
            }

            Binding binding = BINDINGS.get(entity.getUuid());
            if (binding == null) {
                return ActionResult.PASS;
            }
            if (serverPlayer.squaredDistanceTo(entity) > MAX_INTERACTION_DISTANCE_SQUARED) {
                return ActionResult.FAIL;
            }

            String canonicalPlayerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(serverPlayer.getUuid());
            String dimensionId = serverPlayer.getServerWorld().getRegistryKey().getValue().toString();
            WorldEncounterTriggerRequestService.Decision decision = REQUESTS.request(
                    canonicalPlayerId,
                    binding.zoneId(),
                    binding.contextId(),
                    dimensionId,
                    entity.getBlockX(),
                    entity.getBlockY(),
                    entity.getBlockZ(),
                    serverPlayer.getServer().getTicks()
            );

            if (decision.outcome() == WorldEncounterTriggerRequestService.Outcome.CREATED) {
                serverPlayer.sendMessage(Text.literal("You approach the wild Pokemon."), true);
            }

            // Consume registered wild-actor interaction so Cobblemon gameplay logic cannot become
            // encounter or battle authority for this actor.
            return ActionResult.SUCCESS;
        });
    }

    public static void bind(PokemonEntity presentationEntity, String zoneId, String contextId) {
        if (presentationEntity == null) throw new IllegalArgumentException("presentationEntity is required");
        BINDINGS.put(presentationEntity.getUuid(), new Binding(requireId(zoneId, "zoneId"), requireId(contextId, "contextId")));
    }

    public static boolean unbind(UUID entityUuid) {
        return entityUuid != null && BINDINGS.remove(entityUuid) != null;
    }

    public static WorldEncounterTriggerRequestService requests() {
        return REQUESTS;
    }

    static boolean isBound(UUID entityUuid) {
        return entityUuid != null && BINDINGS.containsKey(entityUuid);
    }

    private static String requireId(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }

    private record Binding(String zoneId, String contextId) {}
}
