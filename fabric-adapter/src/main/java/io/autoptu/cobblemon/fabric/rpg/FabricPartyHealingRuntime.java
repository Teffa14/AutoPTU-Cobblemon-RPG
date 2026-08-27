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

/** Minecraft-facing out-of-battle healing service backed only by canonical server state. */
public final class FabricPartyHealingRuntime {
    private FabricPartyHealingRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("healparty")
                                .executes(context -> heal(context.getSource())))));
    }

    private static int heal(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Party healing must be requested by an authenticated player."));
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
                        "Some canonical party members were not healed safely: "
                                + String.join(", ", decision.failedPokemonIds())));
                yield decision.changedState() ? 1 : 0;
            }
            case NO_PARTY -> {
                source.sendError(Text.literal(
                        "No persistent AutoPTU party is configured yet. Enter a canonical encounter/party flow first."));
                yield 0;
            }
            case INVALID_REQUEST -> {
                source.sendError(Text.literal("AutoPTU rejected the healing request: " + decision.reason()));
                yield 0;
            }
        };
    }

    private static String successMessage(CanonicalPartyHealingDecision decision) {
        return "AutoPTU healing complete: "
                + decision.healedPokemon() + " healed, "
                + decision.alreadyFullPokemon() + " already at full HP. "
                + "Statuses and injuries were left unchanged.";
    }
}
