package io.autoptu.cobblemon.battlecore;

import java.util.Objects;

/**
 * Resolves reservation-scoped presentation IDs to live platform handles before forwarding already-
 * authoritative presentation operations to the platform backend.
 */
public final class RegistryBackedPresentationEntityGateway<T> implements PresentationEntityGateway {
    private final PresentationEntityHandleRegistry<T> registry;
    private final PresentationEntityPlatformBackend<T> backend;

    public RegistryBackedPresentationEntityGateway(
            PresentationEntityHandleRegistry<T> registry,
            PresentationEntityPlatformBackend<T> backend
    ) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.backend = Objects.requireNonNull(backend, "backend");
    }

    @Override
    public void animateMove(
            String reservationId,
            String attackerPresentationEntityId,
            String targetPresentationEntityId,
            String moveId
    ) {
        backend.animateMove(
                registry.require(reservationId, attackerPresentationEntityId),
                registry.require(reservationId, targetPresentationEntityId),
                requireIdentifier(moveId, "moveId")
        );
    }

    @Override
    public void projectDisplayedHealth(
            String reservationId,
            String presentationEntityId,
            int targetHp,
            int damage
    ) {
        if (targetHp < 0) throw new IllegalArgumentException("targetHp cannot be negative");
        if (damage < 0) throw new IllegalArgumentException("damage cannot be negative");
        backend.projectDisplayedHealth(
                registry.require(reservationId, presentationEntityId),
                targetHp,
                damage
        );
    }

    @Override
    public void relocate(
            String reservationId,
            String presentationEntityId,
            WorldBlockCoordinate origin,
            WorldBlockCoordinate destination
    ) {
        backend.relocate(
                registry.require(reservationId, presentationEntityId),
                Objects.requireNonNull(origin, "origin"),
                Objects.requireNonNull(destination, "destination")
        );
    }

    @Override
    public void showCue(
            String reservationId,
            String presentationEntityId,
            BattlePresentationCommand command
    ) {
        backend.showCue(
                registry.require(reservationId, presentationEntityId),
                Objects.requireNonNull(command, "command")
        );
    }

    private static String requireIdentifier(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.strip();
    }
}
