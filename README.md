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

The Minecraft/Cobblemon adapter category is PARTIAL. Relocation and positive HP mirroring have real runtime evidence. Zero-HP/faint presentation, move animation, semantic cues, early battle-trigger interception and complete battle playback are not yet verified.

## Near-term vertical test ladder

Completed with production dedicated-server evidence:

1. Spawn a real Cobblemon `PokemonEntity`.
2. Bind its UUID as an opaque presentation entity ID.
3. Send an already-authoritative relocation through the existing presentation path.
4. Resolve the UUID server-side and relocate the live entity.
5. Verify the entity's final server position.
6. Mirror an already-authoritative positive HP result and verify the exact live Cobblemon representation without reading it back into canonical PTU state.

Next:

7. Intercept a real Cobblemon battle start through the earliest adequate public cancelable event and route it toward an AutoPTU reservation without launching Cobblemon's battle engine.
8. Join both directions into the first minimal vertical battle: Cobblemon encounter -> AutoPTU reservation/core -> semantic events -> live entity presentation.

Public Cobblemon APIs/events should be preferred where they provide an early enough hook. Cobblemon 1.7.3 exposes the cancelable `BATTLE_STARTED_PRE` event, whose documented cancellation prevents the `PokemonBattle` from being created and launched. Mixins should therefore be reserved for gaps that public hooks cannot cover. PTU rules must never be implemented inside a Mixin.
