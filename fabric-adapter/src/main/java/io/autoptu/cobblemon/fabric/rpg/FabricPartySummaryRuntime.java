package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalPartyQueryService;
import io.autoptu.cobblemon.authority.CanonicalPartySummary;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Locale;

/** Minecraft-facing read-only projection of the durable canonical party. */
public final class FabricPartySummaryRuntime {
    private FabricPartySummaryRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("party")
                                .executes(context -> show(context.getSource())))));
    }

    private static int show(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Party inspection must be requested by an authenticated player."));
            return 0;
        }

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        CanonicalPartyQueryService service = new CanonicalPartyQueryService(
                FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(player.getServer()),
                FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(player.getServer())
        );

        CanonicalPartySummary party;
        try {
            party = service.findParty(playerId).orElse(null);
        } catch (IllegalStateException invalidCanonicalState) {
            source.sendError(Text.literal("AutoPTU party state is inconsistent and cannot be displayed safely."));
            return 0;
        }
        if (party == null || party.members().isEmpty()) {
            source.sendError(Text.literal("No persistent AutoPTU party exists yet. Use /autoptu starter list first."));
            return 0;
        }

        player.sendMessage(Text.literal("AutoPTU party — " + party.members().size() + " member(s)"), false);
        for (CanonicalPartySummary.Member member : party.members()) {
            player.sendMessage(Text.literal(format(member)), false);
        }
        return 1;
    }

    static String format(CanonicalPartySummary.Member member) {
        String hp = member.hasHealth() ? member.currentHp() + "/" + member.maxHp() + " HP" : "HP unavailable";
        String statuses = member.statuses().isEmpty() ? "clear" : String.join(", ", member.statuses());
        return "[" + member.slot() + "] " + displayName(member.speciesId())
                + " Lv." + member.level() + " | " + hp + " | status: " + statuses;
    }

    private static String displayName(String speciesId) {
        if (speciesId == null || speciesId.isBlank()) return "Unknown";
        String path = speciesId.contains(":") ? speciesId.substring(speciesId.indexOf(':') + 1) : speciesId;
        if (path.isEmpty()) return speciesId;
        return path.substring(0, 1).toUpperCase(Locale.ROOT) + path.substring(1);
    }
}
