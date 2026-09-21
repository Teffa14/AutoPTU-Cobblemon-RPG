package io.autoptu.cobblemon.fabric.mixin.client;

import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.Pokemon;
import io.autoptu.cobblemon.fabric.client.FabricCanonicalPokemonSummaryClient;
import io.autoptu.cobblemon.fabric.network.FabricCanonicalPokemonSummaryPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Read-only presentation override for disposable Pokemon sent to Cobblemon's native Summary screen.
 * Normal Cobblemon Pokemon are untouched because only AutoPTU projection UUIDs exist in the cache.
 */
@Mixin(Pokemon.class)
public abstract class PokemonPtuSummaryProjectionMixin {
    @Inject(method = "getStat", at = @At("HEAD"), cancellable = true)
    private void autoptu$canonicalStat(Stat stat, CallbackInfoReturnable<Integer> cir) {
        FabricCanonicalPokemonSummaryPayload.Projection projection = projection();
        if (projection == null) return;
        if (stat == Stats.HP) cir.setReturnValue(projection.maxHp());
        else if (stat == Stats.ATTACK) cir.setReturnValue(projection.atk());
        else if (stat == Stats.DEFENCE) cir.setReturnValue(projection.def());
        else if (stat == Stats.SPECIAL_ATTACK) cir.setReturnValue(projection.spatk());
        else if (stat == Stats.SPECIAL_DEFENCE) cir.setReturnValue(projection.spdef());
        else if (stat == Stats.SPEED) cir.setReturnValue(projection.spd());
    }

    @Inject(method = "getCurrentHealth", at = @At("HEAD"), cancellable = true)
    private void autoptu$canonicalCurrentHealth(CallbackInfoReturnable<Integer> cir) {
        FabricCanonicalPokemonSummaryPayload.Projection projection = projection();
        if (projection != null) cir.setReturnValue(projection.currentHp());
    }

    private FabricCanonicalPokemonSummaryPayload.Projection projection() {
        Pokemon pokemon = (Pokemon) (Object) this;
        return FabricCanonicalPokemonSummaryClient.projection(pokemon.getUuid()).orElse(null);
    }
}
