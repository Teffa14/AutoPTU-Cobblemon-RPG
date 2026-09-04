package io.autoptu.cobblemon.fabric.demo;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import io.autoptu.cobblemon.battlecore.BattlePresentationCommand;
import io.autoptu.cobblemon.fabric.presentation.CobblemonPresentationEntityBackend;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** DEV_ONLY/QA VISUAL ONLY lab for readable PTU attack-shape screenshots. */
public final class BattleShapeVisualLabRuntime {
    private static final CobblemonPresentationEntityBackend PRESENTATION = new CobblemonPresentationEntityBackend();
    private static final Map<UUID, Session> ACTIVE = new ConcurrentHashMap<>();
    private static final int CUE_TICKS = 36;

    private BattleShapeVisualLabRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("admin")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("shapeviz")
                                        .executes(ctx -> help(ctx.getSource()))
                                        .then(CommandManager.literal("start").executes(ctx -> start(ctx.getSource())))
                                        .then(CommandManager.literal("ranged").executes(ctx -> ranged(ctx.getSource())))
                                        .then(CommandManager.literal("aoe").executes(ctx -> aoe(ctx.getSource())))
                                        .then(CommandManager.literal("blast").executes(ctx -> blast(ctx.getSource())))
                                        .then(CommandManager.literal("line").executes(ctx -> line(ctx.getSource())))
                                        .then(CommandManager.literal("reset").executes(ctx -> reset(ctx.getSource())))
                                        .then(CommandManager.literal("stop").executes(ctx -> stop(ctx.getSource())))))));
        ServerTickEvents.END_SERVER_TICK.register(server -> ACTIVE.values().forEach(BattleShapeVisualLabRuntime::tickCue));
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
        arm(s, Shape.RANGED);
        source.sendFeedback(() -> Text.literal("QA VISUAL ONLY ranged fixture played."), false);
        return 1;
    }

    private static int aoe(ServerCommandSource source) {
        Session s = require(source); if (s == null) return 0;
        animate(s.actors.get(0), s.actors.get(1), "earthquake", 10);
        animate(s.actors.get(0), s.actors.get(2), "earthquake", 10);
        arm(s, Shape.AOE);
        source.sendFeedback(() -> Text.literal("QA VISUAL ONLY AoE fixture: two presentation targets in affected set; others are visual controls."), false);
        return 1;
    }

    private static int blast(ServerCommandSource source) {
        Session s = require(source); if (s == null) return 0;
        animate(s.actors.get(0), s.actors.get(1), "flamethrower", 10);
        animate(s.actors.get(0), s.actors.get(2), "flamethrower", 10);
        animate(s.actors.get(0), s.actors.get(3), "flamethrower", 10);
        arm(s, Shape.BLAST);
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
        arm(s, Shape.LINE);
        source.sendFeedback(() -> Text.literal("QA VISUAL ONLY line fixture played through three authored presentation targets."), false);
        return 1;
    }

    private static void arm(Session s, Shape shape) {
        s.shape = shape;
        s.cueTicks = CUE_TICKS;
    }

    private static void tickCue(Session s) {
        if (s.cueTicks <= 0 || s.shape == Shape.NONE || s.actors.stream().anyMatch(PokemonEntity::isRemoved)) return;
        PokemonEntity attacker = s.actors.get(0);
        if (!(attacker.getWorld() instanceof ServerWorld world)) return;
        switch (s.shape) {
            case RANGED -> renderRanged(world, attacker, s.actors.get(1));
            case AOE -> renderAoe(world, s.actors.get(1));
            case BLAST -> renderBlast(world, attacker, s.actors.get(1));
            case LINE -> renderLine(world, attacker, s.actors.get(3));
            case NONE -> { }
        }
        s.cueTicks--;
        if (s.cueTicks == 0) s.shape = Shape.NONE;
    }

    private static void renderRanged(ServerWorld world, PokemonEntity attacker, PokemonEntity target) {
        Vec3d from = body(attacker, 0.62D);
        Vec3d to = body(target, 0.58D);
        trail(world, from, to, 24, 0.0D);
        world.spawnParticles(ParticleTypes.SPLASH, to.x, to.y, to.z, 12, 0.45D, 0.35D, 0.45D, 0.08D);
    }

    private static void renderAoe(ServerWorld world, PokemonEntity centerEntity) {
        Vec3d center = body(centerEntity, 0.15D);
        for (int ring = 1; ring <= 3; ring++) {
            double radius = ring * 1.05D;
            for (int i = 0; i < 32; i++) {
                double angle = Math.PI * 2.0D * i / 32.0D;
                world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                        center.x + Math.cos(angle) * radius, center.y + 0.12D, center.z + Math.sin(angle) * radius,
                        1, 0.02D, 0.02D, 0.02D, 0.0D);
            }
        }
        world.spawnParticles(ParticleTypes.CLOUD, center.x, center.y + 0.2D, center.z, 8, 1.4D, 0.12D, 1.4D, 0.02D);
    }

    private static void renderBlast(ServerWorld world, PokemonEntity attacker, PokemonEntity primaryTarget) {
        Vec3d from = body(attacker, 0.55D);
        Vec3d forward = body(primaryTarget, 0.45D).subtract(from);
        Vec3d horizontal = new Vec3d(forward.x, 0.0D, forward.z).normalize();
        Vec3d side = new Vec3d(-horizontal.z, 0.0D, horizontal.x);
        for (int step = 1; step <= 12; step++) {
            double distance = step * 0.72D;
            double halfWidth = 0.18D + distance * 0.22D;
            Vec3d center = from.add(horizontal.multiply(distance));
            for (int lane = -2; lane <= 2; lane++) {
                double offset = halfWidth * lane / 2.0D;
                Vec3d p = center.add(side.multiply(offset));
                world.spawnParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 1, 0.04D, 0.05D, 0.04D, 0.0D);
            }
        }
    }

    private static void renderLine(ServerWorld world, PokemonEntity attacker, PokemonEntity farTarget) {
        Vec3d from = body(attacker, 0.58D);
        Vec3d to = body(farTarget, 0.50D);
        trail(world, from, to, 36, 0.0D);
        trail(world, from.add(0.0D, 0.18D, 0.0D), to.add(0.0D, 0.18D, 0.0D), 36, 0.0D);
    }

    private static void trail(ServerWorld world, Vec3d from, Vec3d to, int points, double spread) {
        for (int i = 0; i <= points; i++) {
            double t = i / (double) points;
            Vec3d p = from.lerp(to, t);
            world.spawnParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, spread, spread, spread, 0.0D);
        }
    }

    private static Vec3d body(PokemonEntity entity, double height) {
        return new Vec3d(entity.getX(), entity.getBodyY(height), entity.getZ());
    }

    private static void animate(PokemonEntity attacker, PokemonEntity target, String moveId, int damage) {
        PRESENTATION.animateMove(attacker, target, new BattlePresentationCommand(
                0, 0, BattlePresentationCommand.Kind.MOVE_ANIMATION, "shapeviz-attacker",
                Map.of("targetId", "shapeviz-target", "moveId", moveId, "hit", "true", "crit", "false", "damage", Integer.toString(damage))));
    }

    private static int reset(ServerCommandSource source) {
        Session s = require(source); if (s == null) return 0;
        for (int i = 0; i < s.actors.size(); i++) reposition(s.actors.get(i), s.origins.get(i));
        s.shape = Shape.NONE;
        s.cueTicks = 0;
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

    private enum Shape { NONE, RANGED, AOE, BLAST, LINE }

    private static final class Session {
        final List<PokemonEntity> actors = new ArrayList<>();
        final List<BlockPos> origins = new ArrayList<>();
        Shape shape = Shape.NONE;
        int cueTicks;
    }
}
