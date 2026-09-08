package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import io.autoptu.cobblemon.authority.CanonicalWildEncounterCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWorldMapCatalogue;
import io.autoptu.cobblemon.fabric.battle.ServerOwnedWildEncounterBlueprintPublisher;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Global server-owned lifecycle for approved visible WILD populations.
 *
 * <p>Authored region modules register one content policy through {@link WildEcologyDescriptorRegistry}.
 * This runtime owns normal activation, hibernation, presence reconciliation, actor replacement,
 * canonical encounter binding and destructive-unload cleanup for every registered population.
 * Cobblemon entities remain presentation bodies only.</p>
 */
public final class WildPopulationRuntime {
    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");
    private static final String WILD_TAG_PREFIX = "autoptu:wild-encounter:";
    private static final String WILD_MARKER_TAG = "ouros:visible-wild";
    private static final int PRESENCE_RECONCILE_INTERVAL_TICKS = 100;
    private static final int HABITAT_SEARCH_RADIUS_BLOCKS = 48;
    private static final Map<MinecraftServer, Set<String>> ACTIVE_POPULATIONS = new IdentityHashMap<>();
    private static boolean registered;

    private WildPopulationRuntime() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!(entity instanceof PokemonEntity pokemonEntity)) return;
            var encounter = canonicalEncounterFor(pokemonEntity);
            if (encounter.isEmpty()) return;
            var source = WildEcologyDescriptorRegistry.descriptorFor(encounter.get()).orElse(null);
            if (source == null) return;
            if (!source.worldEligibility().accepts(world)) {
                VisibleWildPokemonEncounterRuntime.unbind(pokemonEntity.getUuid());
                pokemonEntity.discard();
                LOGGER.warn("AutoPTU rejected visible wild actor outside its authored world: encounter={} entity={} dimension={}",
                        encounter.get().canonicalEncounterId(), pokemonEntity.getUuid(), world.getRegistryKey().getValue());
                return;
            }
            if (!FabricCanonicalPlayerStoreRuntime.storesAvailable(world.getServer())) return;
            publishBeforeReveal(world, encounter.get().canonicalEncounterId(), source);
            bind(pokemonEntity, encounter.get());
            applyCurrentProjection(world, pokemonEntity, encounter.get(), source);
        });

        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (!(entity instanceof PokemonEntity pokemonEntity)) return;
            if (canonicalEncounterFor(pokemonEntity).isEmpty()) return;
            var reason = pokemonEntity.getRemovalReason();
            if (reason == null) return;
            if (reason.shouldDestroy() || reason == Entity.RemovalReason.CHANGED_DIMENSION) {
                VisibleWildPokemonEncounterRuntime.unbind(pokemonEntity.getUuid());
                LOGGER.info("AutoPTU visible wild actor released canonical presence: entity={} reason={}",
                        pokemonEntity.getUuid(), reason);
            }
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            clearPopulationActivity(server);
            int visible = reconcileAllWorlds(server);
            LOGGER.info("AutoPTU global visible wild populations ready: {} actors across {} ecology descriptors",
                    visible, WildEcologyDescriptorRegistry.descriptorCount());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(WildPopulationRuntime::clearPopulationActivity);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % PRESENCE_RECONCILE_INTERVAL_TICKS != 0) return;
            reconcileAllWorlds(server);
        });
    }

    public static int ensureProjected(ServerWorld world) {
        if (world == null) throw new IllegalArgumentException("world is required");
        int visible = 0;
        for (var population : CanonicalWildPopulationCatalogue.DEFAULT.populations()) {
            var source = WildEcologyDescriptorRegistry.descriptorFor(population).orElse(null);
            if (source == null || !source.worldEligibility().accepts(world)) continue;
            for (var encounter : CanonicalWildPopulationCatalogue.DEFAULT.members(population)) {
                if (ensureProjected(world, encounter) != null) visible++;
            }
        }
        return visible;
    }

    static PokemonEntity ensureProjected(
            ServerWorld world,
            CanonicalWildEncounterCatalogue.EncounterDefinition encounter
    ) {
        if (world == null) throw new IllegalArgumentException("world is required");
        if (encounter == null) throw new IllegalArgumentException("encounter is required");
        var source = WildEcologyDescriptorRegistry.descriptorFor(encounter)
                .orElseThrow(() -> new IllegalStateException(
                        "no registered visible wild ecology descriptor for " + encounter.populationId()));
        if (!source.worldEligibility().accepts(world)) {
            throw new IllegalArgumentException("wild population projection rejected this Minecraft world");
        }

        publishBeforeReveal(world, encounter.canonicalEncounterId(), source);
        String projectedSiteId = source.projectedSiteId(populationFor(encounter), world.getTime())
                .orElse(encounter.siteId());
        BlockPos anchor = projectedPresentationAnchor(encounter, projectedSiteId);
        loadProjectionAnchorChunk(world, anchor);
        PokemonEntity existing = findExisting(world, encounter.canonicalEncounterId(), anchor);
        if (existing != null) {
            bind(existing, encounter);
            keepInProjectedHabitat(existing, encounter, projectedSiteId);
            setPopulationProjectionActive(existing, true);
            return existing;
        }

        evictMissingBinding(encounter.canonicalEncounterId());
        enforceProjectionContentGate(encounter, source);
        Species species = PokemonSpecies.INSTANCE.getByName(encounter.speciesId());
        if (species == null) {
            throw new IllegalStateException("Cobblemon official species unavailable for visible wild actor: "
                    + encounter.speciesId());
        }

        Pokemon pokemon = new Pokemon();
        pokemon.setSpecies(species);
        PokemonEntity entity = new PokemonEntity(world, pokemon, CobblemonEntities.POKEMON);
        entity.refreshPositionAndAngles(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D, 180.0F, 0.0F);
        entity.setPersistent();
        entity.addCommandTag(WILD_TAG_PREFIX + encounter.canonicalEncounterId());
        entity.addCommandTag(WILD_MARKER_TAG);
        if (!world.spawnEntity(entity)) return null;
        bind(entity, encounter);
        setPopulationProjectionActive(entity, true);
        return entity;
    }

    static PokemonEntity actorForEncounter(ServerWorld world, String canonicalEncounterId) {
        if (world == null || canonicalEncounterId == null || canonicalEncounterId.isBlank()) return null;
        var encounter = CanonicalWildEncounterCatalogue.DEFAULT.encounter(canonicalEncounterId.strip()).orElse(null);
        if (encounter == null) return null;
        var source = WildEcologyDescriptorRegistry.descriptorFor(encounter).orElse(null);
        if (source == null || !source.worldEligibility().accepts(world)) return null;
        var boundUuid = VisibleWildPokemonEncounterRuntime.boundEntityUuid(encounter.canonicalEncounterId());
        if (boundUuid.isPresent()) {
            var loaded = world.getEntity(boundUuid.get());
            if (loaded instanceof PokemonEntity pokemonEntity && !pokemonEntity.isRemoved()) {
                applyCurrentProjection(world, pokemonEntity, encounter, source);
                return pokemonEntity;
            }
            return null;
        }
        PokemonEntity actor = ensureProjected(world, encounter);
        if (actor != null) applyCurrentProjection(world, actor, encounter, source);
        return actor;
    }

    static int presenceReconcileIntervalTicks() {
        return PRESENCE_RECONCILE_INTERVAL_TICKS;
    }

    static int reconcileActivePopulations(ServerWorld world) {
        if (world == null || world.getServer() == null) return 0;
        if (!FabricCanonicalPlayerStoreRuntime.storesAvailable(world.getServer())) return 0;
        int visible = 0;
        for (var population : CanonicalWildPopulationCatalogue.DEFAULT.populations()) {
            var source = WildEcologyDescriptorRegistry.descriptorFor(population).orElse(null);
            if (source == null || !source.worldEligibility().accepts(world)) continue;

            var projectedSiteId = source.projectedSiteId(population, world.getTime());
            if (projectedSiteId.isEmpty()) {
                setPopulationMarkedActive(world.getServer(), population.populationId(), false);
                hibernateLoadedPopulation(world, population);
                continue;
            }
            var projectedSite = CanonicalWorldMapCatalogue.DEFAULT.site(projectedSiteId.get())
                    .orElseThrow(() -> new IllegalStateException(
                            "missing projected canonical wild population site: " + projectedSiteId.get()));

            boolean wasActive = isPopulationMarkedActive(world.getServer(), population.populationId());
            boolean active = hasPlayerInsideFootprint(
                    world,
                    projectedSite,
                    wasActive ? population.retentionFootprint() : population.presenceFootprint()
            );
            setPopulationMarkedActive(world.getServer(), population.populationId(), active);
            if (!active) {
                hibernateLoadedPopulation(world, population);
                continue;
            }

            for (var encounter : CanonicalWildPopulationCatalogue.DEFAULT.members(population)) {
                PokemonEntity actor = WildVisibleActorRecovery.recoverBoundActor(world, encounter);
                if (actor == null) actor = ensureProjected(world, encounter);
                if (actor == null) continue;
                keepInProjectedHabitat(actor, encounter, projectedSiteId.get());
                setPopulationProjectionActive(actor, true);
                visible++;
            }
        }
        return visible;
    }

    private static int reconcileAllWorlds(MinecraftServer server) {
        if (server == null) return 0;
        int visible = 0;
        for (ServerWorld world : server.getWorlds()) visible += reconcileActivePopulations(world);
        return visible;
    }

    private static void applyCurrentProjection(
            ServerWorld world,
            PokemonEntity actor,
            CanonicalWildEncounterCatalogue.EncounterDefinition encounter,
            WildEcologyDescriptorRegistry.Descriptor source
    ) {
        var population = populationFor(encounter);
        var projectedSiteId = source.projectedSiteId(population, world.getTime());
        if (projectedSiteId.isEmpty()) {
            setPopulationMarkedActive(world.getServer(), population.populationId(), false);
            setPopulationProjectionActive(actor, false);
            return;
        }
        var projectedSite = CanonicalWorldMapCatalogue.DEFAULT.site(projectedSiteId.get())
                .orElseThrow(() -> new IllegalStateException(
                        "missing projected canonical wild population site: " + projectedSiteId.get()));
        boolean wasActive = isPopulationMarkedActive(world.getServer(), population.populationId());
        boolean active = hasPlayerInsideFootprint(
                world,
                projectedSite,
                wasActive ? population.retentionFootprint() : population.presenceFootprint()
        );
        setPopulationMarkedActive(world.getServer(), population.populationId(), active);
        if (active) keepInProjectedHabitat(actor, encounter, projectedSiteId.get());
        setPopulationProjectionActive(actor, active);
    }

    private static void hibernateLoadedPopulation(
            ServerWorld world,
            CanonicalWildPopulationCatalogue.PopulationDefinition population
    ) {
        for (var encounter : CanonicalWildPopulationCatalogue.DEFAULT.members(population)) {
            var boundUuid = VisibleWildPokemonEncounterRuntime.boundEntityUuid(encounter.canonicalEncounterId());
            if (boundUuid.isEmpty()) continue;
            var loaded = world.getEntity(boundUuid.get());
            if (loaded instanceof PokemonEntity pokemonEntity && !pokemonEntity.isRemoved()) {
                setPopulationProjectionActive(pokemonEntity, false);
            }
        }
    }

    private static void setPopulationProjectionActive(PokemonEntity entity, boolean active) {
        entity.setInvisible(!active);
        VisibleWildPokemonEncounterRuntime.setInteractionActive(entity.getUuid(), active);
    }

    private static CanonicalWildPopulationCatalogue.PopulationDefinition populationFor(
            CanonicalWildEncounterCatalogue.EncounterDefinition encounter
    ) {
        return CanonicalWildPopulationCatalogue.DEFAULT.population(encounter.populationId())
                .orElseThrow(() -> new IllegalStateException(
                        "missing canonical wild population policy: " + encounter.populationId()));
    }

    private static boolean hasPlayerInsideFootprint(
            ServerWorld world,
            CanonicalWorldMapCatalogue.Site site,
            CanonicalWildPopulationCatalogue.PresenceFootprint footprint
    ) {
        for (var player : world.getPlayers()) {
            if (player.isSpectator()) continue;
            double dx = player.getX() - (site.x() + 0.5D);
            double dy = player.getY() - site.y();
            double dz = player.getZ() - (site.z() + 0.5D);
            if (footprint.containsOffset(dx, dy, dz)) return true;
        }
        return false;
    }

    private static boolean isPopulationMarkedActive(MinecraftServer server, String populationId) {
        synchronized (ACTIVE_POPULATIONS) {
            var active = ACTIVE_POPULATIONS.get(server);
            return active != null && active.contains(populationId);
        }
    }

    private static void setPopulationMarkedActive(MinecraftServer server, String populationId, boolean active) {
        synchronized (ACTIVE_POPULATIONS) {
            Set<String> populations = ACTIVE_POPULATIONS.computeIfAbsent(server, ignored -> new HashSet<>());
            if (active) populations.add(populationId);
            else populations.remove(populationId);
            if (populations.isEmpty()) ACTIVE_POPULATIONS.remove(server);
        }
    }

    private static void clearPopulationActivity(MinecraftServer server) {
        synchronized (ACTIVE_POPULATIONS) {
            ACTIVE_POPULATIONS.remove(server);
        }
    }

    private static java.util.Optional<CanonicalWildEncounterCatalogue.EncounterDefinition> canonicalEncounterFor(
            PokemonEntity entity
    ) {
        if (entity == null || !entity.getCommandTags().contains(WILD_MARKER_TAG)) return java.util.Optional.empty();
        for (String tag : entity.getCommandTags()) {
            if (!tag.startsWith(WILD_TAG_PREFIX)) continue;
            String encounterId = tag.substring(WILD_TAG_PREFIX.length());
            var encounter = CanonicalWildEncounterCatalogue.DEFAULT.encounter(encounterId);
            if (encounter.isPresent() && WildEcologyDescriptorRegistry.descriptorFor(encounter.get()).isPresent()) {
                return encounter;
            }
        }
        return java.util.Optional.empty();
    }

    static void keepInProjectedHabitat(
            PokemonEntity entity,
            CanonicalWildEncounterCatalogue.EncounterDefinition encounter,
            String projectedSiteId
    ) {
        BlockPos anchor = projectedPresentationAnchor(encounter, projectedSiteId);
        int leashRadiusBlocks = populationFor(encounter).habitatLeashRadiusBlocks();
        double centerX = anchor.getX() + 0.5D;
        double centerZ = anchor.getZ() + 0.5D;
        double dx = entity.getX() - centerX;
        double dz = entity.getZ() - centerZ;
        if (dx * dx + dz * dz <= (double) leashRadiusBlocks * leashRadiusBlocks) return;
        entity.requestTeleport(centerX, anchor.getY(), centerZ);
    }

    static BlockPos projectedPresentationAnchor(
            CanonicalWildEncounterCatalogue.EncounterDefinition encounter,
            String projectedSiteId
    ) {
        if (encounter == null) throw new IllegalArgumentException("encounter is required");
        if (projectedSiteId == null || projectedSiteId.isBlank()) {
            throw new IllegalArgumentException("projectedSiteId is required");
        }
        var site = CanonicalWorldMapCatalogue.DEFAULT.site(projectedSiteId)
                .orElseThrow(() -> new IllegalStateException(
                        "missing projected canonical wild encounter site: " + projectedSiteId));
        return new BlockPos(site.x(), site.y(), site.z()).add(
                encounter.presentationOffsetX(), encounter.presentationOffsetY(), encounter.presentationOffsetZ());
    }

    private static void loadProjectionAnchorChunk(ServerWorld world, BlockPos anchor) {
        int chunkX = Math.floorDiv(anchor.getX(), 16);
        int chunkZ = Math.floorDiv(anchor.getZ(), 16);
        world.getChunk(chunkX, chunkZ);
    }

    private static void bind(
            PokemonEntity entity,
            CanonicalWildEncounterCatalogue.EncounterDefinition encounter
    ) {
        VisibleWildPokemonEncounterRuntime.bind(
                entity,
                encounter.canonicalEncounterId(),
                encounter.zoneId(),
                encounter.contextId()
        );
    }

    private static void evictMissingBinding(String canonicalEncounterId) {
        VisibleWildPokemonEncounterRuntime.boundEntityUuid(canonicalEncounterId)
                .ifPresent(VisibleWildPokemonEncounterRuntime::unbind);
    }

    private static void enforceProjectionContentGate(
            CanonicalWildEncounterCatalogue.EncounterDefinition encounter,
            WildEcologyDescriptorRegistry.Descriptor source
    ) {
        if (!source.projectionEligibility().test(encounter)) {
            throw new IllegalStateException("visible wild projection content rejected by source " + source.sourceId()
                    + ": " + encounter.canonicalEncounterId());
        }
    }

    private static void publishBeforeReveal(
            ServerWorld world,
            String canonicalEncounterId,
            WildEcologyDescriptorRegistry.Descriptor source
    ) {
        var registry = FabricCanonicalPlayerStoreRuntime.requireWildEncounterBlueprintRegistry(world.getServer());
        if (registry.resolve(canonicalEncounterId).isPresent()) return;
        boolean published = ServerOwnedWildEncounterBlueprintPublisher
                .fromWorldRuntime(world.getServer(), source.blueprintSource())
                .publish(canonicalEncounterId);
        if (!published || registry.resolve(canonicalEncounterId).isEmpty()) {
            throw new IllegalStateException("canonical wild blueprint must exist before actor reveal: "
                    + canonicalEncounterId);
        }
    }

    private static PokemonEntity findExisting(
            ServerWorld world,
            String canonicalEncounterId,
            BlockPos anchor
    ) {
        String tag = WILD_TAG_PREFIX + canonicalEncounterId;
        return world.getEntitiesByClass(
                        PokemonEntity.class,
                        new Box(anchor).expand(HABITAT_SEARCH_RADIUS_BLOCKS, 24.0D, HABITAT_SEARCH_RADIUS_BLOCKS),
                        entity -> !entity.isRemoved() && entity.getCommandTags().contains(tag)
                )
                .stream()
                .findFirst()
                .orElse(null);
    }
}
