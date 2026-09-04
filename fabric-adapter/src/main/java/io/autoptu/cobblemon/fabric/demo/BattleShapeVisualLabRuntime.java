package io.autoptu.cobblemon.fabric.demo;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import io.autoptu.cobblemon.battlecore.BattlePresentationCommand;
import io.autoptu.cobblemon.fabric.presentation.CobblemonPresentationEntityBackend;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** DEV_ONLY/QA VISUAL ONLY lab for readable PTU attack-shape screenshots. */
public final class BattleShapeVisualLabRuntime {
    private static final CobblemonPresentationEntityBackend PRESENTATION = new CobblemonPresentationEntityBackend();
    private static final Map<UUID, Session> ACTIVE = new ConcurrentHashMap<>();

    private BattleShapeVisualLabRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("admin")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("shapeviz")
                                        .executes(ctx -> help(ctx.getSource()))
                                        .then(CommandManager.literal("start")
                                                .executes(ctx -> start(ctx.getSource())))
                                        .then(CommandManager.literal("ranged")
                                                .executes(ctx -> ranged(ctx.getSource())))
                                        .then(CommandManager.literal("aoe")
                                                .executes(ctx -> aoe(ctx.getSource())))
                                        .then(CommandManager.literal("blast")
                                                .executes(ctx -> blast(ctx.getSource())))
                                        .then(CommandManager.literal("line")
                                                .executes(ctx -> line(ctx.getSource())))
                                        .then(CommandManager.literal("reset")
                                                .executes(ctx -> reset(ctx.getSource())))
                                        .then(CommandManager.literal("stop")
                                                .executes(ctx -> stop(ctx.getSource())))))));
    }

    private static int help(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("QA VISUAL ONLY: /autoptu admin shapeviz start|ranged|aoe|blast|line|reset|stop"), false);
        return 1;
    }

    private static int start(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        cleanup(player.getUuid());
        ServerWorld world = player.getServerWorld();
        BlockPos base = player.getBlockPos();
        Session s = new Session();
        s.actors.add(spawn(world, "charizard", base.add(0, 0, 0)));
        s.actors.add(spawn(world, "blastoise", base.add(8, 0, 0)));
        s.actors.add(spawn(world, "venusaur", base.add(8, 0, 2)));
        s.actors.add(spawn(world, "pikachu", base.add(10, 0, -2)));
        s.actors.add(spawn(world, "snorlax", base.add(12, 0, 3)));
        s.origins.add(base.add(0,0,0));
        s.origins.add(base.add(8,0,0));
        s.origins.add(base.add(8,0,2));
        s.origins.add(base.add(10,0,-2));
        s.origins.add(base.add(12,0,3));
        ACTIVE.put(player.getUuid(), s);
        source.sendFeedback(() -> Text.literal("QA VISUAL ONLY shape scene ready: attacker + four targets."), false);
        return 1;
    }

    private static int ranged(ServerCommandSource source) {
        Session s = require(source); if (s == null) return 0;
        animate(s.actors.get(0), s.actors.get(1), "hydropump", 12);
        source.sendFeedback(() -> Text.literal("QA VISUAL ONLY ranged fixture played."), false);
        return 1;
    }

    private static int aoe(ServerCommandSource source) {
        Session s = require(source); if (s == null) return 0;
        animate(s.actors.get(0), s.actors.get(1), "earthquake", 10);
        animate(s.actors.get(0), s.actors.get(2), "earthquake", 10);
        source.sendFeedback(() -> Text.literal("QA VISUAL ONLY AoE fixture: two presentation targets in affected set; others are visual controls."), false);
        return 1;
    }

    private static int blast(ServerCommandSource source) {
        Session s = require(source); if (s == null) return 0;
        animate(s.actors.get(0), s.actors.get(1), "flamethrower", 10);
        animate(s.actors.get(0), s.actors.get(2), "flamethrower", 10);
        animate(s.actors.get(0), s.actors.get(3), "flamethrower", 10);
        source.sendFeedback(() -> Text.literal("QA VISUAL ONLY Blast fixture: three explicitly-authored presentation targets."), false);
        return 1;
    }

    private static int line(ServerCommandSource source) {
        Session s = require(source); if (s == null) return 0;
        reposition(s.actors.get(1), s.origins.get(1));
        reposition(s.actors.get(2), s.origins.get(1).add(2,0,0));
        reposition(s.actors.get(3), s.origins.get(1).add(4,0,0));
        animate(s.actors.get(0), s.actors.get(1), "thunderbolt", 9);
        animate(s.actors.get(0), s.actors.get(2), "thunderbolt", 9);
        animate(s.actors.get(0), s.actors.get(3), "thunderbolt", 9);
        source.sendFeedback(() -> Text.literal("QA VISUAL ONLY line fixture played through three authored presentation targets."), false);
        return 1;
    }

    private static void animate(PokemonEntity attacker, PokemonEntity target, String moveId, int damage) {
        PRESENTATION.animateMove(attacker, target, new BattlePresentationCommand(
                0, 0, BattlePresentationCommand.Kind.MOVE_ANIMATION, "shapeviz-attacker",
                Map.of("targetId", "shapeviz-target", "moveId", moveId, "hit", "true", "crit", "false", "damage", Integer.toString(damage))));
    }

    private static int reset(ServerCommandSource source) {
        Session s = require(source); if (s == null) return 0;
        for (int i = 0; i < s.actors.size(); i++) reposition(s.actors.get(i), s.origins.get(i));
        return 1;
    }

    private static int stop(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer(); if (player == null) return 0;
        cleanup(player.getUuid()); return 1;
    }

    private static Session require(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer(); if (player == null) return null;
        Session s = ACTIVE.get(player.getUuid());
        if (s == null || s.actors.stream().anyMatch(PokemonEntity::isRemoved)) {
            source.sendError(Text.literal("Run /autoptu admin shapeviz start first."));
            return null;
        }
        return s;
    }

    private static PokemonEntity spawn(ServerWorld world, String speciesId, BlockPos pos) {
        Species species = PokemonSpecies.INSTANCE.getByName(speciesId);
        if (species == null) throw new IllegalStateException("missing Cobblemon species " + speciesId);
        Pokemon pokemon = new Pokemon(); pokemon.setSpecies(species);
        PokemonEntity entity = new PokemonEntity(world, pokemon, CobblemonEntities.POKEMON);
        entity.setAiDisabled(true);
        entity.refreshPositionAndAngles(pos.getX()+0.5D, pos.getY(), pos.getZ()+0.5D, 0.0F, 0.0F);
        if (!world.spawnEntity(entity)) throw new IllegalStateException("failed to spawn QA actor");
        return entity;
    }

    private static void reposition(PokemonEntity e, BlockPos pos) {
        e.requestTeleport(pos.getX()+0.5D, pos.getY(), pos.getZ()+0.5D);
    }

    private static void cleanup(UUID playerId) {
        Session s = ACTIVE.remove(playerId);
        if (s == null) return;
        for (PokemonEntity e : s.actors) if (!e.isRemoved()) e.discard();
    }

    private static final class Session {
        final List<PokemonEntity> actors = new ArrayList<>();
        final List<BlockPos> origins = new ArrayList<>();
    }
}
