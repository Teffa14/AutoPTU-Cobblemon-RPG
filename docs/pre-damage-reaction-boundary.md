# PRE-damage reaction integration boundary

Inspected read-only upstream heads:

- AutoPTU-Java: `5bb41ed8d137e3067258d38e9dd04b2cf0840750`
- AutoPTU Python: `7605265f2548a3967b2de3eb00cc0db33b0e9303`
- AutoPTU-Java Telepathy parity oracle pin: `16d228efa63aabecb67fa788959a359aac7f8f03`

## Verified upstream primitives

AutoPTU-Java owns the generic PRE-damage reaction registry and invokes it from authoritative move resolution. Current main ports Telepathy, Perception, Perception [Errata], and Parry through that seam. Reaction context, ability identity and suppression, team identity, out-of-turn decision gates, threatened tiles, effective target kind, legal reaction movement, temporary-effect bookkeeping, and semantic event emission remain core-owned.

Perception owns readiness consumption, round-scoped usage, optional decisions, safe-tile selection, reaction movement and hit/damage cancellation. Perception [Errata] owns allied damaging-area eligibility plus bounded disengage selection. Parry now owns `parry_ready` consumption, same-round `parry_used` bookkeeping, authoritative melee-target-kind checks, semantic `avoid` emission and hit/damage/type-effectiveness cancellation. Minecraft must not create or consume those temporary effects, classify targeting semantics, or choose movement destinations.

The current Python oracle at `7605265f2548a3967b2de3eb00cc0db33b0e9303` confirms the same ordering. Perception consumes readiness only after the first optional gate and performs its safe-area Shift entirely in rules code. Perception [Errata] handles allied area disengage without adapter participation. Parry consumes `parry_ready` after the optional gate, checks normalized target kind for `melee`, blocks repeat use in the same round with `parry_used`, emits an ability `avoid` event and cancels the incoming hit. The adapter reproduces none of those rules.

AutoPTU-Java current main also supports authoritative multi-target area execution. That bounded support does not make the full move-specific, ability, reaction, movement or stateful-damage categories complete.

## Adapter rule

Minecraft/Cobblemon/Craftics may consume and project semantic reaction movement and rule-effect events emitted by AutoPTU-Java. The adapter may translate authoritative grid coordinates to world coordinates and animate or render the result.

The adapter must not invoke the PRE-damage registry, construct or override threatened tiles, derive effective target kind, classify a move as melee/ranged/area, evaluate Perception, Perception [Errata], Parry or Telepathy eligibility, consume `perception_ready` or `parry_ready`, create `perception_used` or `parry_used`, choose escape destinations, decide movement legality, mutate action economy, cancel hit or damage, change type effectiveness, reorder hooks or mutate HP/history. Those remain authoritative core responsibilities.

The existing semantic playback boundary verifies stable combatant identity, authoritative event ordering, grid-to-world translation and immutable projection inputs. It does not encode ability-specific legality.

## Compatibility dependencies

This bounded slice depends on CORE_TARGETING for authoritative affected-area and target-kind semantics, CORE_MOVEMENT_LEGALITY for legal reaction shifts, COMPLETE_MOVEMENT_BEHAVIOR for the wider forced-movement/interception family, ACTION_ECONOMY_AND_INITIATIVE for out-of-turn semantics, FULL_STATEFUL_DAMAGE_PIPELINE for hit/damage/post-hook/HP ordering, TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS for reaction-phase semantics, ABILITIES for Telepathy/Perception/Parry legality, MOVE_SPECIFIC_BEHAVIOR for authoritative move targeting and area execution, and MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK for projection.

Verified for this slice: generic PRE-damage registry invocation; canonical threatened-area context; authoritative effective target kind; authoritative reaction-escape movement for verified hooks; parity-backed Telepathy, Perception, Perception [Errata], and Parry hooks; Perception and Parry temporary-effect ownership; Python oracle ordering for those hooks; cancellation before downstream HP/history mutation; semantic rule-effect and relocation projection; stable combatant identity; immutable adapter inputs.

Partial for this slice: complete movement behavior; full stateful damage; full turn/round lifecycle; complete status lifecycle; terrain/weather/hazards/zones/reactions; move-specific behavior; abilities; items; Trainer Features; AI tactical scoring/policy; Minecraft/Cobblemon/Craftics playback.

Blocking for broader promotion: the wider forced-movement, push, pull, knockback and interception family is not complete; additional abilities and reaction families are not complete; full stateful modifier coverage across abilities/items/terrain is not complete; AI tactical policy remains incomplete. Representative support must not be interpreted as full-category parity.

## Intentionally deferred

Minecraft does not special-case Perception, Perception [Errata], Parry or Telepathy. It does not calculate affected tiles, derive target kind, select safe squares, spend reaction resources, manipulate readiness/usage state, suppress abilities, cancel damage, execute PTU hooks or manufacture forced movement legality. It consumes authoritative state and events only.

## Next bounded slice

After this PR is clean, reconcile the multi-target projection PR with this updated PRE-damage contract and verify that ordered authoritative area results plus reaction events survive the generic playback stream without move- or ability-specific adapter branches.