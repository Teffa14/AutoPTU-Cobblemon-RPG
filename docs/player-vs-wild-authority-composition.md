# Player-versus-wild authority composition

This integration joins server-authoritative reservation paths without moving PTU rules into Minecraft or Cobblemon.

`BattleAuthorityService` protects persistent player assets. It resolves canonical Trainer state, owned Pokémon, held/consumable items and the selected `BattleArenaSnapshot`. `BattleEncounterRosterReservationService` protects battle affiliation. It resolves canonical combatants for explicit PLAYER/WILD sides without treating persistence ownership as team membership.

`PlayerVsWildEncounterAuthorityService` composes those paths for exactly one PLAYER participant and one WILD participant. It accepts canonical IDs only. External Minecraft/Cobblemon UUIDs must pass through the identity boundary first.

The composition requires the PLAYER participant ID to equal the persistent player ID. Its ordered combatant IDs must exactly equal the Pokémon IDs requested from player authority. Both participants must have different encounter sides. NPC, multi-player and multi-wild-party topologies remain outside this bounded contract.

A package-private `BattleReservationAuthority` carries one server-issued reservation ID and RNG seed into both reservation services. Public standalone service APIs still create their own server authority and do not accept client-supplied IDs or seeds.

Reservation ordering is deliberate. Player Trainer/Pokémon/items/arena are frozen first. The multi-side canonical roster lock runs second with the same identity and seed. If the second reservation is denied, the first reservation is released before the denial is returned. A compensation failure raises an exception rather than returning a false clean result.

The resulting `PlayerVsWildBattleReservation` verifies the shared reservation ID, shared RNG seed, player participant identity and exact player roster again before it can exist.

## Cobblemon pre-start claim boundary

`CobblemonPlayerVsWildClaimCoordinator` bridges the identity-only `BATTLE_STARTED_PRE` signal to composed authority. It accepts one PLAYER actor and one opposed WILD actor. Before reservation it can rebuild both identity mappings from server-owned sources.

For PLAYER, `PersistentCanonicalPlayerPokemonIdentityBinder` requires an authenticated Minecraft session, durable canonical player/profile state, exact roster cardinality and canonical Pokémon ownership. Only then are opaque Cobblemon Pokémon UUIDs mapped to the ordered canonical Pokémon IDs.

For WILD, `ServerOwnedWildEncounterIdentityBinder` accepts only the Cobblemon battle ID, side, WILD actor UUID and ordered Pokémon UUIDs as correlation keys. A `CanonicalWildRosterSource` must already provide the canonical participant ID and ordered canonical Pokémon IDs from trusted server-owned encounter data. Exact roster cardinality and registry alias protection are required before the mapping is installed. See `docs/server-owned-wild-identity-binding.md`.

Neither binder reads species, level, HP, stats, moves, abilities, held items, statuses, inventory truth, legality or outcomes from Cobblemon. Missing canonical state fails closed. The existing interceptor cancels Cobblemon only when the complete authority reservation succeeds.

The authenticated player context supplies canonical player/Pokémon IDs, requested consumable quantities and the server-owned `BattleArenaSnapshot`. `BattleAuthorityService` re-resolves the persistent Pokémon/items and validates ownership and quantities during reservation; a persisted selection is not permission by itself.

## Fabric authenticated-player boundary

`FabricAuthenticatedPlayerContextResolver` parses the Cobblemon PLAYER actor ID as an exact UUID and asks the live `MinecraftServer` player manager for that UUID. Only a currently connected `ServerPlayerEntity` is accepted.

That lookup proves session presence only. The resolver does not read Minecraft health, attributes, inventory, permissions, position, game mode or any other player field into PTU state. After the online check succeeds, `PersistentCanonicalPlayerEncounterContextSource` loads the world-scoped canonical player/profile state.

Malformed UUIDs, offline players, missing canonical records and null source results fail closed. Dedicated-server CI verifies the real failure boundary. A successful logged-in graphical client encounter remains a separate runtime milestone.

The world-scoped runtime now opens durable player, encounter-profile, Pokémon and item/reservation stores. Those stores provide server-owned state across restart. They still do not create a cross-aggregate crash-safe transaction, and WILD canonical battle-value provisioning remains a separate service boundary.

This integration still does not authorize Minecraft to execute missing PTU mechanics. Runtime combat state, remaining damage/status behavior, move specials, ability families, item effects, Trainer Feature effects, terrain semantics, forced movement, reactions and tactical AI remain governed by their current upstream support classifications.
