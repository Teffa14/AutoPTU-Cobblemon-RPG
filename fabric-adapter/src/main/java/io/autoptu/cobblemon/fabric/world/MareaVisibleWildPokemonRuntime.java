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
    private static final int HABITAT_LEASH_RADIUS_BLOCKS = 32;
    private static final int HABITAT_SEARCH_RADIUS_BLOCKS = 48;
    private static final MareaCanonicalWildEncounterBlueprintSource BLUEPRINT_SOURCE =
            new MareaCanonicalWildEncounterBlueprintSource();

    private MareaVisibleWildPokemonRuntime() {}

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!(entity instanceof PokemonEntity pokemonEntity)) return;
            canonicalEncounterFor(pokemonEntity).ifPresent(encounter -> {
                publishBeforeReveal(world, encounter.canonicalEncounterId());
                bind(pokemonEntity, encounter);
            });
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (!(entity instanceof PokemonEntity pokemonEntity)) return;
            if (canonicalEncounterFor(pokemonEntity).isEmpty()) return;
            var reason = pokemonEntity.getRemovalReason();
            if (reason != null && reason.shouldDestroy()) {
                VisibleWildPokemonEncounterRuntime.unbind(pokemonEntity.getUuid());
                LOGGER.info("AutoPTU Marea wild actor destroyed; canonical presence released: entity={} reason={}",
                        pokemonEntity.getUuid(), reason);
            }
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            int visible = ensureProjected(server.getOverworld());
            LOGGER.info("AutoPTU normal Marea visible wild actors ready: {}", visible);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> keepProjectedActorsInHabitat(server.getOverworld()));
    }

    public static int ensureProjected(ServerWorld world) {
        if (world == null) throw new IllegalArgumentException("world is required");
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
        publishBeforeReveal(world, encounter.canonicalEncounterId());
        BlockPos anchor = presentationAnchor(encounter);
        loadHabitatChunks(world, anchor);

        PokemonEntity existing = findExisting(world, encounter.canonicalEncounterId(), anchor);
        if (existing != null) {
            bind(existing, encounter);
            keepInHabitat(existing, anchor);
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
        return entity;
    }

    static PokemonEntity actorForEncounter(ServerWorld world, String canonicalEncounterId) {
        if (world == null || canonicalEncounterId == null || canonicalEncounterId.isBlank()) return null;
        var encounter = CanonicalWildEncounterCatalogue.DEFAULT.encounter(canonicalEncounterId.strip()).orElse(null);
        if (encounter == null || !encounter.siteId().startsWith("ouros.marea.")) return null;
        var boundUuid = VisibleWildPokemonEncounterRuntime.boundEntityUuid(encounter.canonicalEncounterId());
        if (boundUuid.isPresent()) {
            var loaded = world.getEntity(boundUuid.get());
            if (loaded instanceof PokemonEntity pokemonEntity && !pokemonEntity.isRemoved()) {
                keepInHabitat(pokemonEntity, presentationAnchor(encounter));
                return pokemonEntity;
            }
            return null;
        }
        return ensureProjected(world, encounter);
    }

    static int presenceReconcileIntervalTicks() { return PRESENCE_RECONCILE_INTERVAL_TICKS; }

    private static void keepProjectedActorsInHabitat(ServerWorld world) {
        for (var population : CanonicalWildPopulationCatalogue.DEFAULT.populations()) {
            if (!population.siteId().startsWith("ouros.marea.")) continue;
            for (var encounter : CanonicalWildPopulationCatalogue.DEFAULT.members(population)) {
                var boundUuid = VisibleWildPokemonEncounterRuntime.boundEntityUuid(encounter.canonicalEncounterId());
                if (boundUuid.isEmpty()) {
                    PokemonEntity replacement = ensureProjected(world, encounter);
                    if (replacement != null) {
                        LOGGER.info("AutoPTU Marea wild presence restored canonical member: encounter={} new={}",
                                encounter.canonicalEncounterId(), replacement.getUuid());
                    }
                    continue;
                }

                var loaded = world.getEntity(boundUuid.get());
                if (loaded instanceof PokemonEntity pokemonEntity && !pokemonEntity.isRemoved()) {
                    keepInHabitat(pokemonEntity, presentationAnchor(encounter));
                }
                // An unloaded persistent actor intentionally keeps its canonical UUID binding. The
                // Fabric ENTITY_UNLOAD callback releases that identity only for destructive removal.
            }
        }
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

    private static void keepInHabitat(PokemonEntity entity, BlockPos anchor) {
        double centerX = anchor.getX() + 0.5D;
        double centerZ = anchor.getZ() + 0.5D;
        double dx = entity.getX() - centerX;
        double dz = entity.getZ() - centerZ;
        if (dx * dx + dz * dz <= (double) HABITAT_LEASH_RADIUS_BLOCKS * HABITAT_LEASH_RADIUS_BLOCKS) return;
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
