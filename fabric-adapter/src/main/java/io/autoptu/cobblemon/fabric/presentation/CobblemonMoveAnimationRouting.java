package io.autoptu.cobblemon.fabric.presentation;

import com.cobblemon.mod.common.api.moves.animations.ActionEffects;
import net.minecraft.util.Identifier;

import java.util.Comparator;
import java.util.List;

/**
 * Runtime routing for battle presentation assets.
 *
 * Exact Cobblemon ActionEffects are always preferred. When an exact move asset is missing, the
 * router selects a deterministic loaded substitute proposed by the presentation-only fallback
 * planner. Mechanical move identity never changes: AutoPTU still resolves the original move id and
 * this route is used only after that result already exists.
 */
public final class CobblemonMoveAnimationRouting {
    private CobblemonMoveAnimationRouting() {
    }

    public enum Source {
        DIRECT_NATIVE,
        SUBSTITUTE_NATIVE,
        GENERIC_FALLBACK
    }

    public record Route(
            Source source,
            String requestedMoveId,
            String effectPath,
            int variant
    ) {
        public Route {
            if (source == null) throw new IllegalArgumentException("source is required");
            requestedMoveId = BattleMoveAnimationProfile.normalize(requestedMoveId);
            if (effectPath == null) effectPath = "";
            effectPath = effectPath.strip();
            if (variant < 0 || variant > 3) throw new IllegalArgumentException("variant must be 0..3");
            if (source != Source.GENERIC_FALLBACK && effectPath.isBlank()) {
                throw new IllegalArgumentException("native route requires effectPath");
            }
        }

        public boolean nativeEffect() {
            return source != Source.GENERIC_FALLBACK;
        }

        public boolean substituted() {
            return source == Source.SUBSTITUTE_NATIVE;
        }
    }

    public static Route resolve(String moveId) {
        String normalized = BattleMoveAnimationProfile.normalize(moveId);
        var effects = ActionEffects.INSTANCE.getActionEffects();
        Identifier directId = Identifier.of("cobblemon", normalized);
        if (effects.containsKey(directId)) {
            return new Route(Source.DIRECT_NATIVE, normalized, normalized, 0);
        }

        CobblemonMoveAnimationFallbackPlanner.Plan plan =
                CobblemonMoveAnimationFallbackPlanner.plan(normalized);
        for (String candidate : plan.candidateEffectPaths()) {
            if (effects.containsKey(Identifier.of("cobblemon", candidate))) {
                return new Route(Source.SUBSTITUTE_NATIVE, normalized, candidate, plan.variant());
            }
        }
        return new Route(Source.GENERIC_FALLBACK, normalized, "", plan.variant());
    }

    /** Returns loaded Cobblemon move ActionEffect paths for the in-game animation test controls. */
    public static List<String> loadedNativeMoveEffects() {
        return ActionEffects.INSTANCE.getActionEffects().keySet().stream()
                .filter(id -> "cobblemon".equals(id.getNamespace()))
                .map(Identifier::getPath)
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}
