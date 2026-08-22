package io.autoptu.cobblemon.authority;

/**
 * Persistence boundary for canonical player state with optimistic concurrency control.
 *
 * Implementations must perform the revision comparison and replacement atomically. A false result
 * means that the expected revision no longer owns the write. Callers must re-read authoritative
 * state instead of retrying a stale client-derived mutation.
 */
public interface VersionedCanonicalStateRepository extends CanonicalStateRepository {
    boolean replacePlayerIfRevision(
            String playerId,
            long expectedRevision,
            CanonicalPlayerState replacement
    );
}
