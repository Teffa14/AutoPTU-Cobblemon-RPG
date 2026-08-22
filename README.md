# AutoPTU Cobblemon RPG

Server-authoritative Minecraft/Cobblemon integration layer for PTU.

This repository is the writable integration project. `Teffa14/AutoPTU-Java` and `Teffa14/AutoPTU` are read-only upstream/reference repositories for this project.

## Authority boundary

AutoPTU-Java owns battle legality, calculations, lifecycle and outcomes. Minecraft, Fabric and Cobblemon own world projection, entities, networking, animation and rendering. Client packets are intents only. Cobblemon entity state must not become the source of truth for PTU stats, HP, moves, abilities, inventory, legality or results.

## Runtime validation policy

Integration work prioritizes vertical runtime tests. A feature is not considered live merely because DTO tests pass or Fabric/Cobblemon code compiles.

For every bounded slice where the environment permits it, CI exercises the smallest real Minecraft/Fabric/Cobblemon behavior and verifies the resulting server state. Headless contract tests remain required for authority boundaries, but they do not replace runtime evidence.

Every CI run preserves test evidence as a GitHub Actions artifact, including Gradle/JUnit results, complete authority-test output, dedicated-server logs, run metadata and explicit runtime acceptance markers. The evidence upload runs on success and failure and is retained for 14 days. Graphical Minecraft client tests must write MP4/PNG evidence under `test-evidence/visual/`; that directory is already included in the artifact contract. The current production smoke is a `nogui` dedicated server, so its evidence is server logs and markers rather than fabricated video. See `docs/test-evidence.md`.

Current runtime evidence:

- A production-remapped Fabric 1.21.1 dedicated server boots in CI.
- Cobblemon 1.7.3 and Fabric Language Kotlin load in that server.
- The AutoPTU Fabric server initializer executes.
- The adapter compiles directly against Cobblemon `PokemonEntity` and performs server-side UUID lookup without treating that entity as PTU authority.
- CI spawns a real Cobblemon Pokémon, binds its opaque UUID through the presentation registry/gateway, applies an already-authoritative relocation, and verifies the live server position.
- CI separately spawns a real Cobblemon Pokémon, sends an already-authoritative positive HP projection through the entity-bound presentation consumer, and verifies the exact displayed HP mirror.
- CI invokes a real Cobblemon `BattleRegistry.startBattle` with live Pokémon fixtures. The adapter claims the encounter through public `BATTLE_STARTED_PRE`, cancels the start, verifies `ErroredBattleStart`, verifies that the battle never enters Cobblemon's registry, and verifies that `BATTLE_STARTED_POST` never fires.
- The pre-start handoff carries only opaque side, actor-kind, actor UUID and Pokémon UUID identities. No stats, HP, moves, abilities or Showdown state cross that handoff.
- Those live UUIDs are mapped through a server-side identity registry to independently created canonical participant/combatant IDs. The canonical records use fixed PTU fixture values rather than values read from the live Cobblemon entities.
- The mapped canonical combatants are resolved through the authority repository and atomically reserved on two explicit opposing sides before the interceptor is allowed to cancel Cobblemon's battle.
- The Fabric player-context resolver checks an external PLAYER UUID against the real `MinecraftServer` player manager before canonical PTU state may be queried. Dedicated-server CI verifies malformed and offline identities fail before the canonical context source is called.
- `FabricCanonicalPlayerStoreRuntime` opens the durable canonical-player repository beneath the active world's save root. CI seeds a fixed canonical Trainer aggregate in one dedicated-server process, shuts it down, boots the same world in a second process and verifies the exact aggregate from a fresh repository instance.

The Minecraft/Cobblemon adapter category remains PARTIAL. Relocation, positive HP mirroring, early public battle-start preemption, participant identity capture, canonical identity mapping, multi-side roster reservation, the fail-closed live Minecraft player-authentication boundary and world-scoped single-player persistence have bounded dedicated-server evidence. Player-versus-wild Trainer/item/arena composition has contract-test coverage. A successful logged-in graphical player encounter, durable Pokémon/item/arena composition, zero-HP/faint presentation, move animation, semantic cues, runtime materialization and complete battle playback remain pending.

## Encounter reservation model

The original `BattleAuthoritySnapshot` remains the authoritative player Trainer/item reservation path. It protects persistent ownership and freezes Trainer state, owned Pokémon, held/consumable items, an arena snapshot and a server-generated RNG seed.

Multi-side encounters have a separate owner-neutral combatant layer:

- `CanonicalBattlePokemonView` defines canonical battle fields without treating inventory ownership as affiliation.
- `CanonicalEncounterPokemonState` represents server-owned wild/NPC combatants without a fake player owner.
- `BattleCombatantAuthoritySnapshot` freezes participant ID, explicit battle team and participant kind separately from persistence ownership.
- `BattleEncounterRosterReservationService` resolves canonical IDs and atomically locks encounter rosters. Battle team IDs are generated server-side from encounter side and cannot be supplied by Cobblemon or a client.

`PlayerVsWildEncounterAuthorityService` composes those two authority paths for the bounded player-versus-wild topology. It requires exactly one canonical PLAYER participant and one WILD participant on different sides. The player participant ID and ordered Pokémon roster must exactly match the persistent player reservation. Both reservations receive the same server-issued reservation ID and RNG seed. If the multi-side lock fails after player assets were reserved, the player reservation is released before the denial is returned. See `docs/player-vs-wild-authority-composition.md`.

`FabricAuthenticatedPlayerContextResolver` sits before that composition on the live Fabric side. Minecraft proves that the external PLAYER UUID belongs to a currently connected server player. A separate canonical source then resolves the persistent encounter context. The resolver never converts `ServerPlayerEntity` attributes, inventory, health, position or permissions into PTU state.

`FabricCanonicalPlayerStoreRuntime` owns one `FileVersionedCanonicalStateRepository` per live `MinecraftServer`. The repository root is `<world>/autoptu/canonical-state`. It is created from `MinecraftServer.getSavePath(WorldSavePath.ROOT)` at `SERVER_STARTED` and released at `SERVER_STOPPED`. This runtime currently persists only `CanonicalPlayerState`; it does not yet supply the complete player-versus-wild encounter context because canonical Pokémon, items and arena state still require their own durable authority path.

The live Cobblemon bridge maps external UUIDs only to canonical IDs. It never maps external stats into those records. The production battle interception smoke remains wild-versus-wild because dedicated CI has no authenticated client player. The player authentication smoke therefore proves the real fail-closed `PlayerManager` path, while the successful logged-in player-versus-wild path still requires a client runtime.

## Near-term vertical test ladder

Completed with production dedicated-server evidence:

1. Spawn a real Cobblemon `PokemonEntity`.
2. Bind its UUID as an opaque presentation entity ID.
3. Send an already-authoritative relocation through the existing presentation path.
4. Resolve the UUID server-side and relocate the live entity.
5. Verify the entity's final server position.
6. Mirror an already-authoritative positive HP result and verify the exact live Cobblemon representation without reading it back into canonical PTU state.
7. Intercept and cancel a real Cobblemon battle start through public `BATTLE_STARTED_PRE` before Cobblemon registers or launches that battle.
8. Capture side/actor/Pokémon participant identities through an adapter-owned DTO.
9. Map those opaque UUIDs to independently server-owned canonical participant/combatant IDs and create an atomic two-side canonical roster reservation before cancellation succeeds.
10. Verify the production Fabric authentication boundary rejects malformed/offline PLAYER UUIDs through the real `MinecraftServer` player manager before canonical PTU encounter state is queried.
11. Open canonical player persistence under the active world save, write fixed server-owned Trainer state, restart the actual dedicated server over the same world and verify the exact state from a fresh repository instance.

Completed with authority contract tests:

12. Compose the player-owned Trainer/Pokémon/item/arena reservation and the owner-neutral player-versus-wild roster reservation under one server-issued identity and deterministic seed, with exact roster matching and compensation on second-stage lock failure.
13. Verify the authenticated-player resolver accepts an online UUID only before delegating to a separate canonical encounter-context source, without reading PTU state from Minecraft player fields.

Next:

14. Add durable canonical Pokémon/item/arena authority and a server-owned identity-to-encounter-context source so the authenticated player path no longer depends on an abstract context provider.
15. Run the full player-versus-wild pre-start path with an authenticated graphical Minecraft client and record the encounter: connected player -> Cobblemon `BATTLE_STARTED_PRE` -> authenticated identity -> canonical reservation -> Cobblemon preemption.
16. Feed the completed reservation into the Java runtime only when AutoPTU-Java can authoritatively materialize every required `RuntimeCombatantState` field.
17. Join both directions into the first minimal vertical battle: Cobblemon encounter -> AutoPTU reservation/core -> semantic events -> live entity presentation.

Public Cobblemon APIs/events should be preferred where they provide an early enough hook. Cobblemon 1.7.3 exposes the cancelable `BATTLE_STARTED_PRE` event, and the dedicated-server smoke proves that cancellation prevents the battle from reaching the registry or post-start event. Mixins should therefore be reserved for gaps that public hooks cannot cover. PTU rules must never be implemented inside a Mixin.
