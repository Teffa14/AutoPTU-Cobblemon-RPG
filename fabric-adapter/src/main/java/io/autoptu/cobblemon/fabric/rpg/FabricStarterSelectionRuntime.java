package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import io.autoptu.cobblemon.authority.CanonicalStarterCatalogue;
import io.autoptu.cobblemon.authority.CanonicalStarterSelectionDecision;
import io.autoptu.cobblemon.authority.CanonicalStarterSelectionService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Minecraft-facing one-time starter claim backed only by server-owned canonical state. */
public final class FabricStarterSelectionRuntime {
    private static final CanonicalStarterCatalogue CATALOGUE = new CanonicalStarterCatalogue();

    private FabricStarterSelectionRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("starter")
                                .then(CommandManager.literal("choose")
                                        .then(CommandManager.argument("species", IdentifierArgumentType.identifier())
                                                .executes(context -> choose(
                                                        context.getSource(),
                                                        IdentifierArgumentType.getIdentifier(context, "species")
                                                )))))));
    }

    private static int choose(ServerCommandSource source, Identifier requestedSpecies) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Starter selection must be requested by an authenticated player."));
            return 0;
        }

        String requestedId = requestedSpecies.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)
                ? requestedSpecies.getPath()
                : requestedSpecies.toString();
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        BlockPos pos = player.getBlockPos();
        BattleArenaSnapshot arena = new BattleArenaSnapshot(
                player.getServerWorld().getRegistryKey().getValue().toString(),
                pos.getX(), pos.getY(), pos.getZ(),
                1, 0,
                0, 1
        );
        CanonicalStarterSelectionService service = new CanonicalStarterSelectionService(
                CATALOGUE,
                FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(player.getServer()),
                FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(player.getServer())
        );
        CanonicalStarterSelectionDecision decision = service.choose(playerId, requestedId, arena);

        return switch (decision.outcome()) {
            case CHOSEN -> {
                player.sendMessage(Text.literal(
                        "AutoPTU starter chosen: " + displayName(decision.speciesId())
                                + ". Your persistent canonical party now contains this Pokemon."), false);
                yield 1;
            }
            case ALREADY_CHOSEN -> {
                source.sendError(Text.literal(
                        decision.speciesId().isBlank()
                                ? "A persistent AutoPTU party already exists for this player."
                                : "Starter already chosen: " + displayName(decision.speciesId()) + "."));
                yield 0;
            }
            case INVALID_STARTER -> {
                source.sendError(Text.literal(
                        "That species is not an available starter. Use /autoptu starter list."));
                yield 0;
            }
            case INVALID_REQUEST, CONFLICT -> {
                source.sendError(Text.literal("AutoPTU could not persist the starter claim: " + decision.detail()));
                yield 0;
            }
        };
    }

    private static String displayName(String speciesId) {
        return CATALOGUE.findConfigured(speciesId)
                .map(CanonicalStarterCatalogue.StarterOption::displayName)
                .orElse(speciesId);
    }
}
