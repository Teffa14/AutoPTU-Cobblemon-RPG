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
    private static final int CUE_TICKS = 44;
    private static final double AOE_RADIUS = 2.35D;
    private static final double BLAST_LENGTH = 10.5D;
    private static final double LINE_HALF_WIDTH = 0.62D;

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
        resetLayout(s);
        animate(s.actors.get(0), s.actors.get(1), "hydropump", 12);
        arm(s, Shape.RANGED);
        source.sendFeedback(() -> Text.literal("QA VISUAL ONLY ranged fixture: one narrow moving projectile and one impact point."), false);
        return 1;
    }

    private static int aoe(ServerCommandSource source) {
        Session s = require(source); if (s == null) return 0;
        resetLayout(s);
        animate(s.actors.get(0), s.actors.get(1), "earthquake", 10);
        animate(s.actors.get(0), s.actors.get(2), "earthquake", 10);
        arm(s, Shape.AOE);
        source.sendFeedback(() -> Text.literal("QA VISUAL ONLY AoE fixture: circular ground footprint around the authored center; Blastoise/Venusaur are in-set controls."), false);
        return 1;
    }

    private static int blast(ServerCommandSource source) {
        Session s = require(source); if (s == null) return 0;
        resetLayout(s);
        animate(s.actors.get(0), s.actors.get(1), "flamethrower", 10);
        animate(s.actors.get(0), s.actors.get(2), "flamethrower", 10);
        animate(s.actors.get(0), s.actors.get(3), "flamethrower", 10);
        arm(s, Shape.BLAST);
        source.sendFeedback(() -> Text.literal("QA VISUAL ONLY Blast fixture: widening cone from attacker; three explicitly-authored presentation targets."), false);
        return 1;
    }

    private static int line(ServerCommandSource source) {
        Session s = require(source); if (s == null) return 0;
        resetLayout(s);
        reposition(s.actors.get(1), s.origins.get(1));
        reposition(s.actors.get(2), s.origins.get(1).add(2,0,0));
        reposition(s.actors.get(3), s.origins.get(1).add(4,0,0));
        animate(s.actors.get(0), s.actors.get(1), "thunderbolt", 9);
        animate(s.actors.get(0), s.actors.get(2), "thunderbolt", 9);
        animate(s.actors.get(0), s.actors.get(3), "thunderbolt", 9);
        arm(s, Shape.LINE);
        source.sendFeedback(() -> Text.literal("QA VISUAL ONLY Line fixture: constant-width corridor through three authored presentation targets."), false);
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
        double progress = 1.0D - (s.cueTicks / (double) CUE_TICKS);
        switch (s.shape) {
            case RANGED -> renderRanged(world, attacker, s.actors.get(1), progress);
            case AOE -> renderAoe(world, s.actors.get(1), progress);
            case BLAST -> renderBlast(world, attacker, s.actors.get(1), progress);
            case LINE -> renderLine(world, attacker, s.actors.get(3), progress);
            case NONE -> { }
        }
        s.cueTicks--;
        if (s.cueTicks == 0) s.shape = Shape.NONE;
    }

    /** A moving narrow projectile. It intentionally does not draw a persistent corridor. */
    private static void renderRanged(ServerWorld world, PokemonEntity attacker, PokemonEntity target, double progress) {
        Vec3d from = body(attacker, 0.62D);
        Vec3d to = body(target, 0.58D);
        double headT = Math.min(1.0D, Math.max(0.0D, progress * 1.35D));
        double tailT = Math.max(0.0D, headT - 0.18D);
        Vec3d tail = from.lerp(to, tailT);
        Vec3d head = from.lerp(to, headT);
        trail(world, tail, head, 10, ParticleTypes.END_ROD);
        world.spawnParticles(ParticleTypes.SPLASH, head.x, head.y, head.z, 5, 0.13D, 0.13D, 0.13D, 0.03D);
        if (headT >= 0.96D) {
            ring(world, new Vec3d(to.x, target.getY() + 0.18D, to.z), 0.58D, 18, ParticleTypes.SPLASH);
            world.spawnParticles(ParticleTypes.CLOUD, to.x, to.y, to.z, 8, 0.32D, 0.25D, 0.32D, 0.03D);
        }
    }

    /** A flat circular footprint with a persistent outer boundary and an expanding inner pulse. */
    private static void renderAoe(ServerWorld world, PokemonEntity centerEntity, double progress) {
        Vec3d center = new Vec3d(centerEntity.getX(), centerEntity.getY() + 0.13D, centerEntity.getZ());
        ring(world, center, AOE_RADIUS, 56, ParticleTypes.ELECTRIC_SPARK);
        ring(world, center, AOE_RADIUS * 0.66D, 40, ParticleTypes.ELECTRIC_SPARK);
        double pulseRadius = Math.max(0.28D, AOE_RADIUS * Math.min(1.0D, progress * 1.65D));
        ring(world, center.add(0.0D, 0.07D, 0.0D), pulseRadius, 36, ParticleTypes.END_ROD);
        for (int spoke = 0; spoke < 8; spoke++) {
            double angle = Math.PI * 2.0D * spoke / 8.0D;
            Vec3d edge = center.add(Math.cos(angle) * AOE_RADIUS, 0.0D, Math.sin(angle) * AOE_RADIUS);
            dottedSegment(world, center, edge, 6, ParticleTypes.ELECTRIC_SPARK);
        }
        world.spawnParticles(ParticleTypes.CLOUD, center.x, center.y + 0.10D, center.z, 6, 0.65D, 0.08D, 0.65D, 0.01D);
    }

    /** A widening triangular cone. Side rails and cross-sections make the widening footprint explicit. */
    private static void renderBlast(ServerWorld world, PokemonEntity attacker, PokemonEntity primaryTarget, double progress) {
        Vec3d origin = ground(attacker);
        Vec3d toward = ground(primaryTarget).subtract(origin);
        Vec3d forward = new Vec3d(toward.x, 0.0D, toward.z).normalize();
        if (forward.lengthSquared() < 0.000001D) return;
        Vec3d side = new Vec3d(-forward.z, 0.0D, forward.x);

        double farHalfWidth = blastHalfWidth(BLAST_LENGTH);
        Vec3d farCenter = origin.add(forward.multiply(BLAST_LENGTH));
        Vec3d farLeft = farCenter.add(side.multiply(farHalfWidth));
        Vec3d farRight = farCenter.subtract(side.multiply(farHalfWidth));
        trail(world, origin, farLeft, 42, ParticleTypes.FLAME);
        trail(world, origin, farRight, 42, ParticleTypes.FLAME);

        for (int section = 1; section <= 5; section++) {
            double distance = BLAST_LENGTH * section / 5.0D;
            double halfWidth = blastHalfWidth(distance);
            Vec3d center = origin.add(forward.multiply(distance));
            Vec3d left = center.add(side.multiply(halfWidth));
            Vec3d right = center.subtract(side.multiply(halfWidth));
            trail(world, left, right, 14, ParticleTypes.FLAME);
        }

        double frontDistance = Math.max(0.8D, BLAST_LENGTH * Math.min(1.0D, progress * 1.45D));
        double frontHalfWidth = blastHalfWidth(frontDistance);
        Vec3d frontCenter = origin.add(forward.multiply(frontDistance));
        trail(world,
                frontCenter.add(side.multiply(frontHalfWidth)),
                frontCenter.subtract(side.multiply(frontHalfWidth)),
                20,
                ParticleTypes.END_ROD);
        world.spawnParticles(ParticleTypes.FLAME, frontCenter.x, frontCenter.y + 0.10D, frontCenter.z,
                7, frontHalfWidth * 0.42D, 0.12D, frontHalfWidth * 0.42D, 0.02D);
    }

    /** A constant-width rectangular corridor, visually distinct from the moving Ranged projectile and Blast cone. */
    private static void renderLine(ServerWorld world, PokemonEntity attacker, PokemonEntity farTarget, double progress) {
        Vec3d from = ground(attacker);
        Vec3d to = ground(farTarget);
        Vec3d delta = to.subtract(from);
        Vec3d forward = new Vec3d(delta.x, 0.0D, delta.z).normalize();
        if (forward.lengthSquared() < 0.000001D) return;
        Vec3d side = new Vec3d(-forward.z, 0.0D, forward.x);
        Vec3d leftStart = from.add(side.multiply(LINE_HALF_WIDTH));
        Vec3d rightStart = from.subtract(side.multiply(LINE_HALF_WIDTH));
        Vec3d leftEnd = to.add(side.multiply(LINE_HALF_WIDTH));
        Vec3d rightEnd = to.subtract(side.multiply(LINE_HALF_WIDTH));
        trail(world, leftStart, leftEnd, 44, ParticleTypes.END_ROD);
        trail(world, rightStart, rightEnd, 44, ParticleTypes.END_ROD);

        for (int section = 0; section <= 6; section++) {
            double t = section / 6.0D;
            Vec3d center = from.lerp(to, t);
            trail(world,
                    center.add(side.multiply(LINE_HALF_WIDTH)),
                    center.subtract(side.multiply(LINE_HALF_WIDTH)),
                    8,
                    ParticleTypes.ELECTRIC_SPARK);
        }

        double pulseT = Math.min(1.0D, progress * 1.45D);
        Vec3d pulseCenter = from.lerp(to, pulseT);
        trail(world,
                pulseCenter.add(side.multiply(LINE_HALF_WIDTH)),
                pulseCenter.subtract(side.multiply(LINE_HALF_WIDTH)),
                12,
                ParticleTypes.ELECTRIC_SPARK);
    }

    static double blastHalfWidth(double distance) {
        if (!Double.isFinite(distance) || distance < 0.0D) throw new IllegalArgumentException("blast distance must be finite and non-negative");
        return 0.15D + distance * 0.23D;
    }

    static double aoeRadius() {
        return AOE_RADIUS;
    }

    static double lineHalfWidth() {
        return LINE_HALF_WIDTH;
    }

    private static void ring(ServerWorld world, Vec3d center, double radius, int points, net.minecraft.particle.ParticleEffect particle) {
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            world.spawnParticles(particle,
                    center.x + Math.cos(angle) * radius,
                    center.y,
                    center.z + Math.sin(angle) * radius,
                    1, 0.01D, 0.01D, 0.01D, 0.0D);
        }
    }

    private static void dottedSegment(ServerWorld world, Vec3d from, Vec3d to, int points, net.minecraft.particle.ParticleEffect particle) {
        for (int i = 1; i <= points; i++) {
            double t = i / (double) points;
            Vec3d p = from.lerp(to, t);
            world.spawnParticles(particle, p.x, p.y, p.z, 1, 0.01D, 0.01D, 0.01D, 0.0D);
        }
    }

    private static void trail(ServerWorld world, Vec3d from, Vec3d to, int points, net.minecraft.particle.ParticleEffect particle) {
        for (int i = 0; i <= points; i++) {
            double t = i / (double) points;
            Vec3d p = from.lerp(to, t);
            world.spawnParticles(particle, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static Vec3d body(PokemonEntity entity, double height) {
        return new Vec3d(entity.getX(), entity.getBodyY(height), entity.getZ());
    }

    private static Vec3d ground(PokemonEntity entity) {
        return new Vec3d(entity.getX(), entity.getY() + 0.14D, entity.getZ());
    }

    private static void animate(PokemonEntity attacker, PokemonEntity target, String moveId, int damage) {
        PRESENTATION.animateMove(attacker, target, new BattlePresentationCommand(
                0, 0, BattlePresentationCommand.Kind.MOVE_ANIMATION, "shapeviz-attacker",
                Map.of("targetId", "shapeviz-target", "moveId", moveId, "hit", "true", "crit", "false", "damage", Integer.toString(damage))));
    }

    private static int reset(ServerCommandSource source) {
        Session s = require(source); if (s == null) return 0;
        resetLayout(s);
        s.shape = Shape.NONE;
        s.cueTicks = 0;
        return 1;
    }

    private static void resetLayout(Session s) {
        for (int i = 0; i < s.actors.size(); i++) reposition(s.actors.get(i), s.origins.get(i));
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
