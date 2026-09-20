package io.autoptu.cobblemon.fabric.demo;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import io.autoptu.cobblemon.battlecore.BattlePracticeScenario;
import io.autoptu.cobblemon.fabric.network.FabricBattlePracticePayload;
import io.autoptu.cobblemon.fabric.network.FabricBattleGuidePayload;
import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import io.autoptu.cobblemon.battlecore.BattleChoiceVisualPlan;
import io.autoptu.cobblemon.battlecore.BattleGridCoordinate;
import io.autoptu.cobblemon.battlecore.BattleGridTransform;
import io.autoptu.cobblemon.fabric.battle.FabricBattleGridVisualRenderer;
import io.autoptu.cobblemon.fabric.battle.FabricBattleChoiceRuntime;
import io.autoptu.cobblemon.fabric.network.FabricBattleStatusPayload;
import io.autoptu.cobblemon.battlecore.BattleCoreLegalChoice;
import io.autoptu.cobblemon.battlecore.BattleCoreLegalChoiceSet;
import io.autoptu.cobblemon.battlecore.BattleMatchJournal;
import io.autoptu.cobblemon.battlecore.BattleMatchReport;
import io.autoptu.cobblemon.fabric.battle.FabricBattleReportStore;
import io.autoptu.cobblemon.fabric.network.FabricBattleReportPayload;
import io.autoptu.cobblemon.fabric.presentation.CobblemonPresentationEntityBackend;
import io.autoptu.cobblemon.fabric.rpg.FabricRpgWorldProtectionRegistry;
import io.autoptu.core.action.ChoiceTargetMode;
import io.autoptu.core.action.MoveChoice;
import io.autoptu.core.action.MoveOption;
import io.autoptu.core.action.ShiftChoice;
import io.autoptu.core.action.TargetCandidate;
import io.autoptu.core.event.MoveResolvedEvent;
import io.autoptu.core.model.ActionType;
import io.autoptu.core.model.GridCoord;
import io.autoptu.core.model.MoveSpec;
import io.autoptu.core.model.MovementGrid;
import io.autoptu.core.model.MovementProfile;
import io.autoptu.core.random.PythonRandom;
import io.autoptu.core.rules.ActionBudget;
import io.autoptu.core.rules.AutobattlerActionSpace;
import io.autoptu.core.runtime.AppliedActionResult;
import io.autoptu.core.runtime.BattleRuntime;
import io.autoptu.core.runtime.BattleRuntimeState;
import io.autoptu.core.runtime.MoveResolutionInput;
import io.autoptu.core.runtime.RuntimeCombatantState;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * First manually playable vertical battle.
 *
 * This is intentionally a narrow 1v1 operator demo, not the general battle materializer. AutoPTU-Java
 * owns accuracy rolls, damage, action consumption and authoritative HP mutation. Fabric/Cobblemon
 * only select presentation species for a server-owned demo scenario and project its semantic move
 * result into visible entities. Statuses, abilities, items, terrain, Trainer Features, forced movement
 * and rewards are disabled.
 */
public final class PlayableBattleTestRuntime {
    private static final int DEMO_HP = 60;
    private static final int TURN_DELAY_TICKS = 12;
    private static final int OPENING_MOVEMENT_PREVIEW_TICKS = 20;
    private static final int ATTACK_DECLARATION_TICKS = 18;
    private static final int LUNGE_TICKS = 8;
    private static final int CLEANUP_TICKS = 80;

    private static final CobblemonPresentationEntityBackend PRESENTATION =
            new CobblemonPresentationEntityBackend();
    private static final Map<UUID, Session> ACTIVE = new ConcurrentHashMap<>();

    private PlayableBattleTestRuntime() {}

    public static void register() {
        FabricBattleReportPayload.register();
        FabricBattlePracticePayload.register();
        FabricBattleGuidePayload.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var adminDemo = CommandManager.literal("demo")
                    .then(CommandManager.argument("species", StringArgumentType.word())
                            .then(CommandManager.argument("opponent", StringArgumentType.word())
                                    .executes(context -> start(
                                            context.getSource(),
                                            StringArgumentType.getString(context, "species"),
                                            StringArgumentType.getString(context, "opponent")
                                    ))));
            var adminBattle = CommandManager.literal("battle").then(adminDemo)
                    .then(CommandManager.literal("stop").executes(context -> {
                        var player = context.getSource().getPlayer();
                        var session = player == null ? null : ACTIVE.get(player.getUuid());
                        if (session == null) return 0;
                        session.cleanupNow();
                        player.sendMessage(Text.literal("Battle session closed."), false);
                        return 1;
                    }))
                    .then(CommandManager.literal("play")
                            .then(CommandManager.argument("species", StringArgumentType.word())
                                    .then(CommandManager.argument("opponent", StringArgumentType.word())
                                            .executes(context -> start(
                                                    context.getSource(),
                                                    StringArgumentType.getString(context, "species"),
                                                    StringArgumentType.getString(context, "opponent"), true))
                                            .then(CommandManager.argument("scenario", StringArgumentType.word())
                                                    .suggests((context, builder) -> {
                                                        BattlePracticeScenario.CATALOG.forEach(s -> builder.suggest(s.id()));
                                                        return builder.buildFuture();
                                                    })
                                                    .executes(context -> startConfigured(context.getSource(),
                                                            StringArgumentType.getString(context, "species"),
                                                            StringArgumentType.getString(context, "opponent"),
                                                            StringArgumentType.getString(context, "scenario"), 20260823L))
                                                    .then(CommandManager.argument("seed", LongArgumentType.longArg())
                                                            .executes(context -> startConfigured(context.getSource(),
                                                                    StringArgumentType.getString(context, "species"),
                                                                    StringArgumentType.getString(context, "opponent"),
                                                                    StringArgumentType.getString(context, "scenario"),
                                                                    LongArgumentType.getLong(context, "seed"))))))));
            var admin = CommandManager.literal("admin")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(adminBattle);

            var legacy = CommandManager.literal("testbattle")
                    .requires(source -> source.hasPermissionLevel(2));
            legacy.then(CommandManager.literal("bulbasaur")
                    .executes(context -> start(context.getSource(), "bulbasaur", "pikachu")));
            legacy.then(CommandManager.literal("charmander")
                    .executes(context -> start(context.getSource(), "charmander", "pikachu")));
            legacy.then(CommandManager.literal("squirtle")
                    .executes(context -> start(context.getSource(), "squirtle", "pikachu")));

            var root = CommandManager.literal("autoptu");
            root.then(admin);
            root.then(legacy);
            root.then(CommandManager.literal("battle").then(CommandManager.literal("report")
                    .executes(context -> report(context.getSource())))
                    .then(CommandManager.literal("help").executes(context -> {
                        var player = context.getSource().getPlayer();
                        if (player == null) return 0;
                        if (!FabricBattleGuidePayload.send(player)) player.sendMessage(Text.literal(
                                "Practice: /autoptu admin battle play <species> <opponent> [scenario] [seed]. "
                                + "Scenarios: training, duel, distance, endurance. B: menu; H: report. "
                                + "Requires OP. Custom 1v1 practice; no campaign rewards."), false);
                        return 1;
                    }))
                    .then(CommandManager.literal("practice").executes(context -> {
                        var player = context.getSource().getPlayer();
                        if (player == null) return 0;
                        if (!FabricBattlePracticePayload.send(player)) {
                            player.sendMessage(Text.literal("Use /autoptu admin battle play <species> <opponent> [scenario] [seed]"), false);
                        }
                        return 1;
                    })));
            dispatcher.register(root);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (Session session : List.copyOf(ACTIVE.values())) {
                try { session.tick(); }
                catch (RuntimeException error) {
                    session.journal.finish(BattleMatchReport.Outcome.ERROR);
                    org.slf4j.LoggerFactory.getLogger(PlayableBattleTestRuntime.class).error("Battle session failed", error);
                    session.player.sendMessage(Text.literal("Battle stopped after a runtime error. See the server log."), false);
                    session.cleanupNow();
                }
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            Session session = ACTIVE.get(handler.player.getUuid());
            if (session != null) {
                session.cleanupNow();
            }
        });
    }

    private static int start(ServerCommandSource source, String playerSpeciesId, String opponentSpeciesId) {
        return start(source, playerSpeciesId, opponentSpeciesId, false);
    }

    private static int report(ServerCommandSource source) {
        var player = source.getPlayer();
        if (player == null) return 0;
        var session = ACTIVE.get(player.getUuid());
        try {
            var report = session != null ? session.journal.snapshot()
                    : FabricBattleReportStore.load(source.getServer(), player.getUuid()).orElse(null);
            if (report == null) {
                source.sendError(Text.literal("No practice battle report is available yet."));
                return 0;
            }
            if (!FabricBattleReportPayload.send(player, report)) {
                player.sendMessage(Text.literal(report.allyName() + " vs " + report.enemyName() + ": " + report.outcome()), false);
                player.sendMessage(Text.literal("Damage: " + report.ally().damage() + " | Hits: " + report.ally().hits()
                        + "/" + report.ally().attacks() + " | Rounds: " + report.round()), false);
            }
            return 1;
        } catch (java.io.IOException error) {
            source.sendError(Text.literal("The saved practice report could not be read."));
            org.slf4j.LoggerFactory.getLogger(PlayableBattleTestRuntime.class).warn("Cannot read practice report", error);
            return 0;
        }
    }

    private static int start(ServerCommandSource source, String playerSpeciesId, String opponentSpeciesId, boolean interactive) {
        return start(source, playerSpeciesId, opponentSpeciesId, interactive, BattlePracticeScenario.find("training"), 20260823L);
    }

    private static int startConfigured(ServerCommandSource source, String ally, String enemy, String scenarioId, long seed) {
        final BattlePracticeScenario scenario;
        try { scenario = BattlePracticeScenario.find(scenarioId); }
        catch (IllegalArgumentException error) {
            source.sendError(Text.literal(error.getMessage()));
            return 0;
        }
        return start(source, ally, enemy, true, scenario, seed);
    }

    private static int start(ServerCommandSource source, String playerSpeciesId, String opponentSpeciesId,
            boolean interactive, BattlePracticeScenario scenario, long seed) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("The AutoPTU battle demo must be started by a player operator."));
            return 0;
        }
        if (ACTIVE.containsKey(player.getUuid()) || FabricBattleChoiceRuntime.hasBinding(player.getUuid())) {
            source.sendError(Text.literal("You already have an AutoPTU battle demo running."));
            return 0;
        }

        Species playerSpecies = PokemonSpecies.INSTANCE.getByName(playerSpeciesId);
        Species opponentSpecies = PokemonSpecies.INSTANCE.getByName(opponentSpeciesId);
        if (playerSpecies == null) {
            source.sendError(Text.literal("Unknown Cobblemon presentation species: " + playerSpeciesId));
            return 0;
        }
        if (opponentSpecies == null) {
            source.sendError(Text.literal("Unknown Cobblemon presentation opponent: " + opponentSpeciesId));
            return 0;
        }

        ServerWorld world = player.getServerWorld();
        BlockPos arenaOrigin = player.getBlockPos().add(1, 0, -1);
        BlockPos playerOrigin = arenaOrigin.add(scenario.allyStart().x(), 0, scenario.allyStart().y());
        BlockPos enemyOrigin = arenaOrigin.add(scenario.enemyStart().x(), 0, scenario.enemyStart().y());
        PokemonEntity playerPokemon = spawn(world, playerSpecies, playerOrigin);
        PokemonEntity enemyPokemon;
        try { enemyPokemon = spawn(world, opponentSpecies, enemyOrigin); }
        catch (RuntimeException error) { playerPokemon.discard(); throw error; }

        String protectionScopeId = "battle-demo:" + player.getUuidAsString();
        FabricRpgWorldProtectionRegistry.protect(
                protectionScopeId,
                world.getRegistryKey(),
                arenaOrigin.add(-1, -2, -1),
                arenaOrigin.add(scenario.width(), 3, scenario.height()),
                "an AutoPTU battle demo is active here"
        );

        Session session;
        try {
            session = new Session(
                player,
                displayName(playerSpeciesId),
                displayName(opponentSpeciesId),
                playerPokemon,
                enemyPokemon,
                playerOrigin,
                enemyOrigin,
                protectionScopeId, interactive, scenario, seed
            );
        } catch (RuntimeException error) {
            FabricBattleChoiceRuntime.unbind(player.getUuid());
            FabricRpgWorldProtectionRegistry.clear(protectionScopeId);
            playerPokemon.discard();
            enemyPokemon.discard();
            org.slf4j.LoggerFactory.getLogger(PlayableBattleTestRuntime.class).error("Cannot initialize practice battle", error);
            source.sendError(Text.literal("Practice battle could not start. See the server log."));
            return 0;
        }
        ACTIVE.put(player.getUuid(), session);
        session.announceStart();
        return 1;
    }

    private static PokemonEntity spawn(ServerWorld world, Species species, BlockPos position) {
        Pokemon pokemon = new Pokemon();
        pokemon.setSpecies(species);
        PokemonEntity entity = new PokemonEntity(world, pokemon, CobblemonEntities.POKEMON);
        entity.setAiDisabled(true);
        entity.setInvulnerable(true);
        entity.setNoGravity(true);
        entity.setPersistent();
        entity.refreshPositionAndAngles(
                position.getX() + 0.5D,
                position.getY(),
                position.getZ() + 0.5D,
                0.0F,
                0.0F
        );
        if (!world.spawnEntity(entity)) {
            throw new IllegalStateException("failed to spawn playable AutoPTU battle demo PokemonEntity");
        }
        return entity;
    }

    private static RuntimeCombatantState combatant(String id, GridCoord position) {
        return combatant(id, position, DEMO_HP, 3);
    }

    private static RuntimeCombatantState combatant(String id, GridCoord position, int hp, int speed) {
        return new RuntimeCombatantState(
                id,
                MovementProfile.walking(position, speed),
                hp,
                hp,
                new ActionBudget()
        );
    }

    private static MoveOption demoMove() {
        return demoMove("demo-strike");
    }

    static MoveOption demoMove(String moveId) {
        int range = switch (moveId) { case "demo-burst" -> 2; case "demo-arc" -> 3; default -> 5; };
        return MoveOption.standard(
                moveId,
                new MoveSpec("Ranged", "Ranged", range, range, null, null, "Ranged")
        );
    }

    private static MoveResolutionInput demoMoveInput() {
        return demoMoveInput("demo-strike");
    }

    static MoveResolutionInput demoMoveInput(String moveId) {
        return new MoveResolutionInput(
                moveId.equals("demo-burst") ? 4 : moveId.equals("demo-arc") ? 1 : 2,
                0,
                0,
                20,
                false,
                false,
                false,
                moveId.equals("demo-burst") ? 6 : moveId.equals("demo-arc") ? 3 : 4,
                10,
                5,
                false,
                1.0,
                List.of()
        );
    }

    static List<MoveChoice> legalDemoMoves(BattleRuntimeState runtime, RuntimeCombatantState actor, RuntimeCombatantState target) {
        return AutobattlerActionSpace.legalMoveChoices(actor.combatantId(), "Medium", runtime.grid(),
                actor.position(), actor.actionBudget(), List.of(demoMove("demo-strike"), demoMove("demo-burst"), demoMove("demo-arc")),
                List.of(new TargetCandidate(target.combatantId(), target.position(), "Medium")), Set.of());
    }

    private static String displayName(String speciesId) {
        String normalized = speciesId == null ? "pokemon" : speciesId.strip();
        int namespaceSeparator = normalized.indexOf(':');
        if (namespaceSeparator >= 0 && namespaceSeparator + 1 < normalized.length()) {
            normalized = normalized.substring(namespaceSeparator + 1);
        }
        if (normalized.isEmpty()) return "Pokemon";
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private static final class Session {
        private final ServerPlayerEntity player;
        private final String playerPokemonName;
        private final String enemyPokemonName;
        private final PokemonEntity playerEntity;
        private final PokemonEntity enemyEntity;
        private final String protectionScopeId;
        private final RuntimeCombatantState playerState;
        private final RuntimeCombatantState enemyState;
        private final BattleRuntimeState runtime;
        private final PythonRandom random;
        private final BattleGridTransform gridTransform;
        private final List<ShiftChoice> openingLegalShifts;
        private final ShiftChoice openingShift;
        private boolean playerTurn = true;
        private int round = 1;
        private final boolean interactive;
        private final BattlePracticeScenario scenario;
        private final long seed;
        private final BattleMatchJournal journal;
        private long elapsedTicks;
        private boolean reportSaved;
        private String pendingMoveId = "demo-strike";
        private BattleGridCoordinate aiShiftAnchor;
        private int aiShiftTicks;
        private int delay = TURN_DELAY_TICKS;
        private int openingMovementPreviewRemaining = OPENING_MOVEMENT_PREVIEW_TICKS;
        private int openingMovementCommitRemaining;
        private int attackDeclarationRemaining;
        private int attackCommitRemaining;
        private BattleGridCoordinate committedAttackAnchor;
        private BattleGridCoordinate committedAttackOrigin;
        private boolean openingShiftCommitted;
        private int lungeRemaining;
        private PokemonEntity lungingEntity;
        private BlockPos lungeReturn;
        private boolean finished;
        private int cleanupRemaining;

        private Session(
                ServerPlayerEntity player,
                String playerPokemonName,
                String enemyPokemonName,
                PokemonEntity playerEntity,
                PokemonEntity enemyEntity,
                BlockPos playerOrigin,
                BlockPos enemyOrigin,
                String protectionScopeId, boolean interactive, BattlePracticeScenario scenario, long seed
        ) {
            this.player = player;
            this.interactive = interactive;
            this.scenario = scenario;
            this.seed = seed;
            if (interactive) openingMovementPreviewRemaining = 0;
            this.playerPokemonName = playerPokemonName;
            this.enemyPokemonName = enemyPokemonName;
            this.journal = new BattleMatchJournal(UUID.randomUUID(), playerPokemonName, enemyPokemonName, System.currentTimeMillis());
            journal.turn(BattleMatchReport.Side.ALLY);
            this.playerEntity = playerEntity;
            this.enemyEntity = enemyEntity;
            this.protectionScopeId = protectionScopeId;
            this.playerState = combatant("player-demo", new GridCoord(scenario.allyStart().x(), scenario.allyStart().y()), scenario.hp(), scenario.speed());
            this.enemyState = combatant("wild-demo", new GridCoord(scenario.enemyStart().x(), scenario.enemyStart().y()), scenario.hp(), scenario.speed());
            this.runtime = new BattleRuntimeState(
                    new MovementGrid(scenario.width(), scenario.height(), Set.of(), Map.of()),
                    List.of(playerState, enemyState)
            );
            this.random = new PythonRandom(seed);
            this.gridTransform = BattleGridTransform.from(new BattleArenaSnapshot(
                    player.getServerWorld().getRegistryKey().getValue().toString(),
                    playerOrigin.getX() - scenario.allyStart().x(),
                    playerOrigin.getY(),
                    playerOrigin.getZ() - scenario.allyStart().y(),
                    1, 0, 0, 1
            ));
            this.openingLegalShifts = AutobattlerActionSpace.legalShiftChoices(
                    playerState.combatantId(), runtime.grid(), playerState.movementProfile(),
                    playerState.actionBudget(), 0, cell -> !cell.equals(enemyState.position()));
            this.openingShift = openingLegalShifts.stream()
                    .filter(choice -> choice.destination().equals(new GridCoord(2, 2)))
                    .findFirst()
                    .orElseGet(() -> openingLegalShifts.stream().findFirst()
                            .orElseThrow(() -> new IllegalStateException("demo has no authoritative opening Shift")));
            updateNameplates();
            if (interactive) FabricBattleChoiceRuntime.bindSession(player.getUuid(), "demo:" + player.getUuidAsString(),
                    playerState.combatantId(), gridTransform, this::legalChoices, this::executeChoice,
                    this::endTurn, () -> coordinate(playerState.position()));
            if (interactive) {
                Map<String, io.autoptu.cobblemon.battlecore.BattleActionDetail> details = new java.util.LinkedHashMap<>();
                for (String id : List.of("demo-strike", "demo-burst", "demo-arc")) {
                    var move = demoMove(id);
                    var input = demoMoveInput(id);
                    String name = switch (id) { case "demo-burst" -> "Fire burst"; case "demo-arc" -> "Electric arc"; default -> "Long strike"; };
                    details.put(id, new io.autoptu.cobblemon.battlecore.BattleActionDetail(name,
                            "Custom practice attack. One target; consumes the Standard action.",
                            "Range " + move.spec().rangeValue(), "DB " + input.effectiveDb(), "AC " + input.moveAc()));
                }
                FabricBattleChoiceRuntime.describeActions(player.getUuid(), details);
            }
            player.lookAt(net.minecraft.command.argument.EntityAnchorArgumentType.EntityAnchor.EYES,
                    new net.minecraft.util.math.Vec3d(gridTransform.origin().x() + scenario.width() / 2D,
                            gridTransform.origin().y() + 0.5D, gridTransform.origin().z() + scenario.height() / 2D));
            if (interactive) io.autoptu.cobblemon.fabric.battle.FabricBattleCameraRuntime.preserveSessionFraming(player.getUuid());
        }

        private BattleCoreLegalChoiceSet legalChoices(String reservationId, String actorId) {
            List<BattleCoreLegalChoice> choices = new java.util.ArrayList<>();
            if (!reservationId.equals("demo:" + player.getUuidAsString()) || !actorId.equals(playerState.combatantId())) {
                throw new IllegalArgumentException("wrong battle scope");
            }
            if (!finished && playerTurn && attackDeclarationRemaining == 0 && attackCommitRemaining == 0) {
                for (ShiftChoice shift : legalShifts()) {
                    choices.add(new BattleCoreLegalChoice.Shift(actorId, coordinate(shift.destination()), shift.stableKey()));
                }
                for (MoveChoice move : legalMoves(playerState, enemyState)) {
                    choices.add(new BattleCoreLegalChoice.Move(actorId, move.moveId(),
                            io.autoptu.cobblemon.battlecore.BattleClientActionRequest.Target.Mode.COMBATANT,
                            move.targetId(), coordinate(move.targetAnchor()), move.actionType().value(), move.stableKey()));
                }
            }
            return new BattleCoreLegalChoiceSet(reservationId, actorId, choices);
        }

        private void executeChoice(String reservationId, BattleCoreLegalChoice choice) {
            if (!legalChoices(reservationId, playerState.combatantId()).choices().contains(choice)) {
                throw new IllegalArgumentException("choice is no longer legal");
            }
            if (choice instanceof BattleCoreLegalChoice.Shift shift) {
                var from = coordinate(playerState.position());
                ShiftChoice selectedShift = legalShifts().stream()
                        .filter(candidate -> candidate.stableKey().equals(choice.stableKey()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("selected Shift is no longer legal"));
                BattleRuntime.applyAction(runtime, selectedShift, ignored -> true);
                journal.movement(BattleMatchReport.Side.ALLY, from, coordinate(playerState.position()));
                openingShiftCommitted = true;
                var world = gridTransform.toWorld(shift.destination());
                playerEntity.requestTeleport(world.x() + 0.5D, world.y(), world.z() + 0.5D);
                player.sendMessage(Text.literal("Movement confirmed on the tactical grid."), true);
            } else {
                pendingMoveId = ((BattleCoreLegalChoice.Move) choice).moveId();
                attackDeclarationRemaining = ATTACK_DECLARATION_TICKS;
            }
        }

        private void endTurn() {
            if (!playerTurn || finished || attackDeclarationRemaining > 0 || attackCommitRemaining > 0) {
                throw new IllegalStateException("wait for the current action to finish");
            }
            playerTurn = false;
            journal.pass(BattleMatchReport.Side.ALLY);
            journal.turn(BattleMatchReport.Side.ENEMY);
            enemyState.actionBudget().resetConsumedActions();
            delay = 2;
            player.sendMessage(Text.literal("Turn passed to the rival."), true);
        }

        private List<ShiftChoice> legalShifts() {
            return AutobattlerActionSpace.legalShiftChoices(
                    playerState.combatantId(), runtime.grid(), playerState.movementProfile(),
                    playerState.actionBudget(), 0, cell -> !cell.equals(enemyState.position()));
        }

        private List<MoveChoice> legalMoves(RuntimeCombatantState actor, RuntimeCombatantState target) {
            return legalDemoMoves(runtime, actor, target);
        }

        private void announceStart() {
            player.sendMessage(Text.literal(scenario.name() + " | " + scenario.width() + "x" + scenario.height() + " | seed " + seed), false);
            player.sendMessage(Text.literal("AutoPTU ADMIN DEMO: " + playerPokemonName + " vs " + enemyPokemonName), false);
            player.sendMessage(Text.literal(
                    "Presentation species are operator-selected. AutoPTU-Java owns attack rolls, damage and HP."), false);
            player.sendMessage(Text.literal(
                    "TACTICAL FLOW: green = legal movement, gold = selected destination, red = declared attack."), false);
            if (interactive) player.sendMessage(Text.literal("B: actions | Enter: confirm | Backspace: cancel. End turn in the menu. /autoptu admin battle stop closes this session."), false);
        }

        private void tick() {
            journal.clock(++elapsedTicks, round);
            if (playerEntity.isRemoved() || enemyEntity.isRemoved() || !player.isAlive()
                    || player.getWorld() != playerEntity.getWorld()) {
                cleanupNow();
                return;
            }

            if (lungeRemaining > 0 && --lungeRemaining == 0 && lungingEntity != null && lungeReturn != null) {
                lungingEntity.requestTeleport(
                        lungeReturn.getX() + 0.5D,
                        lungeReturn.getY(),
                        lungeReturn.getZ() + 0.5D
                );
                lungingEntity = null;
                lungeReturn = null;
            }
            holdPresentationAnchor(playerEntity, playerState);
            holdPresentationAnchor(enemyEntity, enemyState);

            if (finished) {
                renderTacticalGrid();
                FabricBattleStatusPayload.send(player, playerPokemonName, playerState.hp(), scenario.hp(),
                        enemyPokemonName, enemyState.hp(), scenario.hp(), playerState.hp() == 0 ? "DEFEAT" : "VICTORY", round);
                if (--cleanupRemaining <= 0) cleanupNow();
                return;
            }

            renderTacticalGrid();
            if ((player.getServerWorld().getTime() & 3L) == 0L) FabricBattleStatusPayload.send(player, playerPokemonName, playerState.hp(), scenario.hp(),
                    enemyPokemonName, enemyState.hp(), scenario.hp(),
                    attackDeclarationRemaining > 0 ? "ATTACK WINDUP" : attackCommitRemaining > 0 ? "IMPACT"
                            : playerTurn ? "YOUR TURN / B: ACTIONS" : "RIVAL TURN", round);
            if (aiShiftTicks > 0) { aiShiftTicks--; return; }
            if (openingMovementPreviewRemaining > 0) {
                renderOpeningMovementSelection();
                if (--openingMovementPreviewRemaining == 0) commitOpeningShift();
                return;
            }
            if (openingMovementCommitRemaining > 0) {
                openingMovementCommitRemaining--;
                return;
            }
            if (attackDeclarationRemaining > 0) {
                renderAttackDeclaration();
                if (--attackDeclarationRemaining == 0) {
                    resolveTurn(pendingMoveId);
                    delay = TURN_DELAY_TICKS;
                }
                return;
            }
            if (attackCommitRemaining > 0 && committedAttackAnchor != null) {
                attackCommitRemaining--;
                return;
            }
            if (--delay > 0) return;
            if (interactive && playerTurn) return;
            declareAttack();
        }

        private void renderTacticalGrid() {
            if ((player.getServerWorld().getTime() & 3L) != 0L) return;
            BattleChoiceVisualPlan plan = interactive ? FabricBattleChoiceRuntime.visualPlan(player.getUuid()) : null;
            Set<BattleGridCoordinate> shifts = plan == null ? Set.of() : plan.shiftDestinations();
            Set<BattleGridCoordinate> attacks = plan == null ? Set.of() : plan.attackTargets();
            BattleChoiceVisualPlan.Highlight highlight = plan == null ? null : plan.highlight();
            boolean committed = attackCommitRemaining > 0 || openingMovementCommitRemaining > 0;
            if (openingMovementPreviewRemaining > 0 || openingMovementCommitRemaining > 0) {
                shifts = legalShifts().stream().map(shift -> coordinate(shift.destination())).collect(java.util.stream.Collectors.toSet());
                highlight = new BattleChoiceVisualPlan.Highlight(BattleChoiceVisualPlan.HighlightKind.MOVEMENT,
                        coordinate(openingShift.destination()), openingShift.stableKey(), null);
            } else if (attackDeclarationRemaining > 0 || attackCommitRemaining > 0) {
                var anchor = attackCommitRemaining > 0 ? committedAttackAnchor : coordinate(playerTurn ? enemyState.position() : playerState.position());
                highlight = new BattleChoiceVisualPlan.Highlight(BattleChoiceVisualPlan.HighlightKind.ATTACK,
                        anchor, "declared", pendingMoveId);
            } else if (aiShiftTicks > 0) {
                highlight = new BattleChoiceVisualPlan.Highlight(BattleChoiceVisualPlan.HighlightKind.MOVEMENT,
                        aiShiftAnchor, "rival-shift", null);
                committed = true;
            }
            FabricBattleGridVisualRenderer.render(player.getServerWorld(), gridTransform,
                    new BattleChoiceVisualPlan(new BattleChoiceVisualPlan.GridWindow(0, scenario.width() - 1, 0, scenario.height() - 1), shifts, attacks, highlight),
                    committed, player.getServerWorld().getTime(), attackCommitRemaining > 0 && committedAttackOrigin != null
                            ? committedAttackOrigin : coordinate(playerTurn ? playerState.position() : enemyState.position()));
        }

        private void renderOpeningMovementSelection() {
            // Included in the complete arena frame.
        }

        private void commitOpeningShift() {
            var from = coordinate(playerState.position());
            BattleRuntime.applyAction(runtime, openingShift, ignored -> true);
            journal.movement(BattleMatchReport.Side.ALLY, from, coordinate(playerState.position()));
            var worldDestination = gridTransform.toWorld(coordinate(openingShift.destination()));
            playerEntity.requestTeleport(worldDestination.x() + 0.5D, worldDestination.y(), worldDestination.z() + 0.5D);
            openingShiftCommitted = true;
            openingMovementCommitRemaining = 8;
            player.sendMessage(Text.literal("Movement locked: destination accepted by AutoPTU-Java."), false);
        }

        private void declareAttack() {
            var actor = playerTurn ? playerState : enemyState;
            var target = playerTurn ? enemyState : playerState;
            var moves = legalMoves(actor, target);
            if (moves.isEmpty() && !playerTurn) {
                var shift = AutobattlerActionSpace.legalShiftChoices(actor.combatantId(), runtime.grid(),
                        actor.movementProfile(), actor.actionBudget(), 0, cell -> !cell.equals(target.position()))
                        .stream().min(java.util.Comparator.comparingInt(candidate ->
                                Math.abs(candidate.destination().x() - target.position().x())
                                        + Math.abs(candidate.destination().y() - target.position().y()))).orElse(null);
                if (shift != null) {
                    var from = coordinate(actor.position());
                    BattleRuntime.applyAction(runtime, shift, cell -> !cell.equals(target.position()));
                    journal.movement(BattleMatchReport.Side.ENEMY, from, coordinate(actor.position()));
                    aiShiftAnchor = coordinate(shift.destination());
                    aiShiftTicks = 12;
                    var world = gridTransform.toWorld(aiShiftAnchor);
                    enemyEntity.requestTeleport(world.x() + 0.5D, world.y(), world.z() + 0.5D);
                    delay = 1;
                    return;
                }
            }
            if (moves.isEmpty()) {
                journal.pass(playerTurn ? BattleMatchReport.Side.ALLY : BattleMatchReport.Side.ENEMY);
                playerTurn = !playerTurn;
                (playerTurn ? playerState : enemyState).actionBudget().resetConsumedActions();
                if (playerTurn) round++;
                journal.clock(elapsedTicks, round);
                journal.turn(playerTurn ? BattleMatchReport.Side.ALLY : BattleMatchReport.Side.ENEMY);
                delay = TURN_DELAY_TICKS;
                return;
            }
            pendingMoveId = moves.stream().max(java.util.Comparator.comparingInt(move -> {
                var input = demoMoveInput(move.moveId());
                return switch (scenario.rival()) {
                    case POWER -> input.effectiveDb() * 10 - input.moveAc();
                    case PRECISION -> 100 - input.moveAc() * 10 + input.effectiveDb();
                    case BALANCED -> input.effectiveDb() * (21 - input.moveAc());
                };
            })).orElseThrow().moveId();
            attackDeclarationRemaining = ATTACK_DECLARATION_TICKS;
            String attackerName = playerTurn ? playerPokemonName : enemyPokemonName;
            String targetName = playerTurn ? enemyPokemonName : playerPokemonName;
            player.sendMessage(Text.literal(attackerName + " locks " + pendingMoveId + " on " + targetName + "."), false);
        }

        private void renderAttackDeclaration() {
            // Included in the complete arena frame.
        }

        private void resolveTurn(String selectedMoveId) {
            RuntimeCombatantState attacker = playerTurn ? playerState : enemyState;
            committedAttackOrigin = coordinate(attacker.position());
            RuntimeCombatantState target = playerTurn ? enemyState : playerState;
            PokemonEntity attackerEntity = playerTurn ? playerEntity : enemyEntity;
            PokemonEntity targetEntity = playerTurn ? enemyEntity : playerEntity;
            BlockPos attackerOrigin = attackerEntity.getBlockPos();
            String attackerName = playerTurn ? playerPokemonName : enemyPokemonName;
            String targetName = playerTurn ? enemyPokemonName : playerPokemonName;

            MoveChoice choice = legalMoves(attacker, target).stream()
                    .filter(move -> move.moveId().equals(selectedMoveId)).findFirst()
                    .orElseThrow(() -> new IllegalStateException("selected attack is no longer legal"));

            AppliedActionResult applied = BattleRuntime.applyAuthoritativeMove(
                    runtime,
                    choice,
                    demoMove(selectedMoveId),
                    "Medium",
                    "Medium",
                    Set.of(),
                    playerTurn ? "Player" : "Wild",
                    random,
                    demoMoveInput(selectedMoveId)
            );
            MoveResolvedEvent event = (MoveResolvedEvent) applied.events().stream()
                    .filter(MoveResolvedEvent.class::isInstance)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("AutoPTU-Java emitted no MoveResolvedEvent"));
            journal.attack(playerTurn ? BattleMatchReport.Side.ALLY : BattleMatchReport.Side.ENEMY,
                    event.moveId(), event.hit(), event.crit(), event.damage(), event.targetHp(),
                    coordinate(attacker.position()), coordinate(target.position()));

            PRESENTATION.animateMove(attackerEntity, targetEntity,
                    new io.autoptu.cobblemon.battlecore.BattlePresentationCommand(round, 0,
                            io.autoptu.cobblemon.battlecore.BattlePresentationCommand.Kind.MOVE_ANIMATION,
                            attacker.combatantId(), Map.of("moveId", event.moveId(), "hit", Boolean.toString(event.hit()),
                            "crit", Boolean.toString(event.crit()), "damage", Integer.toString(event.damage()))));
            lungingEntity = attackerEntity;
            lungeReturn = attackerOrigin;
            lungeRemaining = LUNGE_TICKS;

            if (event.hit()) {
                PRESENTATION.projectDisplayedHealth(targetEntity, event.targetHp(), event.damage());
                player.sendMessage(Text.literal(
                        attackerName + " attacks " + targetName + " for " + event.damage()
                                + " damage. HP: " + event.targetHp() + "/" + DEMO_HP
                                + (event.crit() ? " CRITICAL" : "")), false);
            } else {
                player.sendMessage(Text.literal(attackerName + " attacks, but misses."), false);
            }
            updateNameplates();
            committedAttackAnchor = coordinate(target.position());
            attackCommitRemaining = 8;

            if (event.targetHp() == 0) {
                finish(attackerName, targetName);
                return;
            }
            openingShiftCommitted = false;
            playerTurn = !playerTurn;
            (playerTurn ? playerState : enemyState).actionBudget().resetConsumedActions();
            if (playerTurn) round++;
            journal.clock(elapsedTicks, round);
            journal.turn(playerTurn ? BattleMatchReport.Side.ALLY : BattleMatchReport.Side.ENEMY);
        }

        private static BattleGridCoordinate coordinate(GridCoord coordinate) {
            return new BattleGridCoordinate(coordinate.x(), coordinate.y());
        }

        private void updateNameplates() {
            nameplate(playerEntity, playerPokemonName, playerState.hp());
            nameplate(enemyEntity, enemyPokemonName, enemyState.hp());
        }

        private void holdPresentationAnchor(PokemonEntity entity, RuntimeCombatantState state) {
            entity.setVelocity(net.minecraft.util.math.Vec3d.ZERO);
            if (entity == lungingEntity) return;
            var anchor = gridTransform.toWorld(coordinate(state.position()));
            if (entity.squaredDistanceTo(anchor.x() + 0.5D, anchor.y(), anchor.z() + 0.5D) > 0.0025D) {
                entity.requestTeleport(anchor.x() + 0.5D, anchor.y(), anchor.z() + 0.5D);
            }
        }

        private void finish(String winner, String loser) {
            finished = true;
            journal.finish(playerState.hp() == 0 ? BattleMatchReport.Outcome.DEFEAT : BattleMatchReport.Outcome.VICTORY);
            saveReport();
            cleanupRemaining = CLEANUP_TICKS;
            player.sendMessage(Text.literal("BATTLE OVER - WINNER: " + winner + " | LOSER: " + loser), false);
            player.sendMessage(Text.literal("This operator demo does not commit XP, items or campaign results."), false);
            player.sendMessage(Text.literal("[Match report]").styled(style -> style.withColor(net.minecraft.util.Formatting.AQUA)
                    .withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.RUN_COMMAND,
                            "/autoptu battle report"))), false);
        }

        private void saveReport() {
            if (reportSaved || player.getServer() == null) return;
            try {
                FabricBattleReportStore.save(player.getServer(), player.getUuid(), journal.snapshot());
                reportSaved = true;
            } catch (java.io.IOException error) {
                org.slf4j.LoggerFactory.getLogger(PlayableBattleTestRuntime.class).warn("Cannot save practice report", error);
                player.sendMessage(Text.literal("The battle ended, but its report could not be saved."), false);
            }
        }

        private void cleanupNow() {
            journal.finish(BattleMatchReport.Outcome.CLOSED);
            saveReport();
            FabricBattleStatusPayload.clear(player);
            io.autoptu.cobblemon.fabric.network.FabricBattleSelectionPayload.send(player, "", "");
            FabricBattleGridVisualRenderer.clear(player.getServerWorld(), gridTransform);
            FabricBattleChoiceRuntime.unbind(player.getUuid());
            FabricRpgWorldProtectionRegistry.clear(protectionScopeId);
            playerEntity.discard();
            enemyEntity.discard();
            ACTIVE.remove(player.getUuid(), this);
        }

        private void nameplate(PokemonEntity entity, String name, int hp) {
            entity.setCustomName(Text.literal(name + " | HP " + hp + "/" + scenario.hp() + (hp == 0 ? " | KO" : "")));
            entity.setCustomNameVisible(true);
        }
    }
}
