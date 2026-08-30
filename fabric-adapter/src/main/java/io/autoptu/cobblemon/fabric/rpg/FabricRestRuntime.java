package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalPartyHealingDecision;
import io.autoptu.cobblemon.authority.CanonicalPartyHealingService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Bounded server-authoritative rest command.
 *
 * <p>The command restores only canonical party HP through the existing RPG healing authority.
 * It deliberately does not clear statuses, injuries, battle-scoped conditions, or infer any PTU
 * recovery from Minecraft sleep/time. Those mutations require an explicit authoritative contract.
 */
public final class FabricRestRuntime {
    private FabricRestRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("rest")
                                .executes(context -> rest(context.getSource())))));
    }

    private static int rest(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Rest must be requested by an authenticated player."));
            return 0;
        }

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        CanonicalPartyHealingService service = new CanonicalPartyHealingService(
                FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(player.getServer()),
                FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(player.getServer())
        );
        CanonicalPartyHealingDecision decision = service.healParty(playerId);

        return switch (decision.outcome()) {
            case APPLIED -> {
                player.sendMessage(Text.literal(successMessage(decision)), false);
                yield 1;
            }
            case PARTIAL -> {
                player.sendMessage(Text.literal(successMessage(decision)), false);
                source.sendError(Text.literal(
                        "Rest could not safely update these canonical party members: "
                                + String.join(", ", decision.failedPokemonIds())));
                yield decision.changedState() ? 1 : 0;
            }
            case NO_PARTY -> {
                source.sendError(Text.literal(
                        "No persistent AutoPTU party is configured for this Trainer."));
                yield 0;
            }
            case INVALID_REQUEST -> {
                source.sendError(Text.literal("AutoPTU rejected the rest request: " + decision.reason()));
                yield 0;
            }
        };
    }

    static String successMessage(CanonicalPartyHealingDecision decision) {
        return "Rest complete: "
                + decision.healedPokemon() + " Pokemon restored to full HP, "
                + decision.alreadyFullPokemon() + " already at full HP. "
                + "Canonical statuses and injuries remain unchanged; additional PTU recovery requires an authoritative recovery contract.";
    }
}
