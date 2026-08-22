# AutoPTU Cobblemon RPG

Server-authoritative Minecraft/Cobblemon integration layer for PTU.

This repository is the writable integration project. `Teffa14/AutoPTU-Java` and `Teffa14/AutoPTU` are read-only upstream/reference repositories for this project.

## Authority boundary

AutoPTU-Java owns battle legality, calculations, lifecycle and outcomes. Minecraft, Fabric and Cobblemon own world projection, entities, networking, animation and rendering. Client packets are intents only. Cobblemon entity state must not become the source of truth for PTU stats, HP, moves, abilities, inventory, legality or results.

## Runtime validation policy

Integration work prioritizes vertical runtime tests. A feature is not considered live merely because DTO tests pass or Fabric/Cobblemon code compiles.

For every bounded slice where the environment permits it, CI exercises the smallest real Minecraft/Fabric/Cobblemon behavior and verifies the resulting server state. Headless contract tests remain required for authority boundaries, but they do not replace runtime evidence.

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

The Minecraft/Cobblemon adapter category is PARTIAL. Relocation, positive HP mirroring, early public battle-start preemption, participant identity capture, canonical identity mapping and multi-side roster reservation have bounded runtime evidence. Full Trainer/item/arena composition, zero-HP/faint presentation, move animation, semantic cues and complete battle playback remain pending.

## Encounter reservation model

The original `BattleAuthoritySnapshot` remains the authoritative single-player Trainer/item reservation path and is not discarded. It requires its Pokémon/items to belong to that player because it protects persistent player assets.

Multi-side encounters now have a separate owner-neutral combatant layer:

- `CanonicalBattlePokemonView` defines the canonical battle fields without treating inventory ownership as affiliation.
- `CanonicalEncounterPokemonState` represents server-owned wild/NPC combatants without a fake player owner.
- `BattleCombatantAuthoritySnapshot` freezes participant ID, explicit battle team and participant kind separately from persistence ownership.
- `BattleEncounterRosterReservationService` resolves canonical IDs and atomically locks both encounter rosters. Battle team IDs are generated server-side from the encounter side and cannot be supplied by Cobblemon or a client.

The live Cobblemon bridge maps external UUIDs only to canonical IDs. It never maps external stats into those records. The production smoke currently uses a wild-versus-wild topology because CI has no authenticated client player; it proves the identity-to-canonical-reservation pipeline, not yet the full player Trainer/item composition.

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

Next:

10. Compose the multi-side roster reservation with the existing Trainer/item/arena authority needed for a real player-versus-wild encounter.
11. Feed the completed reservation into the Java runtime when AutoPTU-Java can authoritatively materialize every required `RuntimeCombatantState` field.
12. Join both directions into the first minimal vertical battle: Cobblemon encounter -> AutoPTU reservation/core -> semantic events -> live entity presentation.

Public Cobblemon APIs/events should be preferred where they provide an early enough hook. Cobblemon 1.7.3 exposes the cancelable `BATTLE_STARTED_PRE` event, and the dedicated-server smoke proves that cancellation prevents the battle from reaching the registry or post-start event. Mixins should therefore be reserved for gaps that public hooks cannot cover. PTU rules must never be implemented inside a Mixin.
