# Cobblemon UI and Presentation Reuse Doctrine

This project must reuse existing Minecraft ecosystem infrastructure before creating Ouros-specific equivalents.

The preferred implementation order is strict:

1. Minecraft vanilla when it already provides the required platform behavior.
2. Cobblemon-specific UI, models, entities, renderers, animations, effects, registries, networking, blocks or interaction surfaces.
3. Compatible, maintained third-party Fabric mods that already solve the generic infrastructure problem cleanly.
4. Minecraft-native generic primitives.
5. Ouros-specific fallback code only when the existing ecosystem does not provide a safe, maintainable fit.

The rule is strict for UI, battle presentation and generic Minecraft support systems:

1. Before adding or extending any party screen, Pokemon summary, PC/storage screen, move/action selector, HUD, dialogue/menu surface, move animation, particle effect, sound cue, model animation, battle presentation path, backpack, inventory organizer, equipment/accessory slot system, recipe browser, map marker system, generic storage layer, tooltip framework, config screen, keybind helper or similar infrastructure, inspect the pinned Cobblemon/Minecraft stack and compatible maintained mods for an equivalent or reusable component.
2. Prefer reuse or a thin adapter over reimplementation when the dependency is compatible with the pinned Minecraft/Fabric/Cobblemon versions, actively maintained enough for the project, legally distributable in the intended modpack, and can be integrated without surrendering Ouros/AutoPTU authority.
3. Ouros-specific UI or infrastructure is allowed only when no suitable existing surface exists, when an available implementation is inseparably coupled to gameplay authority we must not trust, or when compatibility, maintenance, licensing, stability, performance or distribution constraints make the dependency unsuitable.
4. Reusing a Cobblemon or third-party screen, renderer, model, animation, particle, sound, inventory/container, equipment slot, block, entity, registry, networking primitive or algorithm never transfers RPG or battle authority to that mod. AutoPTU-Java remains authoritative for battle legality, RNG, targeting, movement, damage, HP, statuses, action economy, Trainer Features, item effects, faint, terminal results and battle outcomes. AutoPTU-Cobblemon-RPG remains authoritative for persistent Ouros RPG/world state.
5. Cobblemon Pokemon/BattleState payloads and third-party mod state must not become canonical RPG or battle inputs merely because their presentation or infrastructure code is reused. Canonical state flows from server-authoritative AutoPTU/Ouros services into the reused surface. A third-party mod may own ordinary Minecraft-only state when that state has no Ouros/PTU semantic authority.
6. Existing Ouros UIs and generic infrastructure are migration candidates. If a safe pinned Cobblemon or compatible-mod component can expose the same server-authoritative state, prefer replacing the duplicate Ouros implementation rather than maintaining both.

## Dependency admission gate

A third-party mod is a candidate only after checking all of these:

- Version compatibility with the pinned Minecraft, Fabric Loader/API and Cobblemon stack.
- Maintenance/activity sufficient for the expected project lifetime, or a stable implementation whose maintenance burden is acceptably low.
- License and redistribution terms compatible with the intended Ouros modpack/distribution model.
- Server/client requirements and whether headless dedicated-server operation remains valid.
- No required trust in client-supplied gameplay truth.
- No ownership of PTU battle rules, canonical Pokemon state, encounter outcomes, progression truth or Ouros economy/quest state unless explicitly isolated as non-authoritative presentation.
- API/integration surface stable enough that the adapter is cheaper and safer than maintaining a duplicate implementation.
- Performance and dependency footprint acceptable for the playable modpack.

## Good reuse candidates

Generic Minecraft support systems should strongly prefer ecosystem reuse when a suitable mod exists. Examples include backpacks and inventory organization, accessory/equipment slots, generic storage/container UX, recipe/item viewers, map/minimap markers, tooltip frameworks, configuration screens, keybind helpers, generic dialogue/widget libraries and other infrastructure that does not define Ouros/PTU rules.

When these systems touch canonical RPG state, the external mod is a frontend or platform layer. Server-authoritative Ouros services still validate and commit the meaningful mutation. For example, an equipment-slot mod may render and organize a slot, but it cannot decide PTU equip legality or item effects. A backpack mod may own ordinary Minecraft inventory contents, while canonical Ouros items that participate in RPG transactions remain governed by the canonical item services.

## Required audit order

Before adding new visual or generic infrastructure, audit these existing areas against the pinned ecosystem:

- Party management and Pokemon summary surfaces.
- Cobblemon PC/storage interaction and screen components.
- Battle HUD and legal action/move selection presentation.
- NPC/menu/dialogue presentation where Cobblemon or another compatible mod already exposes reusable widgets.
- Battle move presentation, including move-specific animations/effects first and physical/special/status or other generic Cobblemon fallbacks second.
- Backpacks/inventory organization and item browsing.
- Equipment/accessory slot frameworks.
- Generic storage/container UX.
- Recipe/item viewers and tooltips.
- Map/minimap markers, config UI and keybind helpers.

## Battle playback contract

AutoPTU-Java emits or owns the semantic battle fact. The Fabric adapter resolves presentation only.

For a move event, the adapter should attempt presentation in this order:

1. A Cobblemon move-specific animation/effect for the authoritative move ID, when available and safe to invoke without Cobblemon BattleState authority.
2. A Cobblemon generic move/category animation/effect appropriate to the authoritative event metadata.
3. A compatible existing presentation framework when it can consume the authoritative event without introducing gameplay authority.
4. A Minecraft-native presentation primitive when it communicates the event clearly.
5. A minimal Ouros fallback that does not infer legality, hit, damage, status, faint or result.

A missing presentation implementation must never cause Minecraft to invent the missing PTU outcome.

## Review gate

Any PR that adds a new custom UI, battle visual or generic Minecraft infrastructure should state which pinned Minecraft/Cobblemon/compatible-mod equivalents were checked and why reuse was selected or rejected. Any new third-party dependency should also document version compatibility, maintenance status, license/distribution fit, dedicated-server behavior and authority boundaries. For substantial UI/playback/infrastructure work, the canonical `MINECRAFT_RPG_TOOLING_PLAN.md` must record the corresponding audit/migration item before or with implementation.
