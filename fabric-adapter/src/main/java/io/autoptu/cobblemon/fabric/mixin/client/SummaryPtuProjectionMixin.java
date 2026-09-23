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

/**
 * Keeps disposable AutoPTU Pokemon on Cobblemon's native STATS surface.
 *
 * Other Summary tabs expose Cobblemon-owned nature/IV/EV/marks/edit data that is not yet part of the
 * durable PTU read model. Rather than fabricate those values, projected Pokemon remain on the exact
 * level/HP/combat-stat surface that AutoPTU can authoritatively supply. Canonical conditions, moves
 * and combat stats are surfaced as read-only action-bar context when the selection changes and remain
 * visible when an unsupported native tab is rejected.
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

        String statuses = projection.statuses().isEmpty()
                ? "clear"
                : projection.statuses().stream()
                        .map(SummaryPtuProjectionMixin::autoptu$displayCanonicalId)
                        .collect(Collectors.joining(", "));
        String moves = projection.moveIds().isEmpty()
                ? "unavailable"
                : projection.moveIds().stream()
                        .map(SummaryPtuProjectionMixin::autoptu$displayCanonicalId)
                        .collect(Collectors.joining(", "));
        String message = "PTU Lv " + projection.level()
                + " | HP " + projection.currentHp() + "/" + projection.maxHp()
                + " | Atk " + projection.atk()
                + " Def " + projection.def()
                + " SpA " + projection.spatk()
                + " SpD " + projection.spdef()
                + " Spd " + projection.spd()
                + " | Moves " + moves
                + " | Status " + statuses
                + " | Injuries " + projection.injuries();
        if (blockedTab) {
            message += " | Read-only PTU Summary: unsupported Cobblemon tab unavailable";
        }
        client.player.sendMessage(Text.literal(message), true);
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
