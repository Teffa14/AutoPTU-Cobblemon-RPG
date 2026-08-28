package io.autoptu.cobblemon.fabric.presentation;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.battlecore.BattlePresentationCommand;
import io.autoptu.cobblemon.battlecore.PresentationEntityPlatformBackend;
import io.autoptu.cobblemon.battlecore.WorldBlockCoordinate;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.Objects;

/**
 * Applies presentation-only commands to an already-resolved live Cobblemon PokemonEntity.
 *
 * This class never reads the entity to make PTU decisions. Every value applied here is an
 * already-authoritative presentation output. Cobblemon health is only a write-through mirror and
 * must never be read back into canonical PTU battle state.
 */
public final class CobblemonPresentationEntityBackend
        implements PresentationEntityPlatformBackend<PokemonEntity> {

    @Override
    public void animateMove(PokemonEntity attacker, PokemonEntity target, String moveId) {
        Objects.requireNonNull(attacker, "attacker");
        Objects.requireNonNull(target, "target");
        if (moveId == null || moveId.isBlank()) throw new IllegalArgumentException("moveId is required");

        // Presentation-only facing/lunge. Runtime/grid position is unchanged; callers return the
        // entity to its authoritative presentation anchor after the cue. No range, hit chance,
        // movement legality or move-specific effect is inferred here.
        double dx = target.getX() - attacker.getX();
        double dz = target.getZ() - attacker.getZ();
        if (dx != 0.0D || dz != 0.0D) {
            float yaw = (float) (MathHelper.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
            attacker.setYaw(yaw);
            attacker.setHeadYaw(yaw);
            attacker.setBodyYaw(yaw);
        }

        double nextX = attacker.getX() + dx * 0.45D;
        double nextY = attacker.getY();
        double nextZ = attacker.getZ() + dz * 0.45D;
        attacker.requestTeleport(nextX, nextY, nextZ);

        if (attacker.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                    ParticleTypes.SWEEP_ATTACK,
                    target.getX(),
                    target.getBodyY(0.55D),
                    target.getZ(),
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D
            );
        }
    }

    @Override
    public void projectDisplayedHealth(PokemonEntity entity, int targetHp, int damage) {
        Objects.requireNonNull(entity, "entity");
        if (targetHp < 0) throw new IllegalArgumentException("targetHp cannot be negative");
        if (damage < 0) throw new IllegalArgumentException("damage cannot be negative");

        entity.getPokemon().setCurrentHealth(targetHp);
        int projectedHp = entity.getPokemon().getCurrentHealth();
        if (projectedHp != targetHp) {
            throw new IllegalStateException(
                    "Cobblemon cannot exactly mirror authoritative PTU HP " + targetHp
                            + " for presentation; projected " + projectedHp);
        }

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
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(command, "command");

        if (command.kind() != BattlePresentationCommand.Kind.STATUS_SKIP_CUE) {
            // Other semantic cues stay accepted but presentation-neutral until their bounded UX
            // slices ship. Minecraft must not invent meanings for generic upstream effects.
            return;
        }
        if (!(entity.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        // status_skip is already an authoritative AutoPTU-Java outcome. Fabric only mirrors that
        // fact with a generic particle cue and nearby action-bar text. It never decides whether the
        // status exists, whether an action is skipped, or what the status mechanically does.
        serverWorld.spawnParticles(
                ParticleTypes.EFFECT,
                entity.getX(),
                entity.getBodyY(0.75D),
                entity.getZ(),
                8,
                0.35D,
                0.25D,
                0.35D,
                0.02D
        );

        Text cueText = Text.literal(statusSkipText(command));
        for (var player : serverWorld.getPlayers()) {
            if (player.squaredDistanceTo(entity) <= 1024.0D) {
                player.sendMessage(cueText, true);
            }
        }
    }

    static String statusSkipText(BattlePresentationCommand command) {
        Objects.requireNonNull(command, "command");
        if (command.kind() != BattlePresentationCommand.Kind.STATUS_SKIP_CUE) {
            throw new IllegalArgumentException("STATUS_SKIP_CUE command is required");
        }

        String status = displayValue(command.data().get("status"), "status");
        String phase = displayValue(command.data().get("phase"), "phase");
        String reason = displayValue(command.data().get("reason"), "authoritative skip");
        return status + " · " + phase + " · " + reason;
    }

    private static String displayValue(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value.strip();
    }
}
