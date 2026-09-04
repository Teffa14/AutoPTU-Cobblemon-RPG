package io.autoptu.cobblemon.fabric.presentation;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.battlecore.BattlePresentationCommand;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

/**
 * Renders already-authoritative combatant semantic cues without interpreting PTU mechanics.
 *
 * Every label and value comes directly from the validated presentation command. Particle shape and
 * sound are presentation vocabulary only. This renderer never decides whether a turn starts, a
 * phase changes, a rule effect applies, or a status prevents action.
 */
final class BattleSemanticCueRenderer {
    private BattleSemanticCueRenderer() {
    }

    static void render(PokemonEntity entity, BattlePresentationCommand command) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(command, "command");
        if (!(entity.getWorld() instanceof ServerWorld world)) return;

        switch (command.kind()) {
            case STATUS_SKIP_CUE -> renderStatusSkip(world, entity, command);
            case TURN_START_CUE -> renderTurnStart(world, entity, command);
            case TURN_END_CUE -> renderTurnEnd(world, entity, command);
            case PHASE_CUE -> renderPhase(world, entity, command);
            case RULE_EFFECT_CUE -> renderRuleEffect(world, entity, command);
            default -> {
                // Trainer/field cues do not have a combatant presentation binding in the current
                // gateway. Move/HP/relocation commands use dedicated paths. Never fabricate one.
            }
        }
    }

    static String cueText(BattlePresentationCommand command) {
        Objects.requireNonNull(command, "command");
        return switch (command.kind()) {
            case STATUS_SKIP_CUE -> display(command.data().get("status"), "status")
                    + " · " + display(command.data().get("phase"), "phase")
                    + " · " + display(command.data().get("reason"), "authoritative skip");
            case TURN_START_CUE -> "Turn start · round " + required(command, "round")
                    + " · " + required(command, "phase")
                    + " · initiative " + required(command, "initiativeIndex");
            case TURN_END_CUE -> "Turn end · round " + required(command, "round")
                    + " · " + required(command, "phase");
            case PHASE_CUE -> "Phase · round " + required(command, "round")
                    + " · " + required(command, "phase");
            case RULE_EFFECT_CUE -> required(command, "sourceName")
                    + " · " + required(command, "effect")
                    + " · " + required(command, "amount");
            default -> throw new IllegalArgumentException("command kind has no combatant cue text");
        };
    }

    private static void renderStatusSkip(
            ServerWorld world,
            PokemonEntity entity,
            BattlePresentationCommand command
    ) {
        Vec3d center = center(entity);
        spawn(world, ParticleTypes.EFFECT, center, 10, 0.38D, 0.28D, 0.38D, 0.025D);
        ring(world, ParticleTypes.WITCH, center, 0.62D, 12, 0.05D);
        play(world, center, SoundEvents.BLOCK_FIRE_EXTINGUISH, 0.38F, 1.45F);
        sendNearby(world, entity, cueText(command));
    }

    private static void renderTurnStart(
            ServerWorld world,
            PokemonEntity entity,
            BattlePresentationCommand command
    ) {
        Vec3d center = center(entity);
        ring(world, ParticleTypes.END_ROD, center, 0.72D, 18, 0.04D);
        ring(world, ParticleTypes.ENCHANT, center.add(0.0D, 0.28D, 0.0D), 0.48D, 12, 0.0D);
        spawn(world, ParticleTypes.CRIT, center, 8, 0.25D, 0.35D, 0.25D, 0.04D);
        play(world, center, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.55F, 1.35F);
        sendNearby(world, entity, cueText(command));
    }

    private static void renderTurnEnd(
            ServerWorld world,
            PokemonEntity entity,
            BattlePresentationCommand command
    ) {
        Vec3d center = center(entity);
        ring(world, ParticleTypes.POOF, center, 0.68D, 14, -0.03D);
        spawn(world, ParticleTypes.SMOKE, center, 8, 0.30D, 0.22D, 0.30D, 0.01D);
        play(world, center, SoundEvents.BLOCK_FIRE_EXTINGUISH, 0.28F, 1.8F);
        sendNearby(world, entity, cueText(command));
    }

    private static void renderPhase(
            ServerWorld world,
            PokemonEntity entity,
            BattlePresentationCommand command
    ) {
        Vec3d center = center(entity);
        for (int layer = 0; layer < 3; layer++) {
            ring(
                    world,
                    ParticleTypes.ENCHANT,
                    center.add(0.0D, layer * 0.28D - 0.15D, 0.0D),
                    0.42D + layer * 0.16D,
                    12,
                    layer * 0.18D
            );
        }
        play(world, center, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.35F, 1.65F);
        sendNearby(world, entity, cueText(command));
    }

    private static void renderRuleEffect(
            ServerWorld world,
            PokemonEntity entity,
            BattlePresentationCommand command
    ) {
        Vec3d center = center(entity);
        // Intentionally neutral: source/effect names are displayed, but Minecraft does not classify
        // the authoritative rule effect as positive, negative, damage, healing, status, or movement.
        ring(world, ParticleTypes.EFFECT, center, 0.55D, 16, 0.08D);
        ring(world, ParticleTypes.END_ROD, center.add(0.0D, 0.30D, 0.0D), 0.38D, 10, -0.05D);
        spawn(world, ParticleTypes.ENCHANT, center, 10, 0.30D, 0.36D, 0.30D, 0.02D);
        play(world, center, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.42F, 0.9F);
        sendNearby(world, entity, cueText(command));
    }

    private static void ring(
            ServerWorld world,
            ParticleEffect particle,
            Vec3d center,
            double radius,
            int points,
            double verticalStep
    ) {
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            double y = verticalStep * Math.sin(angle * 2.0D);
            spawnOne(
                    world,
                    particle,
                    center.add(Math.cos(angle) * radius, y, Math.sin(angle) * radius)
            );
        }
    }

    private static Vec3d center(PokemonEntity entity) {
        return new Vec3d(entity.getX(), entity.getBodyY(0.62D), entity.getZ());
    }

    private static void sendNearby(ServerWorld world, PokemonEntity entity, String message) {
        Text text = Text.literal(message);
        for (var player : world.getPlayers()) {
            if (player.squaredDistanceTo(entity) <= 1024.0D) {
                player.sendMessage(text, true);
            }
        }
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

    private static void play(
            ServerWorld world,
            Vec3d point,
            net.minecraft.sound.SoundEvent sound,
            float volume,
            float pitch
    ) {
        world.playSound(null, point.x, point.y, point.z, sound, SoundCategory.PLAYERS, volume, pitch);
    }

    private static String required(BattlePresentationCommand command, String key) {
        String value = command.data().get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " is required for " + command.kind());
        }
        return value.strip();
    }

    private static String display(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value.strip();
    }
}
