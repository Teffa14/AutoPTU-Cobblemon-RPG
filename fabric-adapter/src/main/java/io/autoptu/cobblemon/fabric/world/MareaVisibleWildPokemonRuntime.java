package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import io.autoptu.cobblemon.authority.CanonicalWildEncounterCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWorldMapCatalogue;
import io.autoptu.cobblemon.fabric.battle.MareaCanonicalWildEncounterBlueprintSource;
import io.autoptu.cobblemon.fabric.battle.ServerOwnedWildEncounterBlueprintPublisher;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Normal Marea visible-wild projection backed only by canonical AutoPTU population and encounter state. */
public final class MareaVisibleWildPokemonRuntime {
    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");
    private static final String WILD_TAG_PREFIX = "autoptu:wild-encounter:";
    private static final String WILD_MARKER_TAG = "ouros:visible-wild";
    private static final int PRESENCE_RECONCILE_INTERVAL_TICKS = 100;
    private static final int HABITAT_SEARCH_RADIUS_BLOCKS = 48;
    private static final MareaCanonicalWildEncounterBlueprintSource BLUEPRINT_SOURCE =
            new MareaCanonicalWildEncounterBlueprintSource();

    private MareaVisibleWildPokemonRuntime() {}

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!(entity instanceof PokemonEntity pokemonEntity)) return;
            var encounter = canonicalEncounterFor(pokemonEntity);
            if (encounter.isEmpty()) return;
            if (!isCanonicalMareaWorld(world)) {
                VisibleWildPokemonEncounterRuntime.unbind(pokemonEntity.getUuid());
                pokemonEntity.discard();
                LOGGER.warn("AutoPTU rejected Marea wild actor outside canonical Overworld: entity={} dimension={}",
                        pokemonEntity.getUuid(), world.getRegistryKey().getValue());
                return;
            }
            if (!FabricCanonicalPlayerStoreRuntime.storesAvailable(world.getServer())) return;
            publishBeforeReveal(world, encounter.get().canonicalEncounterId());
            bind(pokemonEntity, encounter.get());
            setPopulationProjectionActive(
                    pokemonEntity,
                    hasPlayerInsidePresenceFootprint(world, populationFor(encounter.get()))
            );
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (!(entity instanceof PokemonEntity pokemonEntity)) return;
            if (canonicalEncounterFor(pokemonEntity).isEmpty()) return;
            var reason = pokemonEntity.getRemovalReason();
            if (reason == null) return;
            if (reason.shouldDestroy() || reason == Entity.RemovalReason.CHANGED_DIMENSION) {
                VisibleWildPokemonEncounterRuntime.unbind(pokemonEntity.getUuid());
                LOGGER.info("AutoPTU Marea wild actor released canonical presence: entity={} reason={}",
                        pokemonEntity.getUuid(), reason);
            }
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            int visible = reconcileActivePopulations(server.getOverworld());
            LOGGER.info("AutoPTU normal Marea visible wild actors ready: {}", visible);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % PRESENCE_RECONCILE_INTERVAL_TICKS != 0) return;
            reconcileActivePopulations(server.getOverworld());
        });
    }

    /**
     * Explicitly projects every authored Marea member. This remains the admin/smoke bootstrap surface;
     * normal gameplay uses player presence inside each population's authored footprint.
     */
    public static int ensureProjected(ServerWorld world) {
        if (world == null) throw new IllegalArgumentException("world is required");
        if (!isCanonicalMareaWorld(world)) throw new IllegalArgumentException("Marea projection requires the canonical Overworld");
        int visible = 0;
        for (var population : CanonicalWildPopulationCatalogue.DEFAULT.populations()) {
            if (!population.siteId().startsWith("ouros.marea.")) continue;
            for (var encounter : CanonicalWildPopulationCatalogue.DEFAULT.members(population)) {
                if (ensureProjected(world, encounter) != null) visible++;
            }
        }
        return visible;
    }

    static PokemonEntity ensureProjected(ServerWorld world, CanonicalWildEncounterCatalogue.EncounterDefinition encounter) {
        if (!isCanonicalMareaWorld(world)) throw new IllegalArgumentException("Marea projection requires the canonical Overworld");
        publishBeforeReveal(world, encounter.canonicalEncounterId());
        BlockPos anchor = presentationAnchor(encounter);
        loadHabitatChunks(world, anchor);
        PokemonEntity existing = findExisting(world, encounter.canonicalEncounterId(), anchor);
        if (existing != null) {
            bind(existing, encounter);
            keepInHabitat(existing, encounter);
            setPopulationProjectionActive(existing, true);
            return existing;
        }

        evictMissingBinding(encounter.canonicalEncounterId());
        enforceProjectionContentGate(encounter);
        Species species = PokemonSpecies.INSTANCE.getByName(encounter.speciesId());
        if (species == null) {
            throw new IllegalStateException("Cobblemon official species unavailable for Marea wild actor: " + encounter.speciesId());
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
        if (!isCanonicalMareaWorld(world)) return null;
        var encounter = CanonicalWildEncounterCatalogue.DEFAULT.encounter(canonicalEncounterId.strip()).orElse(null);
        if (encounter == null || !encounter.siteId().startsWith("ouros.marea.")) return null;
        var boundUuid = VisibleWildPokemonEncounterRuntime.boundEntityUuid(encounter.canonicalEncounterId());
        if (boundUuid.isPresent()) {
            var loaded = world.getEntity(boundUuid.get());
            if (loaded instanceof PokemonEntity pokemonEntity && !pokemonEntity.isRemoved()) {
                keepInHabitat(pokemonEntity, encounter);
                return pokemonEntity;
            }
            return null;
        }
        return ensureProjected(world, encounter);
    }

    static int presenceReconcileIntervalTicks() { return PRESENCE_RECONCILE_INTERVAL_TICKS; }

    static int reconcileActivePopulations(ServerWorld world) {
        if (world == null || !isCanonicalMareaWorld(world)) return 0;
        if (!FabricCanonicalPlayerStoreRuntime.storesAvailable(world.getServer())) return 0;
        int visible = 0;
        for (var population : CanonicalWildPopulationCatalogue.DEFAULT.populations()) {
            if (!population.siteId().startsWith("ouros.marea.")) continue;
            boolean active = hasPlayerInsidePresenceFootprint(world, population);
            if (!active) {
                hibernateLoadedPopulation(world, population);
                continue;
            }
            for (var encounter : CanonicalWildPopulationCatalogue.DEFAULT.members(population)) {
                var boundUuid = VisibleWildPokemonEncounterRuntime.boundEntityUuid(encounter.canonicalEncounterId());
                if (boundUuid.isEmpty()) {
                    PokemonEntity replacement = ensureProjected(world, encounter);
                    if (replacement != null) {
                        visible++;
                        LOGGER.info("AutoPTU Marea wild presence activated/restored canonical member: population={} encounter={} entity={}",
                                population.populationId(), encounter.canonicalEncounterId(), replacement.getUuid());
                    }
                    continue;
                }
                var loaded = world.getEntity(boundUuid.get());
                if (loaded instanceof PokemonEntity pokemonEntity && !pokemonEntity.isRemoved()) {
                    keepInHabitat(pokemonEntity, encounter);
                    setPopulationProjectionActive(pokemonEntity, true);
                    visible++;
                }
                // Ordinary persistent chunk unload preserves the canonical UUID binding. A later chunk
                // load rebinds the same actor through ENTITY_LOAD; authored presence never invents a
                // replacement merely because the habitat chunk is currently unloaded.
            }
        }
        return visible;
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
                .orElseThrow(() -> new IllegalStateException("missing canonical wild population policy: " + encounter.populationId()));
    }

    private static boolean hasPlayerInsidePresenceFootprint(
            ServerWorld world,
            CanonicalWildPopulationCatalogue.PopulationDefinition population
    ) {
        var site = CanonicalWorldMapCatalogue.DEFAULT.site(population.siteId())
                .orElseThrow(() -> new IllegalStateException("missing canonical wild population site: " + population.siteId()));
        var footprint = population.presenceFootprint();
        for (var player : world.getPlayers()) {
            if (player.isSpectator()) continue;
            double dx = player.getX() - (site.x() + 0.5D);
            double dy = player.getY() - site.y();
            double dz = player.getZ() - (site.z() + 0.5D);
            if (footprint.containsOffset(dx, dy, dz)) return true;
        }
        return false;
    }

    private static java.util.Optional<CanonicalWildEncounterCatalogue.EncounterDefinition> canonicalEncounterFor(
            PokemonEntity entity) {
        if (entity == null || !entity.getCommandTags().contains(WILD_MARKER_TAG)) return java.util.Optional.empty();
        for (String tag : entity.getCommandTags()) {
            if (!tag.startsWith(WILD_TAG_PREFIX)) continue;
            String encounterId = tag.substring(WILD_TAG_PREFIX.length());
            var encounter = CanonicalWildEncounterCatalogue.DEFAULT.encounter(encounterId);
            if (encounter.isPresent() && encounter.get().siteId().startsWith("ouros.marea.")) return encounter;
        }
        return java.util.Optional.empty();
    }

    private static boolean isCanonicalMareaWorld(ServerWorld world) {
        return world != null && world.getServer() != null && world == world.getServer().getOverworld();
    }

    private static void keepInHabitat(
            PokemonEntity entity,
            CanonicalWildEncounterCatalogue.EncounterDefinition encounter
    ) {
        BlockPos anchor = presentationAnchor(encounter);
        int leashRadiusBlocks = populationFor(encounter).habitatLeashRadiusBlocks();
        double centerX = anchor.getX() + 0.5D;
        double centerZ = anchor.getZ() + 0.5D;
        double dx = entity.getX() - centerX;
        double dz = entity.getZ() - centerZ;
        if (dx * dx + dz * dz <= (double) leashRadiusBlocks * leashRadiusBlocks) return;
        entity.requestTeleport(centerX, anchor.getY(), centerZ);
    }

    private static void loadHabitatChunks(ServerWorld world, BlockPos anchor) {
        int minChunkX = Math.floorDiv(anchor.getX() - HABITAT_SEARCH_RADIUS_BLOCKS, 16);
        int maxChunkX = Math.floorDiv(anchor.getX() + HABITAT_SEARCH_RADIUS_BLOCKS, 16);
        int minChunkZ = Math.floorDiv(anchor.getZ() - HABITAT_SEARCH_RADIUS_BLOCKS, 16);
        int maxChunkZ = Math.floorDiv(anchor.getZ() + HABITAT_SEARCH_RADIUS_BLOCKS, 16);
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) world.getChunk(chunkX, chunkZ);
        }
    }

    private static void bind(PokemonEntity entity, CanonicalWildEncounterCatalogue.EncounterDefinition encounter) {
        VisibleWildPokemonEncounterRuntime.bind(entity, encounter.canonicalEncounterId(), encounter.zoneId(), encounter.contextId());
    }

    private static void evictMissingBinding(String canonicalEncounterId) {
        VisibleWildPokemonEncounterRuntime.boundEntityUuid(canonicalEncounterId)
                .ifPresent(VisibleWildPokemonEncounterRuntime::unbind);
    }

    private static void enforceProjectionContentGate(CanonicalWildEncounterCatalogue.EncounterDefinition encounter) {
        if (encounter.speciesStatus() != CanonicalWildEncounterCatalogue.SpeciesStatus.OFFICIAL) {
            throw new IllegalStateException("ordinary Marea projection refuses non-official species content");
        }
        if (encounter.fusion()) throw new IllegalStateException("ordinary Marea projection refuses fusion content");
        if (!"standard".equals(encounter.formId())) {
            throw new IllegalStateException("Marea wild projection supports only the approved standard form");
        }
    }

    private static void publishBeforeReveal(ServerWorld world, String canonicalEncounterId) {
        var registry = FabricCanonicalPlayerStoreRuntime.requireWildEncounterBlueprintRegistry(world.getServer());
        if (registry.resolve(canonicalEncounterId).isPresent()) return;
        boolean published = ServerOwnedWildEncounterBlueprintPublisher
                .fromWorldRuntime(world.getServer(), BLUEPRINT_SOURCE).publish(canonicalEncounterId);
        if (!published || registry.resolve(canonicalEncounterId).isEmpty()) {
            throw new IllegalStateException("canonical Marea wild blueprint must exist before actor reveal: " + canonicalEncounterId);
        }
    }

    private static BlockPos presentationAnchor(CanonicalWildEncounterCatalogue.EncounterDefinition encounter) {
        var site = CanonicalWorldMapCatalogue.DEFAULT.site(encounter.siteId())
                .orElseThrow(() -> new IllegalStateException("missing canonical wild encounter site: " + encounter.siteId()));
        return new BlockPos(site.x(), site.y(), site.z()).add(
                encounter.presentationOffsetX(), encounter.presentationOffsetY(), encounter.presentationOffsetZ());
    }

    private static PokemonEntity findExisting(ServerWorld world, String canonicalEncounterId, BlockPos anchor) {
        String tag = WILD_TAG_PREFIX + canonicalEncounterId;
        return world.getEntitiesByClass(PokemonEntity.class,
                        new Box(anchor).expand(HABITAT_SEARCH_RADIUS_BLOCKS, 24.0D, HABITAT_SEARCH_RADIUS_BLOCKS),
                        entity -> !entity.isRemoved() && entity.getCommandTags().contains(tag))
                .stream().findFirst().orElse(null);
    }
}
