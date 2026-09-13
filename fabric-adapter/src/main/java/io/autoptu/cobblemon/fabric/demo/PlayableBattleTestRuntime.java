package io.autoptu.cobblemon.fabric.demo;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.fabric.presentation.CobblemonPresentationEntityBackend;
import io.autoptu.cobblemon.fabric.rpg.FabricRpgWorldProtectionRegistry;
import io.autoptu.core.action.ChoiceTargetMode;
import io.autoptu.core.action.MoveChoice;
import io.autoptu.core.action.MoveOption;
import io.autoptu.core.event.MoveResolvedEvent;
import io.autoptu.core.model.ActionType;
import io.autoptu.core.model.GridCoord;
import io.autoptu.core.model.MoveSpec;
import io.autoptu.core.model.MovementGrid;
import io.autoptu.core.model.MovementProfile;
import io.autoptu.core.random.PythonRandom;
import io.autoptu.core.rules.ActionBudget;
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
    private static final int DEMO_HP = 30;
    private static final int TURN_DELAY_TICKS = 30;
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
            var adminBattle = CommandManager.literal("battle").then(adminDemo);
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
                session.tick();
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
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("The AutoPTU battle demo must be started by a player operator."));
            return 0;
        }
        if (ACTIVE.containsKey(player.getUuid())) {
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
                protectionScopeId
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
        return MoveOption.standard(
                "demo-strike",
                new MoveSpec("Ranged", "Ranged", 3, 3, null, null, "Ranged")
        );
    }

    private static MoveResolutionInput demoMoveInput() {
        return new MoveResolutionInput(
                2,
                0,
                0,
                20,
                false,
                false,
                false,
                4,
                10,
                5,
                false,
                1.0,
                List.of()
        );
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
        private final BlockPos playerOrigin;
        private final BlockPos enemyOrigin;
        private final String protectionScopeId;
        private final RuntimeCombatantState playerState;
        private final RuntimeCombatantState enemyState;
        private final BattleRuntimeState runtime;
        private final PythonRandom random;
        private boolean playerTurn = true;
        private int delay = 20;
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
                String protectionScopeId
        ) {
            this.player = player;
            this.playerPokemonName = playerPokemonName;
            this.enemyPokemonName = enemyPokemonName;
            this.playerEntity = playerEntity;
            this.enemyEntity = enemyEntity;
            this.playerOrigin = playerOrigin;
            this.enemyOrigin = enemyOrigin;
            this.protectionScopeId = protectionScopeId;
            this.playerState = combatant("player-demo", new GridCoord(1, 1));
            this.enemyState = combatant("wild-demo", new GridCoord(2, 1));
            this.runtime = new BattleRuntimeState(
                    new MovementGrid(6, 6, Set.of(), Map.of()),
                    List.of(playerState, enemyState)
            );
            this.random = new PythonRandom(20260823);
            updateNameplates();
        }

        private void announceStart() {
            player.sendMessage(Text.literal("AutoPTU ADMIN DEMO: " + playerPokemonName + " vs " + enemyPokemonName), false);
            player.sendMessage(Text.literal(
                    "Presentation species are operator-selected. AutoPTU-Java owns attack rolls, damage and HP."), false);
        }

        private void tick() {
            if (playerEntity.isRemoved() || enemyEntity.isRemoved()) {
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

            if (finished) {
                if (--cleanupRemaining <= 0) cleanupNow();
                return;
            }
            if (--delay > 0) return;

            resolveTurn();
            delay = TURN_DELAY_TICKS;
        }

        private void resolveTurn() {
            RuntimeCombatantState attacker = playerTurn ? playerState : enemyState;
            RuntimeCombatantState target = playerTurn ? enemyState : playerState;
            PokemonEntity attackerEntity = playerTurn ? playerEntity : enemyEntity;
            PokemonEntity targetEntity = playerTurn ? enemyEntity : playerEntity;
            BlockPos attackerOrigin = playerTurn ? playerOrigin : enemyOrigin;
            String attackerName = playerTurn ? playerPokemonName : enemyPokemonName;
            String targetName = playerTurn ? enemyPokemonName : playerPokemonName;

            attacker.actionBudget().resetConsumedActions();
            MoveChoice choice = new MoveChoice(
                    attacker.combatantId(),
                    "demo-strike",
                    ChoiceTargetMode.COMBATANT,
                    target.combatantId(),
                    target.position(),
                    ActionType.STANDARD
            );

            AppliedActionResult applied = BattleRuntime.applyAuthoritativeMove(
                    runtime,
                    choice,
                    demoMove(),
                    "Medium",
                    "Medium",
                    Set.of(),
                    playerTurn ? "Player" : "Wild",
                    random,
                    demoMoveInput()
            );
            MoveResolvedEvent event = (MoveResolvedEvent) applied.events().stream()
                    .filter(MoveResolvedEvent.class::isInstance)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("AutoPTU-Java emitted no MoveResolvedEvent"));

            PRESENTATION.animateMove(attackerEntity, targetEntity, event.moveId());
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

            if (event.targetHp() == 0) {
                finish(attackerName, targetName);
                return;
            }
            playerTurn = !playerTurn;
        }

        private void updateNameplates() {
            nameplate(playerEntity, playerPokemonName, playerState.hp());
            nameplate(enemyEntity, enemyPokemonName, enemyState.hp());
        }

        private void finish(String winner, String loser) {
            finished = true;
            cleanupRemaining = CLEANUP_TICKS;
            player.sendMessage(Text.literal("BATTLE OVER - WINNER: " + winner + " | LOSER: " + loser), false);
            player.sendMessage(Text.literal("This operator demo does not commit XP, items or campaign results."), false);
        }

        private void cleanupNow() {
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