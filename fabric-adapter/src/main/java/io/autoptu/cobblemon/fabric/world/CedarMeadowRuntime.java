package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * First real Ouros overworld runtime slice.
 *
 * The command builds a small authored habitat and materializes persistent Cobblemon actors. A coarse
 * server-owned behavior controller makes the lookout react to nearby players and moves the feeding
 * group toward shelter after an alarm. No battle, PTU status, stat modifier or ecological truth is
 * derived from Minecraft AI.
 */
public final class CedarMeadowRuntime {
    private static final List<Instance> INSTANCES = new CopyOnWriteArrayList<>();

    private CedarMeadowRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("ouros")
                        .then(CommandManager.literal("world")
                                .then(CommandManager.literal("cedar_meadow")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(context -> build(context.getSource()))))));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<Instance> iterator = INSTANCES.iterator();
            while (iterator.hasNext()) {
                Instance instance = iterator.next();
                if (!instance.tick()) {
                    INSTANCES.remove(instance);
                }
            }
        });
    }

    private static int build(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Cedar Meadow must be placed by a player."));
            return 0;
        }

        ServerWorld world = player.getServerWorld();
        BlockPos origin = player.getBlockPos().add(0, -1, 20);
        CedarMeadowBuilder.BuildResult layout = CedarMeadowBuilder.build(world, origin);

        PokemonEntity lookout = spawn(world, "sentret", layout.lookoutPerch());
        List<PokemonEntity> feeders = new ArrayList<>();
        feeders.add(spawn(world, "hoppip", origin.add(-2, 1, 2)));
        feeders.add(spawn(world, "hoppip", origin.add(1, 1, 3)));
        feeders.add(spawn(world, "skwovet", origin.add(3, 1, 1)));

        if (lookout == null || feeders.stream().anyMatch(entity -> entity == null)) {
            discard(lookout);
            feeders.forEach(CedarMeadowRuntime::discard);
            source.sendError(Text.literal("Cobblemon species data was not ready. Meadow blocks remain for inspection."));
            return 0;
        }

        lookout.setCustomName(Text.literal("Cedar Lookout"));
        lookout.setPersistent();
        feeders.forEach(PokemonEntity::setPersistent);

        INSTANCES.add(new Instance(world, layout, lookout, feeders));
        player.sendMessage(Text.literal("Ouros Cedar Meadow built 20 blocks ahead."), false);
        player.sendMessage(Text.literal("Approach slowly and watch the wild Pokemon react to distance."), false);
        return 1;
    }

    private static PokemonEntity spawn(ServerWorld world, String speciesId, BlockPos position) {
        Species species = PokemonSpecies.INSTANCE.getByName(speciesId);
        if (species == null) {
            return null;
        }
        Pokemon pokemon = new Pokemon();
        pokemon.setSpecies(species);
        PokemonEntity entity = new PokemonEntity(world, pokemon, CobblemonEntities.POKEMON);
        entity.setPersistent();
        entity.refreshPositionAndAngles(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D, 0.0F, 0.0F);
        return world.spawnEntity(entity) ? entity : null;
    }

    private static void discard(PokemonEntity entity) {
        if (entity != null && !entity.isRemoved()) {
            entity.discard();
        }
    }

    private static final class Instance {
        private final ServerWorld world;
        private final CedarMeadowBuilder.BuildResult layout;
        private final PokemonEntity lookout;
        private final List<PokemonEntity> feeders;
        private final CedarMeadowBehavior behavior = new CedarMeadowBehavior();
        private CedarMeadowBehavior.State priorState = CedarMeadowBehavior.State.CALM;
        private int tickCounter;

        private Instance(
                ServerWorld world,
                CedarMeadowBuilder.BuildResult layout,
                PokemonEntity lookout,
                List<PokemonEntity> feeders
        ) {
            this.world = world;
            this.layout = layout;
            this.lookout = lookout;
            this.feeders = List.copyOf(feeders);
        }

        private boolean tick() {
            if (lookout.isRemoved()) {
                return false;
            }
            tickCounter++;
            if (tickCounter % 10 != 0) {
                return true;
            }

            ServerPlayerEntity nearest = world.getClosestPlayer(
                    layout.origin().getX() + 0.5D,
                    layout.origin().getY() + 1.0D,
                    layout.origin().getZ() + 0.5D,
                    24.0D,
                    false
            );
            double distance = nearest == null
                    ? Double.POSITIVE_INFINITY
                    : Math.sqrt(nearest.squaredDistanceTo(lookout));
            CedarMeadowBehavior.State state = behavior.update(distance, nearest != null);

            switch (state) {
                case CALM -> calm();
                case WATCHING -> watch(nearest);
                case ALARMED -> alarm(nearest);
                case RECOVERING -> recover();
            }

            if (state != priorState && nearest != null) {
                announce(nearest, state);
            }
            priorState = state;
            return true;
        }

        private void calm() {
            BlockPos perch = layout.lookoutPerch();
            lookout.getNavigation().startMovingTo(perch.getX() + 0.5D, perch.getY(), perch.getZ() + 0.5D, 0.8D);
            for (int i = 0; i < feeders.size(); i++) {
                PokemonEntity feeder = feeders.get(i);
                if (feeder.isRemoved()) {
                    continue;
                }
                double x = layout.origin().getX() + (i * 2.5D) - 2.0D;
                double z = layout.origin().getZ() + 2.0D + (i % 2);
                feeder.getNavigation().startMovingTo(x, layout.origin().getY() + 1.0D, z, 0.55D);
            }
        }

        private void watch(ServerPlayerEntity player) {
            if (player != null) {
                lookout.getLookControl().lookAt(player, 30.0F, 30.0F);
            }
            feeders.forEach(entity -> entity.getNavigation().stop());
        }

        private void alarm(ServerPlayerEntity player) {
            if (player != null) {
                lookout.getLookControl().lookAt(player, 40.0F, 40.0F);
            }
            BlockPos shelter = layout.shelter();
            for (PokemonEntity feeder : feeders) {
                if (!feeder.isRemoved()) {
                    feeder.getNavigation().startMovingTo(
                            shelter.getX() + 0.5D,
                            shelter.getY(),
                            shelter.getZ() + 0.5D,
                            1.15D
                    );
                }
            }
            lookout.getNavigation().startMovingTo(
                    shelter.getX() - 1.0D,
                    shelter.getY(),
                    shelter.getZ() - 1.0D,
                    1.1D
            );
        }

        private void recover() {
            feeders.forEach(entity -> entity.getNavigation().stop());
        }

        private void announce(ServerPlayerEntity player, CedarMeadowBehavior.State state) {
            Text message = switch (state) {
                case WATCHING -> Text.literal("The lookout stops scanning the meadow and watches your approach.");
                case ALARMED -> Text.literal("The feeding group abandons the open ground and heads for cover.");
                case RECOVERING -> Text.literal("The meadow stays quiet. The group has not returned to feeding yet.");
                case CALM -> Text.literal("After a while, the meadow settles back into its ordinary rhythm.");
            };
            player.sendMessage(message, true);
        }
    }
}
