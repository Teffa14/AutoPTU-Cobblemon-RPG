package io.autoptu.cobblemon.fabric.mixin.client;

import com.cobblemon.mod.common.client.gui.summary.Summary;
import com.cobblemon.mod.common.pokemon.Pokemon;
import io.autoptu.cobblemon.fabric.client.FabricCanonicalPokemonSummaryClient;
import io.autoptu.cobblemon.fabric.network.FabricCanonicalPokemonSummaryPayload;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Keeps disposable AutoPTU Pokemon on Cobblemon's native read-only STATS surface. */
@Mixin(value = Summary.class, remap = false)
public abstract class SummaryPtuProjectionMixin {
    private static final int STATS_SCREEN = 2;
    private static final int MAX_CUE_IDS = 3;

    @Shadow(remap = false)
    private Pokemon selectedPokemon;

    private int autoptu$lastBlockedScreen = -1;
    private UUID autoptu$lastProjectedPokemonUuid;

    @ModifyVariable(method = "displayMainScreen", at = @At("HEAD"), argsOnly = true, remap = false)
    private int autoptu$keepCanonicalProjectionOnStats(int requestedScreen) {
        if (selectedPokemon != null) {
            var projection = FabricCanonicalPokemonSummaryClient.projection(selectedPokemon.getUuid());
            if (projection.isPresent()) {
                UUID selectedUuid = selectedPokemon.getUuid();
                if (!selectedUuid.equals(autoptu$lastProjectedPokemonUuid)) {
                    autoptu$lastProjectedPokemonUuid = selectedUuid;
                    autoptu$lastBlockedScreen = -1;
                    autoptu$showCanonicalSummaryCue(projection.get(), false);
                }
                if (requestedScreen != STATS_SCREEN && requestedScreen != autoptu$lastBlockedScreen) {
                    autoptu$showCanonicalSummaryCue(projection.get(), true);
                    autoptu$lastBlockedScreen = requestedScreen;
                }
                return STATS_SCREEN;
            }
        }
        autoptu$lastProjectedPokemonUuid = null;
        autoptu$lastBlockedScreen = -1;
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
                : autoptu$displayCanonicalIds(projection.statuses());
        String moves = !projection.moveLoadoutAvailable()
                ? "unavailable"
                : projection.moveIds().isEmpty()
                        ? "none"
                        : autoptu$displayCanonicalIds(projection.moveIds());
        String capabilities = projection.capabilitiesAvailable()
                ? "available"
                : "unavailable";
        String movement = projection.movementAvailable()
                ? "available"
                : "unavailable";
        // Level, HP and combat stats are already rendered by Cobblemon's native widgets through
        // PokemonPtuSummaryProjectionMixin. Keep the transient cue for PTU-only state so it stays
        // readable instead of duplicating the native Summary values in the action bar. Availability
        // labels mirror only explicit canonical snapshot flags; they never infer missing PTU values.
        // A zero-HP cue reports the exact canonical HP state only; it deliberately does not infer fainting.
        String hpState = projection.currentHp() == 0 ? " | HP depleted" : "";
        String message = "PTU | Moves " + moves
                + " | Status " + statuses
                + " | " + autoptu$displayInjuries(projection.injuries())
                + " | Held item " + (projection.heldItemEquipped() ? "equipped" : "none")
                + " | Movement " + movement
                + " | Capabilities " + capabilities
                + hpState;
        client.player.sendMessage(Text.literal(message), true);
    }

    private static String autoptu$displayInjuries(int injuries) {
        if (injuries <= 0) return "Injuries none";
        return injuries == 1 ? "1 injury" : injuries + " injuries";
    }

    private static String autoptu$displayCanonicalIds(List<String> canonicalIds) {
        String visible = canonicalIds.stream()
                .limit(MAX_CUE_IDS)
                .map(SummaryPtuProjectionMixin::autoptu$displayCanonicalId)
                .collect(Collectors.joining(", "));
        int remaining = canonicalIds.size() - MAX_CUE_IDS;
        return remaining > 0 ? visible + " +" + remaining + " more" : visible;
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
