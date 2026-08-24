# PRE-damage reaction integration boundary

Inspected read-only upstream heads:

- AutoPTU-Java: `359c31638448f23b6da230679988e42f21777abc`
- AutoPTU Python: `a868d8a95b467030187482c4bf61da600bab912d`
- AutoPTU-Java Telepathy parity oracle pin: `16d228efa63aabecb67fa788959a359aac7f8f03`

## Verified upstream primitives

AutoPTU-Java owns the generic PRE-damage reaction registry and invokes it from authoritative move resolution. Current main ports both Telepathy and Perception through that seam. Reaction context, ability identity and suppression, team identity, out-of-turn decision gates, threatened tiles, legal reaction movement and semantic event emission remain core-owned.

For Perception specifically, AutoPTU-Java `359c31638448f23b6da230679988e42f21777abc` owns readiness consumption, round-scoped usage, optional out-of-turn decisions, safe-tile selection, reaction movement and hit/damage cancellation. Minecraft must not create or consume Perception temporary effects or choose a destination.

The current Python oracle at `a868d8a95b467030187482c4bf61da600bab912d` continues to execute Perception as a `pre_damage_interrupt`. It requires `perception_ready`, applies the out-of-turn decision gate, confirms an area move, rejects repeated round-scoped use, derives legal shift tiles, selects a safe tile outside the threatened area, writes `perception_used`, emits an ability movement event and cancels hit, damage and type effectiveness after a successful escape. The adapter does not reproduce any of those rules.

AutoPTU-Java current main also supports authoritative multi-target area execution and proves Telepathy through that execution path. That bounded support does not make the full move-specific, ability, reaction, movement or stateful-damage categories complete.

## Adapter rule

Minecraft/Cobblemon/Craftics may consume and project semantic reaction movement and rule-effect events emitted by AutoPTU-Java. The adapter may translate authoritative grid coordinates to world coordinates and animate or render the result.

The adapter must not invoke the PRE-damage registry, construct or override threatened tiles, evaluate Perception or Telepathy eligibility, consume `perception_ready`, create `perception_used`, choose escape destinations, decide movement legality, mutate action economy, cancel hit or damage, change type effectiveness, reorder hooks or mutate HP/history. Those remain authoritative core responsibilities.

The existing semantic playback boundary verifies stable combatant identity, authoritative event ordering, grid-to-world translation and immutable projection inputs. It does not encode ability-specific legality.

## Compatibility dependencies

This bounded slice depends on CORE_TARGETING for authoritative affected-area semantics, CORE_MOVEMENT_LEGALITY for legal reaction shifts, COMPLETE_MOVEMENT_BEHAVIOR for the wider forced-movement/interception family, ACTION_ECONOMY_AND_INITIATIVE for out-of-turn semantics, FULL_STATEFUL_DAMAGE_PIPELINE for hit/damage/post-hook/HP ordering, TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS for reaction-phase semantics, ABILITIES for Telepathy and Perception legality, MOVE_SPECIFIC_BEHAVIOR for authoritative area execution, and MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK for projection.

Verified for this slice: generic PRE-damage registry invocation; canonical threatened-area context; authoritative reaction-escape movement for the verified hooks; parity-backed Telepathy boundary; authoritative Perception hook; Perception readiness and round-scoped usage ownership; Python Perception oracle behavior; cancellation before downstream HP/history mutation; semantic rule-effect and relocation projection; stable combatant identity; immutable adapter inputs.

Partial for this slice: complete movement behavior; full stateful damage; full turn/round lifecycle; complete status lifecycle; terrain/weather/hazards/zones/reactions; move-specific behavior; abilities; items; Trainer Features; AI tactical scoring/policy; Minecraft/Cobblemon/Craftics playback.

Blocking for broader promotion: the wider forced-movement, push, pull, knockback and interception family is not complete; additional abilities and reaction families are not complete; full stateful modifier coverage across abilities/items/terrain is not complete; AI tactical policy remains incomplete. Representative Perception and Telepathy support must not be interpreted as full-category parity.

## Intentionally deferred

Minecraft does not special-case Perception or Telepathy. It does not calculate affected tiles, select safe squares, spend reaction resources, manipulate readiness/usage state, suppress abilities, cancel damage, execute PTU hooks or manufacture forced movement legality. It consumes authoritative state and events only.

## Next bounded slice

After this PR is clean, refresh the multi-target projection PR against current main and verify that authoritative area results plus PRE-damage reaction events preserve declaration ordering through the generic playback stream without introducing move- or ability-specific adapter branches.
