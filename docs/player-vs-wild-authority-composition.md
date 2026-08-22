# Player-versus-wild authority composition

This slice joins existing server-authoritative reservation paths without moving PTU rules into Minecraft or Cobblemon.

`BattleAuthorityService` protects persistent player assets. It resolves canonical Trainer state, owned Pokémon, held/consumable items and the selected `BattleArenaSnapshot`. `BattleEncounterRosterReservationService` protects battle affiliation. It resolves canonical combatants for explicit PLAYER/WILD sides without treating persistence ownership as team membership.

`PlayerVsWildEncounterAuthorityService` composes those paths for exactly one PLAYER participant and one WILD participant. It accepts canonical IDs only. External Minecraft/Cobblemon UUIDs must already have passed through the identity mapping boundary before this service is called.

The composition requires the PLAYER participant ID to equal the persistent player ID. Its ordered combatant IDs must exactly equal the Pokémon IDs requested from player authority. Both participants must have different encounter sides. NPC, multi-player and multi-wild-party topologies remain outside this bounded contract.

A package-private `BattleReservationAuthority` carries one server-issued reservation ID and RNG seed into both existing reservation services. Public standalone service APIs still create their own server authority and do not accept client-supplied IDs or seeds.

Reservation ordering is deliberate. Player Trainer/Pokémon/items/arena are frozen first. The multi-side canonical roster lock runs second with the same identity and seed. If the second reservation is denied, the first reservation is released before the denial is returned. A compensation failure raises an exception rather than leaving the caller with a false successful/clean result.

The resulting `PlayerVsWildBattleReservation` verifies the shared reservation ID, shared RNG seed, player participant identity and exact player roster again before it can exist.

## Cobblemon pre-start claim boundary

`CobblemonPlayerVsWildClaimCoordinator` is the adapter-owned bridge from the identity-only `BATTLE_STARTED_PRE` signal to this composed authority. It accepts only one PLAYER actor and one opposed WILD actor. Before any reservation attempt it requires a server-owned authenticated player context keyed by the external player actor UUID, resolves both actors through `CobblemonCanonicalEncounterIdentityRegistry`, and requires the authenticated canonical player ID and ordered Pokémon roster to match the canonical identity mapping exactly.

The authenticated context supplies canonical player/Pokémon IDs, requested consumable quantities and the server-owned `BattleArenaSnapshot`. None of those values are trusted from the Cobblemon battle object. The production constructor requires `PlayerVsWildEncounterAuthorityService` directly and treats only an allowed composed reservation as a successful claim. Returning false leaves the Cobblemon battle untouched. Returning true is the only path that allows the existing interceptor to cancel Cobblemon.

The coordinator rejects missing authentication, unresolved identities, roster mismatches, NPC/multi-party topologies, same-side PLAYER/WILD pairs and authority denial. It does not fall back to Cobblemon data when canonical state is missing.

## Fabric authenticated-player boundary

`FabricAuthenticatedPlayerContextResolver` now supplies the production Minecraft authentication check expected by the coordinator. It parses the Cobblemon PLAYER actor ID as an exact UUID and asks the live `MinecraftServer` player manager for that UUID. Only a currently connected `ServerPlayerEntity` is accepted.

That lookup proves session presence only. The resolver does not read Minecraft health, attributes, inventory, permissions, position, game mode or any other player field into PTU state. After the online check succeeds, it calls a separate `CanonicalPlayerEncounterContextSource` with the authenticated UUID. That source remains responsible for loading canonical Trainer/Pokémon/item/arena state from server-owned persistence.

Malformed UUIDs, offline players, missing canonical records and null source results fail closed. Offline and malformed identities are rejected before the canonical source is invoked. This prevents an external actor ID from becoming an implicit login or a source of battle state.

Dedicated-server CI cannot provide an authenticated graphical client. The production smoke therefore proves the real `MinecraftServer.getPlayerManager()` failure path: an offline UUID and a malformed identity cannot reach canonical PTU context resolution. Unit contract tests cover the successful lookup seam. A successful logged-in client encounter remains a separate runtime milestone and must not be claimed from this headless smoke.

This slice does not materialize `RuntimeCombatantState`. It does not read species, level, HP, stats, moves, abilities, held items, Showdown state, Trainer Features, inventory truth or outcomes from Cobblemon or `ServerPlayerEntity`. Those values remain canonical server state and may enter AutoPTU-Java only through verified upstream runtime contracts.

It also does not make the two persistence repositories a distributed transaction. The current boundary provides deterministic identity plus reservation-time compensation. A future persistence/versioning slice should provide a single durable encounter transaction or recovery record before crash-safe cross-store semantics are claimed.

Capability dependencies remain narrow. The authentication resolver consumes only the partial Minecraft/Cobblemon adapter category. Player-versus-wild authority composition also depends on the partial Items and Trainer Features categories for canonical reservation state, while executing none of their battle effects. Core calculations, targeting, movement, damage, statuses, move specials, abilities, Trainer Feature effects and AI tactical policy remain outside this slice.
