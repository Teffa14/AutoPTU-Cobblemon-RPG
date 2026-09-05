package io.autoptu.cobblemon.fabric.presentation;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/**
 * Small project-owned signature layered over a reused native Cobblemon ActionEffect.
 *
 * This exists only so two AutoPTU moves that borrow the same Cobblemon asset do not look identical.
 * It does not change the native timeline, Pokemon scale, world position, hit state or any battle
 * result. Miss accents are deliberately placed beyond the target and never imply target impact.
 */
final class CobblemonSubstituteAnimationAccent {
    private CobblemonSubstituteAnimationAccent() {
    }

    static void render(
            PokemonEntity attacker,
            PokemonEntity target,
            String moveId,
            boolean hit,
            int variant
    ) {
        if (variant < 0 || variant > 3) throw new IllegalArgumentException("variant must be 0..3");
        if (!(attacker.getWorld() instanceof ServerWorld world)) return;

        BattleMoveAnimationProfile profile = BattleMoveAnimationProfile.resolve(moveId);
        ParticleEffect particle = themedParticle(profile.theme());
        Vec3d source = new Vec3d(attacker.getX(), attacker.getBodyY(0.62D), attacker.getZ());
        Vec3d destination = new Vec3d(target.getX(), target.getBodyY(0.58D), target.getZ());

        if (!hit && attacker != target) {
            renderMissSignature(world, particle, source, destination, variant);
            return;
        }

        switch (variant) {
            case 0 -> compactRing(world, particle, destination, 0.22D, 7);
            case 1 -> {
                compactRing(world, particle, source.add(0.0D, 0.18D, 0.0D), 0.18D, 6);
                compactRing(world, ParticleTypes.END_ROD, destination, 0.28D, 8);
            }
            case 2 -> verticalSpiral(world, particle, destination, 0.18D, 9);
            case 3 -> {
                renderThinTrail(world, particle, source, destination, 6, 0.18D);
                crossBurst(world, particle, destination, 0.32D);
            }
            default -> throw new IllegalStateException("unreachable variant " + variant);
        }
        world.playSound(
                null,
                destination.x,
                destination.y,
                destination.z,
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                SoundCategory.PLAYERS,
                0.18F,
                1.35F + variant * 0.08F
        );
    }

    private static void renderMissSignature(
            ServerWorld world,
            ParticleEffect particle,
            Vec3d source,
            Vec3d target,
            int variant
    ) {
        Vec3d direction = target.subtract(source);
        Vec3d horizontal = new Vec3d(direction.x, 0.0D, direction.z);
        Vec3d forward = horizontal.lengthSquared() > 0.0001D
                ? horizontal.normalize()
                : new Vec3d(0.0D, 0.0D, 1.0D);
        Vec3d side = new Vec3d(-forward.z, 0.0D, forward.x);
        double sideOffset = (variant % 2 == 0 ? 1.0D : -1.0D) * (0.22D + variant * 0.05D);
        Vec3d miss = target.add(forward.multiply(0.65D + variant * 0.08D)).add(side.multiply(sideOffset));
        renderThinTrail(world, particle, target, miss, 4 + variant, 0.12D + variant * 0.04D);
        world.spawnParticles(ParticleTypes.POOF, miss.x, miss.y, miss.z, 3 + variant, 0.10D, 0.08D, 0.10D, 0.01D);
    }

    private static void compactRing(
            ServerWorld world,
            ParticleEffect particle,
            Vec3d center,
            double radius,
            int points
    ) {
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            spawn(world, particle, center.add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius));
        }
    }

    private static void verticalSpiral(
            ServerWorld world,
            ParticleEffect particle,
            Vec3d center,
            double radius,
            int points
    ) {
        for (int i = 0; i < points; i++) {
            double t = i / (double) Math.max(1, points - 1);
            double angle = Math.PI * 2.0D * 1.5D * t;
            spawn(world, particle, center.add(
                    Math.cos(angle) * radius,
                    -0.28D + t * 0.58D,
                    Math.sin(angle) * radius
            ));
        }
    }

    private static void crossBurst(
            ServerWorld world,
            ParticleEffect particle,
            Vec3d center,
            double radius
    ) {
        spawn(world, particle, center.add(radius, 0.0D, 0.0D));
        spawn(world, particle, center.add(-radius, 0.0D, 0.0D));
        spawn(world, particle, center.add(0.0D, radius, 0.0D));
        spawn(world, particle, center.add(0.0D, -radius, 0.0D));
        spawn(world, particle, center.add(0.0D, 0.0D, radius));
        spawn(world, particle, center.add(0.0D, 0.0D, -radius));
    }

    private static void renderThinTrail(
            ServerWorld world,
            ParticleEffect particle,
            Vec3d source,
            Vec3d target,
            int steps,
            double arcHeight
    ) {
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3d point = source.lerp(target, t).add(0.0D, Math.sin(Math.PI * t) * arcHeight, 0.0D);
            spawn(world, particle, point);
        }
    }

    private static void spawn(ServerWorld world, ParticleEffect particle, Vec3d point) {
        world.spawnParticles(particle, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static ParticleEffect themedParticle(BattleMoveAnimationProfile.Theme theme) {
        return switch (theme) {
            case FIRE -> ParticleTypes.FLAME;
            case WATER -> ParticleTypes.SPLASH;
            case ELECTRIC -> ParticleTypes.ELECTRIC_SPARK;
            case ICE -> ParticleTypes.SNOWFLAKE;
            case GRASS, BUG -> ParticleTypes.HAPPY_VILLAGER;
            case PSYCHIC -> ParticleTypes.ENCHANT;
            case GHOST -> ParticleTypes.PORTAL;
            case POISON -> ParticleTypes.WITCH;
            case GROUND, FLYING -> ParticleTypes.CLOUD;
            case ROCK -> ParticleTypes.POOF;
            case DRAGON -> ParticleTypes.DRAGON_BREATH;
            case FAIRY -> ParticleTypes.END_ROD;
            case STEEL, FIGHTING, NORMAL -> ParticleTypes.CRIT;
            case DARK -> ParticleTypes.SMOKE;
        };
    }
}
