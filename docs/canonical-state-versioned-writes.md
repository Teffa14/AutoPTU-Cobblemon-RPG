# Canonical state versioned writes

Persistent Trainer/Pokemon/item/progression state is server authority. CanonicalPlayerState carries a monotonically increasing revision. The integration exposes an optimistic-concurrency write boundary so stale requests cannot silently overwrite newer canonical state.

VersionedCanonicalStateRepository defines replacePlayerIfRevision. Storage implementations compare expectedRevision and replace the aggregate atomically. Returning false means the caller lost the write race and must re-read current authority.

CanonicalPlayerMutationService accepts only a server-owned mutation callback. It reads the current aggregate, verifies the expected revision, runs the domain mutation, requires the replacement to preserve player identity and advance revision by exactly one, then delegates the final compare-and-set to storage. A second writer that wins between read and commit produces CONCURRENT_WRITE rather than a lost update.

FileVersionedCanonicalStateRepository is the first durable implementation. It stores one schema-versioned binary aggregate per canonical player under a server-owned root. Player IDs are mapped to SHA-256 file keys rather than trusted as filesystem paths. Record encoding is deterministic for sets and maps. Unsupported schema versions, malformed records, identity mismatches and invalid revision advances fail closed.

Writes for one player serialize through an in-process lock and a stable operating-system file lock. The implementation writes a temporary file in the same directory, forces the file contents, then requires ATOMIC_MOVE + REPLACE_EXISTING for publication. It does not silently fall back to a non-atomic rename. Independent repository instances therefore have one compare-and-set winner for the same expected revision, and a fresh repository instance can reload the committed aggregate after process restart.

FabricCanonicalPlayerStoreRuntime binds that repository to the real server lifecycle. On SERVER_STARTED it derives the root from MinecraftServer.getSavePath(WorldSavePath.ROOT) and opens `<world>/autoptu/canonical-state`. On SERVER_STOPPED it drops the runtime handle for that MinecraftServer instance. Minecraft selects the world-owned storage location only; no ServerPlayerEntity or world property is translated into Trainer values.

Dedicated-server CI exercises the actual lifecycle twice against one world directory. Boot one starts Fabric/Cobblemon, creates a fixed server-owned CanonicalPlayerState fixture and verifies the exact stored aggregate. CI then stops that process and boots a second server process over the same save. Boot two opens a new repository instance and must read the exact fixture before the smoke can pass. This proves the world path, Fabric lifecycle binding, file publication and cross-process reload together rather than only exercising an in-memory fixture.

The implementation also exposes createPlayerIfAbsent for server-side bootstrap of a new canonical player. Existing state wins; bootstrap never overwrites an existing aggregate.

A Minecraft packet remains an intent. Clients may send an action request and a revision as a concurrency hint, but server code must independently validate authenticated identity, permissions, inventory, progression requirements and the requested domain operation before it builds the mutation callback. Client-supplied CanonicalPlayerState replacements remain outside this boundary.

The durability claim remains bounded to one CanonicalPlayerState aggregate. Pokemon/item/arena persistence, the production identity-to-canonical-player-context source, cross-aggregate transactions, transaction journals, partial-commit recovery, backup/restore policy and migration from future schemas remain separate work. The adapter must not infer PTU rules while those persistence pieces are added.

Compatibility scope: this infrastructure depends only on the Minecraft/Cobblemon/Craftics adapter/server-authority boundary. It does not execute targeting, movement, damage, statuses, moves, abilities, items, Trainer Features, terrain, reactions or tactical AI. Those systems remain limited by their own upstream capability classifications.
