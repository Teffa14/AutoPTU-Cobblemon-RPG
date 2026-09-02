package io.autoptu.cobblemon.fabric.presentation;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.battlecore.BattlePresentationCommand;
import io.autoptu.cobblemon.battlecore.PresentationEntityPlatformBackend;
import io.autoptu.cobblemon.battlecore.WorldBlockCoordinate;
import io.autoptu.cobblemon.fabric.network.FabricBattleHpPresentationNetworking;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;

import java.util.Objects;

/**
 * Applies presentation-only commands to an already-resolved live Cobblemon PokemonEntity.
 *
 * This class never reads the entity to make PTU decisions. Every value received here is an
 * already-authoritative presentation output. Cobblemon native HP is deliberately not used as a
 * mirror of PTU HP: the two systems use different HP scales, and this backend currently receives
 * authoritative current HP but not authoritative max HP. Current PTU HP is projected to clients
 * through an AutoPTU-owned presentation payload. Until the presentation contract carries max HP,
 * mutating native Cobblemon HP would either clamp the value or manufacture a native faint that
 * AutoPTU did not emit.
 */
public final class CobblemonPresentationEntityBackend
        implements PresentationEntityPlatformBackend<PokemonEntity> {

    @Override
    public void animateMove(PokemonEntity attacker, PokemonEntity target, String moveId) {
        Objects.requireNonNull(attacker, "attacker");
        Objects.requireNonNull(target, "target");
        if (moveId == null || moveId.isBlank()) throw new IllegalArgumentException("moveId is required");

        double dx = target.getX() - attacker.getX();
        double dz = target.getZ() - attacker.getZ();
        if (dx != 0.0D || dz != 0.0D) {
            float yaw = (float) (MathHelper.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
            attacker.setYaw(yaw);
            attacker.setHeadYaw(yaw);
            attacker.setBodyYaw(yaw);
        }

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

        // targetHp is authoritative PTU state, but it is not on Cobblemon's native HP scale.
        // Do not write it into Pokemon.currentHealth and do not infer a ratio without authoritative
        // max HP. In particular, targetHp == 0 is NOT permission to trigger Cobblemon faint/death.
        FabricBattleHpPresentationNetworking.broadcast(entity, targetHp);

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
            return;
        }
        if (!(entity.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

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

        var cueText = net.minecraft.text.Text.literal(statusSkipText(command));
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
