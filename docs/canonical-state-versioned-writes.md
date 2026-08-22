# Canonical state versioned writes

Persistent Trainer/Pokemon/item/progression state is server authority. CanonicalPlayerState already carries a monotonically increasing revision. The integration now exposes an explicit optimistic-concurrency write boundary so stale requests cannot silently overwrite newer canonical state.

VersionedCanonicalStateRepository extends the existing read boundary with replacePlayerIfRevision. Storage implementations must compare expectedRevision and replace the aggregate atomically. Returning false means the caller lost the write race and must re-read current authority.

CanonicalPlayerMutationService accepts only a server-owned mutation callback. It first reads the current aggregate, verifies the expected revision, runs the domain mutation, then requires the replacement to preserve player identity and advance revision by exactly one. The repository performs the final atomic compare-and-set. A second writer that wins between read and commit produces CONCURRENT_WRITE rather than a lost update.

This contract does not make a Minecraft packet authoritative. Clients may send action intent and a revision as a concurrency hint, but server code must independently validate identity, permissions, inventory, progression requirements and the requested domain action before constructing the mutation callback. Client-supplied CanonicalPlayerState replacements are outside this boundary.

This slice defines the persistence/versioning contract but does not choose a durable backend. File, SQL or other stores must implement the atomic compare-and-set semantics before they can be treated as production persistence. Pokemon/item aggregate writes and cross-aggregate transactions remain separate future slices.

Compatibility scope: this infrastructure depends only on the Minecraft/Cobblemon/Craftics adapter/server-authority boundary. It does not execute targeting, movement, damage, statuses, moves, abilities, items, Trainer Features, terrain, reactions or tactical AI. Those systems remain limited by their own upstream capability classifications.
