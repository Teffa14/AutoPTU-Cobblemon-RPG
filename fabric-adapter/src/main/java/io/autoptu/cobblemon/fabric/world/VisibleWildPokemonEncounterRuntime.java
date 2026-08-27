package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.fabric.battle.WorldEncounterTriggerRequestService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Visible-wild encounter surface.
 *
 * Cobblemon's PokemonEntity is only the rendered/walking Minecraft actor. AutoPTU owns its canonical
 * encounter identity, visible species identity and world context. Registered interactions never use
 * Cobblemon Pokemon gameplay payload as RPG or battle truth.
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
                    binding.canonicalWildSpeciesId(),
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

            // Registered actors never fall through into Cobblemon's normal entity-use gameplay path.
            return ActionResult.SUCCESS;
        });

        // Capture remains blocked until AutoPTU owns capture legality, RNG and result. The only
        // Cobblemon value inspected here is the presentation entity UUID used to find our binding.
        CobblemonEvents.THROWN_POKEBALL_HIT.subscribe(event -> {
            if (isBound(event.getPokemon().getUuid())) {
                event.cancel();
            }
        });
    }

    public static void bind(
            PokemonEntity presentationEntity,
            String canonicalEncounterId,
            String canonicalWildSpeciesId,
            String zoneId,
            String contextId
    ) {
        if (presentationEntity == null) throw new IllegalArgumentException("presentationEntity is required");
        UUID entityUuid = presentationEntity.getUuid();
        Binding binding = new Binding(
                requireId(canonicalEncounterId, "canonicalEncounterId"),
                requireId(canonicalWildSpeciesId, "canonicalWildSpeciesId"),
                requireId(zoneId, "zoneId"),
                requireId(contextId, "contextId")
        );

        Binding existing = BINDINGS.putIfAbsent(entityUuid, binding);
        if (existing != null) {
            if (!existing.equals(binding)) {
                throw new IllegalStateException("visible wild presentation actor already has a different canonical binding");
            }
            return;
        }

        // Keep this boundary on Cobblemon's public entity API. Do not reach into private data-tracker
        // fields to falsify level or battleability. Native entity-use is consumed above, thrown-ball
        // capture is cancelled, and canonical encounter/battle authority remains outside Cobblemon.
        presentationEntity.setInvulnerable(true);
        presentationEntity.setDrops(null);

        // Cobblemon owns normal entity lifecycle. AutoPTU only releases UUID correlation when the
        // visual actor disappears; entity removal cannot delete canonical encounter state.
        presentationEntity.getRemovalObservable().subscribe(ignored -> BINDINGS.remove(entityUuid, binding));
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

    record Binding(String canonicalEncounterId, String canonicalWildSpeciesId, String zoneId, String contextId) {}
}
