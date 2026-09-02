package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import io.autoptu.cobblemon.authority.CanonicalWildEncounterCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWorldMapCatalogue;
import io.autoptu.cobblemon.fabric.battle.MareaCanonicalWildEncounterBlueprintSource;
import io.autoptu.cobblemon.fabric.battle.ServerOwnedWildEncounterBlueprintPublisher;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * First normal Marea visible-wild projection.
 *
 * <p>The complete canonical PTU blueprint is published to the active world's create-only registry
 * before a Cobblemon PokemonEntity is revealed. The entity carries only species presentation and
 * a correlation tag. Its Pokemon payload is never read back as PTU authority.</p>
 */
public final class MareaVisibleWildPokemonRuntime {
    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");
    private static final String WILD_TAG_PREFIX = "autoptu:wild-encounter:";
    private static final String WILD_MARKER_TAG = "ouros:visible-wild";
    private static final MareaCanonicalWildEncounterBlueprintSource BLUEPRINT_SOURCE =
            new MareaCanonicalWildEncounterBlueprintSource();

    private MareaVisibleWildPokemonRuntime() {}

    /**
     * Provisions the currently authored Marea visible-wild slice on normal server startup. Existing
     * persistent actors are reused and rebound; a missing actor is revealed only after its complete
     * canonical WILD blueprint has been published into this world's server-owned registry.
     */
    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            int visible = ensureProjected(server.getOverworld());
            LOGGER.info("AutoPTU normal Marea visible wild actors ready: {}", visible);
        });
    }

    /** Ensures every currently authored first-slice Marea encounter has a published blueprint and actor. */
    public static int ensureProjected(ServerWorld world) {
        if (world == null) throw new IllegalArgumentException("world is required");
        int visible = 0;
        for (var encounter : CanonicalWildEncounterCatalogue.DEFAULT.encounters()) {
            if (!encounter.siteId().startsWith("ouros.marea.")) continue;
            if (ensureProjected(world, encounter) != null) visible++;
        }
        return visible;
    }

    static PokemonEntity ensureProjected(
            ServerWorld world,
            CanonicalWildEncounterCatalogue.EncounterDefinition encounter
    ) {
        // Authority is established before either finding/rebinding or revealing a presentation actor.
        publishBeforeReveal(world, encounter.canonicalEncounterId());

        BlockPos anchor = presentationAnchor(encounter);
        // Normal lifecycle provisioning must be able to find a persistent actor after restart even when
        // the authored Marea chunk was not already in a player's ticket set.
        world.getChunk(anchor);
        PokemonEntity existing = findExisting(world, encounter.canonicalEncounterId(), anchor);
        if (existing != null) {
            bind(existing, encounter);
            return existing;
        }

        enforceProjectionContentGate(encounter);
        Species species = PokemonSpecies.INSTANCE.getByName(encounter.speciesId());
        if (species == null) {
            throw new IllegalStateException("Cobblemon official species unavailable for Marea wild actor: "
                    + encounter.speciesId());
        }

        // Species is supplied only so Cobblemon can render the correct official model. Canonical level,
        // HP, stats, moves, Ability, statuses and battle state remain exclusively in the published blueprint.
        Pokemon pokemon = new Pokemon();
        pokemon.setSpecies(species);
        PokemonEntity entity = new PokemonEntity(world, pokemon, CobblemonEntities.POKEMON);
        entity.refreshPositionAndAngles(
                anchor.getX() + 0.5D,
                anchor.getY(),
                anchor.getZ() + 0.5D,
                180.0F,
                0.0F
        );
        entity.setPersistent();
        entity.addCommandTag(WILD_TAG_PREFIX + encounter.canonicalEncounterId());
        entity.addCommandTag(WILD_MARKER_TAG);

        if (!world.spawnEntity(entity)) return null;
        bind(entity, encounter);
        return entity;
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

    private static void enforceProjectionContentGate(CanonicalWildEncounterCatalogue.EncounterDefinition encounter) {
        if (encounter.speciesStatus() != CanonicalWildEncounterCatalogue.SpeciesStatus.OFFICIAL) {
            throw new IllegalStateException("ordinary Marea projection refuses non-official species content");
        }
        if (encounter.fusion()) {
            throw new IllegalStateException("ordinary Marea projection refuses fusion content");
        }
        if (!"standard".equals(encounter.formId())) {
            throw new IllegalStateException("first Marea wild projection supports only the approved standard form");
        }
    }

    private static void publishBeforeReveal(ServerWorld world, String canonicalEncounterId) {
        var registry = FabricCanonicalPlayerStoreRuntime.requireWildEncounterBlueprintRegistry(world.getServer());
        if (registry.resolve(canonicalEncounterId).isPresent()) return;

        boolean published = ServerOwnedWildEncounterBlueprintPublisher
                .fromWorldRuntime(world.getServer(), BLUEPRINT_SOURCE)
                .publish(canonicalEncounterId);
        if (!published || registry.resolve(canonicalEncounterId).isEmpty()) {
            throw new IllegalStateException("canonical Marea wild blueprint must exist before actor reveal: "
                    + canonicalEncounterId);
        }
    }

    private static BlockPos presentationAnchor(CanonicalWildEncounterCatalogue.EncounterDefinition encounter) {
        var site = CanonicalWorldMapCatalogue.DEFAULT.site(encounter.siteId())
                .orElseThrow(() -> new IllegalStateException("missing canonical wild encounter site: "
                        + encounter.siteId()));
        return new BlockPos(site.x(), site.y(), site.z()).add(
                encounter.presentationOffsetX(),
                encounter.presentationOffsetY(),
                encounter.presentationOffsetZ()
        );
    }

    private static PokemonEntity findExisting(ServerWorld world, String canonicalEncounterId, BlockPos anchor) {
        String tag = WILD_TAG_PREFIX + canonicalEncounterId;
        return world.getEntitiesByClass(
                        PokemonEntity.class,
                        new Box(anchor).expand(24.0D, 12.0D, 24.0D),
                        entity -> entity.getCommandTags().contains(tag)
                )
                .stream()
                .findFirst()
                .orElse(null);
    }
}
