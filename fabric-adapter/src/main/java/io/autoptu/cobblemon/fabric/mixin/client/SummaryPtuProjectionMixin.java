package io.autoptu.cobblemon.fabric.mixin.client;

import com.cobblemon.mod.common.client.gui.summary.Summary;
import com.cobblemon.mod.common.pokemon.Pokemon;
import io.autoptu.cobblemon.fabric.client.FabricCanonicalPokemonSummaryClient;
import io.autoptu.cobblemon.fabric.network.FabricCanonicalPokemonSummaryPayload;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Keeps disposable AutoPTU Pokemon on Cobblemon's native STATS surface.
 *
 * Other Summary tabs expose Cobblemon-owned nature/IV/EV/marks/edit data that is not yet part of the
 * durable PTU read model. Rather than fabricate those values, projected Pokemon remain on the exact
 * HP/combat-stat surface that AutoPTU can authoritatively supply. Canonical conditions that the native
 * Summary cannot represent are surfaced as read-only action-bar context when the selection changes.
 */
@Mixin(value = Summary.class, remap = false)
public abstract class SummaryPtuProjectionMixin {
    private static final int STATS_SCREEN = 2;
    private static final long BLOCKED_TAB_NOTICE_COOLDOWN_MS = 1_500L;

    @Shadow(remap = false)
    private Pokemon selectedPokemon;

    private long autoptu$lastBlockedTabNoticeMs = Long.MIN_VALUE;
    private UUID autoptu$lastProjectedPokemonUuid;

    @ModifyVariable(method = "displayMainScreen", at = @At("HEAD"), argsOnly = true, remap = false)
    private int autoptu$keepCanonicalProjectionOnStats(int requestedScreen) {
        if (selectedPokemon != null) {
            var projection = FabricCanonicalPokemonSummaryClient.projection(selectedPokemon.getUuid());
            if (projection.isPresent()) {
                UUID selectedUuid = selectedPokemon.getUuid();
                if (!selectedUuid.equals(autoptu$lastProjectedPokemonUuid)) {
                    autoptu$lastProjectedPokemonUuid = selectedUuid;
                    autoptu$lastBlockedTabNoticeMs = Long.MIN_VALUE;
                    autoptu$showCanonicalConditionCue(projection.get());
                }
                if (requestedScreen != STATS_SCREEN) {
                    long now = Util.getMeasuringTimeMs();
                    if (now - autoptu$lastBlockedTabNoticeMs >= BLOCKED_TAB_NOTICE_COOLDOWN_MS) {
                        MinecraftClient client = MinecraftClient.getInstance();
                        if (client.player != null) {
                            client.player.sendMessage(Text.literal(
                                    "AutoPTU keeps this read-only Summary on canonical PTU stats; unsupported Cobblemon tabs are unavailable."),
                                    true);
                        }
                        autoptu$lastBlockedTabNoticeMs = now;
                    }
                }
                return STATS_SCREEN;
            }
        }
        autoptu$lastProjectedPokemonUuid = null;
        return requestedScreen;
    }

    private static void autoptu$showCanonicalConditionCue(FabricCanonicalPokemonSummaryPayload.Projection projection) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        String statuses = projection.statuses().isEmpty() ? "clear" : String.join(", ", projection.statuses());
        client.player.sendMessage(Text.literal(
                "PTU HP " + projection.currentHp() + "/" + projection.maxHp()
                        + " | Status " + statuses
                        + " | Injuries " + projection.injuries()),
                true);
    }
}
