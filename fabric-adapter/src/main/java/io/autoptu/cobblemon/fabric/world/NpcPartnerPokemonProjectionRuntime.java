package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import io.autoptu.cobblemon.authority.CanonicalNpcPokemonCatalogue;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/**
 * One-way projection of canonical named NPC partner Pokemon into Cobblemon entities.
 *
 * These actors are never registered as wild encounters. Their Cobblemon Pokemon payload exists only
 * to render the requested species. PTU combat state, capture legality, ownership, HP, moves, status,
 * despawn meaning and narrative identity remain outside Cobblemon.
 */
public final class NpcPartnerPokemonProjectionRuntime {
    public static final String PARTNER_TAG_PREFIX = "autoptu:npc-partner:";

    private NpcPartnerPokemonProjectionRuntime() {}

    public static PokemonEntity ensureProjected(ServerWorld world, String npcId, BlockPos anchor) {
        var partner = CanonicalNpcPokemonCatalogue.DEFAULT.partnerForNpc(npcId)
                .orElseThrow(() -> new IllegalArgumentException("missing canonical NPC partner: " + npcId));
        PokemonEntity existing = findExisting(world, partner.partnerId(), anchor);
        if (existing != null) return existing;

        Species species = PokemonSpecies.INSTANCE.getByName(partner.speciesId());
        if (species == null) throw new IllegalStateException("Cobblemon species unavailable for NPC partner: " + partner.speciesId());

        Pokemon pokemon = new Pokemon();
        pokemon.setSpecies(species);
        PokemonEntity entity = new PokemonEntity(world, pokemon, CobblemonEntities.POKEMON);
        entity.refreshPositionAndAngles(anchor.getX() + 1.5D, anchor.getY() + 1.0D, anchor.getZ() + 0.5D, 180.0F, 0.0F);
        entity.setCustomName(Text.literal(partner.displayName()));
        entity.setCustomNameVisible(true);
        entity.setPersistent();
        entity.addCommandTag(PARTNER_TAG_PREFIX + partner.partnerId());
        entity.addCommandTag("ouros:npc-partner");
        return world.spawnEntity(entity) ? entity : null;
    }

    static PokemonEntity findExisting(ServerWorld world, String partnerId, BlockPos anchor) {
        String tag = PARTNER_TAG_PREFIX + partnerId;
        return world.getEntitiesByClass(
                        PokemonEntity.class,
                        new Box(anchor).expand(16.0D, 8.0D, 16.0D),
                        entity -> entity.getCommandTags().contains(tag)
                )
                .stream()
                .findFirst()
                .orElse(null);
    }

    public static boolean isNpcPartner(PokemonEntity entity) {
        if (entity == null) return false;
        return entity.getCommandTags().stream().anyMatch(tag -> tag.startsWith(PARTNER_TAG_PREFIX));
    }
}
