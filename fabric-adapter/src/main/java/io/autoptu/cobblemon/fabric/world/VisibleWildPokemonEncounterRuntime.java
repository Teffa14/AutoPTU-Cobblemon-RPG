package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.fabric.battle.WorldEncounterTriggerRequestRepository;
import io.autoptu.cobblemon.fabric.battle.WorldEncounterTriggerRequestService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Visible-wild encounter surface.
 *
 * Cobblemon's PokemonEntity is used only as a rendered/walking Minecraft actor. AutoPTU owns the
 * binding from that actor UUID to canonical encounter identity and world context. This runtime never
 * reads PokemonEntity's Pokemon payload, species, HP, moves, statuses, battle state, ownership or any
 * Cobblemon result.
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
            WorldEncounterTriggerRequestService.Decision decision = REQUESTS.requestBoundEncounter(
                    binding.canonicalEncounterId(),
                    canonicalPlayerId,
                    entity.getUuidAsString(),
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
            } else {
                serverPlayer.sendMessage(Text.literal("Your pending wild encounter is still reserved."), true);
            }

            // Consume registered wild-actor interaction so Cobblemon gameplay logic cannot become
            // encounter or battle authority for this actor.
            return ActionResult.SUCCESS;
        });
    }

    /** Binds the long-lived encounter facade to the current server world's durable session store. */
    public static void bindRequestRepository(WorldEncounterTriggerRequestRepository repository) {
        REQUESTS.useRepository(Objects.requireNonNull(repository, "repository"));
    }

    /** Restores an empty in-memory boundary after the owning server lifecycle ends. */
    public static void resetRequestRepository() {
        REQUESTS.useRepository(new WorldEncounterTriggerRequestRepository() {
            private WorldEncounterTriggerRequestService.Request pending;

            @Override
            public synchronized Optional<WorldEncounterTriggerRequestService.Request> findPending(String canonicalPlayerId) {
                if (canonicalPlayerId == null || canonicalPlayerId.isBlank() || pending == null) return Optional.empty();
                return pending.canonicalPlayerId().equals(canonicalPlayerId.strip()) ? Optional.of(pending) : Optional.empty();
            }

            @Override
            public synchronized boolean saveIfAbsent(WorldEncounterTriggerRequestService.Request request) {
                if (pending != null) return false;
                pending = Objects.requireNonNull(request, "request");
                return true;
            }

            @Override
            public synchronized boolean clear(String canonicalPlayerId) {
                if (pending == null || canonicalPlayerId == null || canonicalPlayerId.isBlank()) return false;
                if (!pending.canonicalPlayerId().equals(canonicalPlayerId.strip())) return false;
                pending = null;
                return true;
            }
        });
    }

    public static void bind(
            PokemonEntity presentationEntity,
            String canonicalEncounterId,
            String zoneId,
            String contextId
    ) {
        if (presentationEntity == null) throw new IllegalArgumentException("presentationEntity is required");
        BINDINGS.put(
                presentationEntity.getUuid(),
                new Binding(
                        requireId(canonicalEncounterId, "canonicalEncounterId"),
                        requireId(zoneId, "zoneId"),
                        requireId(contextId, "contextId")
                )
        );
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

    static Optional<Binding> binding(UUID entityUuid) {
        if (entityUuid == null) return Optional.empty();
        return Optional.ofNullable(BINDINGS.get(entityUuid));
    }

    private static String requireId(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }

    record Binding(String canonicalEncounterId, String zoneId, String contextId) {}
}
