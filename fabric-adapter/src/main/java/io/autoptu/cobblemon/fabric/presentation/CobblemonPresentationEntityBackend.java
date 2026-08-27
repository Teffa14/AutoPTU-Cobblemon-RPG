package io.autoptu.cobblemon.fabric.presentation;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.battlecore.BattlePresentationCommand;
import io.autoptu.cobblemon.battlecore.PresentationEntityPlatformBackend;
import io.autoptu.cobblemon.battlecore.WorldBlockCoordinate;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

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

        // Presentation-only lunge. Runtime/grid position is unchanged; callers return the entity to
        // its authoritative presentation anchor after the cue. No range or movement legality is
        // inferred from this visual displacement.
        double nextX = attacker.getX() + (target.getX() - attacker.getX()) * 0.45D;
        double nextY = attacker.getY();
        double nextZ = attacker.getZ() + (target.getZ() - attacker.getZ()) * 0.45D;
        attacker.requestTeleport(nextX, nextY, nextZ);
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
