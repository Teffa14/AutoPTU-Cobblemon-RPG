package io.autoptu.cobblemon.fabric.demo;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
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
 * This is intentionally a narrow 1v1 test harness, not the general battle materializer. AutoPTU-Java
 * owns accuracy rolls, damage, action consumption and authoritative HP mutation. Fabric/Cobblemon
 * only select a server-owned demo scenario and project its semantic move result into visible entities.
 * Statuses, abilities, items, terrain, Trainer Features, forced movement and rewards are disabled.
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
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("testbattle")
                                .then(CommandManager.literal("bulbasaur")
                                        .executes(context -> start(context.getSource(), "bulbasaur")))
                                .then(CommandManager.literal("charmander")
                                        .executes(context -> start(context.getSource(), "charmander")))
                                .then(CommandManager.literal("squirtle")
                                        .executes(context -> start(context.getSource(), "squirtle"))))));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (Session session : List.copyOf(ACTIVE.values())) {
                session.tick();
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            handler.player.sendMessage(Text.literal("AutoPTU playable battle test is installed."), false);
            handler.player.sendMessage(Text.literal(
                    "Choose a Pokemon: /autoptu testbattle bulbasaur, charmander, or squirtle"), false);
        });
    }

    private static int start(ServerCommandSource source, String starterId) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("This test battle must be started by a player."));
            return 0;
        }
        if (ACTIVE.containsKey(player.getUuid())) {
            source.sendError(Text.literal("You already have an AutoPTU test battle running."));
            return 0;
        }

        Species starter = PokemonSpecies.INSTANCE.getByName(starterId);
        Species opponent = PokemonSpecies.INSTANCE.getByName("pikachu");
        if (starter == null || opponent == null) {
            source.sendError(Text.literal("Cobblemon species data is not ready."));
            return 0;
        }

        ServerWorld world = player.getServerWorld();
        BlockPos playerOrigin = player.getBlockPos().add(2, 0, 0);
        BlockPos enemyOrigin = player.getBlockPos().add(6, 0, 0);
        PokemonEntity playerPokemon = spawn(world, starter, playerOrigin);
        PokemonEntity enemyPokemon = spawn(world, opponent, enemyOrigin);

        String protectionScopeId = "battle-demo:" + player.getUuidAsString();
        FabricRpgWorldProtectionRegistry.protect(
                protectionScopeId,
                world.getRegistryKey(),
                playerOrigin.add(-2, -2, -3),
                enemyOrigin.add(2, 3, 3),
                "an AutoPTU battle is active here"
        );

        Session session = new Session(
                player,
                displayName(starterId),
                "Pikachu",
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
            throw new IllegalStateException("failed to spawn playable AutoPTU battle PokemonEntity");
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
        // These are server-owned scenario inputs to the upstream resolver. DB 4 is a real supported
        // PTU table entry; attack/defense are chosen so every landed hit advances the visible demo.
        // Rolls, crit state, damage arithmetic, action consumption and HP mutation remain in Java.
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
        return Character.toUpperCase(speciesId.charAt(0)) + speciesId.substring(1);
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
            player.sendMessage(Text.literal("AutoPTU TEST: " + playerPokemonName + " vs " + enemyPokemonName), false);
            player.sendMessage(Text.literal("Auto battle started. AutoPTU-Java owns attack rolls, damage and HP."), false);
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
            player.sendMessage(Text.literal("This first vertical test does not commit XP, items or campaign results."), false);
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
