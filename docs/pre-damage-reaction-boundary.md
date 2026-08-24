# PRE-damage reaction integration boundary

Inspected read-only upstream heads:

- AutoPTU-Java: `d829224655f057d19a7470a0bc5cfa1f0bdefeda`
- AutoPTU Python: `7605265f2548a3967b2de3eb00cc0db33b0e9303`
- AutoPTU-Java PRE-damage parity oracle pin: `16d228efa63aabecb67fa788959a359aac7f8f03`

## Verified upstream primitives

AutoPTU-Java owns the generic PRE-damage reaction registry and invokes it from authoritative move resolution. Current main ports Telepathy, Perception, Perception [Errata], and Parry through that seam. Reaction context, ability identity and suppression, team identity, out-of-turn decision gates, threatened tiles, effective target kind, supported legal reaction movement, temporary-effect bookkeeping, and semantic event emission remain core-owned.

Perception owns readiness consumption, round-scoped usage, optional decisions, safe-tile selection, reaction movement and hit/damage cancellation. Perception [Errata] owns allied damaging-area eligibility plus bounded disengage selection. Parry owns `parry_ready` consumption, same-round `parry_used` bookkeeping, authoritative melee-target-kind checks, semantic `avoid` emission and hit/damage/type-effectiveness cancellation. Minecraft must not create or consume those temporary effects, classify targeting semantics, or choose movement destinations.

AutoPTU-Java `d829224655f057d19a7470a0bc5cfa1f0bdefeda` additionally freezes the Python Sway contract. The current Python oracle confirms Sway rejects recursive redirects, requires damaging melee targeting, checks once-per-use and STANDARD availability, asks the optional decision before spending STANDARD, records usage, installs a round-scoped recursion guard, emits redirect before recursive resolution, resolves the attacker into its own move, clears the guard, selects a legal adjacent push destination from authoritative grid state, emits push, and only then cancels the original result.

That is contract evidence only. Java does not yet provide authoritative Sway recursive redirect execution plus post-redirect push execution. Therefore Sway is blocked at the adapter boundary. Minecraft must not approximate any part of it.

AutoPTU-Java also supports bounded authoritative multi-target area execution. That support does not make the full move-specific, ability, reaction, movement or stateful-damage categories complete.

## Adapter rule

Minecraft/Cobblemon/Craftics may consume and project semantic reaction movement and rule-effect events emitted by AutoPTU-Java. The adapter may translate authoritative grid coordinates to world coordinates and animate or render the result.

The adapter must not invoke the PRE-damage registry, construct or override threatened tiles, derive effective target kind, classify a move as melee/ranged/area, evaluate reaction eligibility, consume `perception_ready` or `parry_ready`, create `perception_used`, `parry_used`, `sway_used` or `sway_redirect`, choose escape or push destinations, decide movement legality, recursively re-resolve moves, mutate action economy, cancel hit or damage, change type effectiveness, reorder hooks or mutate HP/history. Those remain authoritative core responsibilities.

The existing semantic playback boundary verifies stable combatant identity, authoritative event ordering, grid-to-world translation and immutable projection inputs. It does not encode ability-specific legality.

## Compatibility dependencies

This bounded slice depends on CORE_TARGETING for authoritative affected-area and target-kind semantics, CORE_MOVEMENT_LEGALITY for legal reaction shifts, COMPLETE_MOVEMENT_BEHAVIOR for the wider forced-movement/push/pull/interception family, ACTION_ECONOMY_AND_INITIATIVE for out-of-turn and STANDARD semantics, FULL_STATEFUL_DAMAGE_PIPELINE for hit/damage/post-hook/HP ordering, TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS for reaction-phase semantics, ABILITIES for reaction legality, MOVE_SPECIFIC_BEHAVIOR for authoritative move targeting and recursive resolution, and MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK for projection.

Verified for this slice: generic PRE-damage registry invocation; canonical threatened-area context; authoritative effective target kind; authoritative reaction-escape movement for verified hooks; parity-backed Telepathy, Perception, Perception [Errata], and Parry hooks; Perception and Parry temporary-effect ownership; frozen Sway Python oracle contract; cancellation before downstream HP/history mutation for executed hooks; semantic rule-effect and relocation projection; stable combatant identity; immutable adapter inputs.

Partial for this slice: complete movement behavior; full stateful damage; full turn/round lifecycle; complete status lifecycle; terrain/weather/hazards/zones/reactions; move-specific behavior; abilities; items; Trainer Features; AI tactical scoring/policy; Minecraft/Cobblemon/Craftics playback.

Blocking for broader promotion: Sway authoritative recursive redirect and post-redirect push are not implemented in Java; the wider forced-movement, push, pull, knockback and interception family is not complete; additional abilities and reaction families are not complete; full stateful modifier coverage across abilities/items/terrain is not complete; AI tactical policy remains incomplete. Representative support must not be interpreted as full-category parity.

## Intentionally deferred

Minecraft does not special-case Perception, Perception [Errata], Parry, Telepathy or Sway. It does not calculate affected tiles, derive target kind, select safe or push squares, spend reaction resources, manipulate readiness/usage state, suppress abilities, redirect attacks, recursively execute moves, cancel damage, execute PTU hooks or manufacture forced-movement legality. It consumes authoritative state and events only.

## Next bounded slice

When AutoPTU-Java promotes Sway from frozen oracle contract to authoritative execution, add an integration contract proving redirect event ordering, recursive-result ownership, recursion-guard cleanup and push playback remain core-owned. Until then, reconcile the generic multi-target projection stream without adding Sway-specific adapter behavior.
