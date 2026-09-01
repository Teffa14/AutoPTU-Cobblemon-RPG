package io.autoptu.cobblemon.fabric.presentation;

import io.autoptu.cobblemon.battlecore.AuthoritativeMoveCatalog;
import io.autoptu.cobblemon.battlecore.AuthoritativeMoveMetadata;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Maps server-owned AutoPTU move metadata to Cobblemon's native generic poser animations.
 *
 * Only an explicit authoritative physical/special damage category is mapped. A move without combat
 * metadata stays unresolved rather than being guessed as a status move; the presentation backend can
 * then use its minimal neutral fallback until upstream metadata explicitly exposes that distinction.
 */
public final class AuthoritativeCobblemonMoveAnimationResolver implements CobblemonMoveAnimationResolver {
    private final AuthoritativeMoveCatalog catalog;

    public AuthoritativeCobblemonMoveAnimationResolver(AuthoritativeMoveCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    @Override
    public Optional<String> resolve(String moveId) {
        if (moveId == null || moveId.isBlank()) return Optional.empty();
        return catalog.findByMoveId(moveId.strip())
                .filter(metadata -> moveId.strip().equals(metadata.moveId()))
                .map(AuthoritativeMoveMetadata::combat)
                .flatMap(this::resolveCombatCategory);
    }

    private Optional<String> resolveCombatCategory(AuthoritativeMoveMetadata.Combat combat) {
        if (combat == null) return Optional.empty();
        String category = combat.damageCategory().toLowerCase(Locale.ROOT);
        return switch (category) {
            case "physical", "special" -> Optional.of(category);
            default -> Optional.empty();
        };
    }
}
