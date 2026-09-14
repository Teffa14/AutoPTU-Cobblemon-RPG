package io.autoptu.cobblemon.fabric.admin;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/** Operator-only Minecraft surface for conservative RPG authority readiness. */
public final class FabricFeatureGatesAdminRuntime implements ModInitializer {
    private final FabricFeatureGateService service = new FabricFeatureGateService();

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("admin")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("featuregates")
                                        .executes(context -> emit(context.getSource()))))));
    }

    private int emit(ServerCommandSource source) {
        var gates = service.snapshot();
        source.sendFeedback(() -> Text.literal("AutoPTU Minecraft RPG authority feature gates"), false);
        for (FabricFeatureGateService.Gate gate : gates) {
            source.sendFeedback(() -> Text.literal(
                    gate.id() + " = " + gate.status() + " | " + gate.reason()), false);
        }
        source.sendFeedback(() -> Text.literal(
                "PARTIAL/BLOCKED gates remain fail-closed; Minecraft does not manufacture missing PTU behavior."), false);
        return gates.size();
    }
}
