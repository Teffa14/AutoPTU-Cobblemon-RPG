package io.autoptu.cobblemon.fabric.presentation;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.battlecore.BattlePresentationCommand;
import io.autoptu.cobblemon.battlecore.PresentationEntityPlatformBackend;
import io.autoptu.cobblemon.battlecore.WorldBlockCoordinate;

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

        // Semantic cues are authoritative rendering inputs. Until a concrete visual renderer is
        // attached, accepting the cue is intentionally side-effect free. This keeps the Fabric
        // entity stream ordered without interpreting ability, item, status, terrain, reaction or
        // Trainer Feature semantics inside Minecraft.
    }
}
