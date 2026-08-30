package io.autoptu.cobblemon.fabric.battle;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.battlecore.BattleAuthoritativeChoiceExecutor;
import io.autoptu.cobblemon.battlecore.BattleAuthoritativeLegalChoiceSource;
import io.autoptu.cobblemon.battlecore.BattleChoiceMenuService;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.MinecraftServer;
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
 * Minecraft-visible battle action menu and HUD backed only by AutoPTU-Java legal choices.
 *
 * Normal battle-start wiring owns bind/unbind. The client never supplies trusted battle scope,
 * target legality or action data; it can select only a stable key from a fresh authoritative set.
 */
public final class FabricBattleChoiceRuntime {
    private static final Map<UUID, SessionBinding> ACTIVE = new ConcurrentHashMap<>();
    private static final Map<UUID, ServerBossBar> HUDS = new ConcurrentHashMap<>();
    private static volatile BattleChoiceMenuService menuService;
    private static int hudTick;

    private FabricBattleChoiceRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("battle")
                                .then(CommandManager.literal("status")
                                        .executes(context -> showStatus(context.getSource())))
                                .then(CommandManager.literal("choices")
                                        .executes(context -> showChoices(context.getSource())))
                                .then(CommandManager.literal("choose")
                                        .then(CommandManager.argument("choiceId", StringArgumentType.greedyString())
                                                .executes(context -> choose(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "choiceId"))))))));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> unbind(handler.player.getUuid()));
        ServerTickEvents.END_SERVER_TICK.register(FabricBattleChoiceRuntime::refreshHud);
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
        if (playerUuid == null) return;
        ACTIVE.remove(playerUuid);
        ServerBossBar hud = HUDS.remove(playerUuid);
        if (hud != null) hud.clearPlayers();
    }

    public static boolean hasBinding(UUID playerUuid) {
        return playerUuid != null && ACTIVE.containsKey(playerUuid);
    }

    /**
     * Returns the current server-owned Minecraft battle projection for a player.
     *
     * This projection deliberately exposes only binding identity plus the count obtained from a
     * fresh authoritative legal-choice query. It does not infer turn ownership, phase, HP,
     * combatants, winner, faint state or any other PTU fact.
     */
    public static BattleStatusView status(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        SessionBinding binding = ACTIVE.get(playerUuid);
        if (binding == null) {
            return BattleStatusView.unbound();
        }

        BattleChoiceMenuService service = menuService;
        if (service == null) {
            return BattleStatusView.bound(binding.actorId(), null);
        }

        try {
            List<BattleChoiceMenuService.Entry> choices = service.choices(binding.reservationId(), binding.actorId());
            return BattleStatusView.bound(binding.actorId(), choices.size());
        } catch (RuntimeException unavailable) {
            return BattleStatusView.bound(binding.actorId(), null);
        }
    }

    private static void refreshHud(MinecraftServer server) {
        if (++hudTick < 10) return;
        hudTick = 0;

        BattleChoiceMenuService service = menuService;
        for (Map.Entry<UUID, SessionBinding> active : ACTIVE.entrySet()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(active.getKey());
            if (player == null) continue;

            SessionBinding binding = active.getValue();
            ServerBossBar hud = HUDS.computeIfAbsent(active.getKey(), ignored -> {
                ServerBossBar created = new ServerBossBar(
                        Text.literal("AutoPTU battle"),
                        BossBar.Color.BLUE,
                        BossBar.Style.PROGRESS
                );
                created.setPercent(1.0F);
                return created;
            });
            hud.addPlayer(player);

            if (service == null) {
                hud.setName(Text.literal("AutoPTU • authoritative choices unavailable"));
                continue;
            }

            try {
                List<BattleChoiceMenuService.Entry> choices =
                        service.choices(binding.reservationId(), binding.actorId());
                hud.setName(Text.literal(hudTitle(binding.actorId(), choices.size())));
            } catch (RuntimeException unavailable) {
                hud.setName(Text.literal("AutoPTU • authoritative choices unavailable"));
            }
        }
    }

    static String hudTitle(String actorId, int legalChoiceCount) {
        if (actorId == null || actorId.isBlank()) throw new IllegalArgumentException("actorId must not be blank");
        if (legalChoiceCount < 0) throw new IllegalArgumentException("legalChoiceCount cannot be negative");
        return "AutoPTU • " + actorId.strip() + " • legal choices " + legalChoiceCount;
    }

    private static int showStatus(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Battle status must be requested by an authenticated player."));
            return 0;
        }

        BattleStatusView status = status(player.getUuid());
        if (!status.bound()) {
            source.sendError(Text.literal("No active authoritative AutoPTU battle is bound to this player."));
            return 0;
        }

        player.sendMessage(Text.literal("AutoPTU battle status"), false);
        player.sendMessage(Text.literal("bound actor: " + status.actorId()), false);
        if (status.authoritativeLegalChoiceCount() == null) {
            player.sendMessage(Text.literal("authoritative legal choices: unavailable"), false);
        } else {
            player.sendMessage(Text.literal(
                    "authoritative legal choices: " + status.authoritativeLegalChoiceCount()), false);
        }
        player.sendMessage(Text.literal(
                "turn, HP, faint and result: unavailable unless emitted by authoritative battle state"), false);
        return 1;
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

    public record BattleStatusView(boolean bound, String actorId, Integer authoritativeLegalChoiceCount) {
        private static BattleStatusView unbound() {
            return new BattleStatusView(false, null, null);
        }

        private static BattleStatusView bound(String actorId, Integer authoritativeLegalChoiceCount) {
            if (actorId == null || actorId.isBlank()) {
                throw new IllegalArgumentException("actorId must not be blank");
            }
            if (authoritativeLegalChoiceCount != null && authoritativeLegalChoiceCount < 0) {
                throw new IllegalArgumentException("authoritativeLegalChoiceCount cannot be negative");
            }
            return new BattleStatusView(true, actorId.strip(), authoritativeLegalChoiceCount);
        }
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
