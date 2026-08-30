package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalCareStatusService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** `/autoptu care status` fallback projection of persistent server-owned Pokemon care state. */
public final class FabricCareStatusRuntime {
    private FabricCareStatusRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("care")
                                .then(CommandManager.literal("status")
                                        .executes(context -> show(context.getSource()))))));
    }

    private static int show(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null || player.getServer() == null) {
            source.sendError(Text.literal("Care status must be requested by an authenticated player."));
            return 0;
        }

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        CanonicalCareStatusService service = new CanonicalCareStatusService(
                FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(player.getServer()),
                FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(player.getServer())
        );
        CanonicalCareStatusService.Summary summary;
        try {
            summary = service.findStatus(playerId).orElse(null);
        } catch (IllegalStateException inconsistentState) {
            source.sendError(Text.literal("Canonical care status is unavailable: " + inconsistentState.getMessage()));
            return 0;
        }
        if (summary == null) {
            source.sendError(Text.literal("No persistent canonical party is configured."));
            return 0;
        }

        for (String line : formatLines(summary)) player.sendMessage(Text.literal(line), false);
        return 1;
    }

    static List<String> formatLines(CanonicalCareStatusService.Summary summary) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("AutoPTU care status");
        if (summary.members().isEmpty()) {
            lines.add("Party: empty");
        } else {
            for (CanonicalCareStatusService.Member member : summary.members()) {
                String hp = member.hasHealth() ? member.currentHp() + "/" + member.maxHp() : "unavailable";
                String statuses = member.statuses().isEmpty() ? "none" : String.join(", ", member.statuses());
                String injuries = member.hasInjuryState() ? Integer.toString(member.injuries()) : "unavailable";
                lines.add("Slot " + member.slot() + " " + member.speciesId()
                        + " | HP " + hp
                        + " | statuses " + statuses
                        + " | injuries " + injuries
                        + " | Pokemon revision " + member.pokemonRevision());
            }
        }
        lines.add("Party revision: " + summary.partyRevision());
        lines.add("Recovery eligibility/effects: authoritative PTU contract required");
        return List.copyOf(lines);
    }
}
