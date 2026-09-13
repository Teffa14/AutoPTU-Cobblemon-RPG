package io.autoptu.cobblemon.fabric.admin;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Operator-visible build capability gates for the Minecraft RPG adapter.
 *
 * <p>These gates are deliberately conservative. They describe which authority boundaries this
 * adapter build is allowed to exercise; they do not infer PTU support from Cobblemon state or
 * promote representative upstream support to a complete rules category.</p>
 */
public final class FabricFeatureGatesAdminRuntime implements ModInitializer {
    private enum GateStatus {
        ENABLED,
        PARTIAL,
        BLOCKED
    }

    private record FeatureGate(String id, GateStatus status, String reason) {}

    private static final List<FeatureGate> GATES = List.of(
            new FeatureGate(
                    "persistent_rpg_state",
                    GateStatus.ENABLED,
                    "Trainer, Pokemon, party/storage, inventory, wallet and progression use server-owned durable state."),
            new FeatureGate(
                    "wild_world_projection",
                    GateStatus.PARTIAL,
                    "Server-authored wild projection is live; broader species coverage requires complete canonical WILD blueprints."),
            new FeatureGate(
                    "autoptu_battle_authority",
                    GateStatus.PARTIAL,
                    "Minecraft delegates battle legality/outcomes to AutoPTU-Java; upstream rule-category coverage remains incomplete."),
            new FeatureGate(
                    "durable_battle_recovery",
                    GateStatus.BLOCKED,
                    "No authoritative persisted battle checkpoint/recovery contract exists."),
            new FeatureGate(
                    "ptu_item_effects",
                    GateStatus.BLOCKED,
                    "Canonical inventory is durable, but Minecraft must not invent authoritative PTU item-use behavior."),
            new FeatureGate(
                    "full_ptu_action_economy",
                    GateStatus.BLOCKED,
                    "Representative upstream action support does not establish complete action-economy and lifecycle coverage."));

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("admin")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("featuregates")
                                        .executes(context -> emit(context.getSource()))))));
    }

    private static int emit(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("AutoPTU Minecraft RPG authority feature gates"), false);
        for (FeatureGate gate : GATES) {
            source.sendFeedback(() -> Text.literal(
                    gate.id() + " = " + gate.status() + " | " + gate.reason()), false);
        }
        source.sendFeedback(() -> Text.literal(
                "Gate output is read-only; BLOCKED/PARTIAL capabilities must remain fail-closed in Minecraft."), false);
        return GATES.size();
    }
}
