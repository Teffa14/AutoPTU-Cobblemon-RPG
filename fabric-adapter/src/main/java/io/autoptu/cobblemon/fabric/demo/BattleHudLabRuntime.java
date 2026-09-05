package io.autoptu.cobblemon.fabric.demo;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.fabric.network.FabricBattleHudNetworking;
import io.autoptu.cobblemon.fabric.network.FabricBattleHudPayload;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Operator-only QA controls for the AutoPTU-owned Cobblemon-style battle HUD. */
public final class BattleHudLabRuntime {
    private static final int DEFAULT_HP = 100;
    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    private BattleHudLabRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("admin")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("animation")
                                        .then(CommandManager.literal("ui")
                                                .executes(context -> help(context.getSource()))
                                                .then(CommandManager.literal("start")
                                                        .then(CommandManager.argument("left", StringArgumentType.word())
                                                                .then(CommandManager.argument("right", StringArgumentType.word())
                                                                        .executes(context -> start(
                                                                                context.getSource(),
                                                                                StringArgumentType.getString(context, "left"),
                                                                                StringArgumentType.getString(context, "right"))))))
                                                .then(CommandManager.literal("hp")
                                                        .then(CommandManager.argument("leftHp", IntegerArgumentType.integer(0))
                                                                .then(CommandManager.argument("rightHp", IntegerArgumentType.integer(0))
                                                                        .executes(context -> hp(
                                                                                context.getSource(),
                                                                                IntegerArgumentType.getInteger(context, "leftHp"),
                                                                                IntegerArgumentType.getInteger(context, "rightHp"))))))
                                                .then(CommandManager.literal("max")
                                                        .then(CommandManager.argument("leftMax", IntegerArgumentType.integer(1))
                                                                .then(CommandManager.argument("rightMax", IntegerArgumentType.integer(1))
                                                                        .executes(context -> max(
                                                                                context.getSource(),
                                                                                IntegerArgumentType.getInteger(context, "leftMax"),
                                                                                IntegerArgumentType.getInteger(context, "rightMax"))))))
                                                .then(CommandManager.literal("damage")
                                                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                                                                .executes(context -> damage(
                                                                        context.getSource(),
                                                                        IntegerArgumentType.getInteger(context, "amount")))))
                                                .then(CommandManager.literal("status")
                                                        .then(CommandManager.argument("leftStatus", StringArgumentType.word())
                                                                .then(CommandManager.argument("rightStatus", StringArgumentType.word())
                                                                        .executes(context -> status(
                                                                                context.getSource(),
                                                                                StringArgumentType.getString(context, "leftStatus"),
                                                                                StringArgumentType.getString(context, "rightStatus"))))))
                                                .then(CommandManager.literal("message")
                                                        .then(CommandManager.argument("text", StringArgumentType.greedyString())
                                                                .executes(context -> message(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(context, "text")))))
                                                .then(CommandManager.literal("swap")
                                                        .executes(context -> swap(context.getSource())))
                                                .then(CommandManager.literal("reset")
                                                        .executes(context -> reset(context.getSource())))
                                                .then(CommandManager.literal("hide")
                                                        .executes(context -> hide(context.getSource()))))))));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> STATES.remove(handler.player.getUuid()));
    }

    private static int help(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("Animation UI QA: start <left> <right> | hp <left> <right> | max <left> <right> | damage <amount> | status <left|none> <right|none> | message <text> | swap | reset | hide"), false);
        return 1;
    }

    private static int start(ServerCommandSource source, String left, String right) {
        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) return 0;
        State state = new State(display(left), normalize(left), display(right), normalize(right));
        STATES.put(player.getUuid(), state);
        send(player, state, "Cobblemon-style AutoPTU HUD ready");
        return 1;
    }

    private static int hp(ServerCommandSource source, int leftHp, int rightHp) {
        State state = requireState(source);
        if (state == null) return 0;
        if (leftHp > state.leftMax || rightHp > state.rightMax) {
            source.sendError(Text.literal("QA HP cannot exceed the configured QA max. Use ui max first."));
            return 0;
        }
        state.leftHp = leftHp;
        state.rightHp = rightHp;
        send(source.getPlayer(), state, "QA HP updated");
        return 1;
    }

    private static int max(ServerCommandSource source, int leftMax, int rightMax) {
        State state = requireState(source);
        if (state == null) return 0;
        state.leftMax = leftMax;
        state.rightMax = rightMax;
        state.leftHp = Math.min(state.leftHp, leftMax);
        state.rightHp = Math.min(state.rightHp, rightMax);
        send(source.getPlayer(), state, "QA max HP updated");
        return 1;
    }

    private static int damage(ServerCommandSource source, int amount) {
        State state = requireState(source);
        if (state == null) return 0;
        state.rightHp = Math.max(0, state.rightHp - amount);
        send(source.getPlayer(), state, "QA target damage " + amount);
        return 1;
    }

    private static int status(ServerCommandSource source, String left, String right) {
        State state = requireState(source);
        if (state == null) return 0;
        state.leftStatus = optional(left);
        state.rightStatus = optional(right);
        send(source.getPlayer(), state, "QA status updated");
        return 1;
    }

    private static int message(ServerCommandSource source, String message) {
        State state = requireState(source);
        if (state == null) return 0;
        state.message = message == null ? "" : message.strip();
        send(source.getPlayer(), state, state.message);
        return 1;
    }

    private static int swap(ServerCommandSource source) {
        State state = requireState(source);
        if (state == null) return 0;
        state.swap();
        send(source.getPlayer(), state, "QA sides swapped");
        return 1;
    }

    private static int reset(ServerCommandSource source) {
        State state = requireState(source);
        if (state == null) return 0;
        state.leftHp = state.leftMax;
        state.rightHp = state.rightMax;
        state.leftStatus = "";
        state.rightStatus = "";
        state.message = "QA HUD reset";
        send(source.getPlayer(), state, state.message);
        return 1;
    }

    private static int hide(ServerCommandSource source) {
        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) return 0;
        STATES.remove(player.getUuid());
        FabricBattleHudNetworking.clear(player);
        return 1;
    }

    private static State requireState(ServerCommandSource source) {
        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) return null;
        State state = STATES.get(player.getUuid());
        if (state == null) source.sendError(Text.literal("Start the animation UI QA first."));
        return state;
    }

    private static ServerPlayerEntity requirePlayer(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) source.sendError(Text.literal("Battle HUD QA requires an in-game operator."));
        return player;
    }

    private static void send(ServerPlayerEntity player, State state, String message) {
        if (player == null) return;
        FabricBattleHudNetworking.send(player, state.payload(message));
    }

    private static String normalize(String value) {
        if (value == null) return "";
        String normalized = value.strip().toLowerCase().replaceAll("[^a-z0-9_-]", "");
        return normalized;
    }

    private static String display(String value) {
        String normalized = normalize(value);
        if (normalized.isBlank()) return "AutoPTU";
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private static String optional(String value) {
        String normalized = normalize(value);
        return "none".equals(normalized) ? "" : normalized;
    }

    private static final class State {
        private String leftName;
        private String leftSpecies;
        private String rightName;
        private String rightSpecies;
        private int leftHp = DEFAULT_HP;
        private int rightHp = DEFAULT_HP;
        private int leftMax = DEFAULT_HP;
        private int rightMax = DEFAULT_HP;
        private String leftStatus = "";
        private String rightStatus = "";
        private String message = "";

        private State(String leftName, String leftSpecies, String rightName, String rightSpecies) {
            this.leftName = leftName;
            this.leftSpecies = leftSpecies;
            this.rightName = rightName;
            this.rightSpecies = rightSpecies;
        }

        private FabricBattleHudPayload payload(String fallbackMessage) {
            String shown = message.isBlank() ? fallbackMessage : message;
            return new FabricBattleHudPayload(
                    true,
                    new FabricBattleHudPayload.Combatant(leftName, leftSpecies, 50, leftHp, leftMax, leftStatus),
                    new FabricBattleHudPayload.Combatant(rightName, rightSpecies, 50, rightHp, rightMax, rightStatus),
                    shown,
                    true
            );
        }

        private void swap() {
            String oldName = leftName; leftName = rightName; rightName = oldName;
            String oldSpecies = leftSpecies; leftSpecies = rightSpecies; rightSpecies = oldSpecies;
            int oldHp = leftHp; leftHp = rightHp; rightHp = oldHp;
            int oldMax = leftMax; leftMax = rightMax; rightMax = oldMax;
            String oldStatus = leftStatus; leftStatus = rightStatus; rightStatus = oldStatus;
        }
    }
}
