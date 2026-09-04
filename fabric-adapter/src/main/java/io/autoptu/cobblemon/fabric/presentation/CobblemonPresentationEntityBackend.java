package io.autoptu.cobblemon.fabric.presentation;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.battlecore.BattlePresentationCommand;
import io.autoptu.cobblemon.battlecore.PresentationEntityPlatformBackend;
import io.autoptu.cobblemon.battlecore.WorldBlockCoordinate;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

/**
 * Applies presentation-only commands to an already-resolved live Cobblemon PokemonEntity.
 *
 * This class never reads the entity to make PTU decisions. Every value received here is an
 * already-authoritative presentation output. Cobblemon native HP is deliberately not used as a
 * mirror of PTU HP: the two systems use different HP scales, and this backend currently receives
 * authoritative current HP but not authoritative max HP. Exact PTU HP is therefore rendered as a
 * presentation-only nameplate. Until the presentation contract carries max HP, mutating native
 * Cobblemon HP would either clamp the value or manufacture a native faint that AutoPTU did not emit.
 */
public final class CobblemonPresentationEntityBackend
        implements PresentationEntityPlatformBackend<PokemonEntity> {

    @Override
    public void animateMove(PokemonEntity attacker, PokemonEntity target, String moveId) {
        renderResolvedMove(attacker, target, BattleMoveAnimationProfile.resolve(moveId), true, false);
    }

    @Override
    public void animateMove(
            PokemonEntity attacker,
            PokemonEntity target,
            BattlePresentationCommand command
    ) {
        Objects.requireNonNull(command, "command");
        if (command.kind() != BattlePresentationCommand.Kind.MOVE_ANIMATION) {
            throw new IllegalArgumentException("command must be MOVE_ANIMATION");
        }
        String moveId = command.data().get("moveId");
        BattleMoveAnimationProfile profile = BattleMoveAnimationProfile.resolve(moveId);
        boolean hit = authoritativeFlag(command, "hit");
        boolean crit = authoritativeFlag(command, "crit");
        if (!hit && crit) {
            throw new IllegalArgumentException("authoritative miss cannot be critical");
        }
        renderResolvedMove(attacker, target, profile, hit, crit);
    }

    private static void renderResolvedMove(
            PokemonEntity attacker,
            PokemonEntity target,
            BattleMoveAnimationProfile profile,
            boolean hit,
            boolean crit
    ) {
        Objects.requireNonNull(attacker, "attacker");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(profile, "profile");

        face(attacker, target);
        if (!(attacker.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        Vec3d source = bodyPoint(attacker, 0.62D);
        Vec3d destination = bodyPoint(target, 0.58D);

        // Miss is not inferred from damage/HP. This branch exists only when the authoritative
        // move_resolved event explicitly says hit=false. The visual travels past the target and
        // deliberately avoids impact feedback on the target entity.
        if (!hit) {
            renderMiss(serverWorld, source, destination, profile, attacker == target);
            return;
        }

        // A move whose already-authoritative endpoints bind to the same presentation entity is
        // rendered as an aura. This is endpoint presentation only; it does not infer move targeting.
        if (attacker == target) {
            renderAura(serverWorld, source, profile);
        } else {
            switch (profile.motion()) {
                case MELEE -> renderMelee(serverWorld, source, destination, profile);
                case PROJECTILE -> renderProjectile(serverWorld, source, destination, profile);
                case BEAM -> renderBeam(serverWorld, source, destination, profile);
                case WAVE -> renderWave(serverWorld, source, destination, profile);
                case BURST -> renderBurst(serverWorld, source, destination, profile);
                case ARC -> renderArc(serverWorld, source, destination, profile);
            }
        }

        // Crit is likewise copied from the authoritative command, never inferred from damage.
        if (crit) {
            renderCritical(serverWorld, destination, profile);
        }
    }

    @Override
    public void projectDisplayedHealth(PokemonEntity entity, int targetHp, int damage) {
        Objects.requireNonNull(entity, "entity");
        if (targetHp < 0) throw new IllegalArgumentException("targetHp cannot be negative");
        if (damage < 0) throw new IllegalArgumentException("damage cannot be negative");

        // targetHp is authoritative PTU state, but it is not on Cobblemon's native HP scale.
        // Do not write it into Pokemon.currentHealth and do not infer a ratio without authoritative
        // max HP. In particular, targetHp == 0 is NOT permission to trigger Cobblemon faint/death;
        // faint presentation remains blocked until AutoPTU emits an explicit semantic faint contract.
        entity.setCustomName(Text.literal(authoritativeHpNameplate(targetHp)));
        entity.setCustomNameVisible(true);

        // Damage is already authoritative upstream. Fabric only mirrors that committed result with
        // generic audiovisual impact feedback. Zero-damage projections stay presentation-neutral.
        if (damage > 0 && entity.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                    ParticleTypes.DAMAGE_INDICATOR,
                    entity.getX(),
                    entity.getBodyY(0.65D),
                    entity.getZ(),
                    Math.min(12, Math.max(2, damage / 5 + 2)),
                    0.35D,
                    0.25D,
                    0.35D,
                    0.08D
            );
            serverWorld.playSound(
                    null,
                    entity.getBlockPos(),
                    SoundEvents.ENTITY_PLAYER_ATTACK_STRONG,
                    SoundCategory.PLAYERS,
                    0.45F,
                    1.15F
            );
        }
    }

    static String authoritativeHpNameplate(int targetHp) {
        if (targetHp < 0) throw new IllegalArgumentException("targetHp cannot be negative");
        return "PTU HP " + targetHp;
    }

    static boolean authoritativeFlag(BattlePresentationCommand command, String key) {
        Objects.requireNonNull(command, "command");
        if (key == null || key.isBlank()) throw new IllegalArgumentException("key is required");
        String value = command.data().get(key.strip());
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new IllegalArgumentException(key + " must be an authoritative boolean");
        }
        return Boolean.parseBoolean(value);
    }

    @Override
    public void relocate(
            PokemonEntity entity,
            WorldBlockCoordinate origin,
            WorldBlockCoordinate destination) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(destination, "destination");

        entity.requestTeleport(destination.x() + 0.5D, destination.y(), destination.z() + 0.5D);
    }

    @Override
    public void showCue(PokemonEntity entity, BattlePresentationCommand command) {
        BattleSemanticCueRenderer.render(entity, command);
    }

    static String statusSkipText(BattlePresentationCommand command) {
        Objects.requireNonNull(command, "command");
        if (command.kind() != BattlePresentationCommand.Kind.STATUS_SKIP_CUE) {
            throw new IllegalArgumentException("STATUS_SKIP_CUE command is required");
        }
        return BattleSemanticCueRenderer.cueText(command);
    }

    private static void face(PokemonEntity attacker, PokemonEntity target) {
        double dx = target.getX() - attacker.getX();
        double dz = target.getZ() - attacker.getZ();
        if (dx == 0.0D && dz == 0.0D) return;
        float yaw = (float) (MathHelper.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        attacker.setYaw(yaw);
        attacker.setHeadYaw(yaw);
        attacker.setBodyYaw(yaw);
    }

    private static Vec3d bodyPoint(PokemonEntity entity, double bodyHeight) {
        return new Vec3d(entity.getX(), entity.getBodyY(bodyHeight), entity.getZ());
    }

    private static void renderMelee(
            ServerWorld world,
            Vec3d source,
            Vec3d target,
            BattleMoveAnimationProfile profile
    ) {
        ParticleEffect themed = themedParticle(profile.theme());
        spawn(world, ParticleTypes.CRIT, source, 7, 0.28D, 0.22D, 0.28D, 0.08D);
        renderTrail(world, themed, source, target, 7, 0.18D, false);
        spawn(world, ParticleTypes.SWEEP_ATTACK, target, 2, 0.12D, 0.10D, 0.12D, 0.0D);
        spawn(world, themed, target, 10, 0.30D, 0.25D, 0.30D, 0.08D);
        play(world, target, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.8F, 1.0F);
    }

    private static void renderProjectile(
            ServerWorld world,
            Vec3d source,
            Vec3d target,
            BattleMoveAnimationProfile profile
    ) {
        ParticleEffect themed = themedParticle(profile.theme());
        renderTrail(world, themed, source, target, 13, 0.42D, true);
        renderTrail(world, ParticleTypes.END_ROD, source, target, 7, 0.30D, true);
        spawn(world, themed, target, 14, 0.32D, 0.28D, 0.32D, 0.10D);
        spawn(world, ParticleTypes.POOF, target, 4, 0.22D, 0.18D, 0.22D, 0.03D);
        play(world, source, SoundEvents.ENTITY_ARROW_SHOOT, 0.65F, 1.15F);
    }

    private static void renderBeam(
            ServerWorld world,
            Vec3d source,
            Vec3d target,
            BattleMoveAnimationProfile profile
    ) {
        ParticleEffect themed = themedParticle(profile.theme());
        renderTrail(world, themed, source, target, 24, 0.0D, false);
        renderTrail(world, ParticleTypes.END_ROD, source, target, 12, 0.0D, false);
        spawn(world, themed, target, 18, 0.30D, 0.30D, 0.30D, 0.05D);
        spawn(world, ParticleTypes.END_ROD, target, 6, 0.22D, 0.22D, 0.22D, 0.03D);
        play(world, source, SoundEvents.BLOCK_BEACON_ACTIVATE, 0.6F, 1.35F);
    }

    private static void renderWave(
            ServerWorld world,
            Vec3d source,
            Vec3d target,
            BattleMoveAnimationProfile profile
    ) {
        ParticleEffect themed = themedParticle(profile.theme());
        Vec3d delta = target.subtract(source);
        Vec3d horizontal = new Vec3d(delta.x, 0.0D, delta.z);
        Vec3d side = horizontal.lengthSquared() > 0.0001D
                ? new Vec3d(-horizontal.z, 0.0D, horizontal.x).normalize()
                : new Vec3d(1.0D, 0.0D, 0.0D);

        for (int i = 0; i <= 14; i++) {
            double t = i / 14.0D;
            Vec3d center = source.lerp(target, t);
            double width = Math.sin(Math.PI * t) * 0.85D;
            spawnOne(world, themed, center);
            spawnOne(world, themed, center.add(side.multiply(width)));
            spawnOne(world, themed, center.subtract(side.multiply(width)));
        }
        spawn(world, ParticleTypes.POOF, target, 8, 0.45D, 0.18D, 0.45D, 0.04D);
        play(world, source, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.6F, 0.75F);
    }

    private static void renderBurst(
            ServerWorld world,
            Vec3d source,
            Vec3d target,
            BattleMoveAnimationProfile profile
    ) {
        ParticleEffect themed = themedParticle(profile.theme());
        renderTrail(world, themed, source, target, 8, 0.18D, true);
        for (int i = 0; i < 24; i++) {
            double angle = (Math.PI * 2.0D * i) / 24.0D;
            double radius = 0.35D + (i % 3) * 0.22D;
            Vec3d point = target.add(Math.cos(angle) * radius, ((i % 5) - 2) * 0.12D, Math.sin(angle) * radius);
            spawnOne(world, themed, point);
        }
        spawn(world, ParticleTypes.POOF, target, 14, 0.55D, 0.35D, 0.55D, 0.08D);
        play(world, target, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 0.75F, 1.25F);
    }

    private static void renderArc(
            ServerWorld world,
            Vec3d source,
            Vec3d target,
            BattleMoveAnimationProfile profile
    ) {
        ParticleEffect themed = themedParticle(profile.theme());
        renderTrail(world, themed, source, target, 15, 0.75D, true);
        renderTrail(world, ParticleTypes.END_ROD, source, target, 8, 0.60D, true);
        spawn(world, themed, target, 10, 0.30D, 0.25D, 0.30D, 0.06D);
        spawn(world, ParticleTypes.SWEEP_ATTACK, target, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        play(world, source, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.45F, 1.1F);
    }

    private static void renderAura(
            ServerWorld world,
            Vec3d center,
            BattleMoveAnimationProfile profile
    ) {
        ParticleEffect themed = themedParticle(profile.theme());
        for (int ring = 0; ring < 3; ring++) {
            double radius = 0.35D + ring * 0.28D;
            double height = -0.15D + ring * 0.35D;
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2.0D * i) / 12.0D;
                spawnOne(world, themed, center.add(Math.cos(angle) * radius, height, Math.sin(angle) * radius));
            }
        }
        spawn(world, ParticleTypes.END_ROD, center.add(0.0D, 0.35D, 0.0D), 8, 0.30D, 0.35D, 0.30D, 0.02D);
        play(world, center, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5F, 1.45F);
    }

    private static void renderMiss(
            ServerWorld world,
            Vec3d source,
            Vec3d target,
            BattleMoveAnimationProfile profile,
            boolean selfTarget
    ) {
        ParticleEffect themed = themedParticle(profile.theme());
        if (selfTarget) {
            spawn(world, ParticleTypes.POOF, source, 8, 0.45D, 0.35D, 0.45D, 0.03D);
            play(world, source, SoundEvents.BLOCK_FIRE_EXTINGUISH, 0.35F, 1.5F);
            return;
        }

        Vec3d direction = target.subtract(source);
        Vec3d horizontal = new Vec3d(direction.x, 0.0D, direction.z);
        Vec3d forward = horizontal.lengthSquared() > 0.0001D
                ? horizontal.normalize()
                : new Vec3d(0.0D, 0.0D, 1.0D);
        Vec3d side = new Vec3d(-forward.z, 0.0D, forward.x);
        Vec3d missPoint = target.add(forward.multiply(0.85D)).add(side.multiply(0.45D));

        boolean arc = profile.motion() == BattleMoveAnimationProfile.Motion.PROJECTILE
                || profile.motion() == BattleMoveAnimationProfile.Motion.ARC
                || profile.motion() == BattleMoveAnimationProfile.Motion.BURST;
        int steps = profile.motion() == BattleMoveAnimationProfile.Motion.BEAM ? 22 : 14;
        renderTrail(world, themed, source, missPoint, steps, arc ? 0.55D : 0.0D, arc);
        renderTrail(world, ParticleTypes.END_ROD, source, missPoint, Math.max(6, steps / 2), arc ? 0.42D : 0.0D, arc);
        spawn(world, ParticleTypes.POOF, missPoint, 5, 0.20D, 0.18D, 0.20D, 0.03D);
        play(world, missPoint, SoundEvents.ENTITY_ARROW_SHOOT, 0.35F, 1.65F);
    }

    private static void renderCritical(
            ServerWorld world,
            Vec3d target,
            BattleMoveAnimationProfile profile
    ) {
        ParticleEffect themed = themedParticle(profile.theme());
        spawn(world, ParticleTypes.CRIT, target, 22, 0.48D, 0.40D, 0.48D, 0.16D);
        spawn(world, ParticleTypes.END_ROD, target, 12, 0.38D, 0.34D, 0.38D, 0.08D);
        spawn(world, themed, target, 12, 0.42D, 0.34D, 0.42D, 0.10D);
        play(world, target, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 0.9F, 0.9F);
    }

    private static void renderTrail(
            ServerWorld world,
            ParticleEffect particle,
            Vec3d source,
            Vec3d target,
            int steps,
            double arcHeight,
            boolean arc
    ) {
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3d point = source.lerp(target, t);
            if (arc) {
                point = point.add(0.0D, Math.sin(Math.PI * t) * arcHeight, 0.0D);
            }
            spawnOne(world, particle, point);
        }
    }

    private static ParticleEffect themedParticle(BattleMoveAnimationProfile.Theme theme) {
        return switch (theme) {
            case FIRE -> ParticleTypes.FLAME;
            case WATER -> ParticleTypes.SPLASH;
            case ELECTRIC -> ParticleTypes.ELECTRIC_SPARK;
            case ICE -> ParticleTypes.SNOWFLAKE;
            case GRASS -> ParticleTypes.HAPPY_VILLAGER;
            case PSYCHIC -> ParticleTypes.ENCHANT;
            case GHOST -> ParticleTypes.PORTAL;
            case POISON -> ParticleTypes.WITCH;
            case GROUND -> ParticleTypes.CLOUD;
            case ROCK -> ParticleTypes.POOF;
            case DRAGON -> ParticleTypes.DRAGON_BREATH;
            case FAIRY -> ParticleTypes.END_ROD;
            case FLYING -> ParticleTypes.CLOUD;
            case STEEL -> ParticleTypes.CRIT;
            case DARK -> ParticleTypes.SMOKE;
            case BUG -> ParticleTypes.HAPPY_VILLAGER;
            case FIGHTING -> ParticleTypes.CRIT;
            case NORMAL -> ParticleTypes.CRIT;
        };
    }

    private static void spawnOne(ServerWorld world, ParticleEffect particle, Vec3d point) {
        world.spawnParticles(particle, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static void spawn(
            ServerWorld world,
            ParticleEffect particle,
            Vec3d point,
            int count,
            double dx,
            double dy,
            double dz,
            double speed
    ) {
        world.spawnParticles(particle, point.x, point.y, point.z, count, dx, dy, dz, speed);
    }

    private static void play(ServerWorld world, Vec3d point, SoundEvent sound, float volume, float pitch) {
        world.playSound(null, point.x, point.y, point.z, sound, SoundCategory.PLAYERS, volume, pitch);
    }
}