package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.fabric.battle.PersistentWorldEncounterPartyHandoffService;
import io.autoptu.cobblemon.fabric.battle.WorldEncounterTriggerRequestRepository;
import io.autoptu.cobblemon.fabric.battle.WorldEncounterTriggerRequestService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import java.util.IdentityHashMap;
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
    private static final Map<String, UUID> ENTITY_BY_ENCOUNTER = new ConcurrentHashMap<>();
    private static final Map<MinecraftServer, PersistentWorldEncounterPartyHandoffService> HANDOFFS =
            new IdentityHashMap<>();

    private VisibleWildPokemonEncounterRuntime() {}

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() || hand != Hand.MAIN_HAND || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }
            if (!(entity instanceof PokemonEntity)) return ActionResult.PASS;

            Binding binding = BINDINGS.get(entity.getUuid());
            if (binding == null) return ActionResult.PASS;
            if (serverPlayer.squaredDistanceTo(entity) > MAX_INTERACTION_DISTANCE_SQUARED) return ActionResult.FAIL;

            var blueprintRegistry = FabricCanonicalPlayerStoreRuntime
                    .requireWildEncounterBlueprintRegistry(serverPlayer.getServer());
            if (blueprintRegistry.resolve(binding.canonicalEncounterId()).isEmpty()) {
                serverPlayer.sendMessage(Text.literal("That wild encounter is not ready."), true);
                return ActionResult.FAIL;
            }

            String canonicalPlayerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(serverPlayer.getUuid());
            String dimensionId = serverPlayer.getServerWorld().getRegistryKey().getValue().toString();
            WorldEncounterTriggerRequestService.Decision decision = REQUESTS.requestBoundEncounter(
                    binding.canonicalEncounterId(), canonicalPlayerId, entity.getUuidAsString(),
                    binding.zoneId(), binding.contextId(), dimensionId,
                    entity.getBlockX(), entity.getBlockY(), entity.getBlockZ(), serverPlayer.getServer().getTicks());

            PersistentWorldEncounterPartyHandoffService.Decision handoff =
                    handoffService(serverPlayer.getServer(), blueprintRegistry).reserve(decision.request());
            if (!handoff.ready() || handoff.reservation() == null
                    || !handoff.reservation().canonicalEncounterId().equals(decision.request().canonicalEncounterId())) {
                if (decision.outcome() == WorldEncounterTriggerRequestService.Outcome.CREATED) {
                    REQUESTS.clearForPlayer(canonicalPlayerId);
                }
                serverPlayer.sendMessage(Text.literal("Your party cannot enter this encounter yet."), true);
                return ActionResult.FAIL;
            }

            serverPlayer.sendMessage(Text.literal(
                    decision.outcome() == WorldEncounterTriggerRequestService.Outcome.CREATED
                            ? "Your party is locked in for the wild encounter."
                            : "Your pending wild encounter remains locked to the same party handoff."), true);
            return ActionResult.SUCCESS;
        });
    }

    private static PersistentWorldEncounterPartyHandoffService handoffService(
            MinecraftServer server,
            io.autoptu.cobblemon.fabric.battle.CanonicalWildEncounterBlueprintSource blueprintSource) {
        Objects.requireNonNull(server, "server");
        synchronized (HANDOFFS) {
            return HANDOFFS.computeIfAbsent(server, ignored -> new PersistentWorldEncounterPartyHandoffService(
                    FabricCanonicalPlayerStoreRuntime.requireRepository(server),
                    FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(server), blueprintSource));
        }
    }

    public static Optional<io.autoptu.cobblemon.fabric.battle.WorldEncounterPartyHandoffService.Reservation>
    handoffForPlayer(MinecraftServer server, String canonicalPlayerId) {
        if (server == null || canonicalPlayerId == null || canonicalPlayerId.isBlank()) return Optional.empty();
        synchronized (HANDOFFS) {
            PersistentWorldEncounterPartyHandoffService service = HANDOFFS.get(server);
            return service == null ? Optional.empty() : service.findByPlayerId(canonicalPlayerId);
        }
    }

    public static void clearServerHandoffs(MinecraftServer server) {
        if (server == null) return;
        synchronized (HANDOFFS) { HANDOFFS.remove(server); }
    }

    /**
     * Clears process-local presentation identity at a Minecraft server/save lifecycle boundary.
     * Canonical encounter requests and RPG persistence live in world-scoped repositories; these
     * UUID bindings must never leak from one integrated-server save into another in the same JVM.
     */
    public static void clearServerPresentationBindings() {
        BINDINGS.clear();
        ENTITY_BY_ENCOUNTER.clear();
    }

    public static void bindRequestRepository(WorldEncounterTriggerRequestRepository repository) {
        REQUESTS.useRepository(Objects.requireNonNull(repository, "repository"));
    }

    public static void resetRequestRepository() { REQUESTS.resetRepository(); }

    public static void bind(PokemonEntity presentationEntity, String canonicalEncounterId, String zoneId, String contextId) {
        if (presentationEntity == null) throw new IllegalArgumentException("presentationEntity is required");
        String encounterId = requireId(canonicalEncounterId, "canonicalEncounterId");
        Binding binding = new Binding(encounterId, requireId(zoneId, "zoneId"), requireId(contextId, "contextId"));
        UUID currentUuid = presentationEntity.getUuid();
        UUID previousUuid = ENTITY_BY_ENCOUNTER.put(encounterId, currentUuid);
        if (previousUuid != null && !previousUuid.equals(currentUuid)) BINDINGS.remove(previousUuid);
        BINDINGS.put(currentUuid, binding);
    }

    public static boolean unbind(UUID entityUuid) {
        if (entityUuid == null) return false;
        Binding removed = BINDINGS.remove(entityUuid);
        if (removed == null) return false;
        ENTITY_BY_ENCOUNTER.remove(removed.canonicalEncounterId(), entityUuid);
        return true;
    }

    public static WorldEncounterTriggerRequestService requests() { return REQUESTS; }

    static boolean isBound(UUID entityUuid) { return entityUuid != null && BINDINGS.containsKey(entityUuid); }

    static Optional<UUID> boundEntityUuid(String canonicalEncounterId) {
        if (canonicalEncounterId == null || canonicalEncounterId.isBlank()) return Optional.empty();
        return Optional.ofNullable(ENTITY_BY_ENCOUNTER.get(canonicalEncounterId.strip()));
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
