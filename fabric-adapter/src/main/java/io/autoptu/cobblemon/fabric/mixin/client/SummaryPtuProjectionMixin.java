package io.autoptu.cobblemon.fabric.mixin.client;

import com.cobblemon.mod.common.client.gui.summary.Summary;
import com.cobblemon.mod.common.pokemon.Pokemon;
import io.autoptu.cobblemon.fabric.client.FabricCanonicalPokemonSummaryClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Keeps disposable AutoPTU Pokemon on Cobblemon's native STATS surface.
 *
 * Other Summary tabs expose Cobblemon-owned nature/IV/EV/marks/edit data that is not yet part of the
 * durable PTU read model. Rather than fabricate those values, projected Pokemon remain on the exact
 * HP/combat-stat surface that AutoPTU can authoritatively supply.
 */
@Mixin(Summary.class)
public abstract class SummaryPtuProjectionMixin {
    private static final int STATS_SCREEN = 2;

    @Shadow
    private Pokemon selectedPokemon;

    @ModifyVariable(method = "displayMainScreen", at = @At("HEAD"), argsOnly = true)
    private int autoptu$keepCanonicalProjectionOnStats(int requestedScreen) {
        if (selectedPokemon != null
                && FabricCanonicalPokemonSummaryClient.projection(selectedPokemon.getUuid()).isPresent()) {
            return STATS_SCREEN;
        }
        return requestedScreen;
    }
}
