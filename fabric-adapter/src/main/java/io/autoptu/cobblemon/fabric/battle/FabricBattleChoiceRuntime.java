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
 * Minecraft-visible battle action menu and read-only spectating HUD backed only by AutoPTU-Java
 * legal choices.
 *
 * Normal battle-start wiring owns participant bind/unbind. The client never supplies trusted battle
 * scope, target legality or action data. Participants can select only a stable key from a fresh
 * authoritative set. Spectators can request only a server-generated opaque spectate ID and never
 * receive a participant binding.
 */
public final class FabricBattleChoiceRuntime {
    private static final Map<UUID, SessionBinding> ACTIVE = new ConcurrentHashMap<>();
    private static final Map<UUID, String> SPECTATORS = new ConcurrentHashMap<>();
    private static final Map<UUID, ServerBossBar> HUDS = new ConcurrentHashMap<>();
    private static final Map<UUID, ServerBossBar> SPECTATOR_HUDS = new ConcurrentHashMap<>();
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
                                                        StringArgumentType.getString(context, "choiceId")))))
                                .then(CommandManager.literal("spectate")
                                        .then(CommandManager.argument("battleId", StringArgumentType.word())
                                                .executes(context -> spectate(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "battleId"))))))));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> clearPlayer(handler.player.getUuid()));
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
        SessionBinding existing = ACTIVE.get(playerUuid);
        String spectateId = existing != null
                && existing.reservationId().equals(normalize(reservationId, "reservationId"))
                && existing.actorId().equals(normalize(actorId, "actorId"))
                ? existing.spectateId()
                : UUID.randomUUID().toString();
        ACTIVE.put(playerUuid, new SessionBinding(reservationId, actorId, spectateId));
        stopSpectating(playerUuid);
    }

    public static void unbind(UUID playerUuid) {
        if (playerUuid == null) return;
        SessionBinding removed = ACTIVE.remove(playerUuid);
        ServerBossBar hud = HUDS.remove(playerUuid);
        if (hud != null) hud.clearPlayers();
        if (removed != null && ACTIVE.values().stream().noneMatch(binding -> binding.spectateId().equals(removed.spectateId()))) {
            for (Map.Entry<UUID, String> entry : List.copyOf(SPECTATORS.entrySet())) {
                if (removed.spectateId().equals(entry.getValue())) stopSpectating(entry.getKey());
            }
        }
    }

    public static boolean hasBinding(UUID playerUuid) {
        return playerUuid != null && ACTIVE.containsKey(playerUuid);
    }

    public static String spectateId(UUID playerUuid) {
        if (playerUuid == null) return null;
        SessionBinding binding = ACTIVE.get(playerUuid);
        return binding == null ? null : binding.spectateId();
    }

    public static boolean beginSpectating(UUID playerUuid, String battleId) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        String requested = normalize(battleId, "battleId");
        if (ACTIVE.containsKey(playerUuid)) return false;
        SessionBinding binding = findBySpectateId(requested);
        if (binding == null) return false;
        SPECTATORS.put(playerUuid, requested);
        return true;
    }

    public static void stopSpectating(UUID playerUuid) {
        if (playerUuid == null) return;
        SPECTATORS.remove(playerUuid);
        ServerBossBar hud = SPECTATOR_HUDS.remove(playerUuid);
        if (hud != null) hud.clearPlayers();
    }

    public static BattleStatusView spectatorStatus(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        String spectateId = SPECTATORS.get(playerUuid);
        if (spectateId == null) return BattleStatusView.unbound();
        SessionBinding binding = findBySpectateId(spectateId);
        if (binding == null) {
            stopSpectating(playerUuid);
            return BattleStatusView.unbound();
        }
        return status(binding);
    }

    /**
     * Returns the current server-owned Minecraft battle projection for a participant.
     *
     * This projection deliberately exposes only binding identity plus the count obtained from a
     * fresh authoritative legal-choice query. It does not infer turn ownership, phase, HP,
     * combatants, winner, faint state or any other PTU fact.
     */
    public static BattleStatusView status(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        SessionBinding binding = ACTIVE.get(playerUuid);
        return binding == null ? BattleStatusView.unbound() : status(binding);
    }

    private static BattleStatusView status(SessionBinding binding) {
        BattleChoiceMenuService service = menuService;
        if (service == null) return BattleStatusView.bound(binding.actorId(), null);
        try {
            List<BattleChoiceMenuService.Entry> choices = service.choices(binding.reservationId(), binding.actorId());
            return BattleStatusView.bound(binding.actorId(), choices.size());
        } catch (RuntimeException unavailable) {
            return BattleStatusView.bound(binding.actorId(), null);
        }
    }

    private static SessionBinding findBySpectateId(String spectateId) {
        for (SessionBinding binding : ACTIVE.values()) {
            if (binding.spectateId().equals(spectateId)) return binding;
        }
        return null;
    }

    private static void refreshHud(MinecraftServer server) {
        if (++hudTick < 10) return;
        hudTick = 0;
        refreshParticipantHud(server);
        refreshSpectatorHud(server);
    }

    private static void refreshParticipantHud(MinecraftServer server) {
        BattleChoiceMenuService service = menuService;
        for (Map.Entry<UUID, SessionBinding> active : ACTIVE.entrySet()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(active.getKey());
            if (player == null) continue;

            SessionBinding binding = active.getValue();
            ServerBossBar hud = HUDS.computeIfAbsent(active.getKey(), ignored -> createHud("AutoPTU battle"));
            hud.addPlayer(player);

            if (service == null) {
                hud.setName(Text.literal("AutoPTU • authoritative choices unavailable"));
                continue;
            }

            try {
                List<BattleChoiceMenuService.Entry> choices = service.choices(binding.reservationId(), binding.actorId());
                hud.setName(Text.literal(hudTitle(binding.actorId(), choices.size())));
            } catch (RuntimeException unavailable) {
                hud.setName(Text.literal("AutoPTU • authoritative choices unavailable"));
            }
        }
    }

    private static void refreshSpectatorHud(MinecraftServer server) {
        for (Map.Entry<UUID, String> entry : List.copyOf(SPECTATORS.entrySet())) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null) continue;
            SessionBinding binding = findBySpectateId(entry.getValue());
            if (binding == null) {
                stopSpectating(entry.getKey());
                continue;
            }

            BattleStatusView status = status(binding);
            ServerBossBar hud = SPECTATOR_HUDS.computeIfAbsent(entry.getKey(), ignored -> createHud("AutoPTU spectating"));
            hud.addPlayer(player);
            String choices = status.authoritativeLegalChoiceCount() == null
                    ? "authoritative choices unavailable"
                    : "legal choices " + status.authoritativeLegalChoiceCount();
            hud.setName(Text.literal("AutoPTU spectating • " + status.actorId() + " • " + choices));
        }
    }

    private static ServerBossBar createHud(String title) {
        ServerBossBar created = new ServerBossBar(Text.literal(title), BossBar.Color.BLUE, BossBar.Style.PROGRESS);
        created.setPercent(1.0F);
        return created;
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
            status = spectatorStatus(player.getUuid());
            if (!status.bound()) {
                source.sendError(Text.literal("No active authoritative AutoPTU battle is bound to this player."));
                return 0;
            }
            player.sendMessage(Text.literal("AutoPTU battle status (spectator, read-only)"), false);
        } else {
            player.sendMessage(Text.literal("AutoPTU battle status"), false);
            player.sendMessage(Text.literal("spectate id: " + spectateId(player.getUuid())), false);
        }

        player.sendMessage(Text.literal("bound actor: " + status.actorId()), false);
        if (status.authoritativeLegalChoiceCount() == null) {
            player.sendMessage(Text.literal("authoritative legal choices: unavailable"), false);
        } else {
            player.sendMessage(Text.literal("authoritative legal choices: " + status.authoritativeLegalChoiceCount()), false);
        }
        player.sendMessage(Text.literal("turn, HP, faint and result: unavailable unless emitted by authoritative battle state"), false);
        return 1;
    }

    private static int spectate(ServerCommandSource source, String battleId) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Battle spectating must be requested by an authenticated player."));
            return 0;
        }
        if (!beginSpectating(player.getUuid(), battleId)) {
            source.sendError(Text.literal("No spectatable server-owned AutoPTU battle matches that ID."));
            return 0;
        }
        BattleStatusView status = spectatorStatus(player.getUuid());
        player.sendMessage(Text.literal("Now spectating AutoPTU battle read-only."), false);
        player.sendMessage(Text.literal("bound actor: " + status.actorId()), false);
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
                player.sendMessage(Text.literal(choice.choiceId() + " | " + choice.label()
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
            BattleChoiceMenuService.Entry selected = service.choose(binding.reservationId(), binding.actorId(), choiceId);
            player.sendMessage(Text.literal("Submitted authoritative choice: " + selected.choiceId()), false);
            return 1;
        } catch (RuntimeException rejected) {
            source.sendError(Text.literal("Battle choice rejected: " + safeMessage(rejected)));
            return 0;
        }
    }

    private static void clearPlayer(UUID playerUuid) {
        unbind(playerUuid);
        stopSpectating(playerUuid);
    }

    private static String normalize(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.strip();
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
            if (actorId == null || actorId.isBlank()) throw new IllegalArgumentException("actorId must not be blank");
            if (authoritativeLegalChoiceCount != null && authoritativeLegalChoiceCount < 0) {
                throw new IllegalArgumentException("authoritativeLegalChoiceCount cannot be negative");
            }
            return new BattleStatusView(true, actorId.strip(), authoritativeLegalChoiceCount);
        }
    }

    private record SessionBinding(String reservationId, String actorId, String spectateId) {
        private SessionBinding {
            reservationId = normalize(reservationId, "reservationId");
            actorId = normalize(actorId, "actorId");
            spectateId = normalize(spectateId, "spectateId");
        }
    }
}
