# AutoPTU Cobblemon RPG

Server-authoritative Minecraft/Cobblemon integration layer for PTU.

This repository is the writable integration project. `Teffa14/AutoPTU-Java` and `Teffa14/AutoPTU` are read-only upstream/reference repositories for this project.

## Authority boundary

AutoPTU-Java owns battle legality, calculations, lifecycle and outcomes. Minecraft, Fabric and Cobblemon own world projection, entities, networking, animation and rendering. Client packets are intents only. Cobblemon entity state must not become the source of truth for PTU stats, HP, moves, abilities, inventory, legality or results.

## Runtime validation policy

Integration work now prioritizes vertical runtime tests. A feature is not considered live merely because DTO tests pass or Fabric/Cobblemon code compiles.

For every bounded slice where the environment permits it, CI should exercise the smallest real Minecraft/Fabric/Cobblemon behavior and verify the resulting server state. Headless contract tests remain required for authority boundaries, but they do not replace runtime evidence.

Current runtime evidence:

- A production-remapped Fabric 1.21.1 dedicated server boots in CI.
- Cobblemon 1.7.3 and Fabric Language Kotlin load in that server.
- The AutoPTU Fabric server initializer executes.
- The adapter compiles directly against Cobblemon `PokemonEntity` and performs server-side UUID lookup without treating that entity as PTU authority.
- CI spawns a real Cobblemon Pokémon, binds its opaque UUID through the presentation registry/gateway, applies an already-authoritative relocation, and verifies the live server position.
- CI separately spawns a real Cobblemon Pokémon, sends an already-authoritative positive HP projection through the entity-bound presentation consumer, and verifies the exact displayed HP mirror.
- CI invokes a real Cobblemon `BattleRegistry.startBattle` with live Pokémon fixtures. The adapter claims the encounter through public `BATTLE_STARTED_PRE`, cancels the start, verifies `ErroredBattleStart`, verifies that the battle never enters Cobblemon's registry, and verifies that `BATTLE_STARTED_POST` never fires.
- That same pre-start handoff now carries only opaque side, actor-kind, actor UUID and Pokémon UUID identities. The production server smoke verifies that both live Cobblemon participants are captured exactly before cancellation. No stats, HP, moves, abilities or Showdown state cross this handoff.

The Minecraft/Cobblemon adapter category is PARTIAL. Relocation, positive HP mirroring, early public battle-start preemption and participant-identity capture now have real runtime evidence. AutoPTU reservation creation from those identities, zero-HP/faint presentation, move animation, semantic cues and complete battle playback remain pending.

## Reservation handoff constraint

The current authoritative `BattleAuthoritySnapshot` represents one canonical player and requires every reserved Pokémon/item to belong to that same player. That was correct for the original reservation slices, but it cannot yet represent a full player-versus-wild or player-versus-NPC encounter.

The Cobblemon adapter must not work around this by inventing synthetic ownership, importing Cobblemon stats, or creating a second unofficial battle state. Intercepted UUIDs are lookup keys only. The next authority slice must extend the server-owned reservation model to represent opposing sides explicitly before the live encounter can become one AutoPTU battle reservation.

## Near-term vertical test ladder

Completed with production dedicated-server evidence:

1. Spawn a real Cobblemon `PokemonEntity`.
2. Bind its UUID as an opaque presentation entity ID.
3. Send an already-authoritative relocation through the existing presentation path.
4. Resolve the UUID server-side and relocate the live entity.
5. Verify the entity's final server position.
6. Mirror an already-authoritative positive HP result and verify the exact live Cobblemon representation without reading it back into canonical PTU state.
7. Intercept and cancel a real Cobblemon battle start through public `BATTLE_STARTED_PRE` before Cobblemon registers or launches that battle.
8. Capture side/actor/Pokémon participant identities through an adapter-owned DTO and verify them against the live battle fixtures without exposing Cobblemon battle state as PTU authority.

Next:

9. Extend the server-owned battle reservation model to explicit opposing sides, then resolve intercepted identity keys only against canonical records and create one AutoPTU reservation for the encounter.
10. Join both directions into the first minimal vertical battle: Cobblemon encounter -> AutoPTU reservation/core -> semantic events -> live entity presentation.

Public Cobblemon APIs/events should be preferred where they provide an early enough hook. Cobblemon 1.7.3 exposes the cancelable `BATTLE_STARTED_PRE` event, and the dedicated-server smoke proves that cancellation prevents the battle from reaching the registry or post-start event. Mixins should therefore be reserved for gaps that public hooks cannot cover. PTU rules must never be implemented inside a Mixin.
