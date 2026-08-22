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
- The adapter compiles directly against Cobblemon `PokemonEntity` and can perform server-side UUID lookup without reading that entity as PTU authority.

The live battle-playback category remains blocked until a battle-derived presentation command is executed against a real spawned `PokemonEntity` and verified from the running server.

## Near-term vertical test ladder

1. Spawn a real Cobblemon `PokemonEntity` in the dedicated-server smoke.
2. Bind its UUID as an opaque presentation entity ID.
3. Send an already-authoritative relocation through the existing entity-bound presentation path.
4. Resolve the UUID server-side and relocate the live entity.
5. Verify the entity's final server position.
6. Add authoritative HP presentation without treating Cobblemon HP as canonical state.
7. Intercept a real Cobblemon battle trigger early enough to create an AutoPTU reservation without entering Cobblemon's battle engine.
8. Join both directions into the first minimal vertical battle: Cobblemon encounter -> AutoPTU reservation/core -> semantic events -> live entity presentation.

Public Cobblemon APIs/events should be preferred where they provide an early enough hook. Mixins should be narrow interception points only. PTU rules must never be implemented inside a Mixin.
