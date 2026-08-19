package io.autoptu.cobblemon.battlecore;

import java.util.Optional;

/** Server-owned move catalog lookup. Implementations must not source metadata from client packets. */
@FunctionalInterface
public interface AuthoritativeMoveCatalog {
    Optional<AuthoritativeMoveMetadata> findByMoveId(String moveId);
}
