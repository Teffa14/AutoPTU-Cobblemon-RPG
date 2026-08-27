package io.autoptu.cobblemon.fabric.battle;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.battlecore.BattleAuthoritativeChoiceExecutor;
import io.autoptu.cobblemon.battlecore.BattleAuthoritativeLegalChoiceSource;
import io.autoptu.cobblemon.battlecore.BattleChoiceMenuService;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minecraft-visible battle action menu backed only by AutoPTU-Java legal choices.
 *
 * Normal battle-start wiring owns bind/unbind. The client never supplies trusted battle scope,
 * target legality or action data; it can select only a stable key from a fresh authoritative set.
 */
public final class FabricBattleChoiceRuntime {
    private static final Map<UUID, SessionBinding> ACTIVE = new ConcurrentHashMap<>();
    private static volatile BattleChoiceMenuService menuService;

    private FabricBattleChoiceRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("battle")
                                .then(CommandManager.literal("choices")
                                        .executes(context -> showChoices(context.getSource())))
                                .then(CommandManager.literal("choose")
                                        .then(CommandManager.argument("choiceId", StringArgumentType.greedyString())
                                                .executes(context -> choose(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "choiceId"))))))));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> unbind(handler.player.getUuid()));
    }

    public static synchronized void configure(
            BattleAuthoritativeLegalChoiceSource legalChoiceSource,
            BattleAuthoritativeChoiceExecutor executor
    ) {
        menuService = new BattleChoiceMenuService(legalChoiceSource, executor);
    }

    public static void bind(UUID playerUuid, String reservationId, String actorId) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        ACTIVE.put(playerUuid, new SessionBinding(reservationId, actorId));
    }

    public static void unbind(UUID playerUuid) {
        if (playerUuid != null) ACTIVE.remove(playerUuid);
    }

    public static boolean hasBinding(UUID playerUuid) {
        return playerUuid != null && ACTIVE.containsKey(playerUuid);
    }

    private static int showChoices(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Battle choices must be requested by an authenticated player."));
            return 0;
        }
        BattleChoiceMenuService service = menuService;
        SessionBinding binding = ACTIVE.get(player.getUuid());
        if (service == null || binding == null) {
            source.sendError(Text.literal("No active authoritative AutoPTU battle is bound to this player."));
            return 0;
        }

        try {
            List<BattleChoiceMenuService.Entry> choices = service.choices(binding.reservationId(), binding.actorId());
            if (choices.isEmpty()) {
                player.sendMessage(Text.literal("AutoPTU battle choices: none currently legal."), false);
                return 1;
            }
            player.sendMessage(Text.literal("AutoPTU battle choices"), false);
            for (BattleChoiceMenuService.Entry choice : choices) {
                player.sendMessage(Text.literal(
                        choice.choiceId() + " | " + choice.label()
                                + " | /autoptu battle choose " + choice.choiceId()), false);
            }
            return 1;
        } catch (RuntimeException rejected) {
            source.sendError(Text.literal("Authoritative battle choices unavailable: " + safeMessage(rejected)));
            return 0;
        }
    }

    private static int choose(ServerCommandSource source, String choiceId) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("A battle choice must be submitted by an authenticated player."));
            return 0;
        }
        BattleChoiceMenuService service = menuService;
        SessionBinding binding = ACTIVE.get(player.getUuid());
        if (service == null || binding == null) {
            source.sendError(Text.literal("No active authoritative AutoPTU battle is bound to this player."));
            return 0;
        }

        try {
            BattleChoiceMenuService.Entry selected = service.choose(
                    binding.reservationId(), binding.actorId(), choiceId);
            player.sendMessage(Text.literal("Submitted authoritative choice: " + selected.choiceId()), false);
            return 1;
        } catch (RuntimeException rejected) {
            source.sendError(Text.literal("Battle choice rejected: " + safeMessage(rejected)));
            return 0;
        }
    }

    private static String safeMessage(RuntimeException error) {
        return error.getMessage() == null || error.getMessage().isBlank()
                ? error.getClass().getSimpleName()
                : error.getMessage();
    }

    private record SessionBinding(String reservationId, String actorId) {
        private SessionBinding {
            if (reservationId == null || reservationId.isBlank()) {
                throw new IllegalArgumentException("reservationId must not be blank");
            }
            if (actorId == null || actorId.isBlank()) {
                throw new IllegalArgumentException("actorId must not be blank");
            }
            reservationId = reservationId.strip();
            actorId = actorId.strip();
        }
    }
}
