# Cobblemon UI and Presentation Reuse Doctrine

This project must reuse Cobblemon and Minecraft presentation infrastructure before creating Ouros-specific equivalents.

The rule is strict for UI and battle presentation:

1. Before adding or extending any party screen, Pokemon summary, PC/storage screen, move/action selector, HUD, dialogue/menu surface, move animation, particle effect, sound cue, model animation or battle presentation path, inspect the pinned Cobblemon version for an equivalent or reusable component.
2. Preferred presentation order is: Cobblemon-specific component/effect/animation -> Cobblemon generic component/effect/animation -> Minecraft-native equivalent -> Ouros fallback.
3. Ouros-specific UI or presentation is allowed only when no suitable native surface exists, or when the existing Cobblemon implementation is inseparably coupled to Cobblemon battle authority or gameplay truth that AutoPTU must not trust.
4. Reusing a Cobblemon screen, renderer, model, animation, particle, sound, block, entity, registry or networking primitive never transfers gameplay authority to Cobblemon. AutoPTU-Java remains authoritative for battle legality, RNG, targeting, movement, damage, HP, statuses, action economy, Trainer Features, item effects, faint, terminal results and battle outcomes.
5. Cobblemon Pokemon/BattleState payloads must not become canonical RPG or battle inputs merely because their presentation code is reused. Presentation data flows one-way from server-authoritative AutoPTU/Ouros state into Cobblemon/Minecraft rendering surfaces.
6. Existing Ouros UIs are migration candidates. If a pinned Cobblemon component can safely display the same server-authoritative state, prefer replacing the duplicate Ouros surface rather than maintaining both.

## Required audit order

Before adding new visual infrastructure, audit these existing areas against the pinned Cobblemon version:

- Party management and Pokemon summary surfaces.
- Cobblemon PC/storage interaction and screen components.
- Battle HUD and legal action/move selection presentation.
- NPC/menu/dialogue presentation where Cobblemon already exposes reusable widgets.
- Battle move presentation, including move-specific animations/effects first and physical/special/status or other generic Cobblemon fallbacks second.

## Battle playback contract

AutoPTU-Java emits or owns the semantic battle fact. The Fabric adapter resolves presentation only.

For a move event, the adapter should attempt presentation in this order:

1. A Cobblemon move-specific animation/effect for the authoritative move ID, when available and safe to invoke without Cobblemon BattleState authority.
2. A Cobblemon generic move/category animation/effect appropriate to the authoritative event metadata.
3. A Minecraft-native presentation primitive when it communicates the event clearly.
4. A minimal Ouros fallback that does not infer legality, hit, damage, status, faint or result.

A missing presentation implementation must never cause Minecraft to invent the missing PTU outcome.

## Review gate

Any PR that adds a new custom UI or battle visual should state which pinned Cobblemon/Minecraft equivalents were checked and why reuse was either selected or rejected. For substantial UI/playback work, the canonical `MINECRAFT_RPG_TOOLING_PLAN.md` must record the corresponding audit/migration item before or with implementation.
