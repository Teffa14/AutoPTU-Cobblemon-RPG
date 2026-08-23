# Durable canonical Pokemon state

This slice persists the complete server-owned `CanonicalPokemonState` without moving PTU mechanics into Minecraft.

`FileCanonicalPokemonRepository` stores one schema-versioned aggregate per canonical Pokemon ID. IDs are SHA-256 hashed before becoming filenames. Writes use an in-process lock, an OS file lock, forced temporary-file contents, and required atomic replacement. `replacePokemonIfRevision` accepts exactly one revision advance and therefore prevents stale writers from silently replacing newer state.

The stored aggregate includes canonical ownership, species identity, level, capabilities, ordered status entries and scalar status metadata, combat stats, HP, move IDs, base movement, type/ability identities, baseline accuracy/evasion values, injuries, held-item instance identity and revision. A new repository instance can reopen the same directory and reproduce the complete aggregate.

Status storage follows the current upstream contract. Multiple entries with the same normalized status name may coexist in insertion order. The legacy `statuses` set remains a unique name view. Persistence preserves every stacked entry and its metadata; it does not interpret duration, expiry, cure, immunity, source, tick timing or any other status rule.

Authority boundary:

- clients and Cobblemon entities never create or replace canonical Pokemon aggregates directly;
- Minecraft health, attributes, species data, moves, abilities and held items do not become PTU truth;
- revision values are concurrency guards, not client authority;
- base movement is persisted as a canonical baseline only; runtime movement modifiers remain AutoPTU-Java-owned;
- move IDs, abilities and held-item identities are persisted identities only; their behavior remains upstream-owned;
- HP and injuries are canonical persistent values, but battle damage/injury rules and outcome commits remain authoritative services;
- stacked status representation is supported, while complete status lifecycle remains partial upstream and is not reproduced here.

Compatibility dependencies are core movement legality, core calculations/combat stats, stateful damage, status lifecycle, move-specific behavior, abilities, items and Minecraft/Cobblemon adapter persistence. The slice consumes storage/input contracts only and has no dependency on a BLOCKING category.

Still pending: wiring this Pokemon repository and the durable item ledger into the Fabric world lifecycle, multi-aggregate transaction journaling/recovery, reservation reconciliation, and a logged-in graphical player-versus-wild runtime test.
