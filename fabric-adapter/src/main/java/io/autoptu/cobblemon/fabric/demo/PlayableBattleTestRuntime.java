package io.autoptu.cobblemon.fabric.demo;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import io.autoptu.cobblemon.battlecore.BattleChoiceVisualPlan;
import io.autoptu.cobblemon.battlecore.BattleGridCoordinate;
import io.autoptu.cobblemon.battlecore.BattleGridTransform;
import io.autoptu.cobblemon.fabric.battle.FabricBattleGridVisualRenderer;
import io.autoptu.cobblemon.fabric.battle.FabricBattleChoiceRuntime;
import io.autoptu.cobblemon.fabric.network.FabricBattleStatusPayload;
import io.autoptu.cobblemon.battlecore.BattleCoreLegalChoice;
import io.autoptu.cobblemon.battlecore.BattleCoreLegalChoiceSet;
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
                                                    StringArgumentType.getString(context, "opponent"), true)))));
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
            dispatcher.register(root);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (Session session : List.copyOf(ACTIVE.values())) {
                try { session.tick(); }
                catch (RuntimeException error) {
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

    private static int start(ServerCommandSource source, String playerSpeciesId, String opponentSpeciesId, boolean interactive) {
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
        BlockPos playerOrigin = player.getBlockPos().add(2, 0, 0);
        BlockPos enemyOrigin = player.getBlockPos().add(6, 0, 0);
        PokemonEntity playerPokemon = spawn(world, playerSpecies, playerOrigin);
        PokemonEntity enemyPokemon = spawn(world, opponentSpecies, enemyOrigin);

        String protectionScopeId = "battle-demo:" + player.getUuidAsString();
        FabricRpgWorldProtectionRegistry.protect(
                protectionScopeId,
                world.getRegistryKey(),
                playerOrigin.add(-2, -2, -3),
                enemyOrigin.add(2, 3, 3),
                "an AutoPTU battle demo is active here"
        );

        Session session = new Session(
                player,
                displayName(playerSpeciesId),
                displayName(opponentSpeciesId),
                playerPokemon,
                enemyPokemon,
                playerOrigin,
                enemyOrigin,
                protectionScopeId, interactive
        );
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
        return new RuntimeCombatantState(
                id,
                MovementProfile.walking(position, 3),
                DEMO_HP,
                DEMO_HP,
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
        private String pendingMoveId = "demo-strike";
        private BattleGridCoordinate aiShiftAnchor;
        private int aiShiftTicks;
        private int delay = TURN_DELAY_TICKS;
        private int openingMovementPreviewRemaining = OPENING_MOVEMENT_PREVIEW_TICKS;
        private int openingMovementCommitRemaining;
        private int attackDeclarationRemaining;
        private int attackCommitRemaining;
        private BattleGridCoordinate committedAttackAnchor;
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
                String protectionScopeId, boolean interactive
        ) {
            this.player = player;
            this.interactive = interactive;
            if (interactive) openingMovementPreviewRemaining = 0;
            this.playerPokemonName = playerPokemonName;
            this.enemyPokemonName = enemyPokemonName;
            this.playerEntity = playerEntity;
            this.enemyEntity = enemyEntity;
            this.protectionScopeId = protectionScopeId;
            this.playerState = combatant("player-demo", new GridCoord(1, 1));
            this.enemyState = combatant("wild-demo", new GridCoord(5, 1));
            this.runtime = new BattleRuntimeState(
                    new MovementGrid(7, 4, Set.of(), Map.of()),
                    List.of(playerState, enemyState)
            );
            this.random = new PythonRandom(20260823);
            this.gridTransform = BattleGridTransform.from(new BattleArenaSnapshot(
                    player.getServerWorld().getRegistryKey().getValue().toString(),
                    playerOrigin.getX() - 1,
                    playerOrigin.getY(),
                    playerOrigin.getZ() - 1,
                    1, 0, 0, 1
            ));
            this.openingLegalShifts = AutobattlerActionSpace.legalShiftChoices(
                    playerState.combatantId(), runtime.grid(), playerState.movementProfile(),
                    playerState.actionBudget(), 0, ignored -> true);
            this.openingShift = openingLegalShifts.stream()
                    .filter(choice -> choice.destination().equals(new GridCoord(2, 2)))
                    .findFirst()
                    .orElseGet(() -> openingLegalShifts.stream().findFirst()
                            .orElseThrow(() -> new IllegalStateException("demo has no authoritative opening Shift")));
            updateNameplates();
            if (interactive) FabricBattleChoiceRuntime.bindSession(player.getUuid(), "demo:" + player.getUuidAsString(),
                    playerState.combatantId(), gridTransform, this::legalChoices, this::executeChoice,
                    this::endTurn, () -> coordinate(playerState.position()));
            player.lookAt(net.minecraft.command.argument.EntityAnchorArgumentType.EntityAnchor.EYES,
                    new net.minecraft.util.math.Vec3d(gridTransform.origin().x() + 3.5D,
                            gridTransform.origin().y() + 0.5D, gridTransform.origin().z() + 2D));
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
                ShiftChoice selectedShift = legalShifts().stream()
                        .filter(candidate -> candidate.stableKey().equals(choice.stableKey()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("selected Shift is no longer legal"));
                BattleRuntime.applyAction(runtime, selectedShift, ignored -> true);
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
            player.sendMessage(Text.literal("AutoPTU ADMIN DEMO: " + playerPokemonName + " vs " + enemyPokemonName), false);
            player.sendMessage(Text.literal(
                    "Presentation species are operator-selected. AutoPTU-Java owns attack rolls, damage and HP."), false);
            player.sendMessage(Text.literal(
                    "TACTICAL FLOW: green = legal movement, gold = selected destination, red = declared attack."), false);
            if (interactive) player.sendMessage(Text.literal("B: actions | Enter: confirm | Backspace: cancel. End turn in the menu. /autoptu admin battle stop closes this session."), false);
        }

        private void tick() {
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
                FabricBattleStatusPayload.send(player, playerPokemonName, playerState.hp(), DEMO_HP,
                        enemyPokemonName, enemyState.hp(), DEMO_HP, playerState.hp() == 0 ? "DEFEAT" : "VICTORY", round);
                if (--cleanupRemaining <= 0) cleanupNow();
                return;
            }

            renderTacticalGrid();
            if ((player.getServerWorld().getTime() & 3L) == 0L) FabricBattleStatusPayload.send(player, playerPokemonName, playerState.hp(), DEMO_HP,
                    enemyPokemonName, enemyState.hp(), DEMO_HP,
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
                    new BattleChoiceVisualPlan(new BattleChoiceVisualPlan.GridWindow(0, 6, 0, 3), shifts, attacks, highlight),
                    committed, player.getServerWorld().getTime(), coordinate(playerTurn ? playerState.position() : enemyState.position()));
        }

        private void renderOpeningMovementSelection() {
            // Included in the complete arena frame.
        }

        private void commitOpeningShift() {
            BattleRuntime.applyAction(runtime, openingShift, ignored -> true);
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
                    BattleRuntime.applyAction(runtime, shift, cell -> !cell.equals(target.position()));
                    aiShiftAnchor = coordinate(shift.destination());
                    aiShiftTicks = 12;
                    var world = gridTransform.toWorld(aiShiftAnchor);
                    enemyEntity.requestTeleport(world.x() + 0.5D, world.y(), world.z() + 0.5D);
                    delay = 1;
                    return;
                }
            }
            if (moves.isEmpty()) {
                playerTurn = !playerTurn;
                (playerTurn ? playerState : enemyState).actionBudget().resetConsumedActions();
                if (playerTurn) round++;
                delay = TURN_DELAY_TICKS;
                return;
            }
            pendingMoveId = moves.get(0).moveId();
            attackDeclarationRemaining = ATTACK_DECLARATION_TICKS;
            String attackerName = playerTurn ? playerPokemonName : enemyPokemonName;
            String targetName = playerTurn ? enemyPokemonName : playerPokemonName;
            player.sendMessage(Text.literal(attackerName + " locks demo-strike on " + targetName + "."), false);
        }

        private void renderAttackDeclaration() {
            // Included in the complete arena frame.
        }

        private void resolveTurn(String selectedMoveId) {
            RuntimeCombatantState attacker = playerTurn ? playerState : enemyState;
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
            cleanupRemaining = CLEANUP_TICKS;
            player.sendMessage(Text.literal("BATTLE OVER - WINNER: " + winner + " | LOSER: " + loser), false);
            player.sendMessage(Text.literal("This operator demo does not commit XP, items or campaign results."), false);
        }

        private void cleanupNow() {
            FabricBattleStatusPayload.clear(player);
            io.autoptu.cobblemon.fabric.network.FabricBattleSelectionPayload.send(player, "", "");
            FabricBattleGridVisualRenderer.clear(player.getServerWorld(), gridTransform);
            FabricBattleChoiceRuntime.unbind(player.getUuid());
            FabricRpgWorldProtectionRegistry.clear(protectionScopeId);
            playerEntity.discard();
            enemyEntity.discard();
            ACTIVE.remove(player.getUuid(), this);
        }

        private static void nameplate(PokemonEntity entity, String name, int hp) {
            entity.setCustomName(Text.literal(name + " | HP " + hp + "/" + DEMO_HP + (hp == 0 ? " | KO" : "")));
            entity.setCustomNameVisible(true);
        }
    }
}
