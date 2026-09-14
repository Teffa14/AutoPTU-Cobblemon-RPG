package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalPartyHealingDecision;
import io.autoptu.cobblemon.authority.CanonicalPartyHealingService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Minecraft-facing out-of-battle healing service backed only by canonical server state. */
public final class FabricPartyHealingRuntime {
    private FabricPartyHealingRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("autoptu")
                    .then(CommandManager.literal("healparty")
                            .executes(context -> healSelf(context.getSource()))));

            dispatcher.register(CommandManager.literal("autoptu")
                    .then(CommandManager.literal("admin")
                            .requires(source -> source.hasPermissionLevel(2))
                            .then(CommandManager.literal("healparty")
                                    .executes(context -> healSelf(context.getSource()))
                                    .then(CommandManager.argument("player", EntityArgumentType.player())
                                            .executes(context -> heal(
                                                    context.getSource(),
                                                    EntityArgumentType.getPlayer(context, "player"),
                                                    true))))));
        });
    }

    private static int healSelf(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Party healing must target an authenticated player."));
            return 0;
        }
        return heal(source, player, false);
    }

    private static int heal(ServerCommandSource source, ServerPlayerEntity target, boolean administrative) {
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(target.getUuid());
        CanonicalPartyHealingService service = new CanonicalPartyHealingService(
                FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(target.getServer()),
                FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(target.getServer())
        );
        CanonicalPartyHealingDecision decision = service.healParty(playerId);

        return switch (decision.outcome()) {
            case APPLIED -> {
                String message = successMessage(decision);
                target.sendMessage(Text.literal(message), false);
                sendAdministrativeConfirmation(source, target, administrative, message);
                yield 1;
            }
            case PARTIAL -> {
                String message = successMessage(decision);
                target.sendMessage(Text.literal(message), false);
                sendAdministrativeConfirmation(source, target, administrative, message);
                source.sendError(Text.literal(
                        "Some canonical party members were not healed safely: "
                                + String.join(", ", decision.failedPokemonIds())));
                yield decision.changedState() ? 1 : 0;
            }
            case NO_PARTY -> {
                source.sendError(Text.literal(
                        "No persistent AutoPTU party is configured for " + target.getName().getString() + "."));
                yield 0;
            }
            case INVALID_REQUEST -> {
                source.sendError(Text.literal("AutoPTU rejected the healing request: " + decision.reason()));
                yield 0;
            }
        };
    }

    private static void sendAdministrativeConfirmation(
            ServerCommandSource source,
            ServerPlayerEntity target,
            boolean administrative,
            String message
    ) {
        if (!administrative) return;
        ServerPlayerEntity sourcePlayer = source.getPlayer();
        if (sourcePlayer != null && sourcePlayer.getUuid().equals(target.getUuid())) return;
        source.sendFeedback(
                () -> Text.literal("Healed canonical party for " + target.getName().getString() + ". " + message),
                false
        );
    }

    private static String successMessage(CanonicalPartyHealingDecision decision) {
        return "AutoPTU healing complete: "
                + decision.healedPokemon() + " healed, "
                + decision.alreadyFullPokemon() + " already at full HP. "
                + "Statuses and injuries were left unchanged.";
    }
}
