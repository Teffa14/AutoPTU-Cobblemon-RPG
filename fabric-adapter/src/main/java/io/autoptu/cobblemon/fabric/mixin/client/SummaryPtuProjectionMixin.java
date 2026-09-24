package io.autoptu.cobblemon.fabric.mixin.client;

import com.cobblemon.mod.common.client.gui.summary.Summary;
import com.cobblemon.mod.common.pokemon.Pokemon;
import io.autoptu.cobblemon.fabric.client.FabricCanonicalPokemonSummaryClient;
import io.autoptu.cobblemon.fabric.network.FabricCanonicalPokemonSummaryPayload;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Keeps disposable AutoPTU Pokemon on Cobblemon's native read-only STATS surface. */
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
                    autoptu$showCanonicalSummaryCue(projection.get(), false);
                }
                if (requestedScreen != STATS_SCREEN) {
                    long now = Util.getMeasuringTimeMs();
                    if (now - autoptu$lastBlockedTabNoticeMs >= BLOCKED_TAB_NOTICE_COOLDOWN_MS) {
                        autoptu$showCanonicalSummaryCue(projection.get(), true);
                        autoptu$lastBlockedTabNoticeMs = now;
                    }
                }
                return STATS_SCREEN;
            }
        }
        autoptu$lastProjectedPokemonUuid = null;
        return requestedScreen;
    }

    private static void autoptu$showCanonicalSummaryCue(
            FabricCanonicalPokemonSummaryPayload.Projection projection,
            boolean blockedTab) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        if (blockedTab) {
            client.player.sendMessage(Text.literal("PTU Summary | Read-only tab unavailable"), true);
            return;
        }

        String statuses = projection.statuses().isEmpty()
                ? "clear"
                : projection.statuses().stream()
                        .map(SummaryPtuProjectionMixin::autoptu$displayCanonicalId)
                        .collect(Collectors.joining(", "));
        String moves = !projection.moveLoadoutAvailable()
                ? "unavailable"
                : projection.moveIds().isEmpty()
                        ? "none"
                        : projection.moveIds().stream()
                                .map(SummaryPtuProjectionMixin::autoptu$displayCanonicalId)
                                .collect(Collectors.joining(", "));
        // Level, HP and combat stats are already rendered by Cobblemon's native widgets through
        // PokemonPtuSummaryProjectionMixin. Keep the transient cue for PTU-only state so it stays
        // readable instead of duplicating the native Summary values in the action bar. A zero-HP
        // cue reports the exact canonical HP state only; it deliberately does not infer fainting.
        String hpState = projection.currentHp() == 0 ? " | HP depleted" : "";
        String message = "PTU | Moves " + moves
                + " | Status " + statuses
                + " | " + autoptu$displayInjuries(projection.injuries())
                + " | Held item " + (projection.heldItemEquipped() ? "equipped" : "none")
                + hpState;
        client.player.sendMessage(Text.literal(message), true);
    }

    private static String autoptu$displayInjuries(int injuries) {
        if (injuries <= 0) return "Injuries none";
        return injuries == 1 ? "1 injury" : injuries + " injuries";
    }

    private static String autoptu$displayCanonicalId(String canonicalId) {
        if (canonicalId == null || canonicalId.isBlank()) return "Unknown";
        String normalized = canonicalId.replace('-', ' ').replace('_', ' ').trim();
        return java.util.Arrays.stream(normalized.split("\\s+"))
                .filter(part -> !part.isBlank())
                .map(part -> part.substring(0, 1).toUpperCase(Locale.ROOT)
                        + part.substring(1).toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(" "));
    }
}
