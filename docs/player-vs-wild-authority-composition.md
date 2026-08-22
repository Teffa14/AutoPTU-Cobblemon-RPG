# Player-versus-wild authority composition

This slice joins two existing server-authoritative reservation paths without moving PTU rules into Minecraft or Cobblemon.

`BattleAuthorityService` protects persistent player assets. It resolves canonical Trainer state, owned Pokémon, held/consumable items and the selected `BattleArenaSnapshot`. `BattleEncounterRosterReservationService` protects battle affiliation. It resolves canonical combatants for explicit PLAYER/WILD sides without treating persistence ownership as team membership.

`PlayerVsWildEncounterAuthorityService` composes those paths for exactly one PLAYER participant and one WILD participant. It accepts canonical IDs only. External Minecraft/Cobblemon UUIDs must already have passed through the identity mapping boundary before this service is called.

The composition requires the PLAYER participant ID to equal the persistent player ID. Its ordered combatant IDs must exactly equal the Pokémon IDs requested from player authority. Both participants must have different encounter sides. NPC, multi-player and multi-wild-party topologies remain outside this bounded contract.

A package-private `BattleReservationAuthority` carries one server-issued reservation ID and RNG seed into both existing reservation services. Public standalone service APIs still create their own server authority and do not accept client-supplied IDs or seeds.

Reservation ordering is deliberate. Player Trainer/Pokémon/items/arena are frozen first. The multi-side canonical roster lock runs second with the same identity and seed. If the second reservation is denied, the first reservation is released before the denial is returned. A compensation failure raises an exception rather than leaving the caller with a false successful/clean result.

The resulting `PlayerVsWildBattleReservation` verifies the shared reservation ID, shared RNG seed, player participant identity and exact player roster again before it can exist.

This slice does not materialize `RuntimeCombatantState`. It does not read species, level, HP, stats, moves, abilities, held items, Showdown state, Trainer Features, inventory truth or outcomes from Cobblemon. Those values remain canonical server state and may enter AutoPTU-Java only through verified upstream runtime contracts.

It also does not make the two persistence repositories a distributed transaction. The current boundary provides deterministic identity plus reservation-time compensation. A future persistence/versioning slice should provide a single durable encounter transaction or recovery record before crash-safe cross-store semantics are claimed.

Capability dependencies for this slice are narrow. Core calculations, targeting, movement, damage, statuses, move specials, abilities, Trainer Feature execution and AI tactical policy are not executed here. The direct dependencies are canonical item/Trainer authority, canonical combatant identity, arena snapshot authority, deterministic seeds and the Minecraft/Cobblemon adapter identity boundary. Items and Trainer Features remain PARTIAL upstream categories, so this service freezes identities/resources but never executes their battle effects.
