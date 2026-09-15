package io.autoptu.cobblemon.fabric.battle;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import io.autoptu.cobblemon.battlecore.BattleAuthoritativeChoiceExecutor;
import io.autoptu.cobblemon.battlecore.BattleAuthoritativeLegalChoiceSource;
import io.autoptu.cobblemon.battlecore.BattleChoiceMenuService;
import io.autoptu.cobblemon.battlecore.BattleChoiceVisualPlan;
import io.autoptu.cobblemon.battlecore.BattleCoreLegalChoice;
import io.autoptu.cobblemon.battlecore.BattleCoreLegalChoiceSet;
import io.autoptu.cobblemon.battlecore.BattleGridCoordinate;
import io.autoptu.cobblemon.battlecore.BattleGridTransform;
import io.autoptu.cobblemon.battlecore.BattleActionDetail;
import io.autoptu.cobblemon.fabric.network.FabricBattleMenuPayload;
import io.autoptu.cobblemon.fabric.network.FabricBattleSelectionPayload;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Minecraft-visible battle action menu, authoritative target overlay and read-only spectating HUD.
 *
 * The overlay never calculates targeting or movement. It renders only destinations/target anchors
 * already present in a fresh AutoPTU-Java legal-choice set and maps those grid coordinates through
 * the server-owned canonical encounter arena. Cobblemon state is not consulted for legality.
 */
public final class FabricBattleChoiceRuntime {
    private static final Map<UUID, SessionBinding> ACTIVE = new ConcurrentHashMap<>();
    private static final Map<UUID, String> SPECTATORS = new ConcurrentHashMap<>();
    private static final Map<UUID, ServerBossBar> HUDS = new ConcurrentHashMap<>();
    private static final Map<UUID, ServerBossBar> SPECTATOR_HUDS = new ConcurrentHashMap<>();
    private static final Map<UUID, SelectionVisual> SELECTIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, String> TOKENS = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, BattleActionDetail>> DETAILS = new ConcurrentHashMap<>();
    private static final long PREVIEW_DURATION_MILLIS = 15_000L;
    private static final long COMMITTED_DURATION_MILLIS = 2_500L;
    private static volatile BattleChoiceMenuService menuService;
    private static volatile BattleAuthoritativeLegalChoiceSource legalChoiceSource;
    private static int hudTick;
    private static long visualFrame;

    private FabricBattleChoiceRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("battle")
                                .then(CommandManager.literal("status")
                                        .executes(context -> showStatus(context.getSource())))
                                .then(CommandManager.literal("choices")
                                        .executes(context -> showChoices(context.getSource())))
                                .then(CommandManager.literal("menu")
                                        .executes(context -> showChoices(context.getSource())))
                                .then(CommandManager.literal("endturn")
                                        .executes(context -> endTurn(context.getSource())))
                                .then(CommandManager.literal("choose")
                                        .then(CommandManager.argument("choiceId", StringArgumentType.greedyString())
                                                .executes(context -> choose(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "choiceId")))))
                                .then(CommandManager.literal("preview")
                                        .then(CommandManager.argument("choiceId", StringArgumentType.greedyString())
                                                .executes(context -> preview(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "choiceId")))))
                                .then(CommandManager.literal("confirm")
                                        .executes(context -> confirm(context.getSource(), null))
                                        .then(CommandManager.argument("token", StringArgumentType.word())
                                                .executes(context -> confirm(context.getSource(), StringArgumentType.getString(context, "token")))))
                                .then(CommandManager.literal("cancel")
                                        .executes(context -> cancelPreview(context.getSource()))
                                        .then(CommandManager.argument("token", StringArgumentType.word())
                                                .executes(context -> cancelPreview(context.getSource()))))
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
        FabricBattleChoiceRuntime.legalChoiceSource = Objects.requireNonNull(legalChoiceSource, "legalChoiceSource");
        menuService = new BattleChoiceMenuService(legalChoiceSource, executor);
    }

    public static void bind(UUID playerUuid, String reservationId, String actorId) {
        bindSession(playerUuid, reservationId, actorId, null, null, null, null);
    }

    public static void bindSession(UUID playerUuid, String reservationId, String actorId,
            BattleGridTransform arena, BattleAuthoritativeLegalChoiceSource source,
            BattleAuthoritativeChoiceExecutor executor, Runnable endTurn) {
        bindSession(playerUuid, reservationId, actorId, arena, source, executor, endTurn, null);
    }

    /** Registers the frozen arena and authoritative services for one authenticated participant. */
    public static void bindSession(UUID playerUuid, String reservationId, String actorId,
            BattleGridTransform arena, BattleAuthoritativeLegalChoiceSource source,
            BattleAuthoritativeChoiceExecutor executor, Runnable endTurn,
            Supplier<BattleGridCoordinate> actorOrigin) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        if ((source == null) != (executor == null)) throw new IllegalArgumentException("source and executor must be provided together");
        SessionBinding existing = ACTIVE.get(playerUuid);
        String spectateId = existing != null
                && existing.reservationId().equals(normalize(reservationId, "reservationId"))
                && existing.actorId().equals(normalize(actorId, "actorId"))
                ? existing.spectateId()
                : UUID.randomUUID().toString();
        ACTIVE.put(playerUuid, new SessionBinding(reservationId, actorId, spectateId,
                arena, source, source == null ? null : new BattleChoiceMenuService(source, executor), endTurn, actorOrigin));
        TOKENS.remove(playerUuid);
        SELECTIONS.remove(playerUuid);
        DETAILS.remove(playerUuid);
        if (existing == null
                || !existing.reservationId().equals(normalize(reservationId, "reservationId"))
                || !existing.actorId().equals(normalize(actorId, "actorId"))) {
            SELECTIONS.remove(playerUuid);
        }
        stopSpectating(playerUuid);
    }

    public static BattleArenaSnapshot arena(UUID playerUuid) {
        SessionBinding binding = playerUuid == null ? null : ACTIVE.get(playerUuid);
        return binding == null || binding.arena() == null ? null : binding.arena().toArenaSnapshot();
    }

    public static void describeActions(UUID playerUuid, Map<String, BattleActionDetail> details) {
        if (!hasBinding(playerUuid)) throw new IllegalStateException("no bound battle");
        DETAILS.put(playerUuid, Map.copyOf(details));
    }

    public static BattleChoiceVisualPlan visualPlan(UUID playerUuid) {
        SessionBinding binding = ACTIVE.get(playerUuid);
        if (binding == null) return null;
        var set = choiceSource(binding).legalChoices(binding.reservationId(), binding.actorId());
        var selection = currentSelection(playerUuid, binding);
        var plan = BattleChoiceVisualPlan.from(set, selection != null && selection.phase() == SelectionPhase.PREVIEW
                ? selection.choice().stableKey() : null);
        return selection != null && selection.phase() == SelectionPhase.COMMITTED
                ? plan.withExecutedHighlight(selection.choice()) : plan;
    }

    private static BattleChoiceMenuService service(SessionBinding binding) {
        return binding != null && binding.service() != null ? binding.service() : menuService;
    }

    private static BattleAuthoritativeLegalChoiceSource choiceSource(SessionBinding binding) {
        return binding.source() != null ? binding.source() : legalChoiceSource;
    }

    public static void unbind(UUID playerUuid) {
        if (playerUuid == null) return;
        SessionBinding removed = ACTIVE.remove(playerUuid);
        DETAILS.remove(playerUuid);
        SELECTIONS.remove(playerUuid);
        TOKENS.remove(playerUuid);
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

    public static BattleStatusView status(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        SessionBinding binding = ACTIVE.get(playerUuid);
        return binding == null ? BattleStatusView.unbound() : status(binding);
    }

    private static BattleStatusView status(SessionBinding binding) {
        BattleChoiceMenuService service = service(binding);
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
        visualFrame++;
        refreshParticipantHud(server);
        refreshSpectatorHud(server);
    }

    private static void refreshParticipantHud(MinecraftServer server) {
        for (Map.Entry<UUID, SessionBinding> active : ACTIVE.entrySet()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(active.getKey());
            if (player == null) continue;

            SessionBinding binding = active.getValue();
            BattleChoiceMenuService service = service(binding);
            ServerBossBar hud = HUDS.computeIfAbsent(active.getKey(), ignored -> createHud("AutoPTU battle"));
            hud.addPlayer(player);

            if (service == null) {
                hud.setName(Text.literal("AutoPTU • authoritative choices unavailable"));
                continue;
            }

            try {
                List<BattleChoiceMenuService.Entry> choices = service.choices(binding.reservationId(), binding.actorId());
                SelectionVisual previous = SELECTIONS.get(active.getKey());
                SelectionVisual selection = currentSelection(active.getKey(), binding);
                if (previous != null && selection == null) FabricBattleSelectionPayload.send(player, "", "");
                hud.setName(Text.literal(selection == null
                        ? hudTitle(binding.actorId(), choices.size())
                        : selectionHudTitle(binding.actorId(), choices.size(), selection)));
                renderAuthoritativeTargetOverlay(player, binding, selection);
            } catch (RuntimeException unavailable) {
                hud.setName(Text.literal("AutoPTU • authoritative choices unavailable"));
            }
        }
    }

    private static void renderAuthoritativeTargetOverlay(
            ServerPlayerEntity player,
            SessionBinding binding,
            SelectionVisual selection
    ) {
        // Sessions with their own turn controller publish their complete arena frame.
        if (binding.endTurn() != null) return;
        BattleAuthoritativeLegalChoiceSource source = choiceSource(binding);
        MinecraftServer server = player.getServer();
        if (source == null || server == null) return;

        BattleCoreLegalChoiceSet set = source.legalChoices(binding.reservationId(), binding.actorId());
        if (!set.reservationId().equals(binding.reservationId()) || !set.actorId().equals(binding.actorId())) {
            throw new IllegalStateException("authoritative legal choice source returned a different battle scope");
        }

        if (binding.arena() == null) return;
        BattleGridTransform transform = binding.arena();
        ServerWorld world = player.getServerWorld();
        String worldDimension = world.getRegistryKey().getValue().toString();
        if (!transform.origin().dimensionId().equals(worldDimension)) return;

        BattleChoiceVisualPlan plan;
        if (selection == null) {
            plan = BattleChoiceVisualPlan.from(set, null);
        } else if (selection.phase() == SelectionPhase.PREVIEW) {
            plan = BattleChoiceVisualPlan.from(set, selection.choice().stableKey());
        } else {
            plan = BattleChoiceVisualPlan.from(set, null).withExecutedHighlight(selection.choice());
        }
        FabricBattleGridVisualRenderer.render(
                world, transform, plan, selection != null && selection.phase() == SelectionPhase.COMMITTED, visualFrame);
    }

    static Set<BattleGridCoordinate> authoritativeTargetAnchors(BattleCoreLegalChoiceSet set) {
        Objects.requireNonNull(set, "set");
        LinkedHashSet<BattleGridCoordinate> anchors = new LinkedHashSet<>();
        for (BattleCoreLegalChoice choice : set.choices()) {
            if (choice instanceof BattleCoreLegalChoice.Shift shift) {
                anchors.add(shift.destination());
            } else if (choice instanceof BattleCoreLegalChoice.Move move
                    && (move.targetMode() == io.autoptu.cobblemon.battlecore.BattleClientActionRequest.Target.Mode.TILE
                    || move.targetMode() == io.autoptu.cobblemon.battlecore.BattleClientActionRequest.Target.Mode.COMBATANT)) {
                anchors.add(move.targetAnchor());
            }
        }
        return Set.copyOf(anchors);
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

    static String selectionHudTitle(String actorId, int legalChoiceCount, SelectionVisual selection) {
        Objects.requireNonNull(selection, "selection");
        String phase = selection.phase() == SelectionPhase.PREVIEW ? "PREVIEW" : "LOCKED";
        String action = selection.choice() instanceof BattleCoreLegalChoice.Shift
                ? "MOVE"
                : ((BattleCoreLegalChoice.Move) selection.choice()).moveId();
        return "AutoPTU • " + actorId.strip() + " • " + phase + " " + action + " • legal " + legalChoiceCount;
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
        SessionBinding binding = ACTIVE.get(player.getUuid());
        BattleChoiceMenuService service = service(binding);
        if (service == null || binding == null) {
            source.sendError(Text.literal("No active authoritative AutoPTU battle is bound to this player."));
            return 0;
        }

        try {
            List<BattleChoiceMenuService.Entry> choices = service.choices(binding.reservationId(), binding.actorId());
            var plan = visualPlan(player.getUuid());
            if (FabricBattleMenuPayload.send(player, choices, binding.endTurn() != null,
                    plan == null ? null : plan.gridWindow(),
                    binding.actorOrigin() == null ? null : binding.actorOrigin().get(),
                    DETAILS.getOrDefault(player.getUuid(), Map.of()))) return 1;
            if (choices.isEmpty()) {
                player.sendMessage(Text.literal("AutoPTU battle choices: none currently legal."), false);
                return 1;
            }
            player.sendMessage(Text.literal("AutoPTU battle choices • preview, then confirm"), false);
            for (BattleChoiceMenuService.Entry choice : choices) {
                MutableText line = Text.literal(choice.label() + " ");
                line.append(Text.literal("[PREVIEW]").styled(style -> style
                        .withColor(Formatting.AQUA)
                        .withUnderline(true)
                        .withClickEvent(new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                "/autoptu battle preview " + choice.choiceId()))));
                player.sendMessage(line, false);
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
        SessionBinding binding = ACTIVE.get(player.getUuid());
        BattleChoiceMenuService service = service(binding);
        if (service == null || binding == null) {
            source.sendError(Text.literal("No active authoritative AutoPTU battle is bound to this player."));
            return 0;
        }

        try {
            BattleCoreLegalChoice selectedChoice = authoritativeChoice(binding, choiceId);
            BattleChoiceMenuService.Entry selected = service.chooseExact(binding.reservationId(), binding.actorId(), selectedChoice);
            TOKENS.remove(player.getUuid());
            FabricBattleSelectionPayload.send(player, "", "");
            rememberSelection(player.getUuid(), binding, selectedChoice, SelectionPhase.COMMITTED);
            player.sendMessage(Text.literal("Submitted authoritative choice: " + selected.choiceId()), false);
            return 1;
        } catch (RuntimeException rejected) {
            source.sendError(Text.literal("Battle choice rejected: " + safeMessage(rejected)));
            return 0;
        }
    }

    private static int endTurn(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        SessionBinding binding = player == null ? null : ACTIVE.get(player.getUuid());
        if (player == null || binding == null || binding.endTurn() == null) {
            source.sendError(Text.literal("No active battle turn can be ended."));
            return 0;
        }
        try {
            binding.endTurn().run();
            SELECTIONS.remove(player.getUuid());
            TOKENS.remove(player.getUuid());
            FabricBattleSelectionPayload.send(player, "", "");
            player.sendMessage(Text.literal("Turn ended; waiting for the opponent."), true);
            return 1;
        } catch (RuntimeException rejected) {
            source.sendError(Text.literal("Turn end rejected: " + safeMessage(rejected)));
            return 0;
        }
    }

    private static int preview(ServerCommandSource source, String choiceId) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("A battle choice preview must be requested by an authenticated player."));
            return 0;
        }
        SessionBinding binding = ACTIVE.get(player.getUuid());
        if (binding == null || service(binding) == null) {
            source.sendError(Text.literal("No active authoritative AutoPTU battle is bound to this player."));
            return 0;
        }
        try {
            BattleCoreLegalChoice selected = authoritativeChoice(binding, choiceId);
            rememberSelection(player.getUuid(), binding, selected, SelectionPhase.PREVIEW);
            String token = UUID.randomUUID().toString();
            TOKENS.put(player.getUuid(), token);
            FabricBattleSelectionPayload.send(player, token, selected.stableKey());
            String label = selected instanceof BattleCoreLegalChoice.Shift
                    ? "movement destination"
                    : "attack target for " + ((BattleCoreLegalChoice.Move) selected).moveId();
            MutableText prompt = Text.literal("Previewing " + label + ". ");
            prompt.append(Text.literal("[CONFIRM]").styled(style -> style
                    .withColor(Formatting.GOLD)
                    .withBold(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/autoptu battle confirm " + token))));
            prompt.append(Text.literal(" "));
            prompt.append(Text.literal("[CANCEL]").styled(style -> style
                    .withColor(Formatting.GRAY)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/autoptu battle cancel"))));
            player.sendMessage(prompt, false);
            return 1;
        } catch (RuntimeException rejected) {
            source.sendError(Text.literal("Battle choice preview rejected: " + safeMessage(rejected)));
            return 0;
        }
    }

    private static int confirm(ServerCommandSource source, String token) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("A battle choice must be confirmed by an authenticated player."));
            return 0;
        }
        SessionBinding binding = ACTIVE.get(player.getUuid());
        SelectionVisual preview = currentSelection(player.getUuid(), binding);
        if (binding == null || service(binding) == null || preview == null || preview.phase() != SelectionPhase.PREVIEW
                || token == null || !token.equals(TOKENS.get(player.getUuid()))) {
            source.sendError(Text.literal("No current authoritative battle choice preview is available to confirm."));
            return 0;
        }
        try {
            service(binding).chooseExact(binding.reservationId(), binding.actorId(), preview.choice());
            rememberSelection(player.getUuid(), binding, preview.choice(), SelectionPhase.COMMITTED);
            TOKENS.remove(player.getUuid());
            FabricBattleSelectionPayload.send(player, "", "");
            return 1;
        } catch (RuntimeException rejected) {
            cancelPreview(source);
            source.sendError(Text.literal("Choice changed or was rejected: " + safeMessage(rejected)));
            return 0;
        }
    }

    private static int cancelPreview(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("A battle choice preview must be cancelled by an authenticated player."));
            return 0;
        }
        SelectionVisual removed = SELECTIONS.remove(player.getUuid());
        TOKENS.remove(player.getUuid());
        FabricBattleSelectionPayload.send(player, "", "");
        player.sendMessage(Text.literal(removed != null && removed.phase() == SelectionPhase.PREVIEW
                ? "Battle choice preview cancelled."
                : "No battle choice preview was active."), false);
        return 1;
    }

    private static BattleCoreLegalChoice authoritativeChoice(SessionBinding binding, String stableKey) {
        BattleAuthoritativeLegalChoiceSource source = choiceSource(binding);
        if (source == null) throw new IllegalStateException("authoritative legal choices unavailable");
        String normalizedKey = normalize(stableKey, "choiceId");
        BattleCoreLegalChoiceSet set = source.legalChoices(binding.reservationId(), binding.actorId());
        if (!set.reservationId().equals(binding.reservationId()) || !set.actorId().equals(binding.actorId())) {
            throw new IllegalStateException("authoritative legal choice source returned a different battle scope");
        }
        return set.choices().stream()
                .filter(choice -> choice.stableKey().equals(normalizedKey))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("choice is no longer legal in the authoritative action space"));
    }

    private static void rememberSelection(
            UUID playerUuid,
            SessionBinding binding,
            BattleCoreLegalChoice choice,
            SelectionPhase phase
    ) {
        long duration = phase == SelectionPhase.PREVIEW ? PREVIEW_DURATION_MILLIS : COMMITTED_DURATION_MILLIS;
        SELECTIONS.put(playerUuid, new SelectionVisual(
                binding.reservationId(), binding.actorId(), choice, phase, System.currentTimeMillis() + duration));
    }

    private static SelectionVisual currentSelection(UUID playerUuid, SessionBinding binding) {
        if (playerUuid == null || binding == null) return null;
        SelectionVisual selection = SELECTIONS.get(playerUuid);
        if (selection == null) return null;
        if (selection.expiresAtMillis() < System.currentTimeMillis()
                || !selection.reservationId().equals(binding.reservationId())
                || !selection.actorId().equals(binding.actorId())) {
            SELECTIONS.remove(playerUuid, selection);
            return null;
        }
        if (selection.phase() == SelectionPhase.PREVIEW) {
            try {
                if (!authoritativeChoice(binding, selection.choice().stableKey()).equals(selection.choice())) {
                    throw new IllegalArgumentException("choice changed after preview");
                }
            } catch (RuntimeException stale) {
                SELECTIONS.remove(playerUuid, selection);
                return null;
            }
        }
        return selection;
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

    private record SessionBinding(String reservationId, String actorId, String spectateId,
                                  BattleGridTransform arena, BattleAuthoritativeLegalChoiceSource source,
                                  BattleChoiceMenuService service, Runnable endTurn,
                                  Supplier<BattleGridCoordinate> actorOrigin) {
        private SessionBinding {
            reservationId = normalize(reservationId, "reservationId");
            actorId = normalize(actorId, "actorId");
            spectateId = normalize(spectateId, "spectateId");
        }
    }

    enum SelectionPhase { PREVIEW, COMMITTED }

    record SelectionVisual(
            String reservationId,
            String actorId,
            BattleCoreLegalChoice choice,
            SelectionPhase phase,
            long expiresAtMillis
    ) {
        SelectionVisual {
            reservationId = normalize(reservationId, "reservationId");
            actorId = normalize(actorId, "actorId");
            choice = Objects.requireNonNull(choice, "choice");
            phase = Objects.requireNonNull(phase, "phase");
            if (expiresAtMillis < 1L) throw new IllegalArgumentException("expiresAtMillis must be positive");
            if (!choice.actorId().equals(actorId)) throw new IllegalArgumentException("choice belongs to a different actor");
        }
    }
}
