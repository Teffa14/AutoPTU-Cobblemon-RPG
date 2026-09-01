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
    private static final int CLEANUP_TICKS = 80;

    // The demo intentionally uses neutral presentation until a real Cobblemon poser-animation id is
    // available from trusted presentation metadata. PTU damage categories are not animation ids.
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

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            Session session = ACTIVE.remove(handler.player.getUuid());
            if (session != null) session.cleanupNow();
        });
    }

    private static int start(ServerCommandSource source, String speciesName) {
        ServerPlayerEntity player;
        try {
            player = source.getPlayerOrThrow();
        } catch (Exception exception) {
            source.sendError(Text.literal("This command must be run by a player."));
            return 0;
        }

        if (ACTIVE.containsKey(player.getUuid())) {
            source.sendError(Text.literal("A test battle is already active."));
            return 0;
        }

        Species playerSpecies = PokemonSpecies.getByName(speciesName);
        Species enemySpecies = PokemonSpecies.getByName("rattata");
        if (playerSpecies == null || enemySpecies == null) {
            source.sendError(Text.literal("Cobblemon species data is not ready."));
            return 0;
        }

        ServerWorld world = player.getServerWorld();
        BlockPos playerOrigin = player.getBlockPos().add(2, 0, 0);
        BlockPos enemyOrigin = player.getBlockPos().add(6, 0, 0);

        PokemonEntity playerEntity = spawn(world, playerSpecies, playerOrigin);
        PokemonEntity enemyEntity = spawn(world, enemySpecies, enemyOrigin);
        if (playerEntity == null || enemyEntity == null) {
            if (playerEntity != null) playerEntity.discard();
            if (enemyEntity != null) enemyEntity.discard();
            source.sendError(Text.literal("Could not materialize Cobblemon battle entities."));
            return 0;
        }

        Session session = new Session(
                player,
                playerEntity,
                enemyEntity,
                playerOrigin,
                enemyOrigin,
                playerSpecies.getName(),
                enemySpecies.getName()
        );
        ACTIVE.put(player.getUuid(), session);
        session.begin();
        return 1;
    }

    private static PokemonEntity spawn(ServerWorld world, Species species, BlockPos pos) {
        Pokemon pokemon = new Pokemon();
        pokemon.setSpecies(species);
        pokemon.setLevel(5);
        pokemon.setCurrentHealth(DEMO_HP);

        PokemonEntity entity = CobblemonEntities.POKEMON.create(world);
        if (entity == null) return null;
        entity.setPokemon(pokemon);
        entity.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        if (!world.spawnEntity(entity)) return null;
        return entity;
    }

    private static final class Session {
        private final ServerPlayerEntity player;
        private final PokemonEntity playerEntity;
        private final PokemonEntity enemyEntity;
        private final BlockPos playerOrigin;
        private final BlockPos enemyOrigin;
        private final String playerPokemonName;
        private final String enemyPokemonName;
        private final BattleRuntime runtime;
        private BattleRuntimeState state;
        private RuntimeCombatantState playerState;
        private RuntimeCombatantState enemyState;
        private final PythonRandom random;
        private boolean playerTurn = true;
        private int delay = 20;
        private boolean finished;
        private int cleanupRemaining;

        private Session(
                ServerPlayerEntity player,
                PokemonEntity playerEntity,
                PokemonEntity enemyEntity,
                BlockPos playerOrigin,
                BlockPos enemyOrigin,
                String playerPokemonName,
                String enemyPokemonName
        ) {
            this.player = player;
            this.playerEntity = playerEntity;
            this.enemyEntity = enemyEntity;
            this.playerOrigin = playerOrigin;
            this.enemyOrigin = enemyOrigin;
            this.playerPokemonName = playerPokemonName;
            this.enemyPokemonName = enemyPokemonName;
            this.runtime = new BattleRuntime();
            this.random = new PythonRandom(1337L);

            MovementGrid grid = MovementGrid.rectangular(12, 6);
            MovementProfile movement = MovementProfile.overland(6);
            playerState = new RuntimeCombatantState("player", DEMO_HP, new GridCoord(1, 2), movement);
            enemyState = new RuntimeCombatantState("enemy", DEMO_HP, new GridCoord(5, 2), movement);
            state = BattleRuntimeState.builder(grid)
                    .combatant(playerState)
                    .combatant(enemyState)
                    .actionBudget("player", ActionBudget.standard())
                    .actionBudget("enemy", ActionBudget.standard())
                    .build();
        }

        private void begin() {
            protect(playerEntity);
            protect(enemyEntity);
            player.sendMessage(Text.literal(
                    "AutoPTU test battle: " + playerPokemonName + " vs " + enemyPokemonName
                            + ". Auto-resolving authoritative turns..."
            ), false);
        }

        private void protect(PokemonEntity entity) {
            FabricRpgWorldProtectionRegistry.protect(entity.getUuid());
        }

        private void tick() {
            if (player.isDisconnected()) {
                cleanupNow();
                return;
            }

            if (finished) {
                if (--cleanupRemaining <= 0) cleanupNow();
                return;
            }

            if (--delay <= 0) {
                resolveTurn();
                delay = TURN_DELAY_TICKS;
            }
        }

        private void resolveTurn() {
            RuntimeCombatantState attacker = playerTurn ? playerState : enemyState;
            RuntimeCombatantState target = playerTurn ? enemyState : playerState;
            PokemonEntity attackerEntity = playerTurn ? playerEntity : enemyEntity;
            PokemonEntity targetEntity = playerTurn ? enemyEntity : playerEntity;
            String attackerName = playerTurn ? playerPokemonName : enemyPokemonName;
            String targetName = playerTurn ? enemyPokemonName : playerPokemonName;

            MoveSpec move = new MoveSpec(
                    "demo-strike",
                    ActionType.STANDARD,
                    2,
                    6,
                    20,
                    false
            );
            MoveOption option = new MoveOption(
                    move,
                    ChoiceTargetMode.COMBATANT,
                    Set.of(target.id()),
                    6,
                    true
            );
            MoveChoice choice = new MoveChoice(attacker.id(), option, target.id());

            MoveResolutionInput resolutionInput = new MoveResolutionInput(
                    choice,
                    10,
                    0,
                    0,
                    0,
                    Set.of()
            );
            AppliedActionResult result = runtime.resolveAndApplyMove(state, resolutionInput, random);
            state = result.state();
            MoveResolvedEvent event = result.events().stream()
                    .filter(MoveResolvedEvent.class::isInstance)
                    .map(MoveResolvedEvent.class::cast)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("AutoPTU-Java emitted no MoveResolvedEvent"));

            PRESENTATION.animateMove(attackerEntity, targetEntity, event.moveId());

            if (event.hit()) {
                PRESENTATION.projectDisplayedHealth(targetEntity, event.targetHp(), event.damage());
            }

            player.sendMessage(Text.literal(
                    attackerName + " used demo-strike on " + targetName
                            + ": hit=" + event.hit()
                            + ", damage=" + event.damage()
                            + ", targetHp=" + event.targetHp()
            ), false);

            playerState = state.combatant("player");
            enemyState = state.combatant("enemy");
            if (playerState.hp() <= 0 || enemyState.hp() <= 0) {
                finished = true;
                cleanupRemaining = CLEANUP_TICKS;
                player.sendMessage(Text.literal("AutoPTU test battle resolved. Entities will clean up."), false);
                return;
            }
            playerTurn = !playerTurn;
        }

        private void cleanupNow() {
            ACTIVE.remove(player.getUuid(), this);
            FabricRpgWorldProtectionRegistry.release(playerEntity.getUuid());
            FabricRpgWorldProtectionRegistry.release(enemyEntity.getUuid());
            if (!playerEntity.isRemoved()) playerEntity.discard();
            if (!enemyEntity.isRemoved()) enemyEntity.discard();
        }
    }
}
