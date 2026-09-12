package io.autoptu.cobblemon.fabric.admin;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.authority.CanonicalPokemonAdminInspectionService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/** Operator-only, read-only inspection of one persistent canonical Pokemon by server-owned ID. */
public final class FabricPokemonInspectionAdminRuntime {
    private FabricPokemonInspectionAdminRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var pokemonIdArgument = CommandManager.argument("pokemonId", StringArgumentType.word())
                    .executes(context -> inspect(
                            context.getSource(),
                            StringArgumentType.getString(context, "pokemonId")));
            var inspectCommand = CommandManager.literal("inspect").then(pokemonIdArgument);
            var pokemonCommand = CommandManager.literal("pokemon").then(inspectCommand);
            var adminCommand = CommandManager.literal("admin")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(pokemonCommand);
            dispatcher.register(CommandManager.literal("autoptu").then(adminCommand));
        });
    }

    private static int inspect(ServerCommandSource source, String pokemonId) {
        CanonicalPokemonAdminInspectionService.Inspection pokemon;
        try {
            pokemon = new CanonicalPokemonAdminInspectionService(
                    FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(source.getServer()))
                    .inspect(pokemonId)
                    .orElse(null);
        } catch (RuntimeException inconsistentState) {
            source.sendError(Text.literal("Canonical Pokemon state failed a repository consistency read: "
                    + safeMessage(inconsistentState)));
            return 0;
        }

        if (pokemon == null) {
            source.sendError(Text.literal("No canonical AutoPTU Pokemon exists with id " + pokemonId + "."));
            return 0;
        }

        source.sendFeedback(() -> Text.literal("AutoPTU Pokemon inspection — " + pokemon.pokemonId()), false);
        source.sendFeedback(() -> Text.literal("Owner: " + pokemon.ownerPlayerId()
                + " | species " + pokemon.speciesId()
                + " | level " + pokemon.level()
                + " | revision " + pokemon.revision()), false);
        source.sendFeedback(() -> Text.literal("HP: " + health(pokemon)
                + " | status " + listOr(pokemon.statuses(), "clear")
                + " | injuries " + (pokemon.injuryState() == null ? "unavailable" : pokemon.injuryState().injuries())), false);
        source.sendFeedback(() -> Text.literal("Combat stats: " + (pokemon.combatStats() == null ? "unavailable" : "available")
                + " | movement: " + (pokemon.baseMovement() == null ? "unavailable" : "available")
                + " | accuracy/evasion: " + (pokemon.accuracyEvasion() == null ? "unavailable" : "available")), false);
        source.sendFeedback(() -> Text.literal("Types/abilities: " + (pokemon.battleTraits() == null ? "unavailable" : "available")
                + " | moves: " + (pokemon.moveLoadout() == null ? "unavailable" : listOr(pokemon.moveLoadout().moveIds(), "none"))), false);
        source.sendFeedback(() -> Text.literal("Capabilities: " + listOr(pokemon.capabilities(), "none")
                + " | held item: " + (pokemon.heldItemInstanceId() == null ? "none" : pokemon.heldItemInstanceId())), false);
        source.sendFeedback(() -> Text.literal(
                "Read-only canonical Pokemon inspection complete; Cobblemon state and PTU legality were not consulted."), false);
        return 1;
    }

    private static String health(CanonicalPokemonAdminInspectionService.Inspection pokemon) {
        return pokemon.health() == null ? "unavailable" : pokemon.health().currentHp() + "/" + pokemon.health().maxHp();
    }

    private static String listOr(java.util.List<String> values, String fallback) {
        return values == null || values.isEmpty() ? fallback : String.join(", ", values);
    }

    private static String safeMessage(RuntimeException error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }
}
