package io.autoptu.cobblemon.authority;

import java.util.Optional;

/** Durable server-owned encounter selection store with optimistic concurrency. */
public interface VersionedCanonicalPlayerEncounterProfileRepository {
    Optional<CanonicalPlayerEncounterProfile> findProfile(String playerId);

    boolean createProfileIfAbsent(CanonicalPlayerEncounterProfile initialProfile);

    boolean replaceProfileIfRevision(
            String playerId,
            long expectedRevision,
            CanonicalPlayerEncounterProfile replacement
    );
}
