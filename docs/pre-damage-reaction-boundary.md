# PRE-damage reaction integration boundary

Inspected read-only upstream heads:

- AutoPTU-Java: `ebfdf7b29da5cdde9b7df7bd6d193ae03f5203f7`
- AutoPTU Python: `2ec841a4bab8ce7de0698afaf37e0169ae61a277`
- AutoPTU-Java Telepathy parity oracle pin: `16d228efa63aabecb67fa788959a359aac7f8f03`

## Verified upstream primitives

AutoPTU-Java has a generic `PreDamageReactionHookRegistry` contract. `BuiltinPreDamageReactionHooks` registers Telepathy against canonical `BattleRuntimeState`, authoritative ability identity/suppression, team identity, the server-owned out-of-turn decision gate, threatened tiles and `ReactionMovementApplication.escapeThreatenedArea`.

A successful Telepathy reaction emits an authoritative ability `RuleEffectEvent` and cancels the current pre-damage result. The reaction movement itself is applied through the core-owned movement primitive rather than through Minecraft.

The current Python oracle executes Telepathy as a `pre_damage_interrupt`: an allied defender inside the affected area may shift to a legal safe tile; only a successful escape cancels hit, damage and type effectiveness.

## Blocking gap

At the inspected Java head, `RuntimeMoveResolution.applyUsingAuthoritativeCombatState` resolves effective-move hooks, authoritative combat state, damage-modifier hooks and post-damage hooks, then delegates to `BattleRuntime`. It does not invoke the generic PRE-damage reaction registry.

Therefore this repository must not claim ordinary live Telepathy execution yet. The missing upstream integration also owns the correct affected-area construction and exact hook ordering relative to accuracy, damage and other PRE-damage mechanics.

## Adapter rule

Minecraft/Cobblemon/Craftics may project a semantic reaction movement or ability event only after AutoPTU-Java has emitted it as an authoritative result. The adapter may translate grid coordinates to world coordinates and animate/render the event.

The adapter must not evaluate Telepathy ownership, suppression, Mold Breaker, team eligibility, threatened-area membership, decision gating, safe-tile selection, movement legality, action-economy effects, hit cancellation, damage cancellation or type-effectiveness cancellation.

This intentionally keeps COMPLETE_MOVEMENT_BEHAVIOR blocking, FULL_STATEFUL_DAMAGE_PIPELINE partial, TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS partial and ABILITIES partial. The presence of one parity-backed reaction hook does not promote any whole category.

## Compatibility dependencies

This bounded guard depends on CORE_TARGETING for authoritative affected-area semantics, CORE_MOVEMENT_LEGALITY for legal reaction movement, COMPLETE_MOVEMENT_BEHAVIOR for the wider reaction/forced-movement family, ACTION_ECONOMY_AND_INITIATIVE for out-of-turn action semantics, FULL_STATEFUL_DAMAGE_PIPELINE for hit/damage cancellation, TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS for reaction ordering, ABILITIES for Telepathy legality and MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK for projection only.

## Next safe promotion

When AutoPTU-Java inserts the generic PRE-damage reaction registry into the ordinary authoritative move-resolution path and tests the affected-area construction plus hook ordering, this repository can add an adapter contract fixture that consumes the resulting semantic movement/event sequence without implementing Telepathy-specific rules.
