package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalStarterCatalogue;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.stream.Collectors;

/** Minecraft-facing read-only starter catalogue backed by server-owned configuration. */
public final class FabricStarterCatalogueRuntime {
    private static final CanonicalStarterCatalogue CATALOGUE = new CanonicalStarterCatalogue();

    private FabricStarterCatalogueRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("starter")
                                .then(CommandManager.literal("list")
                                        .executes(context -> list(context.getSource()))))));
    }

    private static int list(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Starter catalogue must be requested by an authenticated player."));
            return 0;
        }

        String choices = CATALOGUE.configuredStarters().stream()
                .map(option -> option.displayName() + " (" + option.speciesId() + ")")
                .collect(Collectors.joining(", "));
        player.sendMessage(Text.literal("AutoPTU configured starters: " + choices), false);
        return CATALOGUE.configuredStarters().size();
    }
}
